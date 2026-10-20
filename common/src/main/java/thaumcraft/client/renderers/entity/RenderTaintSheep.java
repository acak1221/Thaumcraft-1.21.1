package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.entities.ModelTaintSheep1;
import thaumcraft.client.renderers.models.entities.ModelTaintSheep2;
import thaumcraft.common.entities.monster.EntityTaintSheep;

/**
 * Original TC4 RenderTaintSheep: the TC4 sheep body mesh with the taint sheet plus the
 * wool overlay mesh on the sheep_fur.png sheet while the sheep is not sheared.
 */
public final class RenderTaintSheep extends MobRenderer<EntityTaintSheep, ModelTaintSheep2> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/sheep.png");
    private static final ResourceLocation FUR = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/sheep_fur.png");

    public RenderTaintSheep(EntityRendererProvider.Context context) {
        super(context, new ModelTaintSheep2(ModelTaintSheep2.createBodyLayer().bakeRoot()), .7F);
        addLayer(new WoolLayer(this));
    }

    @Override public ResourceLocation getTextureLocation(EntityTaintSheep sheep) { return SKIN; }

    /** Original setWoolColorAndRender:26-32, the second wool pass. */
    private static final class WoolLayer extends RenderLayer<EntityTaintSheep, ModelTaintSheep2> {
        private final ModelTaintSheep1 wool = new ModelTaintSheep1(ModelTaintSheep1.createBodyLayer().bakeRoot());

        private WoolLayer(RenderTaintSheep renderer) { super(renderer); }

        @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityTaintSheep sheep,
                                     float walkPhase, float walkSpeed, float partialTick, float age, float headYaw, float headPitch) {
            if (sheep.isSheared()) return;
            wool.prepareMobModel(sheep, walkPhase, walkSpeed, partialTick);
            wool.setupAnim(sheep, walkPhase, walkSpeed, age, headYaw, headPitch);
            wool.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(FUR)), light, OverlayTexture.NO_OVERLAY, 0xffffffff);
        }
    }
}
