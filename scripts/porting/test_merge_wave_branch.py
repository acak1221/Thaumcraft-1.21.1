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
from types import SimpleNamespace
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

import merge_wave_branch  # noqa: E402


def unit(path: str, review: dict) -> dict:
    return {"canonical_path": path, "review": review}


def ledger(rows: list[dict]) -> dict:
    return {"schema_version": 2, "units": rows, "counts": {"original_units": len(rows)}}


class MergeLedgerTest(unittest.TestCase):
    def test_merge_base_lookup_reads_the_ledger_once_at_the_commit(self) -> None:
        base = ledger([unit("a/A.java", {"notes": ["the old note"]})])
        commit = "a" * 40
        def git_read(command, **kwargs):
            if command == ["git", "merge-base", "HEAD", "port/review"]:
                return SimpleNamespace(stdout=commit + "\n", returncode=0)
            if command == ["git", "show", f"{commit}:{merge_wave_branch.LEDGER}"]:
                return SimpleNamespace(stdout=json.dumps(base), returncode=0)
            return SimpleNamespace(stdout="", returncode=128)
        with patch.object(merge_wave_branch.subprocess, "run", side_effect=git_read):
            self.assertEqual(base, merge_wave_branch.merge_base_ledger("port/review"))

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


class MergeWaveCliReviewTest(unittest.TestCase):
    def invoke(self, *, dirty="", merge_code=0, conflicts="", merge_head=True):
        self.commands = []

        def run(command, **kwargs):
            self.commands.append(command)
            code, stdout, stderr = 0, "", ""
            if command == ["git", "status", "--porcelain"]:
                stdout = dirty
            elif command[:2] == ["git", "merge"]:
                code = merge_code
                stderr = "merge refused" if code else ""
            elif command[:2] == ["git", "diff"]:
                stdout = conflicts
            elif command[:2] == ["git", "rev-parse"]:
                code = 0 if merge_head else 1
                stdout = "a" * 40 if merge_head else ""
            elif command[:2] == ["git", "log"]:
                stdout = "accepted merge\n"
            return SimpleNamespace(returncode=code, stdout=stdout, stderr=stderr)

        with patch.object(sys, "argv", ["merge_wave_branch.py", "port/review"]), \
                patch.object(merge_wave_branch.subprocess, "run", side_effect=run), \
                patch("builtins.print"):
            return merge_wave_branch.main()

    def assert_no_commit(self):
        self.assertFalse(any(command[:2] == ["git", "commit"] for command in self.commands),
                         "an unsuccessful merge must never commit the caller's index")

    def test_failed_merge_stops_before_any_commit(self):
        self.assertEqual(1, self.invoke(merge_code=128, merge_head=False))
        self.assert_no_commit()

    def test_dirty_worktree_is_refused_before_starting_a_merge(self):
        self.assertEqual(1, self.invoke(dirty="M  existing-user-change.java\n"))
        self.assertFalse(any(command[:2] == ["git", "merge"] for command in self.commands))
        self.assert_no_commit()

    def test_unresolved_source_conflict_is_left_for_the_caller(self):
        self.assertEqual(1, self.invoke(merge_code=1, conflicts="src/Unresolved.java\n"))
        self.assert_no_commit()

    def test_already_merged_branch_does_not_create_an_unrelated_commit(self):
        self.assertEqual(0, self.invoke(merge_head=False))
        self.assert_no_commit()

    def test_clean_successful_merge_can_still_finish(self):
        self.assertEqual(0, self.invoke())
        self.assertEqual(1, sum(command[:2] == ["git", "commit"] for command in self.commands))

    def test_resolved_bookkeeping_conflict_can_still_finish(self):
        with patch.object(merge_wave_branch, "resolve_json_conflict", return_value=True) as resolve:
            self.assertEqual(0, self.invoke(merge_code=1, conflicts="docs/porting/scope-ledger.json\n"))
        resolve.assert_called_once_with(merge_wave_branch.LEDGER, "port/review")
        self.assertEqual(1, sum(command[:2] == ["git", "commit"] for command in self.commands))


if __name__ == "__main__":
    unittest.main()
