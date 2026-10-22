package thaumcraft.client.renderers.fluid;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import thaumcraft.common.blocks.BlockFluidPure;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.lib.fluid.PureFlowRules;

/**
 * The original Forge Classic render geometry of the TC4 purifying fluid.
 *
 * <p>The original 1.7.10 purifying fluid is a Forge {@code BlockFluidClassic}: a source cell is
 * seven eighths of a block high, a flowing cell is {@code quanta / 8} of that, the four corners
 * are averaged with the Forge rule that prefers a full corner over the average, the sides are
 * drawn with the shade of {@code 0.8} (north, south) and {@code 0.6} (west, east) and the bottom
 * with {@code 0.5}. The geometry lives here, in the shared module, so the two loaders draw the
 * very same fluid: the Fabric adapter of
 * {@code thaumcraft.fabric.client.FabricFluidPureRenderHandler} and the NeoForge adapter of
 * {@code thaumcraft.neoforge.mixin.client.NeoForgePureFluidMixin} both call
 * {@link #render}.</p>
 */
public final class PureFluidRender {
    private static final double OFFSET = (double) .001F;
    /** The original sheet of the purifying fluid. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "blocks/fluidpure");
    private static TextureAtlasSprite sprite;

    private PureFluidRender() {}

    /** The client resource reload of the shared sheet. */
    public static void reload(TextureAtlas atlas) {
        sprite = atlas.getSprite(TEXTURE);
    }

    /** The shared sheet of the purifying fluid. */
    public static TextureAtlasSprite sprite() {
        return sprite;
    }

    private static boolean pure(BlockState state) {
        return state.getBlock() instanceof BlockFluidPure;
    }

    /** The original height of one cell. */
    public static float height(BlockAndTintGetter level, BlockPos pos) {
        var state = level.getBlockState(pos);
        var above = level.getBlockState(pos.above());
        if (pure(state)) {
            if (!above.getFluidState().isEmpty() || above.getBlock() instanceof FluxFiniteBlock) return 1F;
            if (state.getValue(net.minecraft.world.level.block.LiquidBlock.LEVEL) == 0) return .875F;
        }
        if (!BlockFluidPure.legacySolid(state) && pure(above)) return 1F;
        return BlockFluidPure.quanta(level, pos) / 8F * .875F;
    }

    /** NW, SW, SE, NE, in precisely the original renderer's sampling order. */
    public static float[] corners(BlockAndTintGetter level, BlockPos pos) {
        float center = height(level, pos);
        if (center == 1F) return new float[]{1, 1, 1, 1};
        float nw = height(level, pos.offset(-1, 0, -1)), w = height(level, pos.west()), sw = height(level, pos.offset(-1, 0, 1));
        float n = height(level, pos.north()), s = height(level, pos.south()), ne = height(level, pos.offset(1, 0, -1)), e = height(level, pos.east()), se = height(level, pos.offset(1, 0, 1));
        return new float[]{PureFlowRules.renderAverage(nw, w, n, center), PureFlowRules.renderAverage(w, sw, s, center),
                PureFlowRules.renderAverage(s, e, se, center), PureFlowRules.renderAverage(n, ne, e, center)};
    }

    private static boolean visible(BlockAndTintGetter level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return !pure(state) && !state.isSolidRender(level, pos);
    }

    private static int light(BlockAndTintGetter level, BlockPos pos) {
        int here = LevelRenderer.getLightColor(level, pos), above = LevelRenderer.getLightColor(level, pos.above());
        return Math.max(here & 255, above & 255) | Math.max(here >> 16 & 255, above >> 16 & 255) << 16;
    }

    /**
     * Draws the original purifying fluid into the given consumer.
     *
     * @return false when the shared sheet is not loaded yet, so the caller can keep the
     *         vanilla fluid instead of drawing nothing
     */
    public static boolean render(BlockPos pos, BlockAndTintGetter level, VertexConsumer out, BlockState state, FluidState fluid) {
        if (!pure(state) || sprite == null) return false;
        var sampled = corners(level, pos);
        double[] corners = {sampled[0], sampled[1], sampled[2], sampled[3]};
        boolean top = !pure(level.getBlockState(pos.above()));
        // Native chunk meshes expect section-relative coordinates, including Y.
        double x = pos.getX() & 15, y = pos.getY() & 15, z = pos.getZ() & 15;
        if (top) {
            for (int i = 0; i < 4; i++) corners[i] -= OFFSET;
            var vector = fluid.getFlow(level, pos);
            float[][] uv = new float[4][2];
            if (vector.x == 0D && vector.z == 0D) {
                uv[0] = new float[]{0, 0};
                uv[1] = new float[]{0, 1};
                uv[2] = new float[]{1, 1};
                uv[3] = new float[]{1, 0};
            } else {
                float direction = (float) (Math.atan2(vector.z, vector.x) - Math.PI / 2), sx = Mth.sin(direction) * .25F, cz = Mth.cos(direction) * .25F;
                uv[0] = new float[]{(8 + (-cz - sx) * 16) / 16, (8 + (-cz + sx) * 16) / 16};
                uv[1] = new float[]{(8 + (-cz + sx) * 16) / 16, (8 + (cz + sx) * 16) / 16};
                uv[2] = new float[]{(8 + (cz + sx) * 16) / 16, (8 + (cz - sx) * 16) / 16};
                uv[3] = new float[]{(8 + (cz - sx) * 16) / 16, (8 + (-cz - sx) * 16) / 16};
            }
            quad(out, sprite, new double[][]{{x, y + corners[0], z}, {x, y + corners[1], z + 1}, {x + 1, y + corners[2], z + 1}, {x + 1, y + corners[3], z}}, uv, 1F, light(level, pos), true);
        }
        if (visible(level, pos.below())) {
            quad(out, sprite, new double[][]{{x, y + OFFSET, z + 1}, {x, y + OFFSET, z}, {x + 1, y + OFFSET, z}, {x + 1, y + OFFSET, z + 1}},
                    new float[][]{{0, 1}, {0, 0}, {1, 0}, {1, 1}}, .5F, light(level, pos.below()), false);
        }
        Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (int side = 0; side < 4; side++) {
            BlockPos neighbour = pos.relative(sides[side]);
            if (!visible(level, neighbour)) continue;
            double ax, bx, az, bz, h1, h2;
            switch (side) {
                case 0 -> {ax = x; bx = x + 1; az = z + OFFSET; bz = az; h1 = corners[0]; h2 = corners[3];}
                case 1 -> {ax = x + 1; bx = x; az = z + 1 - OFFSET; bz = az; h1 = corners[2]; h2 = corners[1];}
                case 2 -> {ax = x + OFFSET; bx = ax; az = z + 1; bz = z; h1 = corners[1]; h2 = corners[0];}
                default -> {ax = x + 1 - OFFSET; bx = ax; az = z; bz = z + 1; h1 = corners[3]; h2 = corners[2];}
            }
            quad(out, sprite, new double[][]{{ax, y + h1, az}, {bx, y + h2, bz}, {bx, y, bz}, {ax, y, az}},
                    new float[][]{{0, (float) ((1 - h1) * .5)}, {.5F, (float) ((1 - h2) * .5)}, {.5F, .5F}, {0, .5F}},
                    side < 2 ? .8F : .6F, light(level, neighbour), true);
        }
        return true;
    }

    private static void quad(VertexConsumer out, TextureAtlasSprite sprite, double[][] vertices, float[][] uv, float shade, int light, boolean doubleSided) {
        for (int index : new int[]{0, 1, 2, 3}) vertex(out, sprite, vertices[index], uv[index], shade, light);
        if (doubleSided) for (int index : new int[]{0, 3, 2, 1}) vertex(out, sprite, vertices[index], uv[index], shade, light);
    }

    private static void vertex(VertexConsumer out, TextureAtlasSprite sprite, double[] position, float[] uv, float shade, int light) {
        out.addVertex((float) position[0], (float) position[1], (float) position[2]).setColor(shade, shade, shade, 1F)
                .setUv(sprite.getU(uv[0]), sprite.getV(uv[1])).setLight(light).setNormal(0, 1, 0);
    }
}
