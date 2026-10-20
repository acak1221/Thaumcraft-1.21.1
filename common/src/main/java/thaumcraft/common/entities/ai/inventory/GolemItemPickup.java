package thaumcraft.common.entities.ai.inventory;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.entities.golems.EntityGolemBase;
import thaumcraft.common.lib.golems.GolemAiRules;

/**
 * Original {@code AIItemPickup} (TC4 4.2.3.5, {@code thaumcraft/common/entities/ai/inventory/AIItemPickup.java}).
 *
 * <p>The golem picks up the item that lies closest to its home: the item has to be at least five ticks old, the golem has to
 * have room for it, and it has to be the very item the golem already carries. The item is taken when the golem is within two
 * blocks of it.</p>
 *
 * <p>The original registered this goal with priority 2.</p>
 */
public final class GolemItemPickup extends Goal {
    /** The original 1.7 goal priority of {@code AIItemPickup}. */
    public static final int PRIORITY = 2;

    private final EntityGolemBase golem;
    private final GolemAiRules.Cargo cargo;
    private ItemEntity target;
    private int count;

    public GolemItemPickup(EntityGolemBase golem) {
        this(golem, GolemAiRules.cargoOf(golem));
    }

    public GolemItemPickup(EntityGolemBase golem, GolemAiRules.Cargo cargo) {
        this.golem = golem;
        this.cargo = cargo;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public boolean canUse() {
        if (!GolemAiRules.searchTick(golem.tickCount)) return false;
        return findItem();
    }

    @Override public boolean canContinueToUse() {
        return target != null && count-- > 0 && !golem.getNavigation().isDone() && target.isAlive();
    }

    @Override public void start() {
        count = GolemAiRules.pathBudget();
        golem.getNavigation().moveTo(target, golem.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
    }

    @Override public void stop() {
        count = 0;
        target = null;
        golem.getNavigation().stop();
    }

    @Override public void tick() {
        if (target == null) return;
        golem.getLookControl().setLookAt(target, 30, 30);
        if (golem.distanceToSqr(target) > GolemAiRules.ITEM_PICKUP_REACH_SQ) return;
        pickUp();
    }

    /** Original {@code findItem}: the closest item inside the golem's range that it may carry. */
    private boolean findItem() {
        if (!(golem.level() instanceof ServerLevel server)) return false;
        BlockPos home = golem.getHomePosition();
        AABB search = new AABB(home).inflate(golem.getRange());
        double nearest = Double.MAX_VALUE;
        ItemEntity chosen = null;
        for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, search)) {
            if (!item.isAlive() || item.level() != server) continue;
            ItemStack stack = item.getItem();
            if (!GolemAiRules.pickupAllowed(item.pickupDelay)) continue;
            if (!accepts(stack)) continue;
            if (!stack.isEmpty() && !cargo.carried().isEmpty()
                    && (!ItemStack.isSameItemSameComponents(cargo.carried(), stack) || stack.getCount() > cargo.carrySpace())) continue;
            double distance = item.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5);
            double fromGolem = item.distanceToSqr(golem);
            if (!(fromGolem < nearest) || !GolemAiRules.withinRange(distance, golem.getRange())) continue;
            nearest = fromGolem;
            chosen = item;
        }
        target = chosen;
        return chosen != null;
    }

    /** Original {@code theGolem.inventory.getAmountNeededSmart}: only a wanted item may be picked up. */
    private boolean accepts(ItemStack stack) {
        boolean empty = true;
        for (int slot = 0; slot < golem.getSlotCount(); slot++) {
            ItemStack filter = golem.getFilter(slot);
            if (filter.isEmpty()) continue;
            empty = false;
            if (ItemStack.isSameItemSameComponents(filter, stack)) return true;
        }
        return empty;
    }

    /** Original {@code pickUp}: the stack moves into the golem's hands. */
    private void pickUp() {
        if (target == null) return;
        ItemStack stack = target.getItem();
        if (stack.isEmpty()) return;
        int amount = Math.min(stack.getCount(), Math.max(0, cargo.carrySpace()));
        if (amount <= 0) return;
        ItemStack paid = stack.split(amount);
        ItemStack carried = cargo.carried();
        if (carried.isEmpty()) {
            cargo.setCarried(paid);
        } else {
            carried.grow(amount);
            cargo.setCarried(carried);
        }
        if (stack.isEmpty()) target.discard();
        else target.setItem(stack);
        if (!(golem.level() instanceof ServerLevel server)) return;
        server.playSound(null, target.getX(), target.getY(), target.getZ(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                net.minecraft.sounds.SoundSource.NEUTRAL, 0.2F,
                ((server.random.nextFloat() - server.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
        count = 0;
        golem.getNavigation().stop();
    }

    /** The block entity at a position, when it is one; the shared lookup of the inventory goals. */
    static BlockEntity tile(ServerLevel server, BlockPos pos) {
        return server.hasChunkAt(pos) ? server.getBlockEntity(pos) : null;
    }
}
