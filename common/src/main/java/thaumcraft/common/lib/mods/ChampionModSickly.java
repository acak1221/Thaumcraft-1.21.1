package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original sickly champion: every hit starved the target for five hundred ticks with a
 * forty percent chance.
 */
public final class ChampionModSickly implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (random.nextFloat() < 0.4f) return Result.status(context, Status.HUNGER, 500);
        return Result.unchanged(context);
    }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextBoolean()) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, -0.02f, 0.0f,
                0.2f, 0.6f + random.nextFloat() * 0.1f, 0.2f + random.nextFloat() * 0.1f, 0.5f, false,
                1, 4, 2, 5 + random.nextInt(4), 0, 0.9f + random.nextFloat() * 0.3f);
    }
}
