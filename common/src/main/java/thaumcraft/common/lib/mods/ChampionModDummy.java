package thaumcraft.common.lib.mods;

import java.util.Random;

/**
 * Original dummy champion effect: it changes nothing. The original kept it as the
 * placeholder of a modifier that does nothing.
 */
public final class ChampionModDummy implements IChampionModifierEffect {
    @Override public Result performEffect(Context context, Random random) { return Result.unchanged(context); }
    @Override public Fx showFx(Random random, float width, float height) { return null; }
}
