package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.lib.mods.ChampionHooks;
import thaumcraft.common.lib.mods.ChampionModifier;
import thaumcraft.common.lib.world.EldritchMobRules;

/**
 * Original TC4 Eldritch Golem: 250 health, 10 damage, 0.3 speed, 6 armour, fire immunity, the headless
 * phase with its eldritch beam and the crushing melee that can lose its head.
 */
public class EntityEldritchGolem extends EntityThaumcraftBoss implements RangedAttackMob, IEldritchMob {
    /**
     * Original {@code Thaumcraft.proxy.arcLightning} (oracle
     * {@code thaumcraft/common/entities/monster/boss/EntityEldritchGolem.java:302}): the client
     * draws the lightning arc between the golem and the block it picked. The server never draws
     * it, so the effect is a client hook installed by the loader client initializer.
     */
    public interface ClientEffects {
        void arc(EntityEldritchGolem golem, double x, double y, double z, double tx, double ty, double tz,
                 float red, float green, float blue, float heightGain);
    }
    private static ClientEffects clientEffects;
    private static final EntityDataAccessor<Boolean> HEADLESS=SynchedEntityData.defineId(EntityEldritchGolem.class,EntityDataSerializers.BOOLEAN);
    private int beamCharge;
    private boolean chargingBeam;
    private int arcing;
    private int arcX,arcY,arcZ;
    private int attackTimer;

    public EntityEldritchGolem(EntityType<? extends EntityEldritchGolem> type,Level level) {
        super(type,level);
        xpReward=EldritchMobRules.GOLEM.experience();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityThaumcraftBoss.bossAttributes(Monster.createMonsterAttributes())
                .add(Attributes.MAX_HEALTH,EldritchMobRules.GOLEM.maxHealth())
                .add(Attributes.ATTACK_DAMAGE,EldritchMobRules.GOLEM.attackDamage())
                .add(Attributes.MOVEMENT_SPEED,EldritchMobRules.GOLEM.movementSpeed())
                .add(Attributes.ARMOR,EldritchMobRules.GOLEM.armor());
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEADLESS,false);
    }

    /**
     * Original golem eye height is three, or 3.33 once its head is gone. The original set the
     * size with {@code func_70105_a(1.75F, 3.5F)} and answered the eye height with
     * {@code func_70047_e()}, so the headless form has its own eye height.
     *
     * <p>Oracle {@code EntityEldritchGolem.func_70047_e:133-135} answered the eye height of
     * the mob directly, so the headless golem looked from 3.33 and the explosion that took its
     * head started at {@code y + 3.33} (oracle {@code EntityEldritchGolem.func_70097_a:199}).
     */
    @Override public EntityDimensions getDefaultDimensions(Pose pose) {
        return EldritchMobRules.golemDimensions(isHeadless());
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1D,false) {
            @Override protected int getAttackInterval() { return EldritchMobRules.MELEE_AI_COOLDOWN; }
        });
        goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8D));
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.8D));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8.0F));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        // Original EntityAINearestAttackableTarget:52-58 - a target chance of zero scans
        // every tick. The native goal scans every fifth tick unless the interval is one.
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,1,true,false,null));
    }

    public boolean isHeadless() { return entityData.get(HEADLESS); }

    /**
     * Original EntityEldritchGolem:114-116 - the golem loses its head and its eye height grows
     * to 3.33. Minecraft 1.21.1 caches the eye height in the entity, so the dimensions have to
     * be refreshed whenever the flag changes.
     */
    public void setHeadless(boolean headless) {
        entityData.set(HEADLESS,headless);
        refreshDimensions();
    }

    /**
     * The client learns the headless flag through the data tracker, so it refreshes its
     * dimensions as well, exactly like the vanilla pose change does.
     */
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (HEADLESS.equals(key)) refreshDimensions();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("headless",isHeadless()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHeadless(tag.getBoolean("headless"));
        if(isHeadless())makeHeadless();
    }

    /** Original golem spawn claims a 100 tick spawn timer. */
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data) {
        super.finalizeSpawn(level,difficulty,reason,data);
        spawnTimer=EldritchMobRules.GOLEM_SPAWN_TIMER;
        return data;
    }

    @Override public void tick() {
        if(getSpawnTimer()==150)level().broadcastEntityEvent(this,(byte)18);
        if(getSpawnTimer()>0)heal(2F);
        super.tick();
        if(level().isClientSide) {
            // Original EntityEldritchGolem:301-305: the client draws the arc while it lasts.
            if(isHeadless()&&arcing>0) {
                float fade=1F-arcing/10F;
                if(clientEffects!=null)
                    clientEffects.arc(this,getX(),getY()+getBbHeight()/2F,getZ(),arcX+.5D,arcY+1,arcZ+.5D,
                            .65F+level().random.nextFloat()*.1F,1F,fade,fade);
                --arcing;
            }
            return;
        }
        if(isHeadless()&&beamCharge<=0)chargingBeam=true;
        if(isHeadless()&&chargingBeam) {
            ++beamCharge;
            level().broadcastEntityEvent(this,(byte)19);
            if(beamCharge==EldritchMobRules.GOLEM_BEAM_MAX_CHARGE)chargingBeam=false;
        }
    }

    /** Original golem ground interaction: it smashes the soft blocks it walks over. */
    @Override public void aiStep() {
        super.aiStep();
        if(attackTimer>0)--attackTimer;
        if(level().isClientSide)return;
        double motion=getDeltaMovement().x*getDeltaMovement().x+getDeltaMovement().z*getDeltaMovement().z;
        if(motion>2.500000277905201E-7&&level().random.nextInt(5)==0) {
            int x=Mth.floor(getX());
            int y=Mth.floor(getY()-.2F);
            int z=Mth.floor(getZ());
            var pos=new BlockPos(x,y,z);
            var state=level().getBlockState(pos);
            if(!state.isAir()&&level() instanceof ServerLevel server)
                server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,state),
                        getX()+(level().random.nextFloat()-.5F)*getBbWidth(),getY()+.1D,
                        getZ()+(level().random.nextFloat()-.5F)*getBbWidth(),4,.5D,0D,.5D,0D);
        }
        int x=Mth.floor(getX()+getDeltaMovement().x);
        int y=Mth.floor(getBoundingBox().minY);
        int z=Mth.floor(getZ()+getDeltaMovement().z);
        var pos=new BlockPos(x,y,z);
        BlockState state=level().getBlockState(pos);
        float hardness=state.getDestroySpeed(level(),pos);
        if(hardness>=0F&&hardness<=.15F)level().destroyBlock(pos,true);
    }

    /** Original golem loses its head on a hit that exceeds its remaining health. */
    @Override public boolean hurt(DamageSource source,float damage) {
        if(!level().isClientSide&&EldritchMobRules.golemBeheads(damage,getHealth(),isHeadless())) {
            setHeadless(true);
            spawnTimer=EldritchMobRules.GOLEM_SPAWN_TIMER;
            double radians=(getYRot()%360F/180F)*(float)Math.PI;
            double x=Math.cos(radians)*.75D,z=Math.sin(radians)*.75D;
            level().explode(this,getX()+x,getY()+getEyeHeight(),getZ()+z,EldritchMobRules.GOLEM_SMASH_STRENGTH,
                    net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            makeHeadless();
            return false;
        }
        return super.hurt(source,damage);
    }

    /** Original headless golem replaces its ranged task with the short cooldown beam. */
    void makeHeadless() {
        goalSelector.addGoal(2,new RangedAttackGoal(this,1.0D,5,5,24) {
            @Override public boolean canUse() {
                return super.canUse()&&getTarget()!=null&&distanceToSqr(getTarget())>=9.0D;
            }
        });
    }

    /** Original golem melee: a ten tick cooldown, three quarters damage and a small lift. */
    @Override public boolean doHurtTarget(Entity target) {
        if(attackTimer>0)return false;
        attackTimer=EldritchMobRules.GOLEM_MELEE_COOLDOWN;
        level().broadcastEntityEvent(this,(byte)4);
        if(!(target instanceof LivingEntity victim))return false;
        float damage=EldritchMobRules.golemMeleeDamage((float)getAttributeValue(Attributes.ATTACK_DAMAGE));
        boolean hit=victim.hurt(damageSources().mobAttack(this),damage);
        if(hit) {
            victim.setDeltaMovement(victim.getDeltaMovement().x,victim.getDeltaMovement().y+EldritchMobRules.GOLEM_MELEE_KNOCKBACK_Y,victim.getDeltaMovement().z);
            if(isHeadless())victim.push(-(float)Math.sin(getYRot()*(float)Math.PI/180F)*EldritchMobRules.GOLEM_HEADLESS_KNOCKBACK,.1D,
                    (float)Math.cos(getYRot()*(float)Math.PI/180F)*EldritchMobRules.GOLEM_HEADLESS_KNOCKBACK);
        }
        return hit;
    }

    /** Original headless beam: the charge is drained by fifteen to nineteen per shot. */
    @Override public void performRangedAttack(LivingEntity target,float distanceFactor) {
        if(!hasLineOfSight(target)||chargingBeam||beamCharge<=0)return;
        beamCharge=EldritchMobRules.golemBeamDrain(beamCharge,level().random.nextInt(5));
        var blast=new EntityGolemOrb(level(),this,target,false);
        Vec3 look=getLookAngle();
        blast.setPos(blast.getX()+look.x,blast.getY(),blast.getZ()+look.z);
        double dx=target.getX()+target.getDeltaMovement().x-getX();
        double dy=target.getY()-getY()-target.getBbHeight()/2D;
        double dz=target.getZ()+target.getDeltaMovement().z-getZ();
        blast.shoot(dx,dy,dz,EldritchMobRules.GOLEM_BEAM_SPEED,EldritchMobRules.GOLEM_BEAM_INACCURACY);
        playSound(sound("egattack"),1.0F,1.0F+level().random.nextFloat()*.1F);
        level().addFreshEntity(blast);
    }

    @Override public void handleEntityEvent(byte event) {
        if(event==4) {
            attackTimer=EldritchMobRules.GOLEM_MELEE_COOLDOWN;
            playSound(sound("mob.irongolem.throw"),1.0F,1.0F);
        } else if(event==18)spawnTimer=150;
        else if(event==19) {
            if(arcing==0) {
                float radius=2.0F+level().random.nextFloat()*2.0F;
                double radians=Math.toRadians(level().random.nextInt(360));
                int x=Mth.floor(getX()+radius*Math.cos(radians));
                int z=Mth.floor(getZ()+radius*Math.sin(radians));
                int y=Mth.floor(getY());
                int tries=0;
                while(tries<5&&level().getBlockState(new BlockPos(x,y,z)).isAir()) {
                    ++tries;
                    --y;
                }
                if(!level().getBlockState(new BlockPos(x,y+1,z)).isAir()&&!level().getBlockState(new BlockPos(x,y,z)).isAir()) {
                    arcX=x;
                    arcY=y;
                    arcZ=z;
                    arcing=EldritchMobRules.golemArcTicks(level().random.nextInt(5));
                    // Original EntityEldritchGolem:270: the arc starts with the golem's voice.
                    playSound(sound("jacobs"),.8F,1F+(level().random.nextFloat()-level().random.nextFloat())*.05F);
                }
            }
        } else super.handleEntityEvent(event);
    }

    public int getAttackTimer() { return attackTimer; }
    public int getBeamCharge() { return beamCharge; }
    /** The remaining lifetime of the client side lightning arc. */
    public int getArcing() { return arcing; }
    public int getArcX() { return arcX; }
    public int getArcY() { return arcY; }
    public int getArcZ() { return arcZ; }
    public static void setClientEffects(ClientEffects effects) { clientEffects=java.util.Objects.requireNonNull(effects); }

    /**
     * Original EntityEldritchGolem.generateName: the name of its champion modifier. The 1.7
     * original read the champion modifier out of the champion attribute of the mob, the
     * native port reads it out of the champion field.
     */
    @Override public void generateName() {
        int modifier = ChampionHooks.championId(this);
        if (modifier < 0) return;
        ChampionModifier champion = ChampionModifier.byId(modifier);
        if (champion == null) return;
        setCustomName(Component.translatable("entity.thaumcraft.eldritchgolem.champion.name",
                Component.translatable(champion.localizationKey())));
    }

    /** Original golem step sound. */
    @Override protected void playStepSound(BlockPos pos,BlockState state) { playSound(sound("mob.irongolem.walk"),1.0F,1.0F); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return sound("mob.irongolem.hit"); }
    @Override protected SoundEvent getDeathSound() { return sound("mob.irongolem.death"); }
    @Override public int getAmbientSoundInterval() { return EldritchMobRules.GOLEM.talkInterval(); }

    static SoundEvent sound(String id) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)); }

    /**
     * Original TC4 Golem Orb: zero gravity, a homing pull of 0.2 over the squared distance, a component
     * clamp of 0.25, a 160 or 240 tick lifetime and a hit damage of one or 0.6 times the owner attack.
     */
}
