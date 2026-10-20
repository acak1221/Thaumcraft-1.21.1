package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.config.ThaumcraftTaintAnimals;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalTaintExperience;
import thaumcraft.common.lib.taint.TaintAnimalRules;
import thaumcraft.common.lib.world.BiomeColumns;

/** Original tainted seed:75HP/4damage, the original fibre spread, the original
 * flux taint aura and the original small tentacle growth. */
public class EntityTaintSeed extends net.minecraft.world.entity.monster.Monster implements ITaintedMob {
    public int boost;
    private boolean firstRun;
    public float attackAnim;
    public EntityTaintSeed(EntityType<? extends EntityTaintSeed> type,Level level) {
        super(type,level);
        xpReward=TaintAnimalRules.seedExperience(false);
    }
    public EntityTaintSeed(Level level) { this(ThaumcraftTaintAnimals.TAINT_SEED.get(),level); }
    /** Original getArea: the prime seed spreads over a doubled area. */
    protected int getArea() { return TaintAnimalRules.seedArea(isPrime()); }
    /** The original prime seed overrides every original value. */
    protected boolean isPrime() { return false; }
    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,TaintAnimalRules.seedMaxHealth(false))
                .add(Attributes.ATTACK_DAMAGE,TaintAnimalRules.seedAttackDamage(false))
                .add(Attributes.MOVEMENT_SPEED,0.0D);
    }
    @Override protected void registerGoals() {
        goalSelector.removeAllGoals(goal->true);
        targetSelector.removeAllGoals(goal->true);
        goalSelector.addGoal(1,new TaintAnimalMeleeGoal(this,null,1.0D,false) {
            @Override protected void beforeAttack(LivingEntity target) {
                if(level() instanceof ServerLevel server)server.broadcastEntityEvent(EntityTaintSeed.this,(byte)16);
                playSound(sound("tentacle"),getSoundVolume(),getVoicePitch());
            }
        });
        targetSelector.addGoal(0,new HurtByTargetGoal(this));
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag);tag.putInt("boost",boost); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);boost=tag.getInt("boost"); }
    @Override public void handleEntityEvent(byte event) {
        if(event==16)attackAnim=0.5F;
        else super.handleEntityEvent(event);
    }
    /** Original EntityTaintSeed: a rooted plant that never moves on its own. */
    @Override public void move(MoverType type,Vec3 movement) {
        super.move(type,new Vec3(0.0D,Math.min(0.0D,movement.y),0.0D));
    }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override public boolean isPushedByFluid() { return true; }
    @Override public void tick() {
        super.tick();
        if(level().isClientSide) {
            if(attackAnim>0.0F)attackAnim*=0.75F;
            if(attackAnim<0.001F)attackAnim=0.0F;
            return;
        }
        if(!(level() instanceof ServerLevel server))return;
        // Original EntityTaintSeed re-registers itself with the taint helper every1200 ticks;
        // the1.21.1 taint helper has no seed registry, so only the original cadence is kept.
        if(!firstRun||tickCount%TaintAnimalRules.SEED_REREGISTER_INTERVAL==0)firstRun=true;
        if(!isAlive())return;
        boolean tickFlag=tickCount%20==0;
        if(boost>0||tickFlag) {
            float mod=boost>0 ? 1.0F : growthSupply();
            if(boost>0)boost--;
            if(mod<=0.0F) {
                hurt(damageSources().starve(),0.5F);
            } else {
                spreadFibres(server);
            }
        }
        if(tickFlag) {
            LivingEntity target=getTarget();
            if(target!=null&&distanceToSqr(target)<TaintAnimalRules.seedTentacleDistanceSquared(getArea())&&canReach(target))spawnTentacles(target);
            for(var living:server.getEntitiesOfClass(LivingEntity.class,new AABB(blockPosition()).inflate(TaintAnimalRules.seedAuraRange(getArea())))) {
                if(living==this)continue;
                living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()),
                        TaintAnimalRules.seedFluxDuration(),TaintAnimalRules.seedFluxAmplifier(getArea()),false,false));
            }
        }
    }
    /** Original PathNavigate.canReach: the path has to end next to the target. */
    private boolean canReach(LivingEntity target) {
        var path=getNavigation().createPath(target,0);
        if(path==null)return false;
        var end=path.getEndNode();
        if(end==null)return false;
        int x=end.x-target.getBlockX(),z=end.z-target.getBlockZ();
        return (double)(x*x+z*z)<=2.25D;
    }
    /** Original TC6 flux saturation has no1.21.1 counterpart: the native taint blocks are the growth supply. */
    private float growthSupply() {
        if(EntityTaintacle.isTaintMaterial(level().getBlockState(blockPosition())))return 1.0F;
        for(Direction side:Direction.values()) {
            var next=blockPosition().relative(side);
            if(EntityTaintacle.isTaintMaterial(level().getBlockState(next)))return 1.0F;
        }
        return 0.0F;
    }
    /** Original TaintHelper.spreadFibres with the ignore flag. */
    private void spreadFibres(ServerLevel server) {
        int area=getArea();
        int x=TaintAnimalRules.seedSpreadOffset(area,random::nextInt);
        int y=TaintAnimalRules.seedSpreadHeight(area,random::nextInt);
        int z=TaintAnimalRules.seedSpreadOffset(area,random::nextInt);
        thaumcraft.common.blocks.BlockTaintFibres.spreadFibres(server,blockPosition().getX()+x,blockPosition().getY()+y,blockPosition().getZ()+z);
    }
    /** Original EntityTaintSeed.spawnTentacles. */
    protected void spawnTentacles(LivingEntity victim) {
        if(!(level() instanceof ServerLevel server))return;
        BlockPos at=victim.blockPosition();
        if(!EntityTaintacle.isTaintMaterial(server.getBlockState(at))&&!EntityTaintacle.isTaintMaterial(server.getBlockState(at.below())))return;
        var tentacle=new EntityTaintacleSmall(server);
        tentacle.moveTo(victim.getX()+(double)server.random.nextFloat()-(double)server.random.nextFloat(),victim.getY(),
                victim.getZ()+(double)server.random.nextFloat()-(double)server.random.nextFloat(),0.0F,0.0F);
        server.addFreshEntity(tentacle);
        playSound(sound("tentacle"),getSoundVolume(),getVoicePitch());
        // The original grows taint fibres in the Outer Lands biome; that biome is not ported yet,
        // so the native taint biome takes its place.
        var biome=BiomeColumns.surfaceHolder(server,at.getX(),at.getZ());
        if(biome!=null&&biome.is(ThaumcraftBiomes.TAINT)&&server.getBlockState(at).isAir()&&adjacentSolid(server,at))
            server.setBlock(at,ThaumcraftTaint.FIBRES.get().defaultBlockState().setValue(BlockTaintFibres.TYPE,0),3);
    }
    private static boolean adjacentSolid(ServerLevel level,BlockPos at) {
        for(Direction side:Direction.values()) {
            var next=at.relative(side);
            if(level.hasChunkAt(next)&&level.getBlockState(next).isFaceSturdy(level,next,side.getOpposite()))return true;
        }
        return false;
    }
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason) {
        if(level.getDifficulty()==Difficulty.PEACEFUL)return false;
        if(!(level instanceof ServerLevel server))return super.checkSpawnRules(level,reason);
        for(var other:server.getEntitiesOfClass(EntityTaintSeed.class,new AABB(blockPosition()).inflate(TaintAnimalRules.SEED_SPAWN_EXCLUSION_RADIUS))) {
            if(other!=this)return false;
        }
        return super.checkSpawnRules(level,reason);
    }
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height) { return OriginalMobLoot.spawn(this,stack,height); }
    @Override protected void dropExperience(Entity killer) {}
    @Override protected void tickDeath() {
        if(OriginalTaintExperience.finishDeathTick(this,()->lastHurtByPlayerTime>0||isAlwaysExperienceDropper(),
                ()->OriginalTaintExperience.originalReward(this,xpReward,this::getEquipmentDropChance)))return;
        super.tickDeath();
    }
    @Override protected void dropFromLootTable(DamageSource source,boolean recent) {}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recent) {
        dropFluxCrystals(level);
        OriginalTaintExperience.rememberEquipment(this);
        super.dropCustomDeathLoot(level,source,recent);
    }
    /** Original EntityTaintSeed.dropFewItems: one flux crystal. */
    protected void dropFluxCrystals(ServerLevel level) {
        spawnAtLocation(new ItemStack(ThaumcraftTaintAnimals.FLUX_CRYSTAL.get()),getBbHeight()/2F);
    }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.CHORUS_FLOWER_DEATH; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return sound("tentacle"); }
    @Override protected SoundEvent getDeathSound() { return sound("tentacle"); }
    @Override protected float getSoundVolume() { return getBbHeight()/8.0F; }
    @Override public float getVoicePitch() { return 1.3F-getBbHeight()/10.0F; }
    protected static SoundEvent sound(String id) { return SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft",id)); }
}
