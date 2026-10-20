package thaumcraft.loot;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.loot.LootBagRules;

class LootBagRulesTest {
    @Test void originalScatterUsesFloatWideningAndIndependentVisualDraws() {
        double[] draws={.2,.9,0,1};int[] cursor={0};
        var motion=LootBagRules.motion(()->draws[cursor[0]++]);
        assertEquals(4,cursor[0]); assertEquals((double)-.1F,motion.x); assertEquals((double).2F,motion.y); assertEquals((double).1F,motion.z);
    }
    @Test void centredHorizontalRollHasNoBiasOrEyeHeightOffsetInVelocity() {
        var motion=LootBagRules.motion(()->.5D);
        assertEquals(0,motion.x);assertEquals((double).2F,motion.y);assertEquals(0,motion.z);
    }
}
