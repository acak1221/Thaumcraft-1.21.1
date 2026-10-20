package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original infested champion: every wound that a living attacker gave it spawned a
 * tainted spiderling next to the champion with a forty percent chance.
 */
public final class ChampionModInfested implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) {
        if (random.nextFloat() < 0.4f) return new Result(context.amount(), 0.0f, 0.0f, Status.NONE, 0, 0, 0, true);
        return Result.unchanged(context);
    }
    @Override public Fx showFx(Random random, float width, float height) {
        if (!random.nextBoolean()) return null;
        return new Fx(Particle.SLIME_JUMP, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, false, 0, 0, 0, 0, 0, 0.0f);
    }
}
