import importlib.util
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch


class GameTestOrderRunnerTest(unittest.TestCase):
    def setUp(self):
        spec = importlib.util.spec_from_file_location("gametest_order", Path(__file__).with_name("verify-gametest-order.py"))
        self.runner = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.runner)
        temp = Path(__file__).resolve().parents[2] / ".tmp"
        temp.mkdir(exist_ok=True)
        self.directory = tempfile.TemporaryDirectory(dir=temp)
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.runner.__file__ = str(self.root / "scripts/porting/verify-gametest-order.py")
        (self.root / "fabric/build").mkdir(parents=True)
        self.calls = []
        self.full_cases = ["native"]

    def run_gradle(self, command, cwd, **kwargs):
        self.assertEqual(self.root, cwd)
        self.assertFalse((self.root / "fabric/run-gametest/world").exists(), "every invocation must have a fresh world")
        (self.root / "fabric/run-gametest/world").mkdir(parents=True)
        selector = next((s.split("=", 1)[1] for s in command if s.startswith("-PgameTestFilter=")), None)
        self.calls.append(selector)
        xml = "gametest-focused-results.xml" if selector else "gametest-results.xml"
        names = [selector] if selector else self.full_cases
        qualified = [name if "." in name else "thaumcraftgametests." + name for name in names]
        (self.root / "fabric/build" / xml).write_text('<testsuite>' + ''.join(f'<testcase name="{name}"/>' for name in qualified) + '</testsuite>')
        return SimpleNamespace(returncode=0)

    def invoke(self, *args, run=None):
        with patch("sys.argv", ["verify-gametest-order.py", *args]), patch.dict("os.environ", JAVA_HOME="java21", GRADLE_USER_HOME="private"), patch.object(self.runner.subprocess, "run", side_effect=run or self.run_gradle), patch("builtins.print"):
            self.runner.main()

    def test_full_and_each_regression_get_separate_fresh_worlds(self):
        self.invoke()
        self.assertEqual(5, len(self.calls))
        self.assertIsNone(self.calls[0])
        self.assertEqual(5, len(json.loads((self.root / ".tmp/order-verification/summary.json").read_text())))

    def test_all_selects_every_native_case_even_when_manifest_lags(self):
        manifest = self.root / "scripts/porting/required-gametests.json"
        manifest.parent.mkdir(parents=True)
        manifest.write_text(json.dumps({"cases": ["alpha"]}))
        self.full_cases = ["alpha", "beta", "newlyAuthored"]
        self.invoke("--all")
        self.assertEqual([None, "thaumcraftgametests.alpha", "thaumcraftgametests.beta",
                          "thaumcraftgametests.newlyAuthored"], self.calls)

    def test_stale_xml_cannot_mask_a_missing_fresh_result(self):
        (self.root / "fabric/build/gametest-results.xml").write_text('<testsuite><testcase name="stale"/></testsuite>')
        with self.assertRaisesRegex(SystemExit, "no fresh XML"):
            self.invoke(run=lambda *args, **kwargs: SimpleNamespace(returncode=0))


if __name__ == "__main__":
    unittest.main()
