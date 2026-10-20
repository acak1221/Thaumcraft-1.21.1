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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.renderers.models.ModelEldritchCap;
import thaumcraft.client.renderers.models.ModelTunnelField;
import thaumcraft.common.tiles.TileEldritchObelisk;

/**
 * Original TileEldritchObeliskRenderer: the eldritch obelisk.
 *
 * <p>The original draws three things at the base of the obelisk: the four
 * vertical tunnel planes of the fixed function effect, the four glass walls of
 * the three block high tube and the two octagonal plates of the wavefront
 * {@code obelisk_cap.obj} model.
 *
 * <p>The fixed function tunnel is replaced by the native
 * {@link ModelTunnelField}; the glass walls and the plates keep the original
 * geometry and the original textures.
 */
public final class TileEldritchObeliskRenderer implements BlockEntityRenderer<TileEldritchObelisk> {
    /** The original texture of the glass wall of the obelisk. */
    public static final ResourceLocation SIDE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/obelisk_side.png");
    /** The original texture of the glass wall in the Outer Lands. */
    public static final ResourceLocation SIDE_OUTER = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/obelisk_side_2.png");
    /** The original height of the obelisk tube in blocks. */
    private static final float HEIGHT = 3.0F;
    /** The original range of the detailed effect, in blocks. */
    private static final double IN_RANGE = 512.0D;

    private final ModelTunnelField tunnel = new ModelTunnelField();
    private final ModelEldritchCap model = new ModelEldritchCap();

    public TileEldritchObeliskRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileEldritchObelisk tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile.getLevel() == null) return;
        var player = Minecraft.getInstance().player;
        float count = (player == null ? 0.0F : player.tickCount + partialTick);
        // The original bob of the obelisk.
        float bob = Mth.sin(count / 10.0F) * .1F + .1F;
        boolean inRange = player == null || player.distanceToSqr(Vec3.atCenterOf(tile.getBlockPos())) < IN_RANGE * IN_RANGE;
        boolean outer = tile.getLevel().dimension().location().getPath().equals("outer_lands");
        ResourceLocation side = outer ? SIDE_OUTER : SIDE;
        ResourceLocation cap = outer ? TileEldritchCapRenderer.CAP_OUTER : TileEldritchCapRenderer.CAP;
        // The four tunnel planes of the tube.
        pose.pushPose();
        try {
            pose.translate(0.0F, 1.0F + bob, 0.0F);
            pose.scale(1.0F, HEIGHT, 1.0F);
            for (Direction face : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                if (inRange) tunnel.renderFace(pose, buffers, face, 1.0F, light, overlay);
                else tunnel.renderFarFace(pose, buffers, face, light, overlay);
            }
        } finally {
            pose.popPose();
        }
        // The four glass walls of the tube.
        var sideBuffer = buffers.getBuffer(RenderType.entityTranslucent(side));
        pose.pushPose();
        try {
            pose.translate(.5F, 1.0F + bob, .5F);
            for (int index = 0; index < 4; index++) {
                pose.pushPose();
                try {
                    pose.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
                    pose.translate(0.0F, 0.0F, -.5F);
                    wall(pose, sideBuffer, light, overlay);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
        // The two octagonal plates of the tube.
        var capBuffer = buffers.getBuffer(RenderType.entityCutoutNoCull(cap));
        pose.pushPose();
        try {
            pose.translate(.5F, 1.0F + bob, .5F);
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            model.renderCapGroup(pose, capBuffer, light, overlay, 0xffffffff);
        } finally {
            pose.popPose();
        }
        pose.pushPose();
        try {
            pose.translate(.5F, 1.0F + HEIGHT + bob, .5F);
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            model.renderCapGroup(pose, capBuffer, light, overlay, 0xffffffff);
        } finally {
            pose.popPose();
        }
    }

    /** The original renderSide: a one block wide and three block high wall. */
    private static void wall(PoseStack pose, VertexConsumer out, int light, int overlay) {
        // The original order of the quad: top left, top right, bottom right, bottom left.
        float[][] corners = {{-.5F, HEIGHT}, {.5F, HEIGHT}, {.5F, 0.0F}, {-.5F, 0.0F}};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int index = 0; index < 4; index++) {
            out.addVertex(pose.last(), corners[index][0], corners[index][1], 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(uv[index][0], uv[index][1]).setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
        }
    }
}
