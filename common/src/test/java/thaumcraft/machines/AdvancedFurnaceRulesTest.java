package thaumcraft.machines;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.AdvancedFurnaceRules;

class AdvancedFurnaceRulesTest {
    @Test void exactOriginalBlueprintLeavesTheUpperCenterUnconstrained() {
        int[] counts=new int[5];
        for(int x=-1;x<=1;x++) for(int y=0;y<=1;y++) for(int z=-1;z<=1;z++) {
            int part=AdvancedFurnaceRules.part(x,y,z); if(part>=0) counts[part]++;
        }
        assertArrayEquals(new int[]{1,4,4,4,4},counts);
        assertEquals(-1,AdvancedFurnaceRules.part(0,1,0));
        assertEquals(-1,AdvancedFurnaceRules.part(2,0,0));
        assertEquals(0,AdvancedFurnaceRules.part(0,0,0));
        assertEquals(2,AdvancedFurnaceRules.part(-1,1,1));
        assertEquals(3,AdvancedFurnaceRules.part(1,1,0));
    }
    @Test void cooldownUsesRemainingHeatAndOriginalFloatTruncation() {
        assertEquals(105,AdvancedFurnaceRules.cooldown(0));
        assertEquals(55,AdvancedFurnaceRules.cooldown(250));
        assertEquals(5,AdvancedFurnaceRules.cooldown(500));
        assertEquals(5,AdvancedFurnaceRules.cooldown(550));
        assertEquals(115,AdvancedFurnaceRules.cooldown(-50));
    }
    @Test void originalNozzleComparatorIsBooleanAndOvershootLightIsThirteen() {
        assertEquals(0,AdvancedFurnaceRules.comparator(0));
        assertEquals(1,AdvancedFurnaceRules.comparator(1));
        assertEquals(1,AdvancedFurnaceRules.comparator(500));
        assertEquals(0,AdvancedFurnaceRules.light(100));
        assertEquals(12,AdvancedFurnaceRules.light(500));
        assertEquals(13,AdvancedFurnaceRules.light(550));
        assertEquals(15,AdvancedFurnaceRules.light(32767)); // Native integer light property bound, original short-wrap heat retained.
    }
}
