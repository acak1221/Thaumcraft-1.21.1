package thaumcraft.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.EldritchBlockRules;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The original eldritch obelisk: the range of the obelisk is a radius around the obelisk itself
 * (oracle TileEldritchObelisk:47 and EntityUtils:181-192), not a box at the world origin.
 */
class EldritchObeliskRulesTest {
    @Test void theObeliskSearchesAroundItselfAndNotAroundTheWorldOrigin() {
        AABB box = EldritchBlockRules.obeliskBox(new BlockPos(1000, 64, -500));
        assertEquals(994.5D, box.minX, 1.0E-6D);
        assertEquals(1006.5D, box.maxX, 1.0E-6D);
        assertEquals(58.0D, box.minY, 1.0E-6D);
        assertEquals(70.0D, box.maxY, 1.0E-6D);
        assertEquals(-505.5D, box.minZ, 1.0E-6D);
        assertEquals(-493.5D, box.maxZ, 1.0E-6D);
    }

    @Test void theObeliskReachesSixBlocksAndNoFurther() {
        AABB box = EldritchBlockRules.obeliskBox(new BlockPos(1000, 64, -500));
        // The middle of the obelisk block is the centre of the search cube.
        assertTrue(box.contains(1000.5D, 64.0D, -500.5D));
        assertTrue(box.contains(1005.5D, 64.0D, -500.5D));
        assertTrue(box.contains(1000.5D, 69.0D, -500.5D));
        assertFalse(box.contains(1007.5D, 64.0D, -500.5D));
        assertFalse(box.contains(1000.5D, 64.0D, -507.5D));
        assertEquals(6.0D, EldritchBlockRules.OBELISK_RANGE);
    }

    @Test void theObeliskOnlyEmpowersTheEldritchMobsThatAreNotRegeneratingYet() {
        // TileEldritchObelisk:49 - a mob that already carries regeneration is skipped.
        // The obelisk BUFFS the eldritch mobs: damage boost and regeneration.
        assertTrue(EldritchBlockRules.obeliskAffects(true, true, false));
        assertFalse(EldritchBlockRules.obeliskAffects(true, true, true));
        assertFalse(EldritchBlockRules.obeliskAffects(false, true, false));
        assertFalse(EldritchBlockRules.obeliskAffects(true, false, false));
        assertTrue(EldritchBlockRules.obeliskAffects(true, true));
        assertFalse(EldritchBlockRules.obeliskAffects(false, true));
        assertEquals(40, EldritchBlockRules.OBELISK_EFFECT_DURATION);
        assertEquals(0, EldritchBlockRules.OBELISK_EFFECT_AMPLIFIER);
        // The original never raises its counter, so the offer is made every tick.
        assertEquals(1, EldritchBlockRules.OBELISK_TICK_PERIOD);
    }
}
