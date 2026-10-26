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
        var compatibleKey=ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST,ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest","overworld_compatible"));
        var compatible=MultiNoiseBiomeSource.createFromPreset(presets.getHolderOrThrow(compatibleKey));
        test.assertTrue(compatible.possibleBiomes().stream().noneMatch(holder->holder.is(ThaumcraftBiomes.MAGICAL_FOREST)||holder.is(ThaumcraftBiomes.EERIE)||holder.is(ThaumcraftBiomes.TAINT)),"a genuine differently keyed native parameter-list using compatible OVERWORLD preset acquires no TC4 palette entries");
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
            var other=compatible.getNoiseBiome(x,16,z,noise.sampler());test.assertTrue(!other.is(ThaumcraftBiomes.MAGICAL_FOREST)&&!other.is(ThaumcraftBiomes.EERIE)&&!other.is(ThaumcraftBiomes.TAINT),"differently keyed compatible source remains native across actual seeded climate queries");
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
    /**
     * The biomes chunk generation writes are the biomes the source answers for the world's own seed.
     * Generation samples the climate through the NoiseChunk's own sampler, and the GameTest world has
     * seed 0, so only a nonzero seed shows whether that sampler carries the seed as well.
     */
    public static void generatedChunkBiomesFollowSeed(GameTestHelper test) {
        var registries=test.getLevel().registryAccess();
        var presets=registries.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        var source=MultiNoiseBiomeSource.createFromPreset(presets.getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        var settings=registries.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
        var generator=new net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator(source,settings);
        var noise=RandomState.create(settings.value(),registries.lookupOrThrow(Registries.NOISE),912873);
        // Quart y 18 is block 72, the surface of an ordinary inland chunk.
        List<net.minecraft.world.level.ChunkPos> patches=new ArrayList<>();
        for(int cx=-256;cx<=256&&patches.size()<6;cx+=16)for(int cz=-256;cz<=256&&patches.size()<6;cz+=16) {
            var expected=source.getNoiseBiome(cx*4+2,18,cz*4+2,noise.sampler());
            if(expected.is(ThaumcraftBiomes.MAGICAL_FOREST)||expected.is(ThaumcraftBiomes.TAINT))patches.add(new net.minecraft.world.level.ChunkPos(cx,cz));
        }
        test.assertTrue(patches.size()==6,"seed 912873 has TC4 biome patches in the searched area");
        List<java.util.concurrent.CompletableFuture<net.minecraft.world.level.chunk.ChunkAccess>> chunks=new ArrayList<>();
        for(var pos:patches) {
            var chunk=new net.minecraft.world.level.chunk.ProtoChunk(pos,net.minecraft.world.level.chunk.UpgradeData.EMPTY,test.getLevel(),
                    registries.registryOrThrow(Registries.BIOME),null);
            // Biome filling runs on the worker pool and asks this level for structure references, so the
            // server thread keeps ticking while the sequence waits for it.
            chunks.add(generator.createBiomes(noise,net.minecraft.world.level.levelgen.blending.Blender.empty(),test.getLevel().structureManager(),chunk));
        }
        test.startSequence().thenWaitUntil(()->test.assertTrue(chunks.stream().allMatch(java.util.concurrent.CompletableFuture::isDone),"chunk biomes are filled"))
                .thenExecute(()->{
                    for(var future:chunks) {
                        var chunk=future.join();var pos=chunk.getPos();
                        var expected=source.getNoiseBiome(pos.x*4+2,18,pos.z*4+2,noise.sampler());
                        // The proto chunk stays at status EMPTY, so read its filled section directly.
                        var actual=chunk.getSection(chunk.getSectionIndex(72)).getNoiseBiome(2,2,2);
                        thaumcraft.common.Thaumcraft.LOG.info("Seeded biome check {}: chunk {}, source {}",pos,actual.unwrapKey().orElse(null),expected.unwrapKey().orElse(null));
                        test.assertTrue(actual.unwrapKey().equals(expected.unwrapKey()),"generated chunk "+pos+" holds "+actual.unwrapKey().map(Object::toString).orElse("?")
                                +" where the seeded source answers "+expected.unwrapKey().map(Object::toString).orElse("?"));
                    }
                }).thenSucceed();
    }
    private static void darkPlants(GameTestHelper test,BlockPos origin) {
        var level=test.getLevel();Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
        var bounds=new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(origin.offset(-2,-1,-2)),net.minecraft.world.phys.Vec3.atLowerCornerOf(origin.offset(3,3,3)));
        var prior=new java.util.HashSet<java.util.UUID>();level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,bounds).forEach(item->prior.add(item.getUUID()));
        for(var mutable:BlockPos.betweenClosed(origin.offset(-2,-1,-2),origin.offset(2,2,2))){var pos=mutable.immutable();test.assertTrue(level.getBlockEntity(pos)==null,"opaque plant-soil fixture has no existing BE");saved.put(pos,level.getBlockState(pos));}
        try {
            for(var pos:saved.keySet()) {
                boolean wall=Math.abs(pos.getX()-origin.getX())==2||Math.abs(pos.getZ()-origin.getZ())==2||pos.getY()==origin.getY()-1||pos.getY()==origin.getY()+2;
                level.setBlock(pos,wall?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            }
            level.setBlock(origin.below(),Blocks.GRASS_BLOCK.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            awaitDarkPlants(test,origin,saved,bounds,prior,0);
        } catch(RuntimeException|Error failure) {restorePlants(level,saved,bounds,prior);throw failure;}
    }
    private static void awaitDarkPlants(GameTestHelper test,BlockPos origin,Map<BlockPos,BlockState> saved,net.minecraft.world.phys.AABB bounds,java.util.Set<java.util.UUID> prior,int waited) {
        var level=test.getLevel();
        try {
            if((level.canSeeSky(origin)||level.getRawBrightness(origin,0)>=8)&&waited<40){test.runAfterDelay(2,()->awaitDarkPlants(test,origin,saved,bounds,prior,waited+2));return;}
            test.assertTrue(level.isEmptyBlock(origin)&&!level.canSeeSky(origin)&&level.getRawBrightness(origin,0)<8,"real opaque enclosure reaches native darkness before any self-luminous plant is inserted");
            var plants=List.<Block>of(ThaumcraftBiomes.VISHROOM.get(),ThaumcraftPlants.SHIMMERLEAF.get(),ThaumcraftPlants.CINDERPEARL.get());
            // Oracle BlockCustomPlant:130-132 answers canPlaceBlockAt with a plain true, so the plant
            // is never refused a position; the rule that keeps it there is the inherited 1.7.10
            // BlockBush.func_149718_j (canBlockStay, the official client jar, the class ajr): the
            // grass, the dirt or the farmland below the plant.
            for(var plant:plants)test.assertTrue(plant.defaultBlockState().canSurvive(level,origin),"original BlockBush stay rule admits each actual plant in unmodified native darkness: "+plant);
            for(var plant:plants){
                level.setBlock(origin.below(),Blocks.GRASS_BLOCK.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                level.setBlock(origin,plant.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                level.getBlockState(origin).randomTick(level,origin,RandomSource.create(715));
                test.assertTrue(level.getBlockState(origin).is(plant),"actual random tick retains the grass-supported plant under the real roof");
                level.setBlock(origin.below(),Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
                // The original plant did not override canBlockStay, so it breaks on the next random
                // tick (oracle BlockCustomPlant:134-136 calls the inherited BlockBush update tick).
                test.assertTrue(!plant.defaultBlockState().canSurvive(level,origin),"removing the original BlockBush soil still invalidates each luminous plant");
                level.getBlockState(origin).randomTick(level,origin,RandomSource.create(716));
                test.assertTrue(level.isEmptyBlock(origin),"actual invalid-soil random tick removes the real plant");
            }
        } catch(RuntimeException|Error failure) {restorePlants(level,saved,bounds,prior);throw failure;}
        restorePlants(level,saved,bounds,prior);test.succeed();
    }
    private static void restorePlants(net.minecraft.server.level.ServerLevel level,Map<BlockPos,BlockState> saved,net.minecraft.world.phys.AABB bounds,java.util.Set<java.util.UUID> prior) {
        saved.forEach((pos,state)->level.setBlock(pos,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE));
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,bounds,item->!prior.contains(item.getUUID())).forEach(net.minecraft.world.entity.Entity::discard);
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
        darkPlants(test,origin);
    }
    private static void clear(net.minecraft.server.level.ServerLevel level,Map<BlockPos,BlockState> saved) {for(var pos:saved.keySet()){level.removeBlockEntity(pos);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|Block.UPDATE_KNOWN_SHAPE);}}
    private static void soil(net.minecraft.server.level.ServerLevel level,BlockPos origin) {for(var pos:BlockPos.betweenClosed(origin.offset(-4,-1,-4),origin.offset(4,-1,4)))level.setBlock(pos,Blocks.DIRT.defaultBlockState(),2|Block.UPDATE_KNOWN_SHAPE);}
}
