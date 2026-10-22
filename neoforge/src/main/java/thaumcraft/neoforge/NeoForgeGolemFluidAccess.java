package thaumcraft.neoforge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import thaumcraft.common.lib.fluid.GolemFluidAccess;
import thaumcraft.common.lib.fluid.GolemFluidBlocks;
import thaumcraft.common.lib.fluid.GolemFluidHandler;

/**
 * The NeoForge half of the loader independent fluid handler API.
 *
 * <p>Oracle {@code thaumcraft/common/entities/golems/GolemHelper.java:341-446}: the home of the golem
 * is any {@code IFluidHandler} and the source is any marked {@code IFluidHandler} of the wanted
 * fluid. The 1.21.1 common module cannot see the Forge capability, so the NeoForge module answers
 * with {@code Capabilities.FluidHandler.BLOCK} behind the port's own interface.</p>
 *
 * <p>NeoForge counts a bucket as one thousand millibuckets, so the unit of the original and the unit
 * of the loader are the same here and no conversion happens. The rule itself lives in the shared
 * code.</p>
 */
public final class NeoForgeGolemFluidAccess {
    private NeoForgeGolemFluidAccess() {}

    public static void init() {
        GolemFluidAccess.register(NeoForgeGolemFluidAccess::handler);
        GolemFluidAccess.registerContainerLookup(NeoForgeGolemFluidAccess::fluidOfContainer);
    }

    /** A block that offers the fluid capability on any side is a fluid handler. */
    private static GolemFluidHandler handler(Level level, BlockPos pos, BlockState state, BlockEntity tile) {
        if (level == null || pos == null || !level.isLoaded(pos)) return null;
        for (Direction face : Direction.values()) {
            if (level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face) != null) return new View(level, pos);
        }
        return null;
    }

    /** The fluid a filled container holds, through the registry of filled containers of NeoForge. */
    private static ResourceLocation fluidOfContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var contained = FluidUtil.getFluidContained(stack);
        if (contained.isEmpty()) return null;
        FluidStack fluid = contained.get();
        if (fluid == null || fluid.isEmpty()) return null;
        return BuiltInRegistries.FLUID.getKey(fluid.getFluid());
    }

    /** The view of one block; every side is resolved through the capability of NeoForge. */
    private static final class View implements GolemFluidHandler {
        private final Level level;
        private final BlockPos pos;

        View(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos.immutable();
        }

        private IFluidHandler handler(Direction face) {
            if (level == null || !level.isLoaded(pos)) return null;
            return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face);
        }

        @Override public ResourceLocation fluid(Direction face) {
            IFluidHandler handler = handler(face);
            if (handler == null) return null;
            FluidStack stack = handler.getFluidInTank(0);
            if (stack == null || stack.isEmpty()) return null;
            return BuiltInRegistries.FLUID.getKey(stack.getFluid());
        }

        @Override public int amount(Direction face) {
            IFluidHandler handler = handler(face);
            if (handler == null) return 0;
            FluidStack stack = handler.getFluidInTank(0);
            return stack == null ? 0 : stack.getAmount();
        }

        @Override public boolean canFill(Direction face, ResourceLocation fluid) {
            IFluidHandler handler = handler(face);
            if (handler == null || fluid == null) return false;
            Fluid variant = GolemFluidBlocks.fluid(fluid);
            if (variant == null) return false;
            return handler.isFluidValid(0, new FluidStack(variant, 1));
        }

        @Override public int drain(Direction face, ResourceLocation fluid, int amountMb, boolean commit) {
            IFluidHandler handler = handler(face);
            if (handler == null || fluid == null || amountMb <= 0) return 0;
            Fluid variant = GolemFluidBlocks.fluid(fluid);
            if (variant == null) return 0;
            FluidStack drained = handler.drain(new FluidStack(variant, amountMb),
                    commit ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
            return drained == null ? 0 : drained.getAmount();
        }

        @Override public int fill(Direction face, ResourceLocation fluid, int amountMb, boolean commit) {
            IFluidHandler handler = handler(face);
            if (handler == null || fluid == null || amountMb <= 0) return 0;
            Fluid variant = GolemFluidBlocks.fluid(fluid);
            if (variant == null) return 0;
            return handler.fill(new FluidStack(variant, amountMb),
                    commit ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
        }
    }
}
