package com.example.tc4addon;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.BaublesApi;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.wands.WandHelper;

/**
 * Talisman of Order: a ring with a switch. A sneaking click wakes or rests it; while awake and worn it refills the wand in the
 * wearer's hand, twice as fast beside the Amulet of Order. It shows the two ways an add-on reaches the accessory slots: an item
 * that keeps the right click for itself ({@link #equipsOnUse}), and the read-only list of what is worn
 * ({@link BaublesApi#getWorn}).
 */
// [doc:talisman]
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
// [/doc]
