package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
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
 * original's fixed lightmap coordinate 200. The vessel itself is the block model.
 */
public final class TileEssentiaReservoirRenderer implements BlockEntityRenderer<TileEssentiaReservoir> {
    /** Original BlockJar.iconLiquid: the animated glow the jar and the reservoir draw their contents with. */
    private static final ResourceLocation LIQUID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/animatedglow");
    /** Original Tessellator.setColor(cr, cg, cb, 0.9f). */
    private static final int ALPHA = Math.round(0.9F * 255);

    public TileEssentiaReservoirRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileEssentiaReservoir tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
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
