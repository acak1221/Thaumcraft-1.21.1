package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Original ModelWand:173-181: the animated depth layer of a primal focus.
 *
 * <p>The original draws a one block cube of the {@code focus_primal_depth} icon inside the focus
 * of the wand, shrunk to {@code 0.16} (a staff {@code 0.165 / 0.1765 / 0.165}) and moved down by
 * {@code 0.09} (a staff {@code 0.15}), before the translucent focus cube is drawn over it. The
 * cube is drawn in the model frame of the wand, so the context pose, the model basis correction
 * and the use pose of {@link thaumcraft.client.ThaumcraftGearModelsClient#renderWand} carry it
 * exactly like the focus cube itself.</p>
 *
 * <p>The icon is an animated strip of thirty frames, so the port keeps it in its own sheet
 * instead of the block atlas; the frame of the strip is the frame of the original's {@code
 * mcmeta} animation, and the six faces of the cube are the six {@code BlockRenderer.drawFaces}
 * faces of the original.</p>
 */
public final class PrimalWandFocusRenderer {
    private static final ResourceLocation DEPTH = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/items/focus_primal_depth.png");
    private PrimalWandFocusRenderer() {}

    /**
     * Original ModelWand:173-181: the depth layer, in the model frame of the wand.
     *
     * @param staff whether the wand is a staff, which uses the larger and lower layer
     * @param tick  the tick of the world, which selects the frame of the strip and the pulse
     */
    public static void renderDepthLayer(PoseStack pose, MultiBufferSource buffers, int light, int overlay, int tick, boolean staff) {
        Geometry.Depth depth = Geometry.depth(staff);
        pose.pushPose();
        try {
            pose.translate(0, depth.translationY(), 0);
            pose.scale((float) depth.x(), (float) depth.y(), (float) depth.z());
            renderDepth(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(DEPTH)), light, overlay, Geometry.frame(tick));
        } finally { pose.popPose(); }
    }

    private static void renderDepth(PoseStack pose, VertexConsumer out, int light, int overlay, int frame) {
        float h = .5F, v0 = frame / 30F, v1 = (frame + 1) / 30F;
        face(pose,out,light,overlay,v0,v1,0,-1,0, new float[][]{{-h,-h,-h},{h,-h,-h},{h,-h,h},{-h,-h,h}});
        face(pose,out,light,overlay,v0,v1,0,1,0, new float[][]{{-h,h,h},{h,h,h},{h,h,-h},{-h,h,-h}});
        face(pose,out,light,overlay,v0,v1,0,0,-1, new float[][]{{h,-h,-h},{-h,-h,-h},{-h,h,-h},{h,h,-h}});
        face(pose,out,light,overlay,v0,v1,0,0,1, new float[][]{{-h,-h,h},{h,-h,h},{h,h,h},{-h,h,h}});
        face(pose,out,light,overlay,v0,v1,-1,0,0, new float[][]{{-h,-h,-h},{-h,-h,h},{-h,h,h},{-h,h,-h}});
        face(pose,out,light,overlay,v0,v1,1,0,0, new float[][]{{h,-h,h},{h,-h,-h},{h,h,-h},{h,h,h}});
    }
    private static void face(PoseStack pose, VertexConsumer out, int light, int overlay, float v0, float v1,
                             float nx, float ny, float nz, float[][] points) {
        for (int index = 0; index < 4; index++) {
            float[] point = points[index];
            out.addVertex(pose.last(),point[0],point[1],point[2]).setColor(0xffffffff)
                    .setUv(index == 1 || index == 2 ? 1F : 0F, index < 2 ? v1 : v0)
                    .setOverlay(overlay).setLight(light).setNormal(pose.last(),nx,ny,nz);
        }
    }

    /** Independent data/math nested class can be tested without initializing any client renderer or GL state. */
    public static final class Geometry {
        private static final int[] FRAMES = {0,1,1,2,3,4,5,6,6,7,8,9,10,11,11,12,13,14,15,16,16,17,18,19,20,21,21,22,23,24,25,26,26,27,28,29};
        public record Depth(double x, double y, double z, double translationY) {}
        private Geometry() {}
        /** Original ModelWand:177-179 and :183-185: the scale and the shift of the depth layer. */
        public static Depth depth(boolean staff) { return staff ? new Depth(.165D,.1765D,.165D,-.15D) : new Depth(.16D,.16D,.16D,-.09D); }
        public static int frame(int tick) { return FRAMES[Math.floorMod(tick,FRAMES.length)]; }
        public static int pulse(int tick) { return (int)(195F + Mth.sin(tick / 3F) * 10F + 10F); }
        public static int[] frames() { return FRAMES.clone(); }
    }
}
