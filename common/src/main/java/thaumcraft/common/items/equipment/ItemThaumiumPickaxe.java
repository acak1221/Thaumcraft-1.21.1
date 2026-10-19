package thaumcraft.common.items.equipment;

import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import thaumcraft.api.IRepairable;

public class ItemThaumiumPickaxe extends PickaxeItem implements IRepairable {
    public ItemThaumiumPickaxe(Tier tier, Properties properties) {
        // Original class is Pickaxe (not Pick); legacy additive damage is2+2.
        super(tier, properties.rarity(Rarity.UNCOMMON).attributes(DiggerItem.createAttributes(tier, 2, -2.8F)));
    }
}
