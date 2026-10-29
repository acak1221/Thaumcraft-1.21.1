/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The inventory scattering that every {@code breakBlock} of the 1.7.10 machines repeated: each stack leaves the block
 * in lumps of 10 to 30 items from a random point inside the block, with the same random motion.
 */
public final class MachineDrops {
    private MachineDrops() {}

    /** Scatters every stack of {@code container} (the stacks are emptied). Server side only. */
    public static void scatter(Level level, BlockPos pos, Container container) {
        if (level.isClientSide) return;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            scatter(level, pos, stack);
            container.setItem(slot, ItemStack.EMPTY);
        }
    }

    /** Scatters one stack the way {@code breakBlock} did; the stack is consumed. */
    public static void scatter(Level level, BlockPos pos, ItemStack stack) {
        if (level.isClientSide || stack.isEmpty()) return;
        RandomSource random = level.random;
        float f = random.nextFloat() * 0.8F + 0.1F;
        float f1 = random.nextFloat() * 0.8F + 0.1F;
        float f2 = random.nextFloat() * 0.8F + 0.1F;
        while (!stack.isEmpty()) {
            int lump = random.nextInt(21) + 10;
            if (lump > stack.getCount()) lump = stack.getCount();
            ItemEntity entity = new ItemEntity(level, pos.getX() + f, pos.getY() + f1, pos.getZ() + f2, stack.split(lump));
            float f3 = 0.05F;
            entity.setDeltaMovement((float) random.nextGaussian() * f3, (float) random.nextGaussian() * f3 + 0.2F,
                    (float) random.nextGaussian() * f3);
            level.addFreshEntity(entity);
        }
    }
}
