package thaumcraft.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumcraft.common.testing.RecipeTransferRuntimeChecks;

public final class RecipeTransferGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE, batch = "recipe_transfer_native")
    public void nativeFillAndPersistence(GameTestHelper test) { RecipeTransferRuntimeChecks.nativeFillAndPersistence(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "recipe_transfer_maximum")
    public void maximumReuseAndComponents(GameTestHelper test) { RecipeTransferRuntimeChecks.maximumReuseAndComponents(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "recipe_transfer_refusals")
    public void missingFullAndUnknownArePure(GameTestHelper test) { RecipeTransferRuntimeChecks.missingFullAndUnknownArePure(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "recipe_transfer_research")
    public void researchAndWandAreUnaffected(GameTestHelper test) { RecipeTransferRuntimeChecks.researchAndWandAreUnaffected(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "recipe_transfer_stale")
    public void staleSessionsAndReachRefuse(GameTestHelper test) { RecipeTransferRuntimeChecks.staleSessionsAndReachRefuse(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "recipe_transfer_vanilla")
    public void shapedAndShapelessVanilla(GameTestHelper test) { RecipeTransferRuntimeChecks.shapedAndShapelessVanilla(test); }
}
