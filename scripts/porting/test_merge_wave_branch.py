#!/usr/bin/env python3
"""Regression test for the union merge of the scope ledger.

The union merge used to keep the notes and the evidence of a row only when the
merged-in side had none, so every row a wave branch had touched LOST its note
and its evidence: the audit trail of the port silently shrank with every wave.

The merge now unions the evidence and takes the note of the side that changed
the row against the merge base.  This test proves both rules.
"""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import merge_wave_branch  # noqa: E402


def unit(path: str, review: dict) -> dict:
    return {"canonical_path": path, "review": review}


def ledger(rows: list[dict]) -> dict:
    return {"schema_version": 2, "units": rows, "counts": {"original_units": len(rows)}}


class MergeLedgerTest(unittest.TestCase):
    def test_the_evidence_of_the_branch_is_never_dropped(self) -> None:
        ours = ledger([unit("a/A.java", {"native_coverage": "partial", "evidence": ["ours"]})])
        theirs = ledger([unit("a/A.java", {"native_coverage": "partial", "evidence": ["theirs"]})])
        merged = merge_wave_branch.merge_ledger(ours, theirs, ledger([]))
        self.assertEqual(["ours", "theirs"], merged["units"][0]["review"]["evidence"])

    def test_the_note_of_the_branch_survives_when_the_branch_changed_the_row(self) -> None:
        base = ledger([unit("a/A.java", {"native_coverage": "partial", "notes": ["the old note"]})])
        ours = ledger([unit("a/A.java", {"native_coverage": "partial", "notes": ["the old note"]})])
        theirs = ledger([unit("a/A.java", {"native_coverage": "partial", "notes": ["the new note"]})])
        merged = merge_wave_branch.merge_ledger(ours, theirs, base)
        self.assertEqual(["the new note"], merged["units"][0]["review"]["notes"])

    def test_the_note_of_the_merged_side_survives_when_the_branch_did_not_touch_the_row(self) -> None:
        base = ledger([unit("a/A.java", {"native_coverage": "partial", "notes": ["the old note"]})])
        ours = ledger([unit("a/A.java", {"native_coverage": "partial", "notes": ["a newer note of another lane"]})])
        theirs = ledger([unit("a/A.java", {"native_coverage": "partial", "notes": ["the old note"]})])
        merged = merge_wave_branch.merge_ledger(ours, theirs, base)
        self.assertEqual(["a newer note of another lane"], merged["units"][0]["review"]["notes"])

    def test_the_notes_of_both_sides_survive_when_both_changed_the_row(self) -> None:
        base = ledger([unit("a/A.java", {"native_coverage": "partial", "notes": ["the old note"]})])
        ours = ledger([unit("a/A.java", {"native_coverage": "partial", "notes": ["the old note", "ours"]})])
        theirs = ledger([unit("a/A.java", {"native_coverage": "partial", "notes": ["the old note", "theirs"]})])
        merged = merge_wave_branch.merge_ledger(ours, theirs, base)
        self.assertEqual(["the old note", "ours", "theirs"], merged["units"][0]["review"]["notes"])

    def test_the_verdicts_keep_their_rule(self) -> None:
        base = ledger([unit("a/A.java", {"native_coverage": "partial", "disposition": "adapt"})])
        ours = ledger([unit("a/A.java", {"native_coverage": "partial", "disposition": "adapt"})])
        theirs = ledger([unit("a/A.java", {"native_coverage": "parity_verified", "disposition": "port"})])
        merged = merge_wave_branch.merge_ledger(ours, theirs, base)
        review = merged["units"][0]["review"]
        self.assertEqual("parity_verified", review["native_coverage"])
        self.assertEqual("port", review["disposition"])

    def test_a_row_only_the_branch_knows_is_added(self) -> None:
        ours = ledger([unit("a/A.java", {"native_coverage": "partial"})])
        theirs = ledger([unit("a/A.java", {"native_coverage": "partial"}),
                         unit("a/B.java", {"native_coverage": "parity_verified"})])
        merged = merge_wave_branch.merge_ledger(ours, theirs, ledger([]))
        self.assertEqual(2, len(merged["units"]))


if __name__ == "__main__":
    unittest.main()
