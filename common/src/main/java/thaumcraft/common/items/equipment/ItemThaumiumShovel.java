package thaumcraft.common.items.equipment;

import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import thaumcraft.api.IRepairable;

public class ItemThaumiumShovel extends ShovelItem implements IRepairable {
    public ItemThaumiumShovel(Tier tier, Properties properties) {
        super(tier, properties.rarity(Rarity.UNCOMMON).attributes(DiggerItem.createAttributes(tier, 1, -3F)));
    }
}
