package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import thaumcraft.common.tiles.TilePedestal;

/** Only the displayed stack. The pedestal's body belongs to its native block JSON. */
public final class TilePedestalRenderer implements BlockEntityRenderer<TilePedestal> {
    private final ItemRenderer items;

    public TilePedestalRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }

    @Override public void render(TilePedestal tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = tile.getLevel();
        var actual = tile.getItem(0);
        if (level == null || actual.isEmpty()) return;
        var player = Minecraft.getInstance().player;
        float ticks = player == null ? 0 : player.tickCount + partialTick;
        var display = actual.copy();
        display.setCount(1); // Never clamp the actual infusion output or lose its components/glint.
        pose.pushPose();
        pose.translate(.5, 1.15 + Mth.sin(ticks % 32767 / 16) * .05, .5);
        pose.mulPose(Axis.YP.rotationDegrees(ticks % 360));
        // Native GROUND replaces the old throwaway EntityItem renderer. Ground
        // models already supply their own scale; doubling block items retains
        // the original pedestal's deliberate block/non-block size distinction.
        float scale = display.getItem() instanceof BlockItem ? 2 : 1;
        pose.scale(scale, scale, scale);
        items.renderStatic(display, ItemDisplayContext.GROUND, light, overlay, pose, buffers, level, 0);
        pose.popPose();
    }

    @Override public boolean shouldRenderOffScreen(TilePedestal tile) { return true; }
}
