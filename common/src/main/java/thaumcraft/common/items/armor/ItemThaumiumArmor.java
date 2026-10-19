package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;

public class ItemThaumiumArmor extends ArmorItem implements IRepairable, IRunicArmor {
    public ItemThaumiumArmor(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.durability(type.getDurability(25)).rarity(Rarity.UNCOMMON));
    }

    @Override
    public int getRunicCharge(ItemStack stack) {
        return 0; // Original constant, not an implemented shield or runic upgrade.
    }
}
