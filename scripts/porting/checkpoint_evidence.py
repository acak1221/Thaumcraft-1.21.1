"""Bind local validation to its actual inputs, fresh reports and required cases.

This is evidence for a development checkpoint, not a gameplay parity verdict.
"""
from __future__ import annotations

from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import time
from xml.etree import ElementTree as ET


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_fingerprints(root: Path) -> dict[str, str]:
    """Include every active source set, resources, build configuration and tools.

    Include the retained asset conversion inputs, but not the uncompiled legacy
    Java/oracle trees, caches, generated outputs or worlds. Hash bytes, not
    mtimes or Git HEAD (work may be dirty).
    """
    files = set()
    for relative in ("common/src", "fabric/src", "neoforge/src", "src/main/resources", "gradle", "scripts"):
        directory = root / relative
        if directory.is_dir():
            files.update(path for path in directory.rglob("*")
                         if path.is_file() and "__pycache__" not in path.parts
                         and path.suffix != ".pyc")
    for relative in ("AGENTS.md", "build.gradle", "settings.gradle", "gradle.properties",
                     "gradlew", "gradlew.bat", "LICENSE", "THIRD_PARTY_NOTICES.md",
                     "common/build.gradle", "fabric/build.gradle", "neoforge/build.gradle",
                     "docs/porting/source-lock.json"):
        path = root / relative
        if path.is_file():
            files.add(path)
    return {path.relative_to(root).as_posix(): sha256(path) for path in sorted(files)}


def begin_run(root: Path, target: Path) -> dict:
    report = {"schema_version": 1, "started_ns": time.time_ns(),
              "source_fingerprints_sha256": source_fingerprints(root)}
    # An old snapshot must not accidentally be relabelled as a fresh run.
    with target.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(report, stream, ensure_ascii=False, indent=2)
        stream.write("\n")
    return report


def validate_snapshot(root: Path, snapshot: dict) -> int:
    if snapshot.get("schema_version") != 1:
        raise ValueError("Unknown validation snapshot schema")
    started_ns = snapshot.get("started_ns")
    if type(started_ns) is not int or not 0 < started_ns <= time.time_ns():
        raise ValueError("Invalid validation start time")
    expected = snapshot.get("source_fingerprints_sha256")
    if not isinstance(expected, dict) or not expected:
        raise ValueError("Validation snapshot has no source fingerprints")
    current = source_fingerprints(root)
    changed = sorted(name for name in expected.keys() | current.keys()
                     if expected.get(name) != current.get(name))
    if changed:
        raise ValueError("Inputs changed during/after validation: " + ", ".join(changed[:12]))
    return started_ns


def fresh_output(path: Path, started_ns: int) -> dict:
    if not path.is_file():
        raise ValueError(f"Missing validation output: {path}")
    stat = path.stat()
    if stat.st_mtime_ns < started_ns:
        raise ValueError(f"Stale validation output (rerun tasks): {path}")
    if not stat.st_size:
        raise ValueError(f"Empty validation output: {path}")
    return {"sha256": sha256(path), "bytes": stat.st_size, "mtime_ns": stat.st_mtime_ns}


def junit_reports(root: Path) -> list[Path]:
    """Require a report for each conventional *Test.java in the common suite."""
    sources = root / "common/src/test/java"
    results = root / "common/build/test-results/test"
    expected = set()
    for path in sources.rglob("*Test.java"):
        # Several existing package-private policy tests intentionally live in a
        # different directory. Java's declared package, not the path, names XML.
        match = re.search(r"(?m)^\s*package\s+([\w.]+)\s*;", path.read_text(encoding="utf-8-sig"))
        prefix = match.group(1) + "." if match else ""
        expected.add("TEST-" + prefix + path.stem + ".xml")
    actual = {path.name: path for path in results.glob("TEST-*.xml")}
    if not expected or not actual:
        raise ValueError("JUnit sources or results are absent")
    missing = sorted(expected - actual.keys())
    if missing:
        raise ValueError("Missing JUnit suites: " + ", ".join(missing))
    return [actual[name] for name in sorted(actual)]


# JUnit suites that read an original JAR the repository does not track (the owner had them removed from the history on
# 2026-10-26). Each skips through an assumption when its JAR is absent from the repository root; nothing else may skip.
ORIGINAL_JAR_SUITES = {"thaumcraft.eldritch.OrphanMobOracleContractTest": "Thaumcraft-1.7.10-4.2.3.5.jar"}


def absent_original_skips(root: Path) -> frozenset[str]:
    """The suites whose skipped cases are accepted: only those whose original JAR is really absent."""
    return frozenset(suite for suite, jar in ORIGINAL_JAR_SUITES.items() if not (root / jar).is_file())


def validate_test_report(path: Path, *, required: list[str] | None = None,
                         fabric_prefix: bool = False, allowed_skips: frozenset[str] = frozenset()) -> dict:
    root = ET.parse(path).getroot()
    cases = list(root.iter("testcase"))
    summary = {"tests": len(cases),
               **{key: sum(1 for _ in root.iter(tag))
                  for key, tag in (("failures", "failure"), ("errors", "error"), ("skipped", "skipped"))},
               "cases": [case.attrib.get("name", "") for case in cases]}
    if not cases or any(not name for name in summary["cases"]):
        raise ValueError(f"Empty or unnamed test cases: {path}")

    def allowed_skipped(node) -> int:
        return sum(1 for case in node.iter("testcase")
                   if case.attrib.get("classname") in allowed_skips and case.find("skipped") is not None)
    for node in root.iter():
        if node.tag in ("testsuite", "testsuites"):
            if "tests" in node.attrib and int(node.attrib["tests"]) != sum(1 for _ in node.iter("testcase")):
                raise ValueError(f"Test count does not match actual cases: {path}")
            for key in ("failures", "errors", "skipped", "disabled"):
                if int(node.attrib.get(key, "0")) != (allowed_skipped(node) if key == "skipped" else 0):
                    raise ValueError(f"Unsuccessful test report ({key}): {path}")
    if summary["failures"] or summary["errors"] or summary["skipped"] != allowed_skipped(root):
        raise ValueError(f"Failed or skipped tests: {path}")
    # JUnit parameterized cases may repeat display names in different classes.
    identities = [(case.attrib.get("classname", ""), case.attrib["name"]) for case in cases]
    if len(identities) != len(set(identities)):
        raise ValueError(f"Duplicate test identity: {path}")
    if required is not None:
        if not required or len(required) != len(set(required)):
            raise ValueError("Required case manifest is empty or has duplicates")
        names = [name.removeprefix("thaumcraftgametests.") if fabric_prefix else name
                 for name in summary["cases"]]
        counts = Counter(names)
        missing = sorted(set(required) - counts.keys())
        if missing:
            raise ValueError(f"Missing required cases in {path}: {', '.join(missing)}")
        if any(counts[name] != 1 for name in required):
            raise ValueError(f"Required case ran more than once: {path}")
        summary["required_cases_passed"] = len(required)
    return summary
