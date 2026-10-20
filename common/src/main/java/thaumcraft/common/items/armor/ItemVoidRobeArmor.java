package thaumcraft.common.items.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.IGoggles;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.IWarpingGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.common.items.VoidMetal;

/**
 * Original ItemVoidRobeArmor (the Void Thaumaturge hood, robe and leggings). Of the void armor material (protection 3/7/6, durability
 * factor 10 -> 110/160/150, enchantability 10, mended with the void ingot) it keeps the self-repair (once from the inventory tick, once
 * more from the armor tick while worn) and adds 2 warp; of the goggles the hood keeps the node sight and the in-world popups (the other
 * pieces show neither); it gives every piece a 5 % vis discount, a magic ward of {@code armor / 35} ({@link MagicWardGear}) and the
 * robes' dye (default 6961280 = 0x6A3880, washed in a water cauldron). Epic rarity. The original ISpecialArmor properties for ordinary
 * damage ({@code armor / 25}, nothing against unblockable damage) are what the vanilla armor attributes already do; armor does not wear
 * from falls in 1.21.1 as the original's {@code damageArmor} arranged.
 */
public class ItemVoidRobeArmor extends ArmorItem implements IRepairable, IRunicArmor, IVisDiscountGear, IGoggles, IRevealer, IWarpingGear, DyedRobe, MagicWardGear {
    public static final int VIS_DISCOUNT = 5, WARP = 2, DURABILITY_FACTOR = 10;
    private static final DyedItemColor DEFAULT_DYED_COLOR = new DyedItemColor(ItemRobeArmor.DEFAULT_COLOR, false);

    public ItemVoidRobeArmor(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.durability(switch (type) {
            case HELMET, CHESTPLATE, LEGGINGS -> type.getDurability(DURABILITY_FACTOR);
            default -> throw new IllegalArgumentException("Void thaumaturge robes have no boots");
        }).rarity(Rarity.EPIC).component(DataComponents.DYED_COLOR, DEFAULT_DYED_COLOR));
    }

    @Override public int getRunicCharge(ItemStack stack) { return 0; }
    @Override public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) { return VIS_DISCOUNT; }
    @Override public int getWarp(ItemStack stack, Player player) { return WARP; }
    @Override public int wardArmor(ItemStack stack) { return getDefense(); }

    /** Original showNodes / showIngamePopups: {@code armorType == 0}, the hood alone. */
    @Override public boolean showNodes(ItemStack stack, LivingEntity wearer) { return type == Type.HELMET; }
    @Override public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) { return type == Type.HELMET; }

    /** Original hasColor is always true, as for the enchanted robes. */
    public boolean hasColor(ItemStack stack) { return true; }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidMetal.armorTick(stack, entity, getEquipmentSlot());
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        super.appendHoverText(stack, context, tooltip, flags);
        tooltip.add(Component.translatable("tc.visdiscount").append(": " + VIS_DISCOUNT + "%").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        InteractionResult washed = ItemRobeArmor.wash(context);
        return washed != null ? washed : super.useOn(context);
    }
}
