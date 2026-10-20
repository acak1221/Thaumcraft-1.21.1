package thaumcraft.common.entities.monster;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.cult.CultistRules;

/**
 * Original TC4 crimson cleric: thirty health, a homing energy orb or a volley of three
 * small fireballs, and the ritualist that kneels at the eldritch altar until it is hit.
 */
public class EntityCultistCleric extends EntityCultist implements RangedAttackMob {
    private static final EntityDataAccessor<Boolean> RITUALIST = SynchedEntityData.defineId(EntityCultistCleric.class, EntityDataSerializers.BOOLEAN);

    public EntityCultistCleric(EntityType<? extends EntityCultistCleric> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCultist.createAttributes().add(Attributes.MAX_HEALTH, CultistRules.CLERIC_HEALTH);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new CultistSwimGoal(this));
        goalSelector.addGoal(1, new CultistAltarFocusGoal(this));
        goalSelector.addGoal(2, new CultistLongRangeGoal(this, 2.0, 1.0, 20, 40, 24.0F));
        goalSelector.addGoal(3, new CultistMeleeGoal(this, 1.0, false));
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new CultistHurtByTargetGoal(this, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RITUALIST, false);
    }

    public boolean getIsRitualist() { return entityData.get(RITUALIST); }

    public void setIsRitualist(boolean value) { entityData.set(RITUALIST, value); }

    @Override public Component getName() {
        return hasCustomName() ? getCustomName() : Component.translatable("entity.thaumcraft.cultistcleric.name");
    }

    /** Original EntityCultistCleric.addRandomArmor: the crimson robe and, rarely, the cultist boots. */
    @Override protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.HEAD, stack(CultistRules.ITEM_ROBE_HOOD));
        setItemSlot(EquipmentSlot.CHEST, stack(CultistRules.ITEM_ROBE_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, stack(CultistRules.ITEM_ROBE_LEGGINGS));
        if (random.nextFloat() < CultistRules.clericBootsChance(level().getDifficulty() == Difficulty.HARD)) {
            setItemSlot(EquipmentSlot.FEET, stack(CultistRules.ITEM_BOOTS));
        }
    }

    /** Original EntityCultistCleric.attackEntityWithRangedAttack: an orb or three small fireballs. */
    @Override public void performRangedAttack(LivingEntity target, float distance) {
        double dx = target.getX() - getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 2.0 - (getY() + getBbHeight() / 2.0);
        double dz = target.getZ() - getZ();
        swing(InteractionHand.MAIN_HAND);
        if (CultistRules.clericFiresOrb(random.nextFloat())) {
            GolemOrb blast = new GolemOrb(level(), this, target, true);
            originalShoot(blast, dx, dy + 2.0, dz, 0.66F, 3.0F);
            playSound(sound("egattack"), 1.0F, 1.0F + random.nextFloat() * 0.1F);
            level().addFreshEntity(blast);
        } else {
            float spread = CultistRules.fireballSpread(distance);
            playSound(SoundEvents.GHAST_SHOOT, 1.0F, 1.0F);
            for (int i = 0; i < CultistRules.clericFireballCount(); i++) {
                SmallFireball fireball = new SmallFireball(level(), this, new net.minecraft.world.phys.Vec3(
                        dx + random.nextGaussian() * spread, dy, dz + random.nextGaussian() * spread));
                fireball.setPos(getX(), getY() + getBbHeight() / 2.0 + CultistRules.fireballSpawnOffset(), getZ());
                level().addFreshEntity(fireball);
            }
        }
    }

    /** Original EntityCultistCleric.canDespawn is false while the cleric is a ritualist. */
    @Override public boolean removeWhenFarAway(double distance) { return !getIsRitualist(); }

    /** Original EntityCultistCleric.attackEntityFrom breaks the ritual. */
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (isInvulnerable()) return false;
        setIsRitualist(false);
        return super.hurt(source, amount);
    }

    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Ritualist", getIsRitualist());
    }

    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setIsRitualist(tag.getBoolean("Ritualist"));
    }

    /** Original EntityCultistCleric.onUpdate turns the ritualist towards its altar. */
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide || !getIsRitualist()) return;
        BlockPos home = getRestrictCenter();
        if (home == null) return;
        double dx = home.getX() + 0.5 - getX();
        double dy = home.getY() + 1.5 - (getY() + getEyeHeight());
        double dz = home.getZ() + 0.5 - getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, horizontal) * 180.0 / Math.PI));
        setXRot(updateRotation(getXRot(), pitch, 10.0F));
        // Original EntityCultistCleric turns the render yaw of the ritualist, not the entity yaw.
        yBodyRot = updateRotation(yBodyRot, yaw, 30.0F);
    }

    /** Original EntityCultistCleric.updateRotation clamps the turn to the given speed. */
    private float updateRotation(float current, float target, float maxDelta) {
        float delta = net.minecraft.util.Mth.wrapDegrees(target - current);
        if (delta > maxDelta) delta = maxDelta;
        if (delta < -maxDelta) delta = -maxDelta;
        return current + delta;
    }

    @Override protected SoundEvent getAmbientSound() { return sound("chant"); }

    @Override public int getAmbientSoundInterval() { return 500; }

    /**
     * Original AIAltarFocus: the ritualist keeps kneeling at the eldritch altar as long as
     * it stays close to it. The altar block itself belongs to the eldritch block unit.
     */
    public static class CultistAltarFocusGoal extends Goal {
        private final EntityCultistCleric cleric;

        public CultistAltarFocusGoal(EntityCultistCleric cleric) {
            this.cleric = cleric;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override public boolean canUse() { return cleric.getIsRitualist() && cleric.getRestrictCenter() != null; }

        @Override public boolean canContinueToUse() { return cleric.getIsRitualist() && cleric.getRestrictCenter() != null; }

        @Override public void tick() {
            BlockPos home = cleric.getRestrictCenter();
            if (home == null || cleric.tickCount % 40 != 0) return;
            double distance = home.distToCenterSqr(cleric.getX(), cleric.getY(), cleric.getZ());
            if (CultistRules.ritualistShouldStop(distance, isEldritchAltar(cleric.level(), home))) cleric.setIsRitualist(false);
        }

        /** The eldritch altar belongs to another unit; the ritualist simply stops without it. */
        public static boolean isEldritchAltar(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            for (String id : CultistRules.BLOCK_ELDRITCH_ALTAR_IDS) {
                if (BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, id))
                        .map(block -> state.is(block)).orElse(false)) return true;
            }
            return false;
        }
    }
}
