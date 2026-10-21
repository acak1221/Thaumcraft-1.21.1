package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.tile.TileBannerRenderer;
import thaumcraft.common.blocks.BannerData;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * Original ItemBannerRenderer:25-59: the banner item of the cultist.
 *
 * <p>The original custom item renderer draws a {@code TileBanner} that carries
 * the colour and the aspect of the item. It only claims the render type when
 * the item really is the banner variant with a colour and with an aspect
 * (original ItemBannerRenderer:27-32); every other banner item keeps the
 * ordinary wooden device art. The native banner item keeps the colour and the
 * aspect in the {@code banner_data} component, so the same condition is
 * checked here.
 *
 * <p>The transform chain of the original is kept verbatim: the equipped
 * variants move the item one block on every axis (original
 * ItemBannerRenderer:43-46), the item is then centred on its own origin
 * (original ItemBannerRenderer:48), turned by 180 degrees about Z (original
 * ItemBannerRenderer:49) and moved by half a block down and back (original
 * ItemBannerRenderer:50) before the banner of the original
 * {@code TileBannerRenderer} is drawn.
 *
 * <p>The 1.7.10 GUI item pipeline that framed the original item no longer
 * exists, so the banner is centred in the item slot with the numbers derived
 * from the original chain: the chain leaves the banner at
 * {@code (-1, -0.03125, -0.828125)} of the item frame and the banner is two
 * blocks high, so the item slot keeps the half scale that fits it.
 */
public final class ItemBannerRenderer {
    /** The position of the banner in the item frame after the original chain. */
    private static final float BANNER_X = -1.0F, BANNER_Y = -0.03125F, BANNER_Z = -0.828125F;
    /** The banner of the original is two blocks high; the item slot keeps half of it. */
    private static final float SLOT_SCALE = 0.5F;

    private ItemBannerRenderer() {}

    /** The loader's builtin item hook. */
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!stack.is(ThaumcraftEldritchBlocks.BANNER_ITEM.get())) return;
        BannerData data = stack.getOrDefault(ThaumcraftEldritchBlocks.BANNER_DATA.get(), BannerData.NONE);
        Aspect aspect = data.aspect();
        int colour = data.color();
        if (colour < 0 && aspect == null) {
            // Original ItemBannerRenderer:27-32 - a banner without a colour and
            // without an aspect is not claimed by the custom renderer.
            drawWoodenDevice(pose, buffers, light, overlay);
            return;
        }
        boolean equipped = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        float shift = equipped ? 1.0F : 0.0F;
        pose.pushPose();
        try {
            // The framework of 1.21.1 centres every item on its own origin before
            // it calls the builtin renderer; the original chain centred the item
            // itself.
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.scale(SLOT_SCALE, SLOT_SCALE, SLOT_SCALE);
            // The banner is centred in the item slot; the original chain leaves it
            // at BANNER_X, BANNER_Y and BANNER_Z of the item frame.
            pose.translate(-BANNER_X - shift, -BANNER_Y - shift, -BANNER_Z - shift);
            // Original ItemBannerRenderer:43-50.
            if (equipped) pose.translate(1.0F, 1.0F, 1.0F);
            pose.translate(-0.5F, -0.5F, -0.5F);
            pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
            pose.translate(-0.5F, -1.0F, -0.5F);
            // Original TileBannerRenderer:31-74 without the facing rotation: the
            // banner of the item has no world, so the original skips it.
            TileBannerRenderer.draw(pose, buffers, light, overlay, aspect, colour, 0.0F, false, 0, false);
        } finally {
            pose.popPose();
        }
    }

    /** The ordinary art of the wooden device, used when the banner has no content. */
    private static void drawWoodenDevice(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var block = ThaumcraftEldritchBlocks.WOODEN_DEVICE.get();
        var state = block.defaultBlockState();
        var renderer = Minecraft.getInstance().getBlockRenderer();
        renderer.getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)),
                state, renderer.getBlockModel(state), 1.0F, 1.0F, 1.0F, light, overlay);
    }
}
