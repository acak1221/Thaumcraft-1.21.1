package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import thaumcraft.common.tiles.TileWarded;

/** Draws the exact saved blockstate, not a generic substitute. Original connected rune/glass overlays remain a later visual unit. */
public final class TileWardedRenderer implements BlockEntityRenderer<TileWarded> {
    public TileWardedRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(TileWarded tile, float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        if (!tile.validWard()) return;
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(tile.originalState(), poses, buffers, light, overlay);
    }
}
