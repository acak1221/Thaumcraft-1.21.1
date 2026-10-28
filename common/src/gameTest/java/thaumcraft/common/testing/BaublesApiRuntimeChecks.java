package thaumcraft.common.testing;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.BaublesApi;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.container.ContainerAccessories;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.network.AccessoryNetworking;

/**
 * The public bauble API through an add-on item: {@link IBauble#equipsOnUse} (the hand equip and its opt-out) and
 * {@link BaublesApi#getWorn} (the read-only view of what is worn).
 *
 * <p>The add-on here is the harness itself, as in {@link WandApiRuntimeChecks}: one test-only item enters the native item
 * registry from the GameTest mod's initialisation, where an add-on registers its items. The player is a real connected
 * survival player ({@link FocusUpgradeRuntimeChecks.Actor}); every click goes through the real use path of the server and
 * the real accessories menu.</p>
 */
public final class BaublesApiRuntimeChecks {
    private static Talisman talisman;
    private BaublesApiRuntimeChecks() {}

    /** Test-only items, registered while the native registry is still open (called from {@link WandApiRuntimeChecks#registerTestItems}). */
    static synchronized void registerTestItems() {
        if (talisman != null) return;
        talisman = Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "addon_talisman"),
                new Talisman(new Item.Properties()));
    }

    /**
     * An add-on talisman: a ring that is put on by a click, except while the player sneaks: a sneaking click runs its own
     * {@code use} (here it only counts), the way Thaumic Tinkerer's talismans toggle on a sneak click.
     */
    static final class Talisman extends Item implements IBauble {
        int uses;
        Talisman(Properties properties) { super(properties.stacksTo(1)); }
        @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.RING; }
        @Override public boolean equipsOnUse(ItemStack stack, Player player) { return !player.isShiftKeyDown(); }
        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            uses++;
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
    }

    private static void click(ServerPlayer player, int slot, ClickType type) {
        var menu = player.containerMenu;
        player.connection.handleContainerClick(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), slot, 0, type,
                ItemStack.EMPTY, new Int2ObjectOpenHashMap<>()));
    }

    private static void use(ServerPlayer player, ItemStack stack) {
        player.getInventory().setItem(player.getInventory().selected, stack);
        player.gameMode.useItem(player, player.serverLevel(), stack, InteractionHand.MAIN_HAND);
    }

    /**
     * The hand equip and its opt-out: an item that answers true (the default of every existing bauble) is put on by the real
     * use path; the same item answering false keeps its own {@code use}; the accessories menu still equips it.
     */
    public static void equipsOnUseOptOut(GameTestHelper test) {
        try (var a = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 240, 4))) {
            ServerPlayer player = a.player;
            var worn = PlayerAccessories.get(player);
            int base = talisman.uses;
            IBauble plain = stack -> BaubleType.AMULET;
            a.require(plain.equipsOnUse(new ItemStack(Items.STICK), player),
                    "an IBauble that does not override equipsOnUse answers true: every existing bauble keeps the hand equip");
            ItemStack mundane = new ItemStack(ThaumcraftBaubles.MUNDANE_AMULET.get());
            a.require(PlayerAccessories.freeSlotFor(player, mundane) == 0, "a native bauble is still put on by the click, into its own slot");

            // Not sneaking: the talisman is put on, its own use does not run.
            player.setShiftKeyDown(false);
            ItemStack first = new ItemStack(talisman);
            use(player, first);
            a.require(worn.getItem(1).is(talisman) && talisman.uses == base && player.getMainHandItem().isEmpty(),
                    "equipsOnUse true: the click puts the talisman into the first free ring slot and Item.use does not run");

            // Sneaking: equipsOnUse is false, the click goes to the item's own use.
            player.setShiftKeyDown(true);
            ItemStack second = new ItemStack(talisman);
            a.require(PlayerAccessories.freeSlotFor(player, second) == -1, "freeSlotFor answers no slot while equipsOnUse is false");
            player.getInventory().setItem(player.getInventory().selected, second);
            boolean consumed = player.gameMode.useItem(player, a.level, second, InteractionHand.MAIN_HAND).consumesAction();
            a.require(consumed && talisman.uses == base + 1 && worn.getItem(2).isEmpty() && player.getMainHandItem() == second && second.getCount() == 1,
                    "equipsOnUse false: the click reaches Item.use once, nothing is put on and the stack stays in the hand");
            use(player, second);
            a.require(talisman.uses == base + 2 && worn.getItem(2).isEmpty(), "every further click reaches Item.use too");

            // The menu is not asked: the same item, still sneaking, is put on by shift-click in the real accessories menu.
            a.require(worn.canPlaceItem(2, second), "the slot still accepts the item: canEquip and the slot type are unchanged");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.getInventory().setItem(9, second);
            a.require(AccessoryNetworking.open(player) && player.containerMenu instanceof ContainerAccessories, "the real accessories receiver opens the menu");
            click(player, 4, ClickType.QUICK_MOVE);
            player.closeContainer();
            a.require(worn.getItem(2).is(talisman) && player.getInventory().getItem(9).isEmpty(),
                    "the menu equip is unchanged: a shift-click puts the item on although equipsOnUse answers false");

            // And off again by the menu; back to not sneaking the click equips again.
            player.setShiftKeyDown(false);
            ItemStack third = new ItemStack(talisman);
            a.require(PlayerAccessories.freeSlotFor(player, third) == -1, "both ring slots are taken: no free slot, whatever the item answers");
            worn.removeItem(2, 1);
            a.require(PlayerAccessories.freeSlotFor(player, third) == 2, "the answer follows the player's state on every click: free again, and equipping again");
        }
        test.succeed();
    }

    /** {@link BaublesApi#getWorn}: slot order, no empty slots, copies, read-only, empty for anything without slots. */
    public static void getWornIsAReadOnlySnapshot(GameTestHelper test) {
        try (var a = new FocusUpgradeRuntimeChecks.Actor(test, new BlockPos(4, 240, 4))) {
            ServerPlayer player = a.player;
            var worn = PlayerAccessories.get(player);
            a.require(BaublesApi.getWorn(player).isEmpty(), "nothing worn: an empty list");

            ItemStack belt = new ItemStack(ThaumcraftBaubles.MUNDANE_BELT.get());
            ItemStack ring = new ItemStack(ThaumcraftBaubles.DISCOUNT_RINGS.get("aer").get());
            ItemStack amulet = new ItemStack(ThaumcraftBaubles.MUNDANE_AMULET.get());
            worn.setItem(3, belt);
            worn.setItem(2, ring);
            worn.setItem(0, amulet);
            List<ItemStack> list = BaublesApi.getWorn(player);
            a.require(list.size() == 3 && ItemStack.matches(list.get(0), amulet) && ItemStack.matches(list.get(1), ring)
                            && ItemStack.matches(list.get(2), belt),
                    "worn accessories in slot order (amulet, ring, belt), the empty first ring slot left out");

            boolean refused = false;
            try { list.add(new ItemStack(Items.STICK)); } catch (UnsupportedOperationException expected) { refused = true; }
            boolean refusedSet = false;
            try { list.set(0, ItemStack.EMPTY); } catch (UnsupportedOperationException expected) { refusedSet = true; }
            boolean refusedRemove = false;
            try { list.remove(0); } catch (UnsupportedOperationException expected) { refusedRemove = true; }
            a.require(refused && refusedSet && refusedRemove && list.size() == 3, "the list cannot be modified");

            list.get(0).set(DataComponents.CUSTOM_NAME, Component.literal("changed copy"));
            list.get(1).setCount(5);
            a.require(list.get(0) != worn.getItem(0) && worn.getItem(0).get(DataComponents.CUSTOM_NAME) == null && worn.getItem(2).getCount() == 1,
                    "the stacks are independent copies: changing them changes nothing that is worn");

            worn.removeItem(2, 1);
            a.require(list.size() == 3 && BaublesApi.getWorn(player).size() == 2 && BaublesApi.getWorn(player).get(1).is(belt.getItem()),
                    "a snapshot: the list in hand is unchanged, a new call answers the new state");

            ItemStack tal = new ItemStack(talisman);
            use(player, tal);
            a.require(BaublesApi.getWorn(player).stream().anyMatch(stack -> stack.is(talisman)),
                    "an add-on bauble put on by the click is listed (the \"is X worn\" check)");

            var zombie = EntityType.ZOMBIE.create(a.level);
            a.require(zombie != null && BaublesApi.getWorn(zombie).isEmpty() && BaublesApi.getWorn(null).isEmpty(),
                    "an entity without accessory slots, and null, answer an empty list");
        }
        test.succeed();
    }
}
