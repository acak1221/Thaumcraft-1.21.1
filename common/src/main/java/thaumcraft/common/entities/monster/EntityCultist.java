package thaumcraft.common.entities.monster;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.PoiTypeTags;
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
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.pathfinder.PathType;
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
        applyOriginalNavigator();
    }

    /**
     * Original EntityCultist:37-38 - the navigator of the cultist may pass a closed wooden
     * door and the pathfinder refuses the water.
     *
     * <p>The 1.7.10 navigator had two flags for the doors:
     * {@code setBreakDoors(true)} let the path run through a <em>closed</em> wooden door and
     * {@code setEnterDoors(true)} (the 1.7.10 default) let it run through an open one. The
     * 1.21.1 navigator splits the same two rules: {@code setCanOpenDoors} decides whether a
     * closed wooden door is walkable ({@code PathType.DOOR_WOOD_CLOSED} becomes
     * {@code WALKABLE_DOOR}) and {@code setCanPassDoors} decides whether a door opening is
     * walkable at all.
     *
     * <p>The 1.7.10 {@code setAvoidsWater(true)} made the pathfinder refuse every water cell
     * ({@code PathFinder.getSafePoint} returned {@code null} for a water cell); the 1.21.1
     * equivalent is a mob that cannot float, because the water path type is not walkable for it.
     */
    public void applyOriginalNavigator() {
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanPassDoors(true);
            // The original refuses every water cell, so the cultist does not float and the water
            // path type is not walkable for it.
            ground.setCanFloat(false);
        }
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

    /**
     * Original EntityCultist:100-115 - the cultist keeps its home area in its saved data.
     * The ritualist cleric anchors its altar there, so the anchor has to survive a reload.
     */
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) {
            tag.putInt("HomeD", (int) getRestrictRadius());
            tag.putInt("HomeX", getRestrictCenter().getX());
            tag.putInt("HomeY", getRestrictCenter().getY());
            tag.putInt("HomeZ", getRestrictCenter().getZ());
        }
    }

    /** Original EntityCultist:100-105 - the cultist reads its home area back. */
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeD")) {
            restrictTo(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")), tag.getInt("HomeD"));
        }
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
     * The original 1.7.10 {@code EntityThrowable.setThrowableHeading} also aimed the
     * projectile: it set the yaw from the direction and the pitch from its height.
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
        // Original EntityThrowable.setThrowableHeading: the projectile looks along its motion.
        float horizontal = (float) Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(x, z) * 180.0 / Math.PI);
        float pitch = (float) (Math.atan2(y, horizontal) * 180.0 / Math.PI);
        projectile.setYRot(yaw);
        projectile.yRotO = yaw;
        projectile.setXRot(pitch);
        projectile.xRotO = pitch;
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
     * Original {@code EntityAITarget.getTargetDistance}: the follow range attribute of the mob,
     * or sixteen when the mob has none. The cultist, the cleric, the knight and the praetor
     * all follow to thirty-two blocks, so the call for help of a hurt cultist reaches the
     * cultists of the whole thirty-two block neighbourhood.
     */
    public static double callForHelpRange(Mob mob) {
        var attribute = mob.getAttribute(Attributes.FOLLOW_RANGE);
        return CultistRules.callForHelpRange(attribute == null ? null : attribute.getValue());
    }

    /**
     * Original {@code AICultistHurtByTarget}:30-53 - the cultist answers whoever hit it and
     * calls the other cultists to the same target. A ritualist cleric only wakes up one time
     * in three.
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
            // Original AICultistHurtByTarget:35-40 - the call reaches as far as the original
            // EntityAITarget.getTargetDistance(), which is the follow range attribute of the
            // caller and not a fixed sixteen blocks. The cultist follows to thirty-two.
            double range = callForHelpRange(mob);
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

    /**
     * Original EntityAIRestrictOpenDoor: the goal of priority four of the cleric and of the
     * knight. At night, next to a door of a village, the mob stops breaking and entering
     * doors, and the restriction counter of that door rises.
     *
     * <p>The 1.7.10 goal looked for the nearest village within sixteen blocks and for the
     * nearest door of that village, and it required the mob to stand within 1.5 blocks of the
     * inside of that door. The 1.21.1 world has no {@code VillageDoorInfo} and no village
     * object; the village is the point-of-interest set of the world and the door is the
     * wooden door next to the mob. The restriction counter of the 1.7.10 door has no 1.21.1
     * counterpart: it only made the villagers of that village close their door.
     */
    public static class CultistRestrictOpenDoorGoal extends Goal {
        private final Mob mob;
        private BlockPos door;

        public CultistRestrictOpenDoorGoal(Mob mob) {
            this.mob = mob;
        }

        @Override public boolean canUse() {
            if (mob.level().isDay()) return false;
            if (!(mob.level() instanceof ServerLevel server)) return false;
            // Original EntityAIRestrictOpenDoor:37 - the nearest village within sixteen blocks.
            if (server.getPoiManager().findClosest(holder -> holder.is(PoiTypeTags.VILLAGE),
                    mob.blockPosition(), CultistRules.RESTRICT_DOOR_VILLAGE_RADIUS, PoiManager.Occupancy.ANY).isEmpty()) {
                return false;
            }
            door = findDoor(mob);
            return door != null;
        }

        @Override public boolean canContinueToUse() {
            if (mob.level().isDay()) return false;
            return door != null && mob.distanceToSqr(Vec3.atCenterOf(door)) <= CultistRules.RESTRICT_DOOR_DISTANCE_SQ;
        }

        @Override public void start() {
            // Original EntityAIRestrictOpenDoor:47-48 - the navigator stops breaking and entering doors.
            setDoors(false, false);
        }

        @Override public void stop() {
            // Original EntityAIRestrictOpenDoor:53-54 - the doors of the navigator come back.
            setDoors(true, true);
            door = null;
        }

        @Override public void tick() {
            // Original EntityAIRestrictOpenDoor:58 - the restriction counter of the door rises.
            // The 1.21.1 door has no such counter; the flag of the navigator is the behaviour.
        }

        private void setDoors(boolean canOpen, boolean canPass) {
            if (mob.getNavigation() instanceof GroundPathNavigation ground) {
                ground.setCanOpenDoors(canOpen);
                ground.setCanPassDoors(canPass);
            }
        }

        /** The wooden door the mob stands at; the original used the inside of the village door. */
        private static BlockPos findDoor(Mob mob) {
            BlockPos origin = mob.blockPosition();
            BlockPos found = null;
            double best = CultistRules.RESTRICT_DOOR_DISTANCE_SQ;
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 1, 1))) {
                if (!DoorBlock.isWoodenDoor(mob.level(), pos)) continue;
                double distance = mob.distanceToSqr(Vec3.atCenterOf(pos));
                if (distance > best) continue;
                best = distance;
                found = pos.immutable();
            }
            return found;
        }
    }

    /**
     * Original EntityAITarget.isSuitableTarget: a target has to stand inside the home area of
     * the mob. The native {@code NearestAttackableTargetGoal} scans the whole follow range
     * and never asks for the restriction, so the cultist selector keeps the original rule.
     */
    public static boolean withinHome(Mob mob, LivingEntity target) {
        if (!mob.hasRestriction()) return true;
        BlockPos home = mob.getRestrictCenter();
        if (home == null) return true;
        return CultistRules.withinHomeArea(true, home.distSqr(target.blockPosition()), mob.getRestrictRadius());
    }

    // ---------------------------------------------------------------- energy orb
    /**
     * Original EntityGolemOrb: a gravity free homing orb that deals the owner's attack
     * damage and dies after 240 ticks. The red variant of the cultists uses the full
     * damage, the plain variant three fifths of it.
     */
}
