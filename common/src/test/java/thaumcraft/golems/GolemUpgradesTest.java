package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entities.golems.EnumGolemType;
import thaumcraft.common.entities.golems.LumberGeometry;
import thaumcraft.common.items.golems.GolemAspectCargo;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUpgrades;
import thaumcraft.common.items.golems.GolemFluidCargo;

/** Pure slot rules and effect arithmetic of the golem upgrades (original EntityGolemBase / ItemGolemBell); no world or registry. */
class GolemUpgradesTest {
    private static final int AIR = GolemUpgrade.AIR.id, EARTH = GolemUpgrade.EARTH.id, FIRE = GolemUpgrade.FIRE.id,
            WATER = GolemUpgrade.WATER.id, ORDER = GolemUpgrade.ORDER.id, ENTROPY = GolemUpgrade.ENTROPY.id, EMPTY = GolemUpgrade.EMPTY;

    @Test void originalTypeOrderIsAirEarthFireWaterOrderEntropy() {
        assertEquals(List.of("air", "earth", "fire", "water", "order", "entropy"),
                java.util.Arrays.stream(GolemUpgrade.values()).map(type -> type.key).toList());
        for (int id = 0; id < 6; id++) assertEquals(id, GolemUpgrade.byId(id).id);
        assertNull(GolemUpgrade.byId(-1));
        assertNull(GolemUpgrade.byId(6));
    }

    @Test void everyOriginalTypeNowHasARealEffectAndNothingElseIsSupported() {
        for (int id = 0; id < 6; id++) {
            assertNotNull(GolemUpgrade.byId(id));
            assertTrue(GolemUpgrade.isSupported(id) && GolemUpgrade.byId(id).isSupported(), "type " + id);
        }
        assertTrue(GolemUpgrade.isSupported(FIRE) && GolemUpgrade.isSupported(ORDER), "Fire and Order joined in the third group");
        assertFalse(GolemUpgrade.isSupported(EMPTY), "an empty slot is not an upgrade");
        assertFalse(GolemUpgrade.isSupported(6));
        assertFalse(GolemUpgrade.isSupported(-2));
    }

    @Test void slotsPerMaterialMatchTheOriginalTable() {
        var slots = java.util.Arrays.stream(EnumGolemType.values()).map(type -> type.upgrades).toList();
        // Straw, Wood, Tallow, Clay, Flesh, Stone, Iron, Thaumium.
        assertEquals(List.of(1, 1, 2, 1, 2, 1, 1, 2), slots);
    }

    @Test void installationTakesTheFirstFreeSlot() {
        int[] slots = GolemUpgrades.empty(2);
        assertEquals(0, GolemUpgrades.installSlot(slots, EARTH));
        slots[0] = EARTH;
        assertEquals(1, GolemUpgrades.installSlot(slots, AIR));
        assertEquals(1, GolemUpgrades.installSlot(slots, EARTH), "a second of the same kind still fits");
        slots[1] = AIR;
        assertEquals(-1, GolemUpgrades.installSlot(slots, ENTROPY), "no free slot, no payment");
    }

    @Test void aThirdUpgradeOfOneKindIsRefusedEvenWithAFreeSlot() {
        // Only reachable with the advanced golem's extra slot, but the original loop's rule is kept.
        int[] slots = {EARTH, EARTH, EMPTY};
        assertEquals(-1, GolemUpgrades.installSlot(slots, EARTH));
        assertEquals(2, GolemUpgrades.installSlot(slots, AIR));
    }

    @Test void aSingleSlotGolemIsFullAfterOneUpgrade() {
        assertEquals(-1, GolemUpgrades.installSlot(new int[]{AIR}, ENTROPY));
    }

    @Test void unknownTypesAreNeverInstalledButFireAndOrderAre() {
        int[] slots = GolemUpgrades.empty(2);
        assertEquals(0, GolemUpgrades.installSlot(slots, FIRE));
        assertEquals(0, GolemUpgrades.installSlot(slots, ORDER));
        assertEquals(-1, GolemUpgrades.installSlot(slots, EMPTY));
        assertEquals(-1, GolemUpgrades.installSlot(slots, 6));
        assertEquals(-1, GolemUpgrades.installSlot(slots, 9));
        assertEquals(-1, GolemUpgrades.installSlot(new int[]{FIRE, FIRE, EMPTY}, FIRE), "never a third Fire");
        assertEquals(-1, GolemUpgrades.installSlot(new int[]{ORDER, ORDER, EMPTY}, ORDER), "never a third Order");
    }

    @Test void fireIgnitesFourSecondsPerUpgradeAndNothingWithoutIt() {
        // Original E:1120-1122: setFire(j * 4) while j > 0, j = Fire Aspect level + Fire upgrades.
        assertEquals(0, GolemUpgrades.igniteSeconds(0));
        assertEquals(4, GolemUpgrades.igniteSeconds(1));
        assertEquals(8, GolemUpgrades.igniteSeconds(2));
        assertEquals(12, GolemUpgrades.igniteSeconds(3));
        assertEquals(0, GolemUpgrades.igniteSeconds(-1), "a negative count never ignites");
    }

    @Test void earthAddsStrengthAndCountsTwiceInMeleeDamage() {
        // Original E:255 = 2 + getGolemStrength() + Earth, and getGolemStrength() already holds Earth.
        assertEquals(1, GolemUpgrades.strength(1, 0));
        assertEquals(2, GolemUpgrades.strength(1, 1));
        assertEquals(3.0F, GolemUpgrades.meleeDamage(1, 0));  // Wood
        assertEquals(5.0F, GolemUpgrades.meleeDamage(1, 1));  // 2 + 2 + 1
        assertEquals(8.0F, GolemUpgrades.meleeDamage(2, 2));  // Tallow with two: 2 + 4 + 2
        assertEquals(2.0F, GolemUpgrades.meleeDamage(0, 0));  // Straw baseline stays 2
    }

    @Test void airMultipliesSpeedByOnePlusFifteenPercentEach() {
        assertEquals(0.38F, GolemUpgrades.speed(0.38F, 0), 0F, "no Air leaves the material speed bit-for-bit");
        assertEquals(0.38F * 1.15F, GolemUpgrades.speed(0.38F, 1), 1e-7F);
        assertEquals(0.33F * 1.3F, GolemUpgrades.speed(0.33F, 2), 1e-7F);
        assertEquals(0.4370F, GolemUpgrades.speed(0.38F, 1), 1e-4F);
    }

    @Test void thornsAreTwoPerUpgradePlusARandomUpToTwoPerUpgrade() {
        List<Integer> bounds = new ArrayList<>();
        assertEquals(0, GolemUpgrades.thornsDamage(0, bound -> { bounds.add(bound); return 0; }), "no Entropy, no reflection");
        assertTrue(bounds.isEmpty(), "no random draw without Entropy");
        assertEquals(2, GolemUpgrades.thornsDamage(1, bound -> { bounds.add(bound); return 0; }));
        assertEquals(3, GolemUpgrades.thornsDamage(1, bound -> bound - 1));
        assertEquals(4, GolemUpgrades.thornsDamage(2, bound -> 0));
        assertEquals(7, GolemUpgrades.thornsDamage(2, bound -> bound - 1));
        assertEquals(List.of(2), bounds, "the bound is 2 * the number of upgrades");
    }

    @Test void dismantlingTossesACoinOnlyForOccupiedSlotsInOrder() {
        List<Integer> tosses = new ArrayList<>();
        boolean[] coins = {true, false};
        var returned = GolemUpgrades.dismantleReturns(new int[]{EARTH, EMPTY, ENTROPY}, () -> {
            tosses.add(tosses.size());
            return coins[tosses.size() - 1];
        });
        assertEquals(List.of(GolemUpgrade.EARTH), returned, "first coin kept Earth, second lost Entropy");
        assertEquals(2, tosses.size(), "the empty slot was not tossed for");
        assertEquals(List.of(GolemUpgrade.EARTH, GolemUpgrade.ENTROPY),
                GolemUpgrades.dismantleReturns(new int[]{EARTH, ENTROPY}, () -> true));
        assertEquals(List.of(), GolemUpgrades.dismantleReturns(new int[]{EARTH, ENTROPY}, () -> false));
        assertEquals(List.of(), GolemUpgrades.dismantleReturns(GolemUpgrades.empty(2), () -> { throw new AssertionError("no toss"); }));
    }

    @Test void entityByteArrayPadsShortInputsAndNeverTruncatesOrAcceptsUnknownTypes() {
        assertArrayEquals(new int[]{EARTH, EMPTY}, GolemUpgrades.fromBytes(new byte[]{1}, 2));
        assertArrayEquals(new int[]{EMPTY}, GolemUpgrades.fromBytes(new byte[0], 1), "a golem without the tag is fully empty");
        assertArrayEquals(new int[]{AIR, ENTROPY}, GolemUpgrades.fromBytes(new byte[]{0, 5}, 2));
        assertNull(GolemUpgrades.fromBytes(new byte[]{1, 5}, 1), "the original silently truncates; here the golem keeps the raw array");
        assertArrayEquals(new int[]{FIRE, ORDER}, GolemUpgrades.fromBytes(new byte[]{2, 4}, 2), "Fire and Order are read back since the third group");
        assertNull(GolemUpgrades.fromBytes(new byte[]{6}, 1));
        assertNull(GolemUpgrades.fromBytes(new byte[]{7}, 1));
        assertNull(GolemUpgrades.fromBytes(new byte[]{-2}, 1));
        assertNull(GolemUpgrades.fromBytes(null, 1));
        byte[] saved = GolemUpgrades.toBytes(new int[]{EARTH, EMPTY});
        assertArrayEquals(new byte[]{1, -1}, saved);
        assertArrayEquals(new int[]{EARTH, EMPTY}, GolemUpgrades.fromBytes(saved, 2));
    }

    @Test void packedSyncRoundTripsEverySlotCombination() {
        int[] kinds = {EMPTY, AIR, EARTH, FIRE, WATER, ORDER, ENTROPY};
        for (int first : kinds) for (int second : kinds) for (int third : kinds) {
            int[] slots = {first, second, third};
            assertArrayEquals(slots, GolemUpgrades.unpack(GolemUpgrades.pack(slots), 3));
        }
        assertEquals(0, GolemUpgrades.pack(GolemUpgrades.empty(2)), "an empty golem synchronises as zero");
        assertArrayEquals(new int[]{EARTH}, GolemUpgrades.unpack(GolemUpgrades.pack(new int[]{EARTH, AIR}), 1));
        assertArrayEquals(new int[0], GolemUpgrades.unpack(123, 0));
    }

    @Test void placerListKeepsSlotOrderAndOnlyDropsTrailingEmptySlots() {
        assertEquals(List.of(), GolemUpgrades.toList(GolemUpgrades.empty(2)));
        assertEquals(List.of(EARTH), GolemUpgrades.toList(new int[]{EARTH, EMPTY}));
        assertEquals(List.of(EMPTY, EARTH), GolemUpgrades.toList(new int[]{EMPTY, EARTH}));
        assertEquals(List.of(EARTH), GolemUpgrades.canonical(List.of(EARTH, EMPTY, EMPTY)));
    }

    /** Original E:360 on the contract's table: Straw 1->5, Wood 4->8, Tallow 8->16/24, Clay 8->16, Flesh 4->8/12, Stone 16->32, Iron 32->48, Thaumium 32->48/64. */
    @Test void carryLimitIsMaterialPlusMinSixteenMaxFourOfItPerEarth() {
        // Straw, Wood, Tallow, Clay, Flesh, Stone, Iron, Thaumium: {no Earth, one, two}.
        int[][] expected = {{1, 5, 9}, {4, 8, 12}, {8, 16, 24}, {8, 16, 24}, {4, 8, 12}, {16, 32, 48}, {32, 48, 64}, {32, 48, 64}};
        EnumGolemType[] types = EnumGolemType.values();
        for (int type = 0; type < types.length; type++) {
            for (int earth = 0; earth <= 2; earth++) {
                assertEquals(expected[type][earth], GolemUpgrades.carry(types[type].carry, earth), types[type] + " with " + earth + " Earth");
            }
        }
    }

    @Test void waterCapacityIsFloorSqrtOfTheCarryLimitInBuckets() {
        int[][] table = {{1, 1000}, {4, 2000}, {5, 2000}, {8, 2000}, {9, 3000}, {12, 3000}, {16, 4000}, {24, 4000}, {32, 5000}, {48, 6000}, {64, 8000}};
        for (int[] row : table) assertEquals(row[1], GolemUpgrades.fluidLimitMb(row[0]), "carry " + row[0]);
        assertEquals(8000, GolemFluidCargo.capacity(7, 2), "Thaumium with two Earth: 64 -> 8000 mB");
        assertEquals(6000, GolemFluidCargo.capacity(7, 1));
        assertEquals(5000, GolemFluidCargo.capacity(7, 0), "the pre-Earth Thaumium and Iron limit is unchanged");
        assertEquals(5000, GolemFluidCargo.capacity(6, 0));
        assertEquals(4000, GolemFluidCargo.capacity(5, 0), "Stone: floor(sqrt(16)) = 4");
        assertEquals(5000, GolemFluidCargo.capacity(5, 1), "Stone with Earth: 32 -> 5000 mB");
        assertEquals(6000, GolemFluidCargo.capacity(6, 1), "Iron with Earth: 48 -> 6000 mB");
        assertEquals(2000, GolemFluidCargo.capacity(2, 0), "Tallow without Earth: floor(sqrt(8)) = 2");
        assertEquals(4000, GolemFluidCargo.capacity(2, 2), "Tallow with two Earth: floor(sqrt(24)) = 4");
    }

    @Test void aspectCapacityIsTheCarryLimit() {
        assertEquals(64, GolemAspectCargo.capacity(7, 2));
        assertEquals(48, GolemAspectCargo.capacity(6, 1));
        assertEquals(32, GolemAspectCargo.capacity(7, 0), "the pre-Earth value is unchanged");
        assertEquals(24, GolemAspectCargo.capacity(2, 2));
        assertEquals(8, GolemAspectCargo.capacity(2, 0));
        assertThrows(IllegalArgumentException.class, () -> GolemAspectCargo.capacity(8, 0));
        assertThrows(IllegalArgumentException.class, () -> GolemFluidCargo.capacity(-1, 0));
    }

    @Test void cargoValidationFollowsTheEarthCountNotJustTheMaterial() {
        new GolemFluidCargo(net.minecraft.resources.ResourceLocation.withDefaultNamespace("water"), 8000).validate(7, 2);
        assertThrows(IllegalArgumentException.class, () -> new GolemFluidCargo(net.minecraft.resources.ResourceLocation.withDefaultNamespace("water"), 8000).validate(7, 1), "8000 mB needs both Earth upgrades");
        assertThrows(IllegalArgumentException.class, () -> new GolemFluidCargo(net.minecraft.resources.ResourceLocation.withDefaultNamespace("water"), 6000).validate(7, 0));
        new GolemFluidCargo(net.minecraft.resources.ResourceLocation.withDefaultNamespace("water"), 6000).validate(7, 1);
        new GolemFluidCargo(net.minecraft.resources.ResourceLocation.withDefaultNamespace("water"), 5000).validate(7, 0);
        assertThrows(IllegalArgumentException.class, () -> new GolemFluidCargo(net.minecraft.resources.ResourceLocation.withDefaultNamespace("water"), 3000).validate(2, 0), "Tallow without Earth holds two buckets");
        new GolemFluidCargo(net.minecraft.resources.ResourceLocation.withDefaultNamespace("water"), 4000).validate(2, 1);
        new GolemAspectCargo(Aspect.AIR, 64).validate(7, 2);
        assertThrows(IllegalArgumentException.class, () -> new GolemAspectCargo(Aspect.AIR, 64).validate(7, 1));
        assertThrows(IllegalArgumentException.class, () -> new GolemAspectCargo(Aspect.AIR, 33).validate(7, 0));
        new GolemAspectCargo(Aspect.AIR, 32).validate(7, 0);
    }

    /** The record ceilings are only a payload bound: they must equal the largest capacity a real golem can reach. */
    @Test void payloadCeilingsEqualTheLargestReachableCapacity() {
        int water = 0, aspect = 0;
        EnumGolemType[] types = EnumGolemType.values();
        for (int type = 0; type < types.length; type++) {
            int earthMax = Math.min(types[type].upgrades, GolemUpgrades.MAX_SAME_KIND);
            water = Math.max(water, GolemFluidCargo.capacity(type, earthMax));
            aspect = Math.max(aspect, GolemAspectCargo.capacity(type, earthMax));
        }
        assertEquals(water, GolemFluidCargo.MAX_AMOUNT_MB);
        assertEquals(aspect, GolemAspectCargo.MAX_AMOUNT);
        assertEquals(8000, water);
        assertEquals(64, aspect);
        assertThrows(IllegalArgumentException.class, () -> new GolemFluidCargo(net.minecraft.resources.ResourceLocation.withDefaultNamespace("water"), 8001));
        assertThrows(IllegalArgumentException.class, () -> new GolemAspectCargo(Aspect.AIR, 65));
    }

    @Test void rangeIsSixteenPlusFourPerWaterAndEveryRadiusDerivesFromIt() {
        assertEquals(16F, GolemUpgrades.range(0));
        assertEquals(20F, GolemUpgrades.range(1));
        assertEquals(24F, GolemUpgrades.range(2));
        // Harvest ceil(range/4) 4/5/6, Lumber ceil(range/3) 6/7/8, Fishing ceil(range/2) 8/10/12.
        for (int water = 0; water <= 2; water++) {
            float range = GolemUpgrades.range(water);
            assertEquals(4 + water, GolemUpgrades.workRadius(range, 4));
            assertEquals(6 + water, LumberGeometry.radius(range));
            assertEquals(8 + 2 * water, GolemUpgrades.workRadius(range, 2));
        }
        assertEquals(8, GolemUpgrades.workRadius(22F, 3) , "ceil(22 / 3) rounds up");
        assertEquals(6, GolemUpgrades.workRadius(21F, 4), "and so does ceil(21 / 4)");
    }

    @Test void waterIsInstalledLikeAnyOtherTypeUpToTwo() {
        int[] slots = GolemUpgrades.empty(2);
        assertEquals(0, GolemUpgrades.installSlot(slots, WATER));
        slots[0] = WATER;
        assertEquals(1, GolemUpgrades.installSlot(slots, WATER), "a second Water still fits");
        slots[1] = WATER;
        assertEquals(2, GolemUpgrades.count(slots, GolemUpgrade.WATER));
        assertEquals(-1, GolemUpgrades.installSlot(new int[]{WATER, WATER, EMPTY}, WATER), "never a third of one kind");
        assertArrayEquals(new int[]{WATER}, GolemUpgrades.fromBytes(new byte[]{3}, 1));
        assertArrayEquals(new int[]{EMPTY, WATER}, GolemUpgrades.fit(List.of(EMPTY, WATER), 2));
        assertArrayEquals(new int[]{WATER, EARTH}, GolemUpgrades.unpack(GolemUpgrades.pack(new int[]{WATER, EARTH}), 2));
    }

    @Test void fittingAListIntoAMaterialNeverLosesOrInventsAnUpgrade() {
        assertArrayEquals(new int[]{EARTH, EMPTY}, GolemUpgrades.fit(List.of(EARTH), 2));
        assertArrayEquals(new int[]{EMPTY}, GolemUpgrades.fit(List.of(), 1));
        assertArrayEquals(new int[]{EMPTY, AIR}, GolemUpgrades.fit(List.of(EMPTY, AIR), 2));
        assertNull(GolemUpgrades.fit(List.of(EARTH, AIR), 1), "two upgrades do not fit a one-slot material");
        assertArrayEquals(new int[]{FIRE, EMPTY}, GolemUpgrades.fit(List.of(FIRE), 2));
        assertArrayEquals(new int[]{EMPTY, ORDER}, GolemUpgrades.fit(List.of(EMPTY, ORDER), 2));
        assertNull(GolemUpgrades.fit(List.of(6), 2), "a type outside the original six is never fitted");
        assertNull(GolemUpgrades.fit(null, 2));
    }
}
