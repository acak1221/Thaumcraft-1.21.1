package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original venomous champion: every hit poisoned the target for one hundred ticks with
 * a forty percent chance.
 */
public final class ChampionModPoison implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (random.nextFloat() < 0.4f) return Result.status(context, Status.POISON, 100);
        return Result.unchanged(context);
    }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextBoolean()) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, 0.02f, 0.0f,
                0.2f, 0.6f + random.nextFloat() * 0.1f, 0.2f + random.nextFloat() * 0.1f, 0.7f, false,
                147, 4, 1, 8 + random.nextInt(4), 0, 0.5f + random.nextFloat() * 0.2f);
    }
}
