package thaumcraft.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import thaumcraft.common.Thaumcraft;
import thaumcraft.client.ClientSetup;

@Mod(Thaumcraft.MODID)
public final class ThaumcraftNeoForge {
    public ThaumcraftNeoForge(IEventBus bus) {
        Thaumcraft.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(ClientSetup::init));
        }
    }
}
