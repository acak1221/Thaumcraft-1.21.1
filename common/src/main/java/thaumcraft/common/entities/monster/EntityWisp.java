package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.lib.entities.OriginalLightRules;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalMobTargeting;
import net.minecraft.world.entity.item.ItemEntity;
import thaumcraft.common.lib.network.WispZapNetworking;

/** TC4's native flying wisp: aspect identity, old waypoint action and movement-sensitive lightning. */
public final class EntityWisp extends FlyingMob implements Enemy {
    public interface ClientEffects { void tick(EntityWisp wisp); void burst(EntityWisp wisp); }
    private static ClientEffects clientEffects;
    private static final EntityDataAccessor<String> ASPECT = SynchedEntityData.defineId(EntityWisp.class,EntityDataSerializers.STRING);
    private static final ResourceKey<Biome> EERIE = ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("thaumcraft","eerie"));
    public int courseChangeCooldown;
    public double waypointX,waypointY,waypointZ;
    private Entity targetedEntity;
    private int aggroCooldown;
    public int prevAttackCounter,attackCounter;
    public EntityWisp(EntityType<? extends EntityWisp> type,Level level) { super(type,level); xpReward=5; }
    public static void setClientEffects(ClientEffects effects) { clientEffects=java.util.Objects.requireNonNull(effects); }
    public static AttributeSupplier.Builder createAttributes() { return FlyingMob.createMobAttributes().add(Attributes.MAX_HEALTH,22).add(Attributes.ATTACK_DAMAGE,3); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(ASPECT,""); }
    public String getAspectTag() { return entityData.get(ASPECT); }
    public void setAspectTag(String tag) { entityData.set(ASPECT,String.valueOf(tag)); }
    public Entity getAttackTarget() { return targetedEntity; }
    @Override protected boolean shouldDespawnInPeaceful() { return true; }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override public SoundSource getSoundSource() { return SoundSource.HOSTILE; }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(source.getDirectEntity() instanceof LivingEntity direct) { targetedEntity=direct; aggroCooldown=200; }
        if(source.getEntity() instanceof LivingEntity attacker) { targetedEntity=attacker; aggroCooldown=200; }
        return super.hurt(source,amount);
    }
    @Override public void die(DamageSource source) { super.die(source); if(level().isClientSide && clientEffects!=null) clientEffects.burst(this); }
    @Override public void tick() { super.tick(); if(level().isClientSide && clientEffects!=null) { if(tickCount<=1) clientEffects.burst(this); clientEffects.tick(this); } }
    @Override protected void customServerAiStep() {
        if(Aspect.getAspect(getAspectTag())==null) {
            if(level().getBiome(new BlockPos(Mth.ceil(getX()),Mth.floor(getY()),Mth.ceil(getZ()))).is(EERIE)) {
                var eerie=new Aspect[]{Aspect.DARKNESS,Aspect.UNDEAD,Aspect.ENTROPY,Aspect.ELDRITCH,Aspect.POISON,Aspect.DEATH};
                setAspectTag(eerie[random.nextInt(6)].getTag());
            } else if(level().random.nextInt(10)!=0) { var primal=Aspect.getPrimalAspects(); setAspectTag(primal.get(level().random.nextInt(primal.size())).getTag()); }
            else { var compound=Aspect.getCompoundAspects(); setAspectTag(compound.get(level().random.nextInt(compound.size())).getTag()); }
        }
        if(level().getDifficulty()==Difficulty.PEACEFUL) discard();
        checkDespawn();
        prevAttackCounter=attackCounter;
        double dx=waypointX-getX(),dy=waypointY-getY(),dz=waypointZ-getZ(),squared=dx*dx+dy*dy+dz*dz;
        if(squared<1 || squared>3600) {
            waypointX=getX()+(random.nextFloat()*2-1)*16;
            waypointY=getY()+(random.nextFloat()*2-1)*16;
            waypointZ=getZ()+(random.nextFloat()*2-1)*16;
        }
        if(courseChangeCooldown--<=0) {
            courseChangeCooldown+=random.nextInt(5)+2;
            // The original deliberately steers using the PREVIOUS waypoint vector on a replacement tick.
            double distance=(float)Math.sqrt(squared);
            if(isCourseTraversable(distance)) {
                // Original divides 0/0 for a waypoint exactly at its position, corrupting the entity with NaN.
                if(distance>0) setDeltaMovement(getDeltaMovement().add(dx/distance*.1,dy/distance*.1,dz/distance*.1));
            } else { waypointX=getX();waypointY=getY();waypointZ=getZ(); }
        }
        if(targetedEntity!=null && targetedEntity.isRemoved()) targetedEntity=null;
        --aggroCooldown;
        if(level().random.nextInt(1000)==0 && (targetedEntity==null || aggroCooldown--<=0)) {
            targetedEntity=OriginalMobTargeting.closestVulnerablePlayer(this,16);
            if(targetedEntity!=null) aggroCooldown=50;
        }
        if(targetedEntity!=null && targetedEntity.distanceToSqr(this)<256) {
            double x=targetedEntity.getX()-getX(),z=targetedEntity.getZ()-getZ();
            yBodyRot=-(float)Math.atan2(x,z)*180/3.141593F;setYRot(yBodyRot);
            if(hasLineOfSight(targetedEntity)) {
                if(++attackCounter==20) {
                    playSound(sound("zap"),1,1.1F); WispZapNetworking.send(this,targetedEntity);
                    float damage=(float)getAttributeValue(Attributes.ATTACK_DAMAGE); Vec3 velocity=targetedEntity.getDeltaMovement();
                    if(Math.abs(velocity.x)>.1F || Math.abs(velocity.y)>.1F || Math.abs(velocity.z)>.1F) {
                        if(level().random.nextFloat()<.4F) targetedEntity.hurt(damageSources().mobAttack(this),damage);
                    } else if(level().random.nextFloat()<.66F) targetedEntity.hurt(damageSources().mobAttack(this),damage+1);
                    attackCounter=-20+level().random.nextInt(20);
                }
            } else if(attackCounter>0) --attackCounter;
        } else {
            Vec3 velocity=getDeltaMovement(); yBodyRot=-(float)Math.atan2(velocity.x,velocity.z)*180/3.141593F;setYRot(yBodyRot);
            if(attackCounter>0) --attackCounter;
        }
    }
    private boolean isCourseTraversable(double distance) {
        Vec3 step=new Vec3((waypointX-getX())/distance,(waypointY-getY())/distance,(waypointZ-getZ())/distance);
        AABB box=getBoundingBox();
        for(int i=1;i<distance;i++) { box=box.move(step); if(!level().noCollision(this,box)) return false; }
        BlockPos end=new BlockPos((int)waypointX,(int)waypointY,(int)waypointZ);
        if(!level().getFluidState(end).isEmpty()) return false;
        for(int i=0;i<11;i++) if(!level().isEmptyBlock(end.below(i))) return true;
        return false;
    }
    private static SoundEvent sound(String id) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)); }
    @Override protected SoundEvent getAmbientSound() { return sound("wisplive"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.FIRE_EXTINGUISH; }
    @Override protected SoundEvent getDeathSound() { return sound("wispdead"); }
    @Override protected float getSoundVolume() { return .25F; }
    @Override protected void dropFromLootTable(DamageSource source,boolean recent) {}
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height) { return OriginalMobLoot.spawn(this,stack,height); }
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recent) {
        var aspect=Aspect.getAspect(getAspectTag());
        if(aspect!=null) { var stack=new ItemStack(ThaumcraftEssences.WISP_ESSENCE.get()); ThaumcraftEssences.WISP_ESSENCE.get().setAspects(stack,new AspectList().add(aspect,2));spawnAtLocation(stack); }
        super.dropCustomDeathLoot(level,source,recent);
        if(recent) OriginalMobLoot.rare(OriginalMobLoot.looting(level,source),random::nextInt);
    }
    public static boolean checkWispSpawnRules(EntityType<? extends EntityWisp> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        AABB candidate=type.getDimensions().makeBoundingBox(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        if(level.getLevel().getEntitiesOfClass(EntityWisp.class,candidate.inflate(16)).size()>=8)return false;
        if(level.getDifficulty()==Difficulty.PEACEFUL) return false;
        // Original EntityWisp.isValidLightLevel: the darkened sky light and the block light.
        return OriginalLightRules.validWispLightLevel(level,pos,random);
    }
    @Override public boolean checkSpawnObstruction(LevelReader level) { return this.level().getEntitiesOfClass(EntityWisp.class,getBoundingBox().inflate(16)).size()<8 && super.checkSpawnObstruction(level); }
    @Override public int getMaxSpawnClusterSize() { return 2; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag);tag.putString("Type",getAspectTag()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);setAspectTag(tag.getString("Type")); }
}
