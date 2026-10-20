package thaumcraft.common.testing;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockEssentiaLamp;
import thaumcraft.common.config.ThaumcraftBiologicalLamps;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.lib.GrowthLampPlants;
import thaumcraft.common.lib.GrowthLampTicks;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileArcaneLampFertility;
import thaumcraft.common.tiles.TileArcaneLampGrowth;
import thaumcraft.common.tiles.TileEssentiaLamp;
import thaumcraft.common.tiles.TileJar;

/** Declared herba/victus jars, one native CropBlock subclass and real animals.
 * Tickers advance synchronously except the actual persistent +1 tick plant job.
 * No injected paid lamp charges, counters, reserve, scan phase or production RNG. */
public final class BiologicalLampRuntimeChecks {
    private static ProbeCrop crop;
    private BiologicalLampRuntimeChecks() {}
    public static synchronized void registerTestProbes() {
        if (crop == null) crop = Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("thaumcraft_gametest", "growth_lamp_native_crop"), new ProbeCrop());
    }

    public static void growth(GameTestHelper test) {
        if (crop == null) throw new IllegalStateException("Game-test bootstrap must register the isolated native crop probe");
        BlockPos origin = test.absolutePos(new BlockPos(3, 3, 3));
        var forced = GolemChunks.hold(test.getLevel(), List.of(origin));
        Fixture f = null;
        try {
            f = fixture(test); f.open();
            growthFuelAndLifecycle(f); recipes(f); f.reset();
            var jar = jar(f, f.base.below(), Aspect.PLANT, 2); var lamp = growthLamp(f, f.base);
            for (int i=0;i<10 && (!lamp.hasReserve() || lamp.getCharges() != 100);++i) tick(f,lamp);
            f.check(jar.getAmount()==0 && lamp.getCharges()==100 && lamp.hasReserve(),"real two-herba payment supplies100 active updates plus exactly one reserve");
            // Advance an entire fresh scan cycle with NO plants; this reaches the original last-column crash deterministically.
            int before=lamp.getRemainingColumns(); int untilEmpty=before==0?169:before;
            for(int i=0;i<untilEmpty;i++)tick(f,lamp);
            f.check(lamp.getRemainingColumns()==0 && lamp.getCharges()==100,"all169 shuffled columns complete without original remove/get index169 crash or fuel debit for empty air");
            BlockPos target=f.base.east(2);
            f.level.setBlock(target.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7),TubeVariantsRuntimeChecks.QUIET);
            f.put(target,crop); crop.calls=0; crop.bounds.clear();
            f.check(!f.level.getBlockState(target).isRandomlyTicking()
                            && !GrowthLampTicks.schedule(lamp,target,f.level.getBlockState(target)) && lamp.getCharges()==100,
                    "ambient ticks are excluded only on the test crop; public scheduler refuses a job outside prepared native growth phase");
            f.check(GrowthLampPlants.eligible(f.level,target,f.level.getBlockState(target))
                            && !GrowthLampPlants.eligible(f.level,target,crop.defaultBlockState().setValue(CropBlock.AGE,7))
                            && !GrowthLampPlants.eligible(f.level,target,Blocks.GRASS_BLOCK.defaultBlockState()),
                    "actual native maturity and original grass-material exclusion");
            for(int i=0;i<169 && lamp.getCharges()==100;i++)tick(f,lamp);
            f.check(lamp.getCharges()==99 && crop.calls==0 && f.level.getBlockState(target).getValue(CropBlock.AGE)==0,
                    "one eligible shuffled column pays one charge and SCHEDULES, never immediate bonemeal/randomTick");
            var factory=new SavedData.Factory<GrowthLampTicks>(GrowthLampTicks::new,GrowthLampTicks::load,null);
            var queue=f.level.getDataStorage().get(factory,"thaumcraft_growth_lamp_ticks");
            var saved=queue.save(new CompoundTag(),f.level.registryAccess());
            f.check(saved.getList("Pending",Tag.TAG_COMPOUND).stream().anyMatch(raw->((CompoundTag)raw).getLong("Pos")==target.asLong()),"paid plant job is real persistent SavedData");
            f.level.getDataStorage().set("thaumcraft_growth_lamp_ticks",GrowthLampTicks.load(saved,f.level.registryAccess()));
            GrowthLampTicks.tick(f.level);
            f.check(crop.calls==0,"same game tick cannot execute the original +1 scheduled update");
            f.check(f.level.destroyBlock(f.base,true),"lamp is actually removed before its paid native update");
            Fixture owned=f;
            test.runAfterDelay(2,()->{
                try {
                    GrowthLampTicks.tick(owned.level);
                    owned.check(crop.calls==1 && !crop.bounds.isEmpty() && owned.level.getBlockState(target).getValue(CropBlock.AGE)==1,
                            "exactly ONE real inherited CropBlock.randomTick grows0->1 after native time, save/load and lamp removal");
                    GrowthLampTicks.tick(owned.level);
                    owned.check(crop.calls==1,"paid scheduled plant update cannot replay");
                    owned.drops(); test.succeed();
                } finally {owned.close(); for(var chunk:forced)test.getLevel().setChunkForced(chunk.x,chunk.z,false);}
            });
        } catch(RuntimeException|Error failure) {
            if(f!=null)f.close(); for(var chunk:forced)test.getLevel().setChunkForced(chunk.x,chunk.z,false); throw failure;
        }
    }

    private static void growthFuelAndLifecycle(Fixture f) {
        var jar=jar(f,f.base.below(),Aspect.PLANT,2);var lamp=growthLamp(f,f.base);
        f.check(lamp.getCharges()==-1 && !lamp.hasReserve() && lamp.getSuctionAmount(Direction.DOWN)==128
                        && lamp.getSuctionAmount(Direction.UP)==0 && lamp.getEssentiaAmount(null)==0 && lamp.getEssentiaType(null)==null
                        && lamp.addEssentia(Aspect.PLANT,1,Direction.DOWN)==0 && lamp.takeEssentia(Aspect.PLANT,1,Direction.DOWN)==0,
                "original no public stored essentia; only support-facing herba suction128");
        f.check(EssentiaTransfers.pullLampOne(lamp).committedUnits()==0 && lamp.prepareInsert(lamp.snapshotAtomicState(),Aspect.PLANT,1)==null,
                "closed lamp draw and raw preparation refuse an unprepared public call");
        tick(f,lamp);tick(f,lamp);
        f.check(lamp.getDrawDelay()==4 && jar.getAmount()==2 && lamp.getCharges()==-1,"empty growth lamp increments drawDelay TWICE each native tick; first four calls pay nothing");
        tick(f,lamp);
        f.check(lamp.getCharges()==100 && !lamp.hasReserve() && jar.getAmount()==1 && lamp.getDrawDelay()==6,"fifth call at tick3 pays exact first herba before same-tick sixth reserve attempt");
        for(int i=0;i<4;i++)tick(f,lamp);
        f.check(lamp.hasReserve() && jar.getAmount()==0 && lamp.getCharges()==100 && lamp.getSuctionAmount(Direction.DOWN)==0
                        && lamp.getBlockState().getLightEmission()==15,"tick7 call10 pays reserve, saturation stops suction, active lamp emits15");
        var saved=lamp.saveWithFullMetadata(f.level.registryAccess());
        var old=lamp;lamp=NodeTransductionRuntimeChecks.reload(f,lamp,saved,TileArcaneLampGrowth.class);
        f.check(lamp.getCharges()==100 && lamp.hasReserve() && lamp.getDrawDelay()==0,"actual registered native reload keeps paid fuel/reserve, resets only original transient delay");
        tick(f,old);f.check(lamp.getCharges()==100 && lamp.hasReserve(),"retired lamp ticker cannot alter replacement");
        var bad=saved.copy();bad.putInt("charges",101);lamp=NodeTransductionRuntimeChecks.reload(f,lamp,bad,TileArcaneLampGrowth.class);
        f.check(lamp.isQuarantined() && lamp.getSuctionAmount(Direction.DOWN)==0
                        && lamp.saveWithoutMetadata(f.level.registryAccess()).getCompound("RejectedEssentiaLamp").equals(bad),
                "malformed paid charge is preserved disabled, never clamped to a free100");
        f.reset();jar=jar(f,f.base.below(),Aspect.LIFE,2);lamp=growthLamp(f,f.base);
        for(int i=0;i<10;i++)tick(f,lamp);
        f.check(jar.getAmount()==2 && lamp.getCharges()==-1,"wrong victus cannot supply growth herba fuel");
        f.level.setBlock(f.base.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        f.check(!f.level.getBlockState(f.base).is(ThaumcraftBiologicalLamps.GROWTH_LAMP.get()),"original Growth loses support and drops");
        f.drops();
    }

    public static void fertility(GameTestHelper test) {
        BlockPos origin=test.absolutePos(new BlockPos(3,3,3));var forced=GolemChunks.hold(test.getLevel(),List.of(origin));
        var animals=new ArrayList<Animal>();
        try(var f=fixture(test)) {
            f.open();var jar=jar(f,f.base.below(),Aspect.LIFE,4);f.put(f.base,ThaumcraftBiologicalLamps.FERTILITY_LAMP.get());
            var lamp=(TileArcaneLampFertility)f.level.getBlockEntity(f.base);
            for(int i=0;i<8;i++){var cow=EntityType.COW.create(f.level);f.check(cow!=null,"native cow fixture");cow.setPos(f.base.getX()+2+(i%3)*.25,f.base.getY(),f.base.getZ()+2+(i/3)*.25);cow.setNoAi(true);cow.setNoGravity(true);cow.setAge(0);f.check(f.level.addFreshEntity(cow),"owned real adult animal enters world index");animals.add(cow);}
            for(int i=0;i<9;i++)tick(f,lamp);
            f.check(lamp.getCharges()==1 && jar.getAmount()==3 && lamp.getPairingCount()==0 && animals.stream().noneMatch(Animal::isInLove),"one victus is insufficient; eligible pairing clock remains0");
            tick(f,lamp);
            f.check(lamp.getCharges()==2 && lamp.getPairingCount()==1 && animals.stream().noneMatch(Animal::isInLove),"first eligible count0 checks immediately, but original same-class density8 refuses pairing");
            for(int i=0;i<10;i++)tick(f,lamp);
            f.check(lamp.getCharges()==4 && jar.getAmount()==0 && lamp.getPairingCount()==11 && lamp.getSuctionAmount(Direction.DOWN)==88,"native fill caps at four victus; original suction remains88 although automatic draw stops");
            animals.removeLast().discard();
            for(int i=0;i<289;i++)tick(f,lamp);
            f.check(lamp.getPairingCount()==300 && lamp.getCharges()==4 && animals.stream().noneMatch(Animal::isInLove),"count11..299 is exactly289 updates; density relief does not invent an immediate attempt");
            tick(f,lamp);
            f.check(lamp.getPairingCount()==301 && lamp.getCharges()==2 && animals.stream().filter(Animal::isInLove).count()==2,"count300 spends exact two victus on ONE same-class adult pair");
            var saved=lamp.saveWithFullMetadata(f.level.registryAccess());lamp=NodeTransductionRuntimeChecks.reload(f,lamp,saved,TileArcaneLampFertility.class);
            f.check(lamp.getCharges()==2 && lamp.getPairingCount()==0,"real save/load persists paid charges but original count is transient");
            tick(f,lamp);
            f.check(lamp.getCharges()==0 && animals.stream().filter(Animal::isInLove).count()==4,"reloaded first eligible count0 pairs one other pair with retained two, never original already-in-love pair");
            f.level.setBlock(f.base.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            f.check(f.level.getBlockEntity(f.base)==lamp && lamp.getBlockState().getLightEmission()==8,"original Fertility remains after support loss; empty emission8");
            recipes(f);
        } finally {for(var animal:animals)animal.discard();for(var chunk:forced)test.getLevel().setChunkForced(chunk.x,chunk.z,false);}
        test.succeed();
    }

    private static void recipes(Fixture f) {
        for(boolean growth:new boolean[]{true,false}) {
            String path=growth?"growth_lamp":"fertility_lamp";var recipe=(InfusionStackRecipe)f.level.getRecipeManager().byKey(id("infusion/"+path)).orElseThrow().value();
            var expected=growth?List.of(Items.GOLD_INGOT,Items.BONE_MEAL,ThaumcraftContent.SHARDS.get("terra").get(),Items.GOLD_INGOT,Items.BONE_MEAL,ThaumcraftContent.SHARDS.get("terra").get())
                    :List.of(Items.GOLD_INGOT,Items.WHEAT,ThaumcraftContent.SHARDS.get("ignis").get(),Items.GOLD_INGOT,Items.GOLDEN_CARROT,ThaumcraftContent.SHARDS.get("ignis").get());
            f.check(recipe.research().equals(growth?"LAMPGROWTH":"LAMPFERTILITY") && recipe.instability()==4
                            && recipe.aspects().equals(growth?Map.of("herba",16,"lux",8,"victus",16):Map.of("bestia",16,"victus",16,"lux",8))
                            && recipe.components().size()==6 && recipe.result().is(growth?ThaumcraftBiologicalLamps.GROWTH_LAMP_ITEM.get():ThaumcraftBiologicalLamps.FERTILITY_LAMP_ITEM.get()),"original native infusion research/result/instability4/aspects: "+path);
            for(int i=0;i<6;i++)f.check(recipe.components().get(i).test(new ItemStack(expected.get(i))),"exact original ordered infusion component "+path+"/"+i);
        }
    }
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    private static Fixture fixture(GameTestHelper test){
        var f=NodeTransductionRuntimeChecks.fixture(test,false);
        for(BlockPos mutable:BlockPos.betweenClosed(f.base.offset(-7,-7,-7),f.base.offset(7,7,7))){
            BlockPos pos=mutable.immutable();if(f.states.containsKey(pos))continue;
            f.check(!f.level.isOutsideBuildHeight(pos) && f.level.hasChunkAt(pos),"entire lamp scan sphere belongs to loaded/restored fixture");
            f.states.put(pos,f.level.getBlockState(pos));var tile=f.level.getBlockEntity(pos);if(tile!=null)f.originals.put(pos,tile);
        }
        return f;
    }
    private static TileJar jar(Fixture f,BlockPos pos,Aspect fuel,int amount){f.put(pos,ThaumcraftContent.JAR.get());var jar=(TileJar)f.level.getBlockEntity(pos);f.check(jar.addToContainer(fuel,amount)==0,"declared actual native jar fuel stock");return jar;}
    private static TileArcaneLampGrowth growthLamp(Fixture f,BlockPos pos){f.put(pos,ThaumcraftBiologicalLamps.GROWTH_LAMP.get());return(TileArcaneLampGrowth)f.level.getBlockEntity(pos);}
    private static void tick(Fixture f,TileEssentiaLamp lamp){TileEssentiaLamp.tick(f.level,lamp.getBlockPos(),lamp.getBlockState(),lamp);}
    /** Native CropBlock behavior, with ONLY its declared random growth branch input forced to zero.
     * The scheduler still calls the actual registered block; all real inherited CropBlock growth/soil/age code runs. */
    private static final class ProbeCrop extends CropBlock {
        int calls;final List<Integer> bounds=new ArrayList<>();
        ProbeCrop(){super(BlockBehaviour.Properties.of().noCollission().instabreak());} // Explicit fixture disables unrelated ambient random ticks.
        @Override protected boolean isRandomlyTicking(BlockState state){return false;}
        @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
            calls++;
            RandomSource input=(RandomSource)Proxy.newProxyInstance(RandomSource.class.getClassLoader(),new Class<?>[]{RandomSource.class},(proxy,method,args)->{
                if(method.getName().equals("nextInt") && args!=null && args.length==1){bounds.add((Integer)args[0]);return 0;}
                if(method.getDeclaringClass()==Object.class)return switch(method.getName()){case"hashCode"->System.identityHashCode(proxy);case"equals"->proxy==args[0];default->"declared growth-only branch input";};
                throw new AssertionError("Unexpected native crop random draw: "+method);
            });
            super.randomTick(state,level,pos,input);
        }
    }
}
