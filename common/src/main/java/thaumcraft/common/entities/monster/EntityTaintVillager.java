package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.StrollThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ThaumcraftTaintAnimals;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalTaintExperience;
import thaumcraft.common.lib.taint.TaintAnimalRules;
import thaumcraft.common.lib.taint.TaintAnimalRules.TaintedAnimal;

/** Original tainted villager:30HP/4damage/0.3speed, the original village search
 * that restricts it to the nearest village and the original villager ambient,
 * hurt and death sounds. */
public final class EntityTaintVillager extends Villager implements ITaintedMob {
    private int randomTickDivider;
    public EntityTaintVillager(EntityType<? extends EntityTaintVillager> type,Level level) {
        super(type,level);
        xpReward=TaintAnimalRules.EXPERIENCE;
        getNavigation().setCanFloat(true);
        // The original navigator can open doors.
        if(getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation navigation)navigation.setCanOpenDoors(true);
    }
    public EntityTaintVillager(Level level) { this(ThaumcraftTaintAnimals.TAINT_VILLAGER.get(),level); }
    public static AttributeSupplier.Builder createAttributes() {
        return Villager.createAttributes()
                .add(Attributes.MAX_HEALTH,TaintAnimalRules.maxHealth(TaintedAnimal.VILLAGER))
                .add(Attributes.ATTACK_DAMAGE,TaintAnimalRules.attackDamage(TaintedAnimal.VILLAGER))
                .add(Attributes.MOVEMENT_SPEED,TaintAnimalRules.movementSpeed(TaintedAnimal.VILLAGER))
                .add(Attributes.FOLLOW_RANGE,TaintAnimalRules.FOLLOW_RANGE);
    }
    @Override protected void registerGoals() {
        goalSelector.removeAllGoals(goal->true);
        targetSelector.removeAllGoals(goal->true);
        goalSelector.addGoal(0,new TaintAnimalMeleeGoal.Swim(this));
        goalSelector.addGoal(2,new MoveBackToVillageGoal(this,1.0D,false));
        goalSelector.addGoal(2,new TaintAnimalMeleeGoal(this,Player.class,1.0D,false));
        goalSelector.addGoal(4,new OpenDoorGoal(this,true));
        goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,1.0D));
        goalSelector.addGoal(6,new StrollThroughVillageGoal(this,400));
        goalSelector.addGoal(9,new LookAtPlayerGoal(this,Player.class,3.0F));
        goalSelector.addGoal(9,new RandomStrollGoal(this,1.0D));
        goalSelector.addGoal(10,new RandomLookAroundGoal(this));
        targetSelector.addGoal(0,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
    }
    /** Original EntityTaintVillager.onUpdate: the village is searched every70 to119 ticks. */
    @Override public void tick() {
        super.tick();
        if(--randomTickDivider<=0) {
            randomTickDivider=TaintAnimalRules.villageSearchDelay(random.nextInt(50));
            searchVillage();
        }
    }
    /** Original: the nearest village within32 blocks becomes the home, no village clears it. */
    private void searchVillage() {
        if(!(level() instanceof ServerLevel server))return;
        BlockPos home=null;
        var homes=server.getPoiManager().getInRange(holder->holder.is(PoiTypes.HOME),blockPosition(),
                TaintAnimalRules.VILLAGE_SEARCH_RADIUS,PoiManager.Occupancy.ANY);
        for(var record:homes.toList()) {
            BlockPos pos=record.getPos();
            if(home==null||home.distSqr(pos)<blockPosition().distSqr(home))home=pos;
        }
        if(home==null) {
            clearRestriction();
            return;
        }
        int population=server.getEntitiesOfClass(Villager.class,new AABB(blockPosition()).inflate(TaintAnimalRules.VILLAGE_SEARCH_RADIUS)).size();
        restrictTo(home,(int)TaintAnimalRules.villageRestrictionRadius(population));
    }
    @Override public void aiStep() {
        super.aiStep();
        if(level().isClientSide&&tickCount<5)TaintSwarmVisuals.sploosh(this,10);
    }
    @Override protected boolean shouldDespawnInPeaceful() { return TaintAnimalRules.despawnsInPeaceful(TaintedAnimal.VILLAGER); }
    @Override public int getAmbientSoundInterval() { return 400; }
    /** Original getMaxSpawnedInChunk has no1.21 counterpart. */
    public int originalMaxSpawnedInChunk() { return TaintAnimalRules.maxSpawnedInChunk(TaintedAnimal.VILLAGER); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.VILLAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VILLAGER_DEATH; }
    @Override public float getVoicePitch() { return TaintAnimalRules.voicePitch(); }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand) { return InteractionResult.PASS; }
    @Override public Villager getBreedOffspring(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.AgeableMob parent) { return null; }
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height) { return OriginalMobLoot.spawn(this,stack,height); }
    @Override protected void dropExperience(Entity killer) {}
    @Override protected void tickDeath() {
        if(OriginalTaintExperience.finishDeathTick(this,()->lastHurtByPlayerTime>0||isAlwaysExperienceDropper(),
                ()->OriginalTaintExperience.originalReward(this,xpReward,this::getEquipmentDropChance)))return;
        super.tickDeath();
    }
    @Override protected void dropFromLootTable(DamageSource source,boolean recent) {}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recent) {
        int drop=TaintAnimalRules.taintedDrop(TaintedAnimal.VILLAGER,level.random::nextInt,level.random::nextBoolean);
        if(drop!=TaintAnimalRules.NO_DROP)spawnAtLocation(ThaumcraftTaintAnimals.taintMaterial(drop),getBbHeight()/2F);
        OriginalTaintExperience.rememberEquipment(this);
        super.dropCustomDeathLoot(level,source,recent);
        // Original: the gold coin drops with a rising chance per looting level.
        if(TaintAnimalRules.dropsGoldCoin(OriginalMobLoot.looting(level,source),level.random::nextInt))
            spawnAtLocation(ThaumcraftTaintAnimals.goldCoin(),1.5F);
    }
}
