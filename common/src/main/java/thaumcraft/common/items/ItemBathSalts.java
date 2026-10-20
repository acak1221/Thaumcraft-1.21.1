package thaumcraft.common.items;

import net.minecraft.world.item.Item;
import thaumcraft.common.lib.SpaRules;

/** The item is used by natural dropped-item expiry and the actual Spa slot, not right-click. */
public final class ItemBathSalts extends Item {
    public ItemBathSalts(Properties properties) { super(properties); }
    public int originalEntityLifespan() { return SpaRules.BATH_SALT_LIFETIME; }
}
