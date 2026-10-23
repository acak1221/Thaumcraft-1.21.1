package thaumcraft.api;

import net.minecraft.world.item.ItemStack;

/** Item callback for contribution to runic shielding, measured in shield charge points rather than durability or vis. */
public interface IRunicArmor {
    int getRunicCharge(ItemStack stack);
}
