package thaumcraft.common.items.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.IGoggles;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.IRevealer;

/** Original ItemGoggles:44-104. Markers do not implement Repair or runic shielding. */
public class ItemGoggles extends ArmorItem implements IRepairable, IRunicArmor, IVisDiscountGear, IRevealer, IGoggles {
    public ItemGoggles(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties.durability(350).rarity(Rarity.RARE));
    }

    @Override public int getRunicCharge(ItemStack stack) { return 0; }
    @Override public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) { return 5; }
    @Override public boolean showNodes(ItemStack stack, LivingEntity wearer) { return true; }
    @Override public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) { return true; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        // Native TooltipContext has no player; the original goggles' discount is constant.
        tooltip.add(Component.translatable("tc.visdiscount").append(": " + getVisDiscount(stack, null, null) + "%")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
