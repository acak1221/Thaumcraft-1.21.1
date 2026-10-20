package thaumcraft.client.renderers.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * Legacy 1.12 RenderFallbackBiped: a biped mesh drawn with one fixed sheet. It is the
 * native stand in for a mob whose own sheet or model is not available.
 */
public final class RenderFallbackBiped<T extends Mob> extends LivingEntityRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;

    public RenderFallbackBiped(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), .5F);
        this.texture = texture;
    }

    @Override public ResourceLocation getTextureLocation(T entity) { return texture; }
}
