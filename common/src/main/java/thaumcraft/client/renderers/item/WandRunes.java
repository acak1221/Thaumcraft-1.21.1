package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The rune ring of a sceptre and the rune bands of the primal staff.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/models/gear/ModelWand.render} (JAR bytecode
 * offsets 938-1199) and {@code ModelWand.drawRune}: after the cap and the focus, in the frame of
 * the model, a sceptre draws ten runes of {@code textures/misc/script.png} turning around the rod,
 * and a rod with runes (the primal staff) draws four bands of fourteen runes along it. Both use
 * the lightmap value {@code 200} and the blend {@code GL_SRC_ALPHA, GL_ONE}; each rune pulses in
 * colour, alpha and size with the age of the player.</p>
 */
public final class WandRunes {
    public static final ResourceLocation SCRIPT = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/script.png");
    /** The lightmap coordinate of the original ({@code OpenGlHelper.setLightmapTextureCoords(200 % 65536, 200 / 65536)}). */
    public static final int LIGHT = 200;
    public static final int SCEPTRE_RUNES = 10, STAFF_BANDS = 4, STAFF_BAND_RUNES = 14;

    private WandRunes() {}

    /** The runes of the wand, in the frame of the model after the cap and the focus. */
    public static void render(PoseStack pose, MultiBufferSource buffers, boolean sceptre, boolean staffRunes, int ticks) {
        if (!sceptre && !staffRunes) return;
        VertexConsumer out = buffers.getBuffer(RuneType.TYPE);
        if (sceptre) {
            for (int rune = 0; rune < SCEPTRE_RUNES; rune++) {
                pose.pushPose();
                try {
                    pose.mulPose(Axis.YP.rotationDegrees(36 * rune + ticks));
                    drawRune(pose, out, .16F, -.01F, -.125F, rune, ticks);
                } finally { pose.popPose(); }
            }
        }
        if (staffRunes) {
            pose.pushPose();
            try {
                for (int band = 0; band < STAFF_BANDS; band++) {
                    pose.mulPose(Axis.YP.rotationDegrees(90));
                    for (int index = 0; index < STAFF_BAND_RUNES; index++) {
                        drawRune(pose, out, .36F + index * .14F, -.01F, -.08F, staffRune(band, index), ticks);
                    }
                }
            } finally { pose.popPose(); }
        }
    }

    /** The rune of a band of the primal staff: {@code (index + band * 3) % 16}. */
    public static int staffRune(int band, int index) { return (index + band * 3) % 16; }

    /** The colour, alpha and size pulse of a rune: red, green, the size term and the alpha. */
    public static float[] pulse(int rune, int ticks) {
        float phase = ticks + rune * 5;
        float red = Mth.sin(phase / 5F) * .1F + .88F;
        float green = Mth.sin(phase / 7F) * .1F + .63F;
        float wave = Mth.sin(phase / 10F) * .2F;
        return new float[]{red, green, wave, wave + .6F};
    }

    private static void drawRune(PoseStack pose, VertexConsumer out, float x, float y, float z, int rune, int ticks) {
        float[] pulse = pulse(rune, ticks);
        float half = .06F + pulse[2] / 40F;
        float u0 = .0625F * rune, u1 = u0 + .0625F;
        pose.pushPose();
        try {
            pose.mulPose(Axis.ZP.rotationDegrees(90));
            pose.translate(x, y, z);
            var last = pose.last();
            vertex(out, last, -half, half, u1, 1, pulse);
            vertex(out, last, half, half, u1, 0, pulse);
            vertex(out, last, half, -half, u0, 0, pulse);
            vertex(out, last, -half, -half, u0, 1, pulse);
        } finally { pose.popPose(); }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float u, float v, float[] pulse) {
        out.addVertex(pose, x, y, 0).setColor(pulse[0], pulse[1], .2F, pulse[3]).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LIGHT).setNormal(pose, 0, 0, 1);
    }

    /** Additive, lit by the lightmap, unculled (1.7.10 drew held items with culling off). */
    private static final class RuneType extends RenderType {
        static final RenderType TYPE = new RuneType(new TextureStateShard(SCRIPT, true, false));

        private RuneType(TextureStateShard texture) {
            super("thaumcraft_wand_runes", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                    () -> begin(texture), () -> end(texture));
        }

        private static void begin(TextureStateShard texture) {
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
            texture.setupRenderState();
            LIGHTNING_TRANSPARENCY.setupRenderState();
            NO_CULL.setupRenderState();
            LIGHTMAP.setupRenderState();
            OVERLAY.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            COLOR_DEPTH_WRITE.setupRenderState();
        }

        private static void end(TextureStateShard texture) {
            COLOR_DEPTH_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            OVERLAY.clearRenderState();
            LIGHTMAP.clearRenderState();
            NO_CULL.clearRenderState();
            LIGHTNING_TRANSPARENCY.clearRenderState();
            texture.clearRenderState();
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
        }
    }
}
