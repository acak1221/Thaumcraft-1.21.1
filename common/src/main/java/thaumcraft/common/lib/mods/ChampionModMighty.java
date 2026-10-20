package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original mighty champion: the original never performed its effect, the modifier only
 * gave the champion its threefold attack damage bonus.
 */
public final class ChampionModMighty implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) { return Result.blocked(); }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextFloat() > 0.3f) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        int particle = 176 + random.nextInt(4) * 3;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, 0.0f, 0.0f,
                0.8f + random.nextFloat() * 0.2f, 0.8f + random.nextFloat() * 0.2f, 0.8f + random.nextFloat() * 0.2f, 0.7f, false,
                particle, 3, 1, 4 + random.nextInt(3), 0, 1.0f + random.nextFloat() * 0.3f);
    }
}
