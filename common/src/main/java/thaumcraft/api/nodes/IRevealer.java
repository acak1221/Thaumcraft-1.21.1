package thaumcraft.api.nodes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface IRevealer {
    boolean showNodes(ItemStack stack, LivingEntity wearer);
}
