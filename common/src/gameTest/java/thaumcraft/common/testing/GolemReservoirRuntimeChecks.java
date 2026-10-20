package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemAspectCargo;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.tiles.TileAlembic;
import thaumcraft.common.tiles.TileEssentiaReservoir;
import thaumcraft.common.tiles.TileJar;

/** Explicit finite stock fixtures; real Alchemy goals choose, walk, debit and deliver. No private world or client is required. */
public final class GolemReservoirRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private GolemReservoirRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        Check check = new Check(test);
        try { check.prepare(); } catch (RuntimeException failure) { check.finish(); throw failure; }
        test.runAfterDelay(5, () -> {
            try { check.start(); } catch (RuntimeException failure) { check.finish(); throw failure; }
        });
        test.onEachTick(() -> {
            if (!check.started || check.finished) return;
            try {
                if (check.poll()) { check.finish(); test.succeed(); }
            } catch (RuntimeException failure) { check.finish(); throw failure; }
        });
        test.runAtTickTime(950, () -> {
            if (check.finished) return;
            String state = check.describe();
            check.finish();
            test.fail("Alchemy reservoir routes did not finish: " + state);
        });
    }

    private static final class Check {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos base;
        final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final List<EntityGolemBase> golems = new ArrayList<>();
        final List<TileEssentiaReservoir> reservoirs = new ArrayList<>();
        TileEssentiaReservoir mixed, partial, wrong, rejected, full, priority, earthSource, earthSink;
        TileJar water, fire, bypassed, preferred;
        TileAlembic alembic;
        EntityGolemBase collector, remainder, mismatch, quarantined, blocked, ranked, jarFirst, earth;
        boolean started, finished, resumed;
        long settled;

        Check(GameTestHelper test) { this.test = test; level = test.getLevel(); base = test.absolutePos(new BlockPos(3, 3, 3)); }

        void prepare() {
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, -1, -1), base.offset(8, 3, 22))) original.put(pos.immutable(), level.getBlockState(pos));
            forced.addAll(GolemChunks.hold(level, original.keySet()));
            original.forEach((pos, state) -> level.setBlock(pos, pos.getY() == base.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET));
        }

        BlockPos pos(int x, int z) { return base.offset(x, 0, z); }
        TileEssentiaReservoir reservoir(int x, int z, Direction face) {
            BlockPos pos = pos(x, z);
            level.setBlock(pos, ThaumcraftTubes.ESSENTIA_RESERVOIR.get().defaultBlockState(), QUIET);
            var tile = (TileEssentiaReservoir) level.getBlockEntity(pos);
            test.assertTrue(tile != null && tile.setFacing(face), "real reservoir has its own " + face + " port");
            reservoirs.add(tile);
            return tile;
        }
        TileJar jar(int x, int z, Aspect type, boolean labelled, int amount) {
            BlockPos pos = pos(x, z);
            level.setBlock(pos, ThaumcraftContent.JAR.get().defaultBlockState(), QUIET);
            var tile = (TileJar) level.getBlockEntity(pos);
            if (labelled) test.assertTrue(tile.setFilter(type), "jar label is set");
            if (amount > 0) test.assertTrue(tile.addToContainer(type, amount) == 0, "jar stock fixture");
            return tile;
        }
        EntityGolemBase golem(int z, List<BlockEntity> sinks, GolemAspectCargo cargo, boolean earthUpgrades) {
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(7); // Thaumium: 32, or 64 with two Earth upgrades.
            golem.applyConfiguration(new GolemData(GolemData.ALCHEMY,
                    Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY), Collections.nCopies(GolemData.FILTER_SLOTS, 0), false,
                    sinks.stream().map(tile -> new GolemMarker(level.dimension().location(), tile.getBlockPos(), Direction.UP)).toList(),
                    GolemUseOptions.DEFAULT, earthUpgrades ? List.of(GolemUpgrade.EARTH.id, GolemUpgrade.EARTH.id) : List.of()));
            golem.applyAspectCargo(cargo);
            golem.setHome(pos(1, z), Direction.EAST); // The actual source is west of the integer home.
            golem.setPos(Vec3.atBottomCenterOf(pos(1, z)));
            test.assertTrue(level.addFreshEntity(golem), "Alchemy golem admitted");
            golems.add(golem);
            return golem;
        }
        void stock(TileEssentiaReservoir tile, Aspect aspect, int amount) {
            test.assertTrue(tile.addToContainer(aspect, amount) == 0, "finite reservoir input accepted");
        }

        void start() {
            mixed = reservoir(0, 0, Direction.NORTH); // Not home-facing EAST: collection uses the reservoir's port.
            stock(mixed, Aspect.WATER, 11); stock(mixed, Aspect.FIRE, 7);
            water = jar(6, 0, Aspect.WATER, true, 1); fire = jar(6, 1, Aspect.FIRE, true, 1);
            collector = golem(0, List.of(water, fire), GolemAspectCargo.EMPTY, false);

            level.setBlock(pos(0, 3), ThaumcraftAlembics.ALEMBIC.get().defaultBlockState(), QUIET);
            alembic = (TileAlembic) level.getBlockEntity(pos(0, 3));
            test.assertTrue(alembic.addToContainer(Aspect.WATER, 8) == 0, "eight real alembic units");
            partial = reservoir(6, 3, Direction.DOWN); stock(partial, Aspect.FIRE, 252);
            remainder = golem(3, List.of(partial), GolemAspectCargo.EMPTY, false);

            wrong = reservoir(0, 6, Direction.SOUTH); stock(wrong, Aspect.WATER, 5); stock(wrong, Aspect.FIRE, 7);
            mismatch = golem(6, List.of(), new GolemAspectCargo(Aspect.FIRE, 3), false);

            rejected = reservoir(0, 9, Direction.WEST);
            var malformed = new CompoundTag(); malformed.putString("Aspects", "broken");
            rejected.loadWithComponents(malformed, level.registryAccess());
            quarantined = golem(9, List.of(), GolemAspectCargo.EMPTY, false);

            full = reservoir(6, 12, Direction.NORTH); stock(full, Aspect.FIRE, 256);
            blocked = golem(12, List.of(full, rejected), new GolemAspectCargo(Aspect.WATER, 8), false);

            bypassed = jar(2, 15, Aspect.WATER, true, 0);
            priority = reservoir(5, 15, Direction.WEST);
            ranked = golem(15, List.of(bypassed, priority), new GolemAspectCargo(Aspect.WATER, 8), false);

            preferred = jar(2, 18, Aspect.WATER, true, 1);
            var farther = reservoir(5, 18, Direction.EAST);
            jarFirst = golem(18, List.of(farther, preferred), new GolemAspectCargo(Aspect.WATER, 8), false);

            earthSource = reservoir(0, 21, Direction.DOWN); stock(earthSource, Aspect.AIR, 64);
            earthSink = reservoir(6, 21, Direction.DOWN);
            earth = golem(21, List.of(earthSink), GolemAspectCargo.EMPTY, true);
            test.assertTrue(earth.getCarryLimit() == 64, "two Earth upgrades preserve the real 64-unit carry limit");

            test.assertTrue(EssentiaTransfers.collectReservoir(collector, mixed, Aspect.WATER, 11).committedUnits() == 0
                            && EssentiaTransfers.deliverReservoir(ranked, priority, Aspect.WATER, 8).committedUnits() == 0,
                    "public executor calls without the private live goal scope cannot move stock");
            started = true;
        }

        boolean poll() {
            test.assertTrue(earthSource.containerContains(Aspect.AIR) + earthSink.containerContains(Aspect.AIR) + cargo(earth, Aspect.AIR) == 64,
                    "all sixty-four units stay in the upgraded golem or one of its two reservoirs");
            test.assertTrue(mixed.containerContains(Aspect.WATER) + water.getAmount() + cargo(collector, Aspect.WATER) == 12
                            && mixed.containerContains(Aspect.FIRE) + fire.getAmount() + cargo(collector, Aspect.FIRE) == 8,
                    "both aspects are conserved during the mixed route");
            test.assertTrue(alembic.getAmount() + partial.containerContains(Aspect.WATER) + cargo(remainder, Aspect.WATER) == (resumed ? 4 : 8),
                    "partial delivery preserves the exact unpaid remainder, after declared drain when resumed");
            test.assertTrue(partial.containerContains(Aspect.FIRE) == 252, "another stored aspect is untouched");
            test.assertTrue(wrong.containerContains(Aspect.WATER) == 5 && wrong.containerContains(Aspect.FIRE) == 7
                            && mismatch.getAspectCargo().equals(new GolemAspectCargo(Aspect.FIRE, 3)),
                    "a carried second aspect never skips past the reservoir's first aspect");
            test.assertTrue(rejected.isQuarantined() && quarantined.getEssentiaAmount() == 0 && full.getAmount() == 256
                            && blocked.getAspectCargo().equals(new GolemAspectCargo(Aspect.WATER, 8)),
                    "full and quarantined endpoints grant and consume nothing");
            test.assertTrue(bypassed.getAmount() == 0, "a reservoir outranks even a nearer empty labelled jar");
            test.assertTrue(reservoirs.stream().filter(tile -> tile.getBlockPos().equals(pos(5, 18))).findFirst().orElseThrow().getAmount() == 0,
                    "a nearer nonempty labelled jar wins within the reservoir's priority tier");

            if (!resumed && partial.getAmount() == 256 && remainder.getEssentiaAmount() == 4) {
                CompoundTag saved = remainder.saveWithoutId(new CompoundTag());
                var restored = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
                restored.load(saved);
                test.assertTrue(restored.getAspectCargo().equals(new GolemAspectCargo(Aspect.WATER, 4)), "real residual cargo survives entity NBT");
                // Explicit external removal creates four free spaces. The same golem must finish the paid delivery once.
                test.assertTrue(partial.takeFromContainer(Aspect.WATER, 4), "declared external drain removes four water units");
                resumed = true;
            }
            boolean done = resumed && water.getAmount() == 12 && fire.getAmount() == 8 && mixed.getAmount() == 0
                    && partial.getAmount() == 256 && remainder.getEssentiaAmount() == 0 && alembic.getAmount() == 0
                    && priority.containerContains(Aspect.WATER) == 8 && ranked.getEssentiaAmount() == 0
                    && preferred.getAmount() == 9 && jarFirst.getEssentiaAmount() == 0 && earthSink.containerContains(Aspect.AIR) == 64
                    && earthSource.getAmount() == 0 && earth.getEssentiaAmount() == 0;
            if (!done) return false;
            if (settled == 0) settled = test.getTick() + 40;
            return test.getTick() >= settled;
        }
        int cargo(EntityGolemBase golem, Aspect aspect) { return golem.getEssentiaAspect() == aspect ? golem.getEssentiaAmount() : 0; }
        String describe() { return golems.stream().map(g -> g.getAspectCargo() + "@" + g.blockPosition().toShortString()).toList().toString(); }
        void finish() {
            if (finished) return;
            finished = true;
            golems.forEach(EntityGolemBase::discard);
            // No burst/flux during fixture cleanup: empty real reservoirs before restoring the old blocks.
            for (var tile : reservoirs) for (Aspect aspect : tile.getAspects().getAspects()) tile.takeFromContainer(aspect, tile.containerContains(aspect));
            original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }
}
