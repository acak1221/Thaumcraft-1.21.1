package thaumcraft.common.items.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.IWarpingGear;
import thaumcraft.api.aspects.Aspect;

/**
 * Original ItemCultistBoots: the boots of the crimson cult. They use the vanilla iron material (protection 2, the iron durability
 * factor 15 and the iron enchantability 15), are uncommon, are mended with an iron ingot, add one point of warp and give one
 * percent off every vis payment. The original tooltip adds the vis discount line and nothing else: it never calls the super
 * method, so the piece shows no armour and no durability line.
 */
public class ItemCultistBoots extends ArmorItem implements IRepairable, IRunicArmor, IWarpingGear, IVisDiscountGear {
    /** The original warp of the cultist boots. */
    public static final int WARP = 1;
    /** The original vis discount of the cultist boots, in percent. */
    public static final int VIS_DISCOUNT = 1;

    public ItemCultistBoots(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.durability(type.getDurability(15)).rarity(Rarity.UNCOMMON));
    }

    @Override public int getRunicCharge(ItemStack stack) { return 0; }
    @Override public int getWarp(ItemStack stack, Player player) { return WARP; }
    @Override public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) { return VIS_DISCOUNT; }

    /** The original repair check: an iron ingot, like the vanilla iron armour. */
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.IRON_INGOT);
    }

    /** The original tooltip never called the super method, so the boots show no armour and no durability line. */
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        tooltip.add(Component.translatable("tc.visdiscount").append(": " + VIS_DISCOUNT + "%").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
