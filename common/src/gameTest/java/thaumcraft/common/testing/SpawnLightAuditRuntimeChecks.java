package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LightLayer;
import thaumcraft.common.config.ThaumcraftMonsters;
import thaumcraft.common.config.ThaumcraftTaint;

public final class SpawnLightAuditRuntimeChecks {
    private SpawnLightAuditRuntimeChecks() {}
    public static void inhabitedPlacement(GameTestHelper test) {
        var level = test.getLevel();
        var pos = new BlockPos(test.absolutePos(BlockPos.ZERO).getX(), 200, test.absolutePos(BlockPos.ZERO).getZ());
        var floor = level.getBlockState(pos.below());
        long time = level.getDayTime();
        try {
            level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
            level.setDayTime(6000); level.updateSkyBrightness();
            test.assertTrue(!SpawnPlacements.checkSpawnRules(thaumcraft.common.config.ThaumcraftEldritchMobs.INHABITED_ZOMBIE.get(), level,
                    MobSpawnType.NATURAL, pos, RandomSource.create(0)), "inhabited zombie has a registered original monster daylight gate, not the default always-true placement");
            level.setDayTime(18000); level.updateSkyBrightness();
            for (long seed = 0; seed < 128; seed++) {
                var expected = RandomSource.create(seed);
                boolean allowed = level.getBrightness(LightLayer.SKY, pos) <= expected.nextInt(32)
                        && level.getMaxLocalRawBrightness(pos, level.isThundering() ? 10 : level.getSkyDarken()) <= expected.nextInt(8);
                var actual = RandomSource.create(seed);
                test.assertTrue(SpawnPlacements.checkSpawnRules(thaumcraft.common.config.ThaumcraftEldritchMobs.INHABITED_ZOMBIE.get(), level,
                        MobSpawnType.NATURAL, pos, actual) == allowed && actual.nextLong() == expected.nextLong(), "inhabited registered predicate keeps inherited original monster night rolls");
            }
        } finally {
            level.setBlock(pos.below(), floor, 2); level.setDayTime(time); level.updateSkyBrightness();
        }
        test.succeed();
    }

    public static void daylight(GameTestHelper test) {
        var level = test.getLevel();
        var pos = new BlockPos(test.absolutePos(BlockPos.ZERO).getX(), 200, test.absolutePos(BlockPos.ZERO).getZ());
        long time = level.getDayTime();
        var oldBlock = level.getBlockState(pos);
        var oldBiome = thaumcraft.common.lib.world.BiomeColumns.surfaceHolder(level, pos.getX(), pos.getZ()).unwrapKey().orElseThrow();
        try {
            level.setDayTime(6000); level.updateSkyBrightness();
            test.assertTrue(level.getBrightness(LightLayer.SKY, pos) == 15 && level.getBrightness(LightLayer.BLOCK, pos) == 0
                    && level.getRawBrightness(pos, level.getSkyDarken()) == 15, "native open-sky daylight has combined15 but block0");
            for (long seed = 0; seed < 64; seed++) {
                test.assertTrue(!SpawnPlacements.checkSpawnRules(ThaumcraftMonsters.FIREBAT.get(), level, MobSpawnType.NATURAL, pos, RandomSource.create(seed)), "original firebat nextInt7 rejects combined daylight15");
                test.assertTrue(!SpawnPlacements.checkSpawnRules(ThaumcraftTaint.TAINT_SWARM.get(), level, MobSpawnType.NATURAL, pos, RandomSource.create(seed)), "original swarm nextInt7 rejects combined daylight15");
                test.assertTrue(!SpawnPlacements.checkSpawnRules(ThaumcraftMonsters.WISP.get(), level, MobSpawnType.NATURAL, pos, RandomSource.create(seed)), "original wisp nextInt8 rejects combined daylight15");
            }
            level.setDayTime(18000); level.updateSkyBrightness();
            for (long seed = 0; seed < 128; seed++) {
                var expected = RandomSource.create(seed);
                boolean allowed = level.getBrightness(LightLayer.SKY, pos) <= expected.nextInt(32)
                        && level.getRawBrightness(pos, level.isThundering() ? 10 : level.getSkyDarken()) <= expected.nextInt(8);
                var actual = RandomSource.create(seed);
                test.assertTrue(SpawnPlacements.checkSpawnRules(ThaumcraftMonsters.WISP.get(), level, MobSpawnType.NATURAL, pos, actual) == allowed
                        && actual.nextLong() == expected.nextLong(), "original wisp uses saved SKY first, combined light second, preserving short-circuit RNG at night");
            }
            thaumcraft.common.lib.world.BiomeColumns.set(level, pos.getX(), pos.getZ(), thaumcraft.common.config.ThaumcraftBiomes.TAINT);
            level.setBlock(pos, ThaumcraftTaint.FIBRES.get().defaultBlockState().setValue(thaumcraft.common.blocks.BlockTaintFibres.TYPE, 0), 2 | 16);
            for (long seed = 0; seed < 128; seed++) {
                var expected = RandomSource.create(seed);
                boolean allowed = level.getBrightness(LightLayer.SKY, pos) <= expected.nextInt(32)
                        && level.getMaxLocalRawBrightness(pos, level.isThundering() ? 10 : level.getSkyDarken()) <= expected.nextInt(8)
                        && .5F - level.getLightLevelDependentMagicValue(pos) >= 0;
                var actual = RandomSource.create(seed);
                test.assertTrue(SpawnPlacements.checkSpawnRules(ThaumcraftTaint.TAINTACLE.get(), level, MobSpawnType.NATURAL, pos, actual) == allowed
                        && actual.nextLong() == expected.nextLong(), "original taintacle inherited gate uses combined light and short-circuit RNG in a real taint/fibre column");
            }
        } finally {
            level.setBlock(pos, oldBlock, 2 | 16);
            thaumcraft.common.lib.world.BiomeColumns.set(level, pos.getX(), pos.getZ(), oldBiome);
            level.setDayTime(time); level.updateSkyBrightness();
        }
        test.succeed();
    }
}
