package thaumcraft.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The original TC4 runic armour item: a plain item that reports a fixed runic charge.
 *
 * <p>Forced 1.21.1 change: the item now needs an {@link Item.Properties} and the item identity of a stack
 * is the registry item, so the charge is answered for every stack of this item.</p>
 */
public class ItemRunic extends Item implements IRunicArmor {
    private final int charge;

    public ItemRunic(int charge) {
        super(new Item.Properties().stacksTo(1));
        this.charge = charge;
    }

    public ItemRunic(int charge, Item.Properties properties) {
        super(properties.stacksTo(1));
        this.charge = charge;
    }

    @Override public int getRunicCharge(ItemStack itemstack) { return this.charge; }
}
