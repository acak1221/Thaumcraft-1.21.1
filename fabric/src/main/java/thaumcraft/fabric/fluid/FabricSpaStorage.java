package thaumcraft.fabric.fluid;

import dev.architectury.fluid.FluidStack;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.Direction;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.tiles.TileSpa;

/** Native droplet transactions retain the full fluid component patch and the current Spa owner. */
public final class FabricSpaStorage {
    // Weak values avoid the value->tile reference retaining a weak key after native chunk retirement.
    private static final Map<TileSpa,WeakReference<Tank>> TANKS=new WeakHashMap<>();
    private static boolean initialized;
    private FabricSpaStorage() {}
    public static void init() {
        if(initialized)return;initialized=true;
        FluidStorage.SIDED.registerForBlockEntity((tile,side)->{
            if(side==Direction.UP||!tile.isLiveOwner())return null;
            var reference=TANKS.get(tile);Tank tank=reference==null?null:reference.get();
            if(tank==null){tank=new Tank(tile);TANKS.put(tile,new WeakReference<>(tank));}
            return tank;
        },ThaumcraftSpa.SPA_TILE.get());
    }

    private static final class Tank extends SnapshotParticipant<TileSpa.FluidSnapshot>
            implements Storage<FluidVariant>,StorageView<FluidVariant> {
        private final TileSpa tile;
        private TileSpa.FluidTransaction lease;
        private TransactionContext outer;
        private Tank(TileSpa tile){this.tile=tile;}
        private boolean enter(TransactionContext transaction) {
            java.util.Objects.requireNonNull(transaction);
            if(!tile.isLiveOwner())return false;
            var owner=transaction.getOpenTransaction(0);
            if(lease!=null)return outer==owner&&lease.live();
            var acquired=tile.beginFluidTransaction(owner);if(acquired==null)return false;
            lease=acquired;outer=owner;
            // All nested snapshots have closed before this callback. Release before dirty/menu callbacks can reenter.
            owner.addOuterCloseCallback(result->{
                var closing=lease;lease=null;outer=null;
                if(closing!=null)closing.finish(!result.wasAborted());
            });
            return true;
        }
        @Override protected TileSpa.FluidSnapshot createSnapshot(){return lease.snapshot();}
        @Override protected void readSnapshot(TileSpa.FluidSnapshot snapshot){lease.rollback(snapshot);}
        @Override public long insert(FluidVariant resource,long maximum,TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource,maximum);
            if(maximum==0||!enter(transaction))return 0;
            updateSnapshots(transaction);
            return lease.insert(FluidStack.create(resource.getFluid(),maximum,resource.getComponents()),maximum);
        }
        @Override public long extract(FluidVariant resource,long maximum,TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource,maximum);
            if(maximum==0||!enter(transaction))return 0;
            updateSnapshots(transaction);
            return lease.extract(FluidStack.create(resource.getFluid(),maximum,resource.getComponents()),maximum).getAmount();
        }
        @Override public boolean supportsInsertion(){return tile.isLiveOwner();}
        @Override public boolean supportsExtraction(){return tile.isLiveOwner();}
        @Override public boolean isResourceBlank(){return getResource().isBlank();}
        @Override public FluidVariant getResource(){
            var fluid=tile.isLiveOwner()?tile.fluid():FluidStack.empty();
            return fluid.isEmpty()?FluidVariant.blank():FluidVariant.of(fluid.getFluid(),fluid.getPatch());
        }
        @Override public long getAmount(){return tile.isLiveOwner()?tile.fluid().getAmount():0;}
        @Override public long getCapacity(){return tile.getCapacityNative();}
        @Override public long getVersion(){return tile.fluidRevision();}
        @Override public Iterator<StorageView<FluidVariant>> iterator(){return List.<StorageView<FluidVariant>>of(this).iterator();}
    }
}
