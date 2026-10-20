package thaumcraft.fabric.client;

import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import thaumcraft.client.renderers.models.gear.HarnessArmorRenderer;
import thaumcraft.common.config.ThaumcraftTravelGear;

/** Actual native armor renderer bridge. Common code contains no Fabric classes. */
public final class FabricTravelGearClient {
    private static boolean initialized;
    private FabricTravelGearClient() {}
    public static void init() {if(initialized)return;initialized=true;ArmorRenderer.register(HarnessArmorRenderer::render,ThaumcraftTravelGear.HOVER_HARNESS.get());}
}
