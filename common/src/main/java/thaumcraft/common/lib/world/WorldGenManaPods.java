package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import thaumcraft.common.blocks.BlockManaPod;
import thaumcraft.common.config.ThaumcraftManaBeans;
import thaumcraft.common.tiles.TileManaPod;

/** Original upward ManaPod search with absolute64/128 bounds. Generation
 * uses native region-safe admission and real typed pod block entities. */
public final class WorldGenManaPods {
    /** Oracle WorldGenManaPods:14 - the search never goes above this height. */
    public static final int MAX_Y = 128;
    /** Oracle WorldGenManaPods:25 - the horizontal spread of the drifting search. */
    public static final int SPREAD = 4;
    /** Oracle WorldGenManaPods:17 - the pod grows with the age two to six. */
    public static final int AGE_MIN = 2;
    /** Oracle WorldGenManaPods:17 - the pod grows with the age two to six. */
    public static final int AGE_VARIANTS = 5;

    public boolean generate(WorldGenLevel level,RandomSource caller,RandomSource growth,BlockPos origin) {
        int initialX=origin.getX(),initialZ=origin.getZ(),x=initialX,y=origin.getY(),z=initialZ;
        // Oracle WorldGenManaPods:23 - the bound of the search is the LIVE height of
        // the 1.7.10 column, which the world generator keeps up to date with every
        // block it places. The 1.21.1 generation region freezes its heightmap
        // before the features step, so the heightmap of the region does not see
        // the trees of the very same decoration pass and the search would never
        // reach the branches the pod hangs under. The port therefore asks the
        // column itself, exactly as the live original heightmap answers.
        int height=liveHeight(level,x,z);
        while(y<MAX_Y&&y<height) {
            if(!level.hasChunkAt(new BlockPos(x,y,z)))return false;
            BlockPos pos=new BlockPos(x,y,z);
            if(!level.hasChunkAt(pos)||!WorldGenWriteArea.canWrite(level, pos)||!level.hasChunkAt(pos.below()))return false;
            if(level.isEmptyBlock(pos)&&level.isEmptyBlock(pos.below())) {
                if(level.getBiome(pos).is(ThaumcraftManaBeans.MAGICAL_BIOMES)&&BlockManaPod.isSupport(level.getBlockState(pos.above()))) {
                    if(level.setBlock(pos,ThaumcraftManaBeans.MANA_POD.get().defaultBlockState().setValue(BlockManaPod.AGE,AGE_MIN+caller.nextInt(AGE_VARIANTS)),2))TileManaPod.initializeGenerated(level,pos,growth);
                    break;
                }
            } else {
                x=initialX+caller.nextInt(SPREAD)-caller.nextInt(SPREAD);
                z=initialZ+caller.nextInt(SPREAD)-caller.nextInt(SPREAD);
                // The drifted column has its own live height, as in the original.
                height=liveHeight(level,x,z);
            }
            ++y;
        }
        return true;
    }
    /**
     * The live height of a column: the block above the highest block that is not
     * air. The 1.7.10 heightmap answered the same value while the generator was
     * still writing the column.
     */
    private static int liveHeight(WorldGenLevel level,int x,int z) {
        for(int y=Math.min(MAX_Y,level.getMaxBuildHeight())-1;y>=level.getMinBuildHeight();--y) {
            BlockPos pos=new BlockPos(x,y,z);
            if(level.hasChunkAt(pos)&&!level.isEmptyBlock(pos))return y+1;
        }
        return level.getMinBuildHeight();
    }
}
