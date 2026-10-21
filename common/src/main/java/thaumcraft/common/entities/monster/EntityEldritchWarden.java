package thaumcraft.common.entities.monster;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.entities.IThaumcraftBoss;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftLoot;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;
import thaumcraft.common.lib.mods.ChampionHooks;
import thaumcraft.common.lib.mods.ChampionModifier;
import thaumcraft.common.lib.ward.WardRules;
import thaumcraft.common.lib.world.EldritchMobRules;

/**
 * Original shared TC4 boss base (oracle {@code EntityThaumcraftBoss}).
 *
 * <p>It owns the spawn protection timer, the per attacker aggro table, the multiplayer health and damage
 * scaling, the 35 damage cap with its 200 tick enrage, the 24 block home, the boss bar and the inherited
 * eldritch drops. The Warden and the Golem both extend it.
 */
abstract class EntityThaumcraftBoss extends Monster implements IEldritchMob, IThaumcraftBoss {
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(EntityThaumcraftBoss.class,EntityDataSerializers.INT);
    /** Original modifier identities of the multiplayer health and damage scaling. */
    private static final ResourceLocation[] HEALTH_BONUS={modifierId("health_buff_1"),modifierId("health_buff_2"),
            modifierId("health_buff_3"),modifierId("health_buff_4"),modifierId("health_buff_5")};
    private static final ResourceLocation[] DAMAGE_BONUS={modifierId("damage_buff_1"),modifierId("damage_buff_2"),
            modifierId("damage_buff_3"),modifierId("damage_buff_4"),modifierId("damage_buff_5")};
    private final Map<Integer,Integer> aggro=new HashMap<>();
    protected int spawnTimer;
    private boolean aiFrozen;
    private final ServerBossEvent bossEvent;
    private float previousMaxHealth;

    private static ResourceLocation modifierId(String id) { return ResourceLocation.fromNamespaceAndPath("thaumcraft",id); }

    EntityThaumcraftBoss(EntityType<? extends EntityThaumcraftBoss> type,Level level) {
        super(type,level);
        xpReward=EldritchMobRules.WARDEN.experience();
        bossEvent=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
        previousMaxHealth=getMaxHealth();
    }

    /** Original boss attribute row: follow range 40 and knockback resistance 0.95. */
    static AttributeSupplier.Builder bossAttributes(AttributeSupplier.Builder builder) {
        return builder.add(Attributes.FOLLOW_RANGE,40F).add(Attributes.KNOCKBACK_RESISTANCE,EldritchMobRules.BOSS_KNOCKBACK_RESISTANCE);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(ANGER,0); }
    public int getAnger() { return entityData.get(ANGER); }
    public void setAnger(int anger) { entityData.set(ANGER,anger); }
    public int getSpawnTimer() { return spawnTimer; }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // The temporary AI freeze of the spawn timer and the field frenzy is never persisted.
        tag.remove("NoAI");
        if(hasRestriction()) {
            tag.putInt("HomeX",getRestrictCenter().getX());
            tag.putInt("HomeY",getRestrictCenter().getY());
            tag.putInt("HomeZ",getRestrictCenter().getZ());
            tag.putInt("HomeD",(int)getRestrictRadius());
        }
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("HomeD"))restrictTo(new BlockPos(tag.getInt("HomeX"),tag.getInt("HomeY"),tag.getInt("HomeZ")),tag.getInt("HomeD"));
    }

    /** Original boss suppresses its task list while the spawn protection timer runs. */
    protected boolean shouldFreezeAi() { return spawnTimer>0; }

    /** Original boss spawn replaces the vanilla spawn mutation and only claims a 24 block home. */
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data) {
        restrictTo(new BlockPos((int)getX(),(int)getY(),(int)getZ()),(int)EldritchMobRules.BOSS_HOME_RADIUS);
        return data;
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if(spawnTimer==0&&getTarget()!=null&&!getTarget().isAlive())setTarget(null);
    }

    @Override public void tick() {
        super.tick();
        if(spawnTimer>0)--spawnTimer;
        if(getAnger()>0)setAnger(getAnger()-1);
        boolean freeze=shouldFreezeAi();
        if(freeze&&!aiFrozen) { aiFrozen=true; setNoAi(true); }
        else if(!freeze&&aiFrozen) { aiFrozen=false; setNoAi(false); }
        if(level().isClientSide) {
            if(level().random.nextInt(15)==0&&getAnger()>0) {
                double dx=level().random.nextGaussian()*.02D,dy=level().random.nextGaussian()*.02D,dz=level().random.nextGaussian()*.02D;
                if(level() instanceof ServerLevel server)server.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        getX()+level().random.nextFloat()*getBbWidth()-.5F*getBbWidth(),getBoundingBox().maxY+level().random.nextFloat()*.5F,
                        getZ()+level().random.nextFloat()*getBbWidth()-.5F*getBbWidth(),1,dx,dy,dz,0D);
            }
        } else {
            if(tickCount%EldritchMobRules.BOSS_HEAL_INTERVAL==0)heal(EldritchMobRules.BOSS_HEAL_AMOUNT);
            if(getTarget()!=null&&tickCount%EldritchMobRules.BOSS_AGGRO_INTERVAL==0)updateAggroAndScaling();
        }
        bossEvent.setProgress(Math.max(0F,Math.min(1F,getHealth()/getMaxHealth())));
        bossEvent.setName(getDisplayName());
    }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossEvent.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossEvent.removePlayer(player); }

    /**
     * Original aggro table walk. The oracle resolves the candidate entity with the current target id and
     * overwrites its highest marker with that id, so the retarget almost never happens; both quirks are kept.
     */
    private void updateAggroAndScaling() {
        LivingEntity current=getTarget();
        int currentId=current.getId();
        int currentAggro=aggro.getOrDefault(currentId,0);
        int highest=currentAggro;
        int players=0;
        Entity newTarget=null;
        Map<Integer,Integer> stale=new LinkedHashMap<>();
        for(Map.Entry<Integer,Integer> entry:aggro.entrySet()) {
            int candidateAggro=entry.getValue();
            if(!EldritchMobRules.bossAggroQualifies(candidateAggro,currentAggro,highest))continue;
            Entity candidate=level().getEntity(currentId);
            if(candidate==null||!candidate.isAlive()||distanceToSqr(candidate)>EldritchMobRules.BOSS_STALE_DISTANCE_SQUARED) {
                stale.put(entry.getKey(),candidateAggro);
                continue;
            }
            newTarget=candidate;
            highest=entry.getKey();
            if(!(candidate instanceof Player))continue;
            ++players;
        }
        for(Integer id:stale.keySet())aggro.remove(id);
        if(newTarget!=null&&newTarget!=current)setTarget((LivingEntity)newTarget);
        applyPlayerScaling(EldritchMobRules.bossBonusPlayers(players));
    }

    /** Original multiplayer scaling: up to five extra players add 50 health and 0.5 damage each. */
    private void applyPlayerScaling(int bonusPlayers) {
        var health=getAttribute(Attributes.MAX_HEALTH);
        var damage=getAttribute(Attributes.ATTACK_DAMAGE);
        for(int i=0;i<EldritchMobRules.BOSS_MAX_BONUS_PLAYERS;i++) {
            health.removeModifier(HEALTH_BONUS[i]);
            damage.removeModifier(DAMAGE_BONUS[i]);
        }
        for(int i=0;i<bonusPlayers;i++) {
            health.addPermanentModifier(new AttributeModifier(HEALTH_BONUS[i],EldritchMobRules.BOSS_HEALTH_BONUS,AttributeModifier.Operation.ADD_VALUE));
            damage.addPermanentModifier(new AttributeModifier(DAMAGE_BONUS[i],EldritchMobRules.BOSS_DAMAGE_BONUS,AttributeModifier.Operation.ADD_VALUE));
        }
        float oldMax=previousMaxHealth;
        float newMax=getMaxHealth();
        if(oldMax>0F)setHealth(Math.max(1F,Math.min(newMax,getHealth()*(newMax/oldMax))));
        previousMaxHealth=newMax;
    }

    @Override public boolean isInvulnerableTo(DamageSource source) { return super.isInvulnerableTo(source)||spawnTimer>0; }
    @Override public boolean isPushable() { return super.isPushable()&&spawnTimer<=0; }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean canPickUpLoot() { return false; }
    @Override public boolean isAlliedTo(Entity entity) { return entity instanceof IEldritchMob||super.isAlliedTo(entity); }

    /** Original boss hit handling: aggro bookkeeping, the 35 damage cap and the 200 tick enrage. */
    @Override public boolean hurt(DamageSource source,float damage) {
        if(!level().isClientSide) {
            if(source.getEntity() instanceof LivingEntity attacker) {
                int id=attacker.getId();
                int value=(int)damage;
                if(aggro.containsKey(id))value+=aggro.get(id);
                aggro.put(id,value);
            }
            if(damage>EldritchMobRules.BOSS_DAMAGE_CAP) {
                if(getAnger()==0) {
                    addEffect(new MobEffectInstance(MobEffects.REGENERATION,200,EldritchMobRules.bossRegenerationAmplifier(damage)));
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,EldritchMobRules.bossStrengthAmplifier(damage)));
                    addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,EldritchMobRules.bossSpeedAmplifier(damage)));
                    setAnger(EldritchMobRules.BOSS_ANGER_TICKS);
                    if(source.getEntity() instanceof Player player)player.displayClientMessage(Component.translatable("tc.boss.enrage"),false);
                }
                damage=EldritchMobRules.bossDamageCap(damage);
            }
        }
        return super.hurt(source,damage);
    }

    /** Original boss drops: one special eldritch object at half height and one rare loot bag at 1.5. */
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit) {
        super.dropCustomDeathLoot(level,source,recentlyHit);
        dropSpecialItem(this,level,EldritchMobRules.ELDRITCH_OBJECT_ID,getBbHeight()/2F);
        spawnAtLocation(new net.minecraft.world.item.ItemStack(ThaumcraftLoot.LOOT_BAG_RARE.get()),1.5F);
    }

    /** Original EntityUtils.entityDropSpecialItem: a fire resistant homing item entity. */
    static void dropSpecialItem(Entity source,ServerLevel level,String itemId,float height) {
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("thaumcraft",itemId));
        if(item.isEmpty())return;
        var special=new EntitySpecialItem(ThaumcraftContent.SPECIAL_ITEM.get(),level);
        special.setPos(source.getX(),source.getY()+height,source.getZ());
        special.setItem(new net.minecraft.world.item.ItemStack(item.get()));
        special.setDeltaMovement(0,.1F,0);
        special.setDefaultPickUpDelay();
        level.addFreshEntity(special);
    }

    /**
     * Original EntityThaumcraftBoss.generateName: the base boss does not name itself at all,
     * the eldritch warden and the eldritch construct do.
     */
    @Override public void generateName() {}
}

/**
 * Original TC4 Eldritch Warden: 200 health, 10 damage, 0.33 speed, 4 armour, the eldritch orb and
 * screech attack pair, the 66 percent ward and the one hundred fifty tick field frenzy.
 */
public class EntityEldritchWarden extends EntityThaumcraftBoss implements RangedAttackMob, IEldritchMob {
    private static final EntityDataAccessor<Integer> TITLE=SynchedEntityData.defineId(EntityEldritchWarden.class,EntityDataSerializers.INT);
    private boolean frenzy;
    private int frenzyCounter;
    private boolean lastBlast;
    public float armLiftL;
    public float armLiftR;

    public EntityEldritchWarden(EntityType<? extends EntityEldritchWarden> type,Level level) {
        super(type,level);
        xpReward=EldritchMobRules.WARDEN.experience();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityThaumcraftBoss.bossAttributes(Monster.createMonsterAttributes())
                .add(Attributes.MAX_HEALTH,EldritchMobRules.WARDEN.maxHealth())
                .add(Attributes.ATTACK_DAMAGE,EldritchMobRules.WARDEN.attackDamage())
                .add(Attributes.MOVEMENT_SPEED,EldritchMobRules.WARDEN.movementSpeed())
                .add(Attributes.ARMOR,EldritchMobRules.WARDEN.armor());
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TITLE,0);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        goalSelector.addGoal(2,new net.minecraft.world.entity.ai.goal.RangedAttackGoal(this,1.0D,20,40,24) {
            @Override public boolean canUse() {
                // Original AILongRangeAttack refuses targets closer than three blocks.
                return super.canUse()&&getTarget()!=null&&distanceToSqr(getTarget())>=9.0D;
            }
        });
        goalSelector.addGoal(3,new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this,1.1D,false) {
            @Override protected int getAttackInterval() { return EldritchMobRules.MELEE_AI_COOLDOWN; }
        });
        goalSelector.addGoal(5,new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this,.8D));
        goalSelector.addGoal(7,new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this,1.0D));
        goalSelector.addGoal(8,new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,Player.class,8.0F));
        goalSelector.addGoal(8,new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        targetSelector.addGoal(2,new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,
                living->living.getType().getDescriptionId().startsWith("entity.thaumcraft.cultist")));
    }

    public int getTitle() { return entityData.get(TITLE); }
    public void setTitle(int title) { entityData.set(TITLE,title); }
    public String getTitleName() { return EldritchMobRules.wardenTitle(getTitle()); }

    /**
     * Original EntityEldritchWarden.generateName: the title of the warden and the name of its
     * champion modifier. The 1.7 original read the champion modifier out of the champion
     * attribute of the mob, the native port reads it out of the champion field.
     */
    @Override public void generateName() {
        int modifier = ChampionHooks.championId(this);
        if (modifier < 0) return;
        ChampionModifier champion = ChampionModifier.byId(modifier);
        if (champion == null) return;
        setCustomName(Component.translatable("entity.thaumcraft.eldritchwarden.champion.name",
                getTitleName(), Component.translatable(champion.localizationKey())));
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("title",(byte)getTitle());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); setTitle(tag.getByte("title")); }

    /** Original warden spawn: 150 tick spawn timer, a random title and the 66 percent ward. */
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data) {
        super.finalizeSpawn(level,difficulty,reason,data);
        spawnTimer=EldritchMobRules.WARDEN_SPAWN_TIMER;
        setTitle(getRandom().nextInt(EldritchMobRules.WARDEN_TITLES.length));
        float base=(float)getAttribute(Attributes.MAX_HEALTH).getBaseValue();
        // Oracle EntityEldritchWarden:318 - the warden adds two thirds of its maximum health to its
        // absorption. 1.21.1 clamps the absorption by the max absorption attribute, so the ward raises
        // that attribute first.
        WardRules.addWard(this,base*EldritchMobRules.WARDEN_WARD_RATIO);
        return data;
    }

    /** Original warden freezes its task list while the field frenzy runs. */
    @Override protected boolean shouldFreezeAi() { return spawnTimer>0||frenzyCounter>0; }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if(frenzyCounter!=0)return;
        if(EldritchMobRules.wardenWardsRegenerate(hurtTime,tickCount)) {
            int cap=EldritchMobRules.wardenWardCap((float)getAttribute(Attributes.MAX_HEALTH).getBaseValue());
            // Oracle EntityEldritchWarden:164-169 - the warden regains one point of its ward every
            // twenty fifth tick, up to the ceiling of two thirds of its maximum health.
            float absorption=getAbsorptionAmount();
            float regenerated=EldritchMobRules.wardenWardRegeneration(absorption,cap);
            if(regenerated!=absorption)WardRules.addWard(this,regenerated-absorption);
        }
    }

    @Override public void tick() {
        if(getSpawnTimer()==EldritchMobRules.WARDEN_SPAWN_TIMER)level().broadcastEntityEvent(this,(byte)18);
        super.tick();
        if(level().isClientSide) {
            if(armLiftL>0F)armLiftL-=.05F;
            if(armLiftR>0F)armLiftR-=.05F;
        }
    }

    /** Original warden field and field frenzy, both server side only. */
    @Override public void aiStep() {
        super.aiStep();
        if(level().isClientSide)return;
        fillEldritchField();
        if(frenzyCounter>0) {
            if(EldritchMobRules.wardenFrenzyTeleports(frenzyCounter))teleportHome();
            performFieldFrenzy();
        }
    }


    /** Original warden footprint of four airy samples around itself. */
    private void fillEldritchField() {
        BlockState field=eldritchFieldState();
        if(field==null)return;
        int y=net.minecraft.util.Mth.floor(getY());
        for(int sample=0;sample<4;sample++) {
            int x=net.minecraft.util.Mth.floor(getX()+(sample%2*2-1)*.25F);
            int z=net.minecraft.util.Mth.floor(getZ()+(sample/2%2*2-1)*.25F);
            var pos=new BlockPos(x,y,z);
            if(!level().getBlockState(pos).isAir())continue;
            level().setBlock(pos,field,3);
        }
    }

    /** Original field frenzy lays shrinking rings of eldritch field blocks around the warden. */
    private void performFieldFrenzy() {
        if(EldritchMobRules.wardenFrenzyRingDue(frenzyCounter)) {
            level().broadcastEntityEvent(this,(byte)17);
            double radius=EldritchMobRules.wardenFrenzyRadius(frenzyCounter);
            int step=EldritchMobRules.wardenFrenzyStep(frenzyCounter);
            int x=net.minecraft.util.Mth.floor(getX()),y=net.minecraft.util.Mth.floor(getY()),z=net.minecraft.util.Mth.floor(getZ());
            BlockState field=eldritchFieldState();
            for(int sample=0;sample<EldritchMobRules.wardenFrenzySamples(frenzyCounter);sample++) {
                double radians=EldritchMobRules.wardenFrenzyAngle(sample,step);
                int dx=(int)(radius*Math.cos(radians)),dz=(int)(radius*Math.sin(radians));
                var pos=new BlockPos(x+dx,y,z+dz);
                if(!level().getBlockState(pos).isAir()||!level().getBlockState(pos.below()).isFaceSturdy(level(),pos.below(),net.minecraft.core.Direction.UP))continue;
                if(field==null)continue;
                level().setBlock(pos,field,3);
                level().scheduleTick(pos,field.getBlock(),250+getRandom().nextInt(150));
                // Original EntityEldritchWarden:231 - three of every ten field blocks draw the arc
                // lightning, the rest draw the plain sparkle of the original PacketFXBlockSparkle.
                if(level() instanceof ServerLevel server&&getRandom().nextFloat()<.3F)
                    thaumcraft.common.lib.network.BlockFxNetworking.sendArc(server,this,pos);
            }
            playSound(sound("zap"),1.0F,.9F+level().random.nextFloat()*.1F);
        }
        --frenzyCounter;
    }

    /** Original warden teleport: home column first, randomised offsets only while the landing is invalid. */
    protected void teleportHome() {
        if(!hasRestriction())return;
        BlockPos home=getRestrictCenter();
        double oldX=getX(),oldY=getY(),oldZ=getZ();
        int x=home.getX(),y=home.getY(),z=home.getZ();
        if(!level().isLoaded(home))return;
        boolean landed=false;
        int tries=20;
        while(!landed&&tries-->0) {
            var below=level().getBlockState(new BlockPos(x,y-1,z));
            var here=level().getBlockState(new BlockPos(x,y,z));
            if(below.blocksMotion()&&!here.blocksMotion()) { landed=true; break; }
            x=home.getX()+level().random.nextInt(8)-level().random.nextInt(8);
            z=home.getZ()+level().random.nextInt(8)-level().random.nextInt(8);
        }
        if(!landed) { setPos(oldX,oldY,oldZ); return; }
        setPos(x+.5D,y+.1D,z+.5D);
        if(!level().noCollision(this)) { setPos(oldX,oldY,oldZ); return; }
        if(level() instanceof ServerLevel server)for(int i=0;i<128;i++) {
            double progress=i/127D;
            server.sendParticles(ParticleTypes.PORTAL,oldX+(getX()-oldX)*progress+level().random.nextDouble()-.5D,
                    oldY+(getY()-oldY)*progress+level().random.nextDouble(),oldZ+(getZ()-oldZ)*progress+level().random.nextDouble()-.5D,
                    1,0D,0D,0D,0D);
        }
        playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,1.0F,1.0F);
    }

    /** Original warden rejects drown and wither damage while it is not frenzied. */
    @Override public boolean hurt(DamageSource source,float damage) {
        if(frenzyCounter>0||source.is(DamageTypes.DROWN)||source.is(DamageTypes.WITHER))return false;
        boolean hurt=super.hurt(source,damage);
        if(!level().isClientSide&&hurt&&!frenzy&&getAbsorptionAmount()<=0F) {
            frenzy=true;
            frenzyCounter=EldritchMobRules.WARDEN_FREENZY_TICKS;
        }
        return hurt;
    }

    /** Original warden ranged attack: an eldritch orb, or a blinding screech with warp. */
    @Override public void performRangedAttack(LivingEntity target,float distanceFactor) {
        if(EldritchMobRules.wardenUsesOrb(level().random.nextFloat())) {
            var blast=new EntityEldritchOrb(level(),this);
            lastBlast=!lastBlast;
            level().broadcastEntityEvent(this,lastBlast?(byte)16:(byte)15);
            int angle=EldritchMobRules.wardenArmAngle(lastBlast);
            double radians=(getYRot()+angle)%360F/180F*(float)Math.PI;
            double x=Math.cos(radians)*.5D,z=Math.sin(radians)*.5D;
            blast.setPos(blast.getX()-x,blast.getY()-.13D,blast.getZ()-z);
            blast.shoot(target.getX()+target.getDeltaMovement().x-getX(),target.getY()-getY()-target.getBbHeight()/2,
                    target.getZ()+target.getDeltaMovement().z-getZ(),1.0F,2.0F);
            playSound(sound("egattack"),2.0F,1.0F+level().random.nextFloat()*.1F);
            level().addFreshEntity(blast);
        } else if(hasLineOfSight(target)) {
            broadcastSonic();
            target.push(-(float)Math.sin(getYRot()*(float)Math.PI/180F)*1.5F,.1D,(float)Math.cos(getYRot()*(float)Math.PI/180F)*1.5F);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,EldritchMobRules.WARDEN_SCREECH_TICKS));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,EldritchMobRules.WARDEN_SCREECH_TICKS));
            if(target instanceof ServerPlayer player)thaumcraft.common.lib.WarpEvents.addTemporaryWarp(player,
                    EldritchMobRules.wardenWarpAmount(level().random.nextInt(3)));
            playSound(sound("egscreech"),4.0F,1.0F+level().random.nextFloat()*.1F);
        }
    }

    /** Original PacketFXSonic broadcast to every player within thirty two blocks. */
    private void broadcastSonic() {
        if(!(level() instanceof ServerLevel server))return;
        var payload=new thaumcraft.common.lib.network.GuardianSonicPayload(getId());
        for(ServerPlayer player:server.players())
            if(player.distanceToSqr(this)<=32D*32D)dev.architectury.networking.NetworkManager.sendToPlayer(player,payload);
    }

    @Override public void handleEntityEvent(byte event) {
        if(event==15)armLiftL=.5F;
        else if(event==16)armLiftR=.5F;
        else if(event==17) { armLiftL=.9F; armLiftR=.9F; }
        else if(event==18)spawnTimer=EldritchMobRules.WARDEN_SPAWN_TIMER;
        else super.handleEntityEvent(event);
    }

    @Override protected SoundEvent getAmbientSound() { return sound("egidle"); }
    @Override protected SoundEvent getDeathSound() { return sound("egdeath"); }
    @Override public int getAmbientSoundInterval() { return EldritchMobRules.WARDEN.talkInterval(); }
    @Override protected float getSoundVolume() { return 1.5F; }

    /** Original eldritch field block, resolved from its original registry id. */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static BlockState eldritchFieldState() {
        var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("thaumcraft",EldritchMobRules.AIRY_BLOCK_ID));
        if(block.isEmpty())return null;
        BlockState state=block.get().defaultBlockState();
        for(var property:state.getProperties()) {
            if(!"type".equals(property.getName()))continue;
            for(var value:((net.minecraft.world.level.block.state.properties.Property)property).getPossibleValues())
                if(value instanceof Integer number&&number==11)return state.setValue((net.minecraft.world.level.block.state.properties.Property)property,11);
        }
        return state;
    }

    static SoundEvent sound(String id) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)); }
}
