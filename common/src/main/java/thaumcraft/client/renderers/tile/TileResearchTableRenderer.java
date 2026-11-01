package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import thaumcraft.api.IScribeTools;
import thaumcraft.client.renderers.item.ResearchTableItemRenderer;
import thaumcraft.common.blocks.BlockTable;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.ItemResearchNotes;
import thaumcraft.common.tiles.TileResearchTable;

public final class TileResearchTableRenderer implements BlockEntityRenderer<TileResearchTable> {
    public TileResearchTableRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileResearchTable tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int variant = tile.getBlockState().getValue(BlockTable.VARIANT);
        if (variant < 2 || variant > 5) return;
        int rotation = switch (variant) { case 2 -> 270; case 3 -> 90; case 4 -> 180; default -> 0; };
        // Exhausted tools remain visible, just as in the original renderer.
        boolean hasTools = !tile.getItem(0).isEmpty() && tile.getItem(0).getItem() instanceof IScribeTools;
        Integer color = null;
        if (!tile.getItem(1).isEmpty() && tile.getItem(1).getItem() instanceof ItemResearchNotes) {
            var data = tile.getItem(1).get(ThaumcraftContent.RESEARCH_NOTE_DATA.get());
            color = data == null ? 0x999999 : data.color();
        }
        ResearchTableItemRenderer.renderTableParts(rotation, hasTools, color, pose, buffers, light, overlay);
    }

    @Override public boolean shouldRenderOffScreen(TileResearchTable tile) { return true; }
}
