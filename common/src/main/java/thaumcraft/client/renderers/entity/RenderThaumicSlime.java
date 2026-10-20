package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityThaumicSlime;

/**
 * Original TC4 RenderThaumicSlime: the vanilla slime mesh with the thaumic sheet, the
 * original squash scaling and the original inner body pass.
 */
public final class RenderThaumicSlime extends MobRenderer<EntityThaumicSlime, SlimeModel<EntityThaumicSlime>> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/tslime.png");

    public RenderThaumicSlime(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), .25F);
        addLayer(new InnerLayer(this));
    }

    @Override public ResourceLocation getTextureLocation(EntityThaumicSlime slime) { return SKIN; }

    /** Original scaleSlime:22-27. */
    @Override protected void scale(EntityThaumicSlime slime, PoseStack pose, float partial) {
        float size = (float) Math.sqrt(slime.getSlimeSize());
        float squash = (slime.oSquish + (slime.squish - slime.oSquish) * partial) / (size * .25F + 1.0F);
        float inverse = 1.0F / (squash + 1.0F);
        pose.scale(inverse * size + .1F, 1.0F / inverse * size + .1F, inverse * size + .1F);
    }

    /** Original shouldSlimeRenderPass:17-31, the inner body pass. */
    private static final class InnerLayer extends RenderLayer<EntityThaumicSlime, SlimeModel<EntityThaumicSlime>> {
        private final SlimeModel<EntityThaumicSlime> inner = new SlimeModel<>(SlimeModel.createInnerBodyLayer().bakeRoot());

        private InnerLayer(RenderThaumicSlime renderer) { super(renderer); }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityThaumicSlime slime,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            if (slime.isInvisible()) return;
            pose.pushPose();
            try {
                pose.scale(.7F, .7F, .7F);
                pose.translate(0.0D, -1.2D, 0.0D);
                inner.prepareMobModel(slime, walkPhase, walkSpeed, partialTick);
                inner.setupAnim(slime, walkPhase, walkSpeed, age, headYaw, headPitch);
                inner.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(SKIN)), light, OverlayTexture.NO_OVERLAY, 0xffffffff);
            } finally { pose.popPose(); }
        }
    }
}
