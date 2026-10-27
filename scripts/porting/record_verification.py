"""Record actual local build results and artifact hashes; never infer full parity."""
from __future__ import annotations

import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import json
import re
from pathlib import Path
import subprocess
from zipfile import ZipFile
import checkpoint_evidence
import convert_assets

ROOT = Path(__file__).resolve().parents[2]


def canonical_notice_bytes(path: Path) -> bytes:
    text = path.read_bytes().decode("utf-8").replace("\r\n", "\n").replace("\r", "\n")
    if not text.endswith("\n"):
        text += "\n"
    return text.encode("utf-8")


def validate_packaged_notices(archive: ZipFile) -> dict[str, str]:
    """Require one current, portable copy of every notice named by the artifact."""
    hashes = {}
    for name in ("THIRD_PARTY_NOTICES.md", "LICENSE"):
        if archive.namelist().count(name) != 1:
            raise ValueError(f"Artifact must contain exactly one {name}")
        expected = canonical_notice_bytes(ROOT / name)
        if archive.read(name) != expected:
            raise ValueError(f"Artifact has stale or noncanonical {name}")
        hashes[name] = hashlib.sha256(expected).hexdigest()
    return hashes


def validate_packaged_isolation(archive: ZipFile) -> dict[str, bool]:
    """A production JAR must not ship fixtures, harnesses or links to them."""
    references = (b"thaumcraft/common/testing/", b"thaumcraft/fabric/testing/",
                  b"thaumcraft/neoforge/testing/", b"thaumcraft/fabric/clienttest/",
                  b"thaumcraft/neoforge/clienttest/", b"thaumcraft/clienttest/",
                  b"net/minecraft/gametest/", b"net/fabricmc/fabric/api/gametest/",
                  b"net/neoforged/neoforge/event/RegisterGameTestsEvent")
    for name in archive.namelist():
        lower = name.lower()
        if (lower.startswith("thaumcraft/") and any(
                marker in lower for marker in ("/testing/", "/gametest/", "/clienttest/"))
                or lower in ("data/thaumcraft/structure/empty.nbt", "data/thaumcraft/structures/empty.nbt")
                or lower.startswith(("data/thaumcraft_gametest/", "assets/thaumcraft_gametest/"))):
            raise ValueError(f"Test support leaked into production artifact: {name}")
        if name.startswith("thaumcraft/") and name.endswith(".class"):
            content = archive.read(name)
            if any(reference in content for reference in references):
                raise ValueError(f"Production class references a test harness: {name}")
    if "fabric.mod.json" in archive.namelist():
        metadata = json.loads(archive.read("fabric.mod.json"))
        if "fabric-gametest" in metadata.get("entrypoints", {}):
            raise ValueError("Production Fabric metadata contains fabric-gametest entrypoint")
    for name in ("fabric.mod.json", "META-INF/neoforge.mods.toml"):
        if name in archive.namelist() and any(marker in archive.read(name).lower()
                                               for marker in (b"thaumcraft_gametest", b"thaumcraft_client_smoke")):
            raise ValueError(f"Test mod metadata leaked into production artifact: {name}")
    return {"test_classes_absent": True, "test_entrypoints_absent": True,
            "test_fixtures_absent": True, "production_test_references_absent": True}


def client_evidence(relative: str) -> dict:
    path = ROOT / relative
    if not path.is_file():
        return {"startup_seen": False, "log": relative, "visual_parity_validated": False}
    text = path.read_text(encoding="utf-8", errors="replace")
    atlases = re.findall(r"Created: (\d+x\d+x\d+) minecraft:textures/atlas/blocks.png-atlas", text)
    resource_errors = re.findall(r"Missing textures in model thaumcraft:|Invalid sounds.json|Failed to parse atlas definition", text)
    return {"startup_seen": bool(atlases) and "Sound engine started" in text,
            "log": relative, "blocks_atlas": atlases[-1] if atlases else None,
            "resource_errors": len(resource_errors), "visual_parity_validated": False,
            "integrated_world_login_seen_in_log": "Starting integrated minecraft server" in text and "joined the game" in text,
            "scan_feedback_seen_in_log": "[CHAT] To understand this" in text}



def unittest_summary(log: str) -> tuple[int, int]:
    """(tests run, tests skipped) of a successful unittest log.

    Tests that need the original JARs, which are not in the repository, skip themselves, so a successful log
    ends in "OK" or "OK (skipped=N)". Anything else is not a successful run.
    """
    ran = re.search(r"Ran (\d+) tests? in", log)
    result = re.search(r"^OK(?: \(skipped=(\d+)\))?\s*$", log, re.MULTILINE)
    if not ran or not result or "FAILED" in log:
        raise SystemExit("Asset test log does not show a successful unittest run")
    return int(ran.group(1)), int(result.group(1) or 0)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--loaders", nargs="+", choices=("fabric", "neoforge"), default=["fabric"],
                        help="Loaders actually built/run for this checkpoint; Fabric-only by current user priority")
    parser.add_argument("--begin-run", type=Path,
                        help="Before validation, create a new input fingerprint snapshot; use --rerun-tasks")
    parser.add_argument("--run-snapshot", type=Path,
                        help="Snapshot from --begin-run; all XML/JARs must be newer and inputs unchanged")
    parser.add_argument("--check-notices", "--check-artifacts", action="store_true",
                        help="Check selected loader JAR notices and test isolation without recording runtime results")
    parser.add_argument("--build-exit-json", type=Path)
    parser.add_argument("--build-log")
    parser.add_argument("--asset-test-log", type=Path)
    parser.add_argument("--fabric-client-log", default=".tmp/modern-client3.log")
    parser.add_argument("--client-smoke-verification", type=Path,
                        help="Separately authored screenshot inspection evidence; never inferred from script success")
    args = parser.parse_args()
    args.loaders = list(dict.fromkeys(args.loaders))
    if args.begin_run:
        checkpoint_evidence.begin_run(ROOT, args.begin_run)
        print(f"Validation input snapshot: {args.begin_run}")
        return
    version = re.search(r"^mod_version=(.+)$", (ROOT / "gradle.properties").read_text(encoding="utf-8"), re.MULTILINE)
    if not version:
        parser.error("gradle.properties is missing mod_version")
    mod_version = version.group(1).strip()
    if args.check_notices:
        results = {}
        for module in args.loaders:
            jar = ROOT / module / f"build/libs/Thaumcraft-{module}-{mod_version}.jar"
            with ZipFile(jar) as archive:
                results[jar.relative_to(ROOT).as_posix()] = validate_packaged_notices(archive)
                validate_packaged_isolation(archive)
        print(json.dumps({"notice_sha256": results}, sort_keys=True))
        return
    if not (args.build_exit_json and args.build_log and args.asset_test_log and args.run_snapshot):
        parser.error("Recording requires --build-exit-json, --build-log, --asset-test-log and --run-snapshot")
    snapshot = json.loads(args.run_snapshot.read_text(encoding="utf-8"))
    started_ns = checkpoint_evidence.validate_snapshot(ROOT, snapshot)
    output_evidence = {}
    def checked_output(path: Path) -> None:
        output_evidence[path.relative_to(ROOT).as_posix()] = checkpoint_evidence.fresh_output(path, started_ns)
    checked_output(args.build_exit_json.resolve())
    checked_output((ROOT / args.build_log).resolve())
    checked_output(args.asset_test_log.resolve())
    exit_record = json.loads(args.build_exit_json.read_text(encoding="utf-8"))
    if exit_record["code"] != 0:
        raise SystemExit("The supplied build did not succeed")
    asset_ran, asset_skipped = unittest_summary(args.asset_test_log.read_text(encoding="utf-8", errors="replace"))
    build_log = (ROOT / args.build_log).read_text(encoding="utf-8", errors="replace")
    if "BUILD SUCCESSFUL" not in build_log or "BUILD FAILED" in build_log:
        raise SystemExit("Build log does not show successful Gradle validation")
    registry_checks = re.findall(r"registry validation: (\d+) accepted, (\d+) missing item ids, (\d+) missing block ids, (\d+) conflicts", build_log)
    if not registry_checks:
        raise SystemExit("Build log does not contain native registry validation")
    accepted, missing_items, missing_blocks, conflicts = map(int, registry_checks[-1])
    suites = []
    for path in checkpoint_evidence.junit_reports(ROOT):
        checked_output(path)
        suites.append(checkpoint_evidence.validate_test_report(path))
    if not suites:
        raise SystemExit("JUnit results are absent")
    junit = {key: sum(s[key] for s in suites)
             for key in ("tests", "failures", "errors", "skipped")}
    if junit["failures"] or junit["errors"]:
        raise SystemExit("JUnit reports failing tests")
    gametests, artifacts = {}, []
    required = json.loads((ROOT / "scripts/porting/required-gametests.json").read_text(encoding="utf-8"))
    if required.get("schema_version") != 1:
        raise SystemExit("Unknown required GameTest manifest schema")
    for module in args.loaders:
        xml = ROOT / module / "build/gametest-results.xml"
        checked_output(xml)
        summary = checkpoint_evidence.validate_test_report(
            xml, required=required["cases"], fabric_prefix=module == "fabric")
        summary["result_file"] = xml.relative_to(ROOT).as_posix()
        gametests[module] = summary
        jar = ROOT / module / f"build/libs/Thaumcraft-{module}-{mod_version}.jar"
        checked_output(jar)
        with ZipFile(jar) as archive:
            notices = validate_packaged_notices(archive)
            isolation = validate_packaged_isolation(archive)
            classes = [name for name in archive.namelist() if name.endswith(".class")]
            if any(name.startswith(("net/minecraft/", "cpw/mods/", "net/minecraftforge/")) for name in classes):
                raise SystemExit("Unexpected game/legacy classes in artifact")
            if any(name.endswith(".jar") for name in archive.namelist()):
                raise SystemExit("Unexpected nested JAR in artifact")
            versions = {name: int.from_bytes(archive.read(name)[6:8], "big") for name in classes}
            if any(version != 65 for name, version in versions.items() if name.startswith("thaumcraft/")):
                raise SystemExit("Own code is not Java 21 bytecode")
            if "thaumcraft-common.mixins.json" not in archive.namelist():
                raise SystemExit("Missing player knowledge mixin metadata")
            artifacts.append({"path": jar.relative_to(ROOT).as_posix(), "bytes": jar.stat().st_size,
                              "sha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
                              "class_files": len(classes), "classfile_major_counts": dict(Counter(versions.values())),
                               "own_code_classfile_major": 65, "minecraft_classes_bundled": False, "nested_jars": False,
                               "notice_sha256": notices, "test_isolation": isolation})
    report = {"schema_version": 2, "captured_utc": datetime.now(timezone.utc).isoformat(),
              "mod_version": mod_version, "target": "Minecraft 1.21.1 / Java 21",
              "branch": subprocess.check_output(["git", "-C", str(ROOT), "branch", "--show-current"], text=True).strip(),
              "scope": "Native foundation checkpoint; full TC4 gameplay parity remains pending",
              "loaders_checked": args.loaders,
              "loaders_not_checked": [module for module in ("fabric", "neoforge") if module not in args.loaders],
              "build": {"entrypoint": "Gradle wrapper validation (see log for exact tasks)",
                        "exit_code": 0, "log": args.build_log,
                        "started_ns": started_ns, "inputs_unchanged": True,
                        "input_files": len(snapshot["source_fingerprints_sha256"]),
                        "source_fingerprints_sha256": snapshot["source_fingerprints_sha256"],
                        "outputs": output_evidence},
              "junit": junit, "gametests": gametests, "artifacts": artifacts,
              "assets": {"python_tests_passed": asset_ran - asset_skipped, "python_tests_skipped": asset_skipped,
                         "test_log": args.asset_test_log.as_posix(),
                         "references": convert_assets.validate_resources(), "source_revision": convert_assets.source_revision()},
              "native_registry_bindings": {"accepted": accepted, "missing_items": missing_items,
                                           "missing_blocks": missing_blocks, "conflicts": conflicts},
              "clients": {module: client_evidence(args.fabric_client_log if module == "fabric" else ".tmp/modern-neo-client1.log")
                          for module in args.loaders},
              "limitations": ["Only loaders_checked belong to this checkpoint; older unselected loader results/artifacts are not reused",
                              "Full gameplay systems pending; see MIGRATION_STATUS.md",
                              "Client model/GUI parity is not established",
                              "Windows Architectury dev transformer logs a caught path-separator exception",
                              "Development GameTests do not replace ordinary packaged runtime validation",
                              "CI configuration reviewed; remote workflow has not run",
                              "Knowledge full snapshots do not yet split histories above the native payload limit"]}
    if args.client_smoke_verification:
        report["client_smoke"] = json.loads(args.client_smoke_verification.read_text(encoding="utf-8"))
        fingerprints = report["client_smoke"].get("source_fingerprints_lf", {})
        matches = bool(fingerprints) and all(
            (ROOT / name).is_file() and hashlib.sha256(
                (ROOT / name).read_bytes().replace(b"\r\n", b"\n")).hexdigest() == expected
            for name, expected in fingerprints.items())
        report["client_smoke"]["matches_current_recorded_inputs"] = matches
        report["client_smoke"]["historical_evidence_only"] = not matches
    checkpoint_evidence.validate_snapshot(ROOT, snapshot)
    target = ROOT / "docs/porting/verification.json"
    target.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps({"junit": junit, "gametests": {key: value["tests"] for key, value in gametests.items()},
                      "artifact_count": len(artifacts), "output": target.relative_to(ROOT).as_posix()}))


if __name__ == "__main__":
    main()
