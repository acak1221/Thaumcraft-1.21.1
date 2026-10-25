package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.renderers.BannerPose;
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
 * (original ItemBannerRenderer:48), turned by 180 degrees about Y (original
 * ItemBannerRenderer:49) and moved by half a block down and back (original
 * ItemBannerRenderer:50) before the banner of the original
 * {@code TileBannerRenderer} is drawn.
 *
 * <p>The 1.7.10 GUI item pipeline that framed the original item no longer
 * exists, so {@link BannerPose#item} adds one shared slot frame around the
 * original context transforms. The banner is roughly two blocks high and uses half scale.
 */
public final class ItemBannerRenderer {
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
        pose.pushPose();
        try {
            BannerPose.item(pose, context);
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
