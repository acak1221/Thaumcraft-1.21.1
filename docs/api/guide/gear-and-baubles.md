# Gear and baubles

Thaumcraft asks the items a player holds and wears for a handful of interfaces; implementing one is enough, nothing is
registered. All of them are the original `thaumcraft.api` interfaces with the 1.21.1 types.

## Baubles: `IBauble`

1.7.10 Thaumcraft relied on the Baubles mod; this port has the same four slots of its own: an amulet, two rings and a
belt. A player opens them with the Baubles key (B by default) or the button next to the player in the inventory, and a
bauble used from the hand goes into the first free slot of its type (an item can keep its right click for itself, see
[below](#a-bauble-that-keeps-its-right-click)).

<!-- snippet:amulet -->
```java
public class OrderAmulet extends Item implements IBauble, IVisDiscountGear, IRunicArmor, IWarpingGear {
    public OrderAmulet(Properties properties) { super(properties.stacksTo(1)); }

    @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.AMULET; }

    /** Every tick while worn, on both sides; this one acts on the server once every five seconds. */
    @Override public void onWornTick(ItemStack stack, LivingEntity wearer) {
        if (wearer.level().isClientSide || wearer.tickCount % 100 != 0 || !(wearer instanceof Player player)) return;
        ItemStack wand = player.getMainHandItem();
        if (WandHelper.isWand(wand)) WandHelper.addVis(wand, Aspect.ORDER, 1, true); // whole vis
    }

    /** Percentage points; the aspect is null when a tooltip asks for the general discount. */
    @Override public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) { return aspect == Aspect.ORDER ? 10 : 2; }

    @Override public int getRunicCharge(ItemStack stack) { return 2; }
    @Override public int getWarp(ItemStack stack, Player player) { return 1; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        tooltip.add(Component.translatable("tc.visdiscount").append(": 2%").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tc.visdiscount").append(" (" + Aspect.ORDER.getName() + "): 10%").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.warping").append(" 1").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
```
<!-- /snippet -->

| Method | Contract |
|---|---|
| `getBaubleType(stack)` | `BaubleType.AMULET`, `RING` or `BELT`: the slot it fits |
| `onWornTick(stack, wearer)` | every tick while worn, on both sides; check `level().isClientSide` before changing anything |
| `onEquipped`, `onUnequipped` | when it enters or leaves a slot (by the menu, by a right click, by shift-click) |
| `canEquip`, `canUnequip` | may refuse; both default to `true` |
| `equipsOnUse(stack, player)` | whether a right click with the item in the hand puts it on; defaults to `true`. Decides the hand equip only |

The slots are saved with the player, dropped on death (unless `keepInventory`), and synchronised to the client.

### A bauble that keeps its right click

The hand equip is the port's own convenience (TC4 and the Baubles mod only had the menu) and it runs where the click would
otherwise call `Item.use`. An item whose right click does something else, a talisman that toggles or a belt that opens a
screen, answers `equipsOnUse(stack, player)` with `false`, and the click goes on to `Item.use` (as it does anyway when no
slot of the item's kind is free). The default is `true`, so no existing bauble changes. The method decides the hand equip and nothing else: the menu, a shift-click into it,
`getBaubleType` and `canEquip` work as before, so the player still puts such an item on there.

The client asks as well, for the same click, so decide from the stack and from state the client has too (for example
`player.isShiftKeyDown()`) and change nothing. A [focus pouch](wands-and-foci.md#focus-pouches-ifocuspouch) is never put on by a
click, whatever it answers.

<!-- snippet:talisman -->
```java
public class OrderTalisman extends Item implements IBauble {
    public OrderTalisman(Properties properties) { super(properties.stacksTo(1)); }

    @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.RING; }

    /**
     * A click puts the talisman on, as it does every bauble, except while sneaking: that click is the talisman's own and
     * reaches {@link #use}. Only the hand equip is decided here; the Baubles menu still puts it on and takes it off. The
     * client asks as well, for the same click, so the answer depends on state both sides have.
     */
    @Override public boolean equipsOnUse(ItemStack stack, Player player) { return !player.isShiftKeyDown(); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("awake", !tag.getBoolean("awake")));
            player.displayClientMessage(Component.translatable(isAwake(stack) ? "tc4_example.order_talisman.awake" : "tc4_example.order_talisman.asleep"), true);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override public void onWornTick(ItemStack stack, LivingEntity wearer) {
        if (wearer.level().isClientSide || wearer.tickCount % 100 != 0 || !(wearer instanceof Player player) || !isAwake(stack)) return;
        ItemStack wand = player.getMainHandItem();
        if (!WandHelper.isWand(wand)) return;
        // "Is it worn?": copies of the stacks in the accessory slots, in slot order, without the empty slots.
        boolean paired = BaublesApi.getWorn(player).stream().anyMatch(worn -> worn.is(ExampleAddon.ORDER_AMULET));
        WandHelper.addVis(wand, Aspect.AIR, paired ? 2 : 1, true); // whole vis
    }

    private static boolean isAwake(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("awake");
    }

    @Override public boolean isFoil(ItemStack stack) { return isAwake(stack); }
}
```
<!-- /snippet -->

### What is worn: `BaublesApi.getWorn`

`BaublesApi.getWorn(entity)` answers the "is X worn" question. It returns an unmodifiable list of **copies** of the stacks in
the accessory slots, in slot order (amulet, ring, ring, belt), without the empty slots. It is empty for `null` and for any
entity that has no accessory slots (everything but a player); on a client it knows the local player's slots only. It is a
snapshot, so ask again when it may have changed. To change a worn item use the stack its own callbacks receive
(`onWornTick`), which is the live one. The talisman above reads it to pair with the amulet.

## What worn and held gear can carry

| Interface | Read from | Effect |
|---|---|---|
| `IVisDiscountGear` | the four armour slots and the four bauble slots | `getVisDiscount(stack, player, aspect)` in **percentage points**, summed; `aspect` is null when a tooltip asks for the general discount |
| `IRunicArmor` | the four armour slots and the four bauble slots | `getRunicCharge(stack)`: points of runic shield; the tooltip line "Runic shield +N" is added for you |
| `IWarpingGear` | the main hand, the four armour slots and the four bauble slots | `getWarp(stack, player)`: warp that counts while held or worn, in the warp events |
| `IGoggles` | the head slot | `showIngamePopups(stack, wearer)`: aspect popups over containers and nodes |
| `IRevealer` (`thaumcraft.api.nodes`) | the head slot | `showNodes(stack, wearer)`: aura nodes drawn in full |

Your own tooltip lines (the discount, the warp) are yours to add, as in 1.7.10. `ItemRunic(charge)` is the original
plain item with a fixed runic charge. The `thaumcraft.api.items` package holds the 1.12 aliases of `IGoggles`,
`IRevealer` and `IVisDiscountGear` for add-ons compiled against Thaumcraft 6; they extend the TC4 interfaces.

## Repair, ink and architect foci

- `IRepairable`: an item with the Repair enchantment that implements it mends itself every two seconds, paying vis from
  the wands in the inventory (original `EventHandlerEntity`). `IRepairableExtended.doRepair(stack, player, level)` may
  do it its own way or refuse.
- `IScribeTools`: an item whose durability is ink for the research table.
- `IArchitect`: a focus that shows the architect overlay (the blocks it would affect and the axis selector) through
  `getArchitectBlocks`, `showAxis` and `getMaxAreaSize`.

## Materials

`ThaumcraftMaterials` holds the original tool tiers (`TOOLMAT_THAUMIUM`, `TOOLMAT_VOID`, `TOOLMAT_ELEMENTAL`) and the
armour materials, which are registry objects in 1.21.1: `armorMatThaumium()`, `armorMatThaumiumFortress()`,
`armorMatVoid()`, `armorMatVoidRobe()`.
