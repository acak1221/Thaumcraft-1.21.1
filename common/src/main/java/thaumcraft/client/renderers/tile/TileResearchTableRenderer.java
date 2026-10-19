package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.IScribeTools;
import thaumcraft.client.renderers.models.ModelResearchTable;
import thaumcraft.common.blocks.BlockTable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.tiles.TileResearchTable;

public final class TileResearchTableRenderer implements BlockEntityRenderer<TileResearchTable> {
    private static final ResourceLocation TABLE = texture("models/restable.png"), SCROLL = texture("models/restable2.png");
    private static final ResourceLocation QUILL = texture("blocks/tablequill.png"), PARCHMENT = texture("misc/parchment.png");
    private final ModelResearchTable model = new ModelResearchTable();

    public TileResearchTableRenderer(BlockEntityRendererProvider.Context context) {}
    private static ResourceLocation texture(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + path); }

    @Override public void render(TileResearchTable tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int variant = tile.getBlockState().getValue(BlockTable.VARIANT);
        if (variant < 2 || variant > 5) return;
        pose.pushPose();
        pose.translate(.5, 1, .5);
        pose.mulPose(Axis.XP.rotationDegrees(180));
        int rotation = switch (variant) { case 2 -> 270; case 3 -> 90; case 4 -> 180; default -> 0; };
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        var wood = buffers.getBuffer(RenderType.entityCutoutNoCull(TABLE));
        model.root.render(pose, wood, light, overlay);
        // Exhausted tools remain visible, just as in the original renderer.
        if (!tile.getItem(0).isEmpty() && tile.getItem(0).getItem() instanceof IScribeTools) {
            model.inkwell.render(pose, wood, light, overlay);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-90));
            pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.translate(-.17, .1, -.15);
            pose.mulPose(Axis.YP.rotationDegrees(15));
            pose.scale(.5F, .5F, .5F);
            quill(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(QUILL)), light, overlay);
            pose.popPose();
        }
        for (int a = 0; a < 6; a++) {
            pose.pushPose();
            pose.translate(.1, -.01 - a * .015, .35);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.ZP.rotationDegrees(15 + a % 3 * 2));
            pose.scale(.5F, .6F, .6F);
            quad(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(PARCHMENT)), light, overlay,
                    new float[][]{{0,1,0,0,1}, {1,1,0,1,1}, {1,0,0,1,0}, {0,0,0,0,0}}, 0, 0, -1);
            pose.popPose();
        }
        if (!tile.getItem(1).isEmpty() && tile.getItem(1).getItem() instanceof ItemResearchNotes) {
            var data = tile.getItem(1).get(ThaumcraftContent.RESEARCH_NOTE_DATA.get());
            int color = data == null ? 0x999999 : data.color();
            var scroll = buffers.getBuffer(RenderType.entityCutoutNoCull(SCROLL));
            model.scrollTube.render(pose, scroll, light, overlay);
            pose.pushPose();
            pose.scale(1.2F, 1.2F, 1.2F);
            model.scrollRibbon.render(pose, scroll, light, overlay, 0xFF000000 | color & 0xFFFFFF);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TileResearchTable tile) { return true; }

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
