/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [28 Sep 2013, 20:24:39 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.render.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.common.block.tile.TileFunnel;

/** Draws the jar of the Essentia Funnel on top of it, with the jar renderer of Thaumcraft (the jar item's renderer). */
public class RenderTileFunnel implements BlockEntityRenderer<TileFunnel> {

    private final ItemRenderer itemRenderer;

    public RenderTileFunnel(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TileFunnel funnel, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
            int overlay) {
        ItemStack stack = funnel.getItem(0);
        if (!stack.isEmpty()) {
            pose.pushPose();
            // The item renderer draws a builtin model with the corner of its block at the origin shifted by minus half a
            // block; the jar stands on the funnel, whose top is 1/8 block high.
            pose.translate(0.5, 0.125 + 0.5, 0.5);
            itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                    funnel.getLevel(), (int) funnel.getBlockPos().asLong());
            pose.popPose();
        }
    }
}
