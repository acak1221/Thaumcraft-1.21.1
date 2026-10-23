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
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;

/** Live selector tests: no direct goal, transfer, damage, harvest or navigation calls. */
public final class GolemEndToEndRuntimeChecks {
    private GolemEndToEndRuntimeChecks() {}

    public static void carryAndRefusals(GameTestHelper test) {
        var f = new Fixture(test, 6);
        var workers = new ArrayList<EntityGolemBase>();
        var sources = new ArrayList<Container>();
        var sinks = new ArrayList<ChestBlockEntity>();
        int[] peak = new int[6];
        for (int i = 0; i < 6; i++) {
            BlockPos home = f.home(i);
            var sink = f.chest(home.west());
            BlockPos sourcePos = home.south(7);
            Container source;
            if (i == 5) {
                f.level.setBlock(sourcePos, Blocks.FURNACE.defaultBlockState(), Fixture.QUIET);
                source = (Container) f.level.getBlockEntity(sourcePos);
            } else source = f.chest(sourcePos);
            source.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
            if (i == 2) for (int slot = 0; slot < sink.getContainerSize(); slot++) sink.setItem(slot, new ItemStack(Items.DIRT, 64));
            var markers = i == 3 ? List.<GolemMarker>of() : List.of(i == 5
                    ? new GolemMarker(f.level.dimension().location(), sourcePos, Direction.DOWN, GolemMarker.ANY) : f.marker(sourcePos));
            workers.add(f.worker(GolemData.FILL, home, i == 1 ? List.of(GolemUpgrade.EARTH.id) : List.of(), markers,
                    i == 4 ? Items.DIAMOND : Items.COBBLESTONE, 40));
            sources.add(source); sinks.add(sink);
        }
        f.poll(1500, () -> {
            for (int i = 0; i < 6; i++) {
                int cargo = workers.get(i).getCarriedItem().getCount();
                peak[i] = Math.max(peak[i], cargo);
                test.assertTrue(count(sources.get(i), Items.COBBLESTONE) + count(sinks.get(i), Items.COBBLESTONE) + cargo == 64,
                        "lane " + i + " preserves all 64 real cobblestone");
                test.assertTrue(count(sinks.get(i), Items.COBBLESTONE) <= 40, "Fill never exceeds requested stock");
                if (i >= 3) test.assertTrue(count(sources.get(i), Items.COBBLESTONE) == 64 && cargo == 0,
                        "unlinked/wrong-filter/sided-inaccessible source remains untouched: lane " + i);
                if (i == 2) test.assertTrue(count(sinks.get(i), Items.DIRT) == 27 * 64 && count(sinks.get(i), Items.COBBLESTONE) == 0,
                        "full sink is never overwritten");
            }
            if (test.getTick() < 1000 || count(sinks.get(0), Items.COBBLESTONE) != 40 || count(sinks.get(1), Items.COBBLESTONE) != 40) return false;
            test.assertTrue(peak[0] == 8 && peak[1] == 16, "MEASURED live peak carry: plain=" + peak[0] + ", Earth=" + peak[1]);
            test.assertTrue(workers.get(0).getCarriedItem().isEmpty() && workers.get(1).getCarriedItem().isEmpty()
                    && count(sources.get(0), Items.COBBLESTONE) == 24 && count(sources.get(1), Items.COBBLESTONE) == 24,
                    "both Fill workers deliver exactly 40 then stop with 24 in their sources");
            Thaumcraft.LOG.info("G21 measured carry plain={} Earth={}", peak[0], peak[1]);
            return true;
        });
    }

    public static void gatherAndAir(GameTestHelper test) {
        var f = new Fixture(test, 2);
        var workers = new ArrayList<EntityGolemBase>();
        var sinks = new ArrayList<ChestBlockEntity>();
        var items = new ArrayList<ItemEntity>();
        long[] picked = {-1, -1};
        boolean[] walked = new boolean[2];
        for (int i = 0; i < 2; i++) {
            BlockPos home = f.home(i);
            sinks.add(f.chest(home.west()));
            workers.add(f.worker(GolemData.GATHER, home, i == 1 ? List.of(GolemUpgrade.AIR.id) : List.of(), List.of(), Items.COBBLESTONE, 1));
            items.add(f.loose(home.south(12), new ItemStack(Items.COBBLESTONE, 8)));
        }
        f.poll(1000, () -> {
            for (int i = 0; i < 2; i++) {
                var worker = workers.get(i);
                walked[i] |= worker.position().distanceToSqr(Vec3.atBottomCenterOf(f.home(i))) > 64;
                if (picked[i] < 0 && !items.get(i).isAlive()) picked[i] = test.getTick();
                int loose = items.get(i).isAlive() ? items.get(i).getItem().getCount() : 0;
                test.assertTrue(loose + worker.getCarriedItem().getCount() + count(sinks.get(i), Items.COBBLESTONE) == 8,
                        "Gather preserves eight actual items through pickup and return");
            }
            if (count(sinks.get(0), Items.COBBLESTONE) != 8 || count(sinks.get(1), Items.COBBLESTONE) != 8) return false;
            test.assertTrue(walked[0] && walked[1], "both workers physically walked more than eight blocks");
            test.assertTrue(picked[1] < picked[0], "MEASURED Air pickup faster: plain=" + picked[0] + ", Air=" + picked[1]);
            Thaumcraft.LOG.info("G21 measured pickup ticks plain={} Air={}", picked[0], picked[1]);
            return true;
        });
    }

    public static void harvestAndLumberDelivery(GameTestHelper test) {
        var f = new Fixture(test, 2);
        var sinks = new ArrayList<ChestBlockEntity>();
        var cutters = new ArrayList<EntityGolemBase>();
        boolean[] walked = new boolean[2];
        boolean[] collecting = {false};
        for (int i = 0; i < 2; i++) {
            BlockPos home = f.home(i), resource = home.south(4);
            sinks.add(f.chest(home.west()));
            cutters.add(f.worker(i == 0 ? GolemData.HARVEST : GolemData.LUMBER, home, List.of(), List.of(), null, 0));
            if (i == 0) f.level.setBlock(resource, Blocks.MELON.defaultBlockState(), Fixture.QUIET);
            else for (int y = 0; y < 3; y++) f.level.setBlock(resource.above(y), Blocks.OAK_LOG.defaultBlockState(), Fixture.QUIET);
        }
        f.poll(1500, () -> {
            for (int i = 0; i < 2; i++) walked[i] |= cutters.get(i).position().distanceToSqr(Vec3.atBottomCenterOf(f.home(i))) > 1;
            // Isolate production from collection rather than making the workers compete for one path.
            // Only observed native removals start collection; no drops or outcomes are supplied.
            if (!collecting[0] && f.level.getBlockState(f.home(0).south(4)).isAir()
                    && f.level.getBlockState(f.home(1).south(4)).isAir()
                    && f.level.getBlockState(f.home(1).south(4).above()).isAir()
                    && f.level.getBlockState(f.home(1).south(4).above(2)).isAir()) {
                for (int i = 0; i < 2; i++) {
                    BlockPos home = f.home(i);
                    var collector = f.worker(GolemData.GATHER, home.east(), List.of(), List.of(), i == 0 ? Items.MELON_SLICE : Items.OAK_LOG, 1);
                    collector.setHome(home, Direction.EAST);
                }
                collecting[0] = true;
            }
            if (test.getTick() == 1490) Thaumcraft.LOG.info("G21 harvest/lumber deadline diagnostic: melon={}, slices={}, logs={}, trunk={}/{}/{}, loose={}",
                    f.level.getBlockState(f.home(0).south(4)), count(sinks.get(0), Items.MELON_SLICE), count(sinks.get(1), Items.OAK_LOG),
                    f.level.getBlockState(f.home(1).south(4)), f.level.getBlockState(f.home(1).south(4).above()),
                    f.level.getBlockState(f.home(1).south(4).above(2)), f.level.getEntitiesOfClass(ItemEntity.class,
                            new AABB(Vec3.atLowerCornerOf(f.base.offset(-3, -2, -2)), Vec3.atLowerCornerOf(f.base.offset(6, 14, 26))))
                            .stream().map(e -> e.getItem() + " at " + e.position()).toList());
            if (count(sinks.get(0), Items.MELON_SLICE) == 0 || count(sinks.get(1), Items.OAK_LOG) < 3) return false;
            test.assertTrue(walked[0] && walked[1], "both cutters physically walked away from home to their resources");
            test.assertTrue(count(sinks.get(1), Items.OAK_LOG) == 3, "exactly three real logs arrive, no duplicated trunk drops");
            test.assertTrue(f.level.getBlockState(f.home(0).south(4)).isAir(), "Harvest really broke its fruit");
            for (int y = 0; y < 3; y++) test.assertTrue(f.level.getBlockState(f.home(1).south(4).above(y)).isAir(), "Lumber really felled its trunk");
            test.assertTrue(cutters.stream().allMatch(g -> g.getCarriedItem().isEmpty()),
                    "original Harvest/Lumber leave drops, not internal cargo; real Gather delivered them");
            return true;
        });
    }

    /** Native drops can rest at either edge of a cell, not just its center. */
    public static void gatherFractionalDropPositions(GameTestHelper test) {
        var f = new Fixture(test, 4);
        Item[] kinds = {Items.COBBLESTONE, Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT};
        var sinks = new ArrayList<ChestBlockEntity>();
        for (int i = 0; i < 4; i++) {
            BlockPos home = f.home(i);
            sinks.add(f.chest(home.west()));
            f.worker(GolemData.GATHER, home, List.of(), List.of(), kinds[i], 1);
            var item = f.loose(home.south(4), new ItemStack(kinds[i]));
            item.setNoGravity(true);
            item.setPos(home.getX() + (i % 2 == 0 ? .001 : .999), home.getY() + .1,
                    home.getZ() + (i < 2 ? 4.999 : 4.001));
        }
        f.poll(500, () -> {
            for (int i = 0; i < 4; i++) if (count(sinks.get(i), kinds[i]) != 1) return false;
            return true;
        });
    }

    public static void measuredWaterPickupRadius(GameTestHelper test) {
        var f = new Fixture(test, 4);
        double[] distances = {15.9, 16.1, 19.9, 20.1};
        Item[] kinds = {Items.COBBLESTONE, Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT};
        var workers = new ArrayList<EntityGolemBase>();
        var sinks = new ArrayList<ChestBlockEntity>();
        var items = new ArrayList<ItemEntity>();
        for (int i = 0; i < 4; i++) {
            BlockPos home = f.home(i);
            sinks.add(f.chest(home.west()));
            workers.add(f.worker(GolemData.GATHER, home, i >= 2 ? List.of(GolemUpgrade.WATER.id) : List.of(), List.of(), kinds[i], 1));
            var item = f.loose(home.south(20), new ItemStack(kinds[i]));
            item.setNoGravity(true);
            item.setPos(Vec3.atCenterOf(home).add(0, 0, distances[i]));
            items.add(item);
        }
        f.poll(1100, () -> {
            for (int i : new int[] {1, 3}) test.assertTrue(items.get(i).isAlive() && items.get(i).getItem().getCount() == 1
                    && workers.get(i).getCarriedItem().isEmpty() && count(sinks.get(i), kinds[i]) == 0,
                    "real resource outside the measured radius stays untouched at " + distances[i]);
            if (test.getTick() < 800 || count(sinks.get(0), kinds[0]) != 1 || count(sinks.get(2), kinds[2]) != 1) return false;
            test.assertTrue(!items.get(0).isAlive() && !items.get(2).isAlive(), "in-range resources were picked and delivered, not copied");
            Thaumcraft.LOG.info("G21 measured Gather pickup radius: plain includes 15.9, excludes 16.1; Water includes 19.9, excludes 20.1");
            return true;
        });
    }

    public static void usePlantsExactlyOneSeed(GameTestHelper test) {
        usePlants(test, false);
    }

    public static void useEmptySpacePlantsExactlyOneSeed(GameTestHelper test) {
        usePlants(test, true);
    }

    private static void usePlants(GameTestHelper test, boolean emptySpace) {
        var f = new Fixture(test, 1);
        BlockPos home = f.home(0), farmland = home.south(6).below();
        var source = f.chest(home.west());
        source.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 8));
        f.level.setBlock(farmland, Blocks.FARMLAND.defaultBlockState(), Fixture.QUIET);
        f.level.setBlock(farmland.east(), Blocks.WATER.defaultBlockState(), Fixture.QUIET);
        var worker = f.worker(GolemData.USE, home, List.of(),
                List.of(new GolemMarker(f.level.dimension().location(), emptySpace ? farmland.above() : farmland, Direction.UP, GolemMarker.ANY)), Items.WHEAT_SEEDS, 1);
        worker.setUseOptions(new GolemUseOptions(emptySpace, false, false));
        boolean[] walked = {false};
        f.poll(700, () -> {
            walked[0] |= worker.position().distanceToSqr(Vec3.atBottomCenterOf(home)) > 4;
            int remaining = count(source, Items.WHEAT_SEEDS) + worker.getCarriedItem().getCount();
            test.assertTrue(remaining >= 7 && remaining <= 8, "one marked block cannot consume multiple seeds: " + remaining);
            if (test.getTick() < 400) return false;
            test.assertTrue(f.level.getBlockState(farmland.above()).is(Blocks.WHEAT), "Use must plant real wheat: block="
                    + f.level.getBlockState(farmland.above()) + ", soil=" + f.level.getBlockState(farmland) + ", source="
                    + count(source, Items.WHEAT_SEEDS) + ", cargo=" + worker.getCarriedItem() + ", pos=" + worker.position()
                    + ", canWork=" + worker.canWork());
            test.assertTrue(walked[0], "Use physically walked more than two blocks from home");
            test.assertTrue(remaining == 7, "exactly ONE actual seed paid for one plant after a repeat-action window");
            return true;
        });
    }

    public static void butcherLeavesBreedingPair(GameTestHelper test) {
        var f = new Fixture(test, 1);
        BlockPos home = f.home(0);
        for (BlockPos pos : f.original.keySet()) {
            if (pos.getY() == home.getY() || pos.getY() == home.getY() + 1) {
                if (pos.getX() == home.getX() - 3 || pos.getX() == home.getX() + 5
                        || pos.getZ() == home.getZ() - 2 || pos.getZ() == home.getZ() + 15)
                    f.level.setBlock(pos, Blocks.GLASS.defaultBlockState(), Fixture.QUIET);
            }
        }
        var worker = f.worker(GolemData.BUTCHER, home, List.of(), List.of(), null, 0);
        var owner = f.player(home.north());
        worker.setOwnerName(owner.getGameProfile().getName());
        var herd = new ArrayList<Pig>();
        for (int i = 0; i < 3; i++) {
            var pig = EntityType.PIG.create(f.level);
            pig.setPos(Vec3.atBottomCenterOf(home.south(4).east(i)));
            pig.setPersistenceRequired(); pig.tickCount = i == 0 ? 1000 : 0;
            f.add(pig); herd.add(pig);
        }
        var bystander = EntityType.COW.create(f.level);
        bystander.setPos(Vec3.atBottomCenterOf(home.south(2))); f.add(bystander);
        boolean[] walked = {false};
        f.poll(1300, () -> {
            walked[0] |= worker.position().distanceToSqr(Vec3.atBottomCenterOf(home)) > 1;
            test.assertTrue(owner.isAlive() && owner.getHealth() == owner.getMaxHealth(), "Butcher never attacks its real connected survival owner");
            test.assertTrue(bystander.getHealth() == bystander.getMaxHealth(), "Butcher never attacks the single cow bystander");
            test.assertTrue(herd.get(1).getHealth() == herd.get(1).getMaxHealth() && herd.get(2).getHealth() == herd.get(2).getMaxHealth(),
                    "Butcher leaves two younger pigs completely unharmed");
            if (test.getTick() < 1000) return false;
            test.assertTrue(!herd.getFirst().isAlive() && walked[0], "Butcher selected, walked to, struck and killed the oldest surplus pig");
            return true;
        });
    }

    private static int count(Container container, Item item) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) if (container.getItem(slot).is(item)) total += container.getItem(slot).getCount();
        return total;
    }

    private static final class Fixture {
        static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        final GameTestHelper test;
        final ServerLevel level;
        final BlockPos base;
        final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        final List<Entity> entities = new ArrayList<>();
        final List<ServerPlayer> players = new ArrayList<>();
        final List<EmbeddedChannel> channels = new ArrayList<>();
        final List<ChunkPos> chunks;
        boolean closed;

        Fixture(GameTestHelper test, int lanes) {
            this.test = test; level = test.getLevel(); base = test.absolutePos(new BlockPos(3, 5, 3));
            for (int lane = 0; lane < lanes; lane++) {
                BlockPos home = home(lane);
                for (BlockPos pos : BlockPos.betweenClosed(home.offset(-3, -2, -2), home.offset(5, 4, 25))) original.put(pos.immutable(), level.getBlockState(pos));
            }
            chunks = GolemChunks.hold(level, original.keySet());
            original.keySet().forEach(pos -> level.setBlock(pos, (pos.getY() - base.getY() + 1) % 8 == 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET));
            BoreWorkRuntimeChecks.testInfo(test).addListener(new GameTestListener() {
                @Override public void testStructureLoaded(GameTestInfo info) {}
                @Override public void testPassed(GameTestInfo info, GameTestRunner runner) { close(); }
                @Override public void testFailed(GameTestInfo info, GameTestRunner runner) { close(); }
                @Override public void testAddedForRerun(GameTestInfo old, GameTestInfo next, GameTestRunner runner) { close(); }
            });
        }
        BlockPos home(int lane) { return base.above(lane * 8); }
        GolemMarker marker(BlockPos pos) { return new GolemMarker(level.dimension().location(), pos, Direction.NORTH, GolemMarker.ANY); }
        void add(Entity entity) { test.assertTrue(level.addFreshEntity(entity), "real fixture entity admitted"); entities.add(entity); }
        ChestBlockEntity chest(BlockPos pos) { level.setBlock(pos, Blocks.CHEST.defaultBlockState(), QUIET); return (ChestBlockEntity) level.getBlockEntity(pos); }
        EntityGolemBase worker(int core, BlockPos home, List<Integer> upgrades, List<GolemMarker> markers, Item filter, int wanted) {
            var filters = new ArrayList<>(Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY));
            var counts = new ArrayList<>(Collections.nCopies(GolemData.FILTER_SLOTS, 0));
            if (filter != null) { filters.set(0, new ItemStack(filter)); counts.set(0, wanted); }
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(2);
            golem.applyConfiguration(new GolemData(core, filters, counts, false, markers, GolemUseOptions.DEFAULT, upgrades));
            golem.setHome(home, Direction.EAST); golem.setPos(Vec3.atBottomCenterOf(home)); golem.setPersistenceRequired(); add(golem);
            GameTestStateGuard.watchWorker(golem, this::close);
            return golem;
        }
        ItemEntity loose(BlockPos pos, ItemStack stack) {
            var item = new ItemEntity(level, pos.getX() + .5, pos.getY() + .1, pos.getZ() + .5, stack);
            item.setDeltaMovement(Vec3.ZERO); item.setPickUpDelay(0); item.setUnlimitedLifetime(); add(item); return item;
        }
        ServerPlayer player(BlockPos pos) {
            var connection = new Connection(PacketFlow.SERVERBOUND);
            channels.add(new EmbeddedChannel(connection));
            var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "g21-" + UUID.randomUUID().toString().substring(0, 8)),
                    ClientInformation.createDefault());
            players.add(player);
            level.getServer().getPlayerList().placeNewPlayer(connection, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            player.setGameMode(GameType.SURVIVAL); player.setPos(Vec3.atBottomCenterOf(pos));
            return player;
        }
        void poll(int deadline, java.util.function.BooleanSupplier check) {
            test.onEachTick(() -> {
                if (closed) return;
                try { if (check.getAsBoolean()) { close(); test.succeed(); } }
                catch (RuntimeException | Error failure) {
                    Thaumcraft.LOG.error("G21 live assertion at tick {}: {}", test.getTick(), failure.getMessage());
                    close(); throw failure;
                }
            });
            test.runAtTickTime(deadline, () -> {
                if (closed) return;
                String state = entities.stream().filter(e -> e instanceof EntityGolemBase)
                        .map(e -> e.position() + " cargo=" + ((EntityGolemBase) e).getCarriedItem() + " " + GolemState.of((EntityGolemBase) e)).toList().toString();
                Thaumcraft.LOG.error("G21 live work deadline; workers={}", state);
                close(); test.fail("G21 live work deadline; see per-worker work witness in server log");
            });
        }
        void close() {
            if (closed) return; closed = true;
            entities.forEach(Entity::discard);
            for (ServerPlayer player : players) {
                if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                player.discard();
            }
            channels.forEach(EmbeddedChannel::finishAndReleaseAll);
            var box = new AABB(Vec3.atLowerCornerOf(base.offset(-4, -3, -3)), Vec3.atLowerCornerOf(base.offset(7, 52, 27)));
            level.getEntitiesOfClass(ItemEntity.class, box).forEach(Entity::discard);
            original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            chunks.forEach(chunk -> level.setChunkForced(chunk.x, chunk.z, false));
        }
    }
}
