package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;

/**
 * Original ItemCultistLeaderArmor: the helm, the chestplate and the greaves of the crimson Praetor. They use the original
 * armorMatThaumiumFortress material (protection 3/7/6, the durability factor 40 and the enchantability 25), are rare and are
 * mended with an iron ingot: the original repair check names the iron ingot, not the thaumium one. The original class has no
 * warp and no vis discount.
 */
public class ItemCultistLeaderArmor extends ArmorItem implements IRepairable, IRunicArmor {
    public ItemCultistLeaderArmor(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.durability(type.getDurability(40)).rarity(Rarity.RARE));
    }

    @Override public int getRunicCharge(ItemStack stack) { return 0; }

    /** The original repair check: an iron ingot, even for the thaumium fortress material. */
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(net.minecraft.world.item.Items.IRON_INGOT);
    }
}
