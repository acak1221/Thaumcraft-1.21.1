package thaumcraft.common.lib.fluid;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import thaumcraft.common.tiles.TileCrucible;

/**
 * The registry of the loader independent fluid handlers.
 *
 * <p>The 1.21.1 common module cannot see the Forge {@code IFluidHandler} capability of NeoForge nor
 * the {@code FluidStorage.SIDED} lookup of Fabric. Each loader registers its own provider here and
 * the goals ask this registry, exactly as the original asked for an {@code IFluidHandler}
 * (oracle {@code thaumcraft/common/entities/golems/GolemHelper.java:341-446}).</p>
 *
 * <p>The port's own containers are answered by the common fallback, so the common module never
 * depends on a loader class.</p>
 */
public final class GolemFluidAccess {
    /** The loader half: a view of the fluid handler of one block. */
    public interface Provider {
        /** The handler of the block, or {@code null} when the block holds no fluid. */
        GolemFluidHandler handler(Level level, BlockPos pos, BlockState state, BlockEntity tile);
    }

    /** The loader half: the fluid a filled container holds. */
    public interface ContainerLookup {
        /** The fluid of the container, or {@code null} when it is not a filled container. */
        net.minecraft.resources.ResourceLocation fluidOf(ItemStack stack);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<ContainerLookup> CONTAINER_LOOKUPS = new CopyOnWriteArrayList<>();

    private GolemFluidAccess() {}

    /** Registers the loader half of the API. */
    public static void register(Provider provider) {
        if (provider != null) PROVIDERS.add(provider);
    }

    /** Registers the loader half of the filled container query. */
    public static void registerContainerLookup(ContainerLookup lookup) {
        if (lookup != null) CONTAINER_LOOKUPS.add(lookup);
    }

    /** The handler of the block at the position, or {@code null}. */
    public static GolemFluidHandler of(Level level, BlockPos pos) {
        if (level == null || pos == null || !level.isLoaded(pos)) return null;
        return of(level, pos, level.getBlockState(pos), level.getBlockEntity(pos));
    }

    /** The handler of the block, or {@code null}. */
    public static GolemFluidHandler of(Level level, BlockPos pos, BlockState state, BlockEntity tile) {
        if (level == null || pos == null) return null;
        // The port's own containers keep the exact rules of the original, so they answer
        // before the loader half does: the Forge capability of the crucible refuses
        // every fluid but the water, whatever the room of the tank is.
        GolemFluidHandler own = commonHandler(level, pos, state, tile);
        if (own != null) return own;
        for (Provider provider : PROVIDERS) {
            GolemFluidHandler handler = provider.handler(level, pos, state, tile);
            if (handler != null) return handler;
        }
        return null;
    }

    /**
     * The fluid a filled container holds.
     *
     * <p>Oracle {@code GolemHelper.getMissingLiquids}:354-363 asks the registry of the original for
     * the fluid of every filled container of the inventory of the golem.</p>
     */
    public static net.minecraft.resources.ResourceLocation fluidOfFilledContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (ContainerLookup lookup : CONTAINER_LOOKUPS) {
            net.minecraft.resources.ResourceLocation fluid = lookup.fluidOf(stack);
            if (fluid != null) return fluid;
        }
        return bucketFluid(stack);
    }

    /**
     * The fluid of a filled bucket.
     *
     * <p>The bucket of a fluid names the fluid it holds, so the filled buckets of the vanilla game
     * answer without any loader. The empty fluid and the flowing variants share the empty bucket and
     * are therefore never a filled container.</p>
     */
    public static net.minecraft.resources.ResourceLocation bucketFluid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            Item bucket = fluid.getBucket();
            if (bucket == null || bucket == Items.BUCKET) continue; // The empty bucket is no filled container.
            if (bucket == stack.getItem()) return BuiltInRegistries.FLUID.getKey(fluid);
        }
        return null;
    }

    /** The port's own containers, without any loader. */
    private static GolemFluidHandler commonHandler(Level level, BlockPos pos, BlockState state, BlockEntity tile) {
        if (tile instanceof TileCrucible crucible) return new CrucibleView(crucible);
        return null;
    }

    /**
     * The crucible of the original as a fluid handler.
     *
     * <p>Oracle {@code thaumcraft/common/tiles/TileCrucible.java:373-410}: the crucible is a Forge
     * {@code IFluidHandler} with a tank of one thousand millibuckets that accepts water only.</p>
     */
    private static final class CrucibleView implements GolemFluidHandler {
        private final TileCrucible crucible;

        CrucibleView(TileCrucible crucible) { this.crucible = crucible; }

        @Override public net.minecraft.resources.ResourceLocation fluid(net.minecraft.core.Direction face) {
            return crucible.getWaterAmount() > 0
                    ? BuiltInRegistries.FLUID.getKey(net.minecraft.world.level.material.Fluids.WATER) : null;
        }

        @Override public int amount(net.minecraft.core.Direction face) { return crucible.getWaterAmount(); }

        @Override public boolean canFill(net.minecraft.core.Direction face, net.minecraft.resources.ResourceLocation fluid) {
            return TileCrucible.canFillWater(GolemFluidBlocks.fluid(fluid));
        }

        @Override public int drain(net.minecraft.core.Direction face, net.minecraft.resources.ResourceLocation fluid, int amountMb, boolean commit) {
            return crucible.drainWater(GolemFluidBlocks.fluid(fluid), amountMb, commit);
        }

        @Override public int fill(net.minecraft.core.Direction face, net.minecraft.resources.ResourceLocation fluid, int amountMb, boolean commit) {
            return crucible.fillWater(GolemFluidBlocks.fluid(fluid), amountMb, commit);
        }
    }
}
