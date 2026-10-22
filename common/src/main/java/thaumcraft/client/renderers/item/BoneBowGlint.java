package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Oracle ItemBowBoneRenderer:91-118, only for its EQUIPPED renderer. */
public final class BoneBowGlint extends RenderType {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/bone_bow_glint.png");
    private static final RenderType FIRST = type("first"), SECOND = type("second");

    private BoneBowGlint() { super("unused", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {}); }

    private static RenderType type(String pass) {
        var blend = new TransparencyStateShard("tc4_src_color_one", () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
        }, () -> { RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc(); });
        var shader = new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);
        var texture = new TextureStateShard(TEXTURE, false, false);
        float[][] oldColour = {null};
        return new RenderType("tc4_bone_glint_" + pass, DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256,
                false, false, () -> {
                    shader.setupRenderState(); texture.setupRenderState(); blend.setupRenderState();
                    oldColour[0] = RenderSystem.getShaderColor().clone();
                    RenderSystem.setShaderColor(BoneBowGlintRules.RED, BoneBowGlintRules.GREEN, BoneBowGlintRules.BLUE, 1F);
                    EQUAL_DEPTH_TEST.setupRenderState(); COLOR_WRITE.setupRenderState(); NO_CULL.setupRenderState();
                }, () -> {
                    NO_CULL.clearRenderState(); COLOR_WRITE.clearRenderState(); EQUAL_DEPTH_TEST.clearRenderState();
                    blend.clearRenderState(); texture.clearRenderState(); shader.clearRenderState();
                    float[] colour = oldColour[0];
                    RenderSystem.setShaderColor(colour[0], colour[1], colour[2], colour[3]);
                }) {};
    }

    public static Draw buffers(PoseStack pose) {
        return new Draw(pose);
    }

    public static final class Draw implements AutoCloseable {
        private final com.mojang.blaze3d.vertex.ByteBufferBuilder base = new com.mojang.blaze3d.vertex.ByteBufferBuilder(4096);
        private final java.util.LinkedHashMap<RenderType, com.mojang.blaze3d.vertex.ByteBufferBuilder> fixed = new java.util.LinkedHashMap<>();
        private final MultiBufferSource.BufferSource original;
        private final MultiBufferSource source;
        private Draw(PoseStack pose) {
            fixed.put(FIRST, new com.mojang.blaze3d.vertex.ByteBufferBuilder(4096));
            fixed.put(SECOND, new com.mojang.blaze3d.vertex.ByteBufferBuilder(4096));
            original = MultiBufferSource.immediateWithBuffers(fixed, base);
        // ItemRenderer translates generated geometry by -.5 after this entry point.
        Matrix4f inverse = new Matrix4f(pose.last().pose()).invert();
        long time = net.minecraft.Util.getMillis();
        source = type -> {
            if (type != RenderType.glint()
                    && type != RenderType.entityGlint() && type != RenderType.entityGlintDirect()
                    && type != RenderType.glintTranslucent()) return original.getBuffer(type);
            return VertexMultiConsumer.create(new GlintVertex(original.getBuffer(FIRST), inverse, time, 0),
                    new GlintVertex(original.getBuffer(SECOND), inverse, time, 1));
        };
        }
        public MultiBufferSource source() { return source; }
        @Override public void close() {
            try { original.endBatch(); }
            finally { fixed.values().forEach(com.mojang.blaze3d.vertex.ByteBufferBuilder::close); base.close(); }
        }
    }

    private record GlintVertex(VertexConsumer out, Matrix4f inverse, long time, int pass) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            Vector3f local = inverse.transformPosition(new Vector3f(x, y, z));
            // Official 1.7.10 bly.a(Tessellator,...): x=0 -> u=0, y=0 -> v=1.
            float[] uv = BoneBowGlintRules.uv(local.x + .5F, .5F - local.y, time, pass);
            out.addVertex(x, y, z).setColor(0xffffffff).setUv(uv[0], uv[1]);
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}
