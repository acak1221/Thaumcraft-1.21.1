package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;

/**
 * Original ItemCultistPlateArmor: the helm, the chestplate and the greaves of the crimson cult knight. They use the vanilla iron
 * material (protection 2/6/5, the iron durability factor 15 and the iron enchantability 15), are uncommon and are mended with an
 * iron ingot. The original class has no warp and no vis discount.
 */
public class ItemCultistPlateArmor extends ArmorItem implements IRepairable, IRunicArmor {
    public ItemCultistPlateArmor(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.durability(type.getDurability(15)).rarity(Rarity.UNCOMMON));
    }

    @Override public int getRunicCharge(ItemStack stack) { return 0; }

    /** The original repair check: an iron ingot, like the vanilla iron armour. */
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.IRON_INGOT);
    }
}
