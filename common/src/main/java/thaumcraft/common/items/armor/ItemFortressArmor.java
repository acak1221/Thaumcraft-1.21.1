package thaumcraft.common.items.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
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
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.common.config.ThaumcraftEquipment;

/**
 * Original ItemFortressArmor (the Thaumium Fortress helm, cuirass and thigh guards): the diamond-like protection 3/7/6 with a durability
 * factor of 40 (440/640/600), enchantability 25, rare, mended with the thaumium ingot. A piece also stands against magic damage
 * ({@code armor / 35}) and against fire and explosions ({@code armor / 20}), and a player wearing several is protected more: the set
 * multiplier {@link FortressRules#set} multiplies every share and the ordinary armor too (see {@link MagicWardGear}, {@code MagicWard}).
 * The helm can be given the goggles' sight (no vis discount) and one of three masks by infusion; both are marked on the stack
 * ({@code fortress_goggles}, {@code fortress_mask}), as the original's NBT {@code goggles} and {@code mask}.
 */
public class ItemFortressArmor extends ArmorItem implements IRepairable, IRunicArmor, IGoggles, IRevealer, MagicWardGear {
    private static final EquipmentSlot[] SET_SLOTS = {EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    public ItemFortressArmor(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.durability(switch (type) {
            case HELMET, CHESTPLATE, LEGGINGS -> type.getDurability(FortressRules.DURABILITY_FACTOR);
            default -> throw new IllegalArgumentException("The Thaumium Fortress armor has no boots");
        }).rarity(Rarity.RARE));
    }

    @Override public int getRunicCharge(ItemStack stack) { return 0; }
    @Override public int wardArmor(ItemStack stack) { return getDefense(); }
    @Override public boolean wardsFire() { return true; }

    /** The set multiplier applies to players only, as in the original. */
    @Override public double wardFactor(ItemStack stack, LivingEntity wearer) { return wearer instanceof Player player ? set(player) : 1.0; }

    /** Original: the helm gives the goggles' sight when it carries the goggles tag. */
    @Override public boolean showNodes(ItemStack stack, LivingEntity wearer) { return stack.has(ThaumcraftEquipment.FORTRESS_GOGGLES.get()); }
    @Override public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) { return stack.has(ThaumcraftEquipment.FORTRESS_GOGGLES.get()); }

    /** The mask on the stack (0 Grinning Devil, 1 Angry Ghost, 2 Sipping Fiend) or -1. */
    public static int mask(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemFortressArmor ? stack.getOrDefault(ThaumcraftEquipment.FORTRESS_MASK.get(), -1) : -1;
    }

    /** The set multiplier of a player's worn pieces: legs, chest and helm count, boots do not. */
    public static double set(Player player) {
        boolean[] worn = new boolean[SET_SLOTS.length], masked = new boolean[SET_SLOTS.length];
        for (int index = 0; index < SET_SLOTS.length; index++) {
            ItemStack piece = player.getItemBySlot(SET_SLOTS[index]);
            worn[index] = !piece.isEmpty() && piece.getItem() instanceof ItemFortressArmor;
            masked[index] = worn[index] && piece.has(ThaumcraftEquipment.FORTRESS_MASK.get());
        }
        return FortressRules.set(worn, masked);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flags) {
        if (stack.has(ThaumcraftEquipment.FORTRESS_GOGGLES.get())) tooltip.add(Component.translatable("item.ItemGoggles.name").withStyle(ChatFormatting.DARK_PURPLE));
        if (stack.has(ThaumcraftEquipment.FORTRESS_MASK.get())) {
            tooltip.add(Component.translatable("item.HelmetFortress.mask." + stack.get(ThaumcraftEquipment.FORTRESS_MASK.get())).withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack, context, tooltip, flags);
    }
}
