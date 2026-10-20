package thaumcraft.common.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.blocks.BlockTaint;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.lib.utils.RejectedEntitySpawn;

/** One real falling crust, retaining its original source until native admission and first tick. */
public final class EntityFallingTaint extends Entity {
    private static final EntityDataAccessor<BlockState> STATE=SynchedEntityData.defineId(EntityFallingTaint.class,EntityDataSerializers.BLOCK_STATE);
    private static final SoundEvent GORE=SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft","gore"));
    @FunctionalInterface public interface ClientEffects { void land(EntityFallingTaint entity); }
    private static ClientEffects clientEffects=entity->{};
    private static final java.util.Map<ServerLevel,java.util.Set<BlockPos>> STARTING=new java.util.IdentityHashMap<>();
    private BlockPos source=BlockPos.ZERO;
    private int fallTime;
    private boolean debited,ticking;
    private CompoundTag rejected;
    public EntityFallingTaint(EntityType<? extends EntityFallingTaint> type,Level level){super(type,level);blocksBuilding=true;}
    public static void setClientEffects(ClientEffects effects){clientEffects=java.util.Objects.requireNonNull(effects);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(STATE,Blocks.AIR.defaultBlockState());}
    public BlockState getBlockState(){return entityData.get(STATE);}
    public BlockPos getSourcePos(){return source;}
    public int getFallTime(){return fallTime;}
    public boolean isDebited(){return debited;}
    public boolean isQuarantined(){return rejected!=null;}
    private static boolean valid(BlockState state){return state.getBlock() instanceof BlockTaint taint&&taint.metadata()==0;}
    /** The old yOffset puts posY at the bounding-box centre, rather than at its feet. */
    @Override protected AABB makeBoundingBox(){double half=(double)(.98F/2F);return new AABB(getX()-half,getY()-half,getZ()-half,getX()+half,getY()+half,getZ()+half);}
    public static boolean start(ServerLevel level,BlockPos source,BlockPos start){
        if(!level.getServer().isSameThread()||level.getServer().getLevel(level.dimension())!=level||!BlockTaintFibres.loaded(level,source)||!BlockTaintFibres.loaded(level,start))return false;
        BlockState state=level.getBlockState(source);if(!valid(state))return false;
        var active=STARTING.computeIfAbsent(level,ignored->new java.util.HashSet<>());if(!active.add(source.immutable()))return false;
        try{
            var falling=ThaumcraftTaint.FALLING_TAINT.get().create(level);if(falling==null)throw new IllegalStateException("Missing native falling taint");
            falling.entityData.set(STATE,state);falling.source=source.immutable();falling.setPos((double)((float)start.getX()+.5F),(double)((float)start.getY()+.5F),(double)((float)start.getZ()+.5F));
            falling.setDeltaMovement(0,0,0);falling.setOldPosAndRot();
            // addFreshEntity may be cancelled or invoke observers. No source has been removed yet.
            var snapshot=RejectedEntitySpawn.snapshotBefore(level,falling);boolean added=false;
            try{added=level.addFreshEntity(falling);return added;}
            finally{if(!added){falling.discard();RejectedEntitySpawn.releaseRejected(level,falling,snapshot,false);}}
        }finally{active.remove(source);if(active.isEmpty())STARTING.remove(level);}
    }
    private boolean current(){return level() instanceof ServerLevel level&&level.getServer().isSameThread()&&level.getServer().getLevel(level.dimension())==level
            &&!isRemoved()&&level.getChunkSource().getChunkNow(blockPosition().getX()>>4,blockPosition().getZ()>>4)!=null&&level.getEntity(getUUID())==this;}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public boolean isPushable(){return false;}
    @Override protected MovementEmission getMovementEmission(){return MovementEmission.NONE;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public void tick(){
        if(ticking||!level().isClientSide&&!current())return;
        ticking=true;
        try{
            if(rejected!=null)return;
            BlockState state=getBlockState();if(!valid(state)){if(!level().isClientSide)discard();return;}
            setOldPosAndRot();++fallTime;
            setDeltaMovement(getDeltaMovement().add(0,-(double).04F,0));
            move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale((double).98F));
            if(level().isClientSide){if(onGround()||fallTime==1)for(int i=0;i<10;i++)clientEffects.land(this);return;}
            ServerLevel world=(ServerLevel)level();if(!current())return;
            if(!debited){
                if(!BlockTaintFibres.loaded(world,source)||world.getBlockState(source)!=state){discard();return;}
                // Claim the single source BEFORE the removal callback can observe this entity again.
                debited=true;world.removeBlock(source,false);
                if(!current())return;
            }
            BlockPos target=BlockPos.containing(position()),below=target.below();
            if(!BlockTaintFibres.loaded(world,target)||!BlockTaintFibres.loaded(world,below)){
                if(fallTime>600||fallTime>100&&(target.getY()<world.getMinBuildHeight()+1||target.getY()>world.getMaxBuildHeight()))discard();return;
            }
            BlockState bottom=world.getBlockState(below);
            if(onGround()||bottom.is(ThaumcraftFlux.FLUX_GOO.get())&&bottom.getValue(FluxFiniteBlock.LEVEL)>=4){
                var movement=getDeltaMovement();setDeltaMovement(movement.x*(double).7F,movement.y*-.5D,movement.z*(double).7F);
                BlockState replacement=world.getBlockState(target);
                if(replacement.is(Blocks.PISTON)||replacement.is(Blocks.PISTON_HEAD)||replacement.is(Blocks.MOVING_PISTON))return;
                world.playSound(null,getX(),getY(),getZ(),GORE,SoundSource.BLOCKS,.5F,((random.nextFloat()-random.nextFloat())*.2F+1F)*.8F);
                // The original consumes the falling entity even if its landing cannot be placed.
                discard();
                if((replacement.getBlock() instanceof BlockTaintFibres||replacement.is(ThaumcraftFlux.FLUX_GOO.get())||replacement.canBeReplaced())
                        &&!BlockTaint.canFallBelow(world,below)&&world.getBlockState(target)==replacement)
                    world.setBlock(target,state,Block.UPDATE_ALL);
            }else if(fallTime>600||fallTime>100&&(target.getY()<world.getMinBuildHeight()+1||target.getY()>world.getMaxBuildHeight()))discard();
        }finally{ticking=false;}
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){
        if(rejected!=null){tag.merge(rejected.copy());return;}
        tag.put("BlockState",NbtUtils.writeBlockState(getBlockState()));tag.putByte("Time",(byte)fallTime);
        tag.putInt("OldX",source.getX());tag.putInt("OldY",source.getY());tag.putInt("OldZ",source.getZ());tag.putBoolean("Debited",debited);
        // Original unused damage settings remain round-trippable at their actual defaults.
        tag.putFloat("FallHurtAmount",2F);tag.putInt("FallHurtMax",40);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag){
        rejected=null;
        if(!tag.contains("BlockState",Tag.TAG_COMPOUND)||!tag.contains("Time",Tag.TAG_BYTE)||!tag.contains("Debited",Tag.TAG_BYTE)){
            rejected=tag.copy();entityData.set(STATE,Blocks.AIR.defaultBlockState());return;
        }
        var state=NbtUtils.readBlockState(registryAccess().lookupOrThrow(Registries.BLOCK),tag.getCompound("BlockState"));
        if(!valid(state)){rejected=tag.copy();entityData.set(STATE,Blocks.AIR.defaultBlockState());return;}
        entityData.set(STATE,state);fallTime=tag.getByte("Time")&255;debited=tag.getBoolean("Debited");
        source=new BlockPos(tag.getInt("OldX"),tag.getInt("OldY"),tag.getInt("OldZ"));
    }
}
