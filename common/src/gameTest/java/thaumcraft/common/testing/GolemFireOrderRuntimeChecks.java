package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.entities.golems.GolemHarvestPlatform;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;

/**
 * Fire and Order on real workers. Fire: a real Iron Guard golem walks to and strikes a real spider; the spider burns for the
 * golem's 4 s per Fire upgrade right after the first landed hit, and not at all for a Guard without the upgrade. Order: real
 * Iron Harvest golems break a real crop and the SAME call replants it from the real drops through the guarded placement
 * (Architectury PLACE, Fabric use callback, game rules, soil), paying one real seed per placement. The golems, crops,
 * soil, loose drops, spiders, protection listeners and the player are explicit INPUTS; targeting, walking, striking,
 * burning, breaking, the drop search, the push toward the golem, placement and the debit are never supplied. NOT a Butcher
 * fight (same strike path), Fishing, filter slots, colours, Guard target switches, a third-party claim mod, multiplayer,
 * save/reload of a running worker, network or visual check.
 */
public final class GolemFireOrderRuntimeChecks {
    /** Real separate loader listener supplied by the loader test module (the Fabric use-on-block callback). */
    public interface PlacementProbe extends AutoCloseable {
        /** From now on every consultation whose clicked block is in the region is vetoed. */
        void deny(Predicate<BlockPos> region);
        /** The clicked block of every consultation by the native golem actor, in order. */
        List<BlockPos> consulted();
        @Override void close();
    }

    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int DEADLINE = 2000;
    private static final int TALLOW = 2, IRON = 6;
    private static final int FIRE = GolemUpgrade.FIRE.id, ORDER = GolemUpgrade.ORDER.id, EMPTY = GolemUpgrade.EMPTY;
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    private GolemFireOrderRuntimeChecks() {}

    public static void run(GameTestHelper test, Supplier<PlacementProbe> probes) {
        var fixture = new Fixture(test, probes.get());
        try {
            fixture.start();
            fixture.installAndPersist();
            fixture.arm();
        } catch (RuntimeException failure) {
            fixture.finish();
            throw failure;
        }
        test.onEachTick(() -> {
            if (fixture.finished) return;
            try {
                if (fixture.poll()) { fixture.finish(); test.succeed(); }
            } catch (RuntimeException failure) {
                fixture.finish();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (fixture.finished) return;
            String state = fixture.describe();
            fixture.finish();
            test.fail("the Fire/Order workers did not all finish within " + DEADLINE + " ticks: " + state);
        });
    }

    private record Placed(BlockPos pos, BlockState state, String actor) {}

    private static final class Fixture {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final PlacementProbe probe;
        private final BlockPos base;
        private final ServerPlayer player;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private final EmbeddedChannel channel = new EmbeddedChannel(connection);
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final Set<Integer> existing = new HashSet<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private final List<Entity> entities = new ArrayList<>();
        private final List<Placed> placed = new ArrayList<>();
        private final List<Lane> lanes = new ArrayList<>();
        private Predicate<BlockPos> archDeny = pos -> false;
        private final BlockEvent.Place listener;
        private boolean registered, touched, finished;

        Fixture(GameTestHelper test, PlacementProbe probe) {
            this.test = test;
            this.probe = probe;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            level.getAllEntities().forEach(entity -> existing.add(entity.getId()));
            var profile = new GameProfile(UUID.randomUUID(), "fo-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
            listener = (world, pos, state, placer) -> {
                if (world != level || placer == null || !inAnyLane(pos)) return EventResult.pass();
                placed.add(new Placed(pos.immutable(), state, placer instanceof ServerPlayer actor ? actor.getGameProfile().getName() : "?"));
                return archDeny.test(pos) ? EventResult.interruptFalse() : EventResult.pass();
            };
        }

        // Lanes sit in a row along +z; Guard lanes are far apart so a Guard never sees the other lane's spider.
        private static final int[] HARVEST_Z = {0, 12, 24, 36, 48, 60};
        private static final int[] GUARD_Z = {110, 160};

        void start() {
            level.getServer().getPlayerList().placeNewPlayer(connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            test.assertTrue(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player,
                    "an actual connected player is an explicit input");
            player.setNoGravity(true);
            registered = true;
            BlockEvent.PLACE.register(listener);
            lanes.add(new SeedLane("H", base.offset(0, 0, HARVEST_Z[0]), Kind.PLAIN));
            lanes.add(new SeedLane("P1", base.offset(0, 0, HARVEST_Z[1]), Kind.ARCHITECTURY_DENY));
            lanes.add(new SeedLane("P2", base.offset(0, 0, HARVEST_Z[2]), Kind.FABRIC_DENY));
            lanes.add(new WheatLane("W", base.offset(0, 0, HARVEST_Z[3]), true));
            lanes.add(new WheatLane("C", base.offset(0, 0, HARVEST_Z[4]), false));
            lanes.add(new CocoaLane("K", base.offset(0, 0, HARVEST_Z[5])));
            lanes.add(new GuardLane("G1", base.offset(0, 0, GUARD_Z[0]), 1));
            lanes.add(new GuardLane("G0", base.offset(0, 0, GUARD_Z[1]), 0));
            // Pathfinding refuses to plan across unloaded chunks; force a +-4 chunk halo, release only what was forced here.
            Set<Long> wanted = new HashSet<>();
            for (Lane lane : lanes) {
                var center = new ChunkPos(lane.b);
                for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                    var chunk = new ChunkPos(center.x + dx, center.z + dz);
                    if (wanted.add(chunk.toLong()) && !level.getForcedChunks().contains(chunk.toLong())) {
                        level.setChunkForced(chunk.x, chunk.z, true);
                        forced.add(chunk);
                    }
                }
            }
            touched = true;
            test.assertTrue(level.mayInteract(player, base), "fixture is not under unrelated spawn protection");
        }

        boolean inAnyLane(BlockPos pos) {
            for (Lane lane : lanes) if (lane.contains(pos)) return true;
            return false;
        }

        // ---- shared helpers -------------------------------------------------------------------------------------

        EntityGolemBase worker(int material, int core, BlockPos home, List<Integer> upgrades) {
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(material);
            golem.applyConfiguration(new GolemData(core, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, upgrades));
            golem.setPos(Vec3.atBottomCenterOf(home));
            golem.setHome(home, Direction.UP);
            golem.setPersistenceRequired();
            test.assertTrue(level.addFreshEntity(golem) && golem.hasSupportedCore(), "a real worker is admitted: core " + core);
            entities.add(golem);
            return golem;
        }

        ItemEntity oldItem(ItemStack stack, Vec3 at) {
            var item = new ItemEntity(level, at.x, at.y, at.z, stack);
            item.setDeltaMovement(Vec3.ZERO);
            item.setPickUpDelay(32767);
            item.setUnlimitedLifetime();
            item.tickCount = 100; // Older than the replant push window: it must be left alone.
            test.assertTrue(level.addFreshEntity(item), "a loose drop is admitted");
            entities.add(item);
            return item;
        }

        void farmland(BlockPos pos) {
            level.setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), QUIET);
        }

        BlockState wheat(int age) { return Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, age); }

        boolean isWheat(BlockPos pos, int age) {
            BlockState state = level.getBlockState(pos);
            return state.is(Blocks.WHEAT) && state.getValue(CropBlock.AGE) == age;
        }

        int loose(BlockPos around, Item item) {
            return level.getEntitiesOfClass(ItemEntity.class, new AABB(around).inflate(8), entity -> !existing.contains(entity.getId()) && entity.isAlive()
                    && entity.getItem().is(item)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        }

        List<Placed> placedIn(Lane lane) {
            return placed.stream().filter(entry -> lane.contains(entry.pos())).toList();
        }

        // ---- Fire and Order through the native installation, persisted ------------------------------------------

        void installAndPersist() {
            BlockPos s = base.offset(0, 0, -16);
            for (BlockPos pos : BlockPos.betweenClosed(s.offset(-4, -2, -4), s.offset(5, 5, 4))) {
                original.put(pos.immutable(), level.getBlockState(pos));
            }
            for (BlockPos pos : original.keySet()) {
                level.setBlock(pos, pos.getY() == s.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
            }
            var tallow = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            tallow.setGolemMaterial(TALLOW);
            tallow.applyConfiguration(new GolemData(GolemData.NO_CORE, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, List.of()));
            tallow.setPos(Vec3.atBottomCenterOf(s));
            test.assertTrue(level.addFreshEntity(tallow), "a coreless Tallow golem is admitted");
            entities.add(tallow);
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setPos(Vec3.atBottomCenterOf(s).add(1.2, 0, 0));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftGolems.UPGRADE_FIRE.get()));
            test.assertTrue(tallow.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME && player.getMainHandItem().isEmpty()
                            && tallow.getUpgrade(0) == FIRE && tallow.getUpgrade(1) == EMPTY && tallow.getUpgradeAmount(GolemUpgrade.FIRE) == 1,
                    "a real player installs Fire into the first free slot and pays the item");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftGolems.UPGRADE_ORDER.get(), 2));
            test.assertTrue(tallow.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME && player.getMainHandItem().getCount() == 1
                            && tallow.getUpgrade(0) == FIRE && tallow.getUpgrade(1) == ORDER,
                    "Order takes the second slot");
            test.assertTrue(tallow.interact(player, InteractionHand.MAIN_HAND) == InteractionResult.PASS && player.getMainHandItem().getCount() == 1,
                    "both slots are full: refused without payment");
            var tag = new CompoundTag();
            tallow.saveWithoutId(tag);
            test.assertTrue(java.util.Arrays.equals(tag.getByteArray("upgrades"), new byte[]{2, 4}), "saved as the original bytes Fire = 2, Order = 4");
            var copy = ThaumcraftGolems.GOLEM.get().create(level);
            copy.load(tag.copy());
            test.assertTrue(copy.getUpgrade(0) == FIRE && copy.getUpgrade(1) == ORDER && copy.configuredData().upgrades().equals(List.of(FIRE, ORDER)),
                    "reloaded: both upgrades survive and the placer data lists them in slot order");
            copy.discard();
        }

        // ---- the concurrent lanes -------------------------------------------------------------------------------

        void arm() {
            for (Lane lane : lanes) lane.setup();
        }

        boolean poll() {
            boolean all = true;
            for (Lane lane : lanes) {
                if (!lane.done) lane.poll();
                all &= lane.done;
            }
            return all;
        }

        String describe() {
            StringBuilder out = new StringBuilder();
            for (Lane lane : lanes) out.append(lane.name).append(lane.done ? " done; " : " pending (" + lane.status() + "); ");
            return out.toString();
        }

        void finish() {
            if (finished) return;
            finished = true;
            try {
                if (registered) BlockEvent.PLACE.unregister(listener);
                probe.close();
                for (Entity entity : entities) entity.discard();
                level.getEntitiesOfClass(ItemEntity.class, new AABB(Vec3.atLowerCornerOf(base.offset(-12, -4, -30)), Vec3.atLowerCornerOf(base.offset(14, 10, 200))),
                        item -> !existing.contains(item.getId())).forEach(Entity::discard);
                if (touched) {
                    for (Lane lane : lanes) lane.restore();
                    original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                }
                for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
            } finally {
                try {
                    if (level.getServer().getPlayerList().getPlayer(player.getUUID()) == player) level.getServer().getPlayerList().remove(player);
                } finally { player.discard(); channel.finishAndReleaseAll(); }
            }
        }

        abstract class Lane {
            final String name;
            final BlockPos b;
            boolean done;
            private final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();

            Lane(String name, BlockPos b) {
                this.name = name;
                this.b = b;
                for (BlockPos pos : BlockPos.betweenClosed(b.offset(-6, -2, -5), b.offset(8, 5, 5))) saved.put(pos.immutable(), level.getBlockState(pos));
            }
            boolean contains(BlockPos pos) { return pos.getX() >= b.getX() - 6 && pos.getX() <= b.getX() + 8 && pos.getZ() >= b.getZ() - 5 && pos.getZ() <= b.getZ() + 5; }
            void prepareGround() {
                for (BlockPos pos : saved.keySet()) {
                    level.setBlock(pos, pos.getY() == b.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
                }
            }
            void restore() { saved.forEach((pos, state) -> level.setBlock(pos, state, QUIET)); }
            abstract void setup();
            abstract void poll();
            String status() { return ""; }
        }

        enum Kind { PLAIN, ARCHITECTURY_DENY, FABRIC_DENY }

        /** A melon is harvested while a real stack of seeds lies on the ground and farmland waits in three of the nine cells. */
        final class SeedLane extends Lane {
            private final Kind kind;
            private final BlockPos at;
            private ItemEntity seeds, stick;
            private EntityGolemBase golem;

            SeedLane(String name, BlockPos b, Kind kind) { super(name, b); this.kind = kind; at = b.offset(2, 0, 0); }
            private List<BlockPos> cells() { return List.of(at, at.south(), at.east()); }
            /**
             * Original order is the harvested cell first. 1.21 light is asynchronous: a cell that just held an opaque melon is
             * still dark for a tick or two, so the platform plants it LAST (a neighbour's shape update would otherwise delete the
             * crop). Both the original order and that adaptation are valid, depending on whether the light had already settled.
             */
            private boolean validOrder(List<BlockPos> order) {
                return order.equals(cells()) || order.equals(List.of(cells().get(1), cells().get(2), cells().get(0)));
            }

            @Override void setup() {
                prepareGround();
                for (BlockPos cell : cells()) farmland(cell.below());
                level.setBlock(at, Blocks.MELON.defaultBlockState(), QUIET);
                seeds = oldItem(new ItemStack(Items.WHEAT_SEEDS, 4), new Vec3(b.getX() + .5, b.getY() + .1, b.getZ() + 3.5));
                stick = oldItem(new ItemStack(Items.STICK), new Vec3(b.getX() - 1.5, b.getY() + .1, b.getZ() + 2.5));
                if (kind == Kind.ARCHITECTURY_DENY) archDeny = archDeny.or(this::contains);
                if (kind == Kind.FABRIC_DENY) probe.deny(this::contains);
                golem = worker(IRON, GolemData.HARVEST, b, List.of(ORDER));
            }
            @Override String status() { return "melon " + level.getBlockState(at).getBlock(); }

            @Override void poll() {
                if (level.getBlockState(at).is(Blocks.MELON)) return;
                test.assertTrue(level.getBlockState(at).isAir() || kind == Kind.PLAIN && isWheat(at, 0),
                        name + ": the melon was harvested (never undone by the replant step): " + level.getBlockState(at));
                List<Placed> mine = placedIn(this);
                List<BlockPos> asked = probe.consulted().stream().filter(this::contains).toList();
                switch (kind) {
                    case PLAIN -> {
                        for (BlockPos cell : cells()) test.assertTrue(isWheat(cell, 0), name + ": wheat age 0 was planted at " + cell + ": " + level.getBlockState(cell));
                        test.assertTrue(!level.getBlockState(at.west()).is(Blocks.WHEAT) && !level.getBlockState(at.north()).is(Blocks.WHEAT)
                                        && !level.getBlockState(at.south().east()).is(Blocks.WHEAT),
                                name + ": the cells without farmland were skipped, not forced");
                        test.assertTrue(seeds.isAlive() && seeds.getItem().getCount() == 1 && seeds.getItem().is(Items.WHEAT_SEEDS),
                                name + ": exactly one real seed was paid per placement (4 -> 1): " + seeds.getItem());
                        test.assertTrue(mine.size() == 3 && mine.stream().allMatch(entry -> entry.state().is(Blocks.WHEAT) && entry.actor().equals(GolemHarvestPlatform.MACHINE_NAME))
                                        && validOrder(mine.stream().map(Placed::pos).toList()),
                                name + ": the Architectury PLACE event saw three wheat placements by the native actor, in cell order (the harvested cell last while its light is stale): " + mine);
                        test.assertTrue(validOrder(asked.stream().map(BlockPos::above).toList()),
                                name + ": the Fabric use callback was consulted once per placement, on the soil below, in that same order: " + asked);
                        // The slices dropped by the break were fresh: given the replant push (a 0.075 lift and at most 0.25 sideways),
                        // not left with the vanilla 0.2 pop. The SIDEWAYS direction depends on where the walking golem stands
                        // relative to each random drop position, so it is not asserted here; GolemReplantTest proves it is toward the golem.
                        var slices = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3), e -> !existing.contains(e.getId()) && e.getItem().is(Items.MELON_SLICE));
                        test.assertTrue(!slices.isEmpty() && slices.stream().allMatch(e -> e.getDeltaMovement().y < 0.12
                                        && Math.hypot(e.getDeltaMovement().x, e.getDeltaMovement().z) <= 0.2501),
                                name + ": fresh drops were pushed with the replant nudge, not the vanilla pop: " + slices.stream().map(Entity::getDeltaMovement).toList());
                        test.assertTrue(stick.isAlive() && stick.getDeltaMovement().x == 0 && stick.getDeltaMovement().z == 0 && stick.getDeltaMovement().y <= 0,
                                name + ": an old, unplantable drop is neither pushed nor touched: " + stick.getDeltaMovement());
                    }
                    case ARCHITECTURY_DENY -> {
                        for (BlockPos cell : cells()) test.assertTrue(!level.getBlockState(cell).is(Blocks.WHEAT), name + ": the vetoed placement at " + cell + " did not happen");
                        test.assertTrue(seeds.getItem().getCount() == 4, name + ": no seed was paid for a vetoed placement: " + seeds.getItem());
                        test.assertTrue(!mine.isEmpty() && mine.stream().allMatch(entry -> entry.actor().equals(GolemHarvestPlatform.MACHINE_NAME)),
                                name + ": the veto listener was really asked, by the native actor: " + mine);
                        test.assertTrue(asked.isEmpty(), name + ": a vetoed placement never reaches the Fabric callback: " + asked);
                    }
                    case FABRIC_DENY -> {
                        for (BlockPos cell : cells()) test.assertTrue(!level.getBlockState(cell).is(Blocks.WHEAT), name + ": the vetoed placement at " + cell + " did not happen");
                        test.assertTrue(seeds.getItem().getCount() == 4, name + ": no seed was paid for a vetoed placement: " + seeds.getItem());
                        test.assertTrue(validOrder(asked.stream().map(BlockPos::above).toList()),
                                name + ": the Fabric callback vetoed each farmland cell in turn: " + asked);
                        test.assertTrue(mine.size() == 3, name + ": the Architectury event ran first for every cell: " + mine);
                    }
                }
                done = true;
            }
        }

        /** A ripe wheat crop with real soil: the natural flow, with or without Order. */
        final class WheatLane extends Lane {
            private final boolean order;
            private final BlockPos at;
            private long harvestedAt = -1;

            WheatLane(String name, BlockPos b, boolean order) { super(name, b); this.order = order; at = b.offset(2, 0, 0); }

            @Override void setup() {
                prepareGround();
                farmland(at.below());
                level.setBlock(at, wheat(7), QUIET);
                worker(IRON, GolemData.HARVEST, b, order ? List.of(ORDER) : List.of());
            }
            @Override String status() { return "crop " + level.getBlockState(at); }

            @Override void poll() {
                if (harvestedAt < 0) {
                    if (isWheat(at, 7)) return;
                    harvestedAt = test.getTick();
                    List<Placed> mine = placedIn(this);
                    if (order) {
                        test.assertTrue(isWheat(at, 0), name + ": the harvested wheat grew back at age 0 in the same step: " + level.getBlockState(at));
                        test.assertTrue(mine.size() == 1 && mine.getFirst().pos().equals(at) && mine.getFirst().state().is(Blocks.WHEAT),
                                name + ": exactly one real placement went through the permission event: " + mine);
                        int seedsLeft = loose(at, Items.WHEAT_SEEDS);
                        test.assertTrue(seedsLeft >= 0 && seedsLeft <= 3 && loose(at, Items.WHEAT) >= 1,
                                name + ": the break dropped the wheat and 1..4 seeds, one seed was planted: seeds left " + seedsLeft);
                        done = true;
                    } else {
                        test.assertTrue(level.getBlockState(at).isAir() && mine.isEmpty(), name + ": without Order the cell stays empty: " + level.getBlockState(at));
                        test.assertTrue(loose(at, Items.WHEAT_SEEDS) >= 1, name + ": and the seeds stay on the ground");
                    }
                } else if (!order && test.getTick() >= harvestedAt + 40) {
                    test.assertTrue(level.getBlockState(at).isAir() && placedIn(this).isEmpty() && loose(at, Items.WHEAT_SEEDS) >= 1,
                            name + ": forty ticks later still no replant: " + level.getBlockState(at));
                    done = true;
                }
            }
        }

        /** A ripe cocoa pod against its jungle log. */
        final class CocoaLane extends Lane {
            private final BlockPos at;

            CocoaLane(String name, BlockPos b) { super(name, b); at = b.offset(2, 0, 0); }

            private boolean ripe() {
                BlockState state = level.getBlockState(at);
                return state.is(Blocks.COCOA) && state.getValue(CocoaBlock.AGE) == 2;
            }
            @Override void setup() {
                prepareGround();
                level.setBlock(at.east(), Blocks.JUNGLE_LOG.defaultBlockState(), QUIET);
                level.setBlock(at, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, Direction.EAST).setValue(CocoaBlock.AGE, 2), QUIET);
                worker(IRON, GolemData.HARVEST, b, List.of(ORDER));
            }
            @Override String status() { return "pod " + level.getBlockState(at); }

            @Override void poll() {
                if (ripe()) return;
                BlockState state = level.getBlockState(at);
                test.assertTrue(state.is(Blocks.COCOA) && state.getValue(CocoaBlock.AGE) == 0 && state.getValue(CocoaBlock.FACING) == Direction.EAST,
                        name + ": the pod regrew at age 0 against the same log: " + state);
                int beans = loose(at, Items.COCOA_BEANS);
                test.assertTrue(beans == 2, name + ": a ripe pod drops three beans and one was planted: " + beans);
                List<Placed> mine = placedIn(this);
                test.assertTrue(mine.size() == 1 && mine.getFirst().pos().equals(at) && mine.getFirst().state().is(Blocks.COCOA), name + ": through the permission event: " + mine);
                done = true;
            }
        }

        /** A real Guard and a real spider that cannot move or fight back. */
        final class GuardLane extends Lane {
            private final int fire;
            private Spider spider;
            private long woundedAt = -1;

            GuardLane(String name, BlockPos b, int fire) { super(name, b); this.fire = fire; }

            @Override void setup() {
                prepareGround();
                // Absolute position: the golem lane is absolute, while GameTestHelper.relativePos/spawn are rotated with the test
                // structure and (rotated 180 degrees here) would put the spider hundreds of blocks away from its golem.
                spider = EntityType.SPIDER.create(level);
                spider.moveTo(Vec3.atBottomCenterOf(b.offset(3, 0, 0)));
                spider.removeFreeWill();
                spider.setPersistenceRequired();
                test.assertTrue(level.addFreshEntity(spider), "the target spider is admitted beside its golem");
                entities.add(spider);
                worker(IRON, GolemData.GUARD, b, fire > 0 ? List.of(FIRE) : List.of());
            }
            @Override String status() { return "spider health " + spider.getHealth() + ", fire ticks " + spider.getRemainingFireTicks(); }

            @Override void poll() {
                if (woundedAt < 0) {
                    if (!(spider.getHealth() < spider.getMaxHealth())) return;
                    woundedAt = test.getTick();
                    test.assertTrue(spider.isAlive(), name + ": the first landed hit did not kill the spider");
                    int ticks = spider.getRemainingFireTicks();
                    if (fire > 0) {
                        test.assertTrue(ticks > 40 && ticks <= 80 && spider.isOnFire(),
                                name + ": the spider burns for the 4 s of one Fire upgrade right after the first landed hit: " + ticks + " ticks");
                        done = true;
                    } else {
                        test.assertTrue(ticks <= 0 && !spider.isOnFire(), name + ": a Guard without Fire does not set the spider alight: " + ticks);
                    }
                } else if (fire == 0 && test.getTick() >= woundedAt + 60) {
                    test.assertTrue(!spider.isOnFire() && spider.getRemainingFireTicks() <= 0, name + ": and sixty ticks later it still does not burn");
                    done = true;
                }
            }
        }
    }
}
