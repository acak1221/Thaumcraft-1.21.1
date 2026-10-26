package thaumcraft.common.testing;

import java.lang.reflect.Proxy;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.WorldGenMound;

/** Exercise the actual loot-state factory with registered blocks, without a live-level RNG escape. */
public final class MoundRandomRuntimeChecks {
    private MoundRandomRuntimeChecks() {}

    public static void generationRandomIsIsolated(GameTestHelper test) {
        int[] draws = {0};
        RandomSource random = (RandomSource) Proxy.newProxyInstance(RandomSource.class.getClassLoader(),
                new Class<?>[]{RandomSource.class}, (proxy, method, args) -> {
                    if (method.getName().equals("nextFloat")) return draws[0]++ == 0 ? 0.299F : 0.3F;
                    throw new AssertionError("Unexpected generation RNG call: " + method.getName());
                });
        WorldGenLevel region = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getRandom")) return random;
                    throw new AssertionError("Mound escaped generation RNG through " + method.getName());
                });
        // Separate from the supplied feature RNG, as in TC4's two-stream algorithm.
        RandomSource featureRandom = RandomSource.create(42L);
        var crate = WorldGenMound.lootState(region, featureRandom, 2);
        var urn = WorldGenMound.lootState(region, featureRandom, 1);
        test.assertTrue(crate.is(ThaumcraftEldritchBlocks.LOOT_CRATE.get()), "Below 0.3 must select a crate");
        test.assertTrue(urn.is(ThaumcraftEldritchBlocks.LOOT_URN.get()), "At 0.3 must select an urn");
        test.assertTrue(crate.getValue(thaumcraft.common.blocks.BlockLoot.RARITY) == 2
                && urn.getValue(thaumcraft.common.blocks.BlockLoot.RARITY) == 1, "Loot rarity must survive");
        test.assertTrue(draws[0] == 2, "Exactly one generation-stream float per container");
        test.assertTrue(featureRandom.nextLong() == RandomSource.create(42L).nextLong(), "Feature stream must be untouched");
        test.succeed();
    }
}
