package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;

/** Original SPECIAL feet armor,350 durability, rare. Movement belongs to the
 * actual worn tick and native Player movement hooks, never inventory presence. */
public final class ItemBootsTraveller extends ArmorItem implements IRepairable,IRunicArmor {
    public ItemBootsTraveller(Holder<ArmorMaterial> material,Properties properties) { super(material,Type.BOOTS,properties.durability(350).rarity(Rarity.RARE)); }
    @Override public int getRunicCharge(ItemStack stack) { return 0; }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack material) { return false; }
}
