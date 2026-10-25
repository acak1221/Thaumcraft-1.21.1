package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.BannerPose;
import thaumcraft.client.renderers.models.ModelBanner;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileBanner;

/**
 * Original TileBannerRenderer:31-74: the cultist banner.
 *
 * <p>The original draws the banner model of {@code ModelBanner}, the beam, the
 * two tabs and the waving cloth, tints the tabs and the cloth with the dye
 * colour of the original {@code Utils.colors} table and hangs the aspect icon
 * on the cloth when an aspect was applied to it.
 *
 * <p>The native tree already carries the converted model in {@link ModelBanner},
 * so the geometry is reused as it is. The original frame is kept verbatim:
 * the banner hangs from the top face of the block, that is one and a half
 * blocks above the block origin (original TileBannerRenderer:41), the original
 * rotate 180 degrees about X follows and the facing of a banner that stands in
 * a world is the original rotate 180 degrees about Y plus the original
 * {@code facing * 360 / 16} degrees about Y (original TileBannerRenderer:44-48).
 * A banner that has no world - the banner item of original ItemBannerRenderer -
 * skips those two rotations.
 */
public final class TileBannerRenderer implements BlockEntityRenderer<TileBanner> {
    /** The original texture of a banner without an aspect. */
    public static final ResourceLocation BLANK = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/banner_blank.png");
    /** The original texture of the cultist banner. */
    public static final ResourceLocation CULTIST = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/banner_cultist.png");
    /** Original UtilsFX.drawTag:552-560 - the aspect icon is drawn with alpha 0.75. */
    private static final int TAG_ALPHA = Math.round(0.75F * 255);

    /** One model for the render thread; the cloth rotation is set and reset inside the draw call. */
    private static final ModelBanner MODEL = new ModelBanner();

    public TileBannerRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileBanner tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = tile.getLevel();
        // Original TileBannerRenderer:33-34 and :60 - the world time of the banner and
        // its own position drive the wave of the cloth.
        long worldTime = level == null ? 0L : level.getGameTime();
        float waveTime = tile.getBlockPos().getX() * 7F + tile.getBlockPos().getY() * 9F + tile.getBlockPos().getZ() * 13F
                + worldTime + partialTick;
        draw(pose, buffers, light, overlay, tile.getAspect(), tile.getColor(), waveTime, tile.isOnWall(),
                EldritchBlockRules.bannerOriginalFacing(tile.getFacing()), level != null);
    }

    /**
     * The original banner drawing, shared by the block entity renderer and the
     * banner item renderer.
     *
     * @param aspect        the aspect on the banner, or null
     * @param colour        the original colour byte, minus one when the banner has none
     * @param waveTime      the original {@code x * 7 + y * 9 + z * 13 + worldTime + partialTicks}
     * @param onWall        the original wall flag of the banner
     * @param originalFacing the original facing byte of the banner
     * @param hasWorld      the original only rotates a banner that stands in a world
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                            Aspect aspect, int colour, float waveTime, boolean onWall,
                            int originalFacing, boolean hasWorld) {
        boolean plain = aspect == null && colour < 0;
        var out = buffers.getBuffer(RenderType.entityCutoutNoCull(plain ? CULTIST : BLANK));
        pose.pushPose();
        try {
            BannerPose.model(pose, originalFacing, hasWorld);
            if (!onWall) {
                MODEL.renderPole(pose, out, light, overlay);
            } else {
                // Original TileBannerRenderer:49-53.
                pose.translate(0.0F, 0.0F, -.4125F);
            }
            MODEL.renderBeam(pose, out, light, overlay);
            // Original TileBannerRenderer:55-58 sets the colour before the tabs and
            // resets it only after the cloth, so the cloth carries the dye tint too.
            int tint = 0xFFFFFFFF;
            if (colour >= 0) tint = 0xff000000 | EldritchBlockRules.bannerColour(colour);
            MODEL.tabLeft().render(pose, out, light, overlay, tint);
            MODEL.tabRight().render(pose, out, light, overlay, tint);
            // Original TileBannerRenderer:60-62.
            float wave = (.005F + .005F * Mth.cos(waveTime * (float) Math.PI * .02F)) * (float) Math.PI;
            MODEL.banner().xRot = wave;
            MODEL.renderBanner(pose, out, light, overlay, tint);
            MODEL.banner().xRot = 0.0F;
            if (aspect != null) {
                drawTag(pose, buffers, aspect, wave, light, overlay);
            }
        } finally {
            pose.popPose();
        }
    }

    /**
     * The original aspect tag of the banner, drawn on the waving cloth.
     *
     * <p>Original TileBannerRenderer:64-72 and original UtilsFX.drawTag:552-560:
     * the icon is drawn from {@code (-8, 4)} to {@code (8, 20)} of the scaled
     * space, which is a sixteen by sixteen icon, in the colour of the aspect and
     * with alpha 0.75.
     */
    private static void drawTag(PoseStack pose, MultiBufferSource buffers, Aspect aspect, float wave, int light, int overlay) {
        pose.pushPose();
        try {
            BannerPose.aspect(pose, wave);
            var out = buffers.getBuffer(RenderType.entityTranslucent(aspect.getImage()));
            int colour = aspect.getColor();
            int red = (colour >> 16) & 255, green = (colour >> 8) & 255, blue = colour & 255;
            // The original draws the icon from (x, y + 16) to (x + 16, y) with the
            // upper left corner first; the original call is drawTag(-8, 4, ...).
            float[][] points = {{-8, 20}, {8, 20}, {8, 4}, {-8, 4}};
            float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
            for (int index = 0; index < 4; index++) {
                out.addVertex(pose.last(), points[index][0], points[index][1], 0.0F)
                        .setColor(red, green, blue, TAG_ALPHA)
                        .setUv(uv[index][0], uv[index][1]).setOverlay(overlay).setLight(light)
                        .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
            }
        } finally {
            pose.popPose();
        }
    }
}
