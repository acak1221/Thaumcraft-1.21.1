package thaumcraft.compat.jade;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;

/**
 * Optional Jade integration: what a Thaumcraft block holds and does, and who a golem serves.
 *
 * <p>TC4 had no such tooltips; it showed a container's aspects only through the goggles of
 * revealing. The essentia tooltip keeps that rule by default (a Jade option drops it). Jade loads
 * this class itself: on Fabric through the {@code jade} entrypoint of fabric.mod.json, on NeoForge
 * through {@link WailaPlugin}. Nothing else references it, so the mod runs unchanged without Jade.
 * Like the EMI plugin, the source is compiled into each loader module, not into common.</p>
 */
@WailaPlugin
public final class ThaumcraftJadePlugin implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EssentiaProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(MachineProvider.INSTANCE, BlockEntity.class);
    }

    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(EssentiaProvider.GOGGLES, true);
        registration.registerBlockComponent(EssentiaProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(MachineProvider.INSTANCE, Block.class);
        registration.registerEntityComponent(GolemProvider.INSTANCE, thaumcraft.common.entities.golems.EntityGolemBase.class);
    }

    /** The essentia a store can hold, or 0 when it has no fixed capacity worth showing (a tube holds one). */
    static int capacity(BlockEntity entity) {
        return entity instanceof EssentiaStorageEndpoint storage && storage.getEssentiaCapacity() > 1 ? storage.getEssentiaCapacity() : 0;
    }
}
