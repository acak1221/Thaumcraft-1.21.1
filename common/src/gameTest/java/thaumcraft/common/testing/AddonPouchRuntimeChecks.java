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
     * (a payload of another version: {@link #getInventory} throws {@link IllegalArgumentException}), "nulls" (empty cells read as
     * {@code null}), or one of the add-on bugs the selector must survive: "explodes" ({@link #getInventory} throws
     * {@link IllegalStateException}, as an unwrapped codec error would), "rejectsWrites" ({@link #setInventory} throws before it
     * writes) and "poisonOnWrite" ({@link #setInventory} writes a marker that {@link #getInventory} cannot read back).
     * {@link #writesLeft} makes every write after the n-th write its cells and then throw.
     */
    static final class Pouch extends Item implements IBauble, IFocusPouch {
        /** -1: never fails; n >= 0: n more calls of {@link #setInventory} succeed, every later one writes and then throws. */
        static int writesLeft = -1;
        /** The stacks {@link #setInventory} was called with, by identity, in order (a scenario clears it before it looks). */
        static final java.util.List<ItemStack> written = new ArrayList<>();
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
            if (tag.getBoolean("unreadable") || tag.getBoolean("poisoned")) throw new IllegalArgumentException("a payload this version cannot read");
            if (tag.getBoolean("explodes")) throw new IllegalStateException("a bug of the add-on");
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
            written.add(stack);
            if (data(stack).getBoolean("rejectsWrites")) throw new IllegalStateException("this pouch cannot be written");
            var kept = new ArrayList<ItemStack>();
            for (ItemStack cell : contents) kept.add(cell == null ? ItemStack.EMPTY : cell.copy());
            Tag encoded = ItemContainerContents.CODEC.encodeStart(ops(), ItemContainerContents.fromItems(kept)).getOrThrow(IllegalStateException::new);
            CompoundTag tag = data(stack);
            tag.put("cells", encoded);
            if (tag.getBoolean("poisonOnWrite")) tag.putBoolean("poisoned", true);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (writesLeft == 0) throw new IllegalStateException("this pouch fails after it has written");
            if (writesLeft > 0) writesLeft--;
        }
        ItemStack flagged(String flag) { return flag(new ItemStack(this), flag, true); }
        /** Sets or clears a flag of a stack of this item (its cells are kept). */
        static ItemStack flag(ItemStack stack, String flag, boolean on) {
            CompoundTag tag = data(stack);
            if (on) tag.putBoolean(flag, true); else tag.remove(flag);
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

    /**
     * Pouches with bugs: one whose getInventory throws something other than IllegalArgumentException, one whose setInventory
     * throws while the selector plans, and one that writes a payload it cannot read back. The selector (listing and selection)
     * stays up, and every refusal leaves the wand, the pouches and the slots byte for byte as they were.
     */
    public static void misbehavingPouchesAreContained(GameTestHelper test) {
        try (var a = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 240, 4))) {
            access = a.level.registryAccess();
            Pouch.writesLeft = -1;
            var player = a.player;
            var worn = PlayerAccessories.get(player);
            ItemStack old = focus(a, "old installed focus"), goodFocus = focus(a, "focus of the good pouch"), blast = focus(a, "in the exploding pouch"),
                    refusing = focus(a, "in the pouch that refuses writes"), poisoned = focus(a, "in the pouch that poisons its write");

            // main 8 poisons what it writes (readable until it is written), 9 explodes when read, 10 is fine, the worn belt refuses writes.
            ItemStack poison = new ItemStack(pouch);
            ItemStack[] poisonCells = cells(40);
            poisonCells[0] = poisoned;
            pouch.setInventory(poison, poisonCells);
            Pouch.flag(poison, "poisonOnWrite", true);
            player.getInventory().setItem(8, poison);

            ItemStack exploding = pouch.flagged("explodes");
            ItemStack[] explodingCells = cells(40);
            explodingCells[0] = blast;
            pouch.setInventory(exploding, explodingCells);
            player.getInventory().setItem(9, exploding);

            ItemStack good = new ItemStack(pouch);
            ItemStack[] goodCells = cells(40);
            goodCells[0] = goodFocus;
            pouch.setInventory(good, goodCells);
            player.getInventory().setItem(10, good);

            ItemStack refuses = new ItemStack(pouch);
            ItemStack[] refusesCells = cells(40);
            refusesCells[0] = refusing;
            pouch.setInventory(refuses, refusesCells);
            Pouch.flag(refuses, "rejectsWrites", true);
            worn.setItem(3, refuses);
            a.equip(old, "gold", 137);
            ItemStack explodingBefore = exploding.copy(), refusesBefore = refuses.copy();

            // A pouch that throws IllegalStateException from getInventory is inactive; the exception reaches neither caller.
            a.require(FocusSelectionSources.contents(exploding) == null, "a pouch whose getInventory throws IllegalStateException reads as inactive");
            Set<Integer> offered = FocusSelectionSources.choices(player).stream().map(FocusSelectionSources.Choice::address).collect(Collectors.toSet());
            a.require(offered.equals(Set.of(FocusSelectionSources.inventoryPouch(8, 0), FocusSelectionSources.inventoryPouch(10, 0), FocusSelectionSources.wornPouch(3, 0))),
                    "the selector lists the other pouches and skips the exploding one: " + offered);
            refused(a, FocusSelectionSources.inventoryPouch(9, 0), "an intent naming a cell of the exploding pouch is refused");

            // setInventory throws while the plan is made on a scratch copy: the pouch cannot give its focus up.
            refused(a, FocusSelectionSources.wornPouch(3, 0), "a pouch that cannot be written cannot give a focus up");

            // The write succeeds but its result cannot be read back: the plan is refused, whether the pouch is the source ...
            refused(a, FocusSelectionSources.inventoryPouch(8, 0), "a source pouch whose written copy cannot be read back refuses the selection");
            // ... or the one the old focus would be returned to (the worn belt that refuses writes is skipped, then slot 8 comes first).
            refused(a, FocusSelectionSources.inventoryPouch(10, 0), "a pouch the old focus would be returned to, whose written copy cannot be read back, refuses the selection");
            a.require(ItemStack.matches(player.getInventory().getItem(8), poison) && !Pouch.data(poison).getBoolean("poisoned"),
                    "the live poisoned-on-write pouch was never written: it is still readable");

            // Control: the same selection works once the pouch writes properly. A pouch that refuses writes is skipped for the
            // return of the old focus, and none of the two broken pouches is written on its live stack.
            Pouch.flag(poison, "poisonOnWrite", false);
            Pouch.written.clear();
            boolean selected = select(a, FocusSelectionSources.inventoryPouch(10, 0));
            ItemStack[] poisonAfter = pouch.getInventory(poison);
            a.require(selected && ItemStack.matches(FocusData.get(a.wand), goodFocus) && ItemStack.matches(poisonAfter[1], old)
                            && ItemStack.matches(poisonAfter[0], poisoned) && pouch.getInventory(good)[0].isEmpty(),
                    "with the flag cleared the selection works: the old focus goes to the first pouch that takes it, the belt that refuses writes is skipped");
            a.require(Pouch.written.stream().noneMatch(w -> w == refuses || w == exploding)
                            && ItemStack.matches(refuses, refusesBefore) && ItemStack.matches(exploding, explodingBefore) && worn.getItem(3) == refuses
                            && player.getInventory().getItem(9) == exploding && a.items().isEmpty(),
                    "the exploding pouch and the pouch that refuses writes are byte for byte as they were, and no focus was dropped");
        } finally {
            Pouch.writesLeft = -1;
        }
        test.succeed();
    }

    /**
     * A pouch that fails on the live stack after the plan was made and read back: the selection is refused and every stack that
     * was already written, a pouch rewritten in place and a plain slot that was emptied, is put back as it was.
     */
    public static void aPouchThatFailsAtCommitLeavesEverythingAsItWas(GameTestHelper test) {
        try (var a = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 240, 4))) {
            access = a.level.registryAccess();
            Pouch.writesLeft = -1;
            var player = a.player;
            var worn = PlayerAccessories.get(player);
            ItemStack old = focus(a, "old installed focus"), fx = focus(a, "in the main pouch"), fy = focus(a, "in the worn pouch"),
                    loose = focus(a, "loose in a slot");

            ItemStack x = new ItemStack(pouch);
            ItemStack[] xCells = cells(40);
            xCells[0] = fx;
            pouch.setInventory(x, xCells);
            player.getInventory().setItem(9, x);
            ItemStack y = new ItemStack(pouch);
            ItemStack[] yCells = cells(40);
            yCells[3] = fy;
            pouch.setInventory(y, yCells);
            worn.setItem(3, y);
            ItemStack looseStack = loose.copy();
            player.getInventory().setItem(20, looseStack);
            a.equip(old, "gold", 137);

            // Source pouch X in the main inventory, the old focus goes into the worn pouch Y. Two writes plan it (one per scratch
            // copy), the third is X on its live stack and the fourth, Y on its live stack, writes and then throws.
            Pouch.writesLeft = 3;
            Pouch.written.clear();
            ItemStack[] before = snapshot(a);
            boolean changed = select(a, FocusSelectionSources.inventoryPouch(9, 0));
            a.require(Pouch.writesLeft == 0 && Pouch.written.size() == 4 && Pouch.written.get(2) == x && Pouch.written.get(3) == y,
                    "the failure came at the commit: both live stacks were written, X first and then Y, which threw after it wrote");
            a.require(!changed && same(before, snapshot(a)) && player.getInventory().getItem(9) == x && worn.getItem(3) == y && a.items().isEmpty(),
                    "X has its focus back, Y has no copy of the old focus, the wand keeps its focus, and both stacks are the same objects in their slots");
            Pouch.writesLeft = -1;
            boolean second = select(a, FocusSelectionSources.inventoryPouch(9, 0));
            a.require(second && ItemStack.matches(FocusData.get(a.wand), fx) && pouch.getInventory(x)[0].isEmpty() && ItemStack.matches(pouch.getInventory(y)[0], old),
                    "control: without the failure the same selection moves the focus to the wand and the old one into Y");

            // A plain slot is emptied before the worn pouch fails: its original stack object is put back.
            Pouch.writesLeft = 1;
            Pouch.written.clear();
            before = snapshot(a);
            boolean loosely = select(a, 20);
            a.require(Pouch.writesLeft == 0 && Pouch.written.size() == 2 && Pouch.written.get(1) == y,
                    "the failure came at the commit, on the worn pouch, after the plain slot had been emptied");
            a.require(!loosely && same(before, snapshot(a)) && player.getInventory().getItem(20) == looseStack && worn.getItem(3) == y && a.items().isEmpty()
                            && ItemStack.matches(FocusData.get(a.wand), fx),
                    "the plain slot has its original stack back, Y is as it was, and the wand keeps its focus");
            Pouch.writesLeft = -1;
            boolean third = select(a, 20);
            a.require(third && ItemStack.matches(FocusData.get(a.wand), loose) && player.getInventory().getItem(20).isEmpty() && ItemStack.matches(pouch.getInventory(y)[1], fx),
                    "control: without the failure the loose focus is mounted and the old one goes into Y");
        } finally {
            Pouch.writesLeft = -1;
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
