package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.EntityTaintCreeper;

/**
 * Original TC4 {@code thaumcraft/client/renderers/entity/RenderTaintCreeper.java}.
 *
 * <p>The original rendered the vanilla creeper mesh with the TC4 taint creeper sheet
 * ({@code thaumcraft:textures/models/creeper.png}), the original swelling scale and the original white flash of
 * the priming creeper.</p>
 *
 * <p>Forced 1.21.1 change: 1.7.10 multiplied the model with an OpenGL call and returned an ARGB colour; 1.21.1
 * scales the pose stack and reports the white overlay, so the same numbers are used there.</p>
 */
public final class RenderTaintCreeper extends MobRenderer<EntityTaintCreeper, CreeperModel<EntityTaintCreeper>> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/creeper.png");

    public RenderTaintCreeper(EntityRendererProvider.Context context) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
    }

    @Override public ResourceLocation getTextureLocation(EntityTaintCreeper entity) { return SKIN; }

    /** Original {@code updateCreeperScale}: the swelling scale of the priming creeper. */
    @Override protected void scale(EntityTaintCreeper entity, PoseStack pose, float partialTicks) {
        float flash = entity.getCreeperFlashIntensity(partialTicks);
        float jitter = 1.0F + Mth.sin(flash * 100.0F) * flash * 0.01F;
        flash = Mth.clamp(flash, 0.0F, 1.0F);
        flash *= flash;
        flash *= flash;
        float wide = (1.0F + flash * 0.4F) * jitter;
        float tall = (1.0F + flash * 0.1F) / jitter;
        pose.scale(wide, tall, wide);
    }

    /** Original {@code updateCreeperColorMultiplier}: the white flash of the priming creeper. */
    @Override protected float getWhiteOverlayProgress(EntityTaintCreeper entity, float partialTicks) {
        float flash = entity.getCreeperFlashIntensity(partialTicks);
        if ((int) (flash * 10.0F) % 2 == 0) return 0.0F;
        return Mth.clamp(flash * 0.2F, 0.0F, 1.0F);
    }
}
