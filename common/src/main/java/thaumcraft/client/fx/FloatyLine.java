package thaumcraft.client.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * The original floaty line of the TC4 1.7.10 client.
 *
 * <p>The original draws it in {@code thaumcraft/client/lib/UtilsFX.java:733-817} and the cultist has
 * its own copy of the same body in {@code thaumcraft/client/renderers/entity/RenderCultist.java:83-176}.
 * The line is a pair of crossed textured ribbons: the first one has its half width on the Y axis, the
 * second one on the X axis, both of them {@code width} wide, and both of them are drawn with the
 * additive blend {@code (770, 1)}, without culling and without depth write.</p>
 *
 * <p>The original numbers: the distance {@code dist} between the two points, the number of blocks
 * {@code round(dist)}, the length of the line {@code blocks * (golemLinkQuality / 2)} with the original
 * quality sixteen (oracle {@code thaumcraft/common/config/Config.java:881}), the loop over
 * {@code length * distance} steps, the fade {@code f3 = 1 - abs(i - length / 2) / (length / 2)}, the
 * sine wobble of the three axes with the phase {@code coordinate % 16 + dist * (1 - f2) * quality / 2 -
 * (time % 32767 / 5)}, the vertex alpha {@code f3} and the scrolling texture coordinate
 * {@code (1 - f2) * dist - time * speed}.</p>
 */
public final class FloatyLine {
    /** The original {@code Config.golemLinkQuality} default of sixteen (oracle Config.java:881). */
    public static final int LINK_QUALITY = 16;
    /** The original half width of the line of the wand pedestal and of the node. */
    public static final float DEFAULT_WIDTH = 0.15F;
    /** The original half width of the line of the cultist (oracle RenderCultist.java:77). */
    public static final float CULTIST_WIDTH = 0.25F;
    /** The original constant alpha of the line of the cultist (oracle RenderCultist.java:138). */
    public static final float CULTIST_ALPHA = .8F;

    /**
     * The two original bodies of the line.
     *
     * <p>The helper of the library ({@code thaumcraft/client/lib/UtilsFX.java:733-817}) draws the
     * ribbons with the additive blend {@code (770, 1)}, without the depth write and with the alpha of
     * the fade {@code f3} of every step. The cultist has its own copy of the body
     * ({@code thaumcraft/client/renderers/entity/RenderCultist.java:83-163}) with the ordinary alpha
     * blend {@code (770, 771)}, with the depth write and with the constant alpha {@code 0.8f}.</p>
     */
    public enum Flavour {
        /** The body of the original {@code UtilsFX.drawFloatyLine}. */
        LIBRARY,
        /** The body of the original {@code RenderCultist.drawFloatyLine}. */
        CULTIST
    }

    private static final java.util.Map<ResourceLocation, RenderType> TYPES = new java.util.concurrent.ConcurrentHashMap<>();

    private FloatyLine() {}

    /**
     * Draws the original floaty line.
     *
     * @param pose the pose of the caller, the line is drawn in the space of that pose
     * @param fromX the first point of the original, in the space of the pose
     * @param fromY the first point of the original, in the space of the pose
     * @param fromZ the first point of the original, in the space of the pose
     * @param toX the second point of the original, in the space of the pose
     * @param toY the second point of the original, in the space of the pose
     * @param toZ the second point of the original, in the space of the pose
     * @param worldX the first point of the original in world coordinates, the phase of the wobble
     * @param worldY the first point of the original in world coordinates, the phase of the wobble
     * @param worldZ the first point of the original in world coordinates, the phase of the wobble
     * @param color the colour of the original
     * @param texture the texture of the original
     * @param speed the scroll speed of the original
     * @param distance the draw distance of the original, from zero to one
     * @param width the half width of the original
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers,
                            double fromX, double fromY, double fromZ,
                            double toX, double toY, double toZ,
                            double worldX, double worldY, double worldZ,
                            int color, ResourceLocation texture, float speed, float distance, float width,
                            Flavour flavour) {
        double dcX = fromX - toX;
        double dcY = fromY - toY;
        double dcZ = fromZ - toZ;
        float dist = (float) Math.sqrt(dcX * dcX + dcY * dcY + dcZ * dcZ);
        float length = lineLength(dist);
        if (length <= 0.0F || distance <= 0.0F) return;
        float time = System.nanoTime() / 30000000L;
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        var out = buffers.getBuffer(TYPES.computeIfAbsent(texture, key -> new FloatyLineType(key, flavour)));
        ribbon(out, pose, true, dcX, dcY, dcZ, toX, toY, toZ, worldX, worldY, worldZ,
                dist, length, distance, time, speed, red, green, blue, width, flavour);
        ribbon(out, pose, false, dcX, dcY, dcZ, toX, toY, toZ, worldX, worldY, worldZ,
                dist, length, distance, time, speed, red, green, blue, width, flavour);
    }

    /**
     * One ribbon of the original. The first one has its half width on the Y axis, the second one on the
     * X axis. The original draws a triangle strip; the native port draws the same triangles as quads
     * so that two lines of the same frame never share one strip.
     */
    private static void ribbon(VertexConsumer out, PoseStack pose, boolean vertical,
                               double dcX, double dcY, double dcZ, double toX, double toY, double toZ,
                               double worldX, double worldY, double worldZ,
                               float dist, float length, float distance, float time, float speed,
                               int red, int green, int blue, float width, Flavour flavour) {
        boolean first = true;
        double previousLowX = 0.0D, previousLowY = 0.0D, previousLowZ = 0.0D;
        double previousHighX = 0.0D, previousHighY = 0.0D, previousHighZ = 0.0D;
        float previousU = 0.0F, previousFade = 0.0F;
        int index = 0;
        while ((float) index <= length * distance) {
            float f2 = index / length;
            float f3 = fade(index, length);
            double dx = dcX + Math.sin((float) (phase(worldZ, dist, f2, time) / 4.0F)) * 0.5F * f3;
            double dy = dcY + Math.sin((float) (phase(worldX, dist, f2, time) / 3.0F)) * 0.5F * f3;
            double dz = dcZ + Math.sin((float) (phase(worldY, dist, f2, time) / 2.0F)) * 0.5F * f3;
            float u = scroll(f2, dist, time, speed);
            // The cultist draws the constant alpha 0.8f, the library helper the fade of the step.
            float fade = flavour == Flavour.CULTIST ? CULTIST_ALPHA : f3;
            double centreX = toX + dx * f2;
            double centreY = toY + dy * f2;
            double centreZ = toZ + dz * f2;
            double lowX = vertical ? centreX : centreX - width;
            double lowY = vertical ? centreY - width : centreY;
            double lowZ = centreZ;
            double highX = vertical ? centreX : centreX + width;
            double highY = vertical ? centreY + width : centreY;
            double highZ = centreZ;
            if (!first) {
                // The original strip: the low edge of a step carries the top of the texture cell.
                vertex(out, pose, previousLowX, previousLowY, previousLowZ, previousU, 1.0F, red, green, blue, previousFade);
                vertex(out, pose, previousHighX, previousHighY, previousHighZ, previousU, 0.0F, red, green, blue, previousFade);
                vertex(out, pose, highX, highY, highZ, u, 0.0F, red, green, blue, fade);
                vertex(out, pose, lowX, lowY, lowZ, u, 1.0F, red, green, blue, fade);
            }
            previousLowX = lowX;
            previousLowY = lowY;
            previousLowZ = lowZ;
            previousHighX = highX;
            previousHighY = highY;
            previousHighZ = highZ;
            previousU = u;
            previousFade = fade;
            first = false;
            index++;
        }
    }

    /**
     * The length of the original line: the number of blocks of the distance times the half of the
     * original link quality (oracle {@code thaumcraft/client/lib/UtilsFX.java:776-778}).
     */
    public static float lineLength(float distance) {
        return Math.round(distance) * ((float) LINK_QUALITY / 2.0F);
    }

    /**
     * The fade of the original: {@code 1 - abs(i - length / 2) / (length / 2)}
     * (oracle {@code thaumcraft/client/lib/UtilsFX.java:786}).
     */
    public static float fade(int index, float length) {
        return 1.0F - Math.abs(index - length / 2.0F) / (length / 2.0F);
    }

    /**
     * The scrolling texture coordinate of the original: {@code (1 - f2) * dist - time * speed}
     * (oracle {@code thaumcraft/client/lib/UtilsFX.java:791}).
     */
    public static float scroll(float f2, float distance, float time, float speed) {
        return (1.0F - f2) * distance - time * speed;
    }

    /**
     * The phase of the wobble of the original, the same for the three axes
     * (oracle {@code thaumcraft/client/lib/UtilsFX.java:787-789}).
     */
    public static double phase(double coordinate, float distance, float f2, float time) {
        return coordinate % 16.0 + (double) (distance * (1.0F - f2) * (float) LINK_QUALITY / 2.0F)
                - (double) (time % 32767.0F / 5.0F);
    }

    private static void vertex(VertexConsumer out, PoseStack pose, double x, double y, double z,
                               float u, float v, int red, int green, int blue, float alpha) {
        int a = (int) (alpha * 255.0F);
        out.addVertex(pose.last(), (float) x, (float) y, (float) z)
                .setColor(red, green, blue, a < 0 ? 0 : a > 255 ? 255 : a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
    }

    /**
     * The render state of the original: the additive blend {@code (770, 1)}, no culling, no depth write.
     */
    private static final class FloatyLineType extends RenderType {
        private final TextureStateShard shard;
        private final boolean additive;
        private final boolean depthWrite;

        private FloatyLineType(ResourceLocation texture, Flavour flavour) {
            super("thaumcraft_floaty_line", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    4096, false, true, () -> {}, () -> {});
            this.shard = new TextureStateShard(texture, false, false);
            // The library helper disables the depth write, the cultist keeps it.
            this.depthWrite = flavour == Flavour.CULTIST;
            this.additive = flavour == Flavour.LIBRARY;
        }

        @Override public void setupRenderState() {
            RENDERTYPE_EYES_SHADER.setupRenderState();
            shard.setupRenderState();
            // The library helper uses the additive blend (770, 1), the cultist the ordinary (770, 771).
            if (additive) LIGHTNING_TRANSPARENCY.setupRenderState();
            else TRANSLUCENT_TRANSPARENCY.setupRenderState();
            NO_CULL.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            COLOR_WRITE.setupRenderState();
            RenderSystem.depthMask(depthWrite);
        }

        @Override public void clearRenderState() {
            COLOR_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            NO_CULL.clearRenderState();
            if (additive) LIGHTNING_TRANSPARENCY.clearRenderState();
            else TRANSLUCENT_TRANSPARENCY.clearRenderState();
            shard.clearRenderState();
            RENDERTYPE_EYES_SHADER.clearRenderState();
        }
    }
}
