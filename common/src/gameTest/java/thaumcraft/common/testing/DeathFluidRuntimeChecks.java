package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.common.blocks.BlockFluidDeath;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.lib.MagicWard;

/** Real registered finite-fluid cells, native block scheduling and contact damage. */
public final class DeathFluidRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private DeathFluidRuntimeChecks() {}

    public static void conservationAndDamage(GameTestHelper test) {
        var fixture = new Fixture(test);
        try {
            fixture.open();
            fixture.states();
            fixture.transfers();
            fixture.damage();
            fixture.scheduledTicks();
        } catch (RuntimeException | Error failure) {
            fixture.close();
            throw failure;
        }
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos base;
        final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
        final List<ChunkPos> held = new ArrayList<>();
        int randomTicks;
        boolean opened, closed;

        Fixture(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 100, 3));
        }

        void open() {
            // Fluid flow needs only its seven immediate cells, not a golem's 48-block navigation ring.
            for (int x = Math.floorDiv(base.getX() - 4, 16); x <= Math.floorDiv(base.getX() + 4, 16); x++)
                for (int z = Math.floorDiv(base.getZ() - 4, 16); z <= Math.floorDiv(base.getZ() + 4, 16); z++) {
                    var chunk = new ChunkPos(x, z);
                    if (!level.getForcedChunks().contains(chunk.toLong())) {
                        level.setChunkForced(x, z, true);
                        held.add(chunk);
                    }
                    level.getChunk(x, z);
                }
            randomTicks = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
            opened = true;
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-3, -2, -3), base.offset(3, 2, 3))) {
                test.assertTrue(level.getBlockEntity(pos) == null, "isolated high-altitude fluid lane has no owned block entities");
                originals.put(pos.immutable(), level.getBlockState(pos));
            }
            reset();
        }

        void set(BlockPos pos, BlockState state) {
            test.assertTrue(originals.containsKey(pos), "fluid fixture writes only its snapshotted volume");
            level.setBlock(pos, state, QUIET);
        }

        void reset() {
            for (BlockPos pos : originals.keySet()) set(pos, Blocks.STONE.defaultBlockState());
        }

        BlockState fluid(int amount) {
            return ThaumcraftLegacyTiles.DEATH_FLUID.get().defaultBlockState().setValue(LiquidBlock.LEVEL, 4 - amount);
        }

        int amount(BlockPos pos) {
            var state = level.getBlockState(pos);
            return state.getBlock() instanceof BlockFluidDeath ? BlockFluidDeath.quanta(state.getValue(LiquidBlock.LEVEL)) : 0;
        }

        int stock() { return originals.keySet().stream().mapToInt(this::amount).sum(); }

        void tick(BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BlockFluidDeath) state.tick(level, pos, level.random);
        }

        void states() {
            for (int amount = 1; amount <= 4; amount++) {
                set(base, fluid(amount));
                var nativeFluid = level.getFluidState(base);
                test.assertTrue(nativeFluid.getAmount() == amount && nativeFluid.createLegacyBlock() == fluid(amount),
                        "registered death fluid round-trips the real finite amount " + amount);
                test.assertTrue(nativeFluid.getOwnHeight() == Math.min(1F, (amount + 1F) / 4F),
                        "Forge filledPercentage adds one quantum without reversing physical height " + amount);
                test.assertTrue(nativeFluid.getType().getTickDelay(level) == 7,
                        "native metadata reports original viscosity1500/200 tick delay");
            }
            reset();
        }

        void transfers() {
            set(base, fluid(4));
            for (Direction side : Direction.Plane.HORIZONTAL) set(base.relative(side), Blocks.AIR.defaultBlockState());
            tick(base);
            test.assertTrue(stock() == 4 && amount(base) == 1,
                    "real horizontal tick distributes the existing bucket4 instead of copying four three-quanta recipients");

            reset();
            set(base, fluid(4));
            set(base.below(), Blocks.AIR.defaultBlockState());
            tick(base);
            test.assertTrue(amount(base) == 0 && amount(base.below()) == 4 && stock() == 4,
                    "real downward transfer clears origin and moves all four quanta");

            reset();
            set(base, fluid(3));
            set(base.below(), fluid(3));
            tick(base);
            test.assertTrue(amount(base) == 2 && amount(base.below()) == 4 && stock() == 6,
                    "a partial lower cell merges up to capacity4 and retains exact overflow2 above");

            reset();
            set(base, fluid(1));
            for (Direction side : Direction.Plane.HORIZONTAL) set(base.relative(side), Blocks.AIR.defaultBlockState());
            tick(base);
            test.assertTrue(amount(base) == 1 && stock() == 1,
                    "the original last quantum persists on a solid floor instead of evaporating without a feeder");

            reset();
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                set(base.offset(x, 0, z), Blocks.AIR.defaultBlockState());
            set(base, fluid(4));
            for (int pass = 0; pass < 30; pass++) {
                var cells = originals.keySet().stream().filter(pos -> amount(pos) > 0).toList();
                for (BlockPos pos : cells) tick(pos);
                test.assertTrue(stock() == 4, "repeated real finite ticks conserve bucket4, pass " + pass);
            }
            reset();
        }

        void damage() {
            Cow cow = EntityType.COW.create(level);
            test.assertTrue(cow != null, "native registered cow is available");
            cow.setNoAi(true);
            cow.setNoGravity(true);
            cow.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
            test.assertTrue(level.addFreshEntity(cow), "real native contact target is admitted");
            try {
                set(base, fluid(4));
                float before = cow.getHealth();
                fluid(4).entityInside(level, base, cow);
                var source = cow.getLastDamageSource();
                test.assertTrue(source != null && source.is(DamageSourceThaumcraft.DISSOLVE)
                                && MagicWard.kind(source) == MagicWard.Kind.NONE && cow.getHealth() == before - 4F,
                        "native cell collision uses registered dissolve, bypasses magic wards and deals its four quanta");
            } finally {
                cow.discard();
            }
            reset();
        }

        void scheduledTicks() {
            BlockPos top = base.above();
            set(top, fluid(4));
            set(base, Blocks.AIR.defaultBlockState());
            set(base.below(), Blocks.AIR.defaultBlockState());
            // Native fluid ticks are an identity adapter; only the pending block tick spends stock.
            level.getFluidState(top).tick(level, top);
            test.assertTrue(amount(top) == 4 && amount(base) == 0, "native fluid callback cannot double-advance the finite block scheduler");
            test.runAfterDelay(5, () -> {
                try {
                    test.assertTrue(amount(top) == 4 && amount(base) == 0 && stock() == 4,
                            "ordinary server ticks wait for original seven-tick finite deadline");
                } catch (RuntimeException | Error failure) { close(); throw failure; }
            });
            test.runAfterDelay(8, () -> {
                try {
                    test.assertTrue(amount(top) == 0 && amount(base) == 4 && amount(base.below()) == 0 && stock() == 4,
                            "first native scheduled block tick moves precisely one complete finite cell");
                } catch (RuntimeException | Error failure) { close(); throw failure; }
            });
            test.runAfterDelay(16, () -> {
                try {
                    test.assertTrue(amount(top) == 0 && amount(base) == 0 && amount(base.below()) == 4 && stock() == 4,
                            "the newly filled destination receives its own later scheduled finite tick");
                    test.succeed();
                } finally { close(); }
            });
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            try {
                originals.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            } finally {
                try { if (opened) level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(randomTicks, level.getServer()); }
                finally { for (ChunkPos chunk : held) level.setChunkForced(chunk.x, chunk.z, false); }
            }
        }
    }
}
