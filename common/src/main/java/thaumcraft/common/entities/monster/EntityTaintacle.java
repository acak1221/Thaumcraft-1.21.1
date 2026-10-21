package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.blocks.BlockShockTrap;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.lib.taint.TaintacleRules;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalTaintExperience;
import thaumcraft.common.lib.world.BiomeColumns;

/** Original rooted hostile: nearest non-tainted living target, melee and remote small tentacles. */
public class EntityTaintacle extends Monster implements ITaintedMob {
    @FunctionalInterface public interface ClientEffects { void arise(EntityTaintacle entity); }
    private static ClientEffects clientEffects;
    // This key is not registered here: the real Outer Lands biome is a separate remaining unit.
    private static final ResourceKey<Biome> ELDRITCH=ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("thaumcraft","eldritch"));
    public float flailIntensity=1;
    private int attackTime;
    private boolean creatingTentacle;
    public EntityTaintacle(EntityType<? extends EntityTaintacle> type,Level level) { super(type,level);xpReward=10; }
    public EntityTaintacle(Level level) { this(ThaumcraftTaint.TAINTACLE.get(),level); }
    public static AttributeSupplier.Builder createAttributes() { return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.ATTACK_DAMAGE,7); }
    public static void setClientEffects(ClientEffects effects) { clientEffects=java.util.Objects.requireNonNull(effects); }
    @Override protected void registerGoals() {}
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return true; }
    @Override public float getPickRadius() { return .25F; }
    @Override public void move(MoverType type,Vec3 movement) { super.move(type,new Vec3(0,Math.min(0,movement.y),0)); }
    @Override protected void customServerAiStep() { if(getTarget()!=null)faceEntity(getTarget(),5); }
    public int originalAttackTime() { return attackTime; }
    public boolean getAgitationState() { return getTarget()!=null&&TaintacleRules.agitated(getTarget().distanceToSqr(this),getBbHeight()); }
    protected LivingEntity findOriginalTarget() {
        LivingEntity closest=null;double distance=Double.MAX_VALUE;
        var point=new AABB(getX(),getY(),getZ(),getX(),getY(),getZ()).inflate(getBbHeight()*6F,getBbHeight()*3F,getBbHeight()*6F);
        for(var candidate:level().getEntitiesOfClass(LivingEntity.class,point)) {
            if(candidate instanceof ITaintedMob)continue;
            double next=candidate.distanceToSqr(this);if(next<distance) {distance=next;closest=candidate;}
        }
        return closest;
    }
    @Override public void tick() {
        if(attackTime>0)attackTime--;
        super.tick();
        if(level() instanceof ServerLevel server&&tickCount%20==0){
            var biome=BiomeColumns.surfaceHolder(server,Mth.floor(getX()),Mth.floor(getZ()));
            if(biome!=null&&!biome.is(ThaumcraftBiomes.TAINT))actuallyHurt(damageSources().starve(),1);
        }
        if(level().isClientSide) {
            if(tickCount>getBbHeight()*10F&&(hurtTime>0||attackTime>0||getTarget()!=null&&getTarget().distanceTo(this)<getBbHeight())) {
                flailIntensity=TaintacleRules.flail(flailIntensity,true);
            } else flailIntensity=TaintacleRules.flail(flailIntensity,false);
            if(tickCount<getBbHeight()*10F&&onGround()&&clientEffects!=null)clientEffects.arise(this);
        }
        if(getTarget()==null)setTarget(findOriginalTarget());
        else if(getTarget().isAlive()&&getAgitationState()) {
            if(!level().isClientSide&&hasLineOfSight(getTarget()))attackOriginal(getTarget(),distanceTo(getTarget()));
        } else setTarget(null);
    }
    protected void attackOriginal(LivingEntity victim,float distance) {
        if(attackTime>0)return;
        if(distance<=getBbHeight()&&victim.getBoundingBox().maxY>getBoundingBox().minY&&victim.getBoundingBox().minY<getBoundingBox().maxY) {
            attackTime=20;doHurtTarget(victim);playSound(sound("tentacle"),getSoundVolume(),getVoicePitch());
        } else if(distance>getBbHeight()&&victim.onGround()&&!(this instanceof EntityTaintacleSmall))spawnTentacles(victim);
    }
    @Override public boolean doHurtTarget(Entity victim) {
        if(!(level() instanceof ServerLevel server))return false;
        DamageSource source=DamageSourceThaumcraft.causeTentacleDamage(this);
        float damage=OriginalTaintCombat.tentacleDamage(this);
        damage=EnchantmentHelper.modifyDamage(server,getMainHandItem(),victim,source,damage);
        float knockback=EnchantmentHelper.modifyKnockback(server,getMainHandItem(),victim,source,0);
        boolean hit=victim.hurt(source,damage);
        if(hit) {
            if(knockback>0) {
                victim.push(-Mth.sin(getYRot()*(float)Math.PI/180F)*knockback*.5F,.1,Mth.cos(getYRot()*(float)Math.PI/180F)*knockback*.5F);
                setDeltaMovement(getDeltaMovement().multiply(.6,1,.6));
            }
            EnchantmentHelper.doPostAttackEffects(server,victim,source);
            setLastHurtMob(victim);
        }
        return hit;
    }
    protected void spawnTentacles(Entity victim) {
        if(!(level() instanceof ServerLevel server)||!server.getServer().isSameThread()||server.getServer().getLevel(server.dimension())!=server
                ||isRemoved()||server.getEntity(getUUID())!=this||creatingTentacle||victim.isRemoved()||victim.level()!=server)return;
        BlockPos at=BlockPos.containing(victim.getX(),victim.getBoundingBox().minY,victim.getZ());
        if(!server.hasChunkAt(at)||!server.hasChunkAt(at.below()))return;
        if(!server.getWorldBorder().isWithinBounds(at))return;
        var biome=BiomeColumns.surfaceHolder(server,at.getX(),at.getZ());if(biome==null)return;
        boolean outer=biome.is(ELDRITCH);
        if(!outer&&!(biome.is(ThaumcraftBiomes.TAINT)&&(isTaintMaterial(server.getBlockState(at))||isTaintMaterial(server.getBlockState(at.below())))))return;
        // Native ADD callbacks may reenter the parent. Retire this one creation intent before publication.
        creatingTentacle=true;
        try {
            attackTime=40+server.random.nextInt(20);
            var small=new EntityTaintacleSmall(server);
            small.moveTo(victim.getX()+(double)server.random.nextFloat()-(double)server.random.nextFloat(),victim.getY(),
                    victim.getZ()+(double)server.random.nextFloat()-(double)server.random.nextFloat(),0,0);
            server.addFreshEntity(small);
            playSound(sound("tentacle"),getSoundVolume(),getVoicePitch());
            if(outer&&BlockShockTrap.semanticAir(server.getBlockState(at))&&adjacentSolid(server,at)) {
                // BiomeColumns is the original x/z operation adapted to the native quart column.
                BiomeColumns.set(server,at.getX(),at.getZ(),ThaumcraftBiomes.TAINT);
                server.setBlock(at,ThaumcraftTaint.FIBRES.get().defaultBlockState().setValue(BlockTaintFibres.TYPE,server.random.nextInt(4)==0?1:0),3);
            }
        } finally { creatingTentacle=false; }
    }
    public static boolean isTaintMaterial(BlockState state) { return state.is(ThaumcraftTaint.CRUST.get())||state.is(ThaumcraftTaint.SOIL.get())||state.is(ThaumcraftTaint.FLESH.get())||state.is(ThaumcraftTaint.FIBRES.get()); }
    private static boolean adjacentSolid(ServerLevel level,BlockPos at) {
        for(Direction d:Direction.values()) {var next=at.relative(d);if(level.hasChunkAt(next)&&level.getBlockState(next).isFaceSturdy(level,next,d.getOpposite()))return true;}return false;
    }
    @Override public boolean hurt(DamageSource source,float damage) {
        if(!(this instanceof EntityTaintacleSmall)&&source.getEntity()!=null&&distanceTo(source.getEntity())>16F&&!level().isClientSide)spawnTentacles(source.getEntity());
        boolean result=super.hurt(source,damage);
        // Original EntityMob retains an actual attacker even beyond the usual nearest-target radius.
        if(result&&source.getEntity() instanceof LivingEntity attacker&&attacker!=this)setTarget(attacker);
        return result;
    }
    public void faceEntity(Entity target,float speed) {
        float wanted=(float)(Math.atan2(target.getZ()-getZ(),target.getX()-getX())*180/Math.PI)-90F;
        setYRot(getYRot()+Mth.clamp(Mth.wrapDegrees(wanted-getYRot()),-speed,speed));
    }
    @Override public float getWalkTargetValue(BlockPos pos,LevelReader level){return .5F-level.getLightLevelDependentMagicValue(pos);}
    /**
     * The original light gate of the taintacle, without the world.
     *
     * <p>Oracle {@code EntityTaintacle.getCanSpawnHere:34-43}: the taintacle does not override
     * {@code isValidLightLevel}, so it inherits the mob gate - the saved sky light of the cell
     * against {@code nextInt(32)} and the BLOCK light of the cell against {@code nextInt(8)}.</p>
     */
    public static boolean originalLightGate(int skyLight,int blockLight,int skyRoll,int blockRoll) {
        return skyLight<=skyRoll&&blockLight<=blockRoll;
    }
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason){
        return !(this instanceof EntityTaintacleSmall)&&level instanceof ServerLevelAccessor server&&checkSpawnRules(ThaumcraftTaint.TAINTACLE.get(),server,reason,blockPosition(),random);
    }
    @Override public boolean checkSpawnObstruction(LevelReader level){return !level.containsAnyLiquid(getBoundingBox())&&level.noCollision(this);}
    public static boolean checkSpawnRules(EntityType<? extends EntityTaintacle> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        if(type==ThaumcraftTaint.TAINTACLE_SMALL.get()||level.getDifficulty()==Difficulty.PEACEFUL)return false;
        var biome=level instanceof WorldGenLevel generation?BiomeColumns.surfaceHolder(generation,pos.getX(),pos.getZ()):BiomeColumns.surfaceHolder(level.getLevel(),pos.getX(),pos.getZ());if(biome==null||!biome.is(ThaumcraftBiomes.TAINT))return false;
        var state=level.getBlockState(pos);
        if(!(state.is(ThaumcraftTaint.FIBRES.get())&&state.getValue(BlockTaintFibres.TYPE)==0||state.is(ThaumcraftTaint.SOIL.get())))return false;
        var area=new AABB(pos.getX()+.5,pos.getY(),pos.getZ()+.5,pos.getX()+.5,pos.getY(),pos.getZ()+.5).inflate(24,8,24);
        if(!level.getLevel().getEntitiesOfClass(EntityTaintacle.class,area).isEmpty())return false;
        // Oracle EntityTaintacle:34-43 - the taintacle does not override isValidLightLevel, so it
        // inherits the mob gate: the saved sky light against nextInt(32) and the BLOCK light of the
        // cell against nextInt(8). The port read the maximum raw brightness of the cell instead
        // of the block light, so the taintacle could not spawn under the open sky of the day.
        int skyRoll=random.nextInt(32);
        if(level.getBrightness(LightLayer.SKY,pos)>skyRoll)return false;
        int blockRoll=random.nextInt(8);
        return originalLightGate(level.getBrightness(LightLayer.SKY,pos),level.getBrightness(LightLayer.BLOCK,pos),skyRoll,blockRoll)
                &&.5F-level.getLightLevelDependentMagicValue(pos)>=0;
    }
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height) { return OriginalMobLoot.spawn(this,stack,height); }
    /** Vanilla1.7 delayed this inherited reward until the twentieth native death update. */
    @Override protected void dropExperience(Entity killer) {}
    @Override protected void tickDeath() {
        if(OriginalTaintExperience.finishDeathTick(this,()->lastHurtByPlayerTime>0||isAlwaysExperienceDropper(),
                ()->OriginalTaintExperience.originalReward(this,xpReward,this::getEquipmentDropChance)))return;
        super.tickDeath();
    }
    @Override protected void dropFromLootTable(DamageSource source,boolean recentlyHit) {}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit) {
        dropNativeEquipment(level,source,recentlyHit);
        spawnAtLocation(new ItemStack(ThaumcraftContent.MATERIALS.get(level.random.nextBoolean()?"tainted_goo":"taint_tendril").get()),getBbHeight()/2F);
    }
    protected final void dropNativeEquipment(ServerLevel level,DamageSource source,boolean recentlyHit){
        OriginalTaintExperience.rememberEquipment(this);
        super.dropCustomDeathLoot(level,source,recentlyHit);
    }
    protected static SoundEvent sound(String id) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)); }
    @Override public int getAmbientSoundInterval() { return 200; }
    @Override public SoundEvent getAmbientSound() { return sound("roots"); }
    @Override public SoundEvent getHurtSound(DamageSource source) { return sound("tentacle"); }
    @Override public SoundEvent getDeathSound() { return sound("tentacle"); }
    @Override protected float getSoundVolume() { return getBbHeight()/8F; }
    @Override public float getVoicePitch() { return 1.3F-getBbHeight()/10F; }
}
