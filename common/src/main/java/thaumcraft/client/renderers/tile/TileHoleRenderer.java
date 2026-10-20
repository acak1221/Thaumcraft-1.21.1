package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.renderers.models.ModelTunnelField;
import thaumcraft.common.tiles.TileHole;

/**
 * Original TileHoleRenderer: the hole a portable hole leaves behind.
 *
 * <p>The original draws the fixed function tunnel on every face of the hole
 * that is not blocked by another hole, so the hole looks like it swallows the
 * world around it. The 1.7.10 effect is reproduced with the native
 * {@link ModelTunnelField}.
 */
public final class TileHoleRenderer implements BlockEntityRenderer<TileHole> {
    /** The original range of the detailed effect, in blocks. */
    private static final double IN_RANGE = 512.0D;

    private final ModelTunnelField tunnel = new ModelTunnelField();

    public TileHoleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileHole tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = tile.getLevel();
        if (level == null) return;
        var player = Minecraft.getInstance().player;
        boolean inRange = player == null || player.distanceToSqr(Vec3.atCenterOf(tile.getBlockPos())) < IN_RANGE * IN_RANGE;
        BlockPos pos = tile.getBlockPos();
        for (Direction face : Direction.values()) {
            BlockPos neighbour = pos.relative(face);
            if (!level.hasChunkAt(neighbour)) continue;
            // The original draws the tunnel on the faces that are open to the air.
            if (!level.getBlockState(neighbour).isAir()) continue;
            pose.pushPose();
            try {
                if (inRange) tunnel.renderFace(pose, buffers, face, 1.0F, light, overlay);
                else tunnel.renderFarFace(pose, buffers, face, light, overlay);
            } finally {
                pose.popPose();
            }
        }
    }
}
