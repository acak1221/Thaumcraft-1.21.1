package thaumcraft.common.mixin;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.world.StreamAxeGeometry;
import thaumcraft.common.lib.world.StreamAxeItemMotion;

/**
 * Per-item transient follow target for chopped drops (Axe of the Stream). The
 * item walks a straight, collision-free path to the player exactly as the original
 * EntityFollowingItem did. Vanilla tick still runs fully, so age, pickup delay,
 * merging and despawn are untouched; only the final position and velocity are
 * overwritten afterwards. Nothing is saved: an unloaded or restarted item is plain.
 * Server only; the entity tracker synchronises the result.
 */
@Mixin(ItemEntity.class)
public abstract class StreamAxeItemMixin implements StreamAxeItemMotion.Following {
    @Unique private UUID thaumcraft$streamTarget;
    @Unique private int thaumcraft$streamCounter = StreamAxeGeometry.FOLLOW_START;
    @Unique private Vec3 thaumcraft$streamFrom;
    @Unique private Vec3 thaumcraft$streamStep;

    @Override public boolean thaumcraft$streamFollowing() { return thaumcraft$streamTarget != null; }

    @Override public boolean thaumcraft$streamFollow(UUID target) {
        if (thaumcraft$streamTarget != null) return false;
        thaumcraft$streamTarget = target;
        thaumcraft$streamCounter = StreamAxeGeometry.FOLLOW_START;
        return true;
    }

    @Unique private void thaumcraft$release(ItemEntity self) {
        thaumcraft$streamTarget = null;
        thaumcraft$streamFrom = null;
        thaumcraft$streamStep = null;
        self.setNoGravity(false);
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void thaumcraft$streamBeforeTick(CallbackInfo callback) {
        if (thaumcraft$streamTarget == null) return;
        ItemEntity self = (ItemEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || self.isRemoved()) return;
        // Invalid, dead, disconnected or cross-world targets are released in place:
        // never teleported to, never given the item.
        Player target = level.getPlayerByUUID(thaumcraft$streamTarget);
        if (target == null || !target.isAlive() || target.isRemoved() || target.level() != level) {
            thaumcraft$release(self);
            return;
        }
        var step = StreamAxeGeometry.follow(self.getX(), self.getY(), self.getZ(),
                target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                thaumcraft$streamCounter, self.getDeltaMovement().x, self.getDeltaMovement().y, self.getDeltaMovement().z);
        thaumcraft$streamCounter = step.counter();
        if (step.arrived()) {
            self.setDeltaMovement(step.vx(), step.vy(), step.vz());
            thaumcraft$release(self);
            return;
        }
        self.setNoGravity(true);
        thaumcraft$streamFrom = self.position();
        thaumcraft$streamStep = new Vec3(step.vx(), step.vy(), step.vz());
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void thaumcraft$streamAfterTick(CallbackInfo callback) {
        if (thaumcraft$streamStep == null) return;
        ItemEntity self = (ItemEntity) (Object) this;
        Vec3 from = thaumcraft$streamFrom, step = thaumcraft$streamStep;
        thaumcraft$streamFrom = null;
        thaumcraft$streamStep = null;
        if (from == null || self.isRemoved()) return;
        // Overrides vanilla collision for this tick: the original flew with noClip.
        self.setPos(from.add(step));
        self.setDeltaMovement(step);
        self.hasImpulse = true;
    }
}
