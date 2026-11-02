package thaumcraft.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import thaumcraft.common.testing.DeepslateOreRuntimeChecks;

public final class DeepslateOreGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE, batch = "deepslate_ore_harvest")
    public void deepOresKeepShardLootAndHarvest(GameTestHelper test) { DeepslateOreRuntimeChecks.registeredLootAndHarvest(test); }
    @GameTest(template = EMPTY_STRUCTURE, batch = "deepslate_ore_generation", timeoutTicks = 2400)
    public void deepOresGenerateBelowZero(GameTestHelper test) { DeepslateOreRuntimeChecks.naturalDeepGeneration(test); }
}
