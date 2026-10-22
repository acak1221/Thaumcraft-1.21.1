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


def merge_base_ledger(branch: str) -> dict | None:
    """The ledger at the merge base of the branch and the current head.

    It tells which side of a conflict carries the newer text of a row: the side
    that changed the row against the base is the newer one.
    """
    base = subprocess.run(["git", "merge-base", "HEAD", branch],
                          capture_output=True, text=True).stdout.strip()
    if not base:
        return None
    return stage(f"{base}:{LEDGER}", LEDGER)


def _union(ours: list | None, theirs: list | None) -> list:
    """The union of two lists, ours first, without duplicates."""
    merged = list(ours or [])
    for item in theirs or []:
        if item not in merged:
            merged.append(item)
    return merged


def _newest(ours: list | None, theirs: list | None, base: list | None) -> list:
    """The newer text of a row, with nothing lost.

    Only the branch changed the row against the merge base: the branch's text is
    the newer one.  Only the merged-in side changed it: that text is newer.  Both
    sides changed it: the two texts are unioned, so that neither the branch's
    new paragraphs nor the merged-in side's are lost.
    """
    ours = list(ours or [])
    theirs = list(theirs or [])
    base = list(base or [])
    if not theirs:
        return ours
    if not ours:
        return theirs
    if theirs == base:
        return ours
    if ours == base:
        return theirs
    return _union(ours, theirs)


def merge_ledger(ours: dict, theirs: dict, base: dict | None = None) -> dict:
    """Union the review verdicts of two ledgers, unit by unit.

    The notes and the evidence of a unit are the audit trail of that unit, and
    they are written by the lane that touched it.  A plain "the other side only
    wins when this side has nothing" rule silently DROPPED the notes and the
    evidence of every row a wave branch had touched, because the audit usually
    already had a note from an earlier wave.  The evidence is therefore unioned,
    and the note of a row is taken from the side that actually CHANGED the row:
    the merge base tells which side is the newer text.
    """
    units = {unit["canonical_path"]: unit for unit in theirs.get("units", [])}
    base_units = {unit["canonical_path"]: unit for unit in (base or {}).get("units", [])}
    for unit in ours.get("units", []):
        path = unit["canonical_path"]
        other = units.get(path)
        if other is None:
            continue
        base = unit.setdefault("review", {})
        other_review = other.get("review", {})
        # The row of the merge base: the text the branch started from.
        base_review = (base_units.get(path) or {}).get("review", {})
        for key, value in other_review.items():
            if key == "evidence":
                # A list: union, never replace, so nothing is lost.
                base[key] = _union(base.get(key), value)
                continue
            if key == "notes":
                # A list of paragraphs: the side that changed the row against the
                # merge base carries the newer text.
                base[key] = _newest(base.get(key), value, base_review.get(key))
                continue
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
    # A unit that only the branch knows is a unit the branch ADDED, so it must
    # survive the merge; the old loop only walked the units of the merged-in
    # side and dropped every new one.
    known = {unit["canonical_path"] for unit in ours.get("units", [])}
    for unit in theirs.get("units", []):
        if unit["canonical_path"] not in known:
            ours.setdefault("units", []).append(unit)
            known.add(unit["canonical_path"])
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
        merged = merge_ledger(ours, theirs, merge_base_ledger(branch))
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
