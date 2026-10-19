package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.fx.particles.ResourceParticle;
import thaumcraft.common.entities.projectile.EntityAlumentum;

/** TC4 RenderAlumentum:18-33 has no item mesh: its visible body is its particle trail. */
public final class RenderAlumentum extends EntityRenderer<EntityAlumentum> {
    public RenderAlumentum(EntityRendererProvider.Context context) { super(context); shadowRadius = .1F; }
    @Override public void render(EntityAlumentum entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {}
    @Override public ResourceLocation getTextureLocation(EntityAlumentum entity) { return ResourceParticle.TEXTURE; }
}
