package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original warped champion: every hit warped its player target by one to three with a
 * thirty three percent chance.
 */
public final class ChampionModWarp implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (random.nextFloat() < 0.33f && context.targetIsPlayer()) return new Result(context.amount(), 0.0f, 0.0f, 0.0f, Status.NONE, 0, 0, 1 + random.nextInt(3), false);
        return Result.unchanged(context);
    }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextBoolean()) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, 0.0f, 0.0f,
                0.8f + random.nextFloat() * 0.2f, 0.0f, 0.9f + random.nextFloat() * 0.1f, 0.7f, true,
                72, 8, 1, 10 + random.nextInt(4), 0, 0.6f + random.nextFloat() * 0.4f);
    }
}
