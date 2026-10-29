/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): the Fabric glue of the Transvector Interface.
 * Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.fabric;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import thaumic.tinkerer.common.block.tile.transvector.TileTransvectorInterface;
import thaumic.tinkerer.common.content.TileBlocks;

/**
 * Forwards the fluid storage of the block a Transvector Interface is bound to (1.7.10: the interface was an
 * {@code IFluidHandler} that handed {@code fill}, {@code drain}, {@code canFill}, {@code canDrain} and {@code getTankInfo}
 * to the bound tile entity, side for side).
 *
 * <p>The Fabric Transfer API asks a block entity for its {@code Storage<FluidVariant>} through
 * {@link FluidStorage#SIDED}; the interface answers with the storage of the bound block for the same side, so a pipe or
 * tank next to the interface fills and drains the bound tank. Items already go through the interface's
 * {@code WorldlyContainer}, which the Transfer API wraps itself, and essentia through the Thaumcraft interfaces, so only
 * fluids need this class.</p>
 *
 * <p><b>Not here: energy.</b> The 1.7.10 interface also forwarded RF and EU. The Fabric energy API (Team Reborn Energy) is
 * not a dependency of this add-on, so power is not forwarded; see docs/deviations/TileBlocks.md, 2.5.</p>
 *
 * <p>{@code TileBlocks.ready()} calls {@link #register()} by name on Fabric (common code cannot link against this
 * module), so nothing has to be listed in {@code fabric.mod.json}. NeoForge has no glue yet.</p>
 */
public final class TileBlocksFabric {
    private static boolean registered;

    private TileBlocksFabric() {}

    /** Registers the fluid lookup of the Transvector Interface (once). */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        FluidStorage.SIDED.registerForBlockEntity(TileBlocksFabric::boundStorage, TileBlocks.interfaceType());
    }

    /**
     * The fluid storage of the bound block for {@code side}, or null while the interface is not bound, the bound block has
     * none, or it is another interface (which the Binder refuses to bind and which would send the lookup round in a circle).
     */
    @Nullable
    static Storage<FluidVariant> boundStorage(TileTransvectorInterface device, @Nullable Direction side) {
        Level level = device.getLevel();
        BlockEntity bound = device.getTile();
        if (level == null || bound == null || bound == device || bound instanceof TileTransvectorInterface) return null;
        return FluidStorage.SIDED.find(level, bound.getBlockPos(), bound.getBlockState(), bound, side);
    }
}
