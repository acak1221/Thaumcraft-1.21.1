package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original 1.7 champion modifier effect reduced to a pure decision. The original
 * performed the effect against live entities; the port keeps the decision free of a
 * world so it can be unit tested, and {@code ChampionRules.Adapter} applies the
 * result to the world.
 */
public interface IChampionModifierEffect {
    /** Original trigger kind: the original never performed such a modifier. */
    int TRIGGER_ATTRIBUTE = -1;
    /** Original trigger kind: the original performed it on every living update. */
    int TRIGGER_TICK = 0;
    /** Original trigger kind: the original performed it when the champion hit a living target. */
    int TRIGGER_ATTACK = 1;
    /** Original trigger kind: the original performed it when a living attacker hurt the champion. */
    int TRIGGER_DAMAGED = 2;

    /** Original status effects a champion inflicted on its target. */
    enum Status { NONE, WEAKNESS, HUNGER, POISON }

    /** Original client particle kinds. */
    enum Particle { NONE, GENERIC, SPARK, SLIME_JUMP }

    /** Immutable snapshot of the original performEffect inputs. */
    record Context(float amount, boolean unblockable, boolean thorns, boolean targetPresent, boolean targetIsPlayer,
                   int ticksExisted, float health, float baseMaxHealth, float absorption, boolean invulnerable) {
        /** Context of a champion that a living attacker hurts. */
        public static Context damaged(float amount, boolean unblockable, boolean thorns, boolean targetPresent, boolean targetIsPlayer,
                                      int ticksExisted, float health, float baseMaxHealth, float absorption, boolean invulnerable) {
            return new Context(amount, unblockable, thorns, targetPresent, targetIsPlayer, ticksExisted, health, baseMaxHealth, absorption, invulnerable);
        }
        /** Context of a champion that hits a living target. */
        public static Context attack(float amount, boolean targetIsPlayer,
                                     int ticksExisted, float health, float baseMaxHealth, float absorption, boolean invulnerable) {
            return new Context(amount, false, false, true, targetIsPlayer, ticksExisted, health, baseMaxHealth, absorption, invulnerable);
        }
        /** Context of a champion that ticks. */
        public static Context tick(int ticksExisted, float health, float baseMaxHealth, float absorption, boolean invulnerable) {
            return new Context(0.0f, false, false, false, false, ticksExisted, health, baseMaxHealth, absorption, invulnerable);
        }
    }

    /** What the adapter has to apply to the world; every field is an original value. */
    record Result(float amount, float heal, float absorption, float reflected, Status status, int statusDuration,
                  int fireSeconds, int warp, boolean spawnSpiderling) {
        /** The original returned the damage unchanged. */
        public static Result unchanged(Context context) { return new Result(context.amount(), 0.0f, 0.0f, 0.0f, Status.NONE, 0, 0, 0, false); }
        /** The original returned zero damage. */
        public static Result blocked() { return new Result(0.0f, 0.0f, 0.0f, 0.0f, Status.NONE, 0, 0, 0, false); }
        /** The original healed the champion. */
        public static Result healing(Context context, float heal) { return new Result(context.amount(), heal, 0.0f, 0.0f, Status.NONE, 0, 0, 0, false); }
        /** The original warded champion: the ward joins the absorption of the champion. */
        public static Result warding(Context context, float ward) { return new Result(context.amount(), 0.0f, ward, 0.0f, Status.NONE, 0, 0, 0, false); }
        /** The original inflicted a status effect on the target. */
        public static Result status(Context context, Status status, int duration) { return new Result(context.amount(), 0.0f, 0.0f, 0.0f, status, duration, 0, 0, false); }
    }

    /** The original client particle request, kept as data so the client can reproduce it. */
    record Fx(Particle kind, float x, float y, float z, float vx, float vy, float vz,
              float red, float green, float blue, float alpha, boolean loop,
              int start, int num, int inc, int age, int delay, float scale) {}

    /** The original effect, resolved without a world. */
    Result performEffect(Context context, Random random);

    /** The original client particles; null when the original showed nothing this time. */
    default Fx showFx(Random random, float width, float height) { return null; }
}
