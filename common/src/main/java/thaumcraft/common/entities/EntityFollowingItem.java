package thaumcraft.common.entities;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftProjectiles;
import thaumcraft.common.lib.entities.ProjectileRules;

/**
 * TC4 EntityFollowingItem, the item that flies to a target.
 * <p>
 * The original is an {@code EntitySpecialItem} that keeps the special item
 * buoyancy, follows an entity or a fixed point with a divisor of twenty that
 * falls to one (so the item accelerates while it travels), stops inside half
 * a block, and falls with its own gravity of 0.04 once it has no target.
 * <p>
 * Declared native adaptations: the native special item is final, so the
 * buoyancy, the fire immunity and the 0.25 block size are repeated here; the
 * original stored the follow divisor in the item age field, which the native
 * item entity keeps private, so the divisor is a field of this class; the
 * client sparkle is a hook instead of a proxy call.
 */
public final class EntityFollowingItem extends ItemEntity {
    /** The original sent the target of the item to the client with its spawn data. */
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(EntityFollowingItem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPARKLE = SynchedEntityData.defineId(EntityFollowingItem.class, EntityDataSerializers.INT);
    private static Consumer<EntityFollowingItem> clientSparkle = ignored -> {};
    private double targetX;
    private double targetY;
    private double targetZ;
    private Entity target;
    private int slowdown = ProjectileRules.FOLLOW_ITEM_SLOWDOWN;
    private boolean clipDisabled;
    public double gravity = ProjectileRules.FOLLOW_ITEM_GRAVITY;

    public EntityFollowingItem(EntityType<? extends EntityFollowingItem> type, Level level) {
        super(type, level);
    }

    public EntityFollowingItem(Level level) {
        this(ThaumcraftProjectiles.FOLLOW_ITEM.get(), level);
    }

    public EntityFollowingItem(Level level, double x, double y, double z, ItemStack stack) {
        this(level);
        setItem(stack);
        setPos(x, y, z);
        setYRot((float) (Math.random() * 360D));
    }

    public EntityFollowingItem(Level level, double x, double y, double z, ItemStack stack, Entity target, int type) {
        this(level, x, y, z, stack);
        setTarget(target);
        updateTargetPosition();
        setSparkleType(type);
        clipDisabled = true;
    }

    public EntityFollowingItem(Level level, double x, double y, double z, ItemStack stack, double tx, double ty, double tz) {
        this(level, x, y, z, stack);
        this.targetX = tx;
        this.targetY = ty;
        this.targetZ = tz;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET, 0);
        builder.define(SPARKLE, 3);
    }

    public Entity getTarget() {
        int id = entityData.get(TARGET);
        if (id <= 0) return null;
        return level().getEntity(id);
    }
    public void setTarget(Entity target) {
        this.target = target;
        entityData.set(TARGET, target == null ? 0 : target.getId());
    }
    public int getSparkleType() { return entityData.get(SPARKLE); }
    public void setSparkleType(int type) { entityData.set(SPARKLE, type); }
    public int getSlowdown() { return slowdown; }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTargetZ() { return targetZ; }

    private void updateTargetPosition() {
        if (target == null) return;
        targetX = target.getX();
        targetY = ProjectileRules.followItemTargetY(target.getY(), target.getBbHeight());
        targetZ = target.getZ();
    }

    @Override public void tick() {
        if (isRemoved()) return;
        Entity current = getTarget();
        if (current != null) {
            target = current;
            updateTargetPosition();
        } else {
            target = null;
        }
        if (targetX != 0D || targetY != 0D || targetZ != 0D) {
            float xd = (float) (targetX - getX());
            float yd = (float) (targetY - getY());
            float zd = (float) (targetZ - getZ());
            slowdown = ProjectileRules.followItemSlowdown(slowdown);
            double distance = Math.sqrt(xd * xd + yd * yd + zd * zd);
            if (!ProjectileRules.followItemArrived(distance)) {
                // The original divides the delta by the distance and the divisor.
                double divisor = distance * slowdown;
                setDeltaMovement(xd / divisor, yd / divisor, zd / divisor);
            } else {
                setDeltaMovement(getDeltaMovement().scale(.1));
                targetX = 0D;
                targetY = 0D;
                targetZ = 0D;
                target = null;
                clipDisabled = false;
            }
            if (level().isClientSide) clientSparkle.accept(this);
        } else {
            setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y - gravity, getDeltaMovement().z);
        }
        // Original EntitySpecialItem buoyancy: damp the rise and cancel the item gravity.
        if (getDeltaMovement().y > 0) setDeltaMovement(getDeltaMovement().multiply(1, 0.9F, 1));
        super.tick();
    }

    @Override protected void applyGravity() {}

    /** The original item does not collide while it follows its target. */
    @Override public void move(net.minecraft.world.entity.MoverType moverType, net.minecraft.world.phys.Vec3 motion) {
        if (clipDisabled) {
            setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
            return;
        }
        super.move(moverType, motion);
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        return !source.is(DamageTypeTags.IS_FIRE) && super.hurt(source, amount);
    }

    public static void setClientSparkle(Consumer<EntityFollowingItem> effect) {
        clientSparkle = Objects.requireNonNull(effect);
    }
}
