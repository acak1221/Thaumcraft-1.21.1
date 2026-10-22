package thaumcraft.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.tiles.TileCrucible;

/**
 * NeoForge half of the crucible fluid capability.
 *
 * <p>Oracle {@code thaumcraft/common/tiles/TileCrucible.java:61-71,373-410}: the crucible is a Forge
 * {@code IFluidHandler} with a tank of one thousand millibuckets that accepts water only and that any
 * other mod may read and fill. NeoForge owns that capability, the Fabric module publishes the very same
 * tank through the transfer API of Fabric.</p>
 *
 * <p>NeoForge counts a bucket as one thousand millibuckets, so the unit of the original and the unit of
 * the loader are the same here and no conversion happens. The rule itself lives in the shared tile.</p>
 */
final class NeoForgeCrucibleFluidHandler {
    private NeoForgeCrucibleFluidHandler() {}

    static void init(IEventBus bus) {
        bus.addListener(NeoForgeCrucibleFluidHandler::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ThaumcraftAlchemy.CRUCIBLE_BLOCK_ENTITY.get(),
                (tile, side) -> new CrucibleFluidHandler((TileCrucible) tile));
    }

    private static final class CrucibleFluidHandler implements IFluidHandler {
        private final TileCrucible crucible;

        CrucibleFluidHandler(TileCrucible crucible) {
            this.crucible = crucible;
        }

        @Override public int getTanks() { return 1; }

        @Override public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
            int amount = crucible.getWaterAmount();
            if (amount <= 0) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            return new net.neoforged.neoforge.fluids.FluidStack(
                    BuiltInRegistries.FLUID.wrapAsHolder(Fluids.WATER), amount);
        }

        @Override public int getTankCapacity(int tank) { return crucible.getWaterCapacity(); }

        @Override public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
            return stack != null && !stack.isEmpty() && crucible.canFillWater(stack.getFluid());
        }

        @Override public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return 0;
            return crucible.fillWater(resource.getFluid(), resource.getAmount(), action.execute());
        }

        @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource,
                                                                          FluidAction action) {
            if (resource == null || resource.isEmpty()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            int drained = crucible.drainWater(resource.getFluid(), resource.getAmount(), action.execute());
            if (drained <= 0) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            return new net.neoforged.neoforge.fluids.FluidStack(
                    BuiltInRegistries.FLUID.wrapAsHolder(Fluids.WATER), drained);
        }

        @Override public net.neoforged.neoforge.fluids.FluidStack drain(int tank, FluidAction action) {
            int drained = crucible.drainWater(Integer.MAX_VALUE, action.execute());
            if (drained <= 0) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            return new net.neoforged.neoforge.fluids.FluidStack(
                    BuiltInRegistries.FLUID.wrapAsHolder(Fluids.WATER), drained);
        }
    }
}
