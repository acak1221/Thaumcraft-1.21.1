package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original grim champion: every hit weakened the target for two hundred ticks with a
 * forty percent chance.
 */
public final class ChampionModGrim implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (random.nextFloat() < 0.4f) return Result.status(context, Status.WEAKNESS, 200);
        return Result.unchanged(context);
    }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextBoolean()) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, -0.02f, 0.0f,
                random.nextFloat() * 0.2f, random.nextFloat() * 0.2f, random.nextFloat() * 0.2f, 0.8f, false,
                160, 10, 1, 8 + random.nextInt(4), 0, 0.6f + random.nextFloat() * 0.4f);
    }
}
