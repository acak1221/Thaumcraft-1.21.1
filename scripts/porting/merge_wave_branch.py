#!/usr/bin/env python3
"""Merge a wave branch into work, resolving the scope ledger by union.

The agents of a wave each update `docs/porting/scope-ledger.json` (the review
verdicts of their own lane), so merging two of them conflicts on that one file.
The verdicts of a unit are a map, and two agents rarely touch the same unit, so
the conflict is resolved by taking the union of the two verdict maps instead of
picking a side.

Usage:
    merge_wave_branch.py <branch> [<branch> ...]
"""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

LEDGER = Path("docs/porting/scope-ledger.json")


def git(*args: str) -> str:
    return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout


def read_ledger(ref: str) -> dict:
    raw = git("show", f"{ref}:{LEDGER}")
    return json.loads(raw)


def union_verdicts(base: dict, other: dict) -> dict:
    """Take the union of the review verdicts of two ledgers, unit by unit."""
    units = {unit["canonical_path"]: unit for unit in other.get("units", [])}
    merged = 0
    for unit in base.get("units", []):
        other_unit = units.get(unit["canonical_path"])
        if other_unit is None:
            continue
        base_review = unit.setdefault("review", {})
        other_review = other_unit.get("review", {})
        for key, value in other_review.items():
            if key not in base_review:
                base_review[key] = value
                merged += 1
            elif base_review[key] != value and key == "native_coverage":
                # The better coverage wins: parity_verified beats partial beats missing.
                rank = {"missing": 0, "partial": 1, "not_assessed": 1, "parity_verified": 2}
                if rank.get(value, 0) > rank.get(base_review[key], 0):
                    base_review[key] = value
                    merged += 1
    return base, merged


def main() -> int:
    branches = sys.argv[1:]
    if not branches:
        print(__doc__)
        return 2
    for branch in branches:
        print(f"=== merging {branch} ===")
        result = subprocess.run(["git", "merge", "--no-commit", "--no-ff", branch],
                                capture_output=True, text=True)
        if result.returncode != 0:
            conflicted = subprocess.run(["git", "diff", "--name-only", "--diff-filter=U"],
                                       capture_output=True, text=True).stdout.split()
            print("conflicts:", conflicted)
            if LEDGER.as_posix() in conflicted:
                ours = json.loads(LEDGER.read_text(encoding="utf-8"))
                theirs = read_ledger(branch)
                merged, count = union_verdicts(ours, theirs)
                LEDGER.write_text(json.dumps(merged, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
                subprocess.run(["git", "add", str(LEDGER)], check=True)
                print(f"ledger merged by union, {count} verdicts taken from the branch")
                conflicted = [name for name in conflicted if name != LEDGER.as_posix()]
            if conflicted:
                print("UNRESOLVED CONFLICTS:", conflicted)
                return 1
        subprocess.run(["git", "commit", "--no-edit"], capture_output=True, text=True)
        print(git("log", "--oneline", "-1"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
