package thaumcraft.common.testing;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import thaumcraft.common.config.*;
import thaumcraft.common.lib.world.WorldGenBigMagicTree;
import thaumcraft.common.lib.world.WorldGenGreatwoodTrees;
import thaumcraft.common.lib.world.biomes.BiomeSourceBootstrapScope;
import net.minecraft.data.registries.VanillaRegistries;

/** Actual bundled registry/source/feature/BE checks. Large high platforms in
 * tree fixtures are declared inputs and restored; fresh natural terrain is a
 * separate entrypoint and is never relabelled as those fixture inputs. */
public final class BiomeRuntimeChecks {
    private BiomeRuntimeChecks() {}
    public static void source(GameTestHelper test) {
        var level=test.getLevel();var registries=level.registryAccess();var biomes=registries.registryOrThrow(Registries.BIOME);
        // Real vanilla RegistrySetBuilder bootstrap must finish without any
        // unbound TC4 datapack holders, even after runtime registries exist.
        var builtin=VanillaRegistries.createLookup();
        test.assertTrue(!BiomeSourceBootstrapScope.active()&&builtin.lookupOrThrow(Registries.BIOME).get(ThaumcraftBiomes.MAGICAL_FOREST).isEmpty(),"real native builtin registry bootstrap remains vanilla and closes its custom-holder guard");
        var builtinSource=MultiNoiseBiomeSource.createFromPreset(builtin.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        test.assertTrue(builtinSource.possibleBiomes().stream().noneMatch(holder->holder.unwrapKey().map(key->key.location().getNamespace().equals("thaumcraft")).orElse(false)),"builtin source without datapack biome holders remains a valid untouched native source");
        var forest=biomes.getHolderOrThrow(ThaumcraftBiomes.MAGICAL_FOREST);var eerie=biomes.getHolderOrThrow(ThaumcraftBiomes.EERIE);var taint=biomes.getHolderOrThrow(ThaumcraftBiomes.TAINT);
        test.assertTrue(forest.is(ThaumcraftManaBeans.MAGICAL_BIOMES)&&eerie.is(ThaumcraftManaBeans.MAGICAL_BIOMES)&&taint.is(ThaumcraftManaBeans.MAGICAL_BIOMES),"actual TC4 biomes, not vanilla replacements, carry native MAGICAL membership");
        test.assertTrue(forest.value().getBaseTemperature()==.7F&&forest.value().hasPrecipitation()&&forest.value().getWaterColor()==30702,"actual forest retains original climate and water tint");
        test.assertTrue(eerie.value().getSkyColor()==0x222299&&taint.value().getWaterColor()==0xCC1188,"actual Eerie/Taint original sky/water effects");
        var pech=forest.value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().filter(row->row.type==ThaumcraftPech.PECH.get()).toList();
        test.assertTrue(pech.size()==2&&pech.stream().allMatch(row->row.getWeight().asInt()==10&&row.minCount==1&&row.maxCount==1),"actual forest retains Forge's updated first PLUS appended second Pech10/group1 rows");
        var giant=eerie.value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().filter(row->row.type==ThaumcraftMonsters.GIANT_BRAINY_ZOMBIE.get()).toList();
        test.assertTrue(giant.size()==1&&giant.getFirst().getWeight().asInt()==8,"actual Eerie giant weight8");
        test.assertTrue(taint.value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(row->row.type==ThaumcraftTaint.TAINTACLE.get()&&row.getWeight().asInt()==1),"real Taintacle population is not silently omitted");
        var presets=registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        var source=MultiNoiseBiomeSource.createFromPreset(presets.getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        var nether=MultiNoiseBiomeSource.createFromPreset(presets.getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER));
        var direct=MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(List.of(Pair.of(Climate.parameters(0,0,0,0,0,0,0),biomes.getHolderOrThrow(Biomes.PLAINS)))));
        var noise=RandomState.create(registries.registryOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD),registries.lookupOrThrow(Registries.NOISE),912873);
        test.assertTrue(source.possibleBiomes().containsAll(List.of(forest,eerie,taint)),"real canonical Overworld source advertises TC4 holders for native feature sorting");
        test.assertTrue(!nether.possibleBiomes().contains(forest)&&!nether.possibleBiomes().contains(eerie)&&!nether.possibleBiomes().contains(taint)&&direct.possibleBiomes().size()==1,"Nether and direct/custom MultiNoise palettes acquire no TC4 biome entries");
        boolean foundForest=false,foundTaint=false;
        for(int x=-2048;x<=2048;x+=64)for(int z=-2048;z<=2048;z+=64){
            var found=source.getNoiseBiome(x,16,z,noise.sampler());
            foundForest|=found.is(ThaumcraftBiomes.MAGICAL_FOREST);foundTaint|=found.is(ThaumcraftBiomes.TAINT);
            test.assertTrue(!found.is(ThaumcraftBiomes.EERIE),"Eerie has no climate ticket; DARK ecology must create it");
            test.assertTrue(direct.getNoiseBiome(x,16,z,noise.sampler()).is(Biomes.PLAINS),"direct native custom source remains its genuine single-biome palette");
        }
        test.assertTrue(foundForest&&foundTaint,"bounded native worldseed/sample grid reaches both actual WARM/COOL TC4 biome tickets");
        // Recreate the actual source and native seeded noise state, then query
        // in reverse order: neither prior query/cache order nor another world
        // sampler may change these biome identities.
        var copy=MultiNoiseBiomeSource.createFromPreset(presets.getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        var sameNoise=RandomState.create(registries.registryOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD),registries.lookupOrThrow(Registries.NOISE),912873);
        for(int x=512;x>=-512;x-=64)for(int z=512;z>=-512;z-=64)test.assertTrue(source.getNoiseBiome(x,16,z,noise.sampler()).unwrapKey().equals(copy.getNoiseBiome(x,16,z,sameNoise.sampler()).unwrapKey()),"real seeded source result survives independent sampler and reverse query order");
        test.succeed();
    }
    public static void treesAndDungeon(GameTestHelper test) {
        var level=test.getLevel();var origin=new BlockPos(test.absolutePos(BlockPos.ZERO).getX()+8,180,test.absolutePos(BlockPos.ZERO).getZ()+8);
        for(int x=(origin.getX()-18)>>4;x<=(origin.getX()+18)>>4;x++)for(int z=(origin.getZ()-18)>>4;z<=(origin.getZ()+18)>>4;z++)level.getChunk(x,z);
        Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
        for(var mutable:BlockPos.betweenClosed(origin.offset(-14,-3,-14),origin.offset(14,42,14))){var pos=mutable.immutable();test.assertTrue(level.getBlockEntity(pos)==null,"high tree/dungeon fixture contains no existing BE");saved.put(pos,level.getBlockState(pos));}
        long resume=level.random.nextLong();
        try {
            clear(level,saved);soil(level,origin);
            level.setBlock(origin.above(6),Blocks.STONE.defaultBlockState(),2);
            var random=RandomSource.create(0);var expected=RandomSource.create(0);expected.nextLong();level.random.setSeed(7813);
            test.assertTrue(new WorldGenBigMagicTree(false).generate(level,random,origin),"actual original BigMagic tree tolerates/truncates obstruction at six blocks");
            test.assertTrue(random.nextLong()==expected.nextLong()&&level.random.nextLong()==RandomSource.create(7813).nextLong(),"actual BigMagic geometry seeds one private stream from caller nextLong, not live WORLD RNG");
            test.assertTrue(saved.keySet().stream().anyMatch(pos->level.getBlockState(pos).is(Blocks.OAK_LOG))&&saved.keySet().stream().anyMatch(pos->level.getBlockState(pos).is(Blocks.OAK_LEAVES)),"BigMagic produces actual native OAK logs/leaves rather than Greatwood or a marker");
            clear(level,saved);soil(level,origin);test.assertTrue(new WorldGenBigMagicTree(false).generate(level,RandomSource.create(71),origin),"a distinct generator call reuses the world's obstruction-shortened height");
            int highest=saved.keySet().stream().filter(pos->level.getBlockState(pos).is(Blocks.OAK_LOG)||level.getBlockState(pos).is(Blocks.OAK_LEAVES)).mapToInt(BlockPos::getY).max().orElseThrow();
            test.assertTrue(highest<=origin.getY()+5,"chosen BigMagic height persists across calls without per-chunk resetting");
            test.assertTrue(saved.keySet().stream().filter(pos->level.getBlockState(pos).is(Blocks.OAK_LEAVES)).allMatch(pos->!level.getBlockState(pos).getValue(LeavesBlock.PERSISTENT)&&level.getBlockState(pos).getValue(LeavesBlock.DISTANCE)<7),"native oak canopy is committed with real supported distance and natural leaves");
            clear(level,saved);soil(level,origin);random=RandomSource.create(0);expected=RandomSource.create(0);expected.nextLong();for(int i=0;i<50;i++){expected.nextInt(14);expected.nextInt(10);expected.nextInt(14);}level.random.setSeed(19813);
            test.assertTrue(new WorldGenGreatwoodTrees(false).generate(level,random,origin,true),"real Greatwood cave-spider dungeon branch generates with its actual tree");
            test.assertTrue(random.nextLong()==expected.nextLong()&&level.random.nextLong()==RandomSource.create(19813).nextLong(),"Greatwood caller consumes one seed plus exactly50xXYZ web draws; actual loot uses private stream, never WORLD");
            test.assertTrue(level.getBlockEntity(origin.below()) instanceof SpawnerBlockEntity&&level.getBlockEntity(origin.below(2)) instanceof ChestBlockEntity,"actual native spawner/chest BEs replace the skipped dungeon branch");
            var spawner=(SpawnerBlockEntity)level.getBlockEntity(origin.below());var data=spawner.getSpawner().save(new net.minecraft.nbt.CompoundTag());
            test.assertTrue(data.getCompound("SpawnData").getCompound("entity").getString("id").equals("minecraft:cave_spider"),"saved real spawner contains original CaveSpider identity");
            var chest=(ChestBlockEntity)level.getBlockEntity(origin.below(2));int occupied=0;for(int i=0;i<chest.getContainerSize();i++)if(!chest.getItem(i).isEmpty())occupied++;
            test.assertTrue(occupied>0&&occupied<=8,"actual original eight weighted random slot writes produce native rewards, with overwrite semantics");
            test.assertTrue(saved.keySet().stream().anyMatch(pos->level.getBlockState(pos).is(Blocks.COBWEB)),"actual original50-attempt branch emits connected native cobwebs");
        } finally {for(var pos:saved.keySet())level.removeBlockEntity(pos);saved.forEach((pos,state)->level.setBlock(pos,state,2|Block.UPDATE_KNOWN_SHAPE));level.random.setSeed(resume);}
        test.succeed();
    }
    private static void clear(net.minecraft.server.level.ServerLevel level,Map<BlockPos,BlockState> saved) {for(var pos:saved.keySet()){level.removeBlockEntity(pos);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|Block.UPDATE_KNOWN_SHAPE);}}
    private static void soil(net.minecraft.server.level.ServerLevel level,BlockPos origin) {for(var pos:BlockPos.betweenClosed(origin.offset(-4,-1,-4),origin.offset(4,-1,4)))level.setBlock(pos,Blocks.DIRT.defaultBlockState(),2|Block.UPDATE_KNOWN_SHAPE);}
}
