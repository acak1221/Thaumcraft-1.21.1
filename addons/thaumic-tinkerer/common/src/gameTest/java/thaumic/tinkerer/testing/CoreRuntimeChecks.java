package thaumic.tinkerer.testing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumcraft.api.research.ResearchCategories;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.TTRegistry;

/**
 * Test-only code of the port (no 1.7.10 counterpart): the add-on as a whole in a running world. Distributed under
 * the same licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
public final class CoreRuntimeChecks {
    private CoreRuntimeChecks() {}

    /** Every unit of registration ran without an exception (the registration is guarded, so this is the alarm). */
    public static void registrationHadNoFailures(GameTestHelper test) {
        test.assertTrue(ThaumicTinkerer.isReady(), "the Thaumcraft ready hook of the add-on ran");
        test.assertTrue(ThaumicTinkerer.failures().isEmpty(), "registration failures: " + ThaumicTinkerer.failures());
        test.succeed();
    }

    public static void tabAndCategoryExist(GameTestHelper test) {
        test.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(LibMisc.id("main")), "the creative tab is registered");
        test.assertTrue(ResearchCategories.getResearchList(LibResearch.CATEGORY_THAUMICTINKERER) != null,
                "the research tab is registered with Thaumcraft");
        test.assertTrue(TTRegistry.CREATIVE_TAB.isPresent(), "the tab supplier resolves");
        test.succeed();
    }
}
