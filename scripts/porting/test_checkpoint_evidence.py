"""Reject stale, missing, skipped or substituted checkpoint evidence."""
from __future__ import annotations

import json
import os
from pathlib import Path
import tempfile
import time
import unittest

import checkpoint_evidence as evidence


class CheckpointEvidenceTest(unittest.TestCase):
    def setUp(self) -> None:
        temporary_root = Path(__file__).resolve().parents[2] / ".tmp"
        temporary_root.mkdir(exist_ok=True)
        self.temporary = tempfile.TemporaryDirectory(dir=temporary_root)
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.write("common/src/main/java/Example.java", "class Example {}\n")

    def write(self, relative: str, content: str) -> Path:
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        return path

    def snapshot(self) -> dict:
        return evidence.begin_run(self.root, self.root / "snapshot.json")

    def test_all_source_sets_resources_and_build_inputs_are_hashed(self) -> None:
        for name in ("common/src/gameTest/java/Test.java", "fabric/src/clientTest/java/Client.java",
                     "neoforge/src/gameTest/resources/META-INF/neoforge.mods.toml",
                     "common/src/main/resources/assets/texture.png", "scripts/porting/check.py",
                     "gradle/wrapper/gradle-wrapper.properties", "fabric/build.gradle"):
            self.write(name, "input")
        self.write("common/build/classes/Generated.class", "generated")
        self.write("fabric/run/saves/World/level.dat", "world")
        self.write("scripts/__pycache__/cache.pyc", "cache")
        fingerprints = evidence.source_fingerprints(self.root)
        self.assertEqual(len(fingerprints), 8)
        self.assertFalse(any("build/classes" in path or "/saves/" in path or "__pycache__" in path
                             for path in fingerprints))

    def test_unchanged_dirty_inputs_are_accepted_without_git(self) -> None:
        snapshot = self.snapshot()
        self.assertEqual(evidence.validate_snapshot(self.root, snapshot), snapshot["started_ns"])
        self.assertEqual(json.loads((self.root / "snapshot.json").read_text()), snapshot)

    def test_snapshot_is_not_overwritten(self) -> None:
        self.snapshot()
        with self.assertRaises(FileExistsError):
            self.snapshot()

    def test_changed_added_or_deleted_input_is_rejected(self) -> None:
        snapshot = self.snapshot()
        original = self.root / "common/src/main/java/Example.java"
        original.write_text("changed", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "Inputs changed"):
            evidence.validate_snapshot(self.root, snapshot)
        original.write_text("class Example {}\n", encoding="utf-8")
        added = self.write("common/src/gameTest/java/New.java", "new")
        with self.assertRaisesRegex(ValueError, "New.java"):
            evidence.validate_snapshot(self.root, snapshot)
        added.unlink()
        original.unlink()
        with self.assertRaisesRegex(ValueError, "Example.java"):
            evidence.validate_snapshot(self.root, snapshot)

    def test_stale_empty_missing_outputs_are_rejected(self) -> None:
        path = self.write("output.xml", "old")
        now = time.time_ns()
        os.utime(path, ns=(now - 10_000_000_000, now - 10_000_000_000))
        with self.assertRaisesRegex(ValueError, "Stale"):
            evidence.fresh_output(path, now)
        path.write_text("", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "Empty"):
            evidence.fresh_output(path, now)
        path.unlink()
        with self.assertRaisesRegex(ValueError, "Missing"):
            evidence.fresh_output(path, now)

    def test_fresh_output_is_fingerprinted(self) -> None:
        started = time.time_ns() - 1_000_000_000
        path = self.write("output.xml", "new")
        self.assertEqual(evidence.fresh_output(path, started)["sha256"], evidence.sha256(path))

    def report(self, xml: str, **kwargs) -> dict:
        return evidence.validate_test_report(self.write("tests.xml", xml), **kwargs)

    def test_required_cases_are_checked_for_each_loader_spelling(self) -> None:
        for prefix in ("", "thaumcraftgametests."):
            result = self.report(f'<testsuite><testcase name="{prefix}required"/></testsuite>',
                                 required=["required"], fabric_prefix=bool(prefix))
            self.assertEqual(result["required_cases_passed"], 1)
            self.assertEqual(result["skipped"], 0)

    def test_substitute_test_does_not_compensate_for_missing_case(self) -> None:
        with self.assertRaisesRegex(ValueError, "Missing required.*original"):
            self.report('<testsuite><testcase name="substitute"/></testsuite>', required=["original"])

    def test_failure_error_skip_and_disabled_summaries_are_rejected(self) -> None:
        for key in ("failures", "errors", "skipped", "disabled"):
            with self.subTest(key=key), self.assertRaisesRegex(ValueError, "Unsuccessful"):
                self.report(f'<testsuite {key}="1"><testcase name="test"/></testsuite>')
        for tag in ("failure", "error", "skipped"):
            with self.subTest(tag=tag), self.assertRaisesRegex(ValueError, "Failed or skipped"):
                self.report(f'<testsuite><testcase name="test"><{tag}/></testcase></testsuite>')

    def test_only_a_suite_whose_original_jar_is_absent_may_skip(self) -> None:
        suite = "thaumcraft.eldritch.OrphanMobOracleContractTest"
        allowed = frozenset({suite})
        skipped = (f'<testsuite tests="2" skipped="2"><testcase classname="{suite}" name="a"><skipped/></testcase>'
                   f'<testcase classname="{suite}" name="b"><skipped/></testcase></testsuite>')
        self.assertEqual(self.report(skipped, allowed_skips=allowed)["skipped"], 2)
        with self.assertRaisesRegex(ValueError, "Unsuccessful"):
            self.report(skipped)
        with self.assertRaisesRegex(ValueError, "Unsuccessful"):
            self.report('<testsuite skipped="1"><testcase classname="demo.Other" name="a"><skipped/></testcase></testsuite>',
                        allowed_skips=allowed)
        with self.assertRaisesRegex(ValueError, "Failed or skipped"):
            self.report('<testsuite><testcase classname="demo.Other" name="a"><skipped/></testcase></testsuite>',
                        allowed_skips=allowed)
        with self.assertRaisesRegex(ValueError, "Unsuccessful"):
            self.report(f'<testsuite skipped="2"><testcase classname="{suite}" name="a"><skipped/></testcase></testsuite>',
                        allowed_skips=allowed)

    def test_skips_are_allowed_only_while_the_original_jar_is_absent(self) -> None:
        self.assertEqual(evidence.absent_original_skips(self.root), frozenset(evidence.ORIGINAL_JAR_SUITES))
        for jar in set(evidence.ORIGINAL_JAR_SUITES.values()):
            self.write(jar, "jar")
        self.assertEqual(evidence.absent_original_skips(self.root), frozenset())

    def test_every_listed_suite_skips_only_on_its_missing_jar(self) -> None:
        tests = Path(__file__).resolve().parents[2] / "common/src/test/java"
        for suite, jar in evidence.ORIGINAL_JAR_SUITES.items():
            source = (tests / (suite.replace(".", "/") + ".java")).read_text(encoding="utf-8")
            self.assertIn(jar, source, suite)
            self.assertIn("assumeTrue(Files.isRegularFile(path)", source, suite)

    def test_empty_unnamed_and_duplicate_cases_are_rejected(self) -> None:
        for xml in ('<testsuite/>', '<testsuite><testcase/></testsuite>',
                    '<testsuite><testcase name="one"/><testcase name="one"/></testsuite>'):
            with self.subTest(xml=xml), self.assertRaises(ValueError):
                self.report(xml)

    def test_same_junit_display_name_in_different_classes_is_allowed(self) -> None:
        result = self.report('<testsuite><testcase classname="A" name="same"/>'
                             '<testcase classname="B" name="same"/></testsuite>')
        self.assertEqual(result["tests"], 2)

    def test_report_count_cannot_hide_missing_cases(self) -> None:
        with self.assertRaisesRegex(ValueError, "count does not match"):
            self.report('<testsuite tests="41"><testcase name="only_one"/></testsuite>')

    def test_junit_requires_each_source_suite_not_just_some_passing_xml(self) -> None:
        self.write("common/src/test/java/demo/OriginalTest.java", "package demo; class OriginalTest {}")
        self.write("common/src/test/java/demo/NewTest.java", "package demo; class NewTest {}")
        self.write("common/build/test-results/test/TEST-demo.OriginalTest.xml", "report")
        with self.assertRaisesRegex(ValueError, "Missing JUnit suites.*NewTest"):
            evidence.junit_reports(self.root)
        self.write("common/build/test-results/test/TEST-demo.NewTest.xml", "report")
        self.assertEqual(len(evidence.junit_reports(self.root)), 2)

    def test_junit_uses_declared_package_instead_of_source_directory(self) -> None:
        self.write("common/src/test/java/layout/PrivatePolicyTest.java",
                   "package actual.policy;\nclass PrivatePolicyTest {}")
        expected = self.write("common/build/test-results/test/TEST-actual.policy.PrivatePolicyTest.xml", "report")
        self.assertEqual(evidence.junit_reports(self.root), [expected])

    def test_required_manifest_must_be_nonempty_and_unique(self) -> None:
        for required in ([], ["test", "test"]):
            with self.assertRaisesRegex(ValueError, "manifest"):
                self.report('<testsuite><testcase name="test"/></testsuite>', required=required)

    def test_future_or_unknown_snapshot_is_rejected(self) -> None:
        snapshot = self.snapshot()
        for changes in ({"schema_version": 2}, {"started_ns": time.time_ns() + 10_000_000_000},
                        {"source_fingerprints_sha256": {}}, {"started_ns": True}):
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                evidence.validate_snapshot(self.root, snapshot | changes)


if __name__ == "__main__":
    unittest.main()
