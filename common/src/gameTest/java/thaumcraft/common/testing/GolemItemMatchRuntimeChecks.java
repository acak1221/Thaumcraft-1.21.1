package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftGolems;
import thaumcraft.common.container.ContainerGolem;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.items.golems.GolemData;
import thaumcraft.common.items.golems.GolemGuardTargets;
import thaumcraft.common.items.golems.GolemItemMatch;
import thaumcraft.common.items.golems.GolemMarker;
import thaumcraft.common.items.golems.GolemUpgrade;
import thaumcraft.common.items.golems.GolemUseOptions;

/**
 * Entropy's three item-comparison switches on real content. A real connected player, the real menu (buttons 55..57), real golems,
 * real chests and real item stacks are explicit INPUTS; which stack a golem fetches, how many, and whether it keeps working are
 * decided by the golems, never supplied. Ten golems run on ten stacked platforms (the neighbouring GameTests wipe the ground around their own structures, so the fixture
 * stays on one small footprint and grows upwards) in five matched pairs (the positive and the negative member
 * differ only in the switch under test, or in the Entropy upgrade): Fill with the damage switch, Fill with the components switch,
 * Fill with the tag switch, Empty (tag widening from Entropy alone) and Sorting (the seed probe). The negative golems must leave
 * every chest untouched for the whole run. NOT the Use core's comparison (needs a use target), not a client/GUI, network or visual
 * check: the three check boxes are unseen.
 */
public final class GolemItemMatchRuntimeChecks {
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int TALLOW = 2, DEADLINE = 1800, SETTLE = 120, LAYER = 6, LANES = 10;
    private static final int ENTROPY = GolemUpgrade.ENTROPY.id, ORDER = GolemUpgrade.ORDER.id;
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    private static final List<ItemStack> NO_FILTERS = Collections.nCopies(GolemData.FILTER_SLOTS, ItemStack.EMPTY);
    private static final List<Integer> NO_COUNTS = Collections.nCopies(GolemData.FILTER_SLOTS, 0);

    private GolemItemMatchRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var check = new Check(test);
        try {
            check.start();
            check.switchesNeedEntropyAndAHostCore();
            check.theMenuUsesTheOriginalButtons();
            check.slotColoursFollowTheSwitches();
            check.persistence();
            check.armLanes();
        } catch (RuntimeException failure) {
            check.finish();
            throw failure;
        }
        test.onEachTick(() -> {
            if (check.finished) return;
            try {
                if (check.poll()) { check.finish(); test.succeed(); }
            } catch (RuntimeException failure) {
                Thaumcraft.LOG.error("Item-match lanes failed at tick {}: {} ({})", test.getTick(), failure.getMessage(), check.describe());
                check.finish();
                throw failure;
            }
        });
        test.runAtTickTime(DEADLINE, () -> {
            if (check.finished) return;
            String state = check.describe();
            check.finish();
            test.fail("the item-match lanes did not finish within " + DEADLINE + " ticks: " + state);
        });
    }

    /** One golem with the chests it works between and the predicates that judge it. */
    private static final class Lane {
        final String name;
        final boolean positive;
        EntityGolemBase golem;
        ChestBlockEntity home, other;
        List<ItemStack> homeBefore, otherBefore;
        BooleanSupplier done = () -> false;
        Runnable invariant = () -> {};
        Runnable verdict = () -> {};
        boolean finished;

        Lane(String name, boolean positive) { this.name = name; this.positive = positive; }
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
        private final List<Lane> lanes = new ArrayList<>();
        private boolean touched, finished;
        private long settleUntil;

        private Check(GameTestHelper test) {
            this.test = test;
            level = test.getLevel();
            base = test.absolutePos(new BlockPos(3, 3, 3));
            for (BlockPos mutable : BlockPos.betweenClosed(base.offset(-2, -2, -2), base.offset(3, LAYER * LANES + 4, 5))) {
                original.put(mutable.immutable(), level.getBlockState(mutable));
            }
            var profile = new GameProfile(UUID.randomUUID(), "match-" + UUID.randomUUID().toString().substring(0, 8));
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
                level.setBlock(mutable, (mutable.getY() - (base.getY() - 1)) % LAYER == 0 && mutable.getY() >= base.getY() - 1
                        ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), QUIET);
            }
            test.assertTrue(level.mayInteract(player, base), "fixture is not under unrelated spawn protection");
        }

        private static GolemMarker marker(BlockPos at, Direction face) { return new GolemMarker(OVERWORLD, at, face, GolemMarker.ANY); }

        private EntityGolemBase golem(int core, List<Integer> upgrades, BlockPos at, Direction facing, List<ItemStack> filters,
                                      List<Integer> counts, List<GolemMarker> markers, List<Integer> colors, int match) {
            var golem = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            golem.setGolemMaterial(TALLOW);
            golem.applyConfiguration(new GolemData(core, filters, counts, false, markers, GolemUseOptions.DEFAULT, upgrades, colors,
                    GolemGuardTargets.DEFAULT, false, "", match));
            golem.setPos(Vec3.atBottomCenterOf(at));
            golem.setHome(at, facing);
            test.assertTrue(level.addFreshEntity(golem), "the golem is admitted");
            spawned.add(golem);
            return golem;
        }

        /** Bench golems stand in a grid on a platform above the lanes, one block apart; they only exist to be configured and are discarded before the lanes run. */
        private BlockPos benchPos(int index) { return base.offset(-2 + index % 6, LAYER * LANES, -1 + index / 6); }

        private EntityGolemBase bench(int core, List<Integer> upgrades, int index) {
            return golem(core, upgrades, benchPos(index), Direction.UP, NO_FILTERS, NO_COUNTS, List.of(), List.of(), 0);
        }

        private void near(EntityGolemBase golem) { player.setPos(Vec3.atBottomCenterOf(golem.blockPosition()).add(1.2, 0, 0)); }

        // ---- who may have the switches --------------------------------------------------------------------------

        private void switchesNeedEntropyAndAHostCore() {
            int index = 0;
            for (int core : new int[] {GolemData.FILL, GolemData.EMPTY_CORE, GolemData.GATHER, GolemData.USE, GolemData.SORTING}) {
                var plain = bench(core, List.of(), index++);
                test.assertTrue(!plain.canSwitchItemMatch() && !plain.setItemMatch(GolemItemMatch.TAGS) && !plain.toggleItemMatch(GolemItemMatch.IGNORE_DAMAGE)
                                && plain.getItemMatch() == 0,
                        "core " + core + " without Entropy has no switches");
                var entropic = bench(core, List.of(ENTROPY), index++);
                test.assertTrue(entropic.canSwitchItemMatch() && entropic.setItemMatch(GolemItemMatch.ALL) && entropic.getItemMatch() == GolemItemMatch.ALL
                                && entropic.setItemMatch(0) && entropic.getItemMatch() == 0,
                        "core " + core + " with Entropy has all three switches");
                test.assertTrue(!entropic.setItemMatch(8) && !entropic.setItemMatch(-1) && !entropic.toggleItemMatch(3) && !entropic.toggleItemMatch(8)
                                && entropic.getItemMatch() == 0,
                        "core " + core + " refuses a switch value that is not one of the three");
            }
            for (int core : new int[] {GolemData.HARVEST, GolemData.GUARD, GolemData.LIQUID, GolemData.LUMBER, GolemData.BUTCHER}) {
                var other = bench(core, List.of(ENTROPY), index++);
                test.assertTrue(!other.canSwitchItemMatch() && !other.setItemMatch(GolemItemMatch.TAGS) && other.getItemMatch() == 0,
                        "core " + core + " has no item-comparison switches even with Entropy");
            }
            var core = bench(GolemData.NO_CORE, List.of(ENTROPY), index);
            test.assertTrue(!core.canSwitchItemMatch() && !core.setItemMatch(1), "a coreless golem has none");
        }

        private void theMenuUsesTheOriginalButtons() {
            var golem = bench(GolemData.FILL, List.of(ENTROPY), 20);
            near(golem);
            var menu = new ContainerGolem(911, player.getInventory(), golem);
            player.containerMenu = menu;
            try {
                test.assertTrue(ContainerGolem.ITEM_MATCH_BUTTON == 55 && GolemItemMatch.buttonOf(GolemItemMatch.TAGS) == 55
                                && GolemItemMatch.buttonOf(GolemItemMatch.IGNORE_DAMAGE) == 56 && GolemItemMatch.buttonOf(GolemItemMatch.IGNORE_COMPONENTS) == 57,
                        "the original button ids");
                test.assertTrue(menu.canSwitchItemMatch() && menu.itemMatch() == 0, "the Entropy menu offers the switches, all off");
                test.assertTrue(menu.clickMenuButton(player, 55) && golem.getItemMatch() == GolemItemMatch.TAGS && menu.itemMatch() == GolemItemMatch.TAGS,
                        "55 switches the tags on, golem " + golem.getItemMatch() + ", menu " + menu.itemMatch());
                test.assertTrue(menu.clickMenuButton(player, 57) && golem.getItemMatch() == (GolemItemMatch.TAGS | GolemItemMatch.IGNORE_COMPONENTS)
                                && menu.itemMatch() == golem.getItemMatch(), "57 adds the components switch and leaves the tags alone");
                test.assertTrue(menu.clickMenuButton(player, 56) && golem.getItemMatch() == GolemItemMatch.ALL && menu.itemMatch() == GolemItemMatch.ALL,
                        "56 adds the damage switch");
                for (int button : new int[] {55, 56, 57}) {
                    test.assertTrue(menu.clickMenuButton(player, button), "button " + button + " flips back");
                }
                test.assertTrue(golem.getItemMatch() == 0 && menu.itemMatch() == 0, "each switch flips off independently");
                for (int button : new int[] {54, 58, -55, 155}) {
                    test.assertTrue(!menu.clickMenuButton(player, button) && golem.getItemMatch() == 0, "button " + button + " is not an item-match switch and changes none");
                }
                // The Guard's switches (51..54) and the item-match switches are separate values: neither disturbs the other.
                test.assertTrue(golem.getGuardTargets() == GolemGuardTargets.DEFAULT, "the Guard mask is untouched by the item-match buttons");
            } finally { menu.removed(player); player.containerMenu = player.inventoryMenu; }

            var plain = bench(GolemData.FILL, List.of(), 21);
            near(plain);
            var plainMenu = new ContainerGolem(912, player.getInventory(), plain);
            player.containerMenu = plainMenu;
            try {
                test.assertTrue(!plainMenu.canSwitchItemMatch(), "a Fill menu without Entropy shows no switches");
                for (int button : new int[] {55, 56, 57}) {
                    test.assertTrue(!plainMenu.clickMenuButton(player, button) && plain.getItemMatch() == 0, "forged button " + button + " on a golem without Entropy");
                }
            } finally { plainMenu.removed(player); player.containerMenu = player.inventoryMenu; }

            var harvest = bench(GolemData.HARVEST, List.of(ENTROPY), 22);
            near(harvest);
            var harvestMenu = new ContainerGolem(913, player.getInventory(), harvest);
            player.containerMenu = harvestMenu;
            try {
                test.assertTrue(!harvestMenu.canSwitchItemMatch(), "a Harvest menu shows no switches even with Entropy");
                for (int button : new int[] {55, 56, 57}) {
                    test.assertTrue(!harvestMenu.clickMenuButton(player, button) && harvest.getItemMatch() == 0, "forged button " + button + " on a Harvest golem");
                }
            } finally { harvestMenu.removed(player); player.containerMenu = player.inventoryMenu; }
        }

        private void slotColoursFollowTheSwitches() {
            var filters = new ArrayList<>(NO_FILTERS);
            filters.set(0, new ItemStack(Items.OAK_PLANKS));
            filters.set(1, new ItemStack(Items.IRON_PICKAXE));
            var counts = new ArrayList<>(NO_COUNTS);
            counts.set(0, 1);
            counts.set(1, 1);
            var colors = new ArrayList<>(Collections.nCopies(GolemData.FILTER_SLOTS, -1));
            colors.set(0, 3);
            colors.set(1, 7);
            var golem = golem(GolemData.FILL, List.of(ORDER, ENTROPY), benchPos(24), Direction.UP, filters, counts, List.of(), colors, 0);
            var birch = new ItemStack(Items.BIRCH_PLANKS);
            var worn = new ItemStack(Items.IRON_PICKAXE);
            worn.setDamageValue(40);
            var named = new ItemStack(Items.IRON_PICKAXE);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));
            test.assertTrue(golem.colorsMatching(new ItemStack(Items.OAK_PLANKS)).equals(List.of(3)), "the exact stack routes by its slot");
            test.assertTrue(golem.colorsMatching(birch).isEmpty() && golem.colorsMatching(worn).isEmpty() && golem.colorsMatching(named).isEmpty(),
                    "without the switches look-alikes match no slot: " + golem.colorsMatching(birch) + golem.colorsMatching(worn) + golem.colorsMatching(named));
            near(golem);
            var menu = new ContainerGolem(914, player.getInventory(), golem);
            player.containerMenu = menu;
            try {
                test.assertTrue(menu.clickMenuButton(player, 55), "tags on");
                test.assertTrue(golem.colorsMatching(birch).equals(List.of(3)), "the tag switch routes birch planks by the oak planks slot: " + golem.colorsMatching(birch));
                test.assertTrue(golem.colorsMatching(worn).isEmpty() && golem.colorsMatching(new ItemStack(Items.DIAMOND_PICKAXE)).isEmpty(),
                        "tools are not an ore name: c:tools/pickaxe lists every pickaxe yet the worn and the diamond pickaxe stay different: "
                                + golem.colorsMatching(worn) + golem.colorsMatching(new ItemStack(Items.DIAMOND_PICKAXE)));
                test.assertTrue(menu.clickMenuButton(player, 56), "damage on");
                test.assertTrue(golem.colorsMatching(worn).equals(List.of(7)) && golem.colorsMatching(named).isEmpty(),
                        "the damage switch routes the worn pickaxe, not the renamed one: " + golem.colorsMatching(worn) + golem.colorsMatching(named));
                test.assertTrue(menu.clickMenuButton(player, 57), "components on");
                test.assertTrue(golem.colorsMatching(named).equals(List.of(7)) && golem.colorsMatching(new ItemStack(Items.IRON_SWORD)).isEmpty(),
                        "the components switch routes the renamed pickaxe and still never another item");
            } finally { menu.removed(player); player.containerMenu = player.inventoryMenu; }
        }

        private static boolean refused(EntityGolemBase golem) {
            try { golem.configuredData(); return false; } catch (IllegalStateException expected) { return true; }
        }

        private void persistence() {
            var saved = golem(GolemData.FILL, List.of(ENTROPY), benchPos(26), Direction.UP, NO_FILTERS, NO_COUNTS, List.of(), List.of(),
                    GolemItemMatch.TAGS | GolemItemMatch.IGNORE_COMPONENTS);
            test.assertTrue(saved.getItemMatch() == 5, "the configuration is applied as given");
            var tag = new CompoundTag();
            saved.saveWithoutId(tag);
            test.assertTrue(tag.contains("ItemMatch", net.minecraft.nbt.Tag.TAG_BYTE) && tag.getByte("ItemMatch") == 5, "NBT keeps the switches in their own byte");
            var reloaded = ThaumcraftGolems.GOLEM.get().create(level);
            reloaded.load(tag.copy());
            test.assertTrue(reloaded.getItemMatch() == 5 && reloaded.configuredData().itemMatch() == 5, "reloading restores the switches");
            var data = saved.configuredData();
            test.assertTrue(data.itemMatch() == 5, "the placer data carries them");
            var copy = new EntityGolemBase(ThaumcraftGolems.GOLEM.get(), level);
            copy.setGolemMaterial(TALLOW);
            copy.applyConfiguration(data);
            test.assertTrue(copy.getItemMatch() == 5 && copy.configuredData().equals(data), "placer data applied to a new golem restores them");

            var off = new CompoundTag();
            golem(GolemData.FILL, List.of(ENTROPY), benchPos(27), Direction.UP, NO_FILTERS, NO_COUNTS, List.of(), List.of(), 0).saveWithoutId(off);
            test.assertTrue(!off.contains("ItemMatch"), "switches that are all off write no key, so earlier saves keep their form");
            var legacy = tag.copy();
            legacy.remove("ItemMatch");
            var fresh = ThaumcraftGolems.GOLEM.get().create(level);
            fresh.load(legacy);
            test.assertTrue(fresh.getItemMatch() == 0, "a save without the key reads as all off");

            // Switches without their Entropy upgrade, outside the three bits or of the wrong type are unsupported data, never dropped.
            var noEntropy = new CompoundTag();
            golem(GolemData.FILL, List.of(), benchPos(28), Direction.UP, NO_FILTERS, NO_COUNTS, List.of(), List.of(), 0).saveWithoutId(noEntropy);
            noEntropy.putByte("ItemMatch", (byte) 1);
            var refusedNoEntropy = ThaumcraftGolems.GOLEM.get().create(level);
            refusedNoEntropy.load(noEntropy);
            test.assertTrue(refused(refusedNoEntropy), "a switch on a golem without Entropy makes it unsupported");
            var outOfRange = tag.copy();
            outOfRange.putByte("ItemMatch", (byte) 8);
            var refusedRange = ThaumcraftGolems.GOLEM.get().create(level);
            refusedRange.load(outOfRange);
            test.assertTrue(refused(refusedRange), "a value above the three bits is refused");
            var wrongType = tag.copy();
            wrongType.putInt("ItemMatch", 1);
            var refusedType = ThaumcraftGolems.GOLEM.get().create(level);
            refusedType.load(wrongType);
            test.assertTrue(refused(refusedType), "a switch of the wrong tag type is refused");
            var hostless = new CompoundTag();
            golem(GolemData.HARVEST, List.of(ENTROPY), benchPos(29), Direction.UP, NO_FILTERS, NO_COUNTS, List.of(), List.of(), 0).saveWithoutId(hostless);
            hostless.putByte("ItemMatch", (byte) 1);
            var refusedHost = ThaumcraftGolems.GOLEM.get().create(level);
            refusedHost.load(hostless);
            test.assertTrue(refused(refusedHost), "a switch on a core that has none is refused");
            for (EntityGolemBase golem : List.of(reloaded, copy, fresh, refusedNoEntropy, refusedRange, refusedType, refusedHost)) golem.discard();
        }

        // ---- real golems between real chests ----------------------------------------------------------------------

        private ChestBlockEntity chest(BlockPos at, ItemStack... contents) {
            level.setBlock(at, Blocks.CHEST.defaultBlockState(), QUIET);
            var chest = (ChestBlockEntity) level.getBlockEntity(at);
            test.assertTrue(chest != null, "a chest block entity at " + at);
            for (int slot = 0; slot < contents.length; slot++) chest.setItem(slot, contents[slot].copy()); // never share a stack between two chests
            return chest;
        }

        private static List<ItemStack> snapshot(Container container) {
            var items = new ArrayList<ItemStack>();
            for (int slot = 0; slot < container.getContainerSize(); slot++) items.add(container.getItem(slot).copy());
            return items;
        }

        private static boolean same(List<ItemStack> expected, Container container) {
            if (expected.size() != container.getContainerSize()) return false;
            for (int slot = 0; slot < expected.size(); slot++) {
                ItemStack actual = container.getItem(slot);
                if (!ItemStack.matches(expected.get(slot), actual)) return false;
            }
            return true;
        }

        private static int count(Container container, Item item) {
            int total = 0;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.is(item)) total += stack.getCount();
            }
            return total;
        }

        private static ItemStack worn(Item item, int damage) {
            var stack = new ItemStack(item);
            stack.setDamageValue(damage);
            return stack;
        }

        private Lane lane(String name, boolean positive, int core, List<Integer> upgrades, int match, ItemStack filter, int wanted,
                          ItemStack[] homeItems, ItemStack[] otherItems) {
            var lane = new Lane(name, positive);
            var at = base.offset(1, LAYER * lanes.size(), 0); // golem; its home chest is west of it, the marked chest three blocks south
            lane.home = chest(at.west(), homeItems);
            lane.other = chest(at.south(3), otherItems);
            lane.homeBefore = snapshot(lane.home);
            lane.otherBefore = snapshot(lane.other);
            var filters = new ArrayList<>(NO_FILTERS);
            var counts = new ArrayList<>(NO_COUNTS);
            if (!filter.isEmpty()) { filters.set(0, filter); counts.set(0, wanted); }
            lane.golem = golem(core, upgrades, at, Direction.EAST, filters, counts, List.of(marker(lane.other.getBlockPos(), Direction.NORTH)), List.of(), match);
            lanes.add(lane);
            return lane;
        }

        /** A negative golem is judged every tick: nothing may ever leave or enter either chest, and it carries nothing. */
        private void mustStayIdle(Lane lane) {
            lane.invariant = () -> {
                test.assertTrue(same(lane.homeBefore, lane.home) && same(lane.otherBefore, lane.other) && lane.golem.getCarriedItem().isEmpty(),
                        lane.name + " must not move anything: home " + describe(lane.home) + ", other " + describe(lane.other) + ", carrying " + lane.golem.getCarriedItem());
            };
        }

        private void armLanes() {
            // The bench golems are done: they must not wander into the lanes.
            for (Entity entity : spawned) entity.discard();
            spawned.clear();
            var oneEntropy = List.of(ENTROPY);

            // Damage: the filter is a fresh pickaxe, the source holds three worn ones. The golem fetches the worn ones, two of them,
            // and stops: the home chest's stock counts under the same switch (otherwise it would empty the source).
            var pickaxes = new ItemStack[] {worn(Items.IRON_PICKAXE, 10), worn(Items.IRON_PICKAXE, 20), worn(Items.IRON_PICKAXE, 30)};
            var damage = lane("fill/damage on", true, GolemData.FILL, oneEntropy, GolemItemMatch.IGNORE_DAMAGE, new ItemStack(Items.IRON_PICKAXE), 2,
                    new ItemStack[0], pickaxes);
            damage.invariant = () -> {
                test.assertTrue(count(damage.home, Items.IRON_PICKAXE) + count(damage.other, Items.IRON_PICKAXE) + damage.golem.getCarriedItem().getCount() == 3,
                        "no pickaxe is lost or made");
                test.assertTrue(count(damage.home, Items.IRON_PICKAXE) <= 2, "the golem never over-fills: the home stock counts under the damage switch too");
            };
            damage.done = () -> count(damage.home, Items.IRON_PICKAXE) == 2 && damage.golem.getCarriedItem().isEmpty();
            damage.verdict = () -> {
                for (int slot = 0; slot < damage.home.getContainerSize(); slot++) {
                    ItemStack stack = damage.home.getItem(slot);
                    test.assertTrue(stack.isEmpty() || stack.getDamageValue() > 0, "what arrives is the worn stock, not a rebuilt fresh pickaxe: " + stack);
                }
                test.assertTrue(count(damage.other, Items.IRON_PICKAXE) == 1, "one worn pickaxe stays in the source");
            };
            mustStayIdle(lane("fill/damage off", false, GolemData.FILL, oneEntropy, 0, new ItemStack(Items.IRON_PICKAXE), 2, new ItemStack[0], pickaxes));

            // Components: a renamed, worn sword is the same sword only with the components switch; the damage switch alone is not enough.
            var renamed = new ItemStack(Items.IRON_SWORD);
            renamed.setDamageValue(5);
            renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Renamed"));
            var components = lane("fill/components on", true, GolemData.FILL, oneEntropy, GolemItemMatch.IGNORE_COMPONENTS, new ItemStack(Items.IRON_SWORD), 1,
                    new ItemStack[0], new ItemStack[] {renamed.copy()});
            components.invariant = () -> test.assertTrue(count(components.home, Items.IRON_SWORD) + count(components.other, Items.IRON_SWORD)
                    + components.golem.getCarriedItem().getCount() == 1, "no sword is lost or made");
            components.done = () -> count(components.home, Items.IRON_SWORD) == 1;
            components.verdict = () -> {
                ItemStack arrived = components.home.getItem(0);
                test.assertTrue(arrived.is(Items.IRON_SWORD) && arrived.getDamageValue() == 5 && "Renamed".equals(arrived.getHoverName().getString()),
                        "the golem delivers the stock stack with its name and damage, not the exemplar: " + arrived);
                test.assertTrue(count(components.other, Items.IRON_SWORD) == 0, "the source gave it up");
            };
            mustStayIdle(lane("fill/components off (damage only)", false, GolemData.FILL, oneEntropy, GolemItemMatch.IGNORE_DAMAGE, new ItemStack(Items.IRON_SWORD), 1,
                    new ItemStack[0], new ItemStack[] {renamed.copy()}));

            // Tags: birch planks stand for oak planks only with the tag switch.
            var tags = lane("fill/tags on", true, GolemData.FILL, oneEntropy, GolemItemMatch.TAGS, new ItemStack(Items.OAK_PLANKS), 8,
                    new ItemStack[0], new ItemStack[] {new ItemStack(Items.BIRCH_PLANKS, 16)});
            tags.invariant = () -> {
                test.assertTrue(count(tags.home, Items.BIRCH_PLANKS) + count(tags.other, Items.BIRCH_PLANKS) + tags.golem.getCarriedItem().getCount() == 16,
                        "no plank is lost or made");
                test.assertTrue(count(tags.home, Items.BIRCH_PLANKS) <= 8, "the golem never over-fills: the home stock counts under the tag switch too");
            };
            tags.done = () -> count(tags.home, Items.BIRCH_PLANKS) == 8 && tags.golem.getCarriedItem().isEmpty();
            tags.verdict = () -> test.assertTrue(count(tags.other, Items.BIRCH_PLANKS) == 8 && count(tags.home, Items.OAK_PLANKS) == 0,
                    "eight birch planks moved, none were conjured as oak");
            mustStayIdle(lane("fill/tags off", false, GolemData.FILL, oneEntropy, 0, new ItemStack(Items.OAK_PLANKS), 8, new ItemStack[0],
                    new ItemStack[] {new ItemStack(Items.BIRCH_PLANKS, 16)}));

            // Empty: Entropy alone widens the wanted oak planks to every plank, with no switch set (original getItemsNeeded(Entropy > 0)).
            var empty = lane("empty/entropy", true, GolemData.EMPTY_CORE, oneEntropy, 0, new ItemStack(Items.OAK_PLANKS), 1,
                    new ItemStack[] {new ItemStack(Items.BIRCH_PLANKS, 12)}, new ItemStack[0]);
            empty.invariant = () -> test.assertTrue(count(empty.home, Items.BIRCH_PLANKS) + count(empty.other, Items.BIRCH_PLANKS) + empty.golem.getCarriedItem().getCount() == 12,
                    "no plank is lost or made");
            empty.done = () -> count(empty.other, Items.BIRCH_PLANKS) == 12;
            empty.verdict = () -> test.assertTrue(count(empty.home, Items.BIRCH_PLANKS) == 0, "the home chest is emptied of the look-alike");
            mustStayIdle(lane("empty/no entropy", false, GolemData.EMPTY_CORE, List.of(), 0, new ItemStack(Items.OAK_PLANKS), 1,
                    new ItemStack[] {new ItemStack(Items.BIRCH_PLANKS, 12)}, new ItemStack[0]));

            // Sorting: the seed in the destination may be a look-alike under the tag switch (strict room, fuzzy seed).
            var sorting = lane("sorting/tags on", true, GolemData.SORTING, oneEntropy, GolemItemMatch.TAGS, ItemStack.EMPTY, 0,
                    new ItemStack[] {new ItemStack(Items.BIRCH_PLANKS, 8)}, new ItemStack[] {new ItemStack(Items.OAK_PLANKS, 1)});
            sorting.invariant = () -> test.assertTrue(count(sorting.home, Items.BIRCH_PLANKS) + count(sorting.other, Items.BIRCH_PLANKS) + sorting.golem.getCarriedItem().getCount() == 8
                    && count(sorting.other, Items.OAK_PLANKS) == 1, "no plank is lost or made, and the seed stays");
            sorting.done = () -> count(sorting.other, Items.BIRCH_PLANKS) == 8;
            sorting.verdict = () -> test.assertTrue(count(sorting.home, Items.BIRCH_PLANKS) == 0, "everything that could be sorted was");
            mustStayIdle(lane("sorting/tags off", false, GolemData.SORTING, oneEntropy, 0, ItemStack.EMPTY, 0,
                    new ItemStack[] {new ItemStack(Items.BIRCH_PLANKS, 8)}, new ItemStack[] {new ItemStack(Items.OAK_PLANKS, 1)}));
            test.assertTrue(lanes.size() == 10, "ten lanes");
        }

        private static String describe(Container container) {
            var text = new StringBuilder();
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty()) text.append(slot).append('=').append(stack).append(' ');
            }
            return text.isEmpty() ? "empty" : text.toString().trim();
        }

        private boolean poll() {
            for (Lane lane : lanes) {
                lane.invariant.run();
                if (lane.positive && !lane.finished && lane.done.getAsBoolean()) lane.finished = true;
            }
            if (settleUntil == 0) {
                if (lanes.stream().filter(lane -> lane.positive).allMatch(lane -> lane.finished)) settleUntil = test.getTick() + SETTLE;
                return false;
            }
            for (Lane lane : lanes) if (lane.positive) test.assertTrue(lane.done.getAsBoolean(), lane.name + " stays finished while the others settle");
            if (test.getTick() < settleUntil) return false;
            for (Lane lane : lanes) lane.verdict.run();
            return true;
        }

        private String describe() {
            var text = new StringBuilder();
            for (Lane lane : lanes) {
                text.append(lane.name).append(lane.positive ? (lane.finished ? " [done]" : " [WAITING]") : " [idle]")
                        .append(" home ").append(describe(lane.home)).append(" | other ").append(describe(lane.other))
                        .append(" | carrying ").append(lane.golem.getCarriedItem()).append(" at ").append(lane.golem.blockPosition().toShortString()).append("; ");
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
