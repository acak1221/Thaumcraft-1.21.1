"""Default Fabric acceptance must retain every authored suite without expanding NeoForge requirements."""
import json
from pathlib import Path
import re
import tempfile
import unittest

import checkpoint_evidence
import neoforge_gametests
import record_verification


ROOT = Path(__file__).resolve().parents[2]
FABRIC_TESTS = ROOT / "fabric/src/gameTest/java/thaumcraft/fabric/testing"
DIAGNOSTICS = frozenset({"regenerationfootprint", "structurecandidatefootprint"})


def authored_fabric_cases():
    cases, classes = set(), set()
    for path in sorted(FABRIC_TESTS.glob("*GameTests.java")):
        source = path.read_text(encoding="utf-8")
        wrappers = neoforge_gametests.methods(source)
        if not wrappers:
            continue
        package = re.search(r"(?m)^package\s+([\w.]+);", source).group(1)
        classes.add(package + "." + path.stem)
        for wrapper in wrappers:
            name = wrapper["name"].lower()
            if name not in DIAGNOSTICS:
                cases.add((path.stem.lower() + "." + name).removeprefix("thaumcraftgametests."))
    return cases, classes


class RequiredGameTestsReviewTest(unittest.TestCase):
    def test_fabric_addendum_does_not_change_shared_or_neoforge_requirements(self):
        manifest = {"cases": ["shared"], "fabric_cases": ["comprehensivereviewgametests.review"]}
        self.assertEqual(["shared", "comprehensivereviewgametests.review"],
                         record_verification.required_cases_for_loader(manifest, "fabric"))
        self.assertEqual(["shared"], record_verification.required_cases_for_loader(manifest, "neoforge"))
        self.assertEqual(["shared"], manifest["cases"], "loader selection cannot mutate the shared baseline")

    def test_missing_fabric_review_cannot_be_hidden_by_passing_shared_cases(self):
        manifest = {"cases": ["shared"], "fabric_cases": ["comprehensivereviewgametests.review"]}
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "tests.xml"
            report.write_text('<testsuite><testcase name="thaumcraftgametests.shared"/></testsuite>', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "Missing required.*comprehensivereviewgametests.review"):
                checkpoint_evidence.validate_test_report(
                    report, required=record_verification.required_cases_for_loader(manifest, "fabric"), fabric_prefix=True)
            report.write_text('<testsuite><testcase name="shared"/></testsuite>', encoding="utf-8")
            self.assertEqual(1, checkpoint_evidence.validate_test_report(
                report, required=record_verification.required_cases_for_loader(manifest, "neoforge"))["tests"])
            report.write_text('<testsuite><testcase name="thaumcraftgametests.shared"/>'
                              '<testcase name="comprehensivereviewgametests.review"/></testsuite>', encoding="utf-8")
            self.assertEqual(2, checkpoint_evidence.validate_test_report(
                report, required=record_verification.required_cases_for_loader(manifest, "fabric"), fabric_prefix=True)["tests"])

    def test_manifest_pins_every_authored_default_fabric_case(self):
        expected, _ = authored_fabric_cases()
        manifest = json.loads((ROOT / "scripts/porting/required-gametests.json").read_text(encoding="utf-8"))
        required = manifest["cases"] + manifest.get("fabric_cases", [])
        self.assertEqual(len(required), len(set(required)), "every required native case appears exactly once")
        self.assertEqual(expected, set(required), "new or removed authored wrappers require explicit acceptance review")

    def test_every_authored_fabric_suite_stays_registered_in_the_test_mod(self):
        _, expected = authored_fabric_cases()
        metadata = json.loads((ROOT / "fabric/src/gameTest/resources/fabric.mod.json").read_text(encoding="utf-8"))
        self.assertEqual(expected, set(metadata["entrypoints"]["fabric-gametest"]),
                         "dropping a suite entrypoint cannot silently drop its acceptance cases")


if __name__ == "__main__":
    unittest.main()
