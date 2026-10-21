package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original grim champion: every hit withered the target for two hundred ticks with a
 * forty percent chance. The original effect is {@code Potion.field_82731_v} (wither), not
 * weakness; the port's own death gaze uses the same field as {@code MobEffects.WITHER}.
 */
public final class ChampionModGrim implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (random.nextFloat() < 0.4f) return Result.status(context, Status.WITHER, 200);
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
