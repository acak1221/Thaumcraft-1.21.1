package thaumcraft.common.testing;

import java.lang.reflect.Field;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import thaumcraft.common.lib.mods.ChampionHooks;

/**
 * The declared original champion join roll of a test fixture.
 *
 * <p>The 1.7 original rolled {@code nextInt100} of the world random for every {@code EntityMob}
 * that joined the world (oracle {@code EventHandlerEntity.entitySpawns:391}) and it paid a
 * champion mob that a player hit five to seven extra experience and one loot bag (oracle
 * {@code EventHandlerEntity.livingDrops:453-461}). A fixture whose exact attributes, reward or
 * loot are asserted declares that draw here, so the scenario stays deterministic: the declared
 * value keeps the fixture an ordinary mob and the real world stream still advances by the
 * original single draw, so no other scenario loses a world draw.
 */
public final class ChampionJoinRoll {
    private ChampionJoinRoll() {}

    /** The declared original roll: far above the promotion threshold of the original. */
    public static final int DECLARED = 97;

    /**
     * Adds the entity to the level with the declared champion join roll.
     *
     * @return whether the native level admitted the entity.
     */
    public static boolean join(ServerLevel level, Entity entity) {
        // The original rolled once for every hostile mob that joined the world and never for
        // anything else, so the declared draw count follows the original rule exactly. The
        // expectation is read before the join, while the mob still carries its base state.
        int expected = rolls(entity);
        var roll = new Roll(level);
        boolean added;
        try {
            added = level.addFreshEntity(entity);
        } finally {
            roll.close();
        }
        roll.require(expected);
        return added;
    }

    /** The original join roll of a hostile mob that was never rolled before is one nextInt100. */
    private static int rolls(Entity entity) {
        if (!(entity instanceof Mob mob) || !(mob instanceof Enemy)) return 0;
        return ChampionHooks.championId(mob) == ChampionHooks.NOT_ROLLED ? 1 : 0;
    }

    /** The scoped world-RNG input that declares the single original champion join roll. */
    private static final class Roll implements AutoCloseable {
        private final ServerLevel level;
        private final Field field;
        private final RandomSource previous;
        private final RandomSource input;
        private int draws;

        Roll(ServerLevel level) {
            this.level = level;
            this.previous = level.random;
            this.input = new LegacyRandomSource(0) {
                @Override public int nextInt() { return previous.nextInt(); }
                @Override public int nextInt(int bound) {
                    // The original champion join roll is the single nextInt100 of the world random.
                    // The real stream still advances by that one draw, only the declared value wins.
                    if (bound == 100) { draws++; previous.nextInt(100); return DECLARED; }
                    return previous.nextInt(bound);
                }
                @Override public long nextLong() { return previous.nextLong(); }
                @Override public boolean nextBoolean() { return previous.nextBoolean(); }
                @Override public float nextFloat() { return previous.nextFloat(); }
                @Override public double nextDouble() { return previous.nextDouble(); }
                @Override public double nextGaussian() { return previous.nextGaussian(); }
            };
            try {
                field = Level.class.getDeclaredField("random");
                field.setAccessible(true);
                field.set(level, input);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(failure);
            }
        }

        /** The original join roll of a mob is exactly one nextInt100 of the world random. */
        void require(int expected) {
            if (draws != expected) throw new AssertionError("the original champion join roll is exactly " + expected + " nextInt100, actual=" + draws);
        }

        @Override public void close() {
            try { field.set(level, previous); }
            catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
        }
    }
}
