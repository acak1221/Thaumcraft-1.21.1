#!/usr/bin/env python3
"""Mutation check of one content group: apply ONE small wrong edit to the sources, run the group's tests, and see whether they notice.

A group is a JSON file in scripts/mutation/groups/. It names the tests that guard the group ("watch") and lists the mutants. Each
mutant replaces an exact, unique anchor text in one source file. A mutant is *caught* when one of the watched tests fails; unrelated
failing tests (a flaky neighbour) are reported but never count. Tiers:
  junit      only :common:test (filtered by "junit_tests") runs; cheap, batched in one CI job
  gametest   :common:test and the Fabric GameTest server run; one CI job per mutant

Commands (run from anywhere; the repository root is found from this file):
  check GROUP                 every anchor occurs exactly once in the current tree
  list GROUP [--tier TIER]    JSON array of mutant ids (for a CI matrix)
  run GROUP [ID...] [--tier TIER]   run the given mutants (default all); the file is always restored byte for byte
  all GROUP                   run every mutant one after the other (local use)

Exit status of `run`/`all` is 1 when a mutant that is expected to be caught survives, or a mutant does not compile; documented
survivors ("expect": "survives" with a reason) never fail the run. Logs go to .tmp/mutation/ (ignored by git).
"""
import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GROUPS = ROOT / "scripts" / "mutation" / "groups"
OUT = ROOT / ".tmp" / "mutation"
TIMEOUT_SECONDS = 50 * 60


def load(name):
    if not re.fullmatch(r"[a-z0-9-]+", name):
        sys.exit(f"invalid group name: {name!r}")
    group = json.loads((GROUPS / f"{name}.json").read_text(encoding="utf-8"))
    ids = [m["id"] for m in group["mutants"]]
    if len(ids) != len(set(ids)):
        sys.exit("duplicate mutant ids")
    for m in group["mutants"]:
        m.setdefault("tier", "gametest")
        m.setdefault("expect", "caught")
        if m["tier"] not in ("junit", "gametest") or m["expect"] not in ("caught", "survives"):
            sys.exit(f"bad tier/expect in {m['id']}")
        if m["expect"] == "survives" and not m.get("reason"):
            sys.exit(f"{m['id']}: a documented survivor needs a reason")
    return group


def read(path):
    with open(ROOT / path, encoding="utf-8", newline="") as handle:  # Path.read_text(newline=) needs Python 3.13; CI has 3.12
        return handle.read()


def write(path, text):
    with open(ROOT / path, "w", encoding="utf-8", newline="") as handle:
        handle.write(text)


def sha(path):
    return hashlib.sha256((ROOT / path).read_bytes()).hexdigest()


def gradle(tasks, label):
    OUT.mkdir(parents=True, exist_ok=True)
    cmd = ["./gradlew", "--no-daemon", "--console=plain", "--continue", "--gradle-user-home", str(ROOT / ".gradle21")] + tasks
    log = OUT / f"{label}.log"
    with open(log, "w", encoding="utf-8") as out:
        try:
            subprocess.run(cmd, cwd=ROOT, stdout=out, stderr=subprocess.STDOUT, timeout=TIMEOUT_SECONDS)
        except subprocess.TimeoutExpired:
            out.write("\nTIMEOUT\n")
    return log.read_text(encoding="utf-8", errors="replace")


def parse(text):
    junit = sorted(set(f"{cls}.{name}" for cls, name in re.findall(r"^(\w+) > (\w+)\(\) FAILED", text, re.M)))
    gametests = []
    at = text.find("required tests failed")
    if at >= 0:
        gametests = sorted(set(re.findall(r"\(Minecraft\)\s+- (\S+)", text[at:at + 6000])))
    return {
        "junit": junit,
        "gametests": gametests,
        "compile_error": bool(re.search(r"\.java:\d+: error:", text)),
        "detail": re.findall(r"(?:lanes|shafts|lane) failed at tick \d+: ([^\n]{0,160})", text)[:1],
        "timeout": "TIMEOUT" in text[-200:],
    }


def watched(group, parsed):
    junit_classes = set(group["watch"].get("junit", []))
    gametests = {name.lower() for name in group["watch"].get("gametests", [])}
    hits = [t for t in parsed["junit"] if t.split(".")[0] in junit_classes]
    hits += [t for t in parsed["gametests"] if t.split(".")[-1] in gametests]
    return hits


def run_one(group, mutant):
    path = mutant["file"]
    before = sha(path)
    source = read(path)
    if source.count(mutant["old"]) != 1:
        return {"id": mutant["id"], "status": "invalid", "why": f"anchor occurs {source.count(mutant['old'])} times"}
    started = time.time()
    write(path, source.replace(mutant["old"], mutant["new"]))
    try:
        if mutant["tier"] == "junit":
            tasks = [":common:test"]
            for pattern in group.get("junit_tests", []):
                tasks += ["--tests", pattern]
        else:
            tasks = [":common:test", ":fabric:runGameTest"]
        parsed = parse(gradle(tasks, mutant["id"]))
    finally:
        write(path, source)
    hits = watched(group, parsed)
    restored = sha(path) == before
    if parsed["compile_error"] and not parsed["junit"] and not parsed["gametests"]:
        status = "invalid"
    elif hits:
        status = "caught"
    else:
        status = "survived"
    ok = restored and (status == "caught" if mutant["expect"] == "caught" else status in ("caught", "survived"))
    return {
        "id": mutant["id"], "name": mutant.get("name", ""), "tier": mutant["tier"], "status": status, "expect": mutant["expect"],
        "ok": ok, "restored": restored, "hits": hits, "other_failures": sorted(set(parsed["junit"] + parsed["gametests"]) - set(hits)),
        "detail": parsed["detail"], "seconds": round(time.time() - started),
        "reason": mutant.get("reason", ""),
    }


def summary_line(result):
    mark = {"caught": "caught", "survived": "SURVIVED", "invalid": "INVALID"}[result["status"]]
    note = "" if result["ok"] else " <-- unexpected"
    if result["status"] == "survived" and result["expect"] == "survives":
        note = " (documented: " + result["reason"] + ")"
    return f"{result['id']}: {mark}{note}; hits={result['hits']} other={result['other_failures']} {result['seconds']}s"


def command_check(group, _args):
    bad = 0
    for m in group["mutants"]:
        count = read(m["file"]).count(m["old"])
        if count != 1:
            print(f"{m['id']}: anchor occurs {count} times in {m['file']}")
            bad += 1
    print(f"{len(group['mutants'])} mutants, {bad} bad anchors")
    return 1 if bad else 0


def selected(group, args):
    missing = set(args.ids or []) - {m["id"] for m in group["mutants"]}
    if missing:
        sys.exit(f"unknown mutant ids: {sorted(missing)}")
    return [m for m in group["mutants"] if (not args.ids or m["id"] in args.ids) and (not args.tier or m["tier"] == args.tier)]


def command_list(group, args):
    print(json.dumps([m["id"] for m in selected(group, args)]))
    return 0


def command_run(group, args):
    failures = 0
    lines = []
    for mutant in selected(group, args):
        result = run_one(group, mutant)
        OUT.mkdir(parents=True, exist_ok=True)
        (OUT / f"{mutant['id']}.json").write_text(json.dumps(result, indent=1), encoding="utf-8")
        print("VERDICT " + json.dumps(result, ensure_ascii=False), flush=True)
        lines.append(summary_line(result))
        failures += 0 if result["ok"] else 1
    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as out:
            out.write("\n".join(f"- {line}" for line in lines) + "\n")
    print("\n".join(lines))
    print(f"{len(lines)} mutants, {failures} unexpected")
    return 1 if failures else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("command", choices=["check", "list", "run", "all"])
    parser.add_argument("group")
    parser.add_argument("ids", nargs="*")
    parser.add_argument("--tier", choices=["junit", "gametest"])
    args = parser.parse_args()
    group = load(args.group)
    handler = {"check": command_check, "list": command_list, "run": command_run, "all": command_run}[args.command]
    sys.exit(handler(group, args))


if __name__ == "__main__":
    main()
