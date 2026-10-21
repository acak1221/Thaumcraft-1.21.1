package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.renderers.models.ModelTunnelField;
import thaumcraft.common.blocks.BlockCosmeticSolid;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.tiles.TileEldritchNothing;

/**
 * Original TileEldritchNothingRenderer: the blank eldritch filler.
 *
 * <p>The original draws the fixed function tunnel on every face of the block
 * whose neighbour is <em>not</em> an opaque cube (oracle
 * {@code TileEldritchNothingRenderer.java:39-59}), so the void swallows the air
 * of the room it borders and the filler itself stays invisible where it touches
 * stone. The 1.7.10 effect is reproduced with the native
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
            // Oracle TileEldritchNothingRenderer:39-59 - the tunnel is drawn on the
            // face whose neighbour is not an opaque cube, so the void is visible on
            // the border between the filler and the air of the room.
            if (isOpaqueCube(level, neighbour)) continue;
            pose.pushPose();
            try {
                if (inRange) tunnel.renderFace(pose, buffers, face, 1.0F, light, overlay);
                else tunnel.renderFarFace(pose, buffers, face, light, overlay);
            } finally {
                pose.popPose();
            }
        }
    }

    /**
     * The 1.7.10 {@code Block.isOpaqueCube} of a block of the Outer Lands.
     *
     * <p>The original asks the neighbour whether it is an opaque cube and draws the
     * tunnel when it is not (oracle {@code TileEldritchNothingRenderer.java:39-59}).
     * The 1.21.1 equivalent of an opaque cube is
     * {@link BlockState#isSolidRender}, but three blocks of the family do not agree
     * with it:
     *
     * <ul>
     *   <li>the eldritch filler (oracle {@code BlockEldritchNothing}) does not
     *       override {@code isOpaqueCube}, so it is an opaque cube and two fillers
     *       do not show the tunnel between them;</li>
     *   <li>the cosmetic solid (oracle {@code BlockCosmeticSolid}) does not override
     *       it either, so the floor of a room is an opaque cube;</li>
     *   <li>the eldritch stone (oracle {@code BlockEldritch.java:171-173}) overrides
     *       it to false, so it never hides the tunnel.</li>
     * </ul>
     */
    public static boolean isOpaqueCube(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ThaumcraftEldritchBlocks.ELDRITCH_NOTHING.get())) return true;
        if (state.getBlock() instanceof BlockCosmeticSolid) return true;
        if (state.getBlock() instanceof BlockEldritch) return false;
        return state.isSolidRender(level, pos);
    }

    /** The original distance test of the 1.7 renderer. */
    private static boolean inRange(BlockPos pos) {
        var player = Minecraft.getInstance().player;
        if (player == null) return true;
        return player.distanceToSqr(Vec3.atCenterOf(pos)) < IN_RANGE * IN_RANGE;
    }
}
