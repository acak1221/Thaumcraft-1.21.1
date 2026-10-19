package thaumcraft.common.items.equipment;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import thaumcraft.api.IRepairable;

public class ItemThaumiumAxe extends AxeItem implements IRepairable {
    public ItemThaumiumAxe(Tier tier, Properties properties) {
        // Legacy axe3 + material2 = additive5, NOT modern vanilla axe damage.
        super(tier, properties.rarity(Rarity.UNCOMMON).attributes(DiggerItem.createAttributes(tier, 3, -3.1F)));
    }
}
