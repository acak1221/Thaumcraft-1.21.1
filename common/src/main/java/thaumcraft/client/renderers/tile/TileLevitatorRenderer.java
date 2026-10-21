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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.tiles.TileLevitator;

/**
 * Original BlockLifterRenderer:66-92: the glowing faces of the levitator.
 *
 * <p>The original draws the block with its own model and adds the glowing
 * faces of the original {@code animatedglow} sheet: the top face in the
 * original colour {@code BlockCustomOreItem.colors[4]} and the four side faces
 * in the original colour {@code 0xDD11FF}. The top face is only drawn when the
 * block above is air, every side face only when its neighbour is air. The
 * brightness of the top face is the fixed lightmap coordinate 180 while the
 * levitator is not powered, and the light of the block otherwise.
 *
 * <p>The original draws the top face one hundredth of a block below the top of
 * the levitator and the side faces one hundredth of a block outside of it.
 */
public final class TileLevitatorRenderer implements BlockEntityRenderer<TileLevitator> {
    /** Original BlockLifter:56 - the sheet of the glow. */
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/animatedglow");
    /** Original BlockCustomOreItem:17 - the colour of the top face, 0xA000A000. */
    private static final int TOP_COLOUR = 0xA00000;
    /** Original BlockLifterRenderer:83 - the colour of the side faces, 0xDD11FF. */
    private static final int SIDE_COLOUR = 0x00DD11;
    /** Original BlockLifterRenderer:82 - the lightmap coordinate of the active top face. */
    private static final int ACTIVE_LIGHT = 180;

    public TileLevitatorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileLevitator tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = tile.getLevel();
        if (level == null) return;
        var pos = tile.getBlockPos();
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(GLOW);
        var out = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        // Original BlockLifterRenderer:78-82.
        if (isAir(level, pos.above())) {
            int lit = tile.gettingPower() ? light : LightTexture.pack(ACTIVE_LIGHT % 16, (ACTIVE_LIGHT / 16) % 16);
            face(pose, out, sprite, lit, overlay, Direction.UP, 0.0F, 0.99F, 0.0F);
        }
        // Original BlockLifterRenderer:83-91.
        if (isAir(level, pos.east())) {
            face(pose, out, sprite, light, overlay, Direction.WEST, -0.01F, 0.0F, 0.0F);
        }
        if (isAir(level, pos.west())) {
            face(pose, out, sprite, light, overlay, Direction.EAST, 1.01F, 0.0F, 0.0F);
        }
        if (isAir(level, pos.south())) {
            face(pose, out, sprite, light, overlay, Direction.NORTH, 0.0F, 0.0F, -0.01F);
        }
        if (isAir(level, pos.north())) {
            face(pose, out, sprite, light, overlay, Direction.SOUTH, 0.0F, 0.0F, 1.01F);
        }
    }

    /**
     * Original Block.isAirBlock: the block is air when it is air or when it is
     * not a solid cube.
     */
    private static boolean isAir(net.minecraft.world.level.Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || !state.isSolidRender(level, pos);
    }

    /**
     * One glow face of the original.
     *
     * @param direction the face that is drawn
     * @param x         the offset of the face on the x axis
     * @param y         the height of the face
     * @param z         the offset of the face on the z axis
     */
    private static void face(PoseStack pose, VertexConsumer out, TextureAtlasSprite sprite, int light, int overlay,
                             Direction direction, float x, float y, float z) {
        int colour = direction == Direction.UP ? TOP_COLOUR : SIDE_COLOUR;
        int red = (colour >> 16) & 255, green = (colour >> 8) & 255, blue = colour & 255;
        float[] points;
        float[] uv;
        switch (direction) {
            case UP -> {
                points = new float[]{0.0F, y, 0.0F, 1.0F, y, 0.0F, 1.0F, y, 1.0F, 0.0F, y, 1.0F};
                uv = new float[]{0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F};
            }
            case WEST -> {
                points = new float[]{x, 0.0F, 0.0F, x, 1.0F, 0.0F, x, 1.0F, 1.0F, x, 0.0F, 1.0F};
                uv = new float[]{0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F};
            }
            case EAST -> {
                points = new float[]{x, 0.0F, 1.0F, x, 1.0F, 1.0F, x, 1.0F, 0.0F, x, 0.0F, 0.0F};
                uv = new float[]{0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F};
            }
            case NORTH -> {
                points = new float[]{1.0F, 0.0F, z, 1.0F, 1.0F, z, 0.0F, 1.0F, z, 0.0F, 0.0F, z};
                uv = new float[]{0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F};
            }
            default -> {
                points = new float[]{0.0F, 0.0F, z, 0.0F, 1.0F, z, 1.0F, 1.0F, z, 1.0F, 0.0F, z};
                uv = new float[]{0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F};
            }
        }
        for (int index = 0; index < 4; index++) {
            out.addVertex(pose.last(), points[index * 3], points[index * 3 + 1], points[index * 3 + 2])
                    .setColor(red, green, blue, 255)
                    .setUv(uv[index * 2], uv[index * 2 + 1]).setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), direction.getStepX(), direction.getStepY(), direction.getStepZ());
        }
    }
}
