package thaumcraft.taint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityTaintSwarm;
import thaumcraft.common.entities.monster.EntityTaintacle;
import thaumcraft.common.lib.entities.OriginalLightRules;

/**
 * The original 1.7.10 spawn gates of the warp ecology.
 *
 * <p>Every number comes from the 4.2.3.5 oracle: the class gate of the fire bat
 * ({@code EntityFireBat:333-340}), the class gate of the taint swarm
 * ({@code EntityTaintSwarm:226-233}) and the inherited mob gate of the taintacle
 * ({@code EntityTaintacle:34-43}).</p>
 */
class OriginalSpawnGateTest {

    @Test void theClassGateRollBoundIsSeven() {
        // Oracle EntityFireBat:337-339 - the roll is rand.nextInt(7).
        assertEquals(7, OriginalLightRules.CLASS_LIGHT_ROLL_BOUND);
    }

    @Test void theClassGateReadsCombinedLightNotTheBlockChannel() {
        // World.getBlockLightValue combines BLOCK with darkened SKY.
        assertTrue(OriginalLightRules.validClassLightLevel(0, 0));
        assertTrue(OriginalLightRules.validClassLightLevel(0, 6));
        assertTrue(OriginalLightRules.validClassLightLevel(6, 6));
        assertFalse(OriginalLightRules.validClassLightLevel(7, 6));
        assertFalse(OriginalLightRules.validClassLightLevel(15, 6));
    }

    @Test void theFireBatKeepsItsOwnClassGate() {
        // Oracle EntityFireBat:333-340 - combined light against nextInt(7).
        assertTrue(EntityFireBat.originalSpawnGate(0, 0, false));
        assertTrue(EntityFireBat.originalSpawnGate(0, 6, false));
        assertFalse(EntityFireBat.originalSpawnGate(7, 6, false));
        // A peaceful world never spawns a hostile mob.
        assertFalse(EntityFireBat.originalSpawnGate(0, 0, true));
    }

    @Test void theTaintSwarmKeepsItsOwnClassGate() {
        // Oracle EntityTaintSwarm:226-233 - the class own gate is combined light against
        // nextInt(7) and the walk target value of the cell has to be at least zero.
        assertTrue(EntityTaintSwarm.originalSpawnGate(0, 6, false, -0.5F));
        assertFalse(EntityTaintSwarm.originalSpawnGate(7, 6, false, -0.5F));
        assertFalse(EntityTaintSwarm.originalSpawnGate(0, 6, true, -0.5F));
        // The walk target value of the cell is 0.5 - the pathfinding cost of the light.
        assertTrue(EntityTaintSwarm.originalSpawnGate(0, 6, false, 0.5F));
        assertFalse(EntityTaintSwarm.originalSpawnGate(0, 6, false, 0.6F));
    }

    @Test void theTaintacleInheritsTheMobGate() {
        // Oracle EntityTaintacle:34-43 - the taintacle does not override isValidLightLevel, so
        // it inherits the mob gate: saved sky against nextInt(32) and combined light
        // against nextInt(8).
        assertTrue(EntityTaintacle.originalLightGate(15, 0, 31, 7));
        assertFalse(EntityTaintacle.originalLightGate(15, 8, 31, 7));
        assertFalse(EntityTaintacle.originalLightGate(15, 0, 14, 7));
    }
}
