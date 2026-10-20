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
        float time = (player == null ? 0.0F : player.tickCount + partialTick) + tile.getCharges();
        float bob = Mth.sin(time / 8.0F) * .075F + .075F;
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
        switch (facing) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            case NORTH -> {
            }
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            default -> pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
    }
}
