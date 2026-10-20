package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.config.ThaumcraftTaintAnimals;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalTaintExperience;
import thaumcraft.common.lib.taint.TaintAnimalRules;
import thaumcraft.common.lib.taint.TaintAnimalRules.TaintedAnimal;
import thaumcraft.common.lib.world.BiomeColumns;

/** Original tainted sheep:20HP/3damage/0.25speed, the original AIConvertGrass
 * that turns dead bush and grass into taint fibres, and the original sheep
 * ambient, hurt and death sounds. */
public final class EntityTaintSheep extends Sheep implements ITaintedMob {
    private int sheepTimer;
    private int grassTimer;
    public EntityTaintSheep(EntityType<? extends EntityTaintSheep> type,Level level) {
        super(type,level);
        xpReward=TaintAnimalRules.EXPERIENCE;
        getNavigation().setCanFloat(true);
    }
    public EntityTaintSheep(Level level) { this(ThaumcraftTaintAnimals.TAINT_SHEEP.get(),level); }
    public static AttributeSupplier.Builder createAttributes() {
        return Sheep.createAttributes()
                .add(Attributes.MAX_HEALTH,TaintAnimalRules.maxHealth(TaintedAnimal.SHEEP))
                .add(Attributes.ATTACK_DAMAGE,TaintAnimalRules.attackDamage(TaintedAnimal.SHEEP))
                .add(Attributes.MOVEMENT_SPEED,TaintAnimalRules.movementSpeed(TaintedAnimal.SHEEP))
                .add(Attributes.FOLLOW_RANGE,TaintAnimalRules.FOLLOW_RANGE);
    }
    @Override protected void registerGoals() {
        goalSelector.removeAllGoals(goal->true);
        targetSelector.removeAllGoals(goal->true);
        goalSelector.addGoal(0,new TaintAnimalMeleeGoal.Swim(this));
        goalSelector.addGoal(3,new TaintAnimalMeleeGoal(this,Player.class,1.0D,false));
        goalSelector.addGoal(3,new TaintAnimalMeleeGoal(this,Villager.class,1.0D,true));
        goalSelector.addGoal(6,new RandomStrollGoal(this,1.0D));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,6.0F));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(0,new HurtByTargetGoal(this));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Villager.class,0,false,false,null));
    }
    /** Original AIConvertGrass: one tick in250, then forty ticks of eating. */
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if(grassTimer>0) {
            grassTimer=TaintAnimalRules.grassTimer(grassTimer);
            if(grassTimer==4)convertGrass();
            return;
        }
        if(!TaintAnimalRules.convertsGrass(random.nextInt(TaintAnimalRules.GRASS_TRIGGER_INTERVAL)))return;
        BlockPos pos=blockPosition();
        if(!(level() instanceof ServerLevel server))return;
        boolean deadBush=server.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.DEAD_BUSH);
        boolean grassBelow=server.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        if(!deadBush&&!grassBelow)return;
        grassTimer=TaintAnimalRules.GRASS_EAT_TICKS;
        getNavigation().stop();
        server.broadcastEntityEvent(this,(byte)10);
    }
    /** Original AIConvertGrass.updateTask: the block becomes taint fibres and the biome becomes taint. */
    private void convertGrass() {
        if(!(level() instanceof ServerLevel server))return;
        BlockPos pos=blockPosition();
        if(server.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.DEAD_BUSH)) {
            server.playSound(null,pos.getX()+.5D,pos.getY()+.5D,pos.getZ()+.5D,SoundEvents.GRASS_BREAK,SoundSource.BLOCKS,.8F,1.0F);
            server.destroyBlock(pos,false);
        } else if(server.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
            server.playSound(null,pos.getX()+.5D,pos.below().getY()+.5D,pos.getZ()+.5D,SoundEvents.GRASS_BREAK,SoundSource.BLOCKS,.8F,1.0F);
        } else {
            return;
        }
        server.setBlock(pos,ThaumcraftTaint.FIBRES.get().defaultBlockState().setValue(BlockTaintFibres.TYPE,0),3);
        BiomeColumns.set(server,pos.getX(),pos.getZ(),ThaumcraftBiomes.TAINT);
        getNavigation().stop();
    }
    /** Original eat animation timer. */
    public int getSheepTimer() { return sheepTimer; }
    /** Original func_44003_c: the head pitch while the sheep eats. */
    public float originalHeadAngle(float partial) { return TaintAnimalRules.sheepHeadAngle(sheepTimer,partial); }
    /** Original func_44002_d: the body yaw while the sheep eats. */
    public float originalBodyAngle(float partial) { return TaintAnimalRules.sheepBodyAngle(sheepTimer,partial,getYRot()); }
    @Override public void handleEntityEvent(byte event) {
        if(event==10)sheepTimer=TaintAnimalRules.GRASS_EAT_TICKS;
        else super.handleEntityEvent(event);
    }
    @Override public void aiStep() {
        if(level().isClientSide)sheepTimer=TaintAnimalRules.grassTimer(sheepTimer);
        super.aiStep();
        if(level().isClientSide&&tickCount<5)TaintSwarmVisuals.sploosh(this,10);
    }
    @Override protected boolean shouldDespawnInPeaceful() { return TaintAnimalRules.despawnsInPeaceful(TaintedAnimal.SHEEP); }
    @Override public int getAmbientSoundInterval() { return 400; }
    /** Original getMaxSpawnedInChunk has no1.21 counterpart. */
    public int originalMaxSpawnedInChunk() { return TaintAnimalRules.maxSpawnedInChunk(TaintedAnimal.SHEEP); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SHEEP_AMBIENT; }
    /** Original1.7 hurt sound is the ambient sound, not entity.sheep.hurt. */
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SHEEP_AMBIENT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SHEEP_AMBIENT; }
    @Override public float getVoicePitch() { return TaintAnimalRules.voicePitch(); }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public boolean canFallInLove() { return false; }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand) { return InteractionResult.PASS; }
    @Override public Sheep getBreedOffspring(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.AgeableMob parent) { return null; }
    /** Original isShearable ignores the shears: only the sheared flag matters. */
    @Override public boolean readyForShearing() { return !isSheared(); }
    /** Original onSheared drops one to three purple wool blocks. */
    @Override public void shear(SoundSource source) {
        setSheared(true);
        int count=TaintAnimalRules.shearWoolCount(random::nextInt);
        for(int i=0;i<count;i++) {
            level().playSound(null,getX(),getY(),getZ(),SoundEvents.SHEEP_SHEAR,source,1.0F,1.0F+random.nextFloat()*0.2F);
            spawnAtLocation(new ItemStack(Items.PURPLE_WOOL));
        }
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
        int drop=TaintAnimalRules.taintedDrop(TaintedAnimal.SHEEP,level.random::nextInt,level.random::nextBoolean);
        if(drop!=TaintAnimalRules.NO_DROP)spawnAtLocation(ThaumcraftTaintAnimals.taintMaterial(drop),getBbHeight()/2F);
        OriginalTaintExperience.rememberEquipment(this);
        super.dropCustomDeathLoot(level,source,recent);
    }
}
