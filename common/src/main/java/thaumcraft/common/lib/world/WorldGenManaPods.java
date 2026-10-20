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
    public boolean generate(WorldGenLevel level,RandomSource caller,RandomSource growth,BlockPos origin) {
        int initialX=origin.getX(),initialZ=origin.getZ(),x=initialX,y=origin.getY(),z=initialZ;
        while(y<128&&y<level.getMaxBuildHeight()) {
            if(!level.hasChunkAt(new BlockPos(x,y,z)))return false;
            if(y>=level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z))break;
            BlockPos pos=new BlockPos(x,y,z);
            if(!level.hasChunkAt(pos)||!level.ensureCanWrite(pos)||!level.hasChunkAt(pos.below()))return false;
            if(level.isEmptyBlock(pos)&&level.isEmptyBlock(pos.below())) {
                if(level.getBiome(pos).is(ThaumcraftManaBeans.MAGICAL_BIOMES)&&BlockManaPod.isSupport(level.getBlockState(pos.above()))) {
                    if(level.setBlock(pos,ThaumcraftManaBeans.MANA_POD.get().defaultBlockState().setValue(BlockManaPod.AGE,2+caller.nextInt(5)),2))TileManaPod.initializeGenerated(level,pos,growth);
                    break;
                }
            } else {
                x=initialX+caller.nextInt(4)-caller.nextInt(4);
                z=initialZ+caller.nextInt(4)-caller.nextInt(4);
            }
            ++y;
        }
        return true;
    }
}
