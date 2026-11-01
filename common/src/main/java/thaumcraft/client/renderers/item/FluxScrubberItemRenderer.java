package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.renderers.tile.TileFluxScrubberRenderer;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/** Original BlockStoneDeviceRenderer:103-106 uses a DOWN-facing scrubber with counter zero. */
public final class FluxScrubberItemRenderer {
    private FluxScrubberItemRenderer() {}

    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        if (!stack.is(ThaumcraftEldritchBlocks.FLUX_SCRUBBER_ITEM.get())) return;
        var player = Minecraft.getInstance().player;
        float phase = player == null ? 0 : player.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        TileFluxScrubberRenderer.renderParts(Direction.DOWN, phase, pose, buffers, light, overlay);
    }
}
