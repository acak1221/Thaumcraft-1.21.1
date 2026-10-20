package thaumcraft.client;

import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.minecraft.client.renderer.RenderType;
import thaumcraft.client.fx.particles.ResourceParticle;
import thaumcraft.common.blocks.BlockVishroom;
import thaumcraft.common.config.ThaumcraftBiomes;

public final class ThaumcraftBiomeClient {
    private ThaumcraftBiomeClient() {}
    public static void init() {
        RenderTypeRegistry.register(RenderType.cutout(),ThaumcraftBiomes.VISHROOM.get());
        BlockVishroom.setClientEffects(ResourceParticle::vishroom);
    }
}
