package thaumcraft.common.entities.monster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.config.ThaumcraftEldritchMobs;
import thaumcraft.common.lib.loot.OriginalMobLoot;
import thaumcraft.common.lib.world.EldritchMobRules;

/**
 * Original TC4 Inhabited Zombie: 30 health, 5 damage, cultist plate armour and the conversion into a
 * helmeted eldritch crab when it dies.
 */
public class EntityInhabitedZombie extends Zombie implements IEldritchMob {
    public EntityInhabitedZombie(EntityType<? extends EntityInhabitedZombie> type,Level level) {
        super(type,level);
        xpReward=EldritchMobRules.INHABITED_ZOMBIE.experience();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH,EldritchMobRules.INHABITED_ZOMBIE.maxHealth())
                .add(Attributes.ATTACK_DAMAGE,EldritchMobRules.INHABITED_ZOMBIE.attackDamage())
                .add(Attributes.MOVEMENT_SPEED,EldritchMobRules.INHABITED_ZOMBIE.movementSpeed())
                .add(Attributes.FOLLOW_RANGE,EldritchMobRules.INHABITED_ZOMBIE.followRange())
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0F);
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,
                living->living.getType().getDescriptionId().startsWith("entity.thaumcraft.cultist")));
    }

    /** Original inhabited zombie wears only the cultist plates, never the vanilla zombie equipment. */
    @Override protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random,DifficultyInstance difficulty) {}

    @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
            DifficultyInstance difficulty,MobSpawnType reason,net.minecraft.world.entity.SpawnGroupData data) {
        super.finalizeSpawn(level,difficulty,reason,data);
        float chance=EldritchMobRules.inhabitedPlateChance(level.getDifficulty().getId());
        setItemSlot(EquipmentSlot.HEAD,cultistPlate(EldritchMobRules.CULTIST_PLATE_HELMET_ID));
        if(getRandom().nextFloat()<=chance)setItemSlot(EquipmentSlot.CHEST,cultistPlate(EldritchMobRules.CULTIST_PLATE_CHEST_ID));
        if(getRandom().nextFloat()<=chance)setItemSlot(EquipmentSlot.LEGS,cultistPlate(EldritchMobRules.CULTIST_PLATE_LEGS_ID));
        return data;
    }

    private static ItemStack cultistPlate(String id) {
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));
        return item.map(stack->new ItemStack(stack)).orElse(ItemStack.EMPTY);
    }

    /** Original inhabited zombie never drops loot. */
    @Override protected void dropFromLootTable(DamageSource source,boolean recentlyHit) {}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit) {}

    /** Original inhabited zombie does not report a kill and dies into a helmeted eldritch crab. */
    @Override public boolean killedEntity(ServerLevel level,LivingEntity victim) { return false; }
    @Override public void die(DamageSource source) {
        if(!level().isClientSide) {
            var crab=new EntityEldritchCrab(ThaumcraftEldritchMobs.ELDRITCH_CRAB.get(),level());
            crab.moveTo(getX(),getY()+getEyeHeight(),getZ(),getYRot(),getXRot());
            crab.setHelm(true);
            level().addFreshEntity(crab);
        }
        for(int i=0;i<20;i++) {
            double dx=getRandom().nextGaussian()*.02D,dy=getRandom().nextGaussian()*.02D,dz=getRandom().nextGaussian()*.02D;
            if(level() instanceof ServerLevel server)server.sendParticles(ParticleTypes.EXPLOSION,
                    getX()+(getRandom().nextFloat()*getBbWidth()*2F)-getBbWidth(),getY()+(getRandom().nextFloat()*getBbHeight()),
                    getZ()+(getRandom().nextFloat()*getBbWidth()*2F)-getBbWidth(),1,dx,dy,dz,0D);
        }
        super.die(source);
    }

    /** Original inhabited zombie keeps its distance from other inhabited zombies. */
    @Override public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level,MobSpawnType reason) {
        var box=new net.minecraft.world.phys.AABB(getX(),getY(),getZ(),getX()+1D,getY()+1D,getZ()+1D)
                .inflate(EldritchMobRules.INHABITED_ISOLATION_RADIUS,EldritchMobRules.INHABITED_ISOLATION_HEIGHT,EldritchMobRules.INHABITED_ISOLATION_RADIUS);
        if(level instanceof ServerLevelAccessor server&&!server.getLevel().getEntitiesOfClass(EntityInhabitedZombie.class,box).isEmpty())return false;
        return super.checkSpawnRules(level,reason);
    }

    @Override public boolean isAlliedTo(Entity entity) { return entity instanceof IEldritchMob||super.isAlliedTo(entity); }
    @Override protected SoundEvent getAmbientSound() { return sound("crabtalk"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.GENERIC_HURT; }
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float height) { return OriginalMobLoot.spawn(this,stack,height); }

    static SoundEvent sound(String id) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft",id)); }
}
