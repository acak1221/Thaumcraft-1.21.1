package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import thaumcraft.client.renderers.block.WardedGlassBakedModel;
import thaumcraft.common.blocks.BlockCosmeticOpaque;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileOwned;

/**
 * The rune faces of the warded glass.
 *
 * <p>Original {@code BlockCosmeticOpaque} (oracle
 * {@code thaumcraft/common/blocks/BlockCosmeticOpaque.java}) draws the warded
 * glass with one of forty seven textures; the texture of a face depends on the
 * eight warded glass blocks that touch it. The native Minecraft 1.21.1 block
 * state cannot carry that neighbour mask, so the mask is computed by the
 * renderer from the world and the rune faces are drawn over the block model.
 */
public final class TileWardedGlassRenderer implements BlockEntityRenderer<TileOwned> {
    /** The original textures of the warded glass. */
    private static final String[] TEXTURES = new String[WardedGlassBakedModel.TEXTURE_COUNT];

    static {
        for (int index = 0; index < TEXTURES.length; index++) {
            TEXTURES[index] = "thaumcraft:textures/blocks/warded_glass_" + (index + 1);
        }
    }

    public TileWardedGlassRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(TileOwned tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = tile.getLevel();
        if (level == null) return;
        BlockPos pos = tile.getBlockPos();
        // The owned tile also belongs to the arcane door, only the warded glass
        // carries the rune faces.
        if (!isGlass(level, pos)) return;
        for (Direction side : Direction.values()) {
            boolean[] mask = new boolean[8];
            neighbours(level, pos, side, mask);
            int texture = WardedGlassBakedModel.textureIndex(mask);
            var out = buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse(TEXTURES[texture])));
            face(pose, out, side, light, overlay);
        }
    }

    /** The original eight neighbour flags of a face of the warded glass. */
    private static void neighbours(Level level, BlockPos pos, Direction side, boolean[] mask) {
        switch (side) {
            case DOWN, UP -> {
                BlockPos below = side == Direction.DOWN ? pos : pos.above();
                mask[0] = isGlass(level, below.offset(-1, 0, -1));
                mask[1] = isGlass(level, below.offset(0, 0, -1));
                mask[2] = isGlass(level, below.offset(1, 0, -1));
                mask[3] = isGlass(level, below.offset(-1, 0, 0));
                mask[4] = isGlass(level, below.offset(1, 0, 0));
                mask[5] = isGlass(level, below.offset(-1, 0, 1));
                mask[6] = isGlass(level, below.offset(0, 0, 1));
                mask[7] = isGlass(level, below.offset(1, 0, 1));
            }
            case NORTH, SOUTH -> {
                int first = side == Direction.NORTH ? 1 : -1;
                mask[0] = isGlass(level, pos.offset(first, 1, 0));
                mask[1] = isGlass(level, pos.offset(0, 1, 0));
                mask[2] = isGlass(level, pos.offset(-first, 1, 0));
                mask[3] = isGlass(level, pos.offset(first, 0, 0));
                mask[4] = isGlass(level, pos.offset(-first, 0, 0));
                mask[5] = isGlass(level, pos.offset(first, -1, 0));
                mask[6] = isGlass(level, pos.offset(0, -1, 0));
                mask[7] = isGlass(level, pos.offset(-first, -1, 0));
            }
            default -> {
                int first = side == Direction.WEST ? -1 : 1;
                mask[0] = isGlass(level, pos.offset(0, 1, first));
                mask[1] = isGlass(level, pos.offset(0, 1, 0));
                mask[2] = isGlass(level, pos.offset(0, 1, -first));
                mask[3] = isGlass(level, pos.offset(0, 0, first));
                mask[4] = isGlass(level, pos.offset(0, 0, -first));
                mask[5] = isGlass(level, pos.offset(0, -1, first));
                mask[6] = isGlass(level, pos.offset(0, -1, 0));
                mask[7] = isGlass(level, pos.offset(0, -1, -first));
            }
        }
    }

    /** True when the block at the position is a warded glass. */
    private static boolean isGlass(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        return state.getBlock() == ThaumcraftEldritchBlocks.COSMETIC_OPAQUE_WARDED.get()
                && BlockCosmeticOpaque.variant(state) == EldritchBlockRules.COSMETIC_OPAQUE_OWNED;
    }

    /** One face of the warded glass, drawn a hair in front of the block model. */
    private static void face(PoseStack pose, VertexConsumer out, Direction side, int light, int overlay) {
        float low = -.001F;
        float high = 1.001F;
        float[][] corners;
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        float nx = side.getStepX();
        float ny = side.getStepY();
        float nz = side.getStepZ();
        switch (side) {
            case DOWN -> corners = new float[][]{{low, low, low}, {high, low, low}, {high, low, high}, {low, low, high}};
            case UP -> corners = new float[][]{{low, high, low}, {high, high, low}, {high, high, high}, {low, high, high}};
            case NORTH -> corners = new float[][]{{high, high, low}, {low, high, low}, {low, low, low}, {high, low, low}};
            case SOUTH -> corners = new float[][]{{low, high, high}, {high, high, high}, {high, low, high}, {low, low, high}};
            case WEST -> corners = new float[][]{{low, high, low}, {low, high, high}, {low, low, high}, {low, low, low}};
            default -> corners = new float[][]{{high, high, low}, {high, high, high}, {high, low, high}, {high, low, low}};
        }
        for (int index = 0; index < 4; index++) {
            out.addVertex(pose.last(), corners[index][0], corners[index][1], corners[index][2])
                    .setColor(255, 255, 255, 255)
                    .setUv(uv[index][0], uv[index][1]).setOverlay(overlay).setLight(light)
                    .setNormal(pose.last(), nx, ny, nz);
        }
    }
}
