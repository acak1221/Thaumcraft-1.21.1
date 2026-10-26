import importlib.util
from pathlib import Path
import unittest

_SPEC = importlib.util.spec_from_file_location("neoforge_gametests", Path(__file__).with_name("neoforge_gametests.py"))
catalogue = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(catalogue)

FABRIC = '''package thaumcraft.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;

public final class ThaumcraftGameTests {
    // Shared check.
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 20)
    public void shared(GameTestHelper helper) { Checks.run(helper); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void player(GameTestHelper test) { var p = test.makeMockServerPlayerInLevel(); p.kill(); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void kept(GameTestHelper test) { Checks.fabric(test); }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void transfer(GameTestHelper test) { FluidConservationRuntimeChecks.run(test); }
}
'''

NEOFORGE = '''package thaumcraft.neoforge.testing;

import net.minecraft.gametest.framework.GameTest;

public final class ThaumcraftGameTests {
    @GameTest(template = "empty", templateNamespace = "thaumcraft")
    public void kept(GameTestHelper test) { Checks.neoforge(test, "}"); }

    private static Object notePlayer(GameTestHelper test) {
        return null;
    }

    private static Object purchaseSession(GameTestHelper test) {
        return null;
    }
}
'''


class NeoForgeGameTestCatalogueTest(unittest.TestCase):
    def test_regeneration_translates_swaps_keeps_and_skips(self):
        text, summary = catalogue.regenerate(FABRIC, NEOFORGE)
        self.assertEqual(["transfer"], summary["skipped"])
        self.assertEqual(["kept"], summary["kept"])
        self.assertIn('@GameTest(template = "empty", templateNamespace = "thaumcraft", timeoutTicks = 20)', text)
        self.assertIn("    // Shared check.\n", text)
        self.assertIn("public void shared(GameTestHelper helper) { Checks.run(helper); }", text)
        self.assertIn('Checks.neoforge(test, "}")', text, "a NeoForge-specific body is kept, quotes included")
        self.assertNotIn("FabricGameTest", text)
        self.assertNotIn("net.fabricmc", text)
        self.assertIn("private static Object notePlayer(GameTestHelper test)", text)
        self.assertEqual([], catalogue.check(FABRIC, text))
        again, summary = catalogue.regenerate(FABRIC, text.replace(
            "var p = test.makeMockServerPlayerInLevel();", "var p = NetworkedGameTestPlayer.create(test);"))
        self.assertEqual(["player"], summary["networked"])
        self.assertIn("var p = NetworkedGameTestPlayer.create(test);", again)

    def test_check_names_missing_extra_and_fabric_only_wrappers(self):
        problems = catalogue.check(FABRIC, NEOFORGE.replace("void kept(", "void extra("))
        self.assertIn("NeoForge lacks the Fabric wrapper shared", problems)
        self.assertIn("NeoForge lacks the Fabric wrapper kept", problems)
        self.assertIn("NeoForge has a wrapper Fabric lacks: extra", problems)
        text, _ = catalogue.regenerate(FABRIC, NEOFORGE)
        wrapped = text.replace("\n    private static Object notePlayer",
                               '\n    @GameTest(template = "empty", templateNamespace = "thaumcraft")\n'
                               '    public void transfer(GameTestHelper test) { }\n    private static Object notePlayer')
        self.assertIn("NeoForge wraps the Fabric-only fixture test transfer", catalogue.check(FABRIC, wrapped))

    def test_repository_catalogues_agree_and_are_regenerated(self):
        fabric = (catalogue.ROOT / catalogue.FABRIC).read_text(encoding="utf-8")
        neoforge = (catalogue.ROOT / catalogue.NEOFORGE).read_text(encoding="utf-8")
        self.assertEqual([], catalogue.check(fabric, neoforge))
        text, _ = catalogue.regenerate(fabric, neoforge)
        self.assertEqual(neoforge, text, "run scripts/porting/neoforge_gametests.py --write after editing the Fabric catalogue")


if __name__ == "__main__":
    unittest.main()
