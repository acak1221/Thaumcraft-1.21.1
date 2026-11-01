package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import thaumcraft.client.renderers.models.ModelEssentiaReservoir;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.lib.essentia.ReservoirRules;
import thaumcraft.common.tiles.TileEssentiaReservoir;

/**
 * Original TileEssentiaReservoirRenderer:61-90: the stored essentia as a coloured volume inside the tank. The box is the inner
 * three sixteenths to thirteen sixteenths of the block and grows ten sixteenths of the block with the fill level units/256; its colour
 * is the aspect the reservoir shows, which the tile fades to over twenty ticks, drawn at 0.9 alpha with the jar's liquid icon and the
 * original's fixed lightmap coordinate 200. The translucent glass is the block model; the original reservoir OBJ is drawn around it.
 */
public final class TileEssentiaReservoirRenderer implements BlockEntityRenderer<TileEssentiaReservoir> {
    /** Original BlockJar.iconLiquid: the animated glow the jar and the reservoir draw their contents with. */
    private static final ResourceLocation LIQUID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/animatedglow");
    /** Original Tessellator.setColor(cr, cg, cb, 0.9f). */
    private static final int ALPHA = Math.round(0.9F * 255);

    private static final RenderType FRAME = new FrameType();
    private static ModelEssentiaReservoir itemModel;
    private final ModelEssentiaReservoir model;

    public TileEssentiaReservoirRenderer(BlockEntityRendererProvider.Context context) {
        // The dispatcher reconstructs this renderer on reload; the inventory shares that fresh OBJ.
        model = new ModelEssentiaReservoir(Minecraft.getInstance().getResourceManager());
        itemModel = model;
    }

    public static void renderInventoryFrame(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderFrame(java.util.Objects.requireNonNull(itemModel, "Reservoir renderer not registered"), Direction.DOWN, pose, buffers, light, overlay);
    }

    private static void renderFrame(ModelEssentiaReservoir model, Direction facing, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        try {
            framePose(pose, facing);
            model.render(pose, buffers.getBuffer(FRAME), light, overlay);
        } finally { pose.popPose(); }
    }

    static void framePose(PoseStack pose, Direction facing) {
        pose.translate(.5, .5, .5);
        var rotation = TileFluxScrubberRenderer.orientationOf(facing);
        switch (rotation.axis()) {
            case X -> pose.mulPose(Axis.XP.rotationDegrees(rotation.degrees()));
            case Y -> pose.mulPose(Axis.YP.rotationDegrees(rotation.degrees()));
            case NONE -> { }
        }
        pose.translate(0, 0, -.5);
    }

    private static final class FrameType extends RenderType {
        private static final TextureStateShard TEXTURE = new TextureStateShard(
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/reservoir.png"), false, false);
        private FrameType() {
            super("thaumcraft_reservoir_frame", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES,
                    16384, false, false, FrameType::begin, FrameType::end);
        }
        private static void begin() {
            RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.setupRenderState(); TEXTURE.setupRenderState();
            NO_TRANSPARENCY.setupRenderState(); NO_CULL.setupRenderState(); LIGHTMAP.setupRenderState(); OVERLAY.setupRenderState();
            LEQUAL_DEPTH_TEST.setupRenderState(); COLOR_DEPTH_WRITE.setupRenderState();
        }
        private static void end() {
            COLOR_DEPTH_WRITE.clearRenderState(); LEQUAL_DEPTH_TEST.clearRenderState(); OVERLAY.clearRenderState(); LIGHTMAP.clearRenderState();
            NO_CULL.clearRenderState(); NO_TRANSPARENCY.clearRenderState(); TEXTURE.clearRenderState(); RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER.clearRenderState();
        }
    }

    @Override public void render(TileEssentiaReservoir tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderFrame(model, tile.getFacing(), pose, buffers, light, overlay);
        int units = tile.getAmount();
        if (units <= 0 || tile.getDisplayAspect() == null) return;
        float x0 = ReservoirRules.LIQUID_EDGE, x1 = ReservoirRules.LIQUID_FAR;
        float bottom = ReservoirRules.LIQUID_EDGE, top = ReservoirRules.liquidTop(units);
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(LIQUID);
        var buffer = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        int red = channel(tile.getRed()), green = channel(tile.getGreen()), blue = channel(tile.getBlue());
        // Original Tessellator.setBrightness(200): the contents are drawn at a fixed lightmap coordinate, not at the world's light.
        // 200 of 255 is the lightmap's 0.784 brightness, which native block light 14 reproduces.
        int lit = LightTexture.pack(14, 0);
        face(pose, buffer, sprite, red, green, blue, lit, overlay, 0, 1, 0, x0, top, x0, x0, top, x1, x1, top, x1, x1, top, x0);
        face(pose, buffer, sprite, red, green, blue, lit, overlay, 0, -1, 0, x0, bottom, x1, x0, bottom, x0, x1, bottom, x0, x1, bottom, x1);
        face(pose, buffer, sprite, red, green, blue, lit, overlay, 0, 0, -1, x1, top, x0, x1, bottom, x0, x0, bottom, x0, x0, top, x0);
        face(pose, buffer, sprite, red, green, blue, lit, overlay, 0, 0, 1, x0, top, x1, x0, bottom, x1, x1, bottom, x1, x1, top, x1);
        face(pose, buffer, sprite, red, green, blue, lit, overlay, -1, 0, 0, x0, top, x0, x0, bottom, x0, x0, bottom, x1, x0, top, x1);
        face(pose, buffer, sprite, red, green, blue, lit, overlay, 1, 0, 0, x1, top, x1, x1, bottom, x1, x1, bottom, x0, x1, top, x0);
    }

    private static int channel(float value) { return Math.round(Math.min(1F, Math.max(0F, value)) * 255); }

    /** The original RenderBlocks face mapping: the side V follows the height, the U follows the face's own axis. */
    private static void face(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite sprite, int red, int green, int blue, int lit, int overlay,
                             float nx, float ny, float nz, float... xyz) {
        for (int i = 0; i < 4; i++) {
            float x = xyz[i * 3], y = xyz[i * 3 + 1], z = xyz[i * 3 + 2];
            float u = nx == 0 ? (nz < 0 ? 1 - x : x) : nx < 0 ? z : 1 - z;
            float v = ny != 0 ? z : 1 - y;
            buffer.addVertex(pose.last(), x, y, z).setColor(red, green, blue, ALPHA)
                    .setUv(sprite.getU(u), sprite.getV(v)).setOverlay(overlay).setLight(lit).setNormal(pose.last(), nx, ny, nz);
        }
    }
}
