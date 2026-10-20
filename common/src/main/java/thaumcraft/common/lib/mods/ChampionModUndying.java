package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original undying champion: every twenty ticks it healed itself for one health.
 */
public final class ChampionModUndying implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (context.ticksExisted() % 20 == 0) return Result.healing(context, 1.0f);
        return Result.unchanged(context);
    }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextBoolean()) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, 0.03f, 0.0f,
                0.1f + random.nextFloat() * 0.1f, 0.8f + random.nextFloat() * 0.2f, 0.1f + random.nextFloat() * 0.1f, 0.9f, true,
                21, 4, 1, 4 + random.nextInt(4), 0, 0.5f + random.nextFloat() * 0.2f);
    }
}
