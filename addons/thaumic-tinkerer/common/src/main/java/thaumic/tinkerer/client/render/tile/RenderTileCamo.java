/**
 * This class is new in the port of Thaumic Tinkerer to Minecraft 1.21.1 for Thaumcraft 4 Reload (no 1.7.10
 * counterpart); it is distributed under the same licence as the add-on (Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.render.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.block.tile.TileCamo;

/**
 * Draws the block a camouflaged platform imitates. The platform's own model is hidden while it is camouflaged (its
 * render shape is invisible then), and this renderer draws the imitated state at the block's position with the
 * lighting, the tint and the face culling of the world around it, as the piston renderer draws a moving block.
 * It does not get ambient-occlusion shading from the vanilla chunk mesh nor animated models of the imitated block:
 * docs/deviations/SimpleBlocks.md.
 */
public class RenderTileCamo implements BlockEntityRenderer<TileCamo> {

    public RenderTileCamo(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TileCamo tile, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            int packedOverlay) {
        BlockState camo = tile.getCamo();
        Level level = tile.getLevel();
        if (camo == null || level == null) return;
        VertexConsumer consumer = buffers.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(camo));
        Minecraft.getInstance().getBlockRenderer().renderBatched(camo, tile.getBlockPos(), level, poseStack, consumer, true,
                level.random);
    }
}
