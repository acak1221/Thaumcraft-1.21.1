package thaumcraft.common.tiles;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftLevitator;
import thaumcraft.common.lib.LevitatorRules;

/**
 * Original TileLifter. Every tick, on both sides as in the original (the client moves the player, the server everything else), unless
 * the block or the one above it is powered: every item, pushable entity or horse in the column above it, up to {@link #getRange()}
 * blocks or the first opaque block, gains upward speed (a sneaker slows its fall instead) and forgets its fall. The range is worked out
 * when the block asks for it and every hundredth tick: 10, and 10 more for every unpowered levitator directly under it. Nothing is saved.
 */
public final class TileLevitator extends BlockEntity {
    private int counter;
    private int rangeAbove;
    private boolean requiresUpdate = true;
    private boolean lastPowerState;

    public TileLevitator(BlockPos pos, BlockState state) { super(ThaumcraftLevitator.LEVITATOR_BLOCK_ENTITY.get(), pos, state); }

    public int getRange() { return rangeAbove; }
    public boolean wasPowered() { return lastPowerState; }
    /** The block marks the levitators it has just changed under (or over) so they count again at their next tick. */
    public void requestUpdate() { requiresUpdate = true; }
    public boolean isUpdateRequested() { return requiresUpdate; }

    /** Original gettingPower: the block itself or the one above it receives a signal. */
    public boolean gettingPower() {
        return level != null && (level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.above()));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileLevitator tile) {
        ++tile.counter;
        if (tile.requiresUpdate || tile.counter % LevitatorRules.RECHECK_TICKS == 0) {
            tile.lastPowerState = tile.gettingPower();
            tile.requiresUpdate = false;
            int max = LevitatorRules.maxRange(0), count = 1;
            while (level.getBlockState(pos.below(count)).is(state.getBlock()) && !level.hasNeighborSignal(pos.below(count))) {
                ++count;
                max += LevitatorRules.RANGE_PER_LEVITATOR;
            }
            tile.rangeAbove = 0;
            while (tile.rangeAbove < max && !level.getBlockState(pos.above(1 + tile.rangeAbove)).canOcclude()) ++tile.rangeAbove;
        }
        if (tile.rangeAbove <= 0 || tile.gettingPower()) return;
        List<Entity> targets = level.getEntitiesOfClass(Entity.class, new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1 + tile.rangeAbove, pos.getZ() + 1));
        for (Entity entity : targets) {
            if (!LevitatorRules.lifts(entity instanceof ItemEntity, entity.isPushable(), entity instanceof AbstractHorse)) continue;
            Vec3 motion = entity.getDeltaMovement();
            double lifted = LevitatorRules.lifted(motion.y, entity.isShiftKeyDown());
            if (lifted != motion.y) entity.setDeltaMovement(motion.x, lifted, motion.z);
            entity.resetFallDistance();
        }
    }
}
