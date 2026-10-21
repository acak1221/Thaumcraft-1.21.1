#!/usr/bin/env python3
"""Merge a wave branch into work, resolving the JSON bookkeeping files by union.

The agents of a wave each update the two bookkeeping files of the port
(`docs/porting/scope-ledger.json` with the review verdicts and
`docs/porting/class-inventory.json` with the class inventory), so merging two
of them conflicts on those files. Both are JSON, and the agents touch different
entries, so the conflict is resolved by taking the union of the two documents
instead of picking a side.

For the ledger the union is done unit by unit on the review verdicts, and the
better coverage wins when both sides touched the same unit.

Usage:
    merge_wave_branch.py <branch> [<branch> ...]
"""
from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path

LEDGER = Path("docs/porting/scope-ledger.json")
INVENTORY = Path("docs/porting/class-inventory.json")
CONFLICT = re.compile(r"<<<<<<< [^\n]*\n(.*?)\n=======\n(.*?)\n>>>>>>> [^\n]*\n", re.S)
COVERAGE_RANK = {"missing": 0, "not_assessed": 1, "partial": 1, "parity_verified": 2}


def git(*args: str) -> str:
    return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout


def stage(ref: str, path: Path) -> dict | None:
    raw = subprocess.run(["git", "show", f"{ref}:{path}"], capture_output=True, text=True).stdout
    if not raw:
        return None
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return None


def merge_ledger(ours: dict, theirs: dict) -> dict:
    """Union the review verdicts of two ledgers, unit by unit."""
    units = {unit["canonical_path"]: unit for unit in theirs.get("units", [])}
    for unit in ours.get("units", []):
        other = units.get(unit["canonical_path"])
        if other is None:
            continue
        base = unit.setdefault("review", {})
        for key, value in other.get("review", {}).items():
            if key not in base:
                base[key] = value
            elif base[key] != value and key == "native_coverage":
                if COVERAGE_RANK.get(value, 0) > COVERAGE_RANK.get(base[key], 0):
                    base[key] = value
            elif base[key] != value and key in ("disposition", "source_semantics", "callsite_audit"):
                # A branch that closed a unit replaces the open verdict of the audit with its own.
                OPEN = (None, "needs_review", "unreviewed", "partial")
                if base[key] in OPEN or value in ("port", "replace", "adapt"):
                    base[key] = value
    for key, value in theirs.items():
        if key != "units" and key not in ours:
            ours[key] = value
    return ours


def merge_inventory(ours: dict, theirs: dict) -> dict:
    """Union the class inventory of two sides, entry by entry."""
    for key, value in theirs.items():
        if key not in ours:
            ours[key] = value
            continue
        if isinstance(ours[key], list) and isinstance(value, list):
            seen = {json.dumps(item, sort_keys=True, ensure_ascii=False) for item in ours[key]}
            for item in value:
                marker = json.dumps(item, sort_keys=True, ensure_ascii=False)
                if marker not in seen:
                    ours[key].append(item)
                    seen.add(marker)
        elif isinstance(ours[key], dict) and isinstance(value, dict):
            ours[key] = merge_inventory(ours[key], value)
        else:
            ours[key] = value
    return ours


def resolve_json_conflict(path: Path, branch: str) -> bool:
    """Resolve a JSON conflict by the union of the two sides.

    The two sides are the two stages of the conflict, so the whole documents are
    merged, not the conflict fragments.
    """
    ours = stage(":2", path)
    theirs = stage(":3", path)
    if ours is None or theirs is None:
        return False
    if path == LEDGER:
        merged = merge_ledger(ours, theirs)
    elif path == INVENTORY:
        merged = merge_inventory(ours, theirs)
    else:
        merged = {**ours, **theirs}
    path.write_text(json.dumps(merged, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    subprocess.run(["git", "add", str(path)], check=True)
    print(f"{path}: resolved by union")
    return True


def main() -> int:
    branches = sys.argv[1:]
    if not branches:
        print(__doc__)
        return 2
    for branch in branches:
        print(f"=== merging {branch} ===")
        subprocess.run(["git", "merge", "--no-commit", "--no-ff", branch], capture_output=True, text=True)
        conflicted = subprocess.run(["git", "diff", "--name-only", "--diff-filter=U"],
                                   capture_output=True, text=True).stdout.split()
        for name in list(conflicted):
            path = Path(name)
            if path.suffix == ".json" and resolve_json_conflict(path, branch):
                conflicted.remove(name)
            elif path.suffix == ".json":
                ours = stage(":2", path)
                theirs = stage(":3", path)
                if ours is not None and theirs is not None:
                    merged = merge_inventory(ours, theirs) if path == INVENTORY else {**ours, **theirs}
                    path.write_text(json.dumps(merged, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
                    subprocess.run(["git", "add", str(path)], check=True)
                    conflicted.remove(name)
                    print(f"{path}: resolved by the union of the two sides")
        if conflicted:
            print("UNRESOLVED CONFLICTS:", conflicted)
            return 1
        result = subprocess.run(["git", "commit", "--no-edit"], capture_output=True, text=True)
        if result.returncode != 0:
            print(result.stdout, result.stderr)
            return 1
        print(git("log", "--oneline", "-1").strip())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
