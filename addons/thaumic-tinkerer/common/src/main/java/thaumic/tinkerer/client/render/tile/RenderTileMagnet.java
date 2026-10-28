/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [12 Sep 2013, 17:55:04 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.render.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import thaumic.tinkerer.common.block.tile.TileMagnet;

/**
 * The three rings of lightning above the magnet: blue when it pulls, red when it pushes, larger with the redstone
 * signal, drifting up and down. The magnet itself is the block model ({@code models/block/magnet*.json}), which
 * replaces the 1.7.10 {@code ModelMagnet} drawn by the same renderer.
 */
public class RenderTileMagnet<T extends TileMagnet> implements BlockEntityRenderer<T> {

    /** The lightning ring of Thaumcraft's hover harness ({@code ItemHoverHarness.iconLightningRing}); in the block atlas. */
    private static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/lightningring");

    public RenderTileMagnet(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(T magnet, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
            int overlay) {
        int redstone = magnet.getRedstone();
        if (redstone == 0) return; // the scale of the rings is zero without a signal
        boolean blue = magnet.isPulling();

        float size = redstone / 15F * 0.7F + 0.4F;
        float half = size / 2;
        float delta = (float) ((Math.cos(System.currentTimeMillis() / 500F) + 1) * 0.09 + 0.1);
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
                .getSprite(RING);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));

        for (int i = 0; i < 3; i++) {
            pose.pushPose();
            // 0.9 blocks above the foot of the block, each ring a little lower than the one before
            pose.translate(0.5, 0.9 - i * delta, 0.5);
            ring(consumer, pose, sprite, half, blue ? 0 : 255, blue ? 255 : 0);
            pose.popPose();
        }
    }

    private static void ring(VertexConsumer consumer, PoseStack pose, TextureAtlasSprite sprite, float half, int red,
            int blue) {
        vertex(consumer, pose, -half, half, sprite.getU0(), sprite.getV1(), red, blue);
        vertex(consumer, pose, half, half, sprite.getU1(), sprite.getV1(), red, blue);
        vertex(consumer, pose, half, -half, sprite.getU1(), sprite.getV0(), red, blue);
        vertex(consumer, pose, -half, -half, sprite.getU0(), sprite.getV0(), red, blue);
    }

    private static void vertex(VertexConsumer consumer, PoseStack pose, float x, float z, float u, float v, int red,
            int blue) {
        consumer.addVertex(pose.last(), x, 0, z).setColor(red, 0, blue, 230).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), 0, 1, 0);
    }
}
