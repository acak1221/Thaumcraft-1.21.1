package thaumcraft.common.testing;

import dev.architectury.fluid.FluidStack;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.lib.pech.PechDrops;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileSpa;

/** Actual connected native Item.use, empty bucket mixin, six-face source ray,
 * Nether, finite overflow and original Spa registry mapping. All input blocks
 * and buckets are declared fixtures; no recipe/acquisition/client art claim. */
public final class PureBucketRuntimeChecks {
    private PureBucketRuntimeChecks(){}
    public static void run(GameTestHelper test){
        try(var f=new Fixture(test)){
            f.open();capture(f);placement(f);overflow(f);spa(f);nether(f);
        }
        test.succeed();
    }
    private static BlockState pure(int metadata){return ThaumcraftSpa.PURE_WATER.get().defaultBlockState().setValue(LiquidBlock.LEVEL,metadata);}
    private static void set(ServerLevel level,BlockPos pos,BlockState state){level.setBlock(pos,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);}
    private static InteractionResult use(Fixture f,InteractionHand hand){return f.player.gameMode.useItem(f.player,f.player.level(),f.player.getItemInHand(hand),hand);}
    private static void aim(Fixture f,BlockPos pos,Direction face){
        var eye=Vec3.atCenterOf(pos).relative(face,2);var to=Vec3.atCenterOf(pos).subtract(eye);
        f.player.setPos(eye.x,eye.y-f.player.getEyeHeight(),eye.z);f.player.setDeltaMovement(Vec3.ZERO);
        f.player.setYRot((float)(Math.toDegrees(Math.atan2(to.z,to.x))-90));
        f.player.setXRot((float)-Math.toDegrees(Math.atan2(to.y,Math.sqrt(to.x*to.x+to.z*to.z))));
    }
    private static void capture(Fixture f){
        f.reset();set(f.level,f.base,pure(0));aim(f,f.base,Direction.SOUTH);
        f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        var empty=new ItemStack(Items.BUCKET);empty.set(DataComponents.CUSTOM_NAME,Component.literal("actual named empty input"));
        f.player.setItemInHand(InteractionHand.OFF_HAND,empty);
        long seed=0x4b00cL;f.level.random.setSeed(seed);
        f.check(use(f,InteractionHand.OFF_HAND).consumesAction()&&f.level.getBlockState(f.base).isAir()
                &&f.player.getOffhandItem().is(ThaumcraftSpa.BUCKET_PURE.get())&&f.player.getOffhandItem().getCount()==1
                &&!f.player.getOffhandItem().has(DataComponents.CUSTOM_NAME)&&f.player.getMainHandItem().is(Items.STICK),
                "actual offhand EMPTY bucket source ray captures one real Pure source into a plain filled bucket and preserves main hand");
        f.check(f.level.random.nextLong()==RandomSource.create(seed).nextLong(),"native Pure capture does not invent world sound/particle RNG draws");
        f.check(f.player.getOffhandItem().getMaxStackSize()==1&&!ThaumcraftSpa.BUCKET_PURE.get().hasCraftingRemainingItem()
                &&ThaumcraftSpa.PURE_FLUID_SOURCE.get().getBucket()==ThaumcraftSpa.BUCKET_PURE.get(),"original max1 container has real bidirectional fluid identity and no invented crafting remainder");
        for(int meta=1;meta<=7;meta++){
            set(f.level,f.base,pure(meta));f.player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.BUCKET));
            f.check(!use(f,InteractionHand.OFF_HAND).consumesAction()&&f.player.getOffhandItem().is(Items.BUCKET)&&f.level.getBlockState(f.base)==pure(meta)
                    &&ThaumcraftSpa.PURE_WATER.get().pickupBlock(f.player,f.level,f.base,pure(meta)).isEmpty()&&f.level.getBlockState(f.base)==pure(meta),
                    "actual source-only ray and native BucketPickup both reject flowing metadata without consumption: "+meta);
        }
        f.player.getInventory().clearContent();f.mode(GameType.CREATIVE);set(f.level,f.base,pure(0));
        f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET,3));
        f.check(use(f,InteractionHand.MAIN_HAND).consumesAction()&&f.level.getBlockState(f.base).isAir()
                &&f.player.getMainHandItem().is(Items.BUCKET)&&f.player.getMainHandItem().getCount()==3
                &&f.player.getInventory().countItem(ThaumcraftSpa.BUCKET_PURE.get())==0,
                "actual creative Forge ALLOW removes source yet keeps the exact empty stack and produces no filled inventory item");
        f.mode(GameType.SURVIVAL);set(f.level,f.base,pure(0));f.player.getInventory().clearContent();
        f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET,2));
        f.check(use(f,InteractionHand.MAIN_HAND).consumesAction()&&f.player.getMainHandItem().is(Items.BUCKET)&&f.player.getMainHandItem().getCount()==1
                &&f.player.getInventory().countItem(ThaumcraftSpa.BUCKET_PURE.get())==1,"actual stack2 pickup debits one empty and routes one filled result to inventory");
        f.reset();set(f.level,f.base,Blocks.WATER.defaultBlockState());aim(f,f.base,Direction.SOUTH);
        f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
        f.check(use(f,InteractionHand.MAIN_HAND).consumesAction()&&f.player.getMainHandItem().is(Items.WATER_BUCKET),"scoped Pure mixin leaves actual ordinary vanilla water capture functional");
    }
    private static void placement(Fixture f){
        for(Direction face:Direction.values()){
            f.reset();set(f.level,f.base,pure(0));if(face==Direction.DOWN)f.put(f.base.below(2),Blocks.AIR);aim(f,f.base,face);
            var filled=new ItemStack(ThaumcraftSpa.BUCKET_PURE.get());filled.set(DataComponents.CUSTOM_NAME,Component.literal("one physical full input"));
            f.player.setItemInHand(InteractionHand.MAIN_HAND,filled);
            f.check(use(f,InteractionHand.MAIN_HAND).consumesAction()&&f.level.getBlockState(f.base)==pure(0)&&f.level.getBlockState(f.base.relative(face))==pure(0)
                    &&f.player.getMainHandItem().is(Items.BUCKET)&&f.player.getMainHandItem().getCount()==1&&!f.player.getMainHandItem().has(DataComponents.CUSTOM_NAME),
                    "actual filled source-only ray hits fluid and always offsets the clicked face; paid plain remainder: "+face);
        }
        f.reset();f.put(f.base,Blocks.STONE);f.put(f.base.south().below(),Blocks.STONE);f.put(f.base.south(),Blocks.OAK_PRESSURE_PLATE);aim(f,f.base,Direction.SOUTH);
        // The source-only ray passes above the low plate and hits stone. Its
        // offset target still has original solid WOOD material, despite a thin
        // modern collision/outline and an otherwise replaceable native profile.
        f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ThaumcraftSpa.BUCKET_PURE.get()));
        f.check(!use(f,InteractionHand.MAIN_HAND).consumesAction()&&f.player.getMainHandItem().is(ThaumcraftSpa.BUCKET_PURE.get())
                &&f.level.getBlockState(f.base.south()).is(Blocks.OAK_PRESSURE_PLATE),"actual filled bucket refuses original solid material in the ray's offset target instead of native replacement/waterlogging");
        f.reset();f.put(f.base,Blocks.STONE);f.put(f.base.south().below(),Blocks.DIRT);f.put(f.base.south(),Blocks.DANDELION);
        f.player.setPos(f.base.getX()+.5,f.base.getY()+.95-f.player.getEyeHeight(),f.base.getZ()+2.5);f.player.setYRot(180);f.player.setXRot(0);
        f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ThaumcraftSpa.BUCKET_PURE.get()));
        int[] drops={0};boolean[] paidBeforeDrop={true};EntityEvent.Add reenter=(entity,world)->{
            if(world==f.level&&entity instanceof ItemEntity item&&item.getItem().is(Items.DANDELION)){
                drops[0]++;paidBeforeDrop[0]&=f.player.getMainHandItem().is(Items.BUCKET);use(f,InteractionHand.MAIN_HAND);
            }return EventResult.pass();
        };
        EntityEvent.ADD.register(reenter);
        try{
            f.check(use(f,InteractionHand.MAIN_HAND).consumesAction()&&f.level.getBlockState(f.base.south())==pure(0)
                    &&f.player.getMainHandItem().is(Items.BUCKET)&&paidBeforeDrop[0]&&drops[0]==1
                    &&f.drops().stream().filter(item->item.getItem().is(Items.DANDELION)).mapToInt(item->item.getItem().getCount()).sum()==1,
                    "actual nonsolid plant displacement pays the filled bucket before native ADD can reenter and publishes exactly one real plant drop");
        }finally{EntityEvent.ADD.unregister(reenter);f.drops().forEach(ItemEntity::discard);}
        f.reset();f.put(f.base,Blocks.STONE);aim(f,f.base,Direction.SOUTH);
        f.mode(GameType.CREATIVE);var creative=new ItemStack(ThaumcraftSpa.BUCKET_PURE.get());creative.set(DataComponents.CUSTOM_NAME,Component.literal("creative original"));f.player.setItemInHand(InteractionHand.MAIN_HAND,creative);
        f.check(use(f,InteractionHand.MAIN_HAND).consumesAction()&&f.level.getBlockState(f.base.south())==pure(0)&&ItemStack.matches(creative,f.player.getMainHandItem()),"actual creative full bucket places real source and retains its original item/components");
        f.mode(GameType.SURVIVAL);
    }
    private static void overflow(Fixture f){
        f.reset();set(f.level,f.base,pure(0));aim(f,f.base,Direction.SOUTH);
        for(int slot=0;slot<36;slot++)f.player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));
        f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET,2));
        long seed=0xb0ce7L;f.player.getRandom().setSeed(seed);var expected=RandomSource.create(seed);for(int i=0;i<4;i++)expected.nextFloat();
        f.check(use(f,InteractionHand.MAIN_HAND).consumesAction()&&f.player.getMainHandItem().getCount()==1&&f.level.getBlockState(f.base).isAir(),"actual full inventory source pickup still pays one empty and captures one source");
        var rows=f.pendingRows();f.check(rows.size()==1&&rows.getFirst().getInt("PickupDelay")==40&&!rows.getFirst().getBoolean("PechDrop")
                &&ItemStack.parse(f.level.registryAccess(),rows.getFirst().getCompound("Stack")).orElseThrow().is(ThaumcraftSpa.BUCKET_PURE.get())
                &&f.player.getRandom().nextLong()==expected.nextLong(),"one paid overflow is durably owned with exact4PLAYER float throw draws/delay40 and no Pech marker");
        f.flushFresh();var drops=f.drops();
        f.check(drops.size()==1&&drops.getFirst().getItem().is(ThaumcraftSpa.BUCKET_PURE.get())&&drops.getFirst().getItem().getCount()==1
                &&!PechDrops.isMarked(drops.getFirst())&&f.level.getEntity(drops.getFirst().getUUID())==drops.getFirst(),"native queue admits exactly one actual filled overflow entity without granting Pech pickup immunity");
        drops.forEach(net.minecraft.world.entity.item.ItemEntity::discard);
    }
    private static void spa(Fixture f){
        f.reset();f.put(f.base,ThaumcraftSpa.ARCANE_SPA.get());var spa=(TileSpa)f.level.getBlockEntity(f.base);long bucket=FluidStack.bucketAmount();
        try{
            for(GameType mode:new GameType[]{GameType.SURVIVAL,GameType.CREATIVE}){
                f.mode(mode);spa.drain(null,Long.MAX_VALUE,true);f.player.getInventory().clearContent();
                var filled=new ItemStack(ThaumcraftSpa.BUCKET_PURE.get());filled.set(DataComponents.CUSTOM_NAME,Component.literal("original container registry ignores item NBT"));
                var mapped=TileSpa.filledContainer(filled);
                f.check(mapped!=null&&mapped.fluid().getFluid()==ThaumcraftSpa.PURE_FLUID_SOURCE.get()&&mapped.fluid().getAmount()==bucket&&mapped.fluid().getPatch().isEmpty()
                        &&mapped.empty().is(Items.BUCKET)&&mapped.empty().getCount()==1,"real Pure filled item has original registered untagged1000mB fluid and one plain empty remainder");
                f.player.setItemInHand(InteractionHand.OFF_HAND,filled);f.aim(Vec3.atCenterOf(f.base));
                f.check(f.use(f.base,Direction.SOUTH,InteractionHand.OFF_HAND).consumesAction()&&spa.getFluidAmountNative()==bucket
                        &&spa.fluid().getFluid()==ThaumcraftSpa.PURE_FLUID_SOURCE.get()&&f.player.getOffhandItem().isEmpty()
                        &&f.player.getInventory().countItem(Items.BUCKET)==1,"actual Spa offhand RMB consumes one physical Pure bucket even in original creative and returns one empty: "+mode);
            }
        }finally{spa.evacuate();f.flushFresh();f.drops();f.put(f.base,Blocks.AIR);}
    }
    private static void nether(Fixture f){
        f.reset();ServerLevel nether=f.level.getServer().getLevel(Level.NETHER);f.check(nether!=null&&nether.dimensionType().ultraWarm(),"real server Nether exists and is actually ultraWarm");
        BlockPos root=new BlockPos(f.base.getX(),Math.min(200,nether.getMaxBuildHeight()-8),f.base.getZ());Map<BlockPos,BlockState> before=new LinkedHashMap<>();
        nether.getChunk(root.getX()>>4,root.getZ()>>4);
        for(BlockPos mutable:BlockPos.betweenClosed(root.offset(-2,-2,-2),root.offset(2,2,3))){var pos=mutable.immutable();nether.getChunk(pos.getX()>>4,pos.getZ()>>4);f.check(nether.getBlockEntity(pos)==null,"declared Nether bucket lane has no existing block owner");before.put(pos,nether.getBlockState(pos));set(nether,pos,Blocks.AIR.defaultBlockState());}
        try{
            set(nether,root,Blocks.STONE.defaultBlockState());var eye=Vec3.atCenterOf(root).add(0,0,2);
            f.player.teleportTo(nether,eye.x,eye.y-f.player.getEyeHeight(),eye.z,180,0);f.mode(GameType.SURVIVAL);
            f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ThaumcraftSpa.BUCKET_PURE.get()));
            f.check(use(f,InteractionHand.MAIN_HAND).consumesAction()&&nether.getBlockState(root.south())==pure(0)&&f.player.getMainHandItem().is(Items.BUCKET),"actual native filled use places a paid Pure source in real ultraWarm Nether without vanilla evaporation");
        }finally{
            f.player.teleportTo(f.level,f.base.getX()+.5,f.base.getY()+.5-f.player.getEyeHeight(),f.base.getZ()+2.5,180,0);
            before.forEach((pos,state)->set(nether,pos,state));
        }
    }
}
