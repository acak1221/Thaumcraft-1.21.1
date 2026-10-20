package thaumcraft.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import thaumcraft.client.fx.particles.PechCurseParticle;
import thaumcraft.client.gui.GuiPech;
import thaumcraft.client.renderers.entity.RenderPech;
import thaumcraft.client.renderers.entity.RenderPechBlast;
import thaumcraft.client.renderers.tile.TileManaPodRenderer;
import thaumcraft.common.config.ThaumcraftManaBeans;
import thaumcraft.common.config.ThaumcraftPech;
import thaumcraft.common.entities.projectile.EntityPechBlast;

public final class ThaumcraftPechClient {
    private static boolean initialized;
    private ThaumcraftPechClient(){}
    public static void init(){
        if(initialized)return;initialized=true;
        EntityRendererRegistry.register(ThaumcraftPech.PECH,RenderPech::new);EntityRendererRegistry.register(ThaumcraftPech.PECH_BLAST,RenderPechBlast::new);
        MenuRegistry.registerScreenFactory(ThaumcraftPech.PECH_MENU.get(),GuiPech::new);
        BlockEntityRendererRegistry.register(ThaumcraftManaBeans.MANA_POD_BLOCK_ENTITY.get(),TileManaPodRenderer::new);
        EntityPechBlast.setClientTrail(PechCurseParticle::trail);EntityPechBlast.setClientImpact(PechCurseParticle::impact);
    }
}
