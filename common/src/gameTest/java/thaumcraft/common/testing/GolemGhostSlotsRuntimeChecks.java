package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.container.ContainerGolem;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;

/**
 * Fire upgrades give Fill/Empty/Gather/Use golems extra pages of six ghost slots. A real golem, a real connected player and
 * the real menu are explicit INPUTS; the slot count, the install, paging, clicks, persistence and the Gather decision are never
 * supplied. NOT a client/GUI, multiplayer or visual check: the pager drawing is unseen.
 */
public final class GolemGhostSlotsRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    // Tallow holds two upgrades (Iron only one), so it can carry the maximum of two Fire upgrades.
    private static final int TALLOW = 2, DEADLINE = 900;
    private static final int FIRE = GolemUpgrade.FIRE.id;
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    private GolemGhostSlotsRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var check = new Check(test);
        try {
            check.start();
            check.pagesFollowFire();
            check.menuPaging();
            check.persistence();
            check.coresThatFireDoesNotEnlarge();
            check.startGather();
        } catch (RuntimeException failure) {
            check.finish();
            throw failure;
        }
        test.onEachTick(() -> {
            if (check.finished) return;
            try {
                if (check.gatherDone()) { check.finish(); test.succeed(); }
            } catch (RuntimeException failure) {
                check.finish();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (check.finished) return;
            String state = check.gatherState();
            check.finish();
            test.fail("the Gather golem did not pick up the page-two exemplar within " + DEADLINE + " ticks: " + state);
        });
    }

    private static final class Check {
        private final GameTestHelper test;
        private final ServerLevel level;
        private final ServerPlayer player;
        private final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        private final EmbeddedChannel channel = new EmbeddedChannel(connection);
        private final BlockPos base;
        private final Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        private final Set<Integer> existing = new HashSet<>();
        private final List<Entity> spawned = new ArrayList<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private EntityGolemBase gatherer;
        private ItemEntity wantedItem, otherItem;
        private boolean touched, finished;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-4, -2, -4), base.offset(8, 5, 4))) {
                original.put(mutable.immutable(), level.getBlockState(mutable));
            }
            level.getAllEntities().forEach(entity -> existing.add(entity.getId()));
            var profile = new GameProfile(UUID.randomUUID(), "slots-" + UUID.randomUUID().toString().substring(0, 8));
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        }

        private void start() {
            level.getServer().getPlayerList().placeNewPlayer(connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            test.assertTrue(player.level() == level && level.getServer().getPlayerList().getPlayer(player.getUUID()) == player,
                    "an actual connected player is an explicit input");
            player.setNoGravity(true);
            touched = true;
            // The Gather golem's pathfinder refuses to plan across unloaded chunks.
            var center = new ChunkPos(base);
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                var chunk = new ChunkPos(center.x + dx, center.z + dz);
                if (!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk); }
            }
            for (BlockPos mutable : original.keySet()) {
                level.setBlock(mutable, mutable.getY() == base.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
            }
            test.assertTrue(level.mayInteract(player, base), "fixture is not under unrelated spawn protection");
        }

        private EntityGolemBase golem(int core, List<Integer> upgrades, BlockPos at) {
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(TALLOW);
            golem.applyConfiguration(new GolemData(core, NO_FILTERS, NO_COUNTS, false, List.of(), GolemUseOptions.DEFAULT, upgrades));
            golem.setPos(Vec3.atBottomCenterOf(at));
            golem.setHome(at, Direction.UP);
            test.assertTrue(level.addFreshEntity(golem), "the golem is admitted");
            spawned.add(golem);
            return golem;
        }

        private InteractionResult installFire(EntityGolemBase golem) {
            player.setGameMode(GameType.SURVIVAL);
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumcraftGolems.upgradeItem(GolemUpgrade.FIRE)));
            player.setPos(Vec3.atBottomCenterOf(golem.blockPosition()).add(1.2, 0, 0));
            return golem.interact(player, InteractionHand.MAIN_HAND);
        }

        private void pagesFollowFire() {
            var fill = golem(GolemData.FILL, List.of(), base);
            test.assertTrue(fill.getSlotCount() == 6, "a fresh Fill golem has one page of six ghost slots");
            fill.setFilter(7, new ItemStack(Items.COBBLESTONE));
            test.assertTrue(fill.getFilter(7).isEmpty() && fill.getWantedCount(7) == 0, "a slot beyond the page cannot be set before the Fire upgrade");
            test.assertTrue(installFire(fill) == InteractionResult.CONSUME && fill.getUpgradeAmount(GolemUpgrade.FIRE) == 1 && fill.getSlotCount() == 12,
                    "one Fire upgrade installed by a real player makes twelve slots");
            fill.setFilter(7, new ItemStack(Items.COBBLESTONE));
            fill.setWantedCount(7, 64);
            test.assertTrue(fill.getFilter(7).is(Items.COBBLESTONE) && fill.getWantedCount(7) == 64, "slot seven is usable on the second page");
            test.assertTrue(installFire(fill) == InteractionResult.CONSUME && fill.getSlotCount() == 18, "a second Fire upgrade makes eighteen slots");
            test.assertTrue(installFire(fill) == InteractionResult.PASS && fill.getSlotCount() == 18,
                    "no third Fire upgrade is accepted, so eighteen is the maximum");
            fill.setFilter(0, new ItemStack(Items.STONE));
            fill.setWantedCount(0, 8);
            fill.setFilter(17, new ItemStack(Items.DIRT));
            fill.setWantedCount(17, 3);
            fill.setFilter(18, new ItemStack(Items.GOLD_INGOT));
            test.assertTrue(fill.getFilter(0).is(Items.STONE) && fill.getWantedCount(0) == 8 && fill.getFilter(17).is(Items.DIRT)
                            && fill.getWantedCount(17) == 3 && fill.getFilter(7).is(Items.COBBLESTONE) && fill.getFilter(18).isEmpty(),
                    "the first, middle and last slots hold their own exemplars; slot eighteen does not exist");
        }

        private EntityGolemBase fill() {
            for (Entity entity : spawned) if (entity instanceof EntityGolemBase golem && golem.getGolemCore() == GolemData.FILL) return golem;
            throw new IllegalStateException("no fill golem");
        }

        private void menuPaging() {
            var fill = fill();
            player.setPos(Vec3.atBottomCenterOf(fill.blockPosition()).add(1.2, 0, 0));
            var menu = new ContainerGolem(901, player.getInventory(), fill);
            player.containerMenu = menu;
            try {
                test.assertTrue(menu.pageCount() == 3 && menu.page() == 0 && menu.hasGhostFilters() && menu.ghostSlotCount() == 6,
                        "the menu offers three pages of six ghosts on page one: " + menu.pageCount());
                test.assertTrue(menu.getSlot(0).getItem().is(Items.STONE) && menu.getWantedCount(0) == 8 && menu.getSlot(1).getItem().isEmpty(),
                        "page one shows slot zero");
                test.assertTrue(menu.clickMenuButton(player, ContainerGolem.NEXT_PAGE) && menu.page() == 1, "the next button opens page two");
                test.assertTrue(menu.getSlot(1).getItem().is(Items.COBBLESTONE) && menu.getWantedCount(1) == 64 && menu.getSlot(0).getItem().isEmpty(),
                        "page two index one is golem slot seven (cobblestone, wanted 64)");
                test.assertTrue(menu.clickMenuButton(player, ContainerGolem.NEXT_PAGE) && menu.page() == 2
                                && menu.getSlot(5).getItem().is(Items.DIRT) && menu.getWantedCount(5) == 3,
                        "page three index five is golem slot seventeen (dirt, wanted 3)");
                test.assertTrue(!menu.clickMenuButton(player, ContainerGolem.NEXT_PAGE) && menu.page() == 2, "there is no fourth page");
                // A real ghost click on page three writes the golem's slot twelve and nothing on the other pages.
                menu.setCarried(new ItemStack(Items.GOLD_INGOT, 5));
                menu.clicked(0, 0, ClickType.PICKUP, player);
                test.assertTrue(fill.getFilter(12).is(Items.GOLD_INGOT) && fill.getWantedCount(12) == 5 && menu.getCarried().getCount() == 5,
                        "clicking the first ghost of page three sets golem slot twelve and never consumes the cursor: " + fill.getFilter(12));
                test.assertTrue(fill.getFilter(0).is(Items.STONE) && fill.getFilter(6).isEmpty() && fill.getFilter(7).is(Items.COBBLESTONE),
                        "the other pages are untouched");
                menu.setCarried(ItemStack.EMPTY);
                test.assertTrue(menu.clickMenuButton(player, ContainerGolem.PREVIOUS_PAGE) && menu.page() == 1
                                && menu.clickMenuButton(player, ContainerGolem.PREVIOUS_PAGE) && menu.page() == 0
                                && !menu.clickMenuButton(player, ContainerGolem.PREVIOUS_PAGE) && menu.page() == 0,
                        "the previous button walks back and stops at page one");
            } finally {
                menu.removed(player);
                player.containerMenu = player.inventoryMenu;
            }
        }

        private void persistence() {
            var fill = fill();
            var tag = new CompoundTag();
            fill.saveWithoutId(tag);
            ListTag saved = tag.getList("Inventory", Tag.TAG_COMPOUND);
            Set<Integer> slots = new HashSet<>();
            for (Tag row : saved) slots.add(((CompoundTag) row).getInt("Slot"));
            test.assertTrue(slots.equals(Set.of(0, 7, 12, 17)) && tag.getIntArray("WantedCounts").length == 18,
                    "NBT keeps every populated slot index and eighteen wanted counts: " + slots + " / " + tag.getIntArray("WantedCounts").length);
            var reloaded = ThaumcraftGolems.GOLEM.get().create(level);
            reloaded.load(tag.copy());
            test.assertTrue(reloaded.getSlotCount() == 18 && reloaded.getFilter(17).is(Items.DIRT) && reloaded.getWantedCount(12) == 5
                            && reloaded.getFilter(7).is(Items.COBBLESTONE) && reloaded.configuredData().filters().size() == 18,
                    "reloading restores all eighteen slots and the placer data lists them");
            // The golem becomes a placer and back without losing a page.
            var data = fill.configuredData();
            var copy = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            copy.setGolemMaterial(TALLOW);
            copy.applyConfiguration(data);
            test.assertTrue(copy.getSlotCount() == 18 && copy.getFilter(12).is(Items.GOLD_INGOT) && copy.getWantedCount(12) == 5
                            && copy.getFilter(17).is(Items.DIRT) && copy.getUpgradeAmount(GolemUpgrade.FIRE) == 2,
                    "placer data applied to a new golem restores every page");
            // A golem saved with six entries that later gained Fire pages still loads (padded), without losing slot zero.
            var legacy = new CompoundTag();
            fill.saveWithoutId(legacy);
            ListTag first = new ListTag();
            for (Tag row : legacy.getList("Inventory", Tag.TAG_COMPOUND)) if (((CompoundTag) row).getInt("Slot") < 6) first.add(row);
            legacy.put("Inventory", first);
            int[] six = new int[6];
            System.arraycopy(legacy.getIntArray("WantedCounts"), 0, six, 0, 6);
            legacy.putIntArray("WantedCounts", six);
            var padded = ThaumcraftGolems.GOLEM.get().create(level);
            padded.load(legacy);
            test.assertTrue(padded.getSlotCount() == 18 && padded.getFilter(0).is(Items.STONE) && padded.getFilter(7).isEmpty()
                            && padded.configuredData().filters().size() == 18,
                    "a six-entry save on a Fire golem is padded, not rejected");
            // An entry outside the golem's pages is unsupported data, never silently dropped.
            var outside = new CompoundTag();
            fill.saveWithoutId(outside);
            ListTag rows = outside.getList("Inventory", Tag.TAG_COMPOUND);
            var stray = (CompoundTag) rows.get(0).copy();
            stray.putByte("Slot", (byte) 20);
            rows.add(stray);
            var refused = ThaumcraftGolems.GOLEM.get().create(level);
            refused.load(outside);
            boolean refusedFlattening;
            try { refused.configuredData(); refusedFlattening = false; } catch (IllegalStateException expected) { refusedFlattening = true; }
            test.assertTrue(refusedFlattening, "a ghost entry beyond the last page makes the golem unsupported instead of being truncated");
            reloaded.discard();
            copy.discard();
            padded.discard();
            refused.discard();
        }

        private void coresThatFireDoesNotEnlarge() {
            var harvest = golem(GolemData.HARVEST, List.of(FIRE, FIRE), base.east(3));
            var liquid = golem(GolemData.LIQUID, List.of(FIRE), base.east(5));
            var use = golem(GolemData.USE, List.of(FIRE), base.east(7));
            test.assertTrue(harvest.getSlotCount() == 6 && liquid.getSlotCount() == 6, "Harvest and Decanting keep one page even with Fire");
            test.assertTrue(use.getSlotCount() == 12, "a Use golem with one Fire has twelve whitelist slots");
            harvest.setFilter(0, new ItemStack(Items.STONE));
            test.assertTrue(harvest.getFilter(0).isEmpty(), "Harvest still has no ghost filters");
        }

        // ---- a real Gather golem honours a page-two exemplar ----------------------------------------------------

        private void startGather() {
            BlockPos at = base.offset(0, 0, 3);
            gatherer = golem(GolemData.GATHER, List.of(FIRE), at);
            gatherer.setFilter(8, new ItemStack(Items.COBBLESTONE));
            test.assertTrue(gatherer.getFilter(8).is(Items.COBBLESTONE) && gatherer.getFilter(0).isEmpty(),
                    "the Gather exemplar sits on page two only");
            wantedItem = loose(new ItemStack(Items.COBBLESTONE), at.offset(3, 0, 0));
            otherItem = loose(new ItemStack(Items.DIRT), at.offset(3, 0, 2));
        }

        private ItemEntity loose(ItemStack stack, BlockPos at) {
            var item = new ItemEntity(level, at.getX() + .5, at.getY() + .1, at.getZ() + .5, stack);
            item.setDeltaMovement(Vec3.ZERO);
            item.setPickUpDelay(0);
            item.setUnlimitedLifetime();
            test.assertTrue(level.addFreshEntity(item), "a loose item is admitted");
            spawned.add(item);
            return item;
        }

        private boolean gatherDone() {
            if (gatherer.getCarriedItem().isEmpty()) return false;
            test.assertTrue(gatherer.getCarriedItem().is(Items.COBBLESTONE),
                    "the Gather golem took the page-two exemplar, not whatever lay nearby: " + gatherer.getCarriedItem());
            test.assertTrue(otherItem.isAlive() && otherItem.getItem().is(Items.DIRT) && otherItem.getItem().getCount() == 1,
                    "the unfiltered dirt was left on the ground");
            return true;
        }

        private String gatherState() {
            return "carrying " + gatherer.getCarriedItem() + ", cobblestone alive=" + wantedItem.isAlive() + " at " + wantedItem.position()
                    + ", golem at " + gatherer.position();
        }

        private void finish() {
            if (finished) return;
            finished = true;
            try {
                for (Entity entity : spawned) entity.discard();
                level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(Vec3.atLowerCornerOf(base.offset(-6, -3, -6)),
                        Vec3.atLowerCornerOf(base.offset(10, 6, 8))), item -> !existing.contains(item.getId())).forEach(Entity::discard);
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
