package thaumcraft.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
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
 *
 * <p>Like the Fabric storage, the handler only moves water while its crucible is the live tile at its
 * position, and here also only on the server thread: NeoForge hands the capability out on the client
 * too, and a client side fill would change a tank the server never sees.</p>
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

        /** The crucible is still the tile at its position and the caller is on the server thread. */
        private boolean live() {
            var pos = crucible.getBlockPos();
            return crucible.getLevel() instanceof ServerLevel server && server.getServer().isSameThread()
                    && !crucible.isRemoved() && server.hasChunkAt(pos) && server.getBlockEntity(pos) == crucible;
        }

        @Override public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
            int amount = crucible.isRemoved() ? 0 : crucible.getWaterAmount();
            if (amount <= 0) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            return new net.neoforged.neoforge.fluids.FluidStack(
                    BuiltInRegistries.FLUID.wrapAsHolder(Fluids.WATER), amount);
        }

        @Override public int getTankCapacity(int tank) { return crucible.getWaterCapacity(); }

        @Override public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
            return stack != null && !stack.isEmpty() && crucible.canFillWater(stack.getFluid());
        }

        @Override public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty() || !live()) return 0;
            return crucible.fillWater(resource.getFluid(), resource.getAmount(), action.execute());
        }

        @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource,
                                                                          FluidAction action) {
            if (resource == null || resource.isEmpty() || !live()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            int drained = crucible.drainWater(resource.getFluid(), resource.getAmount(), action.execute());
            if (drained <= 0) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            return new net.neoforged.neoforge.fluids.FluidStack(
                    BuiltInRegistries.FLUID.wrapAsHolder(Fluids.WATER), drained);
        }

        /** Oracle TileCrucible:395-397 - at most the requested amount, as {@code FluidTank.drain}. */
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !live()) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            int drained = crucible.drainWater(maxDrain, action.execute());
            if (drained <= 0) return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            return new net.neoforged.neoforge.fluids.FluidStack(
                    BuiltInRegistries.FLUID.wrapAsHolder(Fluids.WATER), drained);
        }
    }
}
