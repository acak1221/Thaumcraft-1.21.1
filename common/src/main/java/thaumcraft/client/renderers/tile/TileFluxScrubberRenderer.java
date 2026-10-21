package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.client.renderers.models.ModelEldritchCap;
import thaumcraft.common.tiles.TileFluxScrubber;

/**
 * Original TileFluxScrubberRenderer: the cap of the flux scrubber.
 *
 * <p>The original draws the {@code Cap} group of {@code obelisk_cap.obj} with
 * the flux scrubber texture and the {@code Tip} group floating above it. The
 * native tree already carries the converted wavefront model in
 * {@link ModelEldritchCap}, so the geometry is reused as it is.
 */
public final class TileFluxScrubberRenderer implements BlockEntityRenderer<TileFluxScrubber> {
    /** The original texture of the flux scrubber. */
    public static final ResourceLocation SCRUBBER = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/fluxscrubber.png");

    private final ModelEldritchCap model = new ModelEldritchCap();

    public TileFluxScrubberRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileFluxScrubber tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var player = Minecraft.getInstance().player;
        // Original TileFluxScrubberRenderer:38-39 - the phase of the bob is the
        // age of the player, the partial tick and the counter of the scrubber.
        float time = (player == null ? 0.0F : player.tickCount + partialTick) + tile.getCount();
        float bob = bobFor(time);
        var out = buffers.getBuffer(RenderType.entityCutoutNoCull(SCRUBBER));
        pose.pushPose();
        try {
            pose.translate(.5F, .5F, .5F);
            orientation(pose, tile.getFacing());
            pose.translate(0.0F, 0.0F, -.5F);
            model.renderCapGroup(pose, out, light, overlay, 0xffffffff);
            // The original lifts the tip of the scrubber while it works.
            pose.translate(0.0F, 0.0F, -bob);
            model.renderTipGroup(pose, out, light, overlay, 0xffffffff);
        } finally {
            pose.popPose();
        }
    }

    /** The original translateFromOrientation of the flux scrubber. */
    private static void orientation(PoseStack pose, net.minecraft.core.Direction facing) {
        Orientation rotation = orientationOf(facing);
        switch (rotation.axis()) {
            case X -> pose.mulPose(Axis.XP.rotationDegrees(rotation.degrees()));
            case Y -> pose.mulPose(Axis.YP.rotationDegrees(rotation.degrees()));
            case NONE -> { }
        }
    }

    /** The axis the original turns the cap of the scrubber around. */
    public enum RotationAxis { X, Y, NONE }

    /**
     * The bob of the tip of the cap of the original
     * (oracle {@code thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java:38-40}):
     * {@code sin(phase/8)*0.075 + 0.075}, so the tip never sinks into the cap
     * and never rises more than a tenth and a half of a block above it.
     *
     * @param phase the age of the player, the partial tick and the counter of
     *              the scrubber
     * @return the distance of the tip in front of the cap
     */
    public static float bobFor(float phase) {
        return Mth.sin(phase / 8.0F) * .075F + .075F;
    }

    /**
     * The turn of the original {@code translateFromOrientation} (oracle
     * {@code thaumcraft/client/renderers/tile/TileFluxScrubberRenderer.java:45-61}).
     *
     * <p>The original turns the facing zero (down) and one (up) around the X
     * axis by minus and plus ninety degrees and the facing three (south), four
     * (west) and five (east) around the Y axis by one hundred and eighty, plus
     * and minus ninety degrees. The facing two (north) keeps the model as it
     * is.
     */
    public record Orientation(RotationAxis axis, float degrees) {}

    /**
     * The turn of the cap of the scrubber for that facing.
     *
     * @param facing the facing of the scrubber
     * @return the axis and the angle of the original turn
     */
    public static Orientation orientationOf(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case DOWN -> new Orientation(RotationAxis.X, -90.0F);
            case UP -> new Orientation(RotationAxis.X, 90.0F);
            case NORTH -> new Orientation(RotationAxis.NONE, 0.0F);
            case SOUTH -> new Orientation(RotationAxis.Y, 180.0F);
            case WEST -> new Orientation(RotationAxis.Y, 90.0F);
            default -> new Orientation(RotationAxis.Y, -90.0F);
        };
    }
}
