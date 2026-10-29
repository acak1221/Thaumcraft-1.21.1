/**
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.dim;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Where the Bedrock portal sends someone: the rules of 1.7.10's {@code TeleporterBedrock} (which placed nothing and let
 * {@code BlockBedrockPortal.onEntityCollidedWithBlock} do the work) as 1.21.1 {@link DimensionTransition}s.
 *
 * <p><b>In.</b> The portal of a surface world leads to the column of the Bedrock dimension with the same x and z: its
 * layers 250 to 253 are cleared of stone and bedrock (249 becomes stone if it is air), the bedrock at 254 becomes a
 * portal (the way out), and whoever travels arrives at y = 251, yaw 0, pitch 0, in the middle of the block.</p>
 *
 * <p><b>Out.</b> The portal of the Bedrock dimension leads to the Overworld, to a random column up to 50 blocks from where
 * the traveller stood, down to the first block that is not air from height 120, three blocks above it (in a column
 * that is air all the way down a stone is put at 119 first). The 1.7.10 scan started at 120 even inside a mountain, which
 * left the traveller inside the rock; here a start cell that is not air sends the scan to the top of the column.</p>
 */
public final class TeleporterBedrock {
    /** Where the traveller stands on arrival (1.7.10: {@code setPlayerLocation(x + 0.5, 251, z + 0.5, 0, 0)}). */
    public static final int ENTRANCE_Y = 251;
    /** The layer below the cavity that becomes stone when it is air. */
    public static final int FLOOR_Y = 249;
    /** The cavity: layers 250 to 253. */
    public static final int CAVITY_MIN_Y = 250;
    public static final int CAVITY_MAX_Y = 253;
    /** The layer of the exit portal. */
    public static final int EXIT_Y = 254;
    /** Where the scan for the ground of the Overworld starts. */
    public static final int SURFACE_START_Y = 120;
    /** How far from the traveller the landing column may be, to each side. */
    public static final int SPREAD = 50;

    private TeleporterBedrock() {}

    /**
     * Prepares the column {@code x, z} of the Bedrock dimension and returns the transition to its cavity.
     *
     * @param portal the state of the portal block (the exit at the ceiling of the cavity)
     */
    public static DimensionTransition toBedrock(ServerLevel bedrock, int x, int z, BlockState portal) {
        prepareEntrance(bedrock, x, z, portal);
        return new DimensionTransition(bedrock, new Vec3(x + 0.5D, ENTRANCE_Y, z + 0.5D), Vec3.ZERO, 0F, 0F,
                DimensionTransition.DO_NOTHING);
    }

    /** 1.7.10 {@code onEntityCollidedWithBlock} after the transfer: the cavity and the exit portal of the column. */
    public static void prepareEntrance(ServerLevel bedrock, int x, int z, BlockState portal) {
        // Loads (and generates) the column's chunk; the original's isAirBlock did the same.
        bedrock.getChunk(x >> 4, z >> 4);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, FLOOR_Y, z);
        if (bedrock.getBlockState(pos).isAir()) bedrock.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        for (int y = CAVITY_MIN_Y; y <= CAVITY_MAX_Y; y++) {
            BlockState state = bedrock.getBlockState(pos.set(x, y, z));
            // "something seems to be turning our bedrock to plain stone, let's also check that"
            if (state.is(Blocks.STONE) || state.is(Blocks.BEDROCK)) {
                bedrock.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (bedrock.getBlockState(pos.set(x, EXIT_Y, z)).is(Blocks.BEDROCK)) {
            bedrock.setBlock(pos, portal, Block.UPDATE_ALL);
        }
    }

    /**
     * The transition from the Bedrock dimension to a random landing in the Overworld.
     *
     * @param fromX where the traveller stood (the landing is {@code (int) fromX + nextInt(100) - 50} blocks, as in 1.7.10)
     */
    public static DimensionTransition toSurface(ServerLevel overworld, double fromX, double fromZ, RandomSource random) {
        int x = (int) fromX + random.nextInt(2 * SPREAD);
        int z = (int) fromZ + random.nextInt(2 * SPREAD);
        x -= SPREAD;
        z -= SPREAD;
        int y = landingY(overworld, x, z);
        return new DimensionTransition(overworld, new Vec3(x + 0.5D, y + 3, z + 0.5D), Vec3.ZERO, 0F, 0F,
                DimensionTransition.DO_NOTHING);
    }

    /**
     * The first block that is not air going down from {@link #SURFACE_START_Y}; where the start is not air (a mountain), the
     * top of the column. A column that is air all the way gets a stone at 119 first (the 1.7.10 scaffolding).
     */
    public static int landingY(Level level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        final int start = Math.min(level.getMaxBuildHeight() - 1, SURFACE_START_Y);
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, start, z);
        if (!level.getBlockState(pos).isAir()) {
            return Math.max(level.getMinBuildHeight(), level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1);
        }
        return scanDown(y -> level.getBlockState(pos.set(x, y, z)).isAir(), start, level.getMinBuildHeight() + 1,
                y -> level.setBlock(pos.set(x, y, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL));
    }

    /**
     * 1.7.10's loop: go down while the cell is air; at the floor start again from the top after putting a scaffold below
     * it. {@code isAir} is asked about the cell at a height and {@code scaffold} receives the height to put the stone at.
     * Gives up (and answers the start) after one more pass than the height can hold, so a scaffold that cannot be placed
     * does not loop for ever.
     */
    public static int scanDown(IntPredicate isAir, int start, int floor, IntConsumer scaffold) {
        int y = start;
        int guard = 2 * Math.max(1, start - floor + 2);
        while (isAir.test(y)) {
            if (guard-- <= 0) return start;
            y--;
            // Avoid infinite loop. Set up the scaffolding.
            if (y <= floor) {
                y = start;
                scaffold.accept(y - 1);
            }
        }
        return y;
    }
}
