package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftTaint;
import thaumcraft.common.config.ThaumcraftTaintAnimals;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.loot.OriginalTaintExperience;
import thaumcraft.common.lib.taint.TaintAnimalRules;

/** Original tainted crawler:8HP/2damage/0.275speed, the original silverfish
 * sounds, the original crouching fibre growth and the original flux taint
 * poison on a successful hit. */
public final class EntityTaintCrawler extends net.minecraft.world.entity.monster.Monster implements ITaintedMob {
    private BlockPos lastPos=new BlockPos(0,0,0);
    public EntityTaintCrawler(EntityType<? extends EntityTaintCrawler> type,Level level) {
        super(type,level);
        xpReward=3;
        // The original EntityAISwimming constructor lets the navigator swim.
        getNavigation().setCanFloat(true);
    }
    public EntityTaintCrawler(Level level) { this(ThaumcraftTaintAnimals.TAINT_CRAWLER.get(),level); }
    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.monster.Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,8)
                .add(Attributes.ATTACK_DAMAGE,2)
                .add(Attributes.MOVEMENT_SPEED,0.275D)
                .add(Attributes.FOLLOW_RANGE,TaintAnimalRules.FOLLOW_RANGE);
    }
    @Override protected void registerGoals() {
        goalSelector.removeAllGoals(goal->true);
        targetSelector.removeAllGoals(goal->true);
        goalSelector.addGoal(1,new TaintAnimalMeleeGoal.Swim(this));
        goalSelector.addGoal(2,new TaintAnimalMeleeGoal(this,null,1.0D,false) {
            @Override protected void afterAttack(LivingEntity target) { applyFluxTaint(target); }
        });
        goalSelector.addGoal(3,new RandomStrollGoal(this,1.0D));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8.0F));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));
    }
    /** Original EntityTaintCrawler.onUpdate: a living crawler grows taint fibres every40 ticks. */
    @Override public void tick() {
        super.tick();
        if(level().isClientSide||!isAlive()||tickCount%TaintAnimalRules.CRAWLER_FIBRE_INTERVAL!=0)return;
        if(lastPos.equals(blockPosition()))return;
        lastPos=blockPosition();
        if(!(level() instanceof ServerLevel server))return;
        BlockPos pos=blockPosition();
        BlockState state=server.getBlockState(pos);
        if(state.is(BlockTags.LEAVES))return;
        if(EntityTaintacle.isTaintMaterial(state))return;
        boolean growable=state.isAir()||state.canBeReplaced()||state.getBlock() instanceof BushBlock;
        if(!growable)return;
        if(!adjacentSolid(server,pos))return;
        if(BlockTaintFibres.isOnlyAdjacentToTaint(server,pos.getX(),pos.getY(),pos.getZ()))return;
        server.setBlock(pos,ThaumcraftTaint.FIBRES.get().defaultBlockState().setValue(BlockTaintFibres.TYPE,0),3);
    }
    private static boolean adjacentSolid(ServerLevel level,BlockPos at) {
        for(Direction side:Direction.values()) {
            var next=at.relative(side);
            if(level.hasChunkAt(next)&&level.getBlockState(next).isFaceSturdy(level,next,side.getOpposite()))return true;
        }
        return false;
    }
    /** Original EntityTaintCrawler.attackEntityAsMob: the flux taint poison on normal and hard. */
    private void applyFluxTaint(LivingEntity target) {
        int bound=TaintAnimalRules.crawlerFluxSeconds(level().getDifficulty().getId());
        if(!TaintAnimalRules.crawlerFluxPoison(bound,random::nextInt))return;
        target.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ThaumcraftInfusion.FLUX_TAINT.get()),bound*20,0,false,false));
    }
    @Override public float getPickRadius() { return 0.1F; }
    @Override protected float getSoundVolume() { return 0.7F; }
    @Override public float getVoicePitch() { return 0.7F; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SILVERFISH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SILVERFISH_DEATH; }
    @Override protected void playStepSound(BlockPos pos,BlockState state) {
        this.playSound(SoundEvents.SILVERFISH_STEP,0.15F,1.0F);
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
        // Original: one flux crystal in eight.
        if(level.random.nextInt(8)==0)spawnAtLocation(new ItemStack(ThaumcraftTaintAnimals.FLUX_CRYSTAL.get()),getBbHeight()/2F);
        OriginalTaintExperience.rememberEquipment(this);
        super.dropCustomDeathLoot(level,source,recent);
    }
}
