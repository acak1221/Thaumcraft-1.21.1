package thaumcraft.neoforge;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.tiles.TileSpa;

/**
 * NeoForge half of the arcane spa fluid capability.
 *
 * <p>Fabric exposes the spa tank through the transfer API of Fabric, so other mods can fill and
 * drain it. NeoForge owns the {@link IFluidHandler} capability; this adapter publishes the very
 * same tank, with the same side rules (never from the top) and the same native transaction
 * boundary of the shared tile.</p>
 */
final class NeoForgeSpaStorage {
    private NeoForgeSpaStorage() {}

    static void init(IEventBus bus) {
        bus.addListener(NeoForgeSpaStorage::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ThaumcraftSpa.SPA_TILE.get(),
                (tile, side) -> side == null || side == Direction.UP ? null : new SpaFluidHandler(tile, side));
    }

    private static final class SpaFluidHandler implements IFluidHandler {
        private final TileSpa tile;
        private final Direction side;

        SpaFluidHandler(TileSpa tile, Direction side) {
            this.tile = tile;
            this.side = side;
        }

        @Override public int getTanks() { return 1; }

        @Override public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
            FluidStack stack = tile.fluid();
            if (stack.isEmpty()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            return new net.neoforged.neoforge.fluids.FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(stack.getFluid()),
                    (int) stack.getAmount(), stack.getComponents().asPatch());
        }

        @Override public int getTankCapacity(int tank) { return (int) tile.getCapacityNative(); }

        @Override public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
            return stack != null && !stack.isEmpty() && tile.canFill(side, stack.getFluid());
        }

        @Override public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return 0;
            FluidStack native4 = FluidStack.create(resource.getFluid(), resource.getAmount(),
                    resource.getComponentsPatch());
            long filled = tile.fill(side, native4, action.execute());
            return (int) filled;
        }

        @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource,
                                                                          FluidAction action) {
            if (resource == null || resource.isEmpty()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            FluidStack native4 = FluidStack.create(resource.getFluid(), resource.getAmount(),
                    resource.getComponentsPatch());
            FluidStack drained = tile.drain(side, native4, action.execute());
            if (drained.isEmpty()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            return new net.neoforged.neoforge.fluids.FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(drained.getFluid()),
                    (int) drained.getAmount(), drained.getComponents().asPatch());
        }

        @Override public net.neoforged.neoforge.fluids.FluidStack drain(int tank, FluidAction action) {
            FluidStack drained = tile.drain(side, Long.MAX_VALUE, action.execute());
            if (drained.isEmpty()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            return new net.neoforged.neoforge.fluids.FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(drained.getFluid()),
                    (int) drained.getAmount(), drained.getComponents().asPatch());
        }
    }
}
