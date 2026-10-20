#!/usr/bin/env python3
"""Split the scope ledger into per-family audit worklists and merge authored reviews back.

READ-ONLY helper for the 2026-10-21 full port audit.

  --split <family> [<family> ...]   write the worklist of those families to stdout as JSON
  --merge <file> [...]               merge the authored reviews of those files into the ledger

The review objects follow the schema that `scripts/porting/update_scope_ledger.py`
validates: disposition, source_semantics, callsite_audit, native_coverage, owner,
evidence, notes and reviewed_sha256.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LEDGER = ROOT / "docs/porting/scope-ledger.json"
ORACLE = ROOT.parent / "work/thaumcraft-4.2.3.5-oracle"

ALLOWED = {
    "disposition": {"needs_review", "port", "adapt", "replace", "exclude"},
    "source_semantics": {"unreviewed", "equivalent", "different", "adapted", "not_applicable"},
    "callsite_audit": {"not_started", "partial", "complete"},
    "native_coverage": {"not_assessed", "missing", "partial", "parity_verified"},
}


def split(families: list[str]) -> int:
    data = json.loads(LEDGER.read_text(encoding="utf-8"))
    rows = []
    for unit in data["units"]:
        if unit["queue"]["subsystem_family"] not in families:
            continue
        original = unit.get("original")
        rows.append({
            "canonical_path": unit["canonical_path"],
            "path_relation": unit["path_relation"],
            "family": unit["queue"]["subsystem_family"],
            "milestone": unit["queue"]["planned_milestone"],
            "oracle_path": (ORACLE / original["path"]).as_posix() if original else None,
            "oracle_lines": original["lines"] if original else None,
            "legacy_path": (ROOT / unit["legacy"]["path"]).as_posix() if unit.get("legacy") else None,
            "current_review": unit["review"],
        })
    json.dump({"oracle_root": ORACLE.as_posix(),
               "native_root": ROOT.as_posix(),
               "families": families,
               "rows": rows}, sys.stdout, ensure_ascii=False, indent=1)
    print()
    return 0


def merge(files: list[str]) -> int:
    data = json.loads(LEDGER.read_text(encoding="utf-8"))
    index = {unit["canonical_path"]: unit for unit in data["units"]}
    applied = 0
    unknown: list[str] = []
    for name in files:
        payload = json.loads(Path(name).read_text(encoding="utf-8"))
        for path, review in payload.items():
            unit = index.get(path)
            if unit is None:
                unknown.append(path)
                continue
            for key, values in ALLOWED.items():
                if key not in review or review[key] not in values:
                    raise SystemExit(f"Invalid {key} for {path}: {review.get(key)!r}")
            if not isinstance(review.get("evidence"), list) or not review["evidence"]:
                raise SystemExit(f"Review without evidence: {path}")
            if not isinstance(review.get("notes"), list):
                raise SystemExit(f"Invalid notes for {path}")
            hashes = review.get("reviewed_sha256")
            if not isinstance(hashes, dict) or not hashes:
                raise SystemExit(f"Invalid reviewed_sha256 for {path}")
            for side, value in hashes.items():
                source = unit.get(side)
                if source is None or value != source["sha256_lf"]:
                    raise SystemExit(f"Stale fingerprint for {path} ({side})")
            unit["review"].update(review)
            applied += 1
    LEDGER.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"applied": applied, "unknown": unknown[:10],
                      "unknown_count": len(unknown)}, ensure_ascii=False))
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--split", nargs="+")
    parser.add_argument("--merge", nargs="+")
    args = parser.parse_args()
    if args.split:
        return split(args.split)
    if args.merge:
        return merge(args.merge)
    parser.print_help()
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
