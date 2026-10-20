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
import thaumcraft.common.tiles.TileEldritchNothing;

/**
 * Original TileEldritchNothingRenderer: the blank eldritch filler.
 *
 * <p>The original draws the fixed function tunnel on every face of the block
 * that touches a solid neighbour, so the void appears to eat into the stone
 * around it. The 1.7.10 effect is reproduced with the native
 * {@link ModelTunnelField}.
 */
public final class TileEldritchNothingRenderer implements BlockEntityRenderer<TileEldritchNothing> {
    /** The original range of the detailed effect, in blocks. */
    private static final double IN_RANGE = 512.0D;
    private final ModelTunnelField tunnel = new ModelTunnelField();

    public TileEldritchNothingRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileEldritchNothing tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = tile.getLevel();
        if (level == null) return;
        boolean inRange = inRange(tile.getBlockPos());
        BlockPos pos = tile.getBlockPos();
        for (Direction face : Direction.values()) {
            BlockPos neighbour = pos.relative(face);
            if (!level.hasChunkAt(neighbour)) continue;
            // The original draws the tunnel on the faces that touch a solid block.
            if (level.getBlockState(neighbour).isAir()) continue;
            pose.pushPose();
            try {
                if (inRange) tunnel.renderFace(pose, buffers, face, 1.0F, light, overlay);
                else tunnel.renderFarFace(pose, buffers, face, light, overlay);
            } finally {
                pose.popPose();
            }
        }
    }

    /** The original distance test of the 1.7 renderer. */
    private static boolean inRange(BlockPos pos) {
        var player = Minecraft.getInstance().player;
        if (player == null) return true;
        return player.distanceToSqr(Vec3.atCenterOf(pos)) < IN_RANGE * IN_RANGE;
    }
}
