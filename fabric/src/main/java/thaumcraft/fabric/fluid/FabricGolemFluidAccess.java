package thaumcraft.fabric.fluid;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import thaumcraft.common.lib.fluid.GolemFluidAccess;
import thaumcraft.common.lib.fluid.GolemFluidBlocks;
import thaumcraft.common.lib.fluid.GolemFluidHandler;

/**
 * The Fabric half of the loader independent fluid handler API.
 *
 * <p>Oracle {@code thaumcraft/common/entities/golems/GolemHelper.java:341-446}: the home of the golem
 * is any {@code IFluidHandler} and the source is any marked {@code IFluidHandler} of the wanted
 * fluid. The 1.21.1 common module cannot see the Forge capability, so the Fabric module answers
 * with {@code FluidStorage.SIDED} behind the port's own interface.</p>
 *
 * <p>Fabric counts a bucket as eighty one thousand droplets and the original counts it as one
 * thousand millibuckets, so one millibucket of the original is eighty one droplets of Fabric. The
 * conversion happens here, at the border of the loader; the rule itself lives in the shared code
 * and keeps the unit of the original.</p>
 */
public final class FabricGolemFluidAccess {
    private static boolean initialized;

    private FabricGolemFluidAccess() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        GolemFluidAccess.register(FabricGolemFluidAccess::handler);
        GolemFluidAccess.registerContainerLookup(FabricGolemFluidAccess::fluidOfContainer);
    }

    /** A block that offers a fluid storage on any side is a fluid handler. */
    private static GolemFluidHandler handler(Level level, BlockPos pos, BlockState state, BlockEntity tile) {
        if (level == null || pos == null || !level.isLoaded(pos)) return null;
        for (Direction face : Direction.values()) {
            if (FluidStorage.SIDED.find(level, pos, face) != null) return new View(level, pos);
        }
        return null;
    }

    /** The fluid a filled container holds, through the transfer API of Fabric. */
    private static ResourceLocation fluidOfContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Storage<FluidVariant> storage;
        try {
            storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
        } catch (RuntimeException failure) {
            return null;
        }
        if (storage == null) return null;
        for (StorageView<FluidVariant> view : storage) {
            if (view.isResourceBlank()) continue;
            return BuiltInRegistries.FLUID.getKey(view.getResource().getFluid());
        }
        return null;
    }

    /** The view of one block; every side is resolved through the lookup of Fabric. */
    private static final class View implements GolemFluidHandler {
        private final Level level;
        private final BlockPos pos;

        View(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos.immutable();
        }

        private Storage<FluidVariant> storage(Direction face) {
            if (level == null || !level.isLoaded(pos)) return null;
            return FluidStorage.SIDED.find(level, pos, face);
        }

        @Override public ResourceLocation fluid(Direction face) {
            Storage<FluidVariant> storage = storage(face);
            if (storage == null) return null;
            for (StorageView<FluidVariant> view : storage) {
                if (view.isResourceBlank()) continue;
                return BuiltInRegistries.FLUID.getKey(view.getResource().getFluid());
            }
            return null;
        }

        @Override public int amount(Direction face) {
            Storage<FluidVariant> storage = storage(face);
            if (storage == null) return 0;
            long total = 0;
            for (StorageView<FluidVariant> view : storage) {
                if (view.isResourceBlank()) continue;
                total += view.getAmount();
            }
            return (int) FabricCrucibleStorage.toMillibuckets(total);
        }

        @Override public boolean canFill(Direction face, ResourceLocation fluid) {
            Storage<FluidVariant> storage = storage(face);
            if (storage == null || fluid == null || !storage.supportsInsertion()) return false;
            Fluid variant = GolemFluidBlocks.fluid(fluid);
            if (variant == null) return false;
            try (Transaction outer = Transaction.openOuter()) {
                try (Transaction nested = outer.openNested()) {
                    long inserted = storage.insert(FluidVariant.of(variant), FabricCrucibleStorage.toDroplets(1), nested);
                    return FabricCrucibleStorage.toMillibuckets(inserted) > 0;
                }
            } catch (RuntimeException failure) {
                // Another transfer owns the thread: the original asks a question, it never forces one.
                return false;
            }
        }

        @Override public int drain(Direction face, ResourceLocation fluid, int amountMb, boolean commit) {
            Storage<FluidVariant> storage = storage(face);
            if (storage == null || fluid == null || amountMb <= 0) return 0;
            Fluid variant = GolemFluidBlocks.fluid(fluid);
            if (variant == null) return 0;
            long maximum = FabricCrucibleStorage.toDroplets(amountMb);
            try {
                if (commit) {
                    try (Transaction outer = Transaction.openOuter()) {
                        long extracted = storage.extract(FluidVariant.of(variant), maximum, outer);
                        outer.commit();
                        return (int) FabricCrucibleStorage.toMillibuckets(extracted);
                    }
                }
                try (Transaction outer = Transaction.openOuter()) {
                    try (Transaction nested = outer.openNested()) {
                        long extracted = storage.extract(FluidVariant.of(variant), maximum, nested);
                        return (int) FabricCrucibleStorage.toMillibuckets(extracted);
                    }
                }
            } catch (RuntimeException failure) {
                return 0;
            }
        }

        @Override public int fill(Direction face, ResourceLocation fluid, int amountMb, boolean commit) {
            Storage<FluidVariant> storage = storage(face);
            if (storage == null || fluid == null || amountMb <= 0) return 0;
            Fluid variant = GolemFluidBlocks.fluid(fluid);
            if (variant == null) return 0;
            long maximum = FabricCrucibleStorage.toDroplets(amountMb);
            try {
                if (commit) {
                    try (Transaction outer = Transaction.openOuter()) {
                        long inserted = storage.insert(FluidVariant.of(variant), maximum, outer);
                        outer.commit();
                        return (int) FabricCrucibleStorage.toMillibuckets(inserted);
                    }
                }
                try (Transaction outer = Transaction.openOuter()) {
                    try (Transaction nested = outer.openNested()) {
                        long inserted = storage.insert(FluidVariant.of(variant), maximum, nested);
                        return (int) FabricCrucibleStorage.toMillibuckets(inserted);
                    }
                }
            } catch (RuntimeException failure) {
                return 0;
            }
        }
    }
}
