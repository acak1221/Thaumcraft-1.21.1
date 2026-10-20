package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original bold champion: the original never performed its effect, the modifier only
 * gave the champion its thirty percent movement speed bonus.
 */
public final class ChampionModBold implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) { return Result.blocked(); }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextBoolean()) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height / 3.0f;
        return new Fx(Particle.SPARK, w, h, d, 0.0f, 0.0f, 0.0f,
                0.3f - random.nextFloat() * 0.1f, 0.0f, 0.8f + random.nextFloat() * 0.2f, 1.0f, false,
                0, 0, 0, 0, 0, 0.2f);
    }
}
