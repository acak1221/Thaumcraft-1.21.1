package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.client.renderers.models.ModelTunnelField;
import thaumcraft.common.blocks.BlockMirror;
import thaumcraft.common.tiles.TileMirror;
import thaumcraft.common.tiles.TileMirrorEssentia;

/**
 * Original TileMirrorRenderer: the item mirror and the essentia mirror.
 *
 * <p>The original draws the pane of the mirror on the face the mirror looks at
 * and the fixed function tunnel behind it while the mirror is linked to its
 * twin. The unstable mirror shakes the pane by the amount of its instability.
 *
 * @param <T> the tile the renderer is registered for
 */
public final class TileMirrorRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    /** The original texture of the pane of an unlinked mirror. */
    public static final ResourceLocation PANE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/mirrorpane.png");
    /** The original texture of the pane of a linked mirror. */
    public static final ResourceLocation PANE_TRANSPARENT = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/mirrorpanetrans.png");

    private final ModelTunnelField tunnel = new ModelTunnelField();
    private final java.util.Random random = new java.util.Random();

    public TileMirrorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(T tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile.getLevel() == null) return;
        boolean linked;
        float instability = 0.0F;
        if (tile instanceof TileMirror mirror) {
            linked = mirror.isLinked();
            if (mirror.instability > 0) instability = random.nextFloat() * (mirror.instability / 10000.0F);
        } else if (tile instanceof TileMirrorEssentia essentia) {
            linked = essentia.isLinked();
        } else {
            return;
        }
        Direction facing = tile.getBlockState().getValue(BlockMirror.FACING);
        if (linked) {
            // The tunnel of the linked mirror. The original draws it on the
            // face opposite to the facing of the mirror.
            pose.pushPose();
            try {
                tunnel.renderFace(pose, buffers, facing.getOpposite(), 1.0F, light, overlay);
            } finally {
                pose.popPose();
            }
            pane(pose, buffers, PANE_TRANSPARENT, facing, .02F + instability, light, overlay);
            return;
        }
        pane(pose, buffers, PANE, facing, .02F + instability, light, overlay);
    }

    /**
     * The original pane of the mirror. The original draws the pane on the side
     * opposite to the facing of the mirror, which is reproduced here.
     */
    private static void pane(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, Direction facing,
                             float offset, int light, int overlay) {
        var out = buffers.getBuffer(RenderType.entityTranslucent(texture));
        pose.pushPose();
        try {
            switch (facing) {
                case DOWN -> {
                    pose.translate(.5F, 1.5F, 1.5F);
                    pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
                }
                case UP -> {
                    pose.translate(.5F, .5F, .5F);
                    pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                }
                case NORTH -> pose.translate(.5F, .5F, 1.5F);
                case SOUTH -> {
                    pose.translate(1.5F, .5F, .5F);
                    pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                }
                case WEST -> {
                    pose.translate(1.5F, .5F, 1.5F);
                    pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                }
                default -> {
                    pose.translate(.5F, .5F, .5F);
                    pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
                }
            }
            pose.translate(0.0F, 0.0F, -offset);
            float[][] corners = {{-.5F, .5F}, {.5F, .5F}, {.5F, -.5F}, {-.5F, -.5F}};
            float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
            for (int index = 0; index < 4; index++) {
                out.addVertex(pose.last(), corners[index][0], corners[index][1], 0.0F)
                        .setColor(255, 255, 255, 255)
                        .setUv(uv[index][0], uv[index][1]).setOverlay(overlay).setLight(light)
                        .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
            }
        } finally {
            pose.popPose();
        }
    }
}
