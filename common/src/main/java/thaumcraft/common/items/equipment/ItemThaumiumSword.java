package thaumcraft.common.items.equipment;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import thaumcraft.api.IRepairable;

public class ItemThaumiumSword extends SwordItem implements IRepairable {
    public ItemThaumiumSword(Tier tier, Properties properties) {
        // Legacy sword4 + material2 = additive6; speed is a 1.21 combat adapter.
        super(tier, properties.rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes(tier, 4, -2.4F)));
    }
}
