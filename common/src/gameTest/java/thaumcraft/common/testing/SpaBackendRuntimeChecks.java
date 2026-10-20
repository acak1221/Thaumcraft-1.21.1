package thaumcraft.common.testing;

import dev.architectury.fluid.FluidStack;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.container.ContainerSpa;
import thaumcraft.common.testing.TubeVariantsRuntimeChecks.Fixture;
import thaumcraft.common.tiles.TileSpa;

/** Native current Spa block/ticker, paid sources, exact fluid patches and real inventory RMB.
 * Fluids/salts are declared finite input fixtures, not survival acquisition or elapsed client time.
 * Root binds two isolated wrappers, timeout200 each. */
public final class SpaBackendRuntimeChecks {
    private SpaBackendRuntimeChecks(){}
    public static void firstCycleGridOrderRedstoneAndReload(GameTestHelper test){
        try(var f=new Fixture(test);var cleanup=new Cleanup(f)){
            f.open();floor(f);f.put(f.base,ThaumcraftSpa.ARCANE_SPA.get());var spa=spa(f);long bucket=FluidStack.bucketAmount();
            var salt=new ItemStack(ThaumcraftSpa.BATH_SALTS.get(),64);salt.set(DataComponents.CUSTOM_NAME,Component.literal("all original salts retained"));spa.setItem(0,salt);
            f.check(spa.getMix()&&spa.fill(Direction.UP,FluidStack.create(Fluids.WATER,5*bucket),true)==5*bucket
                            &&!spa.canFill(Direction.UP,Fluids.WATER)&&!spa.canDrain(Direction.UP,Fluids.WATER),
                    "new actual Spa defaults mix=true; direct original top fill ignores advertised automation top restriction");
            var detached=spa.fluid();detached.setAmount(1);f.check(spa.getFluidAmountNative()==5*bucket&&spa.getCapacityNative()==5*bucket&&spa.getFluidAmountMb()==5000,"tank exposes a defensive copy and exact native5000mB capacity");
            tick(f,spa,1);
            f.check(source(f,f.base.above())&&spa.getFluidAmountNative()==4*bucket&&spa.getItem(0).getCount()==63,
                    "first actual server ticker starts counter0 and places paid source immediately above Spa");
            tick(f,spa,39);f.check(sources(f)==1&&spa.getFluidAmountNative()==4*bucket,"next39 genuine server work calls add no source or debit");
            tick(f,spa,1);f.check(source(f,f.base.above().west())&&sources(f)==2&&spa.getFluidAmountNative()==3*bucket&&spa.getItem(0).getCount()==62,
                    "original X-major thenZ-major 5x5 first adjacent choice is west, not a diagonal or radial fill");
            tick(f,spa,40);f.check(source(f,f.base.above().west(2))&&sources(f)==3&&spa.getFluidAmountNative()==2*bucket,"next original cycle extends to the X=-2 boundary through a real full source neighbor");
            f.put(f.base.east(),Blocks.REDSTONE_BLOCK);f.check(f.level.hasNeighborSignal(f.base),"actual native redstone block powers Spa");
            tick(f,spa,40);f.check(sources(f)==3&&spa.getFluidAmountNative()==2*bucket,"powered40th work phase does not spend or place");
            f.put(f.base.east(),Blocks.STONE);tick(f,spa,39);f.check(sources(f)==3,"unpowering preserves original phase instead of resetting an immediate timer");
            tick(f,spa,1);f.check(source(f,f.base.above().offset(-2,0,-1))&&sources(f)==4&&spa.getFluidAmountNative()==bucket&&spa.getItem(0).getCount()==60,
                    "unpowered original phase resumes with precise next X/Z adjacent source and conserved water/salts");
            var saved=spa.saveWithFullMetadata(f.level.registryAccess());f.check(!saved.contains("PaidRelease"),"actual completed placement retires its private paid intent before any later save");
            var old=spa;spa=NodeTransductionRuntimeChecks.reload(f,old,saved,TileSpa.class);
            f.check(spa.getFluidAmountNative()==bucket&&spa.getItem(0).getCount()==60&&spa.getMix(),"actual registered BE load preserves remaining finite fluid and named salt inventory");
            tick(f,old,1);f.check(sources(f)==4&&spa.getFluidAmountNative()==bucket,"detached old owner cannot advance its replacement or place fluid");
            f.check(old.getItem(0).isEmpty()&&old.removeItemNoUpdate(0).isEmpty()&&!old.canPlaceItem(0,salt)
                            &&spa.getItem(0).getCount()==60,
                    "native reload retires old inventory access as well as work; no stale reference can extract the replacement's saved salt stock");
            tick(f,spa,1);f.check(source(f,f.base.above().offset(-2,0,-2))&&sources(f)==5&&spa.fluid().isEmpty()&&spa.getItem(0).getCount()==59,
                    "original transient counter restarts0 on native reload; only the last owned bucket and one salt fund one new source");
            tick(f,spa,80);f.check(sources(f)==5&&spa.getItem(0).getCount()==59,"empty tank never spends remaining salts or generates a free source");
            retire(f,spa);
        }
        test.succeed();
    }
    public static void containerQuirksPatchIdentityAndNativePersistence(GameTestHelper test){
        try(var f=new Fixture(test);var cleanup=new Cleanup(f)){
            f.open();floor(f);f.put(f.base,ThaumcraftSpa.ARCANE_SPA.get());var spa=spa(f);long bucket=FluidStack.bucketAmount();
            f.check(spa.fill(null,FluidStack.create(Fluids.WATER,bucket*9/2),true)==bucket*9/2,"declared finite4500mB Spa input");
            f.mode(GameType.CREATIVE);var filled=new ItemStack(Items.WATER_BUCKET);filled.set(DataComponents.CUSTOM_NAME,Component.literal("one physical creative bucket"));f.player.setItemInHand(InteractionHand.MAIN_HAND,filled);
            f.check(f.use(f.base).consumesAction()&&spa.getFluidAmountNative()==5*bucket&&f.player.getInventory().countItem(Items.WATER_BUCKET)==0&&f.player.getInventory().countItem(Items.BUCKET)==1,
                    "real creative RMB inserts only500mB but consumes WHOLE water bucket and returns one empty, preserving original500mB spill");
            var full=spa.fluid();f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LAVA_BUCKET));var lava=f.player.getMainHandItem().copy();
            f.check(f.use(f.base).consumesAction()&&ItemStack.matches(lava,f.player.getMainHandItem())&&spa.fluid().isFluidStackEqual(full)
                            &&!(f.player.containerMenu instanceof ContainerSpa),"full/wrong-fluid actual filled-container click keeps its source and does not open GUI");
            f.player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));f.check(f.use(f.base).consumesAction()&&f.player.containerMenu instanceof ContainerSpa&&spa.fluid().isFluidStackEqual(full),"empty bucket opens actual native Spa menu instead of inventing direct tank draining");f.player.closeContainer();
            var drained=spa.drain(Direction.UP,Long.MAX_VALUE,true);f.check(drained.getAmount()==5*bucket&&spa.fluid().isEmpty(),"direct original drain ignores side and retains exact native amount");
            f.mode(GameType.SURVIVAL);var waterBottle=PotionContents.createItemStack(Items.POTION,Potions.WATER);f.player.setItemInHand(InteractionHand.MAIN_HAND,waterBottle);
            f.check(f.use(f.base).consumesAction()&&spa.getFluidAmountNative()==bucket&&f.player.getInventory().countItem(Items.POTION)==0&&f.player.getInventory().countItem(Items.GLASS_BOTTLE)==1,
                    "actual Forge1614 registered metadata0 water bottle supplies FULL1000mB and returns glass, not the separate crucible250mB rule");
            spa.drain(null,Long.MAX_VALUE,true);var tagged=FluidStack.create(Fluids.WATER,bucket+1);tagged.set(DataComponents.CUSTOM_NAME,Component.literal("original fluid NBT identity"));
            var expectedPatch=net.minecraft.core.component.DataComponentPatch.builder().set(DataComponents.CUSTOM_NAME,Component.literal("original fluid NBT identity")).build();
            f.check(tagged.getComponents().get(DataComponents.CUSTOM_NAME)!=null&&tagged.getPatch().equals(expectedPatch),
                    "actual nonempty Architectury fluid reports its real native component patch before any Spa equality or copy");
            f.check(tagged.copy().getPatch().equals(expectedPatch)&&tagged.copyWithAmount(bucket/2).getPatch().equals(expectedPatch),
                    "actual normal and amount-changing Architectury copies retain the full native fluid patch");
            var depleted=tagged.copy();depleted.setAmount(0);
            f.check(depleted.isEmpty()&&depleted.getPatch().isEmpty(),"actual empty fluid reports EMPTY patch instead of exposing its stale depleted component map");
            var encoded=tagged.write(f.level.registryAccess(),new net.minecraft.nbt.CompoundTag());
            var decoded=FluidStack.read(f.level.registryAccess(),encoded).orElseThrow();
            f.check(decoded.getFluid()==tagged.getFluid()&&decoded.getAmount()==tagged.getAmount()&&decoded.getPatch().equals(expectedPatch),
                    "actual native registry-aware fluid CODEC write/read retains exact amount and every original identity component");
            var network=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),f.level.registryAccess());
            try {
                tagged.write(network);var transported=FluidStack.read(network);
                f.check(transported.getFluid()==tagged.getFluid()&&transported.getAmount()==tagged.getAmount()&&transported.getPatch().equals(expectedPatch),
                        "actual native registry-aware fluid stream CODEC carries the full patch, not an empty display-only replacement");
            } finally {network.release();}
            f.check(spa.fill(null,tagged,true)==bucket+1&&spa.fill(null,FluidStack.create(Fluids.WATER,bucket),true)==0,
                    "real same-fluid different component patches cannot mix as original Forge fluid NBT could not mix");
            var salt=new ItemStack(ThaumcraftSpa.BATH_SALTS.get(),64);salt.set(DataComponents.CUSTOM_NAME,Component.literal("native component salt stock"));spa.setItem(0,salt);
            f.check(spa.getSlotsForFace(Direction.UP).length==0&&!spa.canTakeItemThroughFace(0,salt,Direction.UP)&&!spa.canPlaceItemThroughFace(0,salt,Direction.UP)
                            &&spa.canTakeItemThroughFace(0,salt,Direction.DOWN)&&spa.canPlaceItemThroughFace(0,salt,Direction.EAST),"actual salt automation excludes top in both directions and allows lower/horizontal original slots");
            tick(f,spa,1);f.check(f.level.isEmptyBlock(f.base.above())&&spa.getFluidAmountNative()==bucket+1&&spa.getItem(0).getCount()==64,"mix=true requires actual untagged FluidRegistry.WATER, not tagged water identity-only approximation");
            spa.toggleMix();var saved=spa.saveWithFullMetadata(f.level.registryAccess());spa=NodeTransductionRuntimeChecks.reload(f,spa,saved,TileSpa.class);
            f.check(!spa.getMix()&&spa.getFluidAmountNative()==bucket+1&&spa.fluid().getPatch().equals(tagged.getPatch())&&ItemStack.matches(spa.getItem(0),salt),"native registered reload preserves mix=false, every native fluid subunit, fluid patch and full salt components");
            tick(f,spa,1);f.check(f.level.getBlockState(f.base.above()).is(Blocks.WATER)&&f.level.getBlockState(f.base.above()).getValue(LiquidBlock.LEVEL)==0
                            &&spa.getFluidAmountNative()==1&&spa.getItem(0).getCount()==64&&spa.fluid().getPatch().equals(tagged.getPatch()),
                    "actual mix=false native placement spends one source bucket and no salts, retaining the last native subunit/patch rather than rounding it away");
            spa.drain(null,Long.MAX_VALUE,true);f.put(f.base.above(),Blocks.AIR);
            f.check(spa.fill(null,FluidStack.create(Fluids.LAVA,bucket),true)==bucket,"actual non-water registered placeable fluid enters empty native tank");
            tick(f,spa,40);f.check(f.level.getBlockState(f.base.above()).is(Blocks.LAVA)&&spa.fluid().isEmpty()&&spa.getItem(0).getCount()==64,"mix=false really places a paid lava source without salt or a hidden water-only restriction");
            retire(f,spa);
        }
        test.succeed();
    }
    private static void floor(Fixture f){for(BlockPos pos:BlockPos.betweenClosed(f.base.offset(-2,0,-2),f.base.offset(2,0,2))){f.put(pos,Blocks.STONE);f.put(pos.above(),Blocks.AIR);}f.aim(net.minecraft.world.phys.Vec3.atCenterOf(f.base));}
    private static TileSpa spa(Fixture f){f.check(f.level.getBlockEntity(f.base) instanceof TileSpa,"actual Spa registry creates correct native BE and bound ticker type");return (TileSpa)f.level.getBlockEntity(f.base);}
    private static void tick(Fixture f,TileSpa spa,int count){for(int i=0;i<count;i++){var ticker=spa.getBlockState().getTicker(f.level,ThaumcraftSpa.SPA_TILE.get());f.check(ticker!=null,"real registered Spa native block ticker exists");ticker.tick(f.level,spa.getBlockPos(),spa.getBlockState(),spa);}}
    private static boolean source(Fixture f,BlockPos pos){var state=f.level.getBlockState(pos);return state.is(ThaumcraftSpa.PURE_WATER.get())&&state.getValue(LiquidBlock.LEVEL)==0;}
    private static int sources(Fixture f){int count=0;for(BlockPos pos:BlockPos.betweenClosed(f.base.offset(-2,1,-2),f.base.offset(2,1,2)))if(source(f,pos))count++;return count;}
    private static void retire(Fixture f,TileSpa spa){var original=spa.getItem(0).copy();f.level.setBlock(f.base,Blocks.AIR.defaultBlockState(),net.minecraft.world.level.block.Block.UPDATE_ALL);f.flushFresh();var drops=f.drops();f.check(spa.fluid().isEmpty()&&!spa.isLiveOwner()&&(original.isEmpty()||drops.stream().anyMatch(drop->ItemStack.matches(drop.getItem(),original))),"actual native onRemove loses tank fluid exactly as original, but returns owned full-component salts durably once");}
    private static final class Cleanup implements AutoCloseable{final Fixture f;Cleanup(Fixture f){this.f=f;}@Override public void close(){if(f.level.getBlockEntity(f.base) instanceof TileSpa)f.level.setBlock(f.base,Blocks.AIR.defaultBlockState(),net.minecraft.world.level.block.Block.UPDATE_ALL);f.flushFresh();f.drops();}}
}
