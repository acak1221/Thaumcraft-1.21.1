package thaumcraft.api.aspects;

import net.minecraft.world.item.ItemStack;

/** Original TC4 item-container contract, with the native ItemStack type. */
public interface IEssentiaContainerItem {
    AspectList getAspects(ItemStack stack);
    void setAspects(ItemStack stack, AspectList aspects);
}
