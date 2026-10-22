package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;
import thaumcraft.common.items.golems.GolemFluidCargo;
import thaumcraft.common.tiles.TileCrucible;

/**
 * The Decanting golem's Entropy queue on real content. A real connected player, three real Thaumium golems with a marker each, real
 * crucibles and real water sources in three walled shafts are explicit INPUTS; which source block each golem drains, in which order and
 * how much water arrives where is decided by the golems. The shafts stand side by side on one small footprint (neighbouring
 * GameTests wipe the ground around their own structures): a nineteen-block shaft with Entropy (range 16: the farthest in-range sources
 * go first and the marked block and the out-of-range tail stay), a five-block shaft with Entropy (the marked block is drained last and
 * nothing is granted from nothing when the queue is spent) and a five-block shaft without Entropy (only the marked block, as before).
 * NOT a client/GUI, network or visual check, not another liquid than water.
 */
public final class GolemLiquidQueueRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int THAUMIUM = 7, DEADLINE = 900, SETTLE = 100, HEIGHT = 20;
    private static final int EARTH = GolemUpgrade.EARTH.id, ENTROPY = GolemUpgrade.ENTROPY.id;
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    private GolemLiquidQueueRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var check = new Check(test);
        try {
            check.start();
            check.armShafts();
        } catch (RuntimeException failure) {
            check.finish();
            throw failure;
        }
        test.onEachTick(() -> {
            if (check.finished) return;
            try {
                if (check.poll()) { check.finish(); test.succeed(); }
            } catch (RuntimeException failure) {
                Thaumcraft.LOG.error("Liquid queue shafts failed at tick {}: {} ({})", test.getTick(), failure.getMessage(), check.describe());
                check.finish();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (check.finished) return;
            String state = check.describe();
            check.finish();
            test.fail("the liquid shafts did not finish within " + DEADLINE + " ticks: " + state);
        });
    }

    /** A walled shaft of {@code cells} water sources whose top cell is marked, a golem on the wall beside it, a crucible beside the golem. */
    private static final class Shaft {
        final String name;
        final int centreX, cells, expectedPickups;
        final List<Integer> expectedOrder; // cell indices from the bottom (0) to the marked top (cells - 1)
        final List<Integer> removed = new ArrayList<>();
        final boolean[] wasSource;
        EntityGolemBase golem;
        TileCrucible crucible;

        Shaft(String name, int centreX, int cells, List<Integer> expectedOrder) {
            this.name = name;
            this.centreX = centreX;
            this.cells = cells;
            this.expectedOrder = expectedOrder;
            this.expectedPickups = expectedOrder.size();
            this.wasSource = new boolean[cells];
        }
    }

    private static final class Check {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final ServerPlayer player;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private final EmbeddedChannel channel = new EmbeddedChannel(connection);
        private final BlockPos base;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final List<Entity> spawned = new ArrayList<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private final List<Shaft> shafts = new ArrayList<>();
        private boolean touched, finished;
        private long settleUntil;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-2, -2, -2), base.offset(7, HEIGHT + 4, 3))) {
                original.put(mutable.immutable(), level.getBlockState(mutable));
            }
            var profile = new GameProfile(UUID.randomUUID(), "liquid-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        }

        private void start() {
            level.getServer().getPlayerList().placeNewPlayer(connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            test.assertTrue(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player,
                    "an actual connected player is an explicit input");
            player.setNoGravity(true);
            player.setGameMode(GameType.SURVIVAL);
            touched = true;
            forced.addAll(GolemChunks.hold(level, original.keySet()));
            for (BlockPos mutable : original.keySet()) {
                level.setBlock(mutable, mutable.getY() == base.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
            }
            test.assertTrue(level.mayInteract(player, base), "fixture is not under unrelated spawn protection");
        }

        private BlockPos cell(Shaft shaft, int index) { return base.offset(shaft.centreX, index, 0); }

        private boolean isSource(BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.is(Blocks.WATER) && state.getFluidState().isSource();
        }

        private Shaft shaft(String name, int centreX, int cells, int earth, boolean entropy, List<Integer> expectedOrder) {
            var shaft = new Shaft(name, centreX, cells, expectedOrder);
            // The walls: the ring around the shaft, up to its top cell. Neighbouring shafts share the wall between them.
            for (int y = 0; y < cells; y++) for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) level.setBlock(base.offset(centreX + dx, y, dz), Blocks.STONE.defaultBlockState(), QUIET);
            }
            for (int y = 0; y < cells; y++) {
                level.setBlock(cell(shaft, y), Blocks.WATER.defaultBlockState(), QUIET);
                shaft.wasSource[y] = true;
            }
            int top = cells - 1;
            var standing = base.offset(centreX + 1, top + 1, 0); // on the wall beside the marked top cell
            var crucible = standing.south();                       // the golem's home inventory: behind it, with the golem facing north
            level.setBlock(crucible, ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState(), QUIET);
            shaft.crucible = (TileCrucible) level.getBlockEntity(crucible);
            test.assertTrue(shaft.crucible != null && shaft.crucible.getWaterAmount() == 0, shaft.name + ": an empty crucible");
            var upgrades = new ArrayList<Integer>();
            for (int count = 0; count < earth; count++) upgrades.add(EARTH);
            if (entropy) upgrades.add(ENTROPY);
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(THAUMIUM);
            golem.applyConfiguration(new GolemData(GolemData.LIQUID, NO_FILTERS, NO_COUNTS, false,
                    List.of(new GolemMarker(OVERWORLD, cell(shaft, top), Direction.UP, GolemMarker.ANY)), GolemUseOptions.DEFAULT, upgrades));
            golem.setPos(Vec3.atBottomCenterOf(standing));
            golem.setHome(standing, Direction.NORTH);
            test.assertTrue(level.addFreshEntity(golem), "the golem is admitted");
            spawned.add(golem);
            shaft.golem = golem;
            test.assertTrue(golem.getUpgradeAmount(GolemUpgrade.ENTROPY) == (entropy ? 1 : 0) && golem.getRange() == 16f, shaft.name + ": configured, range 16");
            shafts.add(shaft);
            return shaft;
        }

        private static List<Integer> sequence(int from, int to) {
            var order = new ArrayList<Integer>();
            for (int index = from; index <= to; index++) order.add(index);
            return order;
        }

        private void armShafts() {
            // Capacity of the long-shaft golem: Thaumium with one Earth upgrade; the crucible takes one bucket, the golem the rest.
            int longBuckets = GolemFluidCargo.capacity(THAUMIUM, 1) / GolemFluidCargo.BUCKET + 1;
            // Range 16 from the marked top cell (index 18) reaches index 2: the farthest in-range sources go first, bottom up.
            shaft("long shaft, Entropy", 0, 19, 1, true, sequence(2, 2 + longBuckets - 1));
            // Five cells, all in range: bottom up, the marked top cell (index 4) last; five buckets fit (crucible + 4000).
            shaft("short shaft, Entropy", 2, 5, 0, true, sequence(0, 4));
            // No Entropy: only the marked top cell, exactly as before.
            shaft("short shaft, no Entropy", 4, 5, 0, false, List.of(4));
        }

        private boolean poll() {
            for (Shaft shaft : shafts) {
                for (int index = 0; index < shaft.cells; index++) {
                    if (shaft.wasSource[index] && !isSource(cell(shaft, index))) {
                        shaft.wasSource[index] = false;
                        shaft.removed.add(index);
                    }
                }
                // Whatever was drained so far is an exact prefix of the expected order: nothing out of order, nothing extra.
                List<Integer> prefix = shaft.expectedOrder.subList(0, Math.min(shaft.removed.size(), shaft.expectedPickups));
                test.assertTrue(shaft.removed.size() <= shaft.expectedPickups && shaft.removed.equals(prefix),
                        shaft.name + " drains " + shaft.expectedOrder + " in that order, not " + shaft.removed);
                int water = shaft.golem.getFluidAmountMb() + shaft.crucible.getWaterAmount();
                test.assertTrue(water == GolemFluidCargo.BUCKET * shaft.removed.size(),
                        shaft.name + ": every drained bucket is in the golem or the crucible, none conjured: " + water + " mB for " + shaft.removed.size());
            }
            if (settleUntil == 0) {
                if (shafts.stream().allMatch(shaft -> shaft.removed.size() == shaft.expectedPickups)) settleUntil = test.getTick() + SETTLE;
                return false;
            }
            if (test.getTick() < settleUntil) return false;
            for (Shaft shaft : shafts) {
                test.assertTrue(shaft.removed.equals(shaft.expectedOrder), shaft.name + " drained exactly " + shaft.expectedOrder + ": " + shaft.removed);
                test.assertTrue(shaft.golem.getFluidAmountMb() + shaft.crucible.getWaterAmount() == GolemFluidCargo.BUCKET * shaft.expectedPickups,
                        shaft.name + ": all water accounted for after the golem went idle: golem " + shaft.golem.getFluidAmountMb() + ", crucible " + shaft.crucible.getWaterAmount());
                test.assertTrue(shaft.crucible.getWaterAmount() == TileCrucible.CAPACITY, shaft.name + ": the golem filled the crucible first");
                for (int index = 0; index < shaft.cells; index++) {
                    boolean drained = shaft.expectedOrder.contains(index);
                    test.assertTrue(isSource(cell(shaft, index)) == !drained, shaft.name + ": cell " + index + (drained ? " was drained" : " is untouched"));
                }
            }
            return true;
        }

        private String describe() {
            var text = new StringBuilder();
            for (Shaft shaft : shafts) {
                text.append(shaft.name).append(" removed ").append(shaft.removed).append(" of ").append(shaft.expectedOrder)
                        .append(", golem ").append(shaft.golem.getFluidAmountMb()).append(" mB at ").append(shaft.golem.blockPosition().toShortString())
                        .append(", crucible ").append(shaft.crucible.getWaterAmount()).append(" mB; ");
            }
            return text.toString();
        }

        private void finish() {
            if (finished) return;
            finished = true;
            try {
                for (Entity entity : spawned) entity.discard();
                if (touched) original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            } finally {
                try {
                    if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                } finally { player.discard(); channel.finishAndReleaseAll(); }
            }
        }
    }
}
