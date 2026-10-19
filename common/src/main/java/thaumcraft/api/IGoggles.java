package thaumcraft.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Original in-world popup opt-in; wearing armor alone grants no popup ability. */
public interface IGoggles {
    boolean showIngamePopups(ItemStack stack, LivingEntity wearer);
}
