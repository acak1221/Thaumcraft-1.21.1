package thaumcraft.neoforge.testing;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.tiles.TileCrucible;
import thaumcraft.common.tiles.TileSpa;

/**
 * The fluid capabilities of the NeoForge module, through the real NeoForge lookups and
 * {@link FluidUtil}.
 *
 * <p>The Fabric suite checks the same tanks through the Fabric transfer API
 * ({@code SpaFluidStorageRuntimeChecks}, {@code PureBucketStorageRuntimeChecks},
 * {@code FluidConservationRuntimeChecks}), which NeoForge does not have, so the shared catalogue
 * skips those scenarios here. This class is NeoForge only and is not part of the generated
 * catalogue.</p>
 */
@PrefixGameTestTemplate(false)
public final class NeoForgeFluidCapabilityGameTests {
    private static final int BUCKET = FluidType.BUCKET_VOLUME;

    @GameTest(template = "empty", templateNamespace = "thaumcraft", batch = "neoforge_fluid_capabilities")
    public void neoSpaCapabilityHonoursSidesAndDrainLimit(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(8, 100, 8));
        var other = pos.east(2);
        var before = level.getBlockState(pos);
        var otherBefore = level.getBlockState(other);
        var above = level.getBlockState(pos.above());
        try {
            level.setBlock(pos, ThaumcraftSpa.ARCANE_SPA.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(other, ThaumcraftSpa.ARCANE_SPA.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            var tile = (TileSpa) level.getBlockEntity(pos);
            var destination = (TileSpa) level.getBlockEntity(other);
            IFluidHandler down = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.DOWN);
            IFluidHandler sideless = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, other, Direction.WEST);
            test.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP) == null,
                    "the spa offers no fluid handler from the top");
            test.assertTrue(down != null && sideless != null && target != null,
                    "the spa offers its tank from the sides, the bottom and without a side, as on Fabric");
            test.assertTrue(down.fill(water(4 * BUCKET), FluidAction.EXECUTE) == 4 * BUCKET
                            && sideless.fill(water(2 * BUCKET), FluidAction.SIMULATE) == BUCKET,
                    "the tank takes four buckets and offers the fifth of a two-bucket fill");
            var simulated = down.drain(BUCKET, FluidAction.SIMULATE);
            test.assertTrue(simulated.getAmount() == BUCKET && tile.getFluidAmountNative() == 4 * BUCKET,
                    "a simulated drain of one bucket answers one bucket, not the whole tank");
            var moved = FluidUtil.tryFluidTransfer(target, down, BUCKET, true);
            test.assertTrue(moved.getAmount() == BUCKET && tile.getFluidAmountNative() == 3 * BUCKET
                            && destination.getFluidAmountNative() == BUCKET,
                    "a one-bucket transfer between two spas moves exactly one bucket");
            test.assertTrue(sideless.drain(BUCKET / 2, FluidAction.EXECUTE).getAmount() == BUCKET / 2
                            && tile.getFluidAmountNative() == 5 * BUCKET / 2,
                    "an executed drain without a side takes exactly the requested half bucket");
            test.assertTrue(down.drain(10 * BUCKET, FluidAction.EXECUTE).getAmount() == 5 * BUCKET / 2
                            && target.drain(10 * BUCKET, FluidAction.EXECUTE).getAmount() == BUCKET
                            && tile.fluid().isEmpty() && destination.fluid().isEmpty(),
                    "a drain larger than the tank gives exactly what the tank holds");
            test.succeed();
        } finally {
            level.setBlock(pos, before, Block.UPDATE_CLIENTS);
            level.setBlock(pos.above(), above, Block.UPDATE_CLIENTS);
            level.setBlock(other, otherBefore, Block.UPDATE_CLIENTS);
        }
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft", batch = "neoforge_fluid_capabilities")
    public void neoPureBucketIsAFluidContainer(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(8, 100, 8));
        var before = level.getBlockState(pos);
        var above = level.getBlockState(pos.above());
        try {
            level.setBlock(pos, ThaumcraftSpa.ARCANE_SPA.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            var tile = (TileSpa) level.getBlockEntity(pos);
            IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.NORTH);
            Fluid pure = ThaumcraftSpa.PURE_FLUID_SOURCE.get();
            var full = new ItemStack(ThaumcraftSpa.BUCKET_PURE.get());
            var item = full.getCapability(Capabilities.FluidHandler.ITEM);
            test.assertTrue(tank != null && item != null,
                    "the bucket of purifying fluid offers a NeoForge item fluid handler");
            var content = item.getFluidInTank(0);
            test.assertTrue(content.is(pure) && content.getAmount() == BUCKET,
                    "the bucket holds one thousand millibuckets of the purifying source, as the original pairing");
            test.assertTrue(!FluidUtil.tryEmptyContainer(full, tank, BUCKET - 1, null, true).isSuccess()
                            && tile.fluid().isEmpty(),
                    "a whole-bucket container refuses a partial empty and the tank stays empty");
            var emptied = FluidUtil.tryEmptyContainer(full, tank, BUCKET, null, true);
            test.assertTrue(emptied.isSuccess() && emptied.getResult().is(Items.BUCKET)
                            && tile.fluid().getFluid() == pure && tile.getFluidAmountNative() == BUCKET,
                    "emptying the bucket into the spa leaves an empty bucket and one bucket of purifying fluid");
            var filled = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), tank, BUCKET, null, true);
            test.assertTrue(filled.isSuccess() && filled.getResult().is(ThaumcraftSpa.BUCKET_PURE.get())
                            && tile.fluid().isEmpty(),
                    "an empty bucket filled from the spa becomes the bucket of purifying fluid, not a water bucket");
            test.assertTrue(tank.fill(water(BUCKET), FluidAction.EXECUTE) == BUCKET, "the spa takes one bucket of water");
            var water = FluidUtil.tryFillContainer(new ItemStack(Items.BUCKET), tank, BUCKET, null, true);
            test.assertTrue(water.isSuccess() && water.getResult().is(Items.WATER_BUCKET) && tile.fluid().isEmpty(),
                    "water still fills a plain water bucket");
            test.succeed();
        } finally {
            level.setBlock(pos, before, Block.UPDATE_CLIENTS);
            level.setBlock(pos.above(), above, Block.UPDATE_CLIENTS);
        }
    }

    @GameTest(template = "empty", templateNamespace = "thaumcraft", batch = "neoforge_fluid_capabilities")
    public void neoCrucibleCapabilityHonoursDrainLimitAndOwner(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(new BlockPos(8, 100, 8));
        var before = level.getBlockState(pos);
        try {
            level.setBlock(pos, ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            var crucible = (TileCrucible) level.getBlockEntity(pos);
            IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
            test.assertTrue(tank != null, "the crucible offers its water tank");
            test.assertTrue(tank.fill(new FluidStack(Fluids.LAVA, BUCKET), FluidAction.EXECUTE) == 0
                            && tank.fill(water(2 * BUCKET), FluidAction.EXECUTE) == BUCKET
                            && crucible.getWaterAmount() == BUCKET,
                    "the tank takes water only, one thousand millibuckets at most");
            test.assertTrue(tank.drain(250, FluidAction.SIMULATE).getAmount() == 250 && crucible.getWaterAmount() == BUCKET,
                    "a simulated drain answers the requested amount and changes nothing");
            test.assertTrue(tank.drain(250, FluidAction.EXECUTE).getAmount() == 250 && crucible.getWaterAmount() == 750,
                    "an executed drain takes exactly the requested amount, as the original FluidTank.drain");
            int offThread = CompletableFuture.supplyAsync(() -> tank.fill(water(100), FluidAction.EXECUTE)).join();
            var drainedOffThread = CompletableFuture.supplyAsync(() -> tank.drain(100, FluidAction.EXECUTE)).join();
            test.assertTrue(offThread == 0 && drainedOffThread.isEmpty() && crucible.getWaterAmount() == 750,
                    "a call from another thread moves no water");
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, ThaumcraftAlchemy.CRUCIBLE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            var replacement = (TileCrucible) level.getBlockEntity(pos);
            test.assertTrue(replacement != crucible && tank.fill(water(BUCKET), FluidAction.EXECUTE) == 0
                            && tank.drain(BUCKET, FluidAction.EXECUTE).isEmpty() && tank.getFluidInTank(0).isEmpty()
                            && replacement.getWaterAmount() == 0,
                    "the handler of a removed crucible neither fills nor drains, nor reaches its replacement");
            IFluidHandler fresh = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.NORTH);
            test.assertTrue(fresh != null && fresh.fill(water(BUCKET), FluidAction.EXECUTE) == BUCKET
                            && replacement.getWaterAmount() == BUCKET,
                    "a new lookup reaches the replacement crucible");
            test.succeed();
        } finally {
            level.setBlock(pos, before, Block.UPDATE_CLIENTS);
        }
    }

    private static FluidStack water(int amount) {
        return new FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(Fluids.WATER), amount);
    }
}
