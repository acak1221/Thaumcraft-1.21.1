package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;

/**
 * Real Fishing golems cast their real lines into real ponds and the catches come out of the world. Twenty-four golems in three groups
 * (plain, Fire, Air) each fish a private pond; the golems, ponds and the world's own random numbers are INPUTS, the cast, the chance,
 * the loot, the optional second catch, the cooking and the delivery are never supplied. The run ends when each group has delivered
 * a few catches. Fire is checked as an invariant over whatever the lines bring in (no raw cod or salmon from a Fire golem, no cooked
 * fish from a golem without Fire); nothing here asserts a particular catch, because the world's random numbers decide those. The
 * exact windows of Order and Entropy are checked with injected random numbers in {@code GolemFishingUpgradesRuntimeChecks}. NOT
 * seen on a client, and the second Air catch may or may not occur in this run: it is reported, not asserted.
 */
public final class GolemFishingLiveRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int TALLOW = 2, DEADLINE = 5200, LANES_PER_GROUP = 8, EVENTS_PER_GROUP = 3, SPACING = 16;
    private static final int EARTH = GolemUpgrade.EARTH.id, FIRE = GolemUpgrade.FIRE.id, AIR = GolemUpgrade.AIR.id;

    private GolemFishingLiveRuntimeChecks() {}

    private enum Group { PLAIN(List.of(EARTH, EARTH)), FIERY(List.of(FIRE, EARTH)), AIRY(List.of(AIR, AIR));
        final List<Integer> upgrades;
        Group(List<Integer> upgrades) { this.upgrades = upgrades; }
    }

    public static void run(GameTestHelper test) {
        var fixture = new Fixture(test);
        try {
            fixture.start();
        } catch (RuntimeException failure) {
            fixture.finish();
            throw failure;
        }
        test.onEachTick(() -> {
            if (fixture.finished) return;
            try {
                if (fixture.poll()) { fixture.finish(); test.succeed(); }
            } catch (RuntimeException failure) {
                Thaumcraft.LOG.error("Fishing live check failed at tick {}: {} ({})", test.getTick(), failure.getMessage(), fixture.describe());
                fixture.finish();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (fixture.finished) return;
            String state = fixture.describe();
            fixture.finish();
            test.fail("the Fishing golems did not deliver enough catches within " + DEADLINE + " ticks: " + state);
        });
    }

    private static final class Fixture {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final BlockPos base;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private final List<Entity> spawned = new ArrayList<>();
        private final List<Lane> lanes = new ArrayList<>();
        private final Set<UUID> existing = new HashSet<>();
        private final Map<Group, Integer> events = new LinkedHashMap<>();
        private final Map<Group, Integer> casts = new LinkedHashMap<>();
        private int multiples;
        private final List<String> delivered = new ArrayList<>();
        private boolean touched, finished;

        Fixture(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            level.getAllEntities().forEach(entity -> existing.add(entity.getUUID()));
            for (Group group : Group.values()) { events.put(group, 0); casts.put(group, 0); }
        }

        void start() {
            int index = 0;
            for (Group group : Group.values()) {
                for (int lane = 0; lane < LANES_PER_GROUP; lane++) {
                    lanes.add(new Lane(group, index, base.offset(lane * SPACING, 0, 40 + index / LANES_PER_GROUP * SPACING)));
                }
                index += LANES_PER_GROUP;
            }
            for (Lane lane : lanes) for (BlockPos pos : lane.region()) original.put(pos.immutable(), level.getBlockState(pos));
            touched = true;
            for (Lane lane : lanes) {
                var center = new ChunkPos(lane.home);
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    var chunk = new ChunkPos(center.x + dx, center.z + dz);
                    if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk); }
                }
            }
            for (Lane lane : lanes) lane.build();
        }

        private final class Lane {
            final Group group;
            final BlockPos home;
            EntityGolemBase golem;
            boolean casting;
            final Set<UUID> seen = new HashSet<>();

            Lane(Group group, int index, BlockPos home) { this.group = group; this.home = home; }

            Iterable<BlockPos> region() { return BlockPos.betweenClosed(home.offset(-4, -6, -5), home.offset(10, 4, 5)); }
            AABB watch() { return new AABB(home.getX() - 4, home.getY() - 8, home.getZ() - 6, home.getX() + 12, home.getY() + 10, home.getZ() + 6); }

            void build() {
                int floor = home.getY() - 1;
                for (BlockPos pos : region()) {
                    BlockState state = pos.getY() <= floor ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    boolean pool = pos.getX() >= home.getX() + 2 && pos.getX() <= home.getX() + 6 && Math.abs(pos.getZ() - home.getZ()) <= 2
                            && pos.getY() >= floor - 2 && pos.getY() <= floor;
                    level.setBlock(pos, pool ? Blocks.WATER.defaultBlockState() : state, QUIET);
                }
                golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
                golem.setGolemMaterial(TALLOW);
                golem.applyConfiguration(new GolemData(GolemData.FISHING, Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY),
                        Collections.nCopies(GolemData.FILTER_SLOTS, 0), false, List.of(), GolemUseOptions.DEFAULT, group.upgrades));
                golem.setPos(Vec3.atBottomCenterOf(home));
                golem.setHome(home, Direction.UP);
                golem.setPersistenceRequired();
                test.assertTrue(level.addFreshEntity(golem) && golem.hasSupportedCore(), "a real Fishing golem is admitted: " + group);
                spawned.add(golem);
            }

            void poll() {
                test.assertTrue(golem.isAlive(), group + ": the Fishing golem is still alive");
                boolean now = golem.hasFishingCast();
                if (now && !casting) casts.merge(group, 1, Integer::sum);
                casting = now;
                List<ItemEntity> fresh = new ArrayList<>();
                for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, watch(), entity -> !existing.contains(entity.getUUID()))) {
                    if (seen.add(item.getUUID())) fresh.add(item);
                }
                if (fresh.isEmpty()) return;
                events.merge(group, 1, Integer::sum);
                if (fresh.size() > 1) multiples++;
                for (ItemEntity item : fresh) {
                    Item kind = item.getItem().getItem();
                    delivered.add(group + ":" + kind.getDescriptionId().replace("item.minecraft.", ""));
                    boolean raw = kind == Items.COD || kind == Items.SALMON;
                    boolean cooked = kind == Items.COOKED_COD || kind == Items.COOKED_SALMON;
                    if (group == Group.FIERY) {
                        test.assertTrue(!raw, "a Fire golem never delivers a raw fish: " + item.getItem());
                        // Original setFire(2): the catch leaves the pond alight (two seconds, so 40 ticks minus the ones already gone).
                        test.assertTrue(item.getRemainingFireTicks() > 0 && item.getRemainingFireTicks() <= 40, "a Fire golem's catch is alight: " + item.getItem()
                                + " with " + item.getRemainingFireTicks() + " ticks of flame");
                    } else {
                        test.assertTrue(!cooked, group + ": a golem without Fire never delivers cooked fish: " + item.getItem());
                        test.assertTrue(!item.isOnFire() && item.getRemainingFireTicks() <= 0, group + ": a catch without Fire is not alight: " + item.getItem()
                                + " with " + item.getRemainingFireTicks() + " ticks of flame");
                    }
                }
            }
        }

        boolean poll() {
            for (Lane lane : lanes) lane.poll();
            for (Group group : Group.values()) if (events.get(group) < EVENTS_PER_GROUP) return false;
            test.assertTrue(casts.values().stream().allMatch(count -> count > 0), "every group cast at least once: " + casts);
            Thaumcraft.LOG.info("Fishing live at tick {}: {}", test.getTick(), describe());
            return true;
        }

        String describe() {
            return "catch events " + events + ", casts started " + casts + ", multi-item events " + multiples + ", delivered " + delivered;
        }

        void finish() {
            if (finished) return;
            finished = true;
            for (Entity entity : spawned) entity.discard();
            for (Lane lane : lanes) {
                level.getEntitiesOfClass(ItemEntity.class, lane.watch(), entity -> !existing.contains(entity.getUUID())).forEach(Entity::discard);
                level.getEntitiesOfClass(thaumcraft.common.entities.golems.EntityGolemBobber.class, lane.watch(), entity -> true).forEach(Entity::discard);
            }
            if (touched) original.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
            for (ChunkPos chunk : forced) level.setChunkForced(chunk.x, chunk.z, false);
        }
    }
}
