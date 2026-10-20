package thaumcraft.client.renderers.entity;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Legacy 1.12 RenderNoop: an entity renderer that deliberately draws nothing. The
 * original taint swarm mob is drawn entirely by its own particles, and this renderer
 * keeps that behaviour available for any entity whose body is its effect.
 */
public final class RenderNoop<T extends Entity> extends EntityRenderer<T> {
    public RenderNoop(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override public void render(T entity, float yaw, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
                                 net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
    }

    @Override public ResourceLocation getTextureLocation(T entity) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/potions.png");
    }
}
