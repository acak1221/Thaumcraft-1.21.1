package thaumcraft.client.renderers.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Legacy 1.12 RenderFallbackLiving: a living entity drawn with one fixed sheet. It is
 * the native stand in for a living entity whose own sheet or model is not available.
 */
public final class RenderFallbackLiving<T extends LivingEntity> extends LivingEntityRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;

    public RenderFallbackLiving(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), .5F);
        this.texture = texture;
    }

    @Override public ResourceLocation getTextureLocation(T entity) { return texture; }
}
