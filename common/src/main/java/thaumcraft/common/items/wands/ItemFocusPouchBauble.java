package thaumcraft.common.items.wands;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.api.baubles.IBauble;

/** The sole original registered pouch identity, wearable in the actual belt slot. */
public final class ItemFocusPouchBauble extends ItemFocusPouch implements IBauble {
    public ItemFocusPouchBauble(Properties properties) { super(properties); }
    @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.BELT; }
}
