package thaumcraft.common.entities.projectile;

import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftResources;

/** Native throwable, owner/item tracking and persistence; TC4 blast semantics. */
public final class EntityAlumentum extends ThrowableItemProjectile {
    public static final float LAUNCH_SPEED = .75F;
    public static final float EXPLOSION_RADIUS = 1.66F;
    private static Consumer<EntityAlumentum> clientEffects;

    public EntityAlumentum(EntityType<? extends EntityAlumentum> type, Level level) { super(type, level); }
    public EntityAlumentum(Level level, LivingEntity owner) {
        super(ThaumcraftResources.ALUMENTUM.get(), owner, level);
        float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
        float pitch = owner.getXRot() * Mth.DEG_TO_RAD;
        setPos(owner.getX() - Mth.cos(yaw) * .16, owner.getEyeY() - .1, owner.getZ() - Mth.sin(yaw) * .16);
        // shootFromRotation would add the shooter's movement, unlike the
        // original EntityThrowable constructor used by TC4.
        shoot(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch), LAUNCH_SPEED, 1);
    }
    public EntityAlumentum(Level level, double x, double y, double z) { super(ThaumcraftResources.ALUMENTUM.get(), x, y, z, level); }
    @Override protected Item getDefaultItem() { return ThaumcraftContent.MATERIALS.get("alumentum").get(); }
    @Override protected double getDefaultGravity() { return .03; }

    public static void setClientEffects(Consumer<EntityAlumentum> value) { clientEffects = value; }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide && !isRemoved() && clientEffects != null) clientEffects.accept(this);
        // TC4 sets no custom time-to-live. Keep native impact/void/unload
        // behavior instead of inventing a short fuse or a refunded item.
    }

    @Override protected void onHit(HitResult hit) {
        if (level().isClientSide || isRemoved() || hit.getType() == HitResult.Type.MISS) return;
        // TC4 EntityAlumentum:50-55 passes a NULL exploder, not its thrower.
        // Preserve unattributed explosion damage (including on entity hits),
        // no additional direct-hit damage, no fire, and mobGriefing-gated
        // terrain damage even when a player threw the projectile.
        level().explode(null, getX(), getY(), getZ(), EXPLOSION_RADIUS, false, Level.ExplosionInteraction.MOB);
        discard();
    }
}
