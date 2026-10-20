package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.config.ThaumcraftWarp;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;
import thaumcraft.common.lib.MagicWard;
import thaumcraft.common.lib.WarpEvents;
import thaumcraft.common.lib.network.WarpEventNetworking;

/** Original guardian's combat, undead/magic ward, proximity mist, home and drops. Natural obelisk/Outer Lands spawns are separate. */
public final class EntityEldritchGuardian extends Monster implements RangedAttackMob, IEldritchMob {
    @FunctionalInterface public interface AmbientEffects { void tick(EntityEldritchGuardian guardian, double x, double y, double z); }
    private static AmbientEffects clientAmbient;
    public static void setClientAmbient(AmbientEffects effects) { clientAmbient = java.util.Objects.requireNonNull(effects); }
    public float armLiftL, armLiftR;
    private boolean lastBlast;
    public EntityEldritchGuardian(EntityType<? extends EntityEldritchGuardian> type, Level level) { super(type, level); xpReward = 20; }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50).add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.MOVEMENT_SPEED, .28).add(Attributes.ATTACK_DAMAGE, 7).add(Attributes.ARMOR, 4);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1, 20, 40, 24) {
            @Override public boolean canUse() { return super.canUse() && getTarget() != null && distanceToSqr(getTarget()) >= 64; }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1, false));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, .8));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // Cultists are added to this predicate when their native entity type is registered; no false vanilla substitute.
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                living -> living.getType().getDescriptionId().startsWith("entity.thaumcraft.cultist")));
    }
    @Override public boolean canAttack(LivingEntity target) { return !(target instanceof IEldritchMob) && super.canAttack(target); }
    @Override public boolean isAlliedTo(Entity entity) { return entity instanceof IEldritchMob; }
    @Override public boolean isInvertedHealAndHarm() { return true; }
    @Override public boolean hurt(DamageSource source, float damage) { return super.hurt(source, source.is(MagicWard.MAGIC_DAMAGE) ? damage / 2 : damage); }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            armLiftL = Math.max(0, armLiftL - .05F); armLiftR = Math.max(0, armLiftR - .05F);
            float x = (float)(getX() + (random.nextFloat() - random.nextFloat()) * .2F);
            float z = (float)(getZ() + (random.nextFloat() - random.nextFloat()) * .2F);
            if (clientAmbient != null) clientAmbient.tick(this, x, (float)(getY() + .22 * getBbHeight()), z);
        }
        else if (!inOuterLands() && tickCount % 100 == 0 && level().getDifficulty() != Difficulty.EASY) {
            double range = level().getDifficulty() == Difficulty.HARD ? 576 : 256;
            for (Player player : level().players()) if (player instanceof ServerPlayer server && player.isAlive() && distanceToSqr(player) < range) WarpEventNetworking.send(server, 2);
        }
    }
    private boolean inOuterLands() { return level().dimension().location().equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "outer_lands")); }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (inOuterLands() && hurtTime <= 0 && tickCount % 25 == 0 && getAbsorptionAmount() < getMaxHealth() / 2) setAbsorptionAmount(getAbsorptionAmount() + 1);
    }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        int difficulty = level().getDifficulty().getId();
        if (hit && getMainHandItem().isEmpty() && isOnFire() && random.nextFloat() < difficulty * .3F) target.igniteForSeconds(2 * difficulty);
        return hit;
    }
    @Override public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (random.nextFloat() > .1F) {
            var orb = new EntityEldritchOrb(level(), this); lastBlast = !lastBlast;
            level().broadcastEntityEvent(this, lastBlast ? (byte) 16 : 15);
            int angle = lastBlast ? 90 : 180;
            double x = Math.cos(Math.toRadians((getYRot() + angle) % 360)) * .5;
            double z = Math.sin(Math.toRadians((getYRot() + angle) % 360)) * .5;
            orb.setPos(orb.getX() - x, orb.getY() - .057777777 * getBbHeight(), orb.getZ() - z);
            orb.shoot(target.getX() + target.getDeltaMovement().x - getX(), target.getY() - getY() - target.getBbHeight() / 2,
                    target.getZ() + target.getDeltaMovement().z - getZ(), 1, 2);
            playSound(sound("egattack"), 2, 1 + random.nextFloat() * .1F); level().addFreshEntity(orb);
        } else if (hasLineOfSight(target)) {
            WarpEventNetworking.sonic(this);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400));
            if (target instanceof ServerPlayer player) WarpEvents.addTemporaryWarp(player, 1 + level().random.nextInt(3));
            playSound(sound("egscreech"), 3, 1 + random.nextFloat() * .1F);
        }
    }
    public static SoundEvent sound(String id) { return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", id)); }
    @Override protected SoundEvent getAmbientSound() { return sound("egidle"); }
    @Override protected SoundEvent getDeathSound() { return sound("egdeath"); }
    @Override public int getAmbientSoundInterval() { return 500; }
    @Override protected float getSoundVolume() { return 1.5F; }
    @Override public void handleEntityEvent(byte event) {
        if (event == 15) armLiftL = .5F;
        else if (event == 16) armLiftR = .5F;
        else if (event == 17) { armLiftL = .9F; armLiftR = .9F; }
        else super.handleEntityEvent(event);
    }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean player) {
        for (Aspect aspect : new Aspect[]{Aspect.UNDEAD, Aspect.ELDRITCH}) if (random.nextBoolean()) {
            var stack = new ItemStack(ThaumcraftEssences.WISP_ESSENCE.get());
            ThaumcraftEssences.WISP_ESSENCE.get().setAspects(stack, new AspectList().add(aspect, 2)); spawnAtLocation(stack);
        }
        if (player) {
            int looting = source.getEntity() instanceof LivingEntity killer
                    ? EnchantmentHelper.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), killer) : 0;
            if (random.nextFloat() < .025F + looting * .01F) spawnAtLocation(ThaumcraftWarp.ELDRITCH_EYE.get());
        }
        super.dropCustomDeathLoot(level, source, player);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) { tag.putLong("WarpHome", getRestrictCenter().asLong()); tag.putFloat("WarpHomeRange", getRestrictRadius()); }
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("WarpHome")) restrictTo(BlockPos.of(tag.getLong("WarpHome")), (int) tag.getFloat("WarpHomeRange"));
    }
}
