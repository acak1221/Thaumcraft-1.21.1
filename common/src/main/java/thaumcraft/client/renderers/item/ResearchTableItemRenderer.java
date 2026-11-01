package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.renderers.models.ModelResearchTable;
import thaumcraft.common.config.ThaumcraftContent;

/** Original inventory fixture: tools and unassigned notes, plus all six parchment sheets. */
public final class ResearchTableItemRenderer {
    private static final ResourceLocation TABLE = texture("models/restable.png"), SCROLL = texture("models/restable2.png");
    private static final ResourceLocation QUILL = texture("blocks/tablequill.png"), PARCHMENT = texture("misc/parchment.png");
    private static final ModelResearchTable model = new ModelResearchTable();

    private ResearchTableItemRenderer() {}
    private static ResourceLocation texture(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path); }

    /** BlockTableRenderer:35-40 initializes tools and default notes for the inventory table. */
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        if (!stack.is(ThaumcraftContent.RESEARCH_TABLE_ITEM.get())) return;
        renderTableParts(0, true, 0x999999, pose, buffers, light, overlay);
    }

    /** Static original parts shared by world and inventory without constructing a block entity. */
    public static void renderTableParts(int rotation, boolean hasTools, Integer scrollColor,
                                        PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        try {
            pose.translate(.5, 1, .5);
            pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.mulPose(Axis.YP.rotationDegrees(rotation));
            var wood = buffers.getBuffer(RenderType.entityCutoutNoCull(TABLE));
            model.root.render(pose, wood, light, overlay);
            // Exhausted tools remain visible, just as in the original renderer.
            if (hasTools) {
                // Original ModelResearchTable.renderInkwell:3-30 blends (770, 771): the ink (alpha 140) shows through.
                model.inkwell.render(pose, buffers.getBuffer(RenderType.entityTranslucent(TABLE)), light, overlay);
                pose.pushPose();
                try {
                    pose.mulPose(Axis.YP.rotationDegrees(-90));
                    pose.mulPose(Axis.XP.rotationDegrees(180));
                    pose.translate(-.17, .1, -.15);
                    pose.mulPose(Axis.YP.rotationDegrees(15));
                    pose.scale(.5F, .5F, .5F);
                    quill(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(QUILL)), light, overlay);
                } finally {
                    pose.popPose();
                }
            }
            for (int a = 0; a < 6; a++) {
                pose.pushPose();
                try {
                    pose.translate(.1, -.01 - a * .015, .35);
                    pose.mulPose(Axis.XP.rotationDegrees(-90));
                    pose.mulPose(Axis.ZP.rotationDegrees(15 + a % 3 * 2));
                    pose.scale(.5F, .6F, .6F);
                    // Original renderTileEntityAt:351-357, UtilsFX.renderQuad(parchment, 771, 1): blended edges.
                    quad(pose, buffers.getBuffer(RenderType.entityTranslucent(PARCHMENT)), light, overlay,
                            new float[][]{{0,1,0,0,1}, {1,1,0,1,1}, {1,0,0,1,0}, {0,0,0,0,0}}, 0, 0, -1);
                } finally {
                    pose.popPose();
                }
            }
            if (scrollColor != null) {
                int color = scrollColor;
                var scroll = buffers.getBuffer(RenderType.entityCutoutNoCull(SCROLL));
                model.scrollTube.render(pose, scroll, light, overlay);
                pose.pushPose();
                try {
                    pose.scale(1.2F, 1.2F, 1.2F);
                    model.scrollRibbon.render(pose, scroll, light, overlay, 0xFF000000 | color & 0xFFFFFF);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
    }

    /** The original renderItemIn2D 16x16 sprite extrusion, including its thin pixel edges. */
    private static void quill(PoseStack pose, VertexConsumer out, int light, int overlay) {
        float d = .025F;
        quad(pose, out, light, overlay, new float[][]{{0,0,0,1,1},{1,0,0,0,1},{1,1,0,0,0},{0,1,0,1,0}}, 0,0,1);
        quad(pose, out, light, overlay, new float[][]{{0,1,-d,1,0},{1,1,-d,0,0},{1,0,-d,0,1},{0,0,-d,1,1}}, 0,0,-1);
        for (int i = 0; i < 16; i++) {
            float p = i / 16F, q = (i + 1) / 16F, uv = 1 - (i + .5F) / 16F;
            quad(pose, out, light, overlay, new float[][]{{p,0,-d,uv,1},{p,0,0,uv,1},{p,1,0,uv,0},{p,1,-d,uv,0}}, -1,0,0);
            quad(pose, out, light, overlay, new float[][]{{q,1,-d,uv,0},{q,1,0,uv,0},{q,0,0,uv,1},{q,0,-d,uv,1}}, 1,0,0);
            quad(pose, out, light, overlay, new float[][]{{0,p,0,1,uv},{1,p,0,0,uv},{1,p,-d,0,uv},{0,p,-d,1,uv}}, 0,-1,0);
            quad(pose, out, light, overlay, new float[][]{{0,q,-d,1,uv},{1,q,-d,0,uv},{1,q,0,0,uv},{0,q,0,1,uv}}, 0,1,0);
        }
    }

    private static void quad(PoseStack pose, VertexConsumer out, int light, int overlay, float[][] points, float nx, float ny, float nz) {
        var transform = pose.last();
        for (float[] p : points) out.addVertex(transform, p[0], p[1], p[2]).setColor(-1).setUv(p[3], p[4])
                .setOverlay(overlay).setLight(light).setNormal(transform, nx, ny, nz);
    }
}
