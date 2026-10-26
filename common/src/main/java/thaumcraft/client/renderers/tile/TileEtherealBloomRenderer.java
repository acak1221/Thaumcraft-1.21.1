package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.ModelCube;
import thaumcraft.common.tiles.TileEtherealBloom;

/**
 * Original TileEtherealBloomRenderer: the ethereal bloom.
 *
 * <p>The original grows the bloom with the growth counter of the tile: four
 * leaf quads, four petal quads, four stalk quads, the crystal capacitor cube
 * in the middle and the light strip that shines out of the plant.
 *
 * <p>The light strip of the original (oracle
 * {@code thaumcraft/client/renderers/tile/TileEtherealBloomRenderer.java:74-79}) is the 1.7 fixed
 * function helper {@code UtilsFX.renderFacingStrip(x + 0.5, y + scale1, z + 0.5, 0, scale1, 1.0, 32, 6,
 * counter % 32, partialTick, 0xAADDFF)}: a camera facing square of the half size {@code scale1} that
 * shows the cell {@code (counter % 32, 6)} of the thirty two by thirty two grid of the
 * {@code nodes.png} texture and that is drawn with the additive blend {@code (770, 1)}.
 */
public final class TileEtherealBloomRenderer implements BlockEntityRenderer<TileEtherealBloom> {
    /** The original texture of the crystal capacitor in the middle of the bloom. */
    public static final ResourceLocation CAPACITOR = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/crystalcapacitor.png");
    /** The original texture of the light strip. */
    public static final ResourceLocation NODES = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/misc/nodes.png");
    /** The original number of cells of the nodes texture. */
    public static final int STRIP_CELLS = 32;
    /** The original row of the nodes texture the light strip shows. */
    public static final int STRIP_ROW = 6;

    private static final RenderType STRIP = new StripRenderType();
    private static final RenderType CAPACITOR_TYPE = new CapacitorRenderType();

    private final ModelCube cube = new ModelCube(0);

    public TileEtherealBloomRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileEtherealBloom tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float growth = tile.growthCounter + partialTick;
        float petals = Math.min(growth, 100.0F);
        float leaves = Math.min(growth, 50.0F);
        float stem = Math.max(0.0F, Math.min(growth - 33.0F, 33.0F));
        float core = Math.max(0.0F, Math.min(growth - 66.0F, 33.0F));
        float scalePetals = petals / 100.0F;
        float scaleLeaves = leaves / 60.0F + .1666666F;
        float scaleStem = stem / 33.0F;
        float scaleCore = core / 33.0F * .7F;
        // The light strip of the original: the camera facing square of the half size scale1 that shows
        // the cell (counter % 32, 6) of the nodes texture, drawn with the additive blend (770, 1).
        pose.pushPose();
        try {
            pose.translate(.5F, scalePetals, .5F);
            pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            int frame = tile.counter % STRIP_CELLS;
            float u0 = frame / (float) STRIP_CELLS;
            float u1 = (frame + 1F) / STRIP_CELLS;
            float v0 = STRIP_ROW / (float) STRIP_CELLS;
            float v1 = (STRIP_ROW + 1F) / STRIP_CELLS;
            var strip = buffers.getBuffer(STRIP);
            // The original vertex order of UtilsFX.renderFacingStrip: the bottom left corner of the
            // camera square carries the right edge of the cell, the top right one its left edge.
            vertex(strip, pose, -scalePetals, -scalePetals, u1, v1);
            vertex(strip, pose, -scalePetals, scalePetals, u1, v0);
            vertex(strip, pose, scalePetals, scalePetals, u0, v0);
            vertex(strip, pose, scalePetals, -scalePetals, u0, v1);
        } finally {
            pose.popPose();
        }
        // The crystal capacitor in the middle of the bloom, still under the additive blend of the strip.
        pose.pushPose();
        try {
            pose.translate(.5F - scaleCore / 8.0F, scalePetals - scaleCore / 6.0F, .5F - scaleCore / 8.0F);
            pose.scale(scaleCore / 4.0F, scaleCore / 3.0F, scaleCore / 4.0F);
            cube.render(pose, buffers.getBuffer(CAPACITOR_TYPE), light, overlay, 0xffffffff);
        } finally {
            pose.popPose();
        }
        var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite leavesSprite = atlas.apply(ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/purifier_leaves"));
        TextureAtlasSprite stalkSprite = atlas.apply(ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/purifier_stalk"));
        var leafBuffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        // The four leaves of the bloom.
        pose.pushPose();
        try {
            pose.translate(.5F, .25F, .5F);
            pose.mulPose(Axis.XP.rotationDegrees(180.0F));
            for (int index = 0; index < 4; index++) {
                pose.pushPose();
                try {
                    pose.scale(scaleStem, scalePetals, scaleStem);
                    pose.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
                    sprite(pose, leafBuffer, leavesSprite, light, overlay);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
        // The four petals of the bloom.
        pose.pushPose();
        try {
            pose.translate(.5F, .6F, .5F);
            pose.mulPose(Axis.YP.rotationDegrees(45.0F));
            pose.mulPose(Axis.XP.rotationDegrees(180.0F));
            for (int index = 0; index < 4; index++) {
                pose.pushPose();
                try {
                    pose.scale(scaleCore, scalePetals * .7F, scaleCore);
                    pose.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
                    sprite(pose, leafBuffer, leavesSprite, light, overlay);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
        // The four stalks of the bloom.
        pose.pushPose();
        try {
            pose.translate(.5F, .5F, .5F);
            pose.mulPose(Axis.XP.rotationDegrees(180.0F));
            for (int index = 0; index < 4; index++) {
                pose.pushPose();
                try {
                    pose.translate(0.0F, (1.0F - scalePetals) / 2.0F, 0.0F);
                    pose.scale(scaleLeaves, scalePetals, scaleLeaves);
                    pose.mulPose(Axis.YP.rotationDegrees(index * 90.0F));
                    sprite(pose, leafBuffer, stalkSprite, light, overlay);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            pose.popPose();
        }
    }

    /** A centred quad of the size of one block. */
    private static void sprite(PoseStack pose, VertexConsumer out, TextureAtlasSprite sprite, int light, int overlay) {
        float[][] corners = {{-.5F, .5F}, {.5F, .5F}, {.5F, -.5F}, {-.5F, -.5F}};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        for (int index = 0; index < 4; index++) {
            out.addVertex(pose.last(), corners[index][0], corners[index][1], 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(sprite.getU(uv[index][0]), sprite.getV(uv[index][1]))
                    .setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
        }
    }

    /**
     * A vertex of the light strip of the original: the colour is the original {@code 0xAADDFF} with the
     * original alpha of one, and the texture cell is the one the original scroll frame asks for.
     */
    private static void vertex(VertexConsumer out, PoseStack pose, float x, float y, float u, float v) {
        out.addVertex(pose.last(), x, y, 0.0F)
                .setColor(0xAA, 0xDD, 0xFF, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
    }

    /**
     * The crystal capacitor of the original (bytecode 141-159, 237-244, 315-326): the blend
     * {@code (770, 1)} the strip enabled is still on, culling and depth writes are back on. Every
     * opaque texel of {@code crystalcapacitor.png} has the alpha 206, so the cube glows through.
     */
    private static final class CapacitorRenderType extends RenderType {
        private static final TextureStateShard TEXTURE = new TextureStateShard(CAPACITOR, false, false);

        private CapacitorRenderType() {
            super("thaumcraft_bloom_capacitor", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    1536, false, true, CapacitorRenderType::begin, CapacitorRenderType::end);
        }

        private static void begin() {
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.setupRenderState();
            TEXTURE.setupRenderState();
            LIGHTNING_TRANSPARENCY.setupRenderState();
            LIGHTMAP.setupRenderState();
            OVERLAY.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            COLOR_DEPTH_WRITE.setupRenderState();
        }

        private static void end() {
            COLOR_DEPTH_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            OVERLAY.clearRenderState();
            LIGHTMAP.clearRenderState();
            LIGHTNING_TRANSPARENCY.clearRenderState();
            TEXTURE.clearRenderState();
            RENDERTYPE_ENTITY_TRANSLUCENT_SHADER.clearRenderState();
        }
    }

    /**
     * The light strip of the original: the additive blend {@code (770, 1)}, no culling, no depth write.
     */
    private static final class StripRenderType extends RenderType {
        private static final TextureStateShard NODES_TEXTURE = new TextureStateShard(NODES, false, false);

        private StripRenderType() {
            super("thaumcraft_bloom_strip", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                    4096, false, true, StripRenderType::begin, StripRenderType::end);
        }

        private static void begin() {
            RENDERTYPE_EYES_SHADER.setupRenderState();
            NODES_TEXTURE.setupRenderState();
            LIGHTNING_TRANSPARENCY.setupRenderState(); // SRC_ALPHA / ONE, the original blend (770, 1).
            NO_CULL.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState();
            COLOR_WRITE.setupRenderState();
        }

        private static void end() {
            COLOR_WRITE.clearRenderState();
            LEQUAL_DEPTH_TEST.clearRenderState();
            NO_CULL.clearRenderState();
            LIGHTNING_TRANSPARENCY.clearRenderState();
            NODES_TEXTURE.clearRenderState();
            RENDERTYPE_EYES_SHADER.clearRenderState();
        }
    }
}
