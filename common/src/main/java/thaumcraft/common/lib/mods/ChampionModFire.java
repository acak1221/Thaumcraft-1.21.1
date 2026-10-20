package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original fiery champion: every hit set the target on fire for four seconds with a
 * forty percent chance.
 */
public final class ChampionModFire implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (random.nextFloat() < 0.4f) return new Result(context.amount(), 0.0f, 0.0f, Status.NONE, 0, 4, 0, false);
        return Result.unchanged(context);
    }
    @Override public Fx showFx(Random random, float width, float height) {
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, 0.03f, 0.0f,
                0.9f + random.nextFloat() * 0.1f, 1.0f, 1.0f, 0.7f, false,
                160, 10, 1, 8 + random.nextInt(4), 0, 0.7f + random.nextFloat() * 0.2f);
    }
}
