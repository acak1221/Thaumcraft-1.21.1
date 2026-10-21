package thaumcraft.common.testing;

import java.lang.reflect.Field;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import thaumcraft.common.lib.mods.ChampionHooks;
import thaumcraft.common.lib.mods.ChampionRules;

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
 *
 * <p>The original rolled the champion modifier of a promoted mob from the same world random
 * (oracle {@code EntityUtils.makeChampion:236}), so a fixture that wants a champion declares that
 * draw as well: the fixture names the original modifier it keeps and the real world stream still
 * advances by the original single draw.
 */
public final class ChampionJoinRoll {
    private ChampionJoinRoll() {}

    /** The declared original roll: far above the promotion threshold of the original. */
    public static final int DECLARED = 97;

    /**
     * The declared original roll of a mob that the original promoted: the lowest roll of the
     * original world random.
     *
     * <p>Oracle {@code EventHandlerEntity.entitySpawns:391-419}: the original rolled
     * {@code nextInt100} of the world random, shifted the roll by the difficulty, the dimension,
     * the biome and the dangerous location, subtracted the whitelist level of the mob from it and
     * promoted the mob when the result was zero or below and its base health reached ten. The
     * roll of zero is therefore the lowest roll that promotes a mob of the whitelist on a normal
     * world.
     */
    public static final int PROMOTED = 0;

    /**
     * Adds the entity to the level with the declared champion join roll. The mob stays an ordinary
     * mob of the original: the declared roll is far above the promotion threshold of the original.
     *
     * @return whether the native level admitted the entity.
     */
    public static boolean join(ServerLevel level, Entity entity) {
        return join(level, entity, DECLARED, ChampionRules.NO_CHAMPION);
    }

    /**
     * Adds the entity to the level with the declared champion join roll and the declared champion
     * modifier roll. The mob becomes a champion of the original and keeps exactly the declared
     * modifier.
     *
     * @param modifierId the id of the original champion modifier the promoted mob keeps.
     * @return whether the native level admitted the entity.
     */
    public static boolean join(ServerLevel level, Entity entity, int modifierId) {
        return join(level, entity, PROMOTED, modifierId);
    }

    /**
     * Declares the original champion join roll of every hostile mob that joins the world
     * inside the given action. The 1.7 original rolled {@code nextInt100} of the world random
     * for every {@code EntityMob} that joined the world (oracle
     * {@code EventHandlerEntity.entitySpawns:391}), so a fixture that asserts the exact
     * numbers of mobs the world itself spawns declares that draw for the whole window: the
     * declared value keeps every mob of the window an ordinary mob and the real world stream
     * still advances by the original single draw per mob, so no other scenario loses a world
     * draw.
     */
    public static <T> T declared(ServerLevel level, int roll, java.util.function.Supplier<T> action) {
        var declared = new Roll(level, roll, -1, null);
        try {
            return action.get();
        } finally {
            declared.close();
        }
    }

    /**
     * Declares the original roll of the champion effects of the given window.
     *
     * <p>The original rolled the effect of a champion from the same world random (oracle
     * {@code EntityUtils.makeChampion:236} and the modifier effects, for example
     * {@code ChampionModPoison}), so a fixture that asserts the effect of a champion declares
     * that draw here: the declared value keeps the effect of the original and the real world
     * stream still advances by the original single draw, so no other scenario loses a world draw.
     *
     * @param value the declared roll of the effect; a value under the original threshold of four
     *              in ten keeps the effect of the original.
     */
    public static void declaredEffect(ServerLevel level, float value, Runnable action) {
        var declared = new Roll(level, DECLARED, -1, value);
        try {
            action.run();
        } finally {
            declared.close();
        }
    }

    /**
     * Adds the entity to the level with the declared champion join roll and the declared champion
     * modifier roll.
     *
     * @return whether the native level admitted the entity.
     */
    private static boolean join(ServerLevel level, Entity entity, int roll, int modifierId) {
        // The original rolled once for every hostile mob that joined the world and never for
        // anything else, so the declared draw count follows the original rule exactly. The
        // expectation is read before the join, while the mob still carries its base state.
        int expected = rolls(entity);
        // The original rolled the modifier of a promoted mob from the same world random, but a
        // creeper never rolled one: the original always made a creeper champion bold.
        int expectedModifiers = modifierId < 0 || entity instanceof Creeper ? 0 : 1;
        var declared = new Roll(level, roll, modifierId, null);
        boolean added;
        try {
            added = level.addFreshEntity(entity);
        } finally {
            declared.close();
        }
        declared.require(expected, expectedModifiers);
        declared.requireChampion(entity, modifierId);
        return added;
    }

    /** The original join roll of a hostile mob that was never rolled before is one nextInt100. */
    private static int rolls(Entity entity) {
        if (!(entity instanceof Mob mob) || !(mob instanceof Enemy)) return 0;
        return ChampionHooks.championId(mob) == ChampionHooks.NOT_ROLLED ? 1 : 0;
    }

    /** The scoped world-RNG input that declares the original champion join and modifier rolls. */
    private static final class Roll implements AutoCloseable {
        private final ServerLevel level;
        private final Field field;
        private final RandomSource previous;
        private final RandomSource input;
        private final int roll;
        private final int modifierId;
        private final Float effect;
        private int draws;
        private int modifiers;

        Roll(ServerLevel level, int roll, int modifierId, Float effect) {
            this.level = level;
            this.roll = roll;
            this.modifierId = modifierId;
            this.effect = effect;
            this.previous = level.random;
            this.input = new LegacyRandomSource(0) {
                @Override public int nextInt() { return previous.nextInt(); }
                @Override public int nextInt(int bound) {
                    // The original champion join roll is the single nextInt100 of the world random.
                    // The real stream still advances by that one draw, only the declared value wins.
                    if (bound == 100) { draws++; previous.nextInt(100); return roll; }
                    // The original rolled the champion modifier of a promoted mob from the same
                    // world random, after the join roll, so the declared modifier wins.
                    if (bound == ChampionRules.modifierCount() && draws > 0 && modifierId >= 0) {
                        modifiers++; previous.nextInt(bound); return modifierId;
                    }
                    return previous.nextInt(bound);
                }
                @Override public long nextLong() { return previous.nextLong(); }
                @Override public boolean nextBoolean() { return previous.nextBoolean(); }
                @Override public float nextFloat() {
                    // The original rolled the effect of a champion from the same world random, so
                    // the declared effect wins while the real stream still advances by that draw.
                    float drawn = previous.nextFloat();
                    return effect == null ? drawn : effect;
                }
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
        void require(int expected, int expectedModifiers) {
            if (draws != expected) throw new AssertionError("the original champion join roll is exactly " + expected + " nextInt100, actual=" + draws);
            if (modifiers != expectedModifiers) throw new AssertionError("the original champion modifier roll is exactly " + expectedModifiers + " nextInt13, actual=" + modifiers);
        }

        /**
         * The declared roll has to reach the original decision: an ordinary mob stays ordinary and
         * a promoted mob keeps exactly the declared modifier.
         */
        void requireChampion(Entity entity, int modifierId) {
            if (!(entity instanceof Mob mob)) return;
            int actual = ChampionHooks.championId(mob);
            if (modifierId < 0) {
                if (actual >= 0) throw new AssertionError("the original champion join roll keeps the mob ordinary, actual modifier=" + actual);
            } else if (actual != modifierId) {
                throw new AssertionError("the original champion join roll promotes the mob with the declared modifier: expected="
                        + modifierId + " actual=" + actual);
            }
        }

        @Override public void close() {
            try { field.set(level, previous); }
            catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
        }
    }
}
