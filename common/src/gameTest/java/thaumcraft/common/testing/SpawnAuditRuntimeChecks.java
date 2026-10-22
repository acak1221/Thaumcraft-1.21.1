package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.config.*;
import thaumcraft.common.lib.mods.ChampionHooks;

/** Queries the native generator pool, not a second copy of the spawn adapter. */
public final class SpawnAuditRuntimeChecks {
    private SpawnAuditRuntimeChecks() {}
    public static void pools(GameTestHelper test) {
        var level = test.getLevel();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        var pos = test.absolutePos(new BlockPos(1, 2, 1));
        var expected = java.util.Map.of(
                ThaumcraftBiomes.MAGICAL_FOREST, java.util.List.of("pech:10:1:1", "wisp:10:1:2"),
                Biomes.PLAINS, java.util.List.of("brainy_zombie:10:1:1"),
                ThaumcraftBiomes.EERIE, java.util.List.of("brainy_zombie:32:1:1", "giant_brainy_zombie:8:1:1", "eldritch_guardian:1:1:1", "wisp:3:1:1", "pech:10:1:1"),
                ThaumcraftBiomes.TAINT, java.util.List.of("taintacle:1:1:1", "pech:10:1:1"),
                ChampionHooks.ELDRITCH_LANDS, java.util.List.of("inhabited_zombie:1:1:1", "eldritch_guardian:1:1:1"),
                Biomes.NETHER_WASTES, java.util.List.of("firebat:10:1:2", "wisp:5:1:1"));
        expected.forEach((key, rows) -> {
            var pool = level.getChunkSource().getGenerator().getMobsAt(biomes.getHolderOrThrow(key), level.structureManager(), MobCategory.MONSTER, pos).unwrap();
            for (var row : rows) test.assertTrue(pool.stream().anyMatch(entry -> row.equals(
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entry.type).toString().replace("thaumcraft:", "")
                            + ":" + entry.getWeight().asInt() + ":" + entry.minCount + ":" + entry.maxCount)), "native spawn pool " + key + " contains " + row);
        });
        test.succeed();
    }

    public static void guardianIsolation(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(1, 2, 1));
        var oldFloor = level.getBlockState(pos.below());
        long oldTime = level.getDayTime();
        var first = ThaumcraftWarp.ELDRITCH_GUARDIAN.get().create(level);
        var candidate = ThaumcraftWarp.ELDRITCH_GUARDIAN.get().create(level);
        try {
            level.setDayTime(18000);
            level.updateSkyBrightness();
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 2);
            candidate.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
            test.assertTrue(candidate.checkSpawnRules(level, MobSpawnType.NATURAL), "isolated guardian admits natural spawn in the nighttime fixture");
            first.moveTo(candidate.getX() + 8, candidate.getY(), candidate.getZ(), 0, 0);
            test.assertTrue(level.addFreshEntity(first), "real neighbouring guardian added");
            test.assertTrue(ChampionHooks.championId(first) != ChampionHooks.NOT_ROLLED, "production entity-add event rolls champions without a test hook");
            test.assertTrue(!candidate.checkSpawnRules(level, MobSpawnType.NATURAL), "original guardian rejects another guardian within 32x16x32");
        } finally {
            first.discard(); candidate.discard();
            level.setBlock(pos.below(), oldFloor, 2);
            level.setDayTime(oldTime);
            level.updateSkyBrightness();
        }
        test.succeed();
    }

    public static void specialPaths(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(1, 2, 1));
        var swarmer = ThaumcraftTaint.TAINT_SWARMER.get().create(level);
        var inhabited = ThaumcraftEldritchMobs.INHABITED_ZOMBIE.get().create(level);
        var bounds = new net.minecraft.world.phys.AABB(pos).inflate(8);
        var before = new java.util.HashSet<java.util.UUID>();
        level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, bounds).forEach(mob -> before.add(mob.getUUID()));
        try {
            swarmer.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
            test.assertTrue(level.addFreshEntity(swarmer), "live taint swarmer installed");
            swarmer.setHealth(0);
            swarmer.tick();
            test.assertTrue(!level.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityTaintSwarm.class, bounds).isEmpty(), "real swarmer death tick produces a swarm");
            inhabited.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
            test.assertTrue(level.addFreshEntity(inhabited), "live inhabited zombie installed");
            inhabited.die(level.damageSources().generic());
            test.assertTrue(!level.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityEldritchCrab.class, bounds).isEmpty(), "real inhabited death produces an eldritch crab");
        } finally {
            swarmer.discard(); inhabited.discard();
            level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, bounds, mob -> !before.contains(mob.getUUID())).forEach(net.minecraft.world.entity.Entity::discard);
        }
        test.succeed();
    }

    public static void championNaturalLifecycle(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(1, 2, 1));
        var mob = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        long resumeSeed = level.random.nextLong();
        try {
            mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
            var facts = ChampionHooks.facts(level, mob);
            int tier = thaumcraft.common.lib.mods.ChampionRules.tierFor("minecraft:zombie", mob.getClass());
            float health = (float)mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue();
            long seed = 0;
            while (seed < 100000 && ChampionHooks.roll(net.minecraft.util.RandomSource.create(seed), facts, tier, health) < 0) seed++;
            test.assertTrue(seed < 100000, "bounded deterministic winning champion seed exists");
            level.random.setSeed(seed);
            test.assertTrue(level.addFreshEntity(mob), "naturally finalized zombie joins the real server world");
            test.assertTrue(ChampionHooks.championId(mob) >= 0 && mob.getMaxHealth() > health, "production join listener promotes the natural-lifecycle mob, including attributes");
        } finally {
            mob.discard(); level.random.setSeed(resumeSeed);
        }
        test.succeed();
    }
}
