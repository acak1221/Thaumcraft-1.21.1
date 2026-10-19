package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.TileJar;

/** Real stored essentia, original TileJarRenderer:120-151 bounds/height/animatedglow. */
public final class TileJarRenderer implements BlockEntityRenderer<TileJar> {
    private static final ResourceLocation LIQUID = ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/animatedglow");
    private static final ResourceLocation LABEL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/label.png");
    public TileJarRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileJar tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderContents(tile.getAspect(), tile.getAmount(), tile.getMaxAmount(), tile.getFilter(), tile.getFacing(), tile.getBlockPos().getX(),
                pose, buffers, light, overlay);
    }

    /** Shared immutable view: item rendering needs neither a fake tile nor a world/gameplay setter. */
    public static void renderContents(Aspect aspect, int amount, int capacity, Aspect filter, Direction facing, int blockX,
                                      PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (filter != null) {
            pose.pushPose();
            pose.translate(.5, .4, .5);
            float angle = switch (facing) { case SOUTH -> 180; case EAST -> -90; case WEST -> 90; default -> 0; };
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            // Config.crooked defaults true. Keep the original signed Java %
            // result; a future config toggle must not invent a new random roll.
            float crooked = (filter.getTag().hashCode() + blockX + facing.get3DDataValue()) % 4 - 2;
            label(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(LABEL)), .25F, -.315F, crooked, 0xffffff, light, overlay);
            label(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(filter.getImage())), .168F, -.316F, crooked,
                    filter.getColor(), light, overlay);
            pose.popPose();
        }
        if (aspect == null || amount <= 0 || capacity <= 0 || amount > capacity) return;
        float bottom = .0625F, top = bottom + amount / (float) capacity * .625F;
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(LIQUID);
        var buffer = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        int color = aspect.getColor(), brightness = Math.max(200, light);
        // Shell remains the separate native translucent block model. Draw all six liquid
        // faces, not a floating aspect icon or a blockstate pretending to store it.
        face(pose, buffer, sprite, color, brightness, overlay, 0, 1, 0,
                .25F, top, .25F, .25F, top, .75F, .75F, top, .75F, .75F, top, .25F);
        face(pose, buffer, sprite, color, brightness, overlay, 0, -1, 0,
                .25F, bottom, .75F, .25F, bottom, .25F, .75F, bottom, .25F, .75F, bottom, .75F);
        face(pose, buffer, sprite, color, brightness, overlay, 0, 0, -1,
                .75F, top, .25F, .75F, bottom, .25F, .25F, bottom, .25F, .25F, top, .25F);
        face(pose, buffer, sprite, color, brightness, overlay, 0, 0, 1,
                .25F, top, .75F, .25F, bottom, .75F, .75F, bottom, .75F, .75F, top, .75F);
        face(pose, buffer, sprite, color, brightness, overlay, -1, 0, 0,
                .25F, top, .25F, .25F, bottom, .25F, .25F, bottom, .75F, .25F, top, .75F);
        face(pose, buffer, sprite, color, brightness, overlay, 1, 0, 0,
                .75F, top, .75F, .75F, bottom, .75F, .75F, bottom, .25F, .75F, top, .25F);
    }

    private static void label(PoseStack pose, VertexConsumer buffer, float half, float z, float crooked, int color, int light, int overlay) {
        pose.pushPose();
        pose.translate(0, 0, z);
        // Parent X180 in the original flips the apparent Z rotation.
        pose.mulPose(Axis.ZP.rotationDegrees(-crooked));
        float[] xy = {-half, half, half, half, half, -half, -half, -half};
        for (int i = 0; i < 4; i++) buffer.addVertex(pose.last(), xy[i * 2], xy[i * 2 + 1], 0)
                .setColor(color >> 16 & 255, color >> 8 & 255, color & 255, 255)
                .setUv(i == 0 || i == 3 ? 0 : 1, i < 2 ? 0 : 1).setOverlay(overlay).setLight(light).setNormal(pose.last(), 0, 0, -1);
        pose.popPose();
    }

    private static void face(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int light, int overlay,
                             float nx, float ny, float nz, float... xyz) {
        for (int i = 0; i < 4; i++) {
            float x = xyz[i * 3], y = xyz[i * 3 + 1], z = xyz[i * 3 + 2];
            // Original RenderBlocks maps the clipped bounds into the sprite;
            // side V follows height, instead of stretching every fill level.
            float u = nx == 0 ? (nz < 0 ? 1 - x : x) : nx < 0 ? z : 1 - z;
            float v = ny != 0 ? z : 1 - y;
            buffer.addVertex(pose.last(), x, y, z).setColor(color >> 16 & 255, color >> 8 & 255, color & 255, 255)
                    .setUv(sprite.getU(u), sprite.getV(v)).setOverlay(overlay).setLight(light).setNormal(pose.last(), nx, ny, nz);
        }
    }
}
