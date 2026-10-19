package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.entities.golems.GolemFishingLoot;
import thaumcraft.common.items.golems.GolemUpgrades;
import thaumcraft.common.items.golems.GolemUpgrades.FishCategory;

/** Upgrades of a Fishing golem (original AIFish:118-120 Air, :183 Entropy, :184 Order). */
class GolemFishingUpgradesTest {
    private static final float EPS = 1e-6f;

    @BeforeAll static void bootstrapNativeStacks() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void entropyLowersTheJunkBaseAndOrderRaisesTheTreasureBase() {
        assertEquals(0.1f, GolemUpgrades.fishJunkBase(0), EPS);
        assertEquals(0.075f, GolemUpgrades.fishJunkBase(1), EPS);
        assertEquals(0.05f, GolemUpgrades.fishJunkBase(2), EPS);
        assertEquals(0.05f, GolemUpgrades.fishTreasureBase(0), EPS);
        assertEquals(0.0625f, GolemUpgrades.fishTreasureBase(1), EPS);
        assertEquals(0.075f, GolemUpgrades.fishTreasureBase(2), EPS);
        // Without upgrades the bases are exactly the old constants, so a plain Fishing golem rolls as it did.
        assertEquals(0.1f, GolemUpgrades.fishJunkBase(0));
        assertEquals(0.05f, GolemUpgrades.fishTreasureBase(0));
    }

    @Test void thePondOnlyMovesTheUpgradedBasesAndStillClamps() {
        var open = new GolemFishingLoot.Pond(0, 0, 0);
        assertEquals(0.1f, open.junkChance(), EPS);
        assertEquals(0.05f, open.treasureChance(), EPS);
        assertEquals(0.05f, open.junkChance(2), EPS);
        assertEquals(0.075f, open.treasureChance(2), EPS);
        // Four sides, all under the sky, three deep cells each: junk -0.005 per side and per sky side, treasure +0.00125 and +0.001.
        var lake = new GolemFishingLoot.Pond(4, 4, 12);
        assertEquals(0.06f, lake.junkChance(), EPS);
        assertEquals(0.01f, lake.junkChance(2), EPS);
        assertEquals(0.072f, lake.treasureChance(), EPS);
        assertEquals(0.097f, lake.treasureChance(2), EPS);
        assertEquals(lake.junkChance(), lake.junkChance(0));
        assertEquals(lake.treasureChance(), lake.treasureChance(0));
        // Each upgrade moves its own chance and never the other one.
        assertEquals(lake.treasureChance(), new GolemFishingLoot.Pond(4, 4, 12).treasureChance(0));
        assertEquals(lake.junkChance(), new GolemFishingLoot.Pond(4, 4, 12).junkChance(0));
        for (int entropy = 0; entropy <= 2; entropy++) for (int order = 0; order <= 2; order++) {
            float junk = lake.junkChance(entropy), treasure = lake.treasureChance(order);
            assertTrue(junk >= 0 && junk <= 1 && treasure >= 0 && treasure <= 1);
            assertEquals(0.06f - 0.025f * entropy, junk, EPS);
            assertEquals(0.072f + 0.0125f * order, treasure, EPS);
        }
    }

    @Test void oneRollPicksJunkTreasureOrFishByTheOriginalWindows() {
        // Plain golem on open water: junk [0, 0.1), treasure [0.1, 0.15), fish above.
        assertEquals(FishCategory.JUNK, GolemUpgrades.fishCategory(0.0f, 0.1f, 0.05f));
        assertEquals(FishCategory.JUNK, GolemUpgrades.fishCategory(0.099f, 0.1f, 0.05f));
        assertEquals(FishCategory.TREASURE, GolemUpgrades.fishCategory(0.1f, 0.1f, 0.05f));
        assertEquals(FishCategory.TREASURE, GolemUpgrades.fishCategory(0.149f, 0.1f, 0.05f));
        assertEquals(FishCategory.FISH, GolemUpgrades.fishCategory(0.151f, 0.1f, 0.05f));
        assertEquals(FishCategory.FISH, GolemUpgrades.fishCategory(0.999f, 0.1f, 0.05f));
        // The same roll lands in another pool once an upgrade has moved a window.
        var open = new GolemFishingLoot.Pond(0, 0, 0);
        float roll = 0.16f;
        assertEquals(FishCategory.FISH, GolemUpgrades.fishCategory(roll, open.junkChance(0), open.treasureChance(0)), "plain");
        assertEquals(FishCategory.TREASURE, GolemUpgrades.fishCategory(roll, open.junkChance(0), open.treasureChance(2)), "two Order: treasure to 0.175");
        float roll2 = 0.07f;
        assertEquals(FishCategory.JUNK, GolemUpgrades.fishCategory(roll2, open.junkChance(0), open.treasureChance(0)), "plain");
        assertEquals(FishCategory.TREASURE, GolemUpgrades.fishCategory(roll2, open.junkChance(2), open.treasureChance(0)), "two Entropy: junk shrinks to 0.05");
    }

    @Test void entropyAndOrderShiftTheShareOfEachPoolOverAFullSweep() {
        var lake = new GolemFishingLoot.Pond(4, 4, 12);
        int steps = 100_000;
        int[][] shares = new int[3][3];
        for (int variant = 0; variant < 3; variant++) {
            for (int step = 0; step < steps; step++) {
                float roll = (step + 0.5f) / steps;
                var category = GolemUpgrades.fishCategory(roll, lake.junkChance(variant == 1 ? 2 : 0), lake.treasureChance(variant == 2 ? 2 : 0));
                shares[variant][category.ordinal()]++;
            }
        }
        // plain: junk 6 %, treasure 7.2 %; two Entropy: junk 1 %; two Order: treasure 9.7 %.
        assertEquals(0.06, shares[0][FishCategory.JUNK.ordinal()] / (double) steps, 0.001);
        assertEquals(0.072, shares[0][FishCategory.TREASURE.ordinal()] / (double) steps, 0.001);
        assertEquals(0.01, shares[1][FishCategory.JUNK.ordinal()] / (double) steps, 0.001);
        assertEquals(0.072, shares[1][FishCategory.TREASURE.ordinal()] / (double) steps, 0.001, "Entropy leaves treasure alone");
        assertEquals(0.06, shares[2][FishCategory.JUNK.ordinal()] / (double) steps, 0.001, "Order leaves junk alone");
        assertEquals(0.097, shares[2][FishCategory.TREASURE.ordinal()] / (double) steps, 0.001);
    }

    @Test void airGivesASecondCatchWithProbabilityAirOverTenAndDrawsOnlyWithAir() {
        var draws = new AtomicInteger();
        assertEquals(1, GolemUpgrades.fishCatches(0, bound -> { draws.incrementAndGet(); return 0; }));
        assertEquals(0, draws.get(), "a golem without Air consumes no random number");
        for (int air = 1; air <= 2; air++) {
            int doubled = 0;
            for (int value = 0; value < 10; value++) {
                int rolled = value;
                int catches = GolemUpgrades.fishCatches(air, bound -> { assertEquals(10, bound); return rolled; });
                assertEquals(value < air ? 2 : 1, catches, "Air " + air + ", draw " + value);
                if (catches == 2) doubled++;
            }
            assertEquals(air, doubled, "Air " + air + " doubles " + air + " of 10 draws");
        }
    }
}
