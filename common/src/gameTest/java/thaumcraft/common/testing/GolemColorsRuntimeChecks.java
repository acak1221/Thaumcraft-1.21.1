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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.container.ContainerGolem;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;
import thaumcraft.common.items.golems.ItemGolemBell;

/**
 * Order marker and slot colours. A real golem, a real connected player, the real menu and the real bell are explicit INPUTS; the
 * colour state, the cycle, the persistence and the routing decision are never supplied. The last part runs a real Fill golem
 * between three chests and asserts WHICH chest it empties. NOT a client/GUI, multiplayer or visual check: the colour chips and
 * the action-bar line are unseen.
 */
public final class GolemColorsRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int TALLOW = 2, DEADLINE = 1200;
    private static final int FIRE = GolemUpgrade.FIRE.id, ORDER = GolemUpgrade.ORDER.id;
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    private GolemColorsRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var check = new Check(test);
        try {
            check.start();
            check.slotColoursNeedOrderAndCycleThroughTheMenu();
            check.markersAcceptTheRightSlots();
            check.theBellCyclesMarkerColours();
            check.coloursPersist();
            check.startRouting();
        } catch (RuntimeException failure) {
            check.finish();
            throw failure;
        }
        test.onEachTick(() -> {
            if (check.finished) return;
            try {
                if (check.routingDone()) { check.finish(); test.succeed(); }
            } catch (RuntimeException failure) {
                check.finish();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (check.finished) return;
            String state = check.routingState();
            check.finish();
            test.fail("the Fill golem did not deliver from the coloured chest within " + DEADLINE + " ticks: " + state);
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
        private final List<Entity> spawned = new ArrayList<>();
        private final List<ChunkPos> forced = new ArrayList<>();
        private EntityGolemBase router;
        private ChestBlockEntity home, near, far;
        private boolean touched, finished;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-4, -2, -4), base.offset(8, 5, 4))) {
                original.put(mutable.immutable(), level.getBlockState(mutable));
            }
            var profile = new GameProfile(UUID.randomUUID(), "colors-" + UUID.randomUUID().toString().substring(0, 8));
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

        private static GolemMarker marker(BlockPos at, Direction face, int color) { return new GolemMarker(OVERWORLD, at, face, color); }

        private EntityGolemBase golem(int core, List<Integer> upgrades, BlockPos at, Direction facing, List<ItemStack> filters,
                                      List<Integer> counts, List<GolemMarker> markers, List<Integer> colors) {
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(TALLOW);
            golem.applyConfiguration(new GolemData(core, filters, counts, false, markers, GolemUseOptions.DEFAULT, upgrades, colors));
            golem.setPos(Vec3.atBottomCenterOf(at));
            golem.setHome(at, facing);
            test.assertTrue(level.addFreshEntity(golem), "the golem is admitted");
            spawned.add(golem);
            return golem;
        }

        private EntityGolemBase golem(int core, List<Integer> upgrades, BlockPos at) {
            return golem(core, upgrades, at, Direction.UP, NO_FILTERS, NO_COUNTS, List.of(), List.of());
        }

        private void near(EntityGolemBase golem) { player.setPos(Vec3.atBottomCenterOf(golem.blockPosition()).add(1.2, 0, 0)); }

        private void slotColoursNeedOrderAndCycleThroughTheMenu() {
            var plain = golem(GolemData.FILL, List.of(), base.east(7));
            test.assertTrue(!plain.canColorSlots() && !plain.setSlotColor(0, 3) && plain.getSlotColor(0) == -1,
                    "a Fill golem without Order cannot be given a slot colour");
            near(plain);
            var plainMenu = new ContainerGolem(901, player.getInventory(), plain);
            player.containerMenu = plainMenu;
            try {
                // Original ContainerGolem:96-109: the colour buttons are the golem's own slot ids (below the slot
                // count) and the slot count plus the slot id (above it).
                test.assertTrue(!plainMenu.canColorSlots() && !plainMenu.clickMenuButton(player, 0)
                                && !plainMenu.clickMenuButton(player, plain.getSlotCount()),
                        "the menu of a golem without Order offers no colour button");
            } finally { plainMenu.removed(player); player.containerMenu = player.inventoryMenu; }

            var ordered = golem(GolemData.FILL, List.of(ORDER), base.east(3));
            test.assertTrue(ordered.canColorSlots() && ordered.setSlotColor(2, 7) && ordered.getSlotColor(2) == 7, "Order allows a slot colour");
            test.assertTrue(!ordered.setSlotColor(2, 16) && !ordered.setSlotColor(2, -2) && ordered.getSlotColor(2) == 7,
                    "an out-of-range colour is refused and changes nothing");
            test.assertTrue(!ordered.setSlotColor(6, 1) && !ordered.setSlotColor(-1, 1) && ordered.getSlotColor(6) == -1,
                    "a slot beyond the golem's page cannot be coloured");
            test.assertTrue(ordered.setSlotColor(2, -1) && ordered.getSlotColor(2) == -1, "a slot goes back to any");
            for (int core : new int[] {GolemData.GATHER, GolemData.HARVEST, GolemData.SORTING}) {
                var other = golem(core, List.of(ORDER), base.offset(5 + core % 3, 0, -3));
                test.assertTrue(!other.canColorSlots() && !other.setSlotColor(0, 3), "core " + core + " routes by Order but has no slot colours");
            }

            near(ordered);
            var menu = new ContainerGolem(902, player.getInventory(), ordered);
            player.containerMenu = menu;
            try {
                test.assertTrue(menu.canColorSlots() && menu.slotColor(2) == -1, "the Order menu offers the colour chips");
                int expected = -1;
                for (int click = 0; click < 17; click++) {
                    expected = expected == 15 ? -1 : expected + 1;
                    test.assertTrue(menu.clickMenuButton(player, ordered.getSlotCount() + 2), "colour button click " + click);
                    test.assertTrue(ordered.getSlotColor(2) == expected && menu.slotColor(2) == expected && ordered.getSlotColor(1) == -1,
                            "click " + click + " cycles slot two to " + expected + " (golem " + ordered.getSlotColor(2) + ", menu " + menu.slotColor(2) + ")");
                }
                // Original ContainerGolem:97-101: the left button lowers the colour, -1 wraps to 15.
                test.assertTrue(menu.clickMenuButton(player, 2) && ordered.getSlotColor(2) == 15,
                        "the left colour button turns any into fifteen");
                test.assertTrue(menu.clickMenuButton(player, 2) && ordered.getSlotColor(2) == 14,
                        "the left colour button lowers the colour");
                test.assertTrue(!menu.clickMenuButton(player, 2 * ordered.getSlotCount())
                                && !menu.clickMenuButton(player, -1),
                        "only the two colour buttons of every slot exist");
            } finally { menu.removed(player); player.containerMenu = player.inventoryMenu; }

            var paged = golem(GolemData.FILL, List.of(FIRE, ORDER), base.east(5));
            near(paged);
            var pagedMenu = new ContainerGolem(903, player.getInventory(), paged);
            player.containerMenu = pagedMenu;
            try {
                test.assertTrue(paged.getSlotCount() == 12, "Fire and Order on one golem: twelve slots");
                test.assertTrue(pagedMenu.clickMenuButton(player, ContainerGolem.NEXT_PAGE) && pagedMenu.page() == 1
                                && pagedMenu.clickMenuButton(player, paged.getSlotCount() + 7),
                        "the second page's chip is clickable");
                test.assertTrue(paged.getSlotColor(7) == 0 && paged.getSlotColor(1) == -1 && pagedMenu.slotColor(1) == 0,
                        "page two index one is golem slot seven: " + paged.getSlotColor(7) + "/" + paged.getSlotColor(1));
                test.assertTrue(pagedMenu.clickMenuButton(player, ContainerGolem.PREVIOUS_PAGE) && pagedMenu.slotColor(1) == -1,
                        "page one shows slot one again, still any");
            } finally { pagedMenu.removed(player); player.containerMenu = player.inventoryMenu; }
        }

        private void markersAcceptTheRightSlots() {
            var filters = new ArrayList<>(NO_FILTERS);
            filters.set(0, new ItemStack(Items.COBBLESTONE));
            filters.set(1, new ItemStack(Items.DIRT));
            var counts = new ArrayList<>(NO_COUNTS);
            counts.set(0, 1);
            counts.set(1, 1);
            var colors = new ArrayList<>(Collections.nCopies(GolemData.FILTER_SLOTS, -1));
            colors.set(0, 3);
            colors.set(2, 7);
            var empty = golem(GolemData.EMPTY_CORE, List.of(ORDER), base.offset(-3, 0, 3), Direction.UP, filters, counts, List.of(), colors);
            var spot = base.offset(0, 0, 1);
            var cobble = new ItemStack(Items.COBBLESTONE);
            test.assertTrue(empty.colorsMatching(cobble).equals(List.of(3)), "cobblestone is routed by its own slot colour: " + empty.colorsMatching(cobble));
            test.assertTrue(empty.markerAccepts(marker(spot, Direction.UP, 3), cobble) && !empty.markerAccepts(marker(spot, Direction.UP, 4), cobble)
                            && !empty.markerAccepts(marker(spot, Direction.UP, -1), cobble),
                    "a coloured slot takes exactly its own marker colour, not an uncoloured marker");
            var dirt = new ItemStack(Items.DIRT);
            test.assertTrue(empty.markerAccepts(marker(spot, Direction.UP, 0), dirt) && empty.markerAccepts(marker(spot, Direction.UP, 15), dirt)
                            && empty.markerAccepts(marker(spot, Direction.UP, -1), dirt),
                    "an uncoloured slot takes any marker");
            var gold = new ItemStack(Items.GOLD_INGOT);
            test.assertTrue(empty.colorsMatching(gold).isEmpty() && !empty.markerAccepts(marker(spot, Direction.UP, -1), gold)
                            && !empty.markerAccepts(marker(spot, Direction.UP, 3), gold),
                    "an item no slot lists is accepted by no marker: " + empty.colorsMatching(gold));
            test.assertTrue(empty.colorsMatching(ItemStack.EMPTY).contains(7), "an empty hand matches the empty slots, including the coloured one");

            var open = golem(GolemData.FILL, List.of(ORDER), base.offset(-3, 0, 1));
            open.setSlotColor(0, 2);
            test.assertTrue(open.colorsMatching(gold).equals(List.of(2, -1, -1, -1, -1, -1)),
                    "with no exemplar at all every slot's colour applies: " + open.colorsMatching(gold));

            var plain = golem(GolemData.EMPTY_CORE, List.of(), base.offset(-3, 0, -1), Direction.UP, filters, counts, List.of(), List.of());
            test.assertTrue(plain.colorsMatching(cobble).equals(List.of(-1)) && plain.markerAccepts(marker(spot, Direction.UP, -1), cobble),
                    "a golem without Order routes as before");
            test.assertTrue(plain.markerAccepts(marker(spot, Direction.UP, 5), cobble), "and ignores any colour");
        }

        // ---- the bell -------------------------------------------------------------------------------------------

        private InteractionResult clickBell(ItemStack bell, BlockPos target, boolean sneaking) {
            player.setItemInHand(InteractionHand.MAIN_HAND, bell);
            player.setShiftKeyDown(sneaking);
            var hit = new BlockHitResult(Vec3.atCenterOf(target).add(0, .5, 0), Direction.UP, target, false);
            return ((ItemGolemBell) bell.getItem()).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        }

        private void markersAre(EntityGolemBase golem, ItemStack bell, List<GolemMarker> expected, String message) {
            test.assertTrue(golem.getMarkers().equals(expected) && ItemGolemBell.selectedMarkers(bell).equals(expected),
                    message + ": golem " + golem.getMarkers() + ", bell " + ItemGolemBell.selectedMarkers(bell));
        }

        private void theBellCyclesMarkerColours() {
            var target = base.offset(-2, 0, 3);
            var other = base.offset(-1, 0, 3);
            level.setBlock(target, Blocks.STONE.defaultBlockState(), QUIET);
            level.setBlock(other, Blocks.STONE.defaultBlockState(), QUIET);
            var ordered = golem(GolemData.FILL, List.of(ORDER), base.offset(-2, 0, 0));
            var plain = golem(GolemData.FILL, List.of(), base.offset(2, 0, 0));

            var bell = new ItemStack(ThaumcraftGolems.GOLEM_BELL.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, bell);
            near(ordered);
            test.assertTrue(ItemGolemBell.select(bell, player, ordered) == InteractionResult.CONSUME, "the bell selects the Order golem");
            player.setPos(Vec3.atBottomCenterOf(target.below()).add(0, 1, -1));
            var spot = marker(target, Direction.UP, -1);
            test.assertTrue(clickBell(bell, target, false) == InteractionResult.CONSUME, "the first click is consumed");
            markersAre(ordered, bell, List.of(spot), "the first click adds an uncoloured marker");
            for (int color = 0; color <= 15; color++) {
                test.assertTrue(clickBell(bell, target, false) == InteractionResult.CONSUME, "click for colour " + color);
                markersAre(ordered, bell, List.of(spot.withColor(color)), "colour " + color);
            }
            test.assertTrue(clickBell(bell, target, false) == InteractionResult.CONSUME, "the seventeenth click is consumed");
            markersAre(ordered, bell, List.of(), "after colour 15 the marker is removed");

            // Two markers: recolouring moves the marker behind the other one (original remove + add).
            clickBell(bell, target, false);
            clickBell(bell, other, false);
            clickBell(bell, target, false);
            markersAre(ordered, bell, List.of(marker(other, Direction.UP, -1), spot.withColor(0)), "the recoloured marker goes last");
            // A sneaking click removes a coloured marker at once.
            test.assertTrue(clickBell(bell, target, true) == InteractionResult.CONSUME, "the sneaking click is consumed");
            markersAre(ordered, bell, List.of(marker(other, Direction.UP, -1)), "sneaking removes the coloured marker");
            player.setShiftKeyDown(false);

            // Without Order the same bell only toggles an uncoloured marker.
            var plainBell = new ItemStack(ThaumcraftGolems.GOLEM_BELL.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, plainBell);
            near(plain);
            test.assertTrue(ItemGolemBell.select(plainBell, player, plain) == InteractionResult.CONSUME, "the bell selects the plain golem");
            player.setPos(Vec3.atBottomCenterOf(target.below()).add(0, 1, -1));
            clickBell(plainBell, target, false);
            markersAre(plain, plainBell, List.of(spot), "a golem without Order gets an uncoloured marker");
            clickBell(plainBell, target, false);
            markersAre(plain, plainBell, List.of(), "and the next click removes it instead of colouring it");
        }

        // ---- persistence ----------------------------------------------------------------------------------------

        private void coloursPersist() {
            var filters = new ArrayList<>(Collections.nCopies(12, ItemStack.EMPTY));
            filters.set(0, new ItemStack(Items.STONE));
            filters.set(9, new ItemStack(Items.DIRT));
            var counts = new ArrayList<>(Collections.nCopies(12, 0));
            counts.set(0, 4);
            counts.set(9, 2);
            var colors = new ArrayList<>(Collections.nCopies(12, -1));
            colors.set(1, 4);
            colors.set(9, 15);
            var markers = List.of(marker(base.offset(1, 0, 3), Direction.NORTH, 3), marker(base.offset(2, 0, 3), Direction.EAST, -1),
                    marker(base.offset(3, 0, 3), Direction.WEST, 15));
            var saved = golem(GolemData.FILL, List.of(FIRE, ORDER), base.offset(4, 0, -3), Direction.UP, filters, counts, markers, colors);
            test.assertTrue(saved.getSlotColor(1) == 4 && saved.getSlotColor(9) == 15 && saved.getSlotColor(0) == -1
                            && saved.getMarkers().equals(markers), "the configuration is applied as given");
            var tag = new CompoundTag();
            saved.saveWithoutId(tag);
            test.assertTrue(tag.getByteArray("colors").length == 12 && tag.getByteArray("colors")[9] == 15, "NBT keeps twelve slot colours");

            var reloaded = ThaumcraftGolems.GOLEM.get().create(level);
            reloaded.load(tag.copy());
            test.assertTrue(reloaded.getSlotColor(1) == 4 && reloaded.getSlotColor(9) == 15 && reloaded.getSlotColor(0) == -1
                            && reloaded.getMarkers().equals(markers) && reloaded.configuredData().colors().equals(colors),
                    "reloading restores slot colours and marker colours: " + reloaded.getMarkers());
            var data = saved.configuredData();
            test.assertTrue(data.colors().equals(colors) && data.markers().equals(markers), "the placer data carries both");
            var copy = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            copy.setGolemMaterial(TALLOW);
            copy.applyConfiguration(data);
            test.assertTrue(copy.getSlotColor(9) == 15 && copy.getMarkers().equals(markers) && copy.configuredData().equals(data),
                    "placer data applied to a new golem restores them");

            // A save from before colours had no key at all: every slot is "any".
            var legacy = tag.copy();
            legacy.remove("colors");
            var plain = ThaumcraftGolems.GOLEM.get().create(level);
            plain.load(legacy);
            boolean allAny = true;
            for (int slot = 0; slot < 12; slot++) allAny &= plain.getSlotColor(slot) == -1;
            test.assertTrue(allAny, "a save without the colour key reads as all any");

            // Colours without their Order rule are unsupported data, never silently dropped.
            var noOrder = golem(GolemData.FILL, List.of(), base.offset(5, 0, -3));
            var forged = new CompoundTag();
            noOrder.saveWithoutId(forged);
            var forgedColors = new byte[6];
            java.util.Arrays.fill(forgedColors, (byte) -1);
            forgedColors[0] = 3;
            forged.putByteArray("colors", forgedColors);
            var refused = ThaumcraftGolems.GOLEM.get().create(level);
            refused.load(forged);
            test.assertTrue(refusedFlattening(refused), "a colour on a golem without Order makes it unsupported");
            var wrongSize = new CompoundTag();
            saved.saveWithoutId(wrongSize);
            wrongSize.putByteArray("colors", new byte[7]);
            var refusedSize = ThaumcraftGolems.GOLEM.get().create(level);
            refusedSize.load(wrongSize);
            test.assertTrue(refusedFlattening(refusedSize), "a colour list of a wrong size is refused instead of truncated");
            reloaded.discard();
            copy.discard();
            plain.discard();
            refused.discard();
            refusedSize.discard();
        }

        private static boolean refusedFlattening(EntityGolemBase golem) {
            try { golem.configuredData(); return false; } catch (IllegalStateException expected) { return true; }
        }

        // ---- a real Fill golem chooses the chest by colour ------------------------------------------------------

        private ChestBlockEntity chest(BlockPos at, ItemStack contents) {
            level.setBlock(at, Blocks.CHEST.defaultBlockState(), QUIET);
            var chest = (ChestBlockEntity) level.getBlockEntity(at);
            test.assertTrue(chest != null, "a chest block entity at " + at);
            if (!contents.isEmpty()) chest.setItem(0, contents);
            return chest;
        }

        private static int count(Container container, net.minecraft.world.item.Item item) {
            int total = 0;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.is(item)) total += stack.getCount();
            }
            return total;
        }

        private void startRouting() {
            // The earlier golems are done: they must not wander into the routing fixture.
            for (Entity entity : spawned) entity.discard();
            spawned.clear();
            // The golem stands at base, its home chest is west of it. The "near" chest is closer but carries the wrong marker
            // colour; the "far" chest carries colour 3, which is the colour of the cobblestone slot.
            home = chest(base.west(), ItemStack.EMPTY);
            near = chest(base.offset(0, 0, -3), new ItemStack(Items.COBBLESTONE, 64));
            far = chest(base.offset(3, 0, 2), new ItemStack(Items.COBBLESTONE, 64));
            var filters = new ArrayList<>(NO_FILTERS);
            filters.set(0, new ItemStack(Items.COBBLESTONE));
            var counts = new ArrayList<>(NO_COUNTS);
            counts.set(0, 8);
            var colors = new ArrayList<>(Collections.nCopies(GolemData.FILTER_SLOTS, -1));
            colors.set(0, 3);
            var markers = List.of(marker(near.getBlockPos(), Direction.SOUTH, 5), marker(far.getBlockPos(), Direction.WEST, 3));
            router = golem(GolemData.FILL, List.of(ORDER), base, Direction.EAST, filters, counts, markers, colors);
            test.assertTrue(router.markerAccepts(markers.get(1), new ItemStack(Items.COBBLESTONE))
                            && !router.markerAccepts(markers.get(0), new ItemStack(Items.COBBLESTONE)),
                    "only the colour-3 marker may serve the cobblestone slot");
        }

        private boolean routingDone() {
            int delivered = count(home, Items.COBBLESTONE);
            if (delivered == 0) return false;
            test.assertTrue(count(near, Items.COBBLESTONE) == 64, "the wrongly coloured chest is never touched: " + count(near, Items.COBBLESTONE));
            test.assertTrue(count(far, Items.COBBLESTONE) + delivered + router.getCarriedItem().getCount() == 64,
                    "everything delivered came out of the colour-3 chest: far " + count(far, Items.COBBLESTONE) + ", delivered " + delivered);
            return true;
        }

        private String routingState() {
            return "home " + count(home, Items.COBBLESTONE) + ", near " + count(near, Items.COBBLESTONE) + ", far " + count(far, Items.COBBLESTONE)
                    + ", carrying " + router.getCarriedItem() + ", golem at " + router.position();
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
