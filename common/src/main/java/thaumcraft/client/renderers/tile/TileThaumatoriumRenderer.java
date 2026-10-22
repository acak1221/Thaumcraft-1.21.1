package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import thaumcraft.client.renderers.models.ModelThaumatorium;
import thaumcraft.common.tiles.TileThaumatorium;

/** Original OBJ shell followed by the selected formula output every forty ticks. */
public final class TileThaumatoriumRenderer implements BlockEntityRenderer<TileThaumatorium> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/thaumatorium.png");
    private final ModelThaumatorium model = new ModelThaumatorium();
    public TileThaumatoriumRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileThaumatorium tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var facing = tile.facing();
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.XP.rotationDegrees(ThaumatoriumPose.MODEL_PITCH));
        pose.mulPose(Axis.ZP.rotationDegrees(ThaumatoriumPose.modelRoll(facing)));
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
        var mc = Minecraft.getInstance();
        if (tile.getLevel() == null || mc.player == null) return;
        var outputs = tile.previewOutputs();
        int index = ThaumatoriumPose.previewIndex(mc.player.tickCount, outputs.size());
        if (index < 0) return;
        pose.pushPose();
        pose.translate(.5 + facing.getStepX() / 1.99F, ThaumatoriumPose.PREVIEW_Y, .5 + facing.getStepZ() / 1.99F);
        pose.mulPose(Axis.YP.rotationDegrees(ThaumatoriumPose.previewYaw(facing)));
        pose.scale(ThaumatoriumPose.PREVIEW_SCALE, ThaumatoriumPose.PREVIEW_SCALE, ThaumatoriumPose.PREVIEW_SCALE);
        // Old RenderItem.renderInFrame=true suppresses bob/spin and uses the fixed display.
        mc.getItemRenderer().renderStatic(outputs.get(index), ItemDisplayContext.FIXED, light, overlay, pose, buffers, tile.getLevel(), 0);
        pose.popPose();
    }
    @Override public int getViewDistance() { return 64; }
}
