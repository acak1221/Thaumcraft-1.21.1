package thaumcraft.common.entities.monster;

import java.util.EnumSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftCultists;
import thaumcraft.common.config.ThaumcraftEldritchItems;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.config.ThaumcraftLegacyItems;
import thaumcraft.common.config.ThaumcraftRunicBaubles;
import thaumcraft.common.lib.cult.CultistEquipmentRules;
import thaumcraft.common.lib.cult.CultistRules;
import thaumcraft.common.lib.loot.OriginalMobLoot;

/**
 * Original TC4 cultist: twenty health, four damage, 0.3 speed, a thirty-two block
 * follow range and ten experience. The crimson knight, the crimson cleric and the
 * praetor share the faction rules of {@link CultistRules}; the melee, ranged and revenge
 * goals and the homing energy orb live here because every cultist kind uses them.
 */
public class EntityCultist extends Monster {
    public EntityCultist(EntityType<? extends EntityCultist> type, Level level) {
        super(type, level);
        xpReward = CultistRules.CULTIST_EXPERIENCE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, CultistRules.CULTIST_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, CultistRules.CULTIST_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, CultistRules.CULTIST_SPEED)
                .add(Attributes.FOLLOW_RANGE, CultistRules.CULTIST_FOLLOW_RANGE);
    }

    /** The original base cultist carries no goal of its own; the three kinds install their own lists. */
    @Override protected void registerGoals() {}

    /** Original EntityCultist.canPickUpLoot is false. */
    @Override public boolean canPickUpLoot() { return false; }

    /** Original EntityCultist.isOnSameTeam: cultists never fight their own faction. */
    @Override public boolean isAlliedTo(Entity other) {
        return other instanceof EntityCultist || other instanceof EntityCultistLeader || super.isAlliedTo(other);
    }

    /** Original EntityCultist.canAttackClass excludes the three cultist kinds. */
    public boolean canAttackClass(Class<? extends Entity> type) {
        return type != EntityCultistCleric.class && type != EntityCultistLeader.class && type != EntityCultistKnight.class;
    }

    // ---------------------------------------------------------------- gear helpers
    /**
     * The registered cultist armour piece of the original equipment table. An empty
     * slot of the original table stays empty.
     */
    public static ItemStack cultistPiece(CultistEquipmentRules.Piece piece) {
        if (piece == null) return ItemStack.EMPTY;
        return ThaumcraftRunicBaubles.cultistPiece(piece.id());
    }

    /** The registered blade of the original equipment table. */
    public static ItemStack cultistWeapon(CultistEquipmentRules.Weapon weapon) {
        return switch (weapon) {
            case NONE -> ItemStack.EMPTY;
            case IRON_SWORD -> new ItemStack(Items.IRON_SWORD);
            case VOID_SWORD -> new ItemStack(ThaumcraftEquipment.VOID_SWORD.get());
            case THAUMIUM_SWORD -> new ItemStack(ThaumcraftEquipment.THAUMIUM_SWORD.get());
            case CRIMSON_SWORD -> new ItemStack(ThaumcraftLegacyItems.CRIMSON_SWORD.get());
        };
    }

    /** Original EntityCultist.dropFewItems: one nextInt(10) roll picks the resource. */
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        int looting = OriginalMobLoot.looting(level, source);
        int meta = CultistRules.resourceDrop(random.nextInt(10), looting);
        if (meta != CultistRules.RESOURCE_NONE) {
            Item drop = switch (meta) {
                case CultistRules.RESOURCE_KNOWLEDGE_FRAGMENT -> ThaumcraftContent.MATERIALS.get("knowledge_fragment").get();
                case CultistRules.RESOURCE_VOID_SEED -> ThaumcraftContent.MATERIALS.get("void_seed").get();
                default -> ThaumcraftContent.MATERIALS.get("gold_coin").get();
            };
            spawnAtLocation(new ItemStack(drop), 1.5F);
        }
        super.dropCustomDeathLoot(level, source, recentlyHit);
        // Original EntityCultist.dropRareDrop hands out the crimson rites as rare cultist loot.
        if (recentlyHit && OriginalMobLoot.rare(looting, random::nextInt)) {
            spawnAtLocation(new ItemStack(ThaumcraftEldritchItems.CRIMSON_RITES.get()), 1.0F);
        }
    }

    /** Original EntityCultist.getDropItem is air, so the entity never drops a plain mob item. */
    @Override protected void dropFromLootTable(DamageSource source, boolean recentlyHit) {}

    /** Original 1.7 item motion used Math.random, not the world or entity random. */
    @Override public net.minecraft.world.entity.item.ItemEntity spawnAtLocation(ItemStack stack, float height) {
        return OriginalMobLoot.spawn(this, stack, height);
    }

    /**
     * Original EntityCultist.onSpawnWithEgg: the equipment and the enchantment of a freshly
     * spawned cultist. The cultist portal spawns its minions itself and calls this.
     */
    public void originalSpawnEquipment(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty) {
        populateDefaultEquipmentSlots(random, difficulty);
        populateDefaultEquipmentEnchantments(level, random, difficulty);
    }

    protected static SoundEvent sound(String id) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, id));
    }

    /**
     * Original Entity.shoot: normalise the direction, spread it by the inaccuracy and scale
     * it by the velocity. The native projectile helper swapped the last two arguments.
     */
    public static void originalShoot(net.minecraft.world.entity.projectile.Projectile projectile,
            double x, double y, double z, float velocity, float inaccuracy) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        x /= length;
        y /= length;
        z /= length;
        x += (projectile.getRandom().nextDouble() - 0.5) * inaccuracy * 2.0;
        y += (projectile.getRandom().nextDouble() - 0.5) * inaccuracy * 2.0;
        z += (projectile.getRandom().nextDouble() - 0.5) * inaccuracy * 2.0;
        projectile.setDeltaMovement(x * velocity, y * velocity, z * velocity);
    }

    // ---------------------------------------------------------------- shared goals
    /**
     * Original AIAttackOnCollide: the cultist paths to its target, keeps looking at it and
     * swings every ten ticks once the target is inside the original reach formula.
     */
    public static class CultistMeleeGoal extends Goal {
        private final Mob mob;
        private final double speed;
        private final boolean longMemory;
        private int attackTick;
        private int pathFindTimer;

        public CultistMeleeGoal(Mob mob, double speed, boolean longMemory) {
            this.mob = mob;
            this.speed = speed;
            this.longMemory = longMemory;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (--pathFindTimer <= 0) {
                pathFindTimer = 4 + mob.getRandom().nextInt(7);
                return mob.getNavigation().createPath(target, 1) != null;
            }
            return true;
        }

        @Override public boolean canContinueToUse() {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (!longMemory) return !mob.getNavigation().isDone();
            return mob.distanceToSqr(target) < 4096.0;
        }

        @Override public void stop() { mob.getNavigation().stop(); }

        @Override public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null) return;
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = mob.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            // Original reach: width * 2 * width * 2 + the target width.
            double reach = mob.getBbWidth() * 2.0F * mob.getBbWidth() * 2.0F + target.getBbWidth();
            if (--pathFindTimer <= 0) {
                pathFindTimer = 4 + mob.getRandom().nextInt(7);
                if (longMemory || mob.hasLineOfSight(target)) {
                    double distanceToTarget = mob.distanceToSqr(target);
                    if (!mob.getNavigation().moveTo(target, speed)) pathFindTimer += 15;
                    else if (distanceToTarget > 1024.0) pathFindTimer += 10;
                    else if (distanceToTarget > 256.0) pathFindTimer += 5;
                }
            }
            if (attackTick > 0) attackTick--;
            if (distance <= reach && attackTick <= 0) {
                attackTick = 10;
                if (!mob.getMainHandItem().isEmpty()) mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(target);
            }
        }
    }

    /**
     * Original AILongRangeAttack: the cultist keeps its distance, fires from the maximum
     * range and never shoots a target that is closer than the original minimum distance.
     */
    public static class CultistLongRangeGoal extends Goal {
        private final Mob mob;
        private final double minDistance;
        private final double moveSpeed;
        private final int minAttackTime;
        private final int maxAttackTime;
        private final float maxRange;
        private final float maxRangeSquared;
        private LivingEntity target;
        private int attackTime = -1;
        private int seeTime;

        public CultistLongRangeGoal(Mob mob, double minDistance, double moveSpeed, int minAttackTime, int maxAttackTime, float maxRange) {
            this.mob = mob;
            this.minDistance = minDistance;
            this.moveSpeed = moveSpeed;
            this.minAttackTime = minAttackTime;
            this.maxAttackTime = maxAttackTime;
            this.maxRange = maxRange;
            this.maxRangeSquared = maxRange * maxRange;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override public boolean canUse() {
            LivingEntity current = mob.getTarget();
            if (current == null) return false;
            target = current;
            if (!current.isAlive()) {
                mob.setTarget(null);
                return false;
            }
            double distance = mob.distanceToSqr(current.getX(), current.getBoundingBox().minY, current.getZ());
            if (distance < minDistance * minDistance) return false;
            return distance <= maxRangeSquared && mob.hasLineOfSight(current);
        }

        @Override public boolean canContinueToUse() { return canUse() || !mob.getNavigation().isDone(); }

        @Override public void stop() {
            target = null;
            seeTime = 0;
            attackTime = -1;
        }

        @Override public void tick() {
            if (target == null) return;
            double distance = mob.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
            boolean sees = mob.hasLineOfSight(target);
            if (sees) seeTime++;
            else seeTime = 0;
            if (distance <= maxRangeSquared && seeTime >= 20) mob.getNavigation().stop();
            else mob.getNavigation().moveTo(target, moveSpeed);
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--attackTime == 0) {
                if (distance > maxRangeSquared || !sees) return;
                float ratio = Mth.clamp((float) (Math.sqrt(distance) / maxRange), 0.1F, 1.0F);
                if (mob instanceof RangedAttackMob ranged) ranged.performRangedAttack(target, ratio);
                attackTime = Mth.floor(ratio * (maxAttackTime - minAttackTime) + minAttackTime);
            } else if (attackTime < 0) {
                float ratio = Mth.clamp((float) (Math.sqrt(distance) / maxRange), 0.1F, 1.0F);
                attackTime = Mth.floor(ratio * (maxAttackTime - minAttackTime) + minAttackTime);
            }
        }
    }

    /**
     * Original AICultistHurtByTarget: the cultist answers whoever hit it and calls the
     * other cultists to the same target. A ritualist cleric only wakes up one time in three.
     */
    public static class CultistHurtByTargetGoal extends Goal {
        private final Mob mob;
        private final boolean callsForHelp;
        private int lastRevengeTimer;

        public CultistHurtByTargetGoal(Mob mob, boolean callsForHelp) {
            this.mob = mob;
            this.callsForHelp = callsForHelp;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override public boolean canUse() {
            LivingEntity revenge = mob.getLastHurtByMob();
            if (revenge == null || revenge == mob) return false;
            int timer = mob.getLastHurtByMobTimestamp();
            return timer != lastRevengeTimer && mob.canAttackType(revenge.getType());
        }

        @Override public void start() {
            LivingEntity revenge = mob.getLastHurtByMob();
            if (revenge == null) return;
            mob.setTarget(revenge);
            lastRevengeTimer = mob.getLastHurtByMobTimestamp();
            if (callsForHelp) callForHelp(revenge);
        }

        private void callForHelp(LivingEntity revenge) {
            double range = 16.0;
            AABB box = new AABB(mob.getX(), mob.getY(), mob.getZ(), mob.getX() + 1.0, mob.getY() + 1.0, mob.getZ() + 1.0)
                    .inflate(range, 10.0, range);
            for (EntityCultist ally : mob.level().getEntitiesOfClass(EntityCultist.class, box, ally -> ally != mob)) {
                if (ally.getTarget() != null || ally.isAlliedTo(revenge)) continue;
                if (ally instanceof EntityCultistCleric cleric && cleric.getIsRitualist()) {
                    if (mob.level().getRandom().nextInt(3) != 0) continue;
                    cleric.setIsRitualist(false);
                    ally.setTarget(revenge);
                    continue;
                }
                ally.setTarget(revenge);
            }
        }
    }

    /**
     * Original EntityAISwimming: the cultist swims up while its head is under water.
     * Vanilla 1.21.1 dropped the swim goal, so the original behaviour is kept here.
     */
    public static class CultistSwimGoal extends Goal {
        private final Mob mob;

        public CultistSwimGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override public boolean canUse() { return mob.isInWater(); }

        @Override public boolean canContinueToUse() { return mob.isInWater(); }

        @Override public void tick() {
            if (!mob.isInWater()) return;
            double height = mob.getBbHeight();
            double eye = mob.getEyeY();
            double surface = mob.getY() + height;
            if (height > 0.0F && eye < surface) {
                mob.setSwimming(true);
                mob.setDeltaMovement(mob.getDeltaMovement().add(0.0, (surface - eye) * 0.2, 0.0));
            }
        }
    }

    // ---------------------------------------------------------------- energy orb
    /**
     * Original EntityGolemOrb: a gravity free homing orb that deals the owner's attack
     * damage and dies after 240 ticks. The red variant of the cultists uses the full
     * damage, the plain variant three fifths of it.
     */
}
