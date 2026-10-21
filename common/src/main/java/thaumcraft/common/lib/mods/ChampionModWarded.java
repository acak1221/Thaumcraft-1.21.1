package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original warded champion: every twenty fifth tick it regained one point of its ward as
 * long as it was not hurt and its ward was below half of its maximum health.
 *
 * <p>Oracle {@code ChampionModWarded:25-30}: the 1.7 original raised the absorption of the
 * champion by one point, never its health, and it measured the ceiling of the ward against the
 * total of the max health attribute, so a champion of a spider regenerated its ward up to
 * twenty three.
 */
public final class ChampionModWarded implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (context.invulnerable() || context.ticksExisted() % 25 != 0) return Result.unchanged(context);
        int ward = (int) context.maxHealth() / 2;
        if (context.absorption() < (float) ward) return Result.warding(context, 1.0f);
        return Result.unchanged(context);
    }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextBoolean()) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, 0.0f, 0.0f,
                0.5f + random.nextFloat() * 0.1f, 0.5f + random.nextFloat() * 0.1f, 0.5f + random.nextFloat() * 0.1f, 0.6f, true,
                21, 4, 1, 4 + random.nextInt(4), 0, 0.8f + random.nextFloat() * 0.3f);
    }
}
