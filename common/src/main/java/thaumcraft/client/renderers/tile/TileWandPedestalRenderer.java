package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.fx.FloatyLine;
import thaumcraft.common.tiles.TileWandPedestal;

/**
 * Original TileWandPedestalRenderer: the focus that floats above the wand
 * pedestal.
 *
 * <p>The original draws the focus of the pedestal as an item that turns around
 * its own axis and bobs up and down, and the wispy line that connects the
 * pedestal with the node it drains while it works.
 *
 * <p>The original wispy line is the 1.7 fixed function helper
 * {@code UtilsFX.drawFloatyLine} (oracle {@code thaumcraft/client/lib/UtilsFX.java:733-817}): a pair of
 * crossed textured ribbons that scroll along the line. The native port draws the same ribbons with the
 * same {@code wispy.png} texture, the same colour and the same numbers.
 */
public final class TileWandPedestalRenderer implements BlockEntityRenderer<TileWandPedestal> {
    /** The original texture of the drain line. */
    public static final ResourceLocation WISPY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/wispy.png");
    /** The original height of the floating focus. */
    private static final float FOCUS_HEIGHT = 1.15F;

    public TileWandPedestalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileWandPedestal tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile.getLevel() == null) return;
        var player = Minecraft.getInstance().player;
        float ticks = (player == null ? 0.0F : player.tickCount + partialTick);
        float bob = Mth.sin(ticks % 32767.0F / 16.0F) * .05F;
        ItemStack focus = tile.getItem(0);
        if (!focus.isEmpty()) {
            pose.pushPose();
            try {
                pose.translate(.5F, FOCUS_HEIGHT + bob, .5F);
                pose.mulPose(Axis.YP.rotationDegrees(ticks % 360.0F));
                Minecraft.getInstance().getItemRenderer()
                        .renderStatic(focus, ItemDisplayContext.FIXED, light, overlay, pose, buffers,
                                tile.getLevel(), 0);
            } finally {
                pose.popPose();
            }
        }
        if (tile.draining) {
            drawBeam(pose, buffers, tile, bob, partialTick, light, overlay);
        }
    }

    /** The original floaty line between the pedestal and the node it drains. */
    private static void drawBeam(PoseStack pose, MultiBufferSource buffers, TileWandPedestal tile, float bob,
                                 float partialTick, int light, int overlay) {
        var origin = tile.getBlockPos();
        // The original TileWandPedestalRenderer:52 draws the line from the centre of the pedestal at
        // the height 1.65 of the block, without the bob of the focus, to the centre of the drained
        // block. The native block entity renderer draws in the space of its own block, so the two
        // points of the original are taken relative to that block.
        double fromX = .5D;
        double fromY = 1.65D - bob * 2.0D;
        double fromZ = .5D;
        double toX = tile.drainX - origin.getX() + .5D;
        double toY = tile.drainY - origin.getY() + .5D;
        double toZ = tile.drainZ - origin.getZ() + .5D;
        float distance = Math.min(tile.getLevel().getGameTime(), 10.0F) / 10.0F;
        FloatyLine.draw(pose, buffers, fromX, fromY, fromZ, toX, toY, toZ,
                origin.getX() + fromX, origin.getY() + fromY, origin.getZ() + fromZ,
                tile.drainColor, WISPY, -0.02F, distance, FloatyLine.DEFAULT_WIDTH);
    }
}
