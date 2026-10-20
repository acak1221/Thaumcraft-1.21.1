package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.BlockTaint;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.config.ThaumcraftTaintSettings;
import thaumcraft.common.entities.EntityFallingTaint;
import thaumcraft.common.entities.monster.EntityTaintSpore;
import thaumcraft.common.lib.world.BiomeColumns;

/** Actual native subtype placement/loot/ecology/exposure and single-source falling custody. */
public final class TaintBlockRuntimeChecks {
    private static final int QUIET=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE;
    private TaintBlockRuntimeChecks(){}
    public static void run(GameTestHelper test){
        var check=new Check(test);try{check.prepare();check.blocks();check.spread();check.ecology();check.gooAndRestoration();check.poison();check.falling();}
        catch(RuntimeException|Error failure){check.finish();throw failure;}
    }
    private static final class Check {
        final GameTestHelper test;final ServerLevel level;final BlockPos base;
        final Map<BlockPos,BlockState> originals=new LinkedHashMap<>();final List<ChunkPos> forced=new ArrayList<>();
        final List<Entity> entities=new ArrayList<>();final Map<BlockPos,List<Holder<Biome>>> biomes=new HashMap<>();
        final ThaumcraftTaintSettings.Values settings=ThaumcraftTaintSettings.values();EntityEvent.Add observer;boolean finished;
        ServerPlayer placementPlayer;Connection placementConnection;EmbeddedChannel placementChannel;
        EntityFallingTaint falling;BlockPos fallSource,landing,expiring;
        Check(GameTestHelper test){this.test=test;level=test.getLevel();base=test.absolutePos(new BlockPos(3,100,3));}
        void set(BlockPos pos,BlockState state){originals.putIfAbsent(pos.immutable(),level.getBlockState(pos));level.setBlock(pos,state,QUIET);}
        void biome(BlockPos pos,net.minecraft.resources.ResourceKey<Biome> key){
            int x=QuartPos.fromBlock(pos.getX()),z=QuartPos.fromBlock(pos.getZ());var column=new BlockPos(x,0,z);
            biomes.computeIfAbsent(column,ignored->{var saved=new ArrayList<Holder<Biome>>();var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);for(int y=QuartPos.fromBlock(level.getMinBuildHeight());y<=QuartPos.fromBlock(level.getMaxBuildHeight()-1);y++)saved.add(chunk.getNoiseBiome(x,y,z));return saved;});
            BiomeColumns.set(level,pos.getX(),pos.getZ(),key);
        }
        void prepare(){
            ThaumcraftTaintSettings.configure(new ThaumcraftTaintSettings.Values(0,false,true));
            for(int x=(base.getX()-40)>>4;x<=(base.getX()+40)>>4;x++)for(int z=(base.getZ()-40)>>4;z<=(base.getZ()+40)>>4;z++){
                if(!level.getForcedChunks().contains(ChunkPos.asLong(x,z))){level.setChunkForced(x,z,true);forced.add(new ChunkPos(x,z));}level.getChunk(x,z);
            }
            for(BlockPos pos:BlockPos.betweenClosed(base.offset(-7,-7,-7),base.offset(7,10,7)))set(pos.immutable(),Blocks.AIR.defaultBlockState());
            for(int x=-8;x<=8;x+=4)for(int z=-8;z<=8;z+=4)biome(base.offset(x,0,z),ThaumcraftBiomes.TAINT);
        }
        void blocks(){
            var crust=ThaumcraftTaint.CRUST.get().defaultBlockState();var soil=ThaumcraftTaint.SOIL.get().defaultBlockState();var fibres=ThaumcraftTaint.FIBRES.get().defaultBlockState();
            test.assertTrue(crust.getDestroySpeed(level,base)==1.75F&&soil.getDestroySpeed(level,base)==1.5F&&ThaumcraftTaint.CRUST.get().getExplosionResistance()==6F&&ThaumcraftTaint.FIBRES.get().getExplosionResistance()==3F,"registered actual metadata hardness and old internal-resistance conversion");
            test.assertTrue(fibres.getCollisionShape(level,base).isEmpty()&&!fibres.isFaceSturdy(level,base,Direction.UP)&&fibres.setValue(BlockTaintFibres.TYPE,2).getLightEmission()==8&&fibres.setValue(BlockTaintFibres.TYPE,4).getLightEmission()==10,"native fibres have no physical support/collision and original independent8/10 light states");
            // Vanilla's mock server player overrides isCreative() to always true.
            // Use an actual connected survival actor for strict physical item payment.
            placementPlayer=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"taint-"+UUID.randomUUID().toString().substring(0,8)),ClientInformation.createDefault());
            placementConnection=new Connection(PacketFlow.SERVERBOUND);placementChannel=new EmbeddedChannel(placementConnection);
            level.getServer().getPlayerList().placeNewPlayer(placementConnection,placementPlayer,CommonListenerCookie.createInitial(placementPlayer.getGameProfile(),false));
            var player=placementPlayer;player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.getInventory().clearContent();player.setPos(base.getX()+.5,base.getY(),base.getZ()+2.5);set(base.below(),Blocks.STONE.defaultBlockState());
            test.assertTrue(!player.isCreative()&&!player.getAbilities().instabuild,"actual registered survival actor pays native block-item placement");
            for(int type=0;type<5;type++){
                set(base,Blocks.AIR.defaultBlockState());var stack=new ItemStack(ThaumcraftTaint.fibreItem(type));player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var hit=new BlockHitResult(Vec3.atCenterOf(base.below()).add(0,.5,0),Direction.UP,base.below(),false);
                var result=stack.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
                test.assertTrue(result.consumesAction()&&level.getBlockState(base).is(ThaumcraftTaint.FIBRES.get())&&level.getBlockState(base).getValue(BlockTaintFibres.TYPE)==type&&stack.isEmpty(),"actual survival BlockItem placement selects original fibre subtype "+type);
                test.assertTrue(ThaumcraftTaint.FIBRES.get().asItem()==ThaumcraftTaint.fibreItem(0)&&level.getBlockState(base).getBlock().getCloneItemStack(level,base,level.getBlockState(base)).is(ThaumcraftTaint.fibreItem(type)),"five subtype items retain canonical Block.asItem0 and exact pick result");
            }
            biome(base,Biomes.PLAINS);set(base,Blocks.AIR.defaultBlockState());var denied=new ItemStack(ThaumcraftTaint.fibreItem(3));player.setItemInHand(InteractionHand.MAIN_HAND,denied);
            var hit=new BlockHitResult(Vec3.atCenterOf(base.below()).add(0,.5,0),Direction.UP,base.below(),false);denied.getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
            test.assertTrue(level.isEmptyBlock(base)&&denied.getCount()==1,"actual native placement outside real TAINT biome refuses before item consumption");biome(base,ThaumcraftBiomes.TAINT);closePlacementPlayer();
            var silk=new ItemStack(Items.DIAMOND_PICKAXE);silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
            test.assertTrue(Block.getDrops(crust,level,base,null,null,ItemStack.EMPTY).isEmpty()&&Block.getDrops(crust,level,base,null,null,silk).stream().mapToInt(ItemStack::getCount).sum()==1,"actual loaded crust loot has no ordinary reward, one exact silk source");
            var ordinary=Block.getDrops(soil,level,base,null,null,ItemStack.EMPTY);var silken=Block.getDrops(soil,level,base,null,null,silk);
            test.assertTrue(ordinary.size()==1&&ordinary.getFirst().is(Items.DIRT)&&ordinary.getFirst().getCount()==1&&silken.size()==1&&silken.getFirst().is(ThaumcraftTaint.SOIL.get().asItem()),"actual soil loot returns dirt normally and exact flattened soil with silk");
            test.assertTrue(Block.getDrops(fibres,level,base,null,null,silk).isEmpty(),"fibre loot remains empty even with silk");
        }
        void spread(){
            int[][] values={{1},{0,8},{0,9,9},{0,9,10}};List<List<Integer>> expected=List.of(List.of(10),List.of(10,10),List.of(10,10,12),List.of(10,10,12));
            for(int type=0;type<4;type++){
                set(base,Blocks.AIR.defaultBlockState());set(base.below(),Blocks.STONE.defaultBlockState());
                try(var input=new Input(level,values[type])){
                    test.assertTrue(BlockTaintFibres.spreadFibres(level,base.getX(),base.getY(),base.getZ())&&level.getBlockState(base).getValue(BlockTaintFibres.TYPE)==type&&input.bounds.equals(expected.get(type)),"actual free ecology follows exact original conditional lottery "+type);
                }
            }
            set(base,Blocks.WATER.defaultBlockState());try(var input=new Input(level)){
                test.assertTrue(!BlockTaintFibres.spreadFibres(level,base.getX(),base.getY(),base.getZ())&&input.bounds.isEmpty()&&level.getBlockState(base).is(Blocks.WATER),"liquid source is never overwritten and consumes no fibre draw");
            }
            set(base,Blocks.AIR.defaultBlockState());set(base.below(),ThaumcraftTaint.FLESH.get().defaultBlockState());
            test.assertTrue(BlockTaintFibres.isOnlyAdjacentToTaint(level,base.getX(),base.getY(),base.getZ())&&BlockTaintFibres.getAdjacentTaint(level,base.getX(),base.getY(),base.getZ())==1,"actual existing flesh_block is original metadata2 in both family predicates");
            try(var input=new Input(level)){test.assertTrue(!BlockTaintFibres.spreadFibres(level,base.getX(),base.getY(),base.getZ())&&input.bounds.isEmpty(),"taint-only supporting material cannot seed more fibres");}
            set(base.below(),Blocks.STONE.defaultBlockState());
            set(base,ThaumcraftTaint.FIBRES.get().defaultBlockState().setValue(BlockTaintFibres.TYPE,1));set(base.east(),ThaumcraftTaint.FLESH.get().defaultBlockState());biome(base,Biomes.PLAINS);
            ThaumcraftTaintSettings.configure(new ThaumcraftTaintSettings.Values(200,false,true));
            try(var input=new Input(level,1,1,0)){BlockTaintFibres.taintBiomeSpread(level,base.getX(),base.getY(),base.getZ(),input.input,ThaumcraftTaint.FIBRES.get());test.assertTrue(!BlockTaintFibres.isTaintBiome(level,base)&&input.bounds.equals(List.of(3,3,1000))&&input.longDraws==0,"actual default biome spread consumes chance before rejecting fewer than two adjacent family blocks");}
            set(base.west(),ThaumcraftTaint.FLESH.get().defaultBlockState());
            try(var input=new Input(level,1,1,0)){BlockTaintFibres.taintBiomeSpread(level,base.getX(),base.getY(),base.getZ(),input.input,ThaumcraftTaint.FIBRES.get());test.assertTrue(BlockTaintFibres.isTaintBiome(level,base)&&input.bounds.equals(List.of(3,3,1000))&&input.longDraws==0,"actual default biome spread rewrites native stored column with original3/3/1000 int schedule");}
            biome(base,Biomes.PLAINS);ThaumcraftTaintSettings.configure(new ThaumcraftTaintSettings.Values(429496730,false,true));
            try(var input=new Input(level,1,1)){BlockTaintFibres.taintBiomeSpread(level,base.getX(),base.getY(),base.getZ(),input.input,ThaumcraftTaint.FIBRES.get());test.assertTrue(BlockTaintFibres.isTaintBiome(level,base)&&input.bounds.equals(List.of(3,3))&&input.longDraws==1,"actual overflowing rate preserves configured value and rewrites native biome through one long draw instead of crashing nextInt(-2147483646)");}
            ThaumcraftTaintSettings.configure(new ThaumcraftTaintSettings.Values(0,false,true));set(base.east(),Blocks.AIR.defaultBlockState());set(base.west(),Blocks.AIR.defaultBlockState());
        }
        void ecology(){
            var source=ThaumcraftTaint.FIBRES.get().defaultBlockState().setValue(BlockTaintFibres.TYPE,1);set(base,source);BlockPos target=base.east();
            set(target,Blocks.OAK_LOG.defaultBlockState());set(target.above(),ThaumcraftTaint.FLESH.get().defaultBlockState());
            try(var input=new Input(level,2,3,1)){source.tick(level,base,input.input);test.assertTrue(level.getBlockState(target).is(ThaumcraftTaint.CRUST.get())&&input.bounds.equals(List.of(3,5,3)),"actual tick converts log with exactly two adjacent family blocks, not soil's three");}
            set(target,Blocks.DIRT.defaultBlockState());try(var input=new Input(level,2,3,1)){source.tick(level,base,input.input);test.assertTrue(level.getBlockState(target).is(Blocks.DIRT),"two adjacent taint blocks do not satisfy original soil conversion threshold");}
            set(target.north(),ThaumcraftTaint.CRUST.get().defaultBlockState());try(var input=new Input(level,2,3,1)){source.tick(level,base,input.input);test.assertTrue(level.getBlockState(target).is(ThaumcraftTaint.SOIL.get()),"third adjacent family block enables actual dirt-to-soil conversion");}
            set(target,Blocks.STONE.defaultBlockState());set(base,source.setValue(BlockTaintFibres.TYPE,3));ThaumcraftTaintSettings.configure(new ThaumcraftTaintSettings.Values(0,true,true));
            try(var input=new Input(level,2,3,1,0)){level.getBlockState(base).tick(level,base,input.input);test.assertTrue(input.bounds.equals(List.of(3,5,3,10))&&level.getBlockState(base).getValue(BlockTaintFibres.TYPE)==4,"failed spread and exact nextInt10 birth advances real stalk3 to4");}
            var spores=level.getEntitiesOfClass(EntityTaintSpore.class,new AABB(base.above()));entities.addAll(spores);test.assertTrue(spores.size()==1&&spores.getFirst().getSporeSize()==2,"real stalk spawns registered native original-size2 spore, not a marker");spores.forEach(Entity::discard);
            try(var input=new Input(level,2,3,1)){level.getBlockState(base).tick(level,base,input.input);test.assertTrue(level.getBlockState(base).getValue(BlockTaintFibres.TYPE)==3,"actual absent-spore query recovers mature4 to stalk3");}
            ThaumcraftTaintSettings.configure(new ThaumcraftTaintSettings.Values(0,false,true));set(base,source);set(target.above(),Blocks.AIR.defaultBlockState());set(target.north(),Blocks.AIR.defaultBlockState());set(target,Blocks.AIR.defaultBlockState());
        }
        void poison(){
            LivingEntity sheep=EntityType.SHEEP.create(level),zombie=EntityType.ZOMBIE.create(level);sheep.moveTo(Vec3.atCenterOf(base));zombie.moveTo(Vec3.atCenterOf(base).add(3,0,0));level.addFreshEntity(sheep);level.addFreshEntity(zombie);entities.add(sheep);entities.add(zombie);
            var effect=BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get());
            try(var input=new Input(level,0)){BlockTaintFibres.poison(level,sheep,true);test.assertTrue(input.bounds.equals(List.of(500))&&sheep.hasEffect(effect)&&sheep.getEffect(effect).getDuration()==160,"actual fibre contact poisons non-undead native living target at500/160");}
            sheep.removeEffect(effect);try(var input=new Input(level,0)){ThaumcraftTaint.CRUST.get().stepOn(level,base,ThaumcraftTaint.CRUST.get().defaultBlockState(),sheep);test.assertTrue(input.bounds.equals(List.of(20))&&sheep.hasEffect(effect),"actual walking native hook uses independent bound20");}
            try(var input=new Input(level)){BlockTaintFibres.poison(level,zombie,true);ThaumcraftTaint.FLESH.get().stepOn(level,base,ThaumcraftTaint.FLESH.get().defaultBlockState(),sheep);test.assertTrue(input.bounds.isEmpty()&&!zombie.hasEffect(effect),"original undead exclusion and inert existing flesh consume no exposure draw");}
            sheep.discard();zombie.discard();
        }
        void gooAndRestoration(){
            var crust=ThaumcraftTaint.CRUST.get().defaultBlockState();set(base,crust);set(base.below(),Blocks.STONE.defaultBlockState());
            for(Direction side:List.of(Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST))set(base.relative(side),ThaumcraftTaint.FLESH.get().defaultBlockState());
            try(var input=new Input(level,1,3,1)){crust.tick(level,base,input.input);test.assertTrue(level.getBlockState(base).is(ThaumcraftFlux.FLUX_GOO.get())&&level.getBlockState(base).getValue(FluxFiniteBlock.LEVEL)==8&&input.bounds.equals(List.of(3,5,3)),"actual sealed crust, including original flesh neighbours, creates exceptional goo8 NINE-unit source");}
            for(Direction side:List.of(Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST))set(base.relative(side),Blocks.AIR.defaultBlockState());
            biome(base,Biomes.PLAINS);set(base,crust);try(var input=new Input(level,1,3,1,0)){crust.tick(level,base,input.input);test.assertTrue(level.getBlockState(base).is(ThaumcraftFlux.FLUX_GOO.get())&&level.getBlockState(base).getValue(FluxFiniteBlock.LEVEL)==8&&input.bounds.equals(List.of(3,5,3,20)),"actual non-TAINT crust cleanup preserves original1/20 goo8 birth");}
            var soil=ThaumcraftTaint.SOIL.get().defaultBlockState();set(base,soil);try(var input=new Input(level,1,3,1,0)){soil.tick(level,base,input.input);test.assertTrue(level.getBlockState(base).is(Blocks.DIRT)&&input.bounds.equals(List.of(3,5,3,10)),"actual non-TAINT soil cleanup is independent1/10 dirt restoration");}
            set(base,Blocks.AIR.defaultBlockState());set(base.above(),Blocks.AIR.defaultBlockState());biome(base,ThaumcraftBiomes.TAINT);
            // A fresh one-chunk corridor deliberately leaves the original32-block admission cube unavailable.
            var remote=new BlockPos((((base.getX()+131072)>>4)<<4)+8,base.getY(),((base.getZ()>>4)<<4)+8);level.getChunk(remote.getX()>>4,remote.getZ()>>4);biome(remote,Biomes.PLAINS);
            for(BlockPos pos:BlockPos.betweenClosed(remote.offset(-1,-4,-1),remote.offset(1,1,1)))set(pos.immutable(),Blocks.AIR.defaultBlockState());
            set(remote.below(5),Blocks.STONE.defaultBlockState());set(remote.above(),Blocks.STONE.defaultBlockState());set(remote,crust);
            test.assertTrue(!level.hasChunksAt(remote.offset(-32,-32,-32),remote.offset(32,32,32)),"dry original fallback fixture retains an actually unavailable32-block cube");
            try(var input=new Input(level,1,3,1,1)){crust.tick(level,remote,input.input);test.assertTrue(level.getBlockState(remote)==crust&&level.isEmptyBlock(remote.below(4))&&input.bounds.equals(List.of(3,5,3,20)),"native dry fallback restores SOURCE coordinate rather than computed landing, then continues original cleanup RNG");}
        }
        void falling(){
            // Remove conversion logs and all anchoring wood from this independent native fall corridor.
            for(BlockPos pos:BlockPos.betweenClosed(base.offset(-3,-3,-3),base.offset(3,10,3)))set(pos.immutable(),Blocks.AIR.defaultBlockState());
            fallSource=base.above(7);landing=fallSource.below(5);set(landing.below(),Blocks.STONE.defaultBlockState());set(fallSource,ThaumcraftTaint.CRUST.get().defaultBlockState());
            set(fallSource.below(),Blocks.OAK_LOG.defaultBlockState());test.assertTrue(!BlockTaint.canFallBelow(level,fallSource.below()),"real log within original27-cell neighbourhood anchors crust");set(fallSource.below(),Blocks.AIR.defaultBlockState());
            set(fallSource.below(),ThaumcraftFlux.FLUX_GOO.get().defaultBlockState().setValue(FluxFiniteBlock.LEVEL,4));test.assertTrue(!BlockTaint.canFallBelow(level,fallSource.below()),"native goo metadata4 is original stopping threshold");set(fallSource.below(),Blocks.AIR.defaultBlockState());
            final int[] attempts={0};observer=(entity,world)->{if(world!=level||!(entity instanceof EntityFallingTaint block)||!block.getSourcePos().equals(fallSource))return EventResult.pass();attempts[0]++;block.tick();test.assertTrue(!block.isDebited()&&level.getBlockState(fallSource).is(ThaumcraftTaint.CRUST.get()),"ADD observer cannot tick a not-yet-admitted source debit");test.assertTrue(!EntityFallingTaint.start(level,fallSource,fallSource),"same-source ADD reentry is bounded by actual falling operation");return EventResult.interruptFalse();};
            EntityEvent.ADD.register(observer);try{test.assertTrue(!EntityFallingTaint.start(level,fallSource,fallSource),"actual canceled native ADD reports refusal");}finally{EntityEvent.ADD.unregister(observer);observer=null;}
            test.assertTrue(attempts[0]==1&&level.getBlockState(fallSource).is(ThaumcraftTaint.CRUST.get()),"actual canceled spawn retains exactly the real source block");
            level.getBlockState(fallSource).tick(level,fallSource,RandomSource.create(0));var spawned=level.getEntitiesOfClass(EntityFallingTaint.class,new AABB(fallSource).inflate(1));test.assertTrue(spawned.size()==1,"registered crust tick naturally reaches the actual falling entity");falling=spawned.getFirst();entities.add(falling);
            test.assertTrue(EntityFallingTaint.start(level,fallSource,fallSource),"second admitted pre-tick falling attempt fixture exists");var duplicate=level.getEntitiesOfClass(EntityFallingTaint.class,new AABB(fallSource).inflate(1)).stream().filter(entity->entity!=falling).findFirst().orElseThrow();entities.add(duplicate);
            double initialY=falling.getY();falling.tick();duplicate.tick();
            test.assertTrue(falling.isDebited()&&level.isEmptyBlock(fallSource)&&duplicate.isRemoved()&&!duplicate.isDebited(),"two admitted falls transfer exactly one source, stale second owner retires without stock");
            test.assertTrue(Math.abs(falling.getY()-(initialY-(double).04F))<1e-10&&Math.abs(falling.getDeltaMovement().y-(-(double).04F*(double).98F))<1e-12&&Math.abs(falling.getBoundingBox().getCenter().y-falling.getY())<1e-10,"actual native falling gravity/move/friction order retains old float widening and centered yOffset");
            var saved=falling.saveWithoutId(new CompoundTag());falling.discard();saved.putByte("Time",(byte)255);falling=reload(saved);set(fallSource,Blocks.GOLD_BLOCK.defaultBlockState());falling.tick();test.assertTrue(falling.getFallTime()==256&&level.getBlockState(fallSource).is(Blocks.GOLD_BLOCK),"actual persisted debited source never claims replacement gold after reload");
            saved=falling.saveWithoutId(new CompoundTag());test.assertTrue(saved.getByte("Time")==0&&saved.getBoolean("Debited"),"original Time is a saved byte while paid custody survives its255 wrap");falling.discard();falling=reload(saved);falling.tick();test.assertTrue(falling.getFallTime()==1&&level.getBlockState(fallSource).is(Blocks.GOLD_BLOCK),"byte-wrapped Time1 does not debit replacement source again");set(fallSource,Blocks.AIR.defaultBlockState());
            var bad=saved.copy();bad.getCompound("BlockState").putString("Name","minecraft:diamond_block");var quarantine=ThaumcraftTaint.FALLING_TAINT.get().create(level);quarantine.load(bad);test.assertTrue(quarantine.isQuarantined(),"invalid stored falling material is preserved rather than fabricated as sand/crust");
            expiring=base.offset(6,0,6);set(expiring,Blocks.STONE.defaultBlockState());var timed=saved.copy();timed.putByte("Time",(byte)100);timed.putUUID("UUID",UUID.randomUUID());var ttl=reload(timed);ttl.setPos(expiring.getX()+.5,level.getMinBuildHeight()-3,expiring.getZ()+.5);ttl.tick();test.assertTrue(ttl.isRemoved(),"actual native out-of-height fall expires after100 even below world bounds");
            // Ordinary registered scheduling, no direct block tick, must kill out-of-biome fibres.
            biome(base,Biomes.PLAINS);set(base.below(),Blocks.STONE.defaultBlockState());set(base,ThaumcraftTaint.FIBRES.get().defaultBlockState().setValue(BlockTaintFibres.TYPE,2));level.scheduleTick(base,ThaumcraftTaint.FIBRES.get(),1);
            test.runAfterDelay(35,()->{try{test.assertTrue(level.isEmptyBlock(base),"ordinary native scheduled tick clears actual non-TAINT fibres");test.assertTrue(falling.isRemoved()&&level.getBlockState(landing).is(ThaumcraftTaint.CRUST.get())&&level.isEmptyBlock(fallSource),"ordinary world/entity ticks settle one actual paid crust at original computed landing");finish();test.succeed();}catch(RuntimeException|Error failure){finish();throw failure;}});
        }
        EntityFallingTaint reload(CompoundTag tag){var entity=ThaumcraftTaint.FALLING_TAINT.get().create(level);entity.load(tag);test.assertTrue(level.addFreshEntity(entity),"native persisted falling owner is admitted after previous UUID retired");entities.add(entity);return entity;}
        void closePlacementPlayer(){
            try{if(placementPlayer!=null){placementPlayer.getInventory().clearContent();try{if(level.getServer().getPlayerList().getPlayer(placementPlayer.getUUID())==placementPlayer)level.getServer().getPlayerList().remove(placementPlayer);}finally{placementPlayer.discard();placementPlayer=null;}}}
            finally{if(placementChannel!=null){placementChannel.finishAndReleaseAll();placementChannel=null;}}
        }
        void finish(){
            if(finished)return;finished=true;if(observer!=null)EntityEvent.ADD.unregister(observer);closePlacementPlayer();entities.forEach(Entity::discard);
            for(var entry:originals.entrySet())level.setBlock(entry.getKey(),entry.getValue(),QUIET);
            var chunks=new java.util.HashSet<ChunkPos>();for(BlockPos column:biomes.keySet())chunks.add(new ChunkPos(QuartPos.toBlock(column.getX())>>4,QuartPos.toBlock(column.getZ())>>4));
            for(ChunkPos pos:chunks){var chunk=level.getChunkSource().getChunkNow(pos.x,pos.z);if(chunk!=null){chunk.fillBiomesFromNoise((x,y,z,ignored)->{var old=biomes.get(new BlockPos(x,0,z));return old==null?chunk.getNoiseBiome(x,y,z):old.get(y-QuartPos.fromBlock(level.getMinBuildHeight()));},level.getChunkSource().randomState().sampler());chunk.setUnsaved(true);}}
            ThaumcraftTaintSettings.configure(settings);for(ChunkPos chunk:forced)level.setChunkForced(chunk.x,chunk.z,false);
        }
    }
    /** Synchronous explicit branch inputs, restored by object identity before any ordinary world tick. */
    private static final class Input implements AutoCloseable {
        final ServerLevel level;final RandomSource original,input;final Field field;final List<Integer> bounds=new ArrayList<>();int index,longDraws;
        Input(ServerLevel level,int... values){this.level=level;original=level.random;
            input=(RandomSource)Proxy.newProxyInstance(RandomSource.class.getClassLoader(),new Class<?>[]{RandomSource.class},(proxy,method,args)->{
                if(method.getName().equals("nextInt")&&args!=null&&args.length==1){int bound=(Integer)args[0];bounds.add(bound);if(index>=values.length)throw new AssertionError("Unexpected native RNG bound "+bound+" after "+bounds);int value=values[index++];if(value<0||value>=bound)throw new AssertionError("Invalid original branch input "+value+"/"+bound);return value;}
                return switch(method.getName()){case "nextFloat"->.5F;case "nextDouble","nextGaussian"->.5D;case "nextLong"->{longDraws++;yield 0L;}case "nextBoolean"->false;case "toString"->"Taint synchronous branch input";case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];default->throw new AssertionError(method);};
            });
            try{field=Level.class.getDeclaredField("random");field.setAccessible(true);field.set(level,input);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        }
        @Override public void close(){try{boolean owned=level.random==input;field.set(level,original);if(!owned||level.random!=original)throw new AssertionError("Original world RNG identity not restored");}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}}
    }
}
