package thaumcraft.api.nodes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Item callback deciding whether the wearer should see aura-node overlays. */
public interface IRevealer {
    boolean showNodes(ItemStack stack, LivingEntity wearer);
}
