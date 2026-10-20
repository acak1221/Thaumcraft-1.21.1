package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original armoured champion: it only takes nineteen twenty-fifths of the damage it
 * would otherwise take, unless the damage bypasses armour.
 */
public final class ChampionModArmored implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (context.unblockable()) return Result.unchanged(context);
        return new Result(context.amount() * 19.0f / 25.0f, 0.0f, 0.0f, 0.0f, Status.NONE, 0, 0, 0, false);
    }
    @Override public Fx showFx(Random random, float width, float height) {
        if (random.nextInt(4) != 0) return null;
        float w = random.nextFloat() * width;
        float d = random.nextFloat() * width;
        float h = random.nextFloat() * height;
        return new Fx(Particle.GENERIC, w, h, d, 0.0f, 0.0f, 0.0f,
                0.9f, 0.9f, 0.9f + random.nextFloat() * 0.1f, 0.7f, false,
                112, 9, 1, 5 + random.nextInt(4), 0, 0.6f + random.nextFloat() * 0.2f);
    }
}
