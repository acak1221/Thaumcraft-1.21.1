/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [16 Sep 2013, 22:39:36 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.client.render.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.common.block.tile.TileEnchanter;

/**
 * The tool of the Osmotic Enchanter lies on its top, the wand hovers beside it. Both are drawn the way 1.7.10 placed them
 * (rotation, scale and offsets of {@code RenderTileEnchanter}); the item is drawn by the item renderer, which draws the
 * model of the item, where 1.7.10 extruded the flat icon. The wand hovers and tilts with the clock as before.
 */
public class RenderTileEnchanter implements BlockEntityRenderer<TileEnchanter> {

    private final ItemRenderer itemRenderer;

    public RenderTileEnchanter(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(TileEnchanter enchanter, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.75, 0.0);

        ItemStack item = enchanter.getItem(TileEnchanter.SLOT_TOOL);
        if (!item.isEmpty()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(90F));
            final float scale = 0.7F;
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0.6F, -0.2F, 0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(30F));
            // 1.7.10 drew the icon from its corner; the model of 1.21.1 is centred on the origin.
            poseStack.translate(0.5F, 0.5F, 0F);
            itemRenderer.renderStatic(item, ItemDisplayContext.NONE, packedLight, packedOverlay, poseStack, buffers,
                    enchanter.getLevel(), (int) enchanter.getBlockPos().asLong());
            poseStack.popPose();
        }

        item = enchanter.getItem(TileEnchanter.SLOT_WAND);
        if (!item.isEmpty()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(90F));
            final float scale = 0.5F;
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0.6F, 1.5F, -0.1F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-70F));
            long millis = Util.getMillis();

            poseStack.translate(0F, 0F, (float) (Math.cos((double) millis / 1000F) - 1.2F) / 10F);
            poseStack.translate(0F, 0.325F, 0F);
            poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.cos((double) millis / 500F) * 5F));
            poseStack.translate(0F, -0.325F, 0F);

            itemRenderer.renderStatic(item, ItemDisplayContext.GROUND, packedLight, packedOverlay, poseStack, buffers,
                    enchanter.getLevel(), (int) enchanter.getBlockPos().asLong() + 1);
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
