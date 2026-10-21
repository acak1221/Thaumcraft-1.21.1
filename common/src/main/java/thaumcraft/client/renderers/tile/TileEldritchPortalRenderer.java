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
 */
public final class TileEldritchPortalRenderer implements BlockEntityRenderer<TileEldritchPortal> {
    /** The original portal texture with its sixteen frames. */
    public static final ResourceLocation PORTAL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/eldritch_portal.png");
    /** The original frame count of the portal texture. */
    public static final int FRAMES = 16;

    public TileEldritchPortalRenderer(BlockEntityRendererProvider.Context context) {}

    /**
     * Original TileEldritchPortal:34-37 - the gate is drawn in a cube of three blocks
     * around itself, so the renderer of the gate is only culled when the whole cube is
     * outside of the view. Minecraft 1.21.1 has no render bounding box for a block entity
     * any more, so the gate is drawn off screen.
     */
    @Override public boolean shouldRenderOffScreen(TileEldritchPortal tile) { return true; }

    @Override public void render(TileEldritchPortal tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile.getLevel() == null) return;
        int open = tile.getOpenCount();
        // The original clamps the opening counter to five and to thirty.
        float height = Mth.clamp(open + partialTick, 0.0F, 30.0F) / 30.0F;
        float width = Mth.clamp(open + partialTick, 0.0F, 5.0F) / 5.0F;
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
        float halfWidth = width * .5F;
        float halfHeight = height * .5F;
        Vector3f corner = new Vector3f((float) centre.x, (float) centre.y, (float) centre.z)
                .add(right.mul(-halfWidth)).add(up.mul(-halfHeight));
        Vector3f rightUp = right.mul(halfWidth * 2.0F);
        Vector3f upUp = up.mul(halfHeight * 2.0F);
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(PORTAL));
        // The original colour of the portal is magenta.
        int red = 255, green = 0, blue = 255;
        vertex(out, pose, corner, u, 1.0F, red, green, blue, light, overlay);
        vertex(out, pose, corner.add(rightUp), uMax, 1.0F, red, green, blue, light, overlay);
        vertex(out, pose, corner.add(rightUp).add(upUp), uMax, 0.0F, red, green, blue, light, overlay);
        vertex(out, pose, corner.add(upUp), u, 0.0F, red, green, blue, light, overlay);
    }

    private static void vertex(VertexConsumer out, PoseStack pose, Vector3f at, float u, float v,
                               int red, int green, int blue, int light, int overlay) {
        out.addVertex(pose.last(), at.x, at.y, at.z)
                .setColor(red, green, blue, 255)
                .setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
    }
}
