package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalTaintExperience;

/** Original stationary growing spore. Only the current server entity may claim
 * one physical burst; client effects cannot spawn entities or edit fibres. */
public class EntityTaintSpore extends Monster implements ITaintedMob {
    private static final EntityDataAccessor<Byte> SIZE=SynchedEntityData.defineId(EntityTaintSpore.class,EntityDataSerializers.BYTE);
    protected int growth;
    public float displaySize;
    private boolean bursting;
    public EntityTaintSpore(EntityType<? extends EntityTaintSpore> type,Level level){super(type,level);setSporeSize(2);}
    public EntityTaintSpore(Level level){this(ThaumcraftTaint.TAINT_SPORE.get(),level);}
    public static AttributeSupplier.Builder createAttributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,1).add(Attributes.ATTACK_DAMAGE,1);}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(SIZE,(byte)1);}
    public void setSporeSize(int value){
        // A negative signed watcher can make the original nextInt bound0. The
        // native API rejects that crash input before it becomes live state.
        if(value<0||value>127)throw new IllegalArgumentException("Spore size outside safe signed-byte range");
        entityData.set(SIZE,(byte)value);xpReward=value;refreshDimensions();
    }
    public int getSporeSize(){return entityData.get(SIZE);}
    protected float physicalSize(){return Math.max(.15F*getSporeSize(),.5F);}
    @Override public EntityDimensions getDefaultDimensions(Pose pose){float size=physicalSize();return EntityDimensions.scalable(size,size);}
    /** Original setSporeSize reapplies the SAME feet XYZ after resizing.
     * Native growth instead preserves the old centre and shifts feet/support. */
    @Override public boolean fudgePositionAfterSizeChange(EntityDimensions previous){return false;}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);
        if(SIZE.equals(key)){refreshDimensions();if(level().isClientSide&&tickCount==0)displaySize=getSporeSize();}
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Size",getSporeSize()-1);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setSporeSize(Math.addExact(tag.getInt("Size"),1));}
    @Override protected int getBaseExperienceReward(){return getSporeSize();}
    @Override public float getPickRadius(){return 0;}
    @Override public boolean isPickable(){return true;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<4096;}
    public BlockPos supportPos(){return BlockPos.containing(getX(),Mth.floor(getBoundingBox().minY)-1,getZ());}
    public boolean anchored(){var state=level().getBlockState(supportPos());return state.is(ThaumcraftTaint.FIBRES.get())&&state.getValue(BlockTaintFibres.TYPE)==4;}
    @Override public void move(MoverType type,Vec3 delta){if(anchored())return;super.move(type,new Vec3(0,Math.min(0,delta.y),0));}
    @Override public void tick(){
        super.tick();if(isRemoved())return;
        if(level() instanceof ServerLevel server&&tickCount%20==0&&!BlockTaintFibres.isTaintBiome(server,blockPosition()))hurt(damageSources().generic(),1);
        sporeOnUpdate();
    }
    protected void sporeOnUpdate(){
        if(getSporeSize()<10&&growth++==1200){setSporeSize(getSporeSize()+1);growth=0;}
        if(level().isClientSide){if(displaySize<getSporeSize())displaySize+=.02F;TaintSwarmVisuals.tick(this,getSporeSize()/3,.1F,10F,0);}
        if(!anchored()||deathTime>0)spiderBurst();
    }
    @Override public void playerTouch(Player player){spiderBurst();}
    protected boolean currentServerEntity(){return level() instanceof ServerLevel server&&server.getServer().isSameThread()&&server.getServer().getLevel(server.dimension())==server&&server.getEntity(getUUID())==this&&!isRemoved();}
    protected void spiderBurst(){
        if(level().isClientSide){TaintSwarmVisuals.sploosh(this,50);return;}
        if(!currentServerEntity()||bursting)return;bursting=true;
        burstSound();int size=getSporeSize(),count=size/3+level().random.nextInt(size/2+1);
        for(int i=0;i<count;i++){
            var spider=new EntityTaintSpider(level());
            spider.moveTo(getX()+(double)level().random.nextFloat()-(double)level().random.nextFloat(),getY()+(double)level().random.nextFloat(),getZ()+(double)level().random.nextFloat()-(double)level().random.nextFloat(),level().random.nextFloat()*360F,0);
            level().addFreshEntity(spider);
        }
        BlockPos below=supportPos();var state=level().getBlockState(below);
        if(state.is(ThaumcraftTaint.FIBRES.get())&&state.getValue(BlockTaintFibres.TYPE)==4)level().setBlock(below,state.setValue(BlockTaintFibres.TYPE,3),3);
        discard();
    }
    protected void burstSound(){playSound(sound("gore"),1,.9F+level().random.nextFloat()*.1F);}
    protected static SoundEvent sound(String name){return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft",name));}
    @Override public void playSound(SoundEvent sound,float volume,float pitch){
        // Native transport requires an audio seed. Keep it independent of the
        // original world RNG used immediately afterwards for burst children.
        if(!isSilent())level().playSeededSound(null,getX(),getY(),getZ(),sound,getSoundSource(),volume,pitch,getUUID().getMostSignificantBits()^tickCount);
    }
    @Override protected float getSoundVolume(){return .1F;}
    @Override public int getAmbientSoundInterval(){return 200;}
    @Override protected SoundEvent getAmbientSound(){return sound("swarm");}
    @Override protected SoundEvent getHurtSound(DamageSource source){return sound("gore");}
    @Override protected SoundEvent getDeathSound(){return sound("gore");}
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height){return OriginalMobLoot.spawn(this,stack,height);}
    @Override protected void dropExperience(Entity killer){} // Original XP belongs to deathTime20, never die().
    @Override protected void tickDeath(){
        if(OriginalTaintExperience.finishDeathTick(this,()->lastHurtByPlayerTime>0||isAlwaysExperienceDropper(),
                ()->OriginalTaintExperience.originalReward(this,getSporeSize(),this::getEquipmentDropChance)))return;
        super.tickDeath();
    }
    @Override protected void dropFromLootTable(DamageSource source,boolean recent){}
    protected int taintedDrops(){return 1;}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recent){
        for(int i=0;i<taintedDrops();i++)spawnAtLocation(new ItemStack(ThaumcraftContent.MATERIALS.get(level.random.nextBoolean()?"tainted_goo":"taint_tendril").get()),getBbHeight()/2F);
        OriginalTaintExperience.rememberEquipment(this);
        super.dropCustomDeathLoot(level,source,recent);
    }
}
