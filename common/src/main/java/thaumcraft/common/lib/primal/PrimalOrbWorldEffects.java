package thaumcraft.common.lib.primal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.entities.projectile.EntityPrimalOrb;
import thaumcraft.common.entities.projectile.EntityShockOrb;
import thaumcraft.common.lib.world.BiomeColumns;
import thaumcraft.common.lib.world.NodeGeneration;

/** Actual small-node and taint outcomes; depends on real all-type node ecology, taint biome and supported fibres. */
public final class PrimalOrbWorldEffects {
    private PrimalOrbWorldEffects(){}
    public static boolean smallNode(ServerLevel level,BlockPos target,RandomSource random){
        return NodeGeneration.createRandomNodeAt(level,target,random,false,false,true);
    }
    public static void taint(EntityPrimalOrb orb,ServerLevel level,RandomSource random){
        if(!level.getServer().isSameThread()||level.getServer().getLevel(level.dimension())!=level||orb.level()!=level)throw new IllegalStateException("Primal taint requires the current native server world");
        int x=(int)orb.getX(),z=(int)orb.getZ(); // Original truncation toward zero, NOT floor.
        for(int attempt=0;attempt<10;attempt++){
            int xx=x+PrimalFocusRules.taintOffset(random.nextFloat(),random.nextFloat());
            int zz=z+PrimalFocusRules.taintOffset(random.nextFloat(),random.nextFloat());
            if(!random.nextBoolean())continue;
            var biome=BiomeColumns.surfaceHolder(level,xx,zz);
            if(biome==null||biome.is(ThaumcraftBiomes.TAINT))continue;
            if(!level.getWorldBorder().isWithinBounds(new BlockPos(xx,level.getMinBuildHeight(),zz)))continue;
            BiomeColumns.set(level,xx,zz,ThaumcraftBiomes.TAINT);
            int yy=level.getHeight(Heightmap.Types.WORLD_SURFACE,xx,zz);BlockPos target=new BlockPos(xx,yy,zz);
            if(!EntityShockOrb.loaded(level,target)||!EntityShockOrb.loaded(level,target.below()))continue;
            if(!level.getBlockState(target.below()).isAir())level.setBlock(target,ThaumcraftTaint.FIBRES.get().defaultBlockState(),Block.UPDATE_ALL);
        }
    }
}
