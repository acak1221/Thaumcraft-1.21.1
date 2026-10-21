package thaumcraft.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import thaumcraft.common.Thaumcraft;
import thaumcraft.client.ClientSetup;

/**
 * NeoForge bootstrap of the shared TC4 4.2.3.5 port.
 *
 * <p>Every registration the Fabric bootstrap performs with a Fabric API is mirrored here with the
 * NeoForge equivalent, so the same registry content, the same spawn rules, the same protection
 * callbacks and the same loot rows exist on both loaders. The shared gameplay code stays in
 * {@code common}; this class only owns the loader wiring.</p>
 */
@Mod(Thaumcraft.MODID)
public final class ThaumcraftNeoForge {
    public ThaumcraftNeoForge(IEventBus bus) {
        // The native creative tab sink must be installed before the shared module
        // publishes its tab stacks.
        NeoForgeCreativeTabs.init(bus);
        // The shared module owns every deferred registry entry, menu, block entity,
        // creative tab and network packet; it must run before the loader wiring so the
        // platform bridges can already resolve the shared catalogues.
        Thaumcraft.init();
        NeoForgeEntityAttributes.register(bus);
        NeoForgeSpawnPlacements.register(bus);
        NeoForgeBiomeSpawns.register(bus);
        NeoForgeInteractions.register(bus);
        NeoForgeLootTables.register(bus);
        NeoForgeBorePlatform.init();
        NeoForgeGolemHarvestPlatform.init();
        NeoForgeGolemUsePlatform.init();
        NeoForgeSpaStorage.init(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                net.neoforged.bus.api.EventPriority.HIGHEST,
                net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.class,
                event -> {
                    var player = (net.minecraft.server.level.ServerPlayer) event.getEntity();
                    var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "research_settings");
                    thaumcraft.common.Thaumcraft.LOG.info("DIAG login player={} connId={} channelId={} setupPresent={} hasChannel={}",
                            player.getGameProfile().getName(),
                            System.identityHashCode(player.connection.getConnection()),
                            System.identityHashCode(player.connection.getConnection().channel()),
                            net.neoforged.neoforge.network.registration.ChannelAttributes.getPayloadSetup(player.connection.getConnection()) != null,
                            net.neoforged.neoforge.network.registration.NetworkRegistry.hasChannel(player.connection, id));
                });
        if (FMLEnvironment.dist == Dist.CLIENT) {
            bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(ClientSetup::init));
            thaumcraft.neoforge.client.ThaumcraftNeoForgeClient.register(bus);
        }
    }
}
