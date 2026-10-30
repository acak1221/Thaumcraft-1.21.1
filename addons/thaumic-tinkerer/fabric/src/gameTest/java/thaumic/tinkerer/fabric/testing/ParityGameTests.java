package thaumic.tinkerer.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumic.tinkerer.testing.ParityRuntimeChecks;

/** Fabric wrappers of {@link ParityRuntimeChecks}. Test-only, not distributed. */
public final class ParityGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void oracleIsPinnedAndComplete(GameTestHelper test) {
        ParityRuntimeChecks.oracleIsPinnedAndComplete(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void researchMatchesTheOriginal(GameTestHelper test) {
        ParityRuntimeChecks.researchMatchesTheOriginal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void recipesMatchTheOriginal(GameTestHelper test) {
        ParityRuntimeChecks.recipesMatchTheOriginal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void objectTagsMatchTheOriginal(GameTestHelper test) {
        ParityRuntimeChecks.objectTagsMatchTheOriginal(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theAddOnRegistersNothingTheOriginalDidNot(GameTestHelper test) {
        ParityRuntimeChecks.theAddOnRegistersNothingTheOriginalDidNot(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void everyKnownDifferenceStillMatches(GameTestHelper test) {
        ParityRuntimeChecks.everyKnownDifferenceStillMatches(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aChangedOracleValueFailsAgainstTheLiveRegistries(GameTestHelper test) {
        ParityRuntimeChecks.aChangedOracleValueFailsAgainstTheLiveRegistries(test);
    }
}
