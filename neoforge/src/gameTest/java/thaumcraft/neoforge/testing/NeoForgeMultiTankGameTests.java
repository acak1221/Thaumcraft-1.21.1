package thaumcraft.neoforge.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.common.lib.fluid.GolemFluidAccess;

/** Real NeoForge capability discovery over two independently typed native FluidTanks. */
@PrefixGameTestTemplate(false)
public final class NeoForgeMultiTankGameTests {
    private static Block probe;
    private static Binding binding;
    private record Binding(ServerLevel level, BlockPos pos, IFluidHandler handler) {}

    static void registerBlock(RegisterEvent event) {
        event.register(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "multi_tank_probe"),
                () -> probe = new Block(BlockBehaviour.Properties.of().strength(.1F)));
    }

    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, tile, side) -> {
            Binding current = binding;
            return current != null && current.level() == level && current.pos().equals(pos) && side == Direction.UP
                    ? current.handler() : null;
        }, probe);
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft", batch = "neoforge_fluid_capabilities")
    public void neoGolemAcceptsFluidInLaterTank(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(8, 100, 8));
        var before = level.getBlockState(pos);
        var handler = new DualTank();
        binding = new Binding(level, pos, handler);
        try {
            level.setBlock(pos, probe.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.invalidateCapabilities(pos);
            test.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP) == handler,
                    "native capability lookup exposes the actual two-tank fixture on its declared face");
            var view = GolemFluidAccess.of(level, pos);
            var water = ResourceLocation.withDefaultNamespace("water");
            var lava = ResourceLocation.withDefaultNamespace("lava");
            test.assertTrue(view != null && view.canFill(Direction.UP, water) && view.canFill(Direction.UP, lava),
                    "golem type discovery sees water in tank one as well as lava in tank zero");
            test.assertTrue(!view.canFill(Direction.NORTH, water)
                            && !view.canFill(Direction.UP, ResourceLocation.withDefaultNamespace("empty")),
                    "the bridge retains the native face and fluid validity restrictions");
            test.assertTrue(view.fill(Direction.UP, water, 1000, false) == 1000
                            && handler.tanks[0].isEmpty() && handler.tanks[1].isEmpty(),
                    "simulated insertion into the later tank preserves both stocks");
            test.assertTrue(view.fill(Direction.UP, water, 1000, true) == 1000
                            && handler.tanks[0].isEmpty() && handler.tanks[1].getFluidAmount() == 1000,
                    "actual bridge delivery credits exactly one bucket to the water tank only");
            test.assertTrue(view.canFill(Direction.UP, water) && view.fill(Direction.UP, water, 1, false) == 0,
                    "original canFill is type eligibility, independent of a full tank's current capacity");
            test.assertTrue(view.drain(Direction.UP, water, 1000, true) == 1000 && handler.tanks[1].isEmpty(),
                    "the same native adapter drains the later tank without losing fluid");
            level.removeBlock(pos, false);
            test.assertTrue(!view.canFill(Direction.UP, water), "a retained view never reaches a removed capability");
        } finally {
            binding = null;
            level.invalidateCapabilities(pos);
            level.setBlock(pos, before, Block.UPDATE_CLIENTS);
        }
        test.succeed();
    }

    private static final class DualTank implements IFluidHandler {
        private final FluidTank[] tanks = {
                new FluidTank(1000, fluid -> fluid.is(Fluids.LAVA)),
                new FluidTank(1000, fluid -> fluid.is(Fluids.WATER))
        };
        @Override public int getTanks() { return tanks.length; }
        @Override public FluidStack getFluidInTank(int tank) { return tanks[tank].getFluid(); }
        @Override public int getTankCapacity(int tank) { return tanks[tank].getCapacity(); }
        @Override public boolean isFluidValid(int tank, FluidStack fluid) { return tanks[tank].isFluidValid(0, fluid); }
        @Override public int fill(FluidStack fluid, FluidAction action) {
            for (var tank : tanks) if (tank.isFluidValid(0, fluid)) return tank.fill(fluid, action);
            return 0;
        }
        @Override public FluidStack drain(FluidStack fluid, FluidAction action) {
            for (var tank : tanks) {
                var drained = tank.drain(fluid, action);
                if (!drained.isEmpty()) return drained;
            }
            return FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            for (var tank : tanks) {
                var drained = tank.drain(amount, action);
                if (!drained.isEmpty()) return drained;
            }
            return FluidStack.EMPTY;
        }
    }
}
