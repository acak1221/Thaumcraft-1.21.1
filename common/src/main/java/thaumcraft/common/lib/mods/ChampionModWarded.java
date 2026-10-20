package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original warded champion: every twenty fifth tick it healed itself for one health as
 * long as it was hurt and below half of its base max health.
 */
public final class ChampionModWarded implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (context.invulnerable() || context.ticksExisted() % 25 != 0) return Result.unchanged(context);
        int ward = (int) context.baseMaxHealth() / 2;
        if (context.health() < (float) ward) return Result.healing(context, 1.0f);
        return Result.unchanged(context);
    }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextBoolean()) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, 0.0f, 0.0f,
                0.5f + random.nextFloat() * 0.1f, 0.5f + random.nextFloat() * 0.1f, 0.5f + random.nextFloat() * 0.1f, 0.6f, true,
                21, 4, 1, 4 + random.nextInt(4), 0, 0.8f + random.nextFloat() * 0.3f);
    }
}
