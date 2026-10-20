package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.tiles.TileWandPedestal;

/**
 * Original TileWandPedestalRenderer: the focus that floats above the wand
 * pedestal.
 *
 * <p>The original draws the focus of the pedestal as an item that turns around
 * its own axis and bobs up and down, and the wispy line that connects the
 * pedestal with the node it drains while it works.
 *
 * <p>Deviation: the original wispy line is a 1.7 fixed function beam. The
 * native port draws the same beam with the same {@code wispy.png} texture as a
 * translucent camera facing strip, which keeps the original texture and the
 * original colour but not the original fixed function distortion.
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
        double fromX = origin.getX() + .5D;
        double fromY = origin.getY() + 1.65D - bob * 2.0D;
        double fromZ = origin.getZ() + .5D;
        double toX = tile.drainX + .5D;
        double toY = tile.drainY + .5D;
        double toZ = tile.drainZ + .5D;
        float length = (float) Math.sqrt((toX - fromX) * (toX - fromX) + (toY - fromY) * (toY - fromY)
                + (toZ - fromZ) * (toZ - fromZ));
        if (length <= 0.0F) return;
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = camera.x - (fromX + toX) * .5D;
        double dy = camera.y - (fromY + toY) * .5D;
        double dz = camera.z - (fromZ + toZ) * .5D;
        double norm = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (norm < 1.0E-6D) return;
        dx /= norm;
        dy /= norm;
        dz /= norm;
        // A camera facing strip between the two points.
        double rx = -dz;
        double rz = dx;
        double rl = Math.sqrt(rx * rx + rz * rz);
        if (rl < 1.0E-6D) {
            rx = 1.0D;
            rz = 0.0D;
            rl = 1.0D;
        }
        rx /= rl;
        rz /= rl;
        float width = .05F;
        var out = buffers.getBuffer(RenderType.entityTranslucent(WISPY));
        int color = tile.drainColor;
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        vertex(out, pose, fromX + rx * width, fromY, fromZ + rz * width, 0.0F, 0.0F, red, green, blue, light, overlay);
        vertex(out, pose, fromX - rx * width, fromY, fromZ - rz * width, 1.0F, 0.0F, red, green, blue, light, overlay);
        vertex(out, pose, toX - rx * width, toY, toZ - rz * width, 1.0F, 1.0F, red, green, blue, light, overlay);
        vertex(out, pose, toX + rx * width, toY, toZ + rz * width, 0.0F, 1.0F, red, green, blue, light, overlay);
    }

    private static void vertex(VertexConsumer out, PoseStack pose, double x, double y, double z, float u, float v,
                               int red, int green, int blue, int light, int overlay) {
        out.addVertex(pose.last(), (float) x, (float) y, (float) z)
                .setColor(red, green, blue, 255)
                .setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
    }
}
