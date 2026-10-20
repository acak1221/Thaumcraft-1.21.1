package thaumcraft.common.tiles;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidBucketHooks;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockFluidPure;
import thaumcraft.common.config.ThaumcraftSpa;
import thaumcraft.common.container.ContainerSpa;
import thaumcraft.common.items.ItemBathSalts;
import thaumcraft.common.lib.SpaRules;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Original one-slot/5000mB spa. Native tank custody and one paid placement survive owner reload. */
public final class TileSpa extends BlockEntity implements WorldlyContainer,MenuProvider {
    private ItemStack salt=ItemStack.EMPTY;
    private FluidStack tank=FluidStack.empty();
    private boolean mix=true,working,bucketing,retired;
    private int counter;
    private long fluidRevision;
    private FluidTransaction fluidTransaction;
    private Release release;
    private CompoundTag rejected;
    private record Release(BlockPos pos,BlockState before,BlockState after){}
    public TileSpa(BlockPos pos,BlockState state){super(ThaumcraftSpa.SPA_TILE.get(),pos,state);}
    public boolean current(){return level!=null&&!isRemoved()&&level.hasChunkAt(worldPosition)&&level.getBlockEntity(worldPosition)==this&&level.getBlockState(worldPosition).is(ThaumcraftSpa.ARCANE_SPA.get());}
    public boolean isLiveOwner(){return !retired&&rejected==null&&level instanceof ServerLevel server&&server.getServer().isSameThread()&&server.getServer().getLevel(server.dimension())==server&&current();}
    private boolean idle(){return isLiveOwner()&&!working&&!bucketing&&fluidTransaction==null;}
    public boolean canInteract(){return idle();}
    public boolean getMix(){return mix;}
    public void toggleMix(){if(!idle())return;mix=!mix;changed();}
    public FluidStack fluid(){return tank.copy();}
    public long fluidRevision(){return fluidRevision;}
    public long getFluidAmountNative(){return tank.getAmount();}
    public long getCapacityNative(){return FluidStack.bucketAmount()*5;}
    public int getFluidAmountMb(){return (int)(tank.getAmount()*1000/FluidStack.bucketAmount());}
    public int getCapacityMb(){return SpaRules.CAPACITY_MB;}
    public boolean canFill(@Nullable Direction side,Fluid fluid){return side!=Direction.UP;}
    public boolean canDrain(@Nullable Direction side,Fluid fluid){return side!=Direction.UP;}
    /** Forge1.7 compares identity AND NBT; Architectury.isFluidEqual alone omits its component patch. */
    private static boolean sameFluid(FluidStack first,FluidStack second){return first.isFluidEqual(second)&&first.isComponentEqual(second);}
    private static boolean registered(FluidStack stack){return !stack.isEmpty()&&BuiltInRegistries.FLUID.getResourceKey(stack.getFluid()).isPresent();}
    private long fillable(FluidStack resource,long maximum){return maximum<=0||!registered(resource)||!tank.isEmpty()&&!sameFluid(tank,resource)?0:Math.min(Math.min(maximum,resource.getAmount()),getCapacityNative()-tank.getAmount());}
    private void insertRaw(FluidStack resource,long amount){tank=tank.isEmpty()?resource.copyWithAmount(amount):tank.copyWithAmount(tank.getAmount()+amount);fluidRevision++;}
    private FluidStack extractRaw(long amount){var result=tank.copyWithAmount(amount);tank=amount==tank.getAmount()?FluidStack.empty():tank.copyWithAmount(tank.getAmount()-amount);fluidRevision++;return result;}
    /** Direct call ignores its side exactly like original fill; automation advertises side restrictions separately. */
    public long fill(@Nullable Direction side,FluidStack resource,boolean execute){
        if(resource==null||!idle())return 0;long amount=fillable(resource,resource.getAmount());if(execute&&amount>0){insertRaw(resource,amount);changed();}return amount;
    }
    public FluidStack drain(@Nullable Direction side,long maximum,boolean execute){
        if(!idle()||maximum<=0||tank.isEmpty())return FluidStack.empty();long amount=Math.min(maximum,tank.getAmount());
        var result=tank.copyWithAmount(amount);if(execute){extractRaw(amount);changed();}return result;
    }
    public FluidStack drain(@Nullable Direction side,FluidStack requested,boolean execute){return requested==null||!sameFluid(tank,requested)?FluidStack.empty():drain(side,requested.getAmount(),execute);}

    /** One native outer transaction owns the lease; nested savepoints carry only this exact lease's opaque snapshots. */
    public FluidTransaction beginFluidTransaction(Object identity){
        Objects.requireNonNull(identity,"native outer transaction identity");
        if(fluidTransaction!=null)return fluidTransaction.identity==identity&&fluidTransaction.live()?fluidTransaction:null;
        if(!idle())return null;return fluidTransaction=new FluidTransaction(identity);
    }
    public static final class FluidSnapshot {
        private final FluidTransaction transaction;private final FluidStack fluid;private final long revision;
        private FluidSnapshot(FluidTransaction transaction,FluidStack fluid,long revision){this.transaction=transaction;this.fluid=fluid.copy();this.revision=revision;}
    }
    public final class FluidTransaction {
        private final Object identity;private final FluidStack initial=tank.copy();private final long initialRevision=fluidRevision;private boolean open=true;
        private FluidTransaction(Object identity){this.identity=identity;}
        public boolean live(){return open&&fluidTransaction==this&&isLiveOwner();}
        public FluidSnapshot snapshot(){if(!live())throw new IllegalStateException("Retired spa fluid transaction");return new FluidSnapshot(this,tank,fluidRevision);}
        public long insert(FluidStack resource,long maximum){if(resource==null||!live())return 0;long amount=fillable(resource,maximum);if(amount>0)insertRaw(resource,amount);return amount;}
        public FluidStack extract(FluidStack resource,long maximum){if(resource==null||!live()||maximum<=0||tank.isEmpty()||!sameFluid(tank,resource))return FluidStack.empty();return extractRaw(Math.min(Math.min(maximum,resource.getAmount()),tank.getAmount()));}
        public boolean rollback(FluidSnapshot snapshot){
            if(snapshot==null||snapshot.transaction!=this)throw new IllegalArgumentException("Foreign spa fluid snapshot");
            if(!live())return false;tank=snapshot.fluid.copy();fluidRevision=snapshot.revision;return true;
        }
        public void finish(boolean commit){
            if(!open)return;boolean owned=live();open=false;
            if(fluidTransaction==this)fluidTransaction=null;
            if(!owned)return; // A retired/replaced spa never regains private fluid from a rollback.
            if(!commit){tank=initial.copy();fluidRevision=initialRevision;return;}
            if(!tank.isFluidStackEqual(initial))changed();
        }
    }

    public static void tick(Level level,BlockPos pos,BlockState state,TileSpa tile){
        if(tile.level!=level||!tile.worldPosition.equals(pos)||!state.is(ThaumcraftSpa.ARCANE_SPA.get())||!tile.idle())return;tile.working=true;
        try{if(tile.release!=null)tile.releasePaid();if(tile.isLiveOwner()&&tile.counter++%SpaRules.PERIOD==0&&!level.hasNeighborSignal(pos))tile.work();}
        finally{tile.working=false;}
    }
    private void work(){
        if(!hasIngredients())return;var target=mix?ThaumcraftSpa.PURE_WATER.get().defaultBlockState():placeableState(tank);
        if(target==null)return;var server=(ServerLevel)level;BlockPos center=worldPosition.above();BlockPos destination=null;
        if(!loaded(server,center))return;
        if(fullSource(server.getBlockState(center),target.getBlock())){
            for(int x=-2;x<=2&&destination==null;x++)for(int z=-2;z<=2;z++){BlockPos candidate=center.offset(x,0,z);if(validLocation(server,candidate,true,target)){destination=candidate;break;}}
        }else if(validLocation(server,center,false,target))destination=center;
        if(destination==null||!isLiveOwner()||!hasIngredients())return;
        BlockState before=server.getBlockState(destination);ItemStack snapshot=salt.copy();FluidStack fluidBefore=tank.copy();
        // All expensive/current/world preflight is finished. Raw input debit and owned durable intent are adjacent.
        if(mix){if(salt.isEmpty()||!ItemStack.matches(salt,snapshot))return;salt=salt.getCount()==1?ItemStack.EMPTY:salt.copyWithCount(salt.getCount()-1);}
        tank=fluidBefore.getAmount()==FluidStack.bucketAmount()?FluidStack.empty():fluidBefore.copyWithAmount(fluidBefore.getAmount()-FluidStack.bucketAmount());fluidRevision++;
        release=new Release(destination.immutable(),before,target);super.setChanged();
        releasePaid(); // Original publishes during this same first/40th work call.
        if(isLiveOwner())changed();
    }
    private boolean hasIngredients(){
        if(tank.isEmpty()||tank.getAmount()<FluidStack.bucketAmount())return false;
        if(mix)return sameFluid(tank,FluidStack.create(Fluids.WATER,FluidStack.bucketAmount()))&&!salt.isEmpty()&&salt.getItem() instanceof ItemBathSalts;
        return placeableState(tank)!=null;
    }
    @Nullable private static BlockState placeableState(FluidStack fluid){
        if(fluid.isEmpty())return null;BlockState state=fluid.getFluid().defaultFluidState().createLegacyBlock();
        return state.isAir()||state.getFluidState().isEmpty()?null:state.getBlock().defaultBlockState();
    }
    private static boolean loaded(ServerLevel level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&level.getWorldBorder().isWithinBounds(pos)&&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    private static boolean fullSource(BlockState state,Block block){return state.is(block)&&(!state.hasProperty(LiquidBlock.LEVEL)||state.getValue(LiquidBlock.LEVEL)==0);}
    private boolean validLocation(ServerLevel level,BlockPos pos,boolean adjacent,BlockState target){
        if(target.is(Blocks.WATER)&&level.dimensionType().ultraWarm()||!loaded(level,pos)||!loaded(level,pos.below()))return false;
        var state=level.getBlockState(pos);if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)||!state.canBeReplaced()||fullSource(state,target.getBlock()))return false;
        if(!adjacent)return true;
        for(var direction:new Direction[]{Direction.SOUTH,Direction.NORTH,Direction.EAST,Direction.WEST,Direction.UP,Direction.DOWN}){
            var next=pos.relative(direction);if(loaded(level,next)&&fullSource(level.getBlockState(next),target.getBlock()))return true;
        }return false;
    }
    private void releasePaid(){
        if(release==null||!isLiveOwner())return;Release paid=release;
        // Retire intent before native world callbacks. Failed/replaced targets still keep their original paid debit.
        release=null;super.setChanged();var server=(ServerLevel)level;
        if(!loaded(server,paid.pos)||!server.getBlockState(paid.pos).equals(paid.before)||!isLiveOwner())return;
        server.setBlock(paid.pos,paid.after,Block.UPDATE_ALL);
        if(!isLiveOwner()||!loaded(server,paid.pos))return;
        var placed=server.getBlockState(paid.pos);
        if(placed.getBlock() instanceof BlockFluidPure&&placed.hasProperty(LiquidBlock.LEVEL)){
            int metadata=placed.getValue(LiquidBlock.LEVEL),normalized=SpaRules.normalizedMetadata(metadata);
            if(normalized!=metadata)server.setBlock(paid.pos,placed.setValue(LiquidBlock.LEVEL,normalized),Block.UPDATE_ALL);
        }
    }

    public record ContainerFluid(FluidStack fluid,ItemStack empty){public ContainerFluid{fluid=fluid.copy();empty=empty.copy();}@Override public FluidStack fluid(){return fluid.copy();}@Override public ItemStack empty(){return empty.copy();}}
    @Nullable public static ContainerFluid filledContainer(ItemStack stack){
        if(stack.isEmpty())return null;
        if(stack.getItem() instanceof BucketItem bucket&&!(stack.getItem() instanceof MobBucketItem)){
            var fluid=FluidBucketHooks.getFluid(bucket);if(fluid!=Fluids.EMPTY)return new ContainerFluid(FluidStack.create(fluid,FluidStack.bucketAmount()),new ItemStack(Items.BUCKET));
        }
        var potion=stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        // Forge1.7 registry gives metadata0 water potion a FULL1000mB; ContainerKey ignores item NBT.
        if(stack.is(Items.POTION)&&potion!=null&&potion.potion().filter(holder->holder.is(Potions.WATER.unwrapKey().orElseThrow())).isPresent())return new ContainerFluid(FluidStack.create(Fluids.WATER,FluidStack.bucketAmount()),new ItemStack(Items.GLASS_BOTTLE));
        return null;
    }
    public boolean fillFromHeldContainer(ServerPlayer actor,InteractionHand hand){
        if(!idle()||actor.level()!=level||actor.isRemoved()||!actor.isAlive()||actor.isSpectator()||!stillValid(actor)
                ||actor.server.getPlayerList().getPlayer(actor.getUUID())!=actor)return false;
        var held=actor.getItemInHand(hand);var content=filledContainer(held);if(content==null)return false;var offered=content.fluid();long accepted=fillable(offered,offered.getAmount());if(accepted<=0)return false;
        var expected=held.copy();var remainder=held.copy();remainder.shrink(1);var empty=content.empty();bucketing=true;
        try{
            if(!isLiveOwner()||actor.getItemInHand(hand)!=held||!ItemStack.matches(held,expected))return false;
            held.shrink(1);insertRaw(offered,accepted); // Raw inventory debit then raw tank credit, before all callbacks.
            // Native creative useItemOn restores its argument's count; detach that consumed argument in either mode.
            if(actor.getItemInHand(hand)==held)actor.setItemInHand(hand,remainder);
            if(!actor.getInventory().add(empty))DeferredItemDrops.enqueue((ServerLevel)level,actor.blockPosition(),List.of(empty));
            actor.getInventory().setChanged();actor.containerMenu.broadcastChanges();changed();
            level.playSound(null,worldPosition,SoundEvents.GENERIC_SWIM,SoundSource.BLOCKS,.33F,1F+(level.random.nextFloat()-level.random.nextFloat())*.3F);return true;
        }finally{bucketing=false;}
    }
    @Override public int getContainerSize(){return 1;}
    @Override public boolean isEmpty(){return getItem(0).isEmpty();}
    @Override public ItemStack getItem(int slot){return slot==0&&current()&&!retired&&rejected==null&&fluidTransaction==null?salt:ItemStack.EMPTY;}
    @Override public int getMaxStackSize(){return 64;}
    private boolean inventoryMutable(){return idle();}
    @Override public ItemStack removeItem(int slot,int count){if(slot!=0||count<=0||!inventoryMutable())return ItemStack.EMPTY;var result=salt.split(count);if(salt.isEmpty())salt=ItemStack.EMPTY;if(!result.isEmpty())changed();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){if(slot!=0||!inventoryMutable())return ItemStack.EMPTY;var result=salt;salt=ItemStack.EMPTY;return result;}
    @Override public void setItem(int slot,ItemStack stack){if(slot==0&&stack!=null&&inventoryMutable()){salt=stack;changed();}}
    @Override public void clearContent(){if(inventoryMutable()){salt=ItemStack.EMPTY;changed();}}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&inventoryMutable()&&!stack.isEmpty()&&stack.getItem() instanceof ItemBathSalts;}
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.UP||!inventoryMutable()?new int[0]:new int[]{0};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return side!=Direction.UP&&inventoryMutable()&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return slot==0&&side!=Direction.UP&&inventoryMutable();}
    @Override public boolean stillValid(Player player){return current()&&!retired&&rejected==null&&fluidTransaction==null&&player.level()==level&&player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    public void evacuate(){
        if(!(level instanceof ServerLevel server)||!server.getServer().isSameThread()||server.getServer().getLevel(server.dimension())!=server||retired||isRemoved()||!level.hasChunkAt(worldPosition)||level.getBlockEntity(worldPosition)!=this)return;var owned=salt.copy();retired=true;salt=ItemStack.EMPTY;tank=FluidStack.empty();fluidRevision++;release=null;super.setChanged();
        if(!owned.isEmpty())DeferredItemDrops.enqueue((ServerLevel)level,worldPosition,List.of(owned)); // Original tank fluid is lost on break, not invented fluid items.
    }
    private void changed(){super.setChanged();if(level!=null&&current())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    @Override public void setChanged(){if(fluidTransaction==null)changed();else super.setChanged();}
    @Override public Component getDisplayName(){return Component.translatable("thaumcraft.spa");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new ContainerSpa(id,inventory,this);}
    @Override public boolean onlyOpCanSetNbt(){return true;}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);if(rejected!=null){tag.put("RejectedSpa",rejected.copy());return;}
        tag.putBoolean("mix",mix);var savedTank=fluidTransaction!=null&&fluidTransaction.open&&!retired?fluidTransaction.initial:tank;if(!savedTank.isEmpty())tag.put("Tank",savedTank.write(registries,new CompoundTag()));
        if(!salt.isEmpty()){var entry=(CompoundTag)salt.copyWithCount(1).save(registries);entry.putByte("Slot",(byte)0);tag.put("Items",new ListTag());tag.getList("Items",Tag.TAG_COMPOUND).add(entry);}else tag.put("Items",new ListTag());
        tag.putInt("SaltCount",salt.getCount());
        if(release!=null){var receipt=new CompoundTag();receipt.putLong("Target",release.pos.asLong());receipt.put("Before",NbtUtils.writeBlockState(release.before));receipt.put("After",NbtUtils.writeBlockState(release.after));tag.put("PaidRelease",receipt);}
        // Original counter and transaction/working ownership are transient. Native target receipt is the authority fix.
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        // loadWithComponents is final and assigns its component map AFTER this
        // method. Throw before any mutation so neither private fields nor base
        // components can reload committed stock over an active native transfer.
        if(fluidTransaction!=null&&fluidTransaction.open&&isLiveOwner())throw new IllegalStateException("Cannot reload a live spa during a fluid transaction");
        super.loadAdditional(tag,registries);if(fluidTransaction!=null)fluidTransaction.open=false;fluidTransaction=null;salt=ItemStack.EMPTY;tank=FluidStack.empty();mix=tag.getBoolean("mix");counter=0;release=null;rejected=null;fluidRevision++;retired=false;
        if(tag.contains("RejectedSpa",Tag.TAG_COMPOUND)){rejected=tag.getCompound("RejectedSpa").copy();return;}
        try{
            if(tag.contains("Tank")){if(!tag.contains("Tank",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid native spa tank");tank=FluidStack.read(registries,tag.getCompound("Tank")).orElseThrow();if(!registered(tank)||tank.getAmount()>getCapacityNative())throw new IllegalArgumentException("Invalid native spa amount");}
            if(tag.contains("Items")&&!tag.contains("Items",Tag.TAG_LIST))throw new IllegalArgumentException("Invalid salt inventory");if(tag.get("Items") instanceof ListTag raw&&!raw.isEmpty()&&raw.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("Invalid salt entries");var items=tag.getList("Items",Tag.TAG_COMPOUND);
            if(items.size()>1)throw new IllegalArgumentException("Duplicate salt inventory");if(!items.isEmpty()){var item=items.getCompound(0);if(!item.contains("Slot",Tag.TAG_ANY_NUMERIC)||item.getInt("Slot")!=0)throw new IllegalArgumentException("Invalid salt slot");salt=ItemStack.parse(registries,item).orElseThrow();if(salt.isEmpty())throw new IllegalArgumentException("Empty salt entry");}
            if(tag.contains("SaltCount")){if(!tag.contains("SaltCount",Tag.TAG_INT)||tag.getInt("SaltCount")<0||salt.isEmpty()!=(tag.getInt("SaltCount")==0))throw new IllegalArgumentException("Invalid native salt count");if(!salt.isEmpty())salt.setCount(tag.getInt("SaltCount"));}
            if(tag.contains("PaidRelease")){if(!tag.contains("PaidRelease",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid paid release");var receipt=tag.getCompound("PaidRelease");var target=BlockPos.of(receipt.getLong("Target"));var blocks=registries.lookupOrThrow(Registries.BLOCK);var before=NbtUtils.readBlockState(blocks,receipt.getCompound("Before"));var after=NbtUtils.readBlockState(blocks,receipt.getCompound("After"));
                var source=after.getFluidState();
                if(target.getY()!=worldPosition.getY()+1||Math.abs((long)target.getX()-worldPosition.getX())>2||Math.abs((long)target.getZ()-worldPosition.getZ())>2||!source.isSource()||!after.equals(source.getType().defaultFluidState().createLegacyBlock())||!fullSource(after,after.getBlock()))throw new IllegalArgumentException("Invalid bounded fluid release");release=new Release(target,before,after);}
        }catch(RuntimeException invalid){rejected=tag.copy();salt=ItemStack.EMPTY;tank=FluidStack.empty();release=null;}
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
