package thaumcraft.mods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import thaumcraft.common.lib.mods.ChampionHooks;
import thaumcraft.common.lib.mods.ChampionModifier;
import thaumcraft.common.lib.mods.ChampionRules;

/**
 * The native champion glue: the spawn roll of a mob that enters the world, the
 * third state of a mob that was never rolled and the dangerous location extension
 * point of the original Outer Lands maze.
 */
class ChampionHooksTest {
    /** Random that answers with a fixed value so the original chances can be checked exactly. */
    private static final class ScriptedJavaRandom extends java.util.Random {
        private final int intValue;
        ScriptedJavaRandom(int intValue) { this.intValue = intValue; }
        @Override public int nextInt(int bound) { return Math.floorMod(intValue, bound); }
    }

    /**
     * Random that answers with a scripted sequence, so the original spawn roll and
     * the original modifier roll can be told apart.
     */
    private static final class ScriptedRandom implements RandomSource {
        private final int[] values;
        private int index;
        ScriptedRandom(int... values) { this.values = values; }
        private int next() { int value = values[Math.min(index, values.length - 1)]; index++; return value; }
        @Override public RandomSource fork() { return this; }
        @Override public PositionalRandomFactory forkPositional() { throw new UnsupportedOperationException(); }
        @Override public void setSeed(long seed) {}
        @Override public int nextInt() { return next(); }
        @Override public int nextInt(int bound) { return Math.floorMod(next(), bound); }
        @Override public long nextLong() { return next(); }
        @Override public boolean nextBoolean() { return next() % 2 == 0; }
        @Override public float nextFloat() { return next() / 100.0F; }
        @Override public double nextDouble() { return next() / 100.0; }
        @Override public double nextGaussian() { return 0.0D; }
    }

    private static ChampionHooks.SpawnFacts facts(boolean easy, boolean hard, boolean outer, boolean hostile,
                                                  boolean dangerous, boolean creeper, boolean enabled) {
        return new ChampionHooks.SpawnFacts(enabled, easy, hard, outer, hostile, dangerous, creeper);
    }

    @Test void originalRollBecomesAChampionWhenTheShiftedRollReachesZero() {
        assertEquals(ChampionModifier.BOLD, ChampionHooks.roll(new ScriptedRandom(0, 0),
                facts(false, false, false, false, false, false, true), ChampionRules.TIER_COMMON, 20.0F));
        assertEquals(ChampionRules.NO_CHAMPION, ChampionHooks.roll(new ScriptedRandom(1, 0),
                facts(false, false, false, false, false, false, true), ChampionRules.TIER_COMMON, 20.0F));
    }

    @Test void originalRollKeepsEveryWorldShift() {
        // The easy difficulty adds two, so a roll of zero no longer reaches the champion.
        assertEquals(ChampionRules.NO_CHAMPION, ChampionHooks.roll(new ScriptedRandom(0, 0),
                facts(true, false, false, false, false, false, true), ChampionRules.TIER_COMMON, 20.0F));
        // The hard difficulty takes two away, so a roll of two still reaches the champion.
        assertEquals(ChampionModifier.BOLD, ChampionHooks.roll(new ScriptedRandom(2, 0),
                facts(false, true, false, false, false, false, true), ChampionRules.TIER_COMMON, 20.0F));
        // The Outer Lands take three away.
        assertEquals(ChampionModifier.BOLD, ChampionHooks.roll(new ScriptedRandom(3, 0),
                facts(false, false, true, false, false, false, true), ChampionRules.TIER_COMMON, 20.0F));
        // A spooky, nether or end biome takes two away.
        assertEquals(ChampionModifier.BOLD, ChampionHooks.roll(new ScriptedRandom(2, 0),
                facts(false, false, false, true, false, false, true), ChampionRules.TIER_COMMON, 20.0F));
        // A dangerous location takes ten away.
        assertEquals(ChampionModifier.BOLD, ChampionHooks.roll(new ScriptedRandom(10, 0),
                facts(false, false, false, false, true, false, true), ChampionRules.TIER_COMMON, 20.0F));
    }

    @Test void originalWhitelistLevelShiftsTheRoll() {
        assertEquals(ChampionModifier.BOLD, ChampionHooks.roll(new ScriptedRandom(1, 0),
                facts(false, false, false, false, false, false, true), ChampionRules.TIER_RARE, 20.0F));
        assertEquals(ChampionRules.NO_CHAMPION, ChampionHooks.roll(new ScriptedRandom(2, 0),
                facts(false, false, false, false, false, false, true), ChampionRules.TIER_RARE, 20.0F));
        assertEquals(ChampionRules.NO_CHAMPION, ChampionHooks.roll(new ScriptedRandom(0, 0),
                facts(false, false, false, false, false, false, true), ChampionRules.TIER_NONE, 20.0F));
    }

    @Test void originalHealthGateKeepsTenBaseHealth() {
        assertEquals(ChampionRules.NO_CHAMPION, ChampionHooks.roll(new ScriptedRandom(0, 0),
                facts(false, false, false, false, false, false, true), ChampionRules.TIER_COMMON, 9.9F));
        assertEquals(ChampionModifier.BOLD, ChampionHooks.roll(new ScriptedRandom(0, 0),
                facts(false, false, false, false, false, false, true), ChampionRules.TIER_COMMON, 10.0F));
    }

    @Test void originalCreeperChampionWasAlwaysBold() {
        // The modifier roll would answer the warded modifier, the creeper takes bold.
        assertEquals(ChampionModifier.BOLD, ChampionHooks.roll(new ScriptedRandom(0, 5),
                facts(false, false, false, false, false, true, true), ChampionRules.TIER_COMMON, 20.0F));
        assertEquals(ChampionModifier.WARDED, ChampionHooks.roll(new ScriptedRandom(0, 5),
                facts(false, false, false, false, false, false, true), ChampionRules.TIER_COMMON, 20.0F));
    }

    @Test void originalDisabledChampionsOnlyKeptTheBoss() {
        assertEquals(ChampionRules.NO_CHAMPION, ChampionHooks.roll(new ScriptedRandom(0, 0),
                facts(false, false, false, false, false, false, false), ChampionRules.TIER_COMMON, 20.0F));
        assertEquals(ChampionModifier.BOLD, ChampionHooks.roll(new ScriptedRandom(0, 0),
                facts(false, false, false, false, false, false, false), ChampionRules.TIER_BOSS, 20.0F));
    }

    @Test void nativeRollAgreesWithThePortedAdapter() {
        ChampionHooks.SpawnFacts world = facts(false, false, true, true, false, false, true);
        int rolled = ChampionRules.Adapter.spawnRoll(new ScriptedRandom(3, 0), world.championsEnabled(),
                world.easy(), world.hard(), world.outerDimension(), world.hostileBiome(), world.dangerousLocation());
        boolean champion = ChampionRules.becomesChampion(rolled, ChampionRules.TIER_RARE, 20.0F, world.championsEnabled());
        int expected = champion ? ChampionRules.rollModifier(new ScriptedJavaRandom(0), world.creeper()).id
                : ChampionRules.NO_CHAMPION;
        assertEquals(expected, ChampionHooks.roll(new ScriptedRandom(3, 0), world, ChampionRules.TIER_RARE, 20.0F));
    }

    @Test void neverRolledKeepsTheOriginalAttributeValue() {
        // The original attribute held -2.0 while it was never rolled and -1.0 once a
        // mob was rolled and lost the roll, so the native field needs both values.
        assertEquals(-2, ChampionHooks.NOT_ROLLED);
        assertEquals(ChampionRules.NO_CHAMPION, -1);
        assertNotEquals(ChampionHooks.NOT_ROLLED, ChampionRules.NO_CHAMPION);
        assertEquals("tc.mobmod", ChampionHooks.SAVE_KEY);
    }

    @Test void originalBiomeTypesAreResolvedFromTheNativeTags() {
        assertEquals("thaumcraft:eldritch_lands", ChampionHooks.ELDRITCH_LANDS.location().toString());
        assertEquals("thaumcraft:node_biome_types/spooky", ChampionHooks.SPOOKY.location().toString());
        assertEquals("thaumcraft:node_biome_types/nether", ChampionHooks.NETHER.location().toString());
        assertEquals("thaumcraft:node_biome_types/end", ChampionHooks.END.location().toString());
    }

    @Test void noMazeDataMeansNoDangerousLocation() {
        // The native port has no Outer Lands maze yet, so nothing is dangerous until
        // the maze wires its own test in.
        assertFalse(ChampionHooks.dangerous(null, 0, 0, 0));
        ChampionHooks.setDangerousLocation((level, x, y, z) -> x == 16 && z == 32);
        try {
            assertTrue(ChampionHooks.dangerous(null, 16, 0, 32));
            assertFalse(ChampionHooks.dangerous(null, 0, 0, 0));
        } finally {
            ChampionHooks.setDangerousLocation(null);
        }
        assertFalse(ChampionHooks.dangerous(null, 16, 0, 32));
    }

    @Test void originalChampionMobsAreEnabledByDefault() {
        assertTrue(ChampionHooks.championsEnabled());
        ChampionHooks.setChampionsEnabled(false);
        try {
            assertFalse(ChampionHooks.championsEnabled());
        } finally {
            ChampionHooks.setChampionsEnabled(true);
        }
    }
}
