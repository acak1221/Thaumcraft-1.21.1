package thaumcraft.api.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** TC4's block stabiliser hook. The original spelling is part of the public API. */
public interface IInfusionStabiliser {
    boolean canStabaliseInfusion(Level level, BlockPos pos);
}
