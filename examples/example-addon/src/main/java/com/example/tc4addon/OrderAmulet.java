package com.example.tc4addon;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.IWarpingGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;
import thaumcraft.api.wands.WandHelper;

/**
 * Amulet of Order: a bauble for the amulet slot that discounts Ordo, carries runic charge and one warp, and slowly refills
 * the wand in the wearer's hand. Every one of these is an original TC4 interface; the port reads them from the four
 * accessory slots (amulet, two rings, belt) exactly as 1.7.10 read them from the Baubles slots.
 */
// [doc:amulet]
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
// [/doc]
