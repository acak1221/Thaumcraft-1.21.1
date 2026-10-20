package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityTaintSwarm;

/** The original renderer deliberately draws nothing: the actual independently ticking FXSwarm particles ARE this mob's body. */
public final class RenderTaintSwarm extends EntityRenderer<EntityTaintSwarm> {
    public RenderTaintSwarm(EntityRendererProvider.Context context) { super(context);shadowRadius=0; }
    @Override public void render(EntityTaintSwarm entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {}
    @Override public ResourceLocation getTextureLocation(EntityTaintSwarm entity) { return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png"); }
}
