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
        while(y<MAX_Y&&y<level.getMaxBuildHeight()) {
            if(!level.hasChunkAt(new BlockPos(x,y,z)))return false;
            // FEATURES runs after CARVERS: ProtoChunk now updates the final
            // heightmaps, while WORLD_SURFACE_WG retains pre-tree terrain.
            // The original getHeight must include the canopy just generated.
            if(y>=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z))break;
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
            }
            ++y;
        }
        return true;
    }
}
