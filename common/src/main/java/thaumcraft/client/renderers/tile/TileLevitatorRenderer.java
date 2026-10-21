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
 * Original BlockLifterRenderer:40-79, the glowing faces of the levitator.
 *
 * <p>The original draws the block with its own model and adds the glowing
 * faces of the original {@code animatedglow} sheet: the top face in the
 * original colour {@code BlockCustomOreItem.colors[4]} (0x00A000, a green) and
 * the four side faces in the original colour {@code 14488063} (0xDD11FF, a
 * magenta). The top face is only drawn when the block above is air, every side
 * face only when its neighbour is air. One brightness is used for all five
 * faces: the fixed lightmap coordinate 180 while the levitator is not powered
 * and the light of the block otherwise.
 *
 * <p>The original places every panel one hundredth of a block INSIDE its own
 * face (the top face at {@code y + 0.99}, the +X face at {@code x + 0.99}, the
 * -X face at {@code x + 0.01} and so on). The levitator of the original is not
 * a solid cube in the world - its own {@code isSideSolid} override keeps the
 * neighbouring faces away - so those recessed panels are what the player sees.
 * The 1.21.1 port keeps the opaque cube of the levitator, therefore the panels
 * are drawn one hundredth of a block OUTSIDE their face, otherwise the cube
 * would hide them completely. This is the only deliberate difference of this
 * renderer; the side, the colour, the brightness and the sheet are the ones of
 * the original.
 */
public final class TileLevitatorRenderer implements BlockEntityRenderer<TileLevitator> {
    /** Original BlockLifter:56 - the sheet of the glow. */
    public static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/animatedglow");
    /** Original BlockCustomOreItem:17 - the colour of the top face, 0x00A000. */
    public static final int TOP_COLOUR = 0x00A000;
    /** Original BlockLifterRenderer:58 - the colour of the side faces, 0xDD11FF. */
    public static final int SIDE_COLOUR = 0xDD11FF;
    /** Original BlockLifterRenderer:50 - the lightmap coordinate of an active levitator. */
    public static final int ACTIVE_LIGHT = 180;
    /** The one hundredth of a block of the original renderer. */
    public static final float OFFSET = 0.01F;

    /**
     * The face of the levitator that carries the panel of one neighbour.
     *
     * <p>Original BlockLifterRenderer:59-73 draws the +X face when the +X
     * neighbour is renderable, the -X face when the -X neighbour is renderable
     * and so on, so the panel always belongs to the side that is open.
     */
    public static Direction panelFace(Direction neighbour) {
        return neighbour;
    }

    /**
     * The position of the panel of one neighbour on the axis of the face.
     *
     * <p>The original puts the panel one hundredth of a block INSIDE the face
     * ({@code x + 0.99} for the +X face, {@code x + 0.01} for the -X face). The
     * port keeps the opaque cube of the levitator, therefore the panel is drawn
     * one hundredth of a block OUTSIDE its face, otherwise the cube would hide
     * it completely.
     */
    public static float panelOffset(Direction face) {
        return face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0F + OFFSET : -OFFSET;
    }

    public TileLevitatorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileLevitator tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = tile.getLevel();
        if (level == null) return;
        var pos = tile.getBlockPos();
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(GLOW);
        var out = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        // Original BlockLifterRenderer:48-53: one brightness for every face of the
        // levitator, the fixed lightmap coordinate 180 while the levitator is not
        // powered and the light of the block otherwise.
        int lit = tile.gettingPower() ? light : LightTexture.pack(ACTIVE_LIGHT % 16, (ACTIVE_LIGHT / 16) % 16);
        // Original BlockLifterRenderer:54-57, the top face.
        if (isAir(level, pos.above())) {
            face(pose, out, sprite, lit, overlay, Direction.UP, panelOffset(Direction.UP));
        }
        // Original BlockLifterRenderer:58-73, the four side faces. The original
        // draws the +X face when the +X neighbour is renderable, the -X face when
        // the -X neighbour is renderable and so on.
        if (isAir(level, pos.east())) {
            face(pose, out, sprite, lit, overlay, panelFace(Direction.EAST), panelOffset(Direction.EAST));
        }
        if (isAir(level, pos.west())) {
            face(pose, out, sprite, lit, overlay, panelFace(Direction.WEST), panelOffset(Direction.WEST));
        }
        if (isAir(level, pos.south())) {
            face(pose, out, sprite, lit, overlay, panelFace(Direction.SOUTH), panelOffset(Direction.SOUTH));
        }
        if (isAir(level, pos.north())) {
            face(pose, out, sprite, lit, overlay, panelFace(Direction.NORTH), panelOffset(Direction.NORTH));
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
     * @param offset    the position of the face on the axis of the direction
     */
    private static void face(PoseStack pose, VertexConsumer out, TextureAtlasSprite sprite, int light, int overlay,
                             Direction direction, float offset) {
        int colour = direction == Direction.UP ? TOP_COLOUR : SIDE_COLOUR;
        int red = (colour >> 16) & 255, green = (colour >> 8) & 255, blue = colour & 255;
        float[] points;
        float[] uv;
        // The cell of the sheet in the block atlas; the original draws the icon
        // of the glow, so the face takes that cell and not the whole atlas.
        float u0 = sprite.getU0(), v0 = sprite.getV0(), u1 = sprite.getU1(), v1 = sprite.getV1();
        switch (direction) {
            case UP -> {
                points = new float[]{0.0F, offset, 0.0F, 0.0F, offset, 1.0F, 1.0F, offset, 1.0F, 1.0F, offset, 0.0F};
                // The top face of the original runs the texture along x and z.
                uv = new float[]{u0, v0, u0, v1, u1, v1, u1, v0};
            }
            case EAST -> {
                points = new float[]{offset, 0.0F, 1.0F, offset, 0.0F, 0.0F, offset, 1.0F, 0.0F, offset, 1.0F, 1.0F};
                uv = new float[]{u0, v1, u1, v1, u1, v0, u0, v0};
            }
            case WEST -> {
                points = new float[]{offset, 0.0F, 0.0F, offset, 0.0F, 1.0F, offset, 1.0F, 1.0F, offset, 1.0F, 0.0F};
                uv = new float[]{u0, v1, u1, v1, u1, v0, u0, v0};
            }
            case SOUTH -> {
                points = new float[]{0.0F, 0.0F, offset, 1.0F, 0.0F, offset, 1.0F, 1.0F, offset, 0.0F, 1.0F, offset};
                uv = new float[]{u0, v1, u1, v1, u1, v0, u0, v0};
            }
            default -> {
                points = new float[]{1.0F, 0.0F, offset, 0.0F, 0.0F, offset, 0.0F, 1.0F, offset, 1.0F, 1.0F, offset};
                uv = new float[]{u0, v1, u1, v1, u1, v0, u0, v0};
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
