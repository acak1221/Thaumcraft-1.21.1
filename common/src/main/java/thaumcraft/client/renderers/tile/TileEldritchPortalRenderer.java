package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import thaumcraft.client.renderers.CameraBasis;
import thaumcraft.common.tiles.TileEldritchPortal;

/**
 * Original TileEldritchPortalRenderer: the eldritch gate.
 *
 * <p>The original draws a camera facing quad of the {@code eldritch_portal}
 * texture. The texture holds sixteen animation frames in a row, the original
 * picks the frame with {@code System.nanoTime() / 50000000 % 16} and scales the
 * quad with the opening counter of the portal.
 *
 * <p>Oracle {@code TileEldritchPortalRenderer.java:38-81}: the corners of the
 * quad are the four corners of a unit square of the basis of the original
 * {@code ActiveRenderInfo} ({@code -left - up}, {@code -left + up},
 * {@code left + up} and {@code left - up}), the horizontal part of a corner is
 * multiplied by {@code scale = min(openCount, 5) / 5} and its vertical part by
 * {@code scaley = min(openCount, 30) / 30}. The half width of the gate is
 * therefore the scale and its half height the scaley, so a fully opened gate is
 * two blocks wide and two blocks high. The frame of the texture is turned by
 * ninety degrees in the original: its {@code u} runs from the bottom to the top
 * of the gate and its {@code v} from the left to the right.
 */
public final class TileEldritchPortalRenderer implements BlockEntityRenderer<TileEldritchPortal> {
    /** The original portal texture with its sixteen frames. */
    public static final ResourceLocation PORTAL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/eldritch_portal.png");
    /** The original frame count of the portal texture. */
    public static final int FRAMES = 16;
    /** The original maximum of the opening counter of the width, oracle :41. */
    public static final int WIDTH_LIMIT = 5;
    /** The original maximum of the opening counter of the height, oracle :40. */
    public static final int HEIGHT_LIMIT = 30;

    public TileEldritchPortalRenderer(BlockEntityRendererProvider.Context context) {}

    /**
     * Original TileEldritchPortal:34-37 - the gate is drawn in a cube of three blocks
     * around itself, so the block entity renderer of the gate is only culled when the
     * whole cube is outside of the view.
     */
    @Override public boolean shouldRenderOffScreen(TileEldritchPortal tile) { return true; }

    /**
     * Original TileEldritchPortal:31-33 - the gate is drawn up to ninety six blocks away,
     * the square root of the 9216 of the original.
     */
    @Override public int getViewDistance() { return 96; }

    @Override public void render(TileEldritchPortal tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile.getLevel() == null) return;
        int open = tile.getOpenCount();
        // The original clamps the opening counter to five and to thirty.
        float height = halfHeight(open, partialTick);
        float width = halfWidth(open, partialTick);
        if (width <= 0.0F || height <= 0.0F) return;
        long time = System.nanoTime() / 50000000L;
        int frame = (int) (time % FRAMES);
        float[] uv = frameUv(frame);
        Vector3f left = CameraBasis.left();
        Vector3f up = CameraBasis.up();
        // Oracle TileEldritchPortalRenderer:44-52 - the scale and the scaley of the
        // original are the half extents of the quad, so the open gate is two blocks
        // wide and two blocks high.
        Vector3f bottomRight = new Vector3f(left).negate().sub(up);
        Vector3f topRight = new Vector3f(left).negate().add(up);
        Vector3f topLeft = new Vector3f(left).add(up);
        Vector3f bottomLeft = new Vector3f(left).sub(up);
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(PORTAL));
        // The original colour of the portal is magenta.
        int red = 255, green = 0, blue = 255;
        pose.pushPose();
        try {
            // The 1.21.1 block entity renderer is placed on the corner of the block
            // (LevelRenderer:1232-1275), so the gate is drawn in the middle of it,
            // like the original (TileEldritchPortalRenderer:57-59).
            pose.translate(.5F, .5F, .5F);
            // Oracle TileEldritchPortalRenderer:76-81 - the frame of the original is
            // turned by ninety degrees, the u of the frame is the vertical axis of
            // the gate and its v the horizontal one.
            vertex(out, pose, bottomRight, width, height, uv[0], uv[1], red, green, blue, light, overlay);
            vertex(out, pose, topRight, width, height, uv[2], uv[3], red, green, blue, light, overlay);
            vertex(out, pose, topLeft, width, height, uv[4], uv[5], red, green, blue, light, overlay);
            vertex(out, pose, bottomLeft, width, height, uv[6], uv[7], red, green, blue, light, overlay);
        } finally { pose.popPose(); }
    }

    /**
     * The half width of the gate of the original.
     *
     * <p>Oracle {@code TileEldritchPortalRenderer.java:41,44}: the scale is the
     * opening counter clamped to five and divided by five, and it is the half
     * width of the quad.
     */
    public static float halfWidth(int openCount, float partialTick) {
        return Mth.clamp(openCount + partialTick, 0.0F, WIDTH_LIMIT) / WIDTH_LIMIT;
    }

    /**
     * The half height of the gate of the original.
     *
     * <p>Oracle {@code TileEldritchPortalRenderer.java:40,45}: the scaley is the
     * opening counter clamped to thirty and divided by thirty, and it is the half
     * height of the quad.
     */
    public static float halfHeight(int openCount, float partialTick) {
        return Mth.clamp(openCount + partialTick, 0.0F, HEIGHT_LIMIT) / HEIGHT_LIMIT;
    }

    /**
     * The four texture coordinates of the gate of the original.
     *
     * <p>Oracle {@code TileEldritchPortalRenderer.java:74-81}: the frame of the
     * sheet is turned by ninety degrees, the u of the frame is the vertical axis
     * of the gate and its v the horizontal one. The corners are the ones of the
     * original in its order: the bottom right, the top right, the top left and
     * the bottom left.
     */
    public static float[] frameUv(int frame) {
        float u = frame / (float) FRAMES;
        float uMax = u + 1.0F / FRAMES;
        return new float[]{u, 1.0F, uMax, 1.0F, uMax, 0.0F, u, 0.0F};
    }

    private static void vertex(VertexConsumer out, PoseStack pose, Vector3f corner, float scale, float scaley,
                               float u, float v, int red, int green, int blue, int light, int overlay) {
        // The original multiplies the horizontal part of the corner with the scale
        // and its vertical part with the scaley.
        out.addVertex(pose.last(), corner.x * scale, corner.y * scaley, corner.z * scale)
                .setColor(red, green, blue, 255)
                .setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
    }
}
