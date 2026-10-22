package thaumcraft.fabric.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.tiles.TileCrucible;

/**
 * Fabric half of the crucible fluid capability.
 *
 * <p>Oracle {@code thaumcraft/common/tiles/TileCrucible.java:61-71,373-410}: the crucible is a Forge
 * {@code IFluidHandler} with a tank of one thousand millibuckets that accepts water only and that any
 * other mod may read and fill. Fabric exposes the very same tank through the transfer API of Fabric, so
 * the tank of the crucible is reachable for other mods on both loaders.</p>
 *
 * <p>Fabric counts a bucket as {@code 81000} droplets and the original tank holds one thousand
 * millibuckets, so one millibucket of the original is eighty one droplets of Fabric. The rule itself
 * lives in the shared tile, this adapter only converts the unit and never decides anything.</p>
 */
public final class FabricCrucibleStorage {
    /** Fabric counts a bucket as eighty one thousand droplets. */
    private static final long DROPLETS_PER_BUCKET = FluidConstants.BUCKET;
    /** The original tank of the crucible holds one thousand millibuckets. */
    private static final long MILLIBUCKETS_PER_BUCKET = 1000L;
    private static boolean initialized;
    // One snapshot participant per real tank, including lookups on different faces.
    // Weak values avoid retaining the tile through the weak map's value.
    private static final Map<TileCrucible, WeakReference<Tank>> TANKS = new WeakHashMap<>();
    private FabricCrucibleStorage() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        FluidStorage.SIDED.registerForBlockEntity((tile, side) -> {
            if (!(tile instanceof TileCrucible crucible)) return null;
            var reference = TANKS.get(crucible);
            Tank tank = reference == null ? null : reference.get();
            if (tank == null) { tank = new Tank(crucible); TANKS.put(crucible, new WeakReference<>(tank)); }
            return tank;
        }, ThaumcraftAlchemy.CRUCIBLE_BLOCK_ENTITY.get());
    }

    /** The droplets of the original millibuckets. */
    static long toDroplets(long millibuckets) {
        return millibuckets * DROPLETS_PER_BUCKET / MILLIBUCKETS_PER_BUCKET;
    }

    /** The original millibuckets of the droplets. */
    static long toMillibuckets(long droplets) {
        return droplets * MILLIBUCKETS_PER_BUCKET / DROPLETS_PER_BUCKET;
    }

    private static final class Tank extends SnapshotParticipant<Integer> implements Storage<FluidVariant>, StorageView<FluidVariant> {
        private final TileCrucible crucible;
        private Tank(TileCrucible crucible) { this.crucible = crucible; }

        private boolean current() {
            var level = crucible.getLevel();
            return level != null && !crucible.isRemoved() && level.hasChunkAt(crucible.getBlockPos())
                    && level.getBlockEntity(crucible.getBlockPos()) == crucible;
        }
        @Override protected Integer createSnapshot() { return crucible.getWaterAmount(); }
        @Override protected void readSnapshot(Integer amount) {
            // Retired capabilities must never resurrect stock into a replacement owner.
            if (!current()) return;
            int difference = amount - crucible.getWaterAmount();
            if (difference > 0) crucible.fillWater(Fluids.WATER, difference, true);
            else if (difference < 0) crucible.drainWater(-difference, true);
        }

        @Override public long insert(FluidVariant resource, long maximum, TransactionContext transaction) {
            if (resource == null || maximum <= 0 || !current()) return 0;
            Fluid fluid = resource.getFluid();
            if (fluid != Fluids.WATER) return 0;
            long millibuckets = Math.min(toMillibuckets(maximum), crucible.getWaterCapacity() - crucible.getWaterAmount());
            if (millibuckets <= 0) return 0;
            updateSnapshots(transaction);
            int accepted = crucible.fillWater(fluid, (int) millibuckets, true);
            return toDroplets(accepted);
        }

        @Override public long extract(FluidVariant resource, long maximum, TransactionContext transaction) {
            if (resource == null || maximum <= 0 || !current()) return 0;
            if (resource.getFluid() != Fluids.WATER) return 0;
            long millibuckets = Math.min(toMillibuckets(maximum), crucible.getWaterAmount());
            if (millibuckets <= 0) return 0;
            updateSnapshots(transaction);
            int drained = crucible.drainWater((int) millibuckets, true);
            return toDroplets(drained);
        }

        @Override public boolean supportsInsertion() { return current(); }
        @Override public boolean supportsExtraction() { return current(); }
        @Override public boolean isResourceBlank() { return getAmount() <= 0; }
        @Override public FluidVariant getResource() {
            return current() && crucible.getWaterAmount() > 0 ? FluidVariant.of(Fluids.WATER) : FluidVariant.blank();
        }
        @Override public long getAmount() { return current() ? toDroplets(crucible.getWaterAmount()) : 0; }
        @Override public long getCapacity() { return toDroplets(crucible.getWaterCapacity()); }
        @Override public long getVersion() { return crucible.getWaterAmount(); }
        @Override public Iterator<StorageView<FluidVariant>> iterator() { return List.<StorageView<FluidVariant>>of(this).iterator(); }
    }
}
