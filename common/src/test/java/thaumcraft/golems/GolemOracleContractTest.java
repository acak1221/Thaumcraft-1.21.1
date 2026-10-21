package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.golems.EntityGolemBobber;
import thaumcraft.common.entities.golems.EnumGolemType;
import thaumcraft.common.items.golems.GolemDecorations;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrades;

/**
 * Numbers and rules of the TC4 4.2.3.5 golem that the port reproduces, pinned against the
 * readable 1.7.10 original.
 *
 * <p>Every value in this file is quoted from the original:
 * {@code thaumcraft/common/entities/golems/EnumGolemType.java:7-14} (the eight material rows),
 * {@code thaumcraft/common/entities/golems/EntityGolemBase.java:315-327,355-366,396-403,417-427,
 * 1132-1147,1247-1249} (the carry limit, the fluid limit, the repair delay, the work radius, the
 * armour and the attack delay), {@code thaumcraft/common/entities/golems/EntityGolemBobber.java:57-72}
 * (the cast velocity) and {@code thaumcraft/common/entities/golems/ItemGolemBell.java:104-184}
 * (the marker selection).</p>
 */
class GolemOracleContractTest {
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");

    /** One row of the original material table: health, armour, speed, fireResist, upgrades, carry, regenDelay, strength. */
    private record Material(int health, int armor, float speed, boolean fireResist, int upgrades, int carry, int regenDelay, int strength) {}

    private static final List<Material> ORIGINAL_MATERIALS = List.of(
            new Material(10, 0, 0.38F, false, 1, 1, 75, 0),   // STRAW
            new Material(20, 6, 0.35F, false, 1, 4, 75, 1),   // WOOD
            new Material(20, 9, 0.33F, false, 2, 8, 75, 2),   // TALLOW
            new Material(25, 9, 0.33F, true, 1, 8, 100, 2),   // CLAY
            new Material(15, 6, 0.35F, false, 2, 4, 40, 1),   // FLESH
            new Material(30, 12, 0.32F, true, 1, 16, 100, 3),  // STONE
            new Material(35, 15, 0.31F, true, 1, 32, 125, 4),  // IRON
            new Material(40, 15, 0.32F, true, 2, 32, 100, 4)); // THAUMIUM

    @BeforeAll static void bootstrapNativeClasses() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void theEightMaterialRowsAreTheOriginalTableSymbolForSymbol() {
        EnumGolemType[] types = EnumGolemType.values();
        assertEquals(ORIGINAL_MATERIALS.size(), types.length, "the original has eight materials");
        for (int index = 0; index < types.length; index++) {
            EnumGolemType type = types[index];
            Material expected = ORIGINAL_MATERIALS.get(index);
            assertEquals(expected.health(), type.health, "health of " + type);
            assertEquals(expected.armor(), type.armor, "armour of " + type);
            assertEquals(expected.speed(), type.speed, 0F, "speed of " + type);
            assertEquals(expected.fireResist(), type.fireResist, "fire resistance of " + type);
            assertEquals(expected.upgrades(), type.upgrades, "upgrade slots of " + type);
            assertEquals(expected.carry(), type.carry, "carry limit of " + type);
            assertEquals(expected.regenDelay(), type.regenDelay, "repair delay of " + type);
            assertEquals(expected.strength(), type.strength, "strength of " + type);
        }
        // The ordinal is the metadata of the original placer and of the data watcher.
        for (int index = 0; index < types.length; index++) assertEquals(types[index], EnumGolemType.getType(index));
    }

    @Test void theCarryLimitAddsTheBoundedEarthBonusOfTheOriginal() {
        // Original EntityGolemBase:355-362: carry + min(16, max(4, carry)) * Earth.
        assertEquals(5, GolemUpgrades.carry(1, 1), "Straw with one Earth");
        assertEquals(8, GolemUpgrades.carry(4, 1), "Wood with one Earth");
        assertEquals(16, GolemUpgrades.carry(8, 1), "Tallow with one Earth");
        assertEquals(24, GolemUpgrades.carry(8, 2), "Tallow with two Earth");
        assertEquals(32, GolemUpgrades.carry(16, 1), "Stone with one Earth");
        assertEquals(48, GolemUpgrades.carry(32, 1), "Iron with one Earth, the bonus is capped at sixteen");
        assertEquals(64, GolemUpgrades.carry(32, 2), "Iron with two Earth");
        assertEquals(32, GolemUpgrades.carry(32, 0));
    }

    @Test void theFluidLimitIsTheFlooredSquareRootOfTheCarryLimitInMillibuckets() {
        // Original EntityGolemBase:364-366: floor(sqrt(carry)) * 1000.
        assertEquals(1000, GolemUpgrades.fluidLimitMb(1));
        assertEquals(2000, GolemUpgrades.fluidLimitMb(4));
        assertEquals(2000, GolemUpgrades.fluidLimitMb(8));
        assertEquals(4000, GolemUpgrades.fluidLimitMb(16));
        assertEquals(5000, GolemUpgrades.fluidLimitMb(32));
        assertEquals(6000, GolemUpgrades.fluidLimitMb(48));
        assertEquals(8000, GolemUpgrades.fluidLimitMb(64));
    }

    @Test void theWorkRadiusIsSixteenPlusFourPerWaterUpgrade() {
        // Original EntityGolemBase:417-427: 16 + 4n, the spectacles add max(range * 0.1f, 1.0f),
        // an advanced golem adds max(range * 0.2f, 2.0f) on the already enlarged value.
        assertEquals(16F, GolemUpgrades.range(0, false, ""), 1e-6F);
        assertEquals(20F, GolemUpgrades.range(1, false, ""), 1e-6F);
        assertEquals(32F, GolemUpgrades.range(4, false, ""), 1e-6F);
        assertEquals(17.6F, GolemUpgrades.range(0, false, "G"), 1e-5F, "the spectacles add a tenth");
        assertEquals(19.2F, GolemUpgrades.range(0, true, ""), 1e-5F, "an advanced golem adds a fifth");
        assertEquals(21.12F, GolemUpgrades.range(0, true, "G"), 1e-4F, "the advanced fifth is taken from the enlarged value");
    }

    @Test void theArmourIsTheMaterialPlusTheAccessoriesAndNeverAboveTwenty() {
        // Original EntityGolemBase:1132-1147: super + material armour + 1 (visor) + 4 (plating), capped at 20.
        assertEquals(15, GolemDecorations.armor(15, ""));
        assertEquals(16, GolemDecorations.armor(15, "V"));
        assertEquals(19, GolemDecorations.armor(15, "P"));
        assertEquals(20, GolemDecorations.armor(15, "VP"));
        assertEquals(20, GolemDecorations.armor(20, "VP"), "never above twenty");
        assertEquals(5, GolemDecorations.maxHealthBonus("H"), "the top hat adds five health");
        assertEquals(0, GolemDecorations.maxHealthBonus("GB"));
    }

    @Test void theAttackDelayIsTwentyTicksAndEighteenForAnAdvancedGolem() {
        // Original EntityGolemBase:1247-1249: 20 - (advanced ? 2 : 0).
        assertEquals(20, GolemUpgrades.attackSpeed(false));
        assertEquals(18, GolemUpgrades.attackSpeed(true));
    }

    @Test void theRepairDelayIsTwoThirdsWithTheFez() {
        // Original EntityGolemBase:396-403: (int)((float) delay * 0.66f) with the fez.
        assertEquals(75, GolemDecorations.regenDelay(75, ""));
        assertEquals(49, GolemDecorations.regenDelay(75, "F"));
        assertEquals(66, GolemDecorations.regenDelay(100, "F"));
        assertEquals(26, GolemDecorations.regenDelay(40, "F"));
    }

    @Test void theBobberCastTakesTheSquareRootOfTheDistanceAndNotOfItsSquare() {
        // Original EntityGolemBobber:57-72: motionY = dy * 0.1 + sqrt(distance) * 0.08, where the distance is
        // sqrt(dx*dx + dy*dy + dz*dz); the square root is taken over the distance, not over its square.
        var straight = EntityGolemBobber.castDelta(0, 4, 0);
        assertEquals(0.0D, straight.x, 1e-12);
        assertEquals(0.4D + Math.sqrt(4.0D) * 0.08D, straight.y, 1e-12);
        assertEquals(0.0D, straight.z, 1e-12);
        var diagonal = EntityGolemBobber.castDelta(3, 4, 0);
        double distance = Math.sqrt(3 * 3 + 4 * 4);
        assertEquals(0.3D, diagonal.x, 1e-12);
        assertEquals(0.4D + Math.sqrt(distance) * 0.08D, diagonal.y, 1e-12);
        assertEquals(0.0D, diagonal.z, 1e-12);
        assertNotEquals(distance * 0.08D, diagonal.y - 0.4D, 1e-9D, "the original does not use the plain distance");
        assertNotEquals(Math.sqrt(distance * distance) * 0.08D, diagonal.y - 0.4D, 1e-9D, "and not its square root");
    }

    @Test void withOrderTheBellCyclesTheLowestColourOfThatSpotFirst() {
        // Original ItemGolemBell.changeMarkers:119-129: with Order the colours -1 .. 15 are searched in
        // ascending order and the LOWEST colour present at that spot is the one that is cycled.
        var spot = new GolemMarker(OVERWORLD, new BlockPos(4, 64, 4), Direction.UP, GolemMarker.ANY);
        var high = spot.withColor(9);
        var low = spot.withColor(2);
        List<GolemMarker> edited = GolemMarker.click(List.of(high, low), spot, true, false, 64);
        assertEquals(2, edited.size());
        assertEquals(3, edited.get(1).color(), "the lowest colour is the one that advances");
        assertEquals(9, edited.get(0).color(), "the other colour of that spot is untouched");
    }

    @Test void withoutOrderTheBellNeverTouchesAColouredMarkerOfThatSpot() {
        // Original ItemGolemBell.changeMarkers:119-121 and :150-152: without Order the search is for the
        // uncoloured marker only, and a spot without one simply receives an uncoloured marker.
        var spot = new GolemMarker(OVERWORLD, new BlockPos(4, 64, 4), Direction.UP, GolemMarker.ANY);
        var coloured = spot.withColor(6);
        List<GolemMarker> edited = GolemMarker.click(List.of(coloured), spot, false, false, 64);
        assertEquals(List.of(coloured, spot), edited);
        assertEquals(GolemMarker.ANY, edited.get(1).color());
    }
}
