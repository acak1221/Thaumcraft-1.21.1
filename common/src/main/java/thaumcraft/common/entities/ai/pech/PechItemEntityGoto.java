package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIPechItemEntityGoto} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/pech/AIPechItemEntityGoto.java}).
 *
 * <p>The pech walks to the dropped item that lies closest to it, but only every fifth tick and only for an item it may
 * pick up. It takes the whole stack when it reaches the item, and it retries the path with a growing penalty when the
 * path does not lead to the item.</p>
 *
 * <p>The original registered this goal with priority 3.</p>
 */
public final class PechItemEntityGoto extends Goal {
    /** The original 1.7 goal priority of {@code AIPechItemEntityGoto}. */
    public static final int PRIORITY = 3;

    private final EntityPech pech;
    private ItemEntity target;
    private int count;
    private int failedPathFindingPenalty;

    public PechItemEntityGoto(EntityPech pech) {
        this.pech = pech;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private double speed() { return pech.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.5; }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (!GolemAiRules.searchTick(pech.tickCount)) return false;
        if (--count > 0) return false;
        double nearest = Double.MAX_VALUE;
        ItemEntity chosen = null;
        for (Entity entity : pech.level().getEntities(pech, pech.getBoundingBox().inflate(GolemAiRules.PECH_ITEM_RANGE))) {
            if (!(entity instanceof ItemEntity item) || !pech.canPickup(item.getItem())) continue;
            double distance = item.distanceToSqr(pech);
            if (!(distance < nearest) || !GolemAiRules.withinRange(distance, (float) GolemAiRules.PECH_ITEM_RANGE)) continue;
            nearest = distance;
            chosen = item;
        }
        target = chosen;
        return chosen != null;
    }

    @Override public boolean canContinueToUse() {
        return target != null && target.isAlive() && !pech.getNavigation().isDone()
                && target.distanceToSqr(pech) < GolemAiRules.PECH_ITEM_RANGE_SQ;
    }

    @Override public void start() {
        Path path = pech.getNavigation().createPath(target, 0);
        pech.getNavigation().moveTo(path, speed());
        count = 0;
    }

    @Override public void stop() { target = null; }

    @Override public void tick() {
        if (target == null) return;
        pech.getLookControl().setLookAt(target, 30, 30);
        if (pech.getSensing().hasLineOfSight(target) && --count <= 0) {
            count = failedPathFindingPenalty + GolemAiRules.PECH_RETRY_BASE + pech.getRandom().nextInt(GolemAiRules.PECH_RETRY_SPAN);
            pech.getNavigation().moveTo(target, speed());
            Path current = pech.getNavigation().getPath();
            var end = current == null ? null : current.getEndNode();
            failedPathFindingPenalty = end != null && target.distanceToSqr(end.x, end.y, end.z) < 1.0 ? 0
                    : failedPathFindingPenalty + GolemAiRules.PECH_FAILED_PATH_PENALTY;
        }
        if (pech.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()) > 1.5) return;
        count = 0;
        int before = target.getItem().getCount();
        ItemStack remaining = pech.pickupItem(target.getItem());
        if (remaining == null || remaining.isEmpty()) target.discard();
        else target.setItem(remaining);
        if (remaining == null || remaining.getCount() != before) {
            pech.level().playSound(null, target.getX(), target.getY(), target.getZ(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                    target.getSoundSource(), 0.2F,
                    ((pech.level().random.nextFloat() - pech.level().random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
        }
    }
}
