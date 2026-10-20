package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.entities.ModelEldritchCrab;
import thaumcraft.common.entities.monster.EntityEldritchCrab;

/**
 * Original TC4 RenderEldritchCrab: the crab skin plus a second translucent pass over the
 * craboverlay.png sheet. The original overlay pass used a fixed lightmap of 200 and
 * standard alpha blending; the native pass is full bright with the same blend mode.
 */
public final class RenderEldritchCrab extends MobRenderer<EntityEldritchCrab, ModelEldritchCrab> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/crab.png");
    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/craboverlay.png");

    public RenderEldritchCrab(EntityRendererProvider.Context context) {
        super(context, new ModelEldritchCrab(ModelEldritchCrab.createBodyLayer().bakeRoot()), 1.0F);
        addLayer(new OverlayLayer(this));
    }

    @Override public ResourceLocation getTextureLocation(EntityEldritchCrab crab) { return SKIN; }

    /** Original func_77032_a:48-66, the second render pass over the overlay sheet. */
    private static final class OverlayLayer extends RenderLayer<EntityEldritchCrab, ModelEldritchCrab> {
        private OverlayLayer(RenderEldritchCrab renderer) { super(renderer); }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityEldritchCrab crab,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            var model = getParentModel();
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(OVERLAY)),
                    LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(crab, 0), 0xffffffff);
        }
    }
}
