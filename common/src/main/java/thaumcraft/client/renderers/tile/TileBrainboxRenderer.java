package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.tiles.TileBrainbox;

/**
 * Original brainbox of the {@code BlockMetalDevice} renderer: the brain that
 * sticks out of the box on the side it looks at.
 *
 * <p>The original draws a small cube of the brain box texture that grows out
 * of the metal device towards the machine it feeds. The inset box itself is the
 * block model, this renderer only draws the part that depends on the facing of
 * the tile entity.
 */
public final class TileBrainboxRenderer implements BlockEntityRenderer<TileBrainbox> {
    /** The original size of the protrusion, in sixteenths of a block. */
    private static final float MIN = 6.0F / 16.0F;
    private static final float MAX = 10.0F / 16.0F;

    public TileBrainboxRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileBrainbox tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = tile.getFacing();
        if (facing == null) return;
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/brainbox"));
        var out = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        pose.pushPose();
        try {
            switch (facing) {
                case UP -> cube(pose, out, sprite, MIN, 13.0F / 16.0F, MIN, MAX, 1.0F, MAX, light, overlay);
                case DOWN -> cube(pose, out, sprite, MIN, 0.0F, MIN, MAX, 3.0F / 16.0F, MAX, light, overlay);
                case EAST -> cube(pose, out, sprite, 13.0F / 16.0F, MIN, MIN, 1.0F, MAX, MAX, light, overlay);
                case WEST -> cube(pose, out, sprite, 0.0F, MIN, MIN, 3.0F / 16.0F, MAX, MAX, light, overlay);
                case SOUTH -> cube(pose, out, sprite, MIN, MIN, 13.0F / 16.0F, MAX, MAX, 1.0F, light, overlay);
                default -> cube(pose, out, sprite, MIN, MIN, 0.0F, MAX, MAX, 3.0F / 16.0F, light, overlay);
            }
        } finally {
            pose.popPose();
        }
    }

    /** The six faces of the brain box, drawn with the original texture. */
    private static void cube(PoseStack pose, VertexConsumer out,
                             net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                             float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                             int light, int overlay) {
        face(pose, out, sprite, minX, minY, minZ, maxX, minY, maxZ, 0, 0, 1, light, overlay);
        face(pose, out, sprite, minX, maxY, minZ, maxX, maxY, maxZ, 0, 1, 1, light, overlay);
        face(pose, out, sprite, minX, minY, minZ, maxX, maxY, minZ, 0, 0, -1, light, overlay);
        face(pose, out, sprite, minX, minY, maxZ, maxX, maxY, maxZ, 0, 0, 1, light, overlay);
        face(pose, out, sprite, minX, minY, minZ, minX, maxY, maxZ, -1, 0, 0, light, overlay);
        face(pose, out, sprite, maxX, minY, minZ, maxX, maxY, maxZ, 1, 0, 0, light, overlay);
    }

    private static void face(PoseStack pose, VertexConsumer out, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float nx, float ny, float nz, int light, int overlay) {
        float[][] corners = {{x1, y1}, {x2, y1}, {x2, y2}, {x1, y2}};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int index = 0; index < 4; index++) {
            float z = nz > 0 ? z2 : z1;
            out.addVertex(pose.last(), corners[index][0], corners[index][1], z)
                    .setColor(255, 255, 255, 255)
                    .setUv(sprite.getU(uv[index][0]), sprite.getV(uv[index][1]))
                    .setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), nx, ny, nz);
        }
    }
}
