package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.client.fx.InfusionClientEffects;
import thaumcraft.client.renderers.models.ModelCube;
import thaumcraft.common.tiles.TileInfusionMatrix;

/** Original eight cubes, lower-texture-half runes and seed-245 crafting rays. */
public final class TileInfusionMatrixRenderer implements BlockEntityRenderer<TileInfusionMatrix> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/infuser.png");
    private static final RenderType RUNES = new RuneRenderType();
    private static final RenderType HALO = new HaloRenderType();
    private final ModelCube cube = new ModelCube(0);
    private final ModelCube runes = new ModelCube(32);

    public TileInfusionMatrixRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileInfusionMatrix tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var player = Minecraft.getInstance().player;
        if (player == null || tile.getLevel() == null) return;
        float ticks = player.tickCount + partialTick;
        var animation = InfusionClientEffects.animation(tile, partialTick);
        float startup = animation.startup();
        float instability = Math.min(6, 1 + tile.getInstability() * .66F * (Math.min(animation.craftCount(), 50) / 50));
        pose.pushPose();
        try {
            pose.translate(.5, .5, .5);
            pose.mulPose(Axis.YP.rotationDegrees(ticks % 360 * startup));
            pose.mulPose(Axis.XP.rotationDegrees(35 * startup));
            pose.mulPose(Axis.ZP.rotationDegrees(45 * startup));
            // TC4 TileRunicMatrixRenderer:103-175. One native unit cube at each
            // (+/- .25)^3 corner, scaled .45, with the same per-axis phase offsets.
            for (int a = 0; a < 2; a++) {
                for (int b = 0; b < 2; b++) {
                    for (int c = 0; c < 2; c++) {
                        pose.pushPose();
                        try {
                            float x = (a == 0 ? -.25F : .25F), y = (b == 0 ? -.25F : .25F), z = (c == 0 ? -.25F : .25F);
                            if (tile.isActive()) {
                                x += wobble(ticks, a, 15, instability, startup);
                                y += wobble(ticks, b, 14, instability, startup);
                                z += wobble(ticks, c, 13, instability, startup);
                            }
                            pose.translate(x, y, z);
                            if (a > 0) pose.mulPose(Axis.XP.rotationDegrees(90));
                            if (b > 0) pose.mulPose(Axis.YP.rotationDegrees(90));
                            if (c > 0) pose.mulPose(Axis.ZP.rotationDegrees(90));
                            pose.scale(.45F, .45F, .45F);
                            cube.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay, 0xffffffff);
                            if (tile.isActive()) {
                                float alpha = (Mth.sin((ticks + a * 2 + b * 3 + c * 4) / 4) * .1F + .2F) * startup;
                                int argb = Mth.clamp((int)(alpha * 255), 0, 255) << 24 | 0xcc19ff;
                                runes.render(pose, buffers.getBuffer(RUNES), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argb);
                            }
                        } finally {
                            pose.popPose();
                        }
                    }
                }
            }
        } finally {
            pose.popPose();
        }
        if (tile.isCrafting() && animation.craftCount() > 0) halo(pose, buffers.getBuffer(HALO), animation.craftCount());
    }

    private static float wobble(float ticks, int corner, float divisor, float instability, float startup) {
        return Mth.sin((ticks + corner * 10) / (divisor - instability / 2)) * .01F * startup * instability;
    }

    private static void halo(PoseStack pose, VertexConsumer buffer, float count) {
        // Source renderer:46-94: untextured, depth-tested non-terrain rays, no
        // depth writes, white opaque origin -> transparent purple tips. Rebuild
        // the deterministic cumulative rotations rather than randomising frames.
        Random random = new Random(245L);
        int rays = Minecraft.useFancyGraphics() ? 20 : 10;
        float ramp = Math.min(count, 50) / 50;
        pose.pushPose();
        try {
            pose.translate(.5, .5, .5);
            for (int i = 0; i < rays; i++) {
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360));
                pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360));
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360));
                pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360 + count / 500 * 360));
                float length = (random.nextFloat() * 20 + 5) * ramp / 20;
                float width = (random.nextFloat() * 2 + 1) * ramp / 20;
                // Expand the original 5-vertex TRIANGLE_FAN into three genuine
                // triangles. Never submit fan/triangle vertices to a QUADS buffer.
                rayTriangle(pose, buffer, -.866F * width, -.5F * width, .866F * width, -.5F * width, length);
                rayTriangle(pose, buffer, .866F * width, -.5F * width, 0, width, length);
                rayTriangle(pose, buffer, 0, width, -.866F * width, -.5F * width, length);
            }
        } finally {
            pose.popPose();
        }
    }

    private static void rayTriangle(PoseStack pose, VertexConsumer buffer, float x1, float z1, float x2, float z2, float y) {
        buffer.addVertex(pose.last(), 0, 0, 0).setColor(255, 255, 255, 255);
        buffer.addVertex(pose.last(), x1, y, z1).setColor(204, 0, 255, 0);
        buffer.addVertex(pose.last(), x2, y, z2).setColor(204, 0, 255, 0);
    }

    @Override public boolean shouldRenderOffScreen(TileInfusionMatrix tile) { return true; }
    @Override public int getViewDistance() { return 64; }

    private static final class RuneRenderType extends RenderType {
        private static final TextureStateShard INFUSER_TEXTURE = new TextureStateShard(TEXTURE, false, false);

        private RuneRenderType() {
            super("thaumcraft_infuser_runes", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    4096, false, true, RuneRenderType::begin, RuneRenderType::end);
        }

        private static void begin() {
            // Native eyes shader is emissive AND keeps the low startup alpha;
            // the ordinary cutout shader would discard the pulsing overlay.
            RENDERTYPE_EYES_SHADER.setupRenderState();
            INFUSER_TEXTURE.setupRenderState();
            LIGHTNING_TRANSPARENCY.setupRenderState(); // SRC_ALPHA / ONE, not eyes()' ONE / ONE.
            NO_CULL.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            COLOR_WRITE.setupRenderState();
        }

        private static void end() {
            COLOR_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            NO_CULL.clearRenderState();
            LIGHTNING_TRANSPARENCY.clearRenderState();
            INFUSER_TEXTURE.clearRenderState();
            RENDERTYPE_EYES_SHADER.clearRenderState();
        }
    }

    private static final class HaloRenderType extends RenderType {
        private HaloRenderType() {
            super("thaumcraft_infuser_halo", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES,
                    4096, false, false, HaloRenderType::begin, HaloRenderType::end);
        }

        private static void begin() {
            RENDERTYPE_LIGHTNING_SHADER.setupRenderState();
            NO_TEXTURE.setupRenderState();
            LIGHTNING_TRANSPARENCY.setupRenderState();
            CULL.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            COLOR_WRITE.setupRenderState();
        }

        private static void end() {
            COLOR_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            CULL.clearRenderState();
            LIGHTNING_TRANSPARENCY.clearRenderState();
            NO_TEXTURE.clearRenderState();
            RENDERTYPE_LIGHTNING_SHADER.clearRenderState();
        }
    }
}
