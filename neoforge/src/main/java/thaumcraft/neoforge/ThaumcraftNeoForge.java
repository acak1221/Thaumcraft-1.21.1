package thaumcraft.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
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
    public ThaumcraftNeoForge(IEventBus bus, ModContainer container) {
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
        // Oracle EventHandlerWorld:225-245 - the original cancelled a block placement next to an
        // active eldritch boss of the Outer Lands.
        NeoForgeBossPlacement.register(bus);
        NeoForgeLootTables.register(bus);
        NeoForgeBorePlatform.init();
        NeoForgeGolemHarvestPlatform.init();
        NeoForgeGolemUsePlatform.init();
        NeoForgeWandUseCapture.init();
        // The shared Architectury break event must reach the shared protection for
        // every break attempt, also for the three refusals NeoForge posts its
        // native break event already cancelled for.
        NeoForgeSharedBreakEvent.init();
        NeoForgeSpaStorage.init(bus);
        // The loader independent fluid handler API of the golem (oracle GolemHelper:341-446).
        NeoForgeGolemFluidAccess.init();
        // Original TileCrucible:373-410 - the water tank of the crucible that other mods may read and fill.
        NeoForgeCrucibleFluidHandler.init(bus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(ClientSetup::init));
            thaumcraft.neoforge.client.ThaumcraftNeoForgeClient.register(bus);
            thaumcraft.neoforge.client.NeoForgeConfigScreen.register(container);
        }
    }
}
