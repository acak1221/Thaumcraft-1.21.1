package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import thaumcraft.common.tiles.TileEldritchPortal;

/**
 * Original TileEldritchPortalRenderer: the eldritch gate.
 *
 * <p>The original draws a camera facing quad of the {@code eldritch_portal}
 * texture. The texture holds sixteen animation frames in a row, the original
 * picks the frame with {@code System.nanoTime() / 50000000 % 16} and scales the
 * quad with the opening counter of the portal.
 *
 * <p>Oracle {@code TileEldritchPortalRenderer.java:38-60}: the corners of the
 * quad are the four corners of a unit square in the camera frame
 * ({@code -left - up}, {@code -left + up}, {@code left + up} and
 * {@code left - up}), the horizontal part is multiplied by
 * {@code scale = min(openCount, 5) / 5} and the vertical part by
 * {@code scaley = min(openCount, 30) / 30}. The half width of the gate is
 * therefore {@code scale} and its half height {@code scaley}, so a fully opened
 * gate is two blocks wide and two blocks high. The texture of the frame is
 * turned by ninety degrees in the original: the {@code u} of the frame runs
 * from the bottom to the top of the gate and its {@code v} from the left to the
 * right.
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

    @Override public void render(TileEldritchPortal tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile.getLevel() == null) return;
        int open = tile.getOpenCount();
        // The original clamps the opening counter to five and to thirty.
        float height = Mth.clamp(open + partialTick, 0.0F, HEIGHT_LIMIT) / HEIGHT_LIMIT;
        float width = Mth.clamp(open + partialTick, 0.0F, WIDTH_LIMIT) / WIDTH_LIMIT;
        if (width <= 0.0F || height <= 0.0F) return;
        long time = System.nanoTime() / 50000000L;
        int frame = (int) (time % FRAMES);
        float u = frame / (float) FRAMES;
        float uMax = u + 1.0F / FRAMES;
        Vec3 centre = Vec3.atCenterOf(tile.getBlockPos());
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f look = camera.getLookVector();
        Vector3f right = new Vector3f(look).cross(new Vector3f(0.0F, 1.0F, 0.0F));
        if (right.lengthSquared() < 1.0E-6F) right.set(1.0F, 0.0F, 0.0F);
        right.normalize();
        Vector3f up = new Vector3f(right).cross(look).normalize();
        // Oracle TileEldritchPortalRenderer:44-52 - the scale and the scaley of the
        // original are the half extents of the quad, so the open gate is two blocks
        // wide and two blocks high.
        float halfWidth = width;
        float halfHeight = height;
        Vector3f corner = new Vector3f((float) centre.x, (float) centre.y, (float) centre.z)
                .add(right.mul(-halfWidth)).add(up.mul(-halfHeight));
        Vector3f rightUp = right.mul(halfWidth * 2.0F);
        Vector3f upUp = up.mul(halfHeight * 2.0F);
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(PORTAL));
        // The original colour of the portal is magenta.
        int red = 255, green = 0, blue = 255;
        // Oracle TileEldritchPortalRenderer:76-81 - the frame of the original is
        // turned by ninety degrees, the u of the frame is the vertical axis of the
        // gate and its v the horizontal one.
        vertex(out, pose, corner, u, 0.0F, red, green, blue, light, overlay);
        vertex(out, pose, corner.add(rightUp), u, 1.0F, red, green, blue, light, overlay);
        vertex(out, pose, corner.add(rightUp).add(upUp), uMax, 1.0F, red, green, blue, light, overlay);
        vertex(out, pose, corner.add(upUp), uMax, 0.0F, red, green, blue, light, overlay);
    }

    private static void vertex(VertexConsumer out, PoseStack pose, Vector3f at, float u, float v,
                               int red, int green, int blue, int light, int overlay) {
        out.addVertex(pose.last(), at.x, at.y, at.z)
                .setColor(red, green, blue, 255)
                .setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
    }
}
