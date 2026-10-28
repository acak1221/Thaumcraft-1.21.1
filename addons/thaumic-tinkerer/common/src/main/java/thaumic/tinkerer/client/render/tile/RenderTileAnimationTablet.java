/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [9 Sep 2013, 17:12:26 (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.render.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.block.tile.tablet.TileAnimationTablet;

/**
 * Draws the Animation Tablet: the turning discs on top (the centre disc, the left or right click symbol and the mark that
 * shows the way the tablet faces) and the held item floating above, swinging with the tablet (1.7.10
 * {@code RenderTileAnimationTablet}; the transforms are the original's).
 *
 * <p>Not ported: the GregTech World Accelerator "xN" text of the GTNH fork (no such mod).</p>
 */
public class RenderTileAnimationTablet implements BlockEntityRenderer<TileAnimationTablet> {

    private static final ResourceLocation overlayCenter = ResourceLocation.parse(LibResources.MISC_AT_CENTER);
    private static final ResourceLocation overlayLeft = ResourceLocation.parse(LibResources.MISC_AT_LEFT);
    private static final ResourceLocation overlayRight = ResourceLocation.parse(LibResources.MISC_AT_RIGHT);
    private static final ResourceLocation overlayIndent = ResourceLocation.parse(LibResources.MISC_AT_INDENT);

    private static final float[][] TRANSLATIONS = new float[][] { { 0F, 0F, -1F }, { -1F, 0F, 0F }, { 0F, 0F, 0F },
            { -1F, 0F, -1F } };

    /** The overlays are drawn at the heights of 1.7.10 (0.13 and 0.131), a hair apart so that they keep their order. */
    private static final float CENTER_HEIGHT = 0.13F;
    private static final float LEFT_HEIGHT = 0.1303F;
    private static final float RIGHT_HEIGHT = 0.131F;
    private static final float INDENT_HEIGHT = 0.1315F;

    private final BlockEntityRendererProvider.Context context;

    public RenderTileAnimationTablet(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    @Override
    public void render(TileAnimationTablet tile, float partialTicks, PoseStack pose, MultiBufferSource buffer, int packedLight,
            int packedOverlay) {
        int meta = switch (tile.getFacing()) {
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> 2;
        };

        int rotation = meta == 2 ? 270 : meta == 3 ? 90 : meta == 4 ? 0 : 180;

        pose.pushPose();
        renderOverlay(tile, pose, buffer, overlayCenter, -1, 0.65, CENTER_HEIGHT, partialTicks);
        if (tile.leftClick) renderOverlay(tile, pose, buffer, overlayLeft, 1, 1, LEFT_HEIGHT, partialTicks);
        else renderOverlay(tile, pose, buffer, overlayRight, 1, 1, RIGHT_HEIGHT, partialTicks);
        renderIndents(pose, buffer, rotation + 90F);

        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(0.1, 0.2 + Math.cos(System.currentTimeMillis() / 600D) / 18F, 0.5);
        float[] translations = TRANSLATIONS[meta - 2];
        pose.translate(translations[0], translations[1], translations[2]);
        pose.scale(0.8F, 0.8F, 0.8F);
        pose.translate(0.5F, 0F, 0.5F);
        float swingProgress = tile.getRenderSwingProgress(partialTicks);
        pose.mulPose(Axis.ZP.rotationDegrees(swingProgress));
        pose.translate(-0.5F, 0F, -0.5F);
        pose.translate(-swingProgress / 250F, swingProgress / 1000F, 0F);
        pose.mulPose(Axis.of(new Vector3f(1F, 0F, 1F).normalize()).rotationDegrees((float) Math.cos(System.currentTimeMillis() / 400F) * 5F));
        renderItem(tile, pose, buffer, packedLight, packedOverlay);
        pose.popPose();
    }

    /**
     * The held item, like an item in a frame. 1.7.10 drew the item entity at scale 1.5 (2.5 for blocks); the model-based
     * renderer of 1.21.1 draws a different base size, so the scales are chosen to give the same size on screen.
     * (Estimated from the original's numbers, not compared in a running client.)
     */
    private void renderItem(TileAnimationTablet tablet, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack stack = tablet.getItem(0);
        if (!stack.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5F, 0.55F, 0F);
            if (stack.getItem() instanceof BlockItem) pose.scale(1.55F, 1.55F, 1.55F);
            else pose.scale(1.9F, 1.9F, 1.9F);
            context.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, pose, buffer,
                    tablet.getLevel(), (int) tablet.getBlockPos().asLong());
            pose.popPose();
        }
    }

    private void renderOverlay(TileAnimationTablet tablet, PoseStack pose, MultiBufferSource buffer, ResourceLocation texture,
            int rotationMod, double size, float height, float partialTicks) {
        pose.pushPose();
        pose.translate(0.5F, height, 0.5F);
        float deg = (tablet.getTicksExisted(partialTicks) * rotationMod % 360F);
        pose.mulPose(Axis.YP.rotationDegrees(deg));
        quad(pose, buffer.getBuffer(RenderType.entityTranslucent(texture)), (float) (size / 2));
        pose.popPose();
    }

    private void renderIndents(PoseStack pose, MultiBufferSource buffer, float forceDeg) {
        pose.pushPose();
        pose.translate(0.5F, INDENT_HEIGHT, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(forceDeg));
        quad(pose, buffer.getBuffer(RenderType.entityTranslucent(overlayIndent)), 0.25F);
        pose.popPose();
    }

    /** The flat quad of the overlays, facing up: the corners and texture coordinates of 1.7.10. */
    private static void quad(PoseStack pose, VertexConsumer consumer, float half) {
        PoseStack.Pose last = pose.last();
        int light = LightTexture.FULL_BRIGHT;
        consumer.addVertex(last, -half, 0, half).setColor(255, 255, 255, 255).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(last, 0, 1, 0);
        consumer.addVertex(last, half, 0, half).setColor(255, 255, 255, 255).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(last, 0, 1, 0);
        consumer.addVertex(last, half, 0, -half).setColor(255, 255, 255, 255).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(last, 0, 1, 0);
        consumer.addVertex(last, -half, 0, -half).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(last, 0, 1, 0);
    }

    /** True when a tablet far away is still worth drawing: the discs and the item are small. */
    @Override
    public int getViewDistance() {
        return 64;
    }

    static Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
