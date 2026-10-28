/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.render.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.block.tile.TileRepairer;

/**
 * The dynamic part of the Thaumic Restorer: the tool that floats and turns inside the glass, and the magic circle that
 * turns on top of it. The frame and the glass are the block model ({@code models/block/repairer.json}), which
 * replaces the 1.7.10 {@code ModelRepairer} drawn by the same renderer.
 */
public class RenderTileRepairer implements BlockEntityRenderer<TileRepairer> {

    private static final ResourceLocation repair = ResourceLocation.parse(LibResources.MISC_REPAIR);
    private static final ResourceLocation repairOff = ResourceLocation.parse(LibResources.MISC_REPAIR_OFF);

    private final ItemRenderer itemRenderer;

    public RenderTileRepairer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TileRepairer repairer, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
            int overlay) {
        float time = repairer.ticksExisted + partialTick;
        float deg = time * 0.75F % 360F;

        ItemStack item = repairer.getItem(0);
        if (!item.isEmpty()) {
            pose.pushPose();
            // the middle of the glass box, bobbing by 0.05 blocks; the item is half a block wide
            pose.translate(0.5, 0.5 + Math.sin(time / 10F) * 0.05, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(deg));
            itemRenderer.renderStatic(item, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                    repairer.getLevel(), (int) repairer.getBlockPos().asLong());
            pose.popPose();
        }

        renderOverlay(pose, buffers, repairer.tookLastTick ? repair : repairOff, 1.25F, deg);
    }

    private void renderOverlay(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, double size,
            float deg) {
        pose.pushPose();
        // 1.7.10: translate(0, -0.525, 0) from the top of the frame, which is the top of the block
        pose.translate(0.5, 0.975, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(deg));

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
        float size1 = (float) (size / 2);
        float size2 = -size1;
        vertex(consumer, pose, size2, size1, 0, 1);
        vertex(consumer, pose, size1, size1, 1, 1);
        vertex(consumer, pose, size1, size2, 1, 0);
        vertex(consumer, pose, size2, size2, 0, 0);
        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack pose, float x, float z, float u, float v) {
        consumer.addVertex(pose.last(), x, 0, z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), 0, 1, 0);
    }
}
