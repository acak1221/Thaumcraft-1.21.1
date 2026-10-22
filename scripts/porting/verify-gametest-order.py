#!/usr/bin/env python3
"""Fresh-world full/isolated acceptance. --all checks EVERY native full-run wrapper alone.

The native ownership guard detects leaks; paired execution also detects assumptions
about preceding scenarios and wall-clock scheduling that no leak snapshot can prove.
Run serially with JAVA_HOME and a private GRADLE_USER_HOME already exported.
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--all", action="store_true", help="isolate every scenario in the fresh native full report (long-running)")
    parser.add_argument("--case", action="append", help="additional/replacement isolated selector")
    parser.add_argument("--heap", help="optional GameTest heap override, e.g. 2600M on a memory-contended host")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    if not os.environ.get("GRADLE_USER_HOME") or not os.environ.get("JAVA_HOME"):
        parser.error("export JAVA_HOME and a private GRADLE_USER_HOME first")
    cases = args.case or [
        "waterUpgradeWidensTheLiveWorkRadiusOfRunningLumberAndHarvestGolems",
        "infusionComponentPredicateSession",
        "advancedFurnacePreservesCustodyAcrossRealChunkUnload",
        "embersFlyDamageIgniteAndExpire",
    ]
    output = root / ".tmp/order-verification"
    output.mkdir(parents=True, exist_ok=True)
    (output / "summary.json").unlink(missing_ok=True)
    summary = []
    pending = [None] if args.all else [None, *cases]
    while pending:
        selector = pending.pop(0)
        label = selector or "full"
        # This is deliberately the sole disposable GameTest world, never a user's save.
        world = root / "fabric/run-gametest/world"
        if world.exists():
            shutil.rmtree(world)
        xml = root / "fabric/build" / ("gametest-focused-results.xml" if selector else "gametest-results.xml")
        xml.unlink(missing_ok=True)
        (output / (label + ".xml")).unlink(missing_ok=True)
        command = [str(root / "gradlew"), ":fabric:runGameTest", "--no-daemon", "-Dorg.gradle.jvmargs=-Xmx768m"]
        if selector:
            command.append("-PgameTestFilter=" + selector)
        if args.heap:
            command.append("-PgameTestHeap=" + args.heap)
        print("Running", label, flush=True)
        with (output / (label + ".log")).open("w") as log:
            result = subprocess.run(command, cwd=root, stdout=log, stderr=subprocess.STDOUT)
        if not xml.exists():
            raise SystemExit(f"{label}: exit {result.returncode}; no fresh XML; see {output}")
        shutil.copyfile(xml, output / (label + ".xml"))
        tests = ET.parse(xml).findall(".//testcase")
        failures = [t.attrib.get("name") for t in tests if t.find("failure") is not None or t.find("error") is not None]
        if selector and (len(tests) != 1 or not tests[0].attrib.get("name", "").lower().endswith(selector.lower())):
            failures.append("isolated selector did not execute exactly the requested scenario")
        summary.append({"case": label, "exit": result.returncode, "tests": len(tests), "failures": failures})
        (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
        if result.returncode or failures or (selector and len(tests) != 1) or not tests:
            raise SystemExit(f"{label}: FAILED; see {output}")
        if selector is None and args.all:
            # Inventory the actual native run, not a hand-maintained manifest:
            # that manifest can lag newly authored wrappers (317 vs356 at this base).
            names = {test.attrib.get("name", "") for test in tests}
            if not all(names):
                raise SystemExit("full report contains an unnamed scenario")
            pending.extend(sorted(names))
        print(f"{label}: {len(tests)} passed", flush=True)


if __name__ == "__main__":
    main()
