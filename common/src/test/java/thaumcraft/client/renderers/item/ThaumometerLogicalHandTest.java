package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.junit.jupiter.api.Test;

class ThaumometerLogicalHandTest {
    @Test void physicalDisplayArmMapsThroughThePlayersMainArm() {
        assertEquals(InteractionHand.MAIN_HAND, ItemThaumometerRenderer.logicalHand(false, HumanoidArm.RIGHT));
        assertEquals(InteractionHand.OFF_HAND, ItemThaumometerRenderer.logicalHand(true, HumanoidArm.RIGHT));
        assertEquals(InteractionHand.MAIN_HAND, ItemThaumometerRenderer.logicalHand(true, HumanoidArm.LEFT));
        assertEquals(InteractionHand.OFF_HAND, ItemThaumometerRenderer.logicalHand(false, HumanoidArm.LEFT));
    }
}
