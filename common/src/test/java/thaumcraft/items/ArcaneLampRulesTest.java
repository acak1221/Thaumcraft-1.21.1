package thaumcraft.items;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.ArcaneLampLighting;
import thaumcraft.common.lib.ArcaneLampRules;

class ArcaneLampRulesTest {
    @Test void oldAndNegativeHeightWorldsKeepFiveBlocksOfFloorMargin() {
        assertEquals(5,ArcaneLampRules.candidateY(-20,100,0));
        assertEquals(-59,ArcaneLampRules.candidateY(-90,-40,-64));
        assertEquals(44,ArcaneLampRules.candidateY(90,40,0));
        assertEquals(-36,ArcaneLampRules.candidateY(-30,-40,-64));
        assertEquals(30,ArcaneLampRules.candidateY(30,80,0));
    }
    @Test void theSafetyThresholdStopsExactlyAtNine() {
        assertTrue(ArcaneLampRules.darkEnough(8));
        assertFalse(ArcaneLampRules.darkEnough(9));
        assertFalse(ArcaneLampRules.darkEnough(15));
    }
    @Test void theBoreUsesTheOriginalEvenDistancesAndFourStepSidePattern() {
        BlockPos top=BlockPos.ZERO;
        assertEquals(new BlockPos(1,0,3),ArcaneLampLighting.boreCandidate(top,Direction.EAST,0));
        assertEquals(new BlockPos(3,0,0),ArcaneLampLighting.boreCandidate(top,Direction.EAST,1));
        assertEquals(new BlockPos(5,0,-3),ArcaneLampLighting.boreCandidate(top,Direction.EAST,2));
        assertEquals(new BlockPos(7,-2,0),ArcaneLampLighting.boreCandidate(top,Direction.EAST,3));
        assertEquals(new BlockPos(0,-2,-63),ArcaneLampLighting.boreCandidate(top,Direction.NORTH,31));
        assertEquals(new BlockPos(3,1,0),ArcaneLampLighting.boreCandidate(top,Direction.UP,0));
        assertEquals(new BlockPos(0,-7,0),ArcaneLampLighting.boreCandidate(top,Direction.DOWN,3));
        assertThrows(IllegalArgumentException.class,()->ArcaneLampLighting.boreCandidate(top,Direction.UP,32));
    }
}
