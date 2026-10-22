package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityTaintCrawler;

/**
 * The tainted crawler, drawn with the model the 1.12.2 backport used for it.
 *
 * <p>TC4 4.2.3.5 has no tainted crawler; the entity comes from
 * {@code Thaumcraft-1.12.2-6.1.BETA26.jar}, whose client registers
 * {@code RenderingRegistry.registerEntityRenderingHandler(EntityTaintCrawler.class, new RenderTaintCrawler(...))}
 * ({@code thaumcraft/proxies/ProxyEntities.java:199}) and whose {@code RenderTaintCrawler}
 * is a {@code RenderLiving} over the vanilla {@code ModelSilverfish} with the shadow of
 * 0.2, the flipped turn of 180 degrees and the pre-render scale of 0.7.</p>
 *
 * <p>Declared native adaptation: the 1.12.2 original drew the silverfish mesh with the
 * backport sheet {@code textures/entity/crawler.png}. That sheet is not part of this port's
 * asset tree (the native tree carries the retained TC4 art only), so the native renderer
 * keeps the original model, the original shadow, the original flip and the original scale
 * and takes the vanilla silverfish skin for the missing sheet.</p>
 */
public final class RenderTaintCrawler extends MobRenderer<EntityTaintCrawler, SilverfishModel<EntityTaintCrawler>> {
    private static final ResourceLocation SKIN = ResourceLocation.withDefaultNamespace("textures/entity/silverfish.png");

    public RenderTaintCrawler(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel<>(context.bakeLayer(ModelLayers.SILVERFISH)), 0.2F);
    }

    /** Original RenderTaintCrawler.func_180584_a: the crawler is drawn upside down. */
    @Override protected float getFlipDegrees(EntityTaintCrawler entity) {
        return 180.0F;
    }

    @Override public ResourceLocation getTextureLocation(EntityTaintCrawler entity) {
        return SKIN;
    }

    /** Original RenderTaintCrawler.func_77041_b: the whole crawler is scaled to 0.7. */
    @Override protected void scale(EntityTaintCrawler entity, PoseStack pose, float amount) {
        pose.scale(0.7F, 0.7F, 0.7F);
    }
}
