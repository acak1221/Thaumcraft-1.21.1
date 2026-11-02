package thaumcraft.fabric.testing;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class WandVisualGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE, batch = "wand_visual_identity")
    public void wandChargePreservesRenderIdentity(GameTestHelper test) {
        thaumcraft.common.testing.WandEquipRuntimeChecks.run(test);
    }
}
