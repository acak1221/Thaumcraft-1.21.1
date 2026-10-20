package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original spined champion: every wound that a living attacker gave it reflected one to
 * three thorns damage back at that attacker.
 */
public final class ChampionModSpined implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (!context.targetPresent() || context.thorns()) return Result.unchanged(context);
        return new Result(context.amount(), 0.0f, 0.0f, 1 + random.nextInt(3), Status.NONE, 0, 0, 0, false);
    }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextBoolean()) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        int particle = 176 + random.nextInt(4) * 3;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, 0.0f, 0.0f,
                0.5f + random.nextFloat() * 0.2f, 0.1f + random.nextFloat() * 0.2f, 0.1f + random.nextFloat() * 0.2f, 0.7f, false,
                particle, 3, 1, 3, 0, 1.2f + random.nextFloat() * 0.3f);
    }
}
