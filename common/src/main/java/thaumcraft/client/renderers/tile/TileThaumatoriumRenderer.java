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
        try {
            pose.translate(.5, 0, .5);
            pose.mulPose(Axis.XP.rotationDegrees(ThaumatoriumPose.MODEL_PITCH));
            pose.mulPose(Axis.ZP.rotationDegrees(ThaumatoriumPose.modelRoll(facing)));
            model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        } finally {
            pose.popPose();
        }
        var mc = Minecraft.getInstance();
        if (tile.getLevel() == null || mc.player == null) return;
        var outputs = tile.previewOutputs();
        int index = ThaumatoriumPose.previewIndex(mc.player.tickCount, outputs.size());
        if (index < 0 || outputs.get(index).isEmpty()) return;
        pose.pushPose();
        try {
            pose.translate(.5 + facing.getStepX() / 1.99F, ThaumatoriumPose.PREVIEW_Y, .5 + facing.getStepZ() / 1.99F);
            pose.mulPose(Axis.YP.rotationDegrees(ThaumatoriumPose.previewYaw(facing)));
            pose.scale(ThaumatoriumPose.PREVIEW_SCALE, ThaumatoriumPose.PREVIEW_SCALE, ThaumatoriumPose.PREVIEW_SCALE);
            // The original creates a fresh EntityItem (age/hoverStart zero): its bob is
            // a constant .1, not a running animation. RenderItem.renderInFrame stops spin.
            pose.translate(0, .1F, 0);
            var output = outputs.get(index);
            var itemModel = mc.getItemRenderer().getModel(output, tile.getLevel(), null, 0);
            if (itemModel.isGui3d()) {
                // Official 1.7.10 client bny.java:80-95, ordinary 3D block in a frame.
                pose.scale(1.25F, 1.25F, 1.25F);
                pose.translate(0, .05F, 0);
                pose.mulPose(Axis.YP.rotationDegrees(-90));
                pose.scale(.25F, .25F, .25F);
            } else {
                // bny.java:137-139,183-195; centre the native generated item on the
                // old renderItemIn2D quad, whose extent was 0..1, -1/16..0.
                pose.scale(.5128205F, .5128205F, .5128205F);
                pose.translate(0, -.05F, 0);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.translate(0, .25F, .0109375F);
            }
            mc.getItemRenderer().renderStatic(output, ItemDisplayContext.NONE, light, overlay, pose, buffers, tile.getLevel(), 0);
        } finally {
            pose.popPose();
        }
    }
    @Override public int getViewDistance() { return 64; }
    @Override public boolean shouldRenderOffScreen(TileThaumatorium tile) { return true; }
}
