package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.models.ModelBanner;
import thaumcraft.common.tiles.TileBanner;

/**
 * Original TileBannerRenderer: the cultist banner.
 *
 * <p>The original draws the banner model of {@code ModelBanner}, the beam, the
 * two tabs and the waving cloth, tints the tabs with the dye colour and hangs
 * the aspect icon on the cloth when an aspect was applied to it.
 *
 * <p>The native tree already carries the converted model in {@link ModelBanner},
 * so the geometry is reused as it is.
 */
public final class TileBannerRenderer implements BlockEntityRenderer<TileBanner> {
    /** The original texture of a banner without an aspect. */
    public static final ResourceLocation BLANK = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/banner_blank.png");
    /** The original texture of the cultist banner. */
    public static final ResourceLocation CULTIST = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/banner_cultist.png");

    private final ModelBanner model = new ModelBanner();

    public TileBannerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileBanner tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Aspect aspect = tile.getAspect();
        byte color = tile.getColor();
        boolean plain = aspect == null && color == -1;
        var out = buffers.getBuffer(RenderType.entityCutoutNoCull(plain ? CULTIST : BLANK));
        pose.pushPose();
        try {
            // The original hangs the banner from the top face of the block.
            pose.translate(.5F, 1.0F, .5F);
            pose.mulPose(Axis.XP.rotationDegrees(180.0F));
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.mulPose(Axis.YP.rotationDegrees(tile.getFacing().get2DDataValue() * 90.0F));
            if (!tile.isOnWall()) {
                model.renderPole(pose, out, light, overlay);
            } else {
                pose.translate(0.0F, 0.0F, -.4125F);
            }
            model.renderBeam(pose, out, light, overlay);
            int tint = 0xffffffff;
            if (color != -1) {
                DyeColor dye = DyeColor.byId(color);
                int rgb = dye.getTextureDiffuseColor();
                tint = 0xff000000 | rgb;
            }
            // The original tints the two tabs with the dye colour.
            model.tabLeft().render(pose, out, light, overlay, tint);
            model.tabRight().render(pose, out, light, overlay, tint);
            // The original wave of the cloth.
            float time = tile.getBlockPos().getX() * 7F + tile.getBlockPos().getY() * 9F + tile.getBlockPos().getZ() * 13F
                    + (tile.getLevel() == null ? 0.0F : tile.getLevel().getGameTime()) + partialTick;
            float wave = (.005F + .005F * Mth.sin(time * (float) Math.PI * .02F)) * (float) Math.PI;
            model.banner().xRot = wave;
            model.renderBanner(pose, out, light, overlay);
            model.banner().xRot = 0.0F;
            if (aspect != null) {
                drawTag(pose, buffers, aspect, wave, light, overlay);
            }
        } finally {
            pose.popPose();
        }
    }

    /** The original aspect tag of the banner, drawn on the waving cloth. */
    private static void drawTag(PoseStack pose, MultiBufferSource buffers, Aspect aspect, float wave, int light, int overlay) {
        pose.pushPose();
        try {
            pose.translate(0.0F, 0.0F, .05001F);
            pose.scale(.0375F, .0375F, .0375F);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.mulPose(Axis.XP.rotationDegrees(-wave * 57.295776F * 2.0F));
            var out = buffers.getBuffer(RenderType.entityCutoutNoCull(aspect.getImage()));
            // The original draws the sixteen by sixteen icon of the aspect.
            float[][] corners = {{-.5F, .25F}, {.5F, .25F}, {.5F, -.75F}, {-.5F, -.75F}};
            float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
            for (int index = 0; index < 4; index++) {
                out.addVertex(pose.last(), corners[index][0], corners[index][1], 0.0F)
                        .setColor(255, 255, 255, 255)
                        .setUv(uv[index][0], uv[index][1]).setOverlay(overlay).setLight(light)
                        .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
            }
        } finally {
            pose.popPose();
        }
    }
}
