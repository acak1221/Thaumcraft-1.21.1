package thaumcraft.common.testing;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftOuterLands;
import thaumcraft.common.entities.monster.EntityCultist;
import thaumcraft.common.entities.monster.EntityCultistCleric;
import thaumcraft.common.entities.monster.EntityCultistKnight;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.FamilyVariant;
import thaumcraft.common.tiles.TileEldritchAltar;

/** Real altar ticks on the solid floor used by EldritchRingFeature; no eyes or portal. */
public final class EldritchAltarSpawnRuntimeChecks {
    private EldritchAltarSpawnRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var lane = new WaveLane(test, 3, 2, 3);
        var level = lane.level; BlockPos origin = lane.origin;
        var area = new AABB(origin).inflate(24);
        boolean[] closed = {false};
        Runnable cleanup = () -> {
            if (closed[0]) return; closed[0] = true;
            if (level.getBlockEntity(origin) instanceof TileEldritchAltar altar) altar.setSpawner(false);
            owned(lane, area).forEach(EntityCultist::discard);
            lane.clean();
        };
        try {
            test.assertTrue(level.getDifficulty() != Difficulty.PEACEFUL, "non-peaceful altar fixture");
            lane.floor(4);
            var state = FamilyVariant.with(ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState(), EldritchBlockRules.ELDRITCH_ALTAR);
            lane.put(origin, state);
            var altar = (TileEldritchAltar) level.getBlockEntity(origin);
            test.assertTrue(ThaumcraftOuterLands.configureAltar(altar, true, EldritchBlockRules.ALTAR_SPAWN_TYPE_CULTIST),
                    "the world generator's real configuration arms a cultist altar");
            test.assertTrue(altar.getEyes() == 0, "cultist spawn needs no eyes or portal progression");
            var saved = altar.saveWithFullMetadata(level.registryAccess());
            var loaded = BlockEntity.loadStatic(origin, state, saved, level.registryAccess());
            test.assertTrue(loaded instanceof TileEldritchAltar copy && copy.isSpawner()
                            && copy.getSpawnType() == EldritchBlockRules.ALTAR_SPAWN_TYPE_CULTIST && copy.getEyes() == 0,
                    "generated altar flags survive the native block-entity codec");
            for (int i = 0; i < 4; i++) {
                BlockPos feet = corner(origin, i);
                test.assertTrue(level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                                && level.isEmptyBlock(feet),
                        "ritualist feet are air ABOVE a solid ring floor");
                lane.put(feet.above(), Blocks.STONE.defaultBlockState());
            }
            long[] since = {level.getGameTime()}; int[] phase = {0};
            test.onEachTick(() -> {
                if (closed[0]) return;
                try {
                    long elapsed = level.getGameTime() - since[0];
                    var own = owned(lane, area);
                    var clerics = own.stream().filter(EntityCultistCleric.class::isInstance).map(EntityCultistCleric.class::cast).toList();
                    switch (phase[0]) {
                        case 0 -> {
                            if (elapsed < 90) return;
                            test.assertTrue(own.isEmpty(), "ritualists cannot spawn through solid head-space blocks");
                            for (int i = 0; i < 4; i++) lane.put(corner(origin, i).above(), Blocks.AIR.defaultBlockState());
                            since[0] = level.getGameTime(); phase[0] = 1;
                        }
                        case 1 -> {
                            if (clerics.size() != 4) {
                                test.assertTrue(elapsed < 240, "armed zero-eye altar never spawned four ritualists above its solid floor: "
                                        + clerics.size() + " spawner=" + altar.isSpawner() + " type=" + altar.getSpawnType());
                                return;
                            }
                            for (var cleric : clerics) test.assertTrue(cleric.getIsRitualist() && cleric.isPersistenceRequired()
                                            && cleric.getRestrictCenter().equals(origin) && cleric.getRestrictRadius() == 8F,
                                    "native ritualists keep their altar, original home range and persistence");
                            since[0] = level.getGameTime(); phase[0] = 2;
                        }
                        case 2 -> {
                            if (own.stream().noneMatch(EntityCultistKnight.class::isInstance)) {
                                test.assertTrue(elapsed < 240, "living ritualists must permit the normal knight reinforcements");
                                return;
                            }
                            clerics.forEach(EntityCultistCleric::discard);
                            since[0] = level.getGameTime(); phase[0] = 3;
                        }
                        case 3 -> {
                            if (altar.isSpawner()) {
                                test.assertTrue(elapsed < 100, "removing all ritualists must stop the original reinforcement spawner");
                                return;
                            }
                            test.assertTrue(owned(lane, area).stream().noneMatch(EntityCultistCleric.class::isInstance),
                                    "stopped altar does not recreate the ritualists");
                            cleanup.run(); test.succeed();
                        }
                        default -> throw new IllegalStateException("Unknown altar test phase");
                    }
                } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
            });
            test.runAfterDelay(790, () -> {
                if (closed[0]) return;
                String detail = "altar fixture timed out at phase=" + phase[0] + " elapsed=" + (level.getGameTime() - since[0]);
                cleanup.run(); test.fail(detail);
            });
        } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
    }

    private static BlockPos corner(BlockPos origin, int i) {
        return origin.offset(EldritchBlockRules.altarClericOffsetX(i), 0, EldritchBlockRules.altarClericOffsetZ(i));
    }
    private static List<EntityCultist> owned(WaveLane lane, AABB area) {
        return lane.level.getEntitiesOfClass(EntityCultist.class, area, mob -> mob.getRestrictCenter().equals(lane.origin));
    }
}
