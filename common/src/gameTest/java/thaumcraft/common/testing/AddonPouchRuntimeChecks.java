package thaumcraft.common.testing;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.IFocusPouch;
import thaumcraft.common.config.ThaumcraftFoci;
import thaumcraft.common.container.ContainerAccessories;
import thaumcraft.common.items.wands.FocusSelectionSources;
import thaumcraft.common.items.wands.ItemFocusPouchBauble;
import thaumcraft.common.items.wands.foci.FocusData;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.network.AccessoryNetworking;
import thaumcraft.common.lib.network.FocusSelectionNetworking;
import thaumcraft.common.lib.network.FocusSelectionPayload;

/**
 * A focus pouch of an add-on, through the public {@link IFocusPouch}, in real gameplay.
 *
 * <p>The add-on here is the harness itself, as in {@link WandApiRuntimeChecks}: two test-only pouch items (40 and 130 cells,
 * both belt baubles that count their own {@code use}) enter the native item registry from the GameTest mod's initialisation.
 * They keep their cells in the stack's custom data, not in the native container component, and the player never sees a
 * class of Thaumcraft's pouch machinery: the wand's selector reaches them only through {@link IFocusPouch}. The player is a
 * real connected survival player ({@link FocusUpgradeRuntimeChecks.Actor}), every selection goes through the production
 * receiver and the equip checks through the real use path and the real accessories menu.</p>
 */
public final class AddonPouchRuntimeChecks {
    private static Pouch pouch, bigPouch;
    /** The registries the test pouch encodes its cells with; set by each scenario before it creates a pouch. */
    private static RegistryAccess access;
    private AddonPouchRuntimeChecks() {}

    /** Test-only items, registered while the native registry is still open (called from {@link WandApiRuntimeChecks#registerTestItems}). */
    static synchronized void registerTestItems() {
        if (pouch != null) return;
        pouch = Registry.register(BuiltInRegistries.ITEM, id("addon_pouch"), new Pouch(new Item.Properties(), 40));
        bigPouch = Registry.register(BuiltInRegistries.ITEM, id("addon_big_pouch"), new Pouch(new Item.Properties(), 130));
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", path); }

    /**
     * An add-on pouch in the manner of Thaumic Tinkerer's Bottomless Pouch: a belt bauble with a fixed number of cells, a use
     * of its own (it would open its screen; here it only counts) and its own storage. A stack may carry the flag "unreadable"
     * (a payload of another version: {@link #getInventory} throws) or "nulls" (empty cells read as {@code null}).
     */
    static final class Pouch extends Item implements IBauble, IFocusPouch {
        final int size;
        int uses;
        Pouch(Properties properties, int size) { super(properties.stacksTo(1)); this.size = size; }
        @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.BELT; }
        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            uses++;
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        private static RegistryOps<Tag> ops() {
            if (access == null) throw new IllegalStateException("the scenario sets the registries first");
            return RegistryOps.create(NbtOps.INSTANCE, access);
        }
        private static CompoundTag data(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
        @Override public ItemStack[] getInventory(ItemStack stack) {
            CompoundTag tag = data(stack);
            if (tag.getBoolean("unreadable")) throw new IllegalArgumentException("a payload this version cannot read");
            ItemStack[] cells = new ItemStack[size];
            Arrays.fill(cells, tag.getBoolean("nulls") ? null : ItemStack.EMPTY);
            if (tag.contains("cells")) {
                var stored = ItemContainerContents.CODEC.parse(ops(), tag.get("cells")).getOrThrow(IllegalArgumentException::new);
                int cell = 0;
                for (ItemStack kept : stored.stream().toList()) {
                    if (cell >= size) break;
                    if (!kept.isEmpty()) cells[cell] = kept.copy();
                    cell++;
                }
            }
            return cells;
        }
        @Override public void setInventory(ItemStack stack, ItemStack[] contents) {
            if (contents == null || contents.length != size) throw new IllegalArgumentException("this pouch has " + size + " cells");
            var kept = new ArrayList<ItemStack>();
            for (ItemStack cell : contents) kept.add(cell == null ? ItemStack.EMPTY : cell.copy());
            Tag encoded = ItemContainerContents.CODEC.encodeStart(ops(), ItemContainerContents.fromItems(kept)).getOrThrow(IllegalStateException::new);
            CompoundTag tag = data(stack);
            tag.put("cells", encoded);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        ItemStack flagged(String flag) {
            ItemStack stack = new ItemStack(this);
            CompoundTag tag = data(stack);
            tag.putBoolean(flag, true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return stack;
        }
    }

    private static ItemStack[] cells(int size) { ItemStack[] out = new ItemStack[size]; Arrays.fill(out, ItemStack.EMPTY); return out; }

    private static ItemStack focus(FocusUpgradeRuntimeChecks.Actor a, String name) {
        ItemStack stack = a.focus(ThaumcraftFoci.FIRE_FOCUS.get(), FocusUpgradeType.frugal, 1, FocusUpgradeType.potency, 2);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static boolean select(FocusUpgradeRuntimeChecks.Actor a, int address) {
        return FocusSelectionNetworking.handle(a.player, new FocusSelectionPayload(InteractionHand.MAIN_HAND, address));
    }

    private static void click(ServerPlayer player, int slot, ClickType type) {
        var menu = player.containerMenu;
        player.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), slot, 0, type,
                ItemStack.EMPTY, new Int2ObjectOpenHashMap<>()));
    }

    /** Everything a selection may touch, as copies: the wand, the four accessory slots and the 36 main slots. */
    private static ItemStack[] snapshot(FocusUpgradeRuntimeChecks.Actor a) {
        ItemStack[] out = new ItemStack[41];
        out[0] = a.wand.copy();
        for (int slot = 0; slot < 4; slot++) out[1 + slot] = PlayerAccessories.get(a.player).getItem(slot).copy();
        for (int slot = 0; slot < 36; slot++) out[5 + slot] = a.player.getInventory().getItem(slot).copy();
        return out;
    }

    private static boolean same(ItemStack[] before, ItemStack[] after) {
        for (int slot = 0; slot < before.length; slot++) if (!ItemStack.matches(before[slot], after[slot])) return false;
        return true;
    }

    private static void refused(FocusUpgradeRuntimeChecks.Actor a, int address, String message) {
        ItemStack[] before = snapshot(a);
        boolean changed = select(a, address);
        a.require(!changed && same(before, snapshot(a)) && a.items().isEmpty(), message);
    }

    /**
     * The selector offers the foci of add-on pouches, worn and carried, native and add-on side by side; picking one writes
     * the cells back through the pouch's own methods into the stack that stays in its slot.
     */
    public static void offeredAndSelectedLikeTheNativeOne(GameTestHelper test) {
        try (var a = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 240, 4))) {
            access = a.level.registryAccess();
            var player = a.player;
            var worn = PlayerAccessories.get(player);
            ItemStack old = focus(a, "old installed focus"), f0 = focus(a, "belt cell 0"), f17 = focus(a, "belt cell 17"),
                    f18 = focus(a, "belt cell 18"), f39 = focus(a, "belt cell 39"), big127 = focus(a, "big pouch cell 127"),
                    nulls3 = focus(a, "null pouch cell 3"), native17 = focus(a, "native pouch cell 17");

            ItemStack belt = new ItemStack(pouch);
            ItemStack[] beltCells = cells(40);
            beltCells[0] = f0; beltCells[5] = new ItemStack(Items.STICK); beltCells[17] = f17; beltCells[18] = f18; beltCells[39] = f39;
            pouch.setInventory(belt, beltCells);
            worn.setItem(3, belt);

            ItemStack big = new ItemStack(bigPouch);
            ItemStack[] bigCells = cells(130);
            bigCells[127] = big127;
            bigPouch.setInventory(big, bigCells);
            player.getInventory().setItem(9, big);

            ItemStack nullPouch = pouch.flagged("nulls");
            ItemStack[] nullCells = cells(40);
            nullCells[3] = nulls3;
            pouch.setInventory(nullPouch, nullCells);
            player.getInventory().setItem(12, nullPouch);

            ItemFocusPouchBauble nativeItem = thaumcraft.common.config.ThaumcraftFocusPouches.FOCUS_POUCH.get();
            ItemStack nativePouch = new ItemStack(nativeItem);
            ItemStack[] nativeCells = cells(18);
            nativeCells[17] = native17;
            nativeItem.setInventory(nativePouch, nativeCells);
            player.getInventory().setItem(11, nativePouch);
            a.require(nativeItem instanceof IFocusPouch, "the native pouch is an IFocusPouch");
            a.equip(old, "gold", 137);

            // Listing: every focus of the first 128 cells of every pouch, non-focus items and unaddressed cells left out.
            var choices = FocusSelectionSources.choices(player);
            Set<Integer> offered = choices.stream().map(FocusSelectionSources.Choice::address).collect(Collectors.toSet());
            Set<Integer> expected = new HashSet<>(Set.of(FocusSelectionSources.wornPouch(3, 0), FocusSelectionSources.wornPouch(3, 17),
                    FocusSelectionSources.wornPouch(3, 18), FocusSelectionSources.wornPouch(3, 39), FocusSelectionSources.inventoryPouch(9, 127),
                    FocusSelectionSources.inventoryPouch(11, 17), FocusSelectionSources.inventoryPouch(12, 3)));
            a.require(choices.size() == 7 && offered.equals(expected),
                    "the add-on pouches (worn, 130 cells, null-reading) and the native one are offered, cell by cell: " + offered);
            a.require(choices.stream().anyMatch(c -> c.address() == FocusSelectionSources.wornPouch(3, 18) && ItemStack.matches(c.focus(), f18))
                            && choices.stream().anyMatch(c -> c.address() == FocusSelectionSources.inventoryPouch(9, 127) && ItemStack.matches(c.focus(), big127)),
                    "an offered cell carries its focus with every component, from a cell past the native eighteen");
            a.require(FocusSelectionSources.contents(nullPouch).length == 40 && Arrays.stream(FocusSelectionSources.contents(nullPouch)).noneMatch(java.util.Objects::isNull),
                    "a pouch that reads an empty cell as null is read as empty");

            // The wire form of an address past the native cells.
            var wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), a.level.registryAccess());
            try {
                for (int address : new int[] {FocusSelectionSources.wornPouch(3, 39), FocusSelectionSources.inventoryPouch(35, 127)}) {
                    var intent = new FocusSelectionPayload(InteractionHand.OFF_HAND, address);
                    FocusSelectionPayload.STREAM_CODEC.encode(wire, intent);
                    a.require(wire.readableBytes() == 4 && FocusSelectionPayload.STREAM_CODEC.decode(wire).equals(intent), "the intent for cell address " + address + " is the usual four bytes");
                }
                wire.writeByte(0);
                wire.writeByte(127);
                wire.writeShort(7400);
                boolean refusedAddress = false;
                try { FocusSelectionPayload.STREAM_CODEC.decode(wire); } catch (io.netty.handler.codec.DecoderException ignored) { refusedAddress = true; }
                a.require(refusedAddress, "an address past the last cell band cannot name anything");
            } finally { wire.release(); }

            // Intents the client can craft: each is refused and changes nothing.
            refused(a, FocusSelectionSources.wornPouch(3, 100), "a cell past the end of a 40-cell pouch is no cell");
            refused(a, FocusSelectionSources.wornPouch(2, 0), "a slot that holds no pouch has no cells");
            refused(a, FocusSelectionSources.wornPouch(3, 5), "a cell holding a stick is no focus and stays where it is");
            refused(a, FocusSelectionSources.inventoryPouch(9, 20), "an empty cell is no focus");
            refused(a, FocusSelectionSources.inventoryPouch(13, 0), "a main slot without a pouch has no cells");

            // Select a cell past the native eighteen of the worn add-on pouch.
            boolean first = select(a, FocusSelectionSources.wornPouch(3, 18));
            ItemStack[] afterFirst = pouch.getInventory(belt);
            a.require(first && ItemStack.matches(FocusData.get(a.wand), f18) && worn.getItem(3) == belt && afterFirst[18].isEmpty()
                            && ItemStack.matches(afterFirst[1], old) && ItemStack.matches(afterFirst[0], f0) && ItemStack.matches(afterFirst[17], f17)
                            && afterFirst[5].is(Items.STICK) && ItemStack.matches(afterFirst[39], f39) && a.vis("ignis") == 137,
                    "cell 18 of the worn add-on pouch moves into the wand, the old focus takes the pouch's first empty cell, the others stay, no vis is paid");
            a.require(belt.get(DataComponents.CONTAINER) == null && belt.get(DataComponents.CUSTOM_DATA) != null,
                    "the cells were written by the pouch's own method, into its own storage");

            // The big pouch in the main inventory: cell 127 is the last one with an address.
            boolean second = select(a, FocusSelectionSources.inventoryPouch(9, 127));
            a.require(second && ItemStack.matches(FocusData.get(a.wand), big127) && player.getInventory().getItem(9) == big
                            && bigPouch.getInventory(big)[127].isEmpty() && ItemStack.matches(pouch.getInventory(belt)[2], f18),
                    "cell 127 of the 130-cell pouch is selected, its stack stays in its slot, and the old focus goes to the worn pouch first");

            // A pouch that reads empty cells as null.
            boolean third = select(a, FocusSelectionSources.inventoryPouch(12, 3));
            a.require(third && ItemStack.matches(FocusData.get(a.wand), nulls3) && player.getInventory().getItem(12) == nullPouch
                            && pouch.getInventory(nullPouch)[3] == null && ItemStack.matches(pouch.getInventory(belt)[3], big127),
                    "the null-reading pouch is selected from and written back like any other");

            // The native pouch beside them is unchanged.
            boolean fourth = select(a, FocusSelectionSources.inventoryPouch(11, 17));
            a.require(fourth && ItemStack.matches(FocusData.get(a.wand), native17) && player.getInventory().getItem(11) == nativePouch
                            && nativeItem.getInventory(nativePouch)[17].isEmpty() && ItemStack.matches(pouch.getInventory(belt)[4], nulls3),
                    "the native pouch is selected from as before, next to the add-on ones");
            a.require(a.items().isEmpty(), "no focus was dropped or duplicated");
            long foci = 0;
            for (ItemStack stack : snapshot(a)) {
                var inside = FocusSelectionSources.contents(stack);
                if (inside != null) foci += Arrays.stream(inside).filter(FocusData::isSupportedFocus).count();
            }
            a.require(foci == 7 && ItemStack.matches(FocusData.get(a.wand), native17),
                    "all eight foci of the fixture are still there, seven in the pouches and the selected one on the wand: " + foci);
        }
        test.succeed();
    }

    /** Past the last addressed cell, an unreadable payload, and the return of a focus: nothing is lost and nothing is touched. */
    public static void beyondTheCapAndUnreadablePouches(GameTestHelper test) {
        try (var a = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 240, 4))) {
            access = a.level.registryAccess();
            var player = a.player;
            ItemStack old = focus(a, "old installed focus"), edge = focus(a, "cell 127"), beyond = focus(a, "cell 128"), hidden = focus(a, "in the unreadable pouch");

            // 130 cells: 0..126 hold sticks, cell 127 (the last one with an address) a focus, cell 128 a focus without an address.
            ItemStack big = new ItemStack(bigPouch);
            ItemStack[] bigCells = cells(130);
            for (int cell = 0; cell < FocusSelectionSources.MAX_CELLS - 1; cell++) bigCells[cell] = new ItemStack(Items.STICK);
            bigCells[127] = edge;
            bigCells[128] = beyond;
            bigPouch.setInventory(big, bigCells);
            player.getInventory().setItem(9, big);

            // setInventory does not read the payload, so the unreadable stack can be given its (foreign) cells.
            ItemStack unreadable = pouch.flagged("unreadable");
            ItemStack[] hiddenCells = cells(40);
            hiddenCells[0] = hidden;
            pouch.setInventory(unreadable, hiddenCells);
            player.getInventory().setItem(10, unreadable);
            a.equip(old, "gold", 137);
            ItemStack unreadableBefore = unreadable.copy();

            var choices = FocusSelectionSources.choices(player);
            a.require(FocusSelectionSources.contents(unreadable) == null && choices.size() == 1
                            && choices.get(0).address() == FocusSelectionSources.inventoryPouch(9, 127) && ItemStack.matches(choices.get(0).focus(), edge),
                    "only the focus in cell 127 is offered: the one in cell 128 has no address and the unreadable pouch is inactive");
            boolean refusedCell = false;
            try { FocusSelectionSources.inventoryPouch(9, 128); } catch (IllegalArgumentException expected) { refusedCell = true; }
            a.require(refusedCell && FocusSelectionSources.addressable(new ItemStack[300]) == 128, "cell 128 has no address");

            // The last addressed cell is selected; the old focus is returned to the cell that was just freed, not to 128 or 129.
            boolean selected = select(a, FocusSelectionSources.inventoryPouch(9, 127));
            ItemStack[] afterSelect = bigPouch.getInventory(big);
            a.require(selected && ItemStack.matches(FocusData.get(a.wand), edge) && ItemStack.matches(afterSelect[127], old)
                            && ItemStack.matches(afterSelect[128], beyond) && afterSelect[129].isEmpty() && afterSelect[0].is(Items.STICK),
                    "the old focus takes the freed cell 127 of the big pouch; cells 128 and 129 are not touched");

            // Every addressed cell is taken now: removing the focus does not use 128 or 129, it goes to a free slot.
            boolean removed = select(a, -1);
            ItemStack placed = ItemStack.EMPTY;
            for (int slot = 0; slot < 36; slot++) if (ItemStack.matches(player.getInventory().getItem(slot), edge)) placed = player.getInventory().getItem(slot);
            ItemStack[] afterBig = bigPouch.getInventory(big);
            a.require(removed && FocusData.get(a.wand).isEmpty() && !placed.isEmpty() && ItemStack.matches(afterBig[128], beyond) && afterBig[129].isEmpty()
                            && ItemStack.matches(afterBig[127], old),
                    "the removed focus is not put into cell 128 or 129 of the 130-cell pouch: they are not addressed; it goes to a free slot");
            a.require(ItemStack.matches(player.getInventory().getItem(10), unreadableBefore) && a.items().isEmpty(),
                    "the unreadable pouch is left byte for byte as it was");
            refused(a, FocusSelectionSources.inventoryPouch(10, 0), "an intent naming a cell of the unreadable pouch is refused");
        }
        test.succeed();
    }

    /** A pouch keeps its own use whether or not it is a belt bauble, and the menu still puts it on. */
    public static void addonPouchKeepsItsOwnUse(GameTestHelper test) {
        try (var a = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 240, 4))) {
            access = a.level.registryAccess();
            ServerPlayer player = a.player;
            var worn = PlayerAccessories.get(player);
            int base = pouch.uses;
            ItemStack held = new ItemStack(pouch);
            a.require(held.getItem() instanceof IBauble bauble && bauble.equipsOnUse(held, player) && worn.getItem(3).isEmpty(),
                    "the test pouch is a belt bauble that does not opt out of the hand equip itself");
            a.require(PlayerAccessories.freeSlotFor(player, held) == -1, "a pouch of an add-on is not put on by a click, like the native one");
            player.getInventory().setItem(player.getInventory().selected, held);
            boolean consumed = player.gameMode.useItem(player, a.level, held, InteractionHand.MAIN_HAND).consumesAction();
            a.require(consumed && pouch.uses == base + 1 && worn.getItem(3).isEmpty() && player.getMainHandItem() == held && held.getCount() == 1,
                    "the click runs the pouch's own use once and puts nothing on");

            a.require(worn.canPlaceItem(3, held), "the belt slot accepts it");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.getInventory().setItem(9, held);
            a.require(AccessoryNetworking.open(player) && player.containerMenu instanceof ContainerAccessories, "the real accessories receiver opens the menu");
            click(player, 4, ClickType.QUICK_MOVE);
            player.closeContainer();
            a.require(worn.getItem(3).is(pouch) && player.getInventory().getItem(9).isEmpty(), "the menu puts the pouch on, into the belt slot");
        }
        test.succeed();
    }
}
