package thaumic.tinkerer.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumic.tinkerer.testing.CoreRuntimeChecks;

/** Fabric wrappers of {@link CoreRuntimeChecks}. Test-only, not distributed. */
public final class CoreGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void registrationHadNoFailures(GameTestHelper test) {
        CoreRuntimeChecks.registrationHadNoFailures(test);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void tabAndCategoryExist(GameTestHelper test) {
        CoreRuntimeChecks.tabAndCategoryExist(test);
    }
}
