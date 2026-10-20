package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original vampiric champion: every hit healed the champion for half the damage it
 * dealt, but never less than two health.
 */
public final class ChampionModVampire implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        return Result.healing(context, Math.max(2.0f, context.amount() / 2.0f));
    }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextFloat() > 0.2f) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, 0.0f, 0.0f,
                0.9f + random.nextFloat() * 0.1f, 0.0f, 0.0f, 0.9f, false,
                147, 4, 1, 8 + random.nextInt(4), 0, 0.5f + random.nextFloat() * 0.2f);
    }
}
