package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityThaumicSlime;

/**
 * Original TC4 RenderThaumicSlime: the vanilla slime mesh with the thaumic sheet, the
 * original squash scaling and the original translucent outer shell.
 *
 * <p>Oracle {@code ClientProxy} registers {@code new RenderThaumicSlime(new ModelSlime(16),
 * new ModelSlime(0), 0.25F)}: the main model is the inner body with the face, and the render pass
 * model is the eight-cube outer shell, drawn by {@code shouldSlimeRenderPass} (pass 0) with the
 * blend {@code (770, 771)} in the same frame as the body. 1.21.1 splits the same two meshes into
 * {@code SLIME} and {@code SLIME_OUTER}.</p>
 */
public final class RenderThaumicSlime extends MobRenderer<EntityThaumicSlime, SlimeModel<EntityThaumicSlime>> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/tslime.png");

    public RenderThaumicSlime(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), .25F);
        addLayer(new OuterLayer(this, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME_OUTER))));
    }

    @Override public ResourceLocation getTextureLocation(EntityThaumicSlime slime) { return SKIN; }

    /** Original scaleSlime:22-27. */
    @Override protected void scale(EntityThaumicSlime slime, PoseStack pose, float partial) {
        float size = (float) Math.sqrt(slime.getSlimeSize());
        float squash = (slime.oSquish + (slime.squish - slime.oSquish) * partial) / (size * .25F + 1.0F);
        float inverse = 1.0F / (squash + 1.0F);
        pose.scale(inverse * size + .1F, 1.0F / inverse * size + .1F, inverse * size + .1F);
    }

    /** Original shouldSlimeRenderPass:9-43: the outer shell, alpha blended, nothing while invisible. */
    private static final class OuterLayer extends RenderLayer<EntityThaumicSlime, SlimeModel<EntityThaumicSlime>> {
        private final SlimeModel<EntityThaumicSlime> outer;

        private OuterLayer(RenderThaumicSlime renderer, SlimeModel<EntityThaumicSlime> outer) {
            super(renderer);
            this.outer = outer;
        }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityThaumicSlime slime,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            if (slime.isInvisible()) return;
            getParentModel().copyPropertiesTo(outer);
            outer.prepareMobModel(slime, walkPhase, walkSpeed, partialTick);
            outer.setupAnim(slime, walkPhase, walkSpeed, age, headYaw, headPitch);
            outer.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(SKIN)), light,
                    LivingEntityRenderer.getOverlayCoords(slime, 0.0F));
        }
    }
}
