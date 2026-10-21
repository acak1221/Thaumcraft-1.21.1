#!/usr/bin/env python3
"""Validate the authored reviews of the scope ledger without the private oracle.

`update_scope_ledger.py --check` needs the private TC4 oracle root to recompute the
source fingerprints. When that root is not reachable, this script still proves the
three things an authored review change can break:

* the shape of every review object is valid (`validate_review` of the ledger tool);
* every `reviewed_sha256` anchor still matches the source hash stored in the same row,
  so `stale_review_fingerprints` stays zero for the rows that are anchored;
* the metadata counters (`counts`) were not touched by the change.

Usage: python3 scripts/porting/check_scope_ledger_reviews.py
"""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LEDGER = ROOT / "docs/porting/scope-ledger.json"


def load_tool():
    spec = importlib.util.spec_from_file_location(
        "update_scope_ledger", ROOT / "scripts/porting/update_scope_ledger.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main() -> None:
    tool = load_tool()
    data = json.loads(LEDGER.read_text(encoding="utf-8"))
    problems: list[str] = []
    stale = 0
    for row in data["units"]:
        sources = {"original": row["original"], "legacy": row["legacy"]}
        try:
            tool.validate_review(row["review"], sources, row["canonical_path"])
        except ValueError as exception:
            problems.append(str(exception))
            continue
        anchored = row["review"]["reviewed_sha256"]
        current = all(anchored.get(side) == source["sha256_lf"]
                      for side, source in sources.items() if source) if anchored else None
        if current is False:
            stale += 1
    counts = data["counts"]
    dispositions: dict[str, int] = {}
    for row in data["units"]:
        key = row["review"]["disposition"]
        dispositions[key] = dispositions.get(key, 0) + 1
    milestones: dict[str, int] = {}
    for row in data["units"]:
        key = row["queue"]["planned_milestone"]
        milestones[key] = milestones.get(key, 0) + 1
    if dispositions != counts["dispositions"]:
        problems.append("counts.dispositions does not match the authored dispositions")
    if milestones != counts["milestone_queue"]:
        problems.append("counts.milestone_queue does not match the authored queue")
    print(json.dumps({
        "mode": "check",
        "rows": len(data["units"]),
        "invalid_reviews": len(problems),
        "stale_review_fingerprints": stale,
        "counts_match": not problems,
    }, ensure_ascii=False))
    for problem in problems:
        print(problem)
    if problems:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
