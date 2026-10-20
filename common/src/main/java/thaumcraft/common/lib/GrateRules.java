package thaumcraft.common.lib;

import net.minecraft.core.Direction;

/**
 * The pure rules of the Item Grate (original BlockMetalDevice metadata 5 = open, 6 = closed, TileGrate, EntityItemGrate): when a
 * neighbouring change opens or closes it, what stops which entity, which side takes items, and where a piped item reappears.
 */
public final class GrateRules {
    /** BlockMetalDevice:130 - the plate is the top 3/16 of the block. */
    public static final double PLATE_BOTTOM = 0.8125D;
    /** TileGrate.setInventorySlotContents - a piped-in item appears 0.6 above the block's floor, centred, moving down at 0.1 per tick. */
    public static final double SPAWN_HEIGHT = 0.6D;
    public static final double FALL_SPEED = -0.1D;

    private GrateRules() {}

    /**
     * BlockMetalDevice.onNeighborBlockChange + onPoweredBlockChange: the grate only reacts when it is powered now or when the block that
     * changed can provide power (a lever switched off, a torch burning out); then an open grate closes when powered and a closed one
     * opens when unpowered, which for both comes down to "closed exactly when powered". Otherwise nothing happens.
     */
    public static boolean closedAfterNeighbourChange(boolean closed, boolean powered, boolean changedBlockProvidesPower) {
        return powered || changedBlockProvidesPower ? powered : closed;
    }

    /**
     * BlockMetalDevice.addCollisionBoxesToList: a closed grate stops everything, an open one stops everything that is not a dropped item,
     * and, quirk kept, stops nothing at all when asked without an entity (placement and similar checks).
     */
    public static boolean collides(boolean closed, boolean hasEntity, boolean isDroppedItem) {
        return closed || hasEntity && !isDroppedItem;
    }

    /** TileGrate.getAccessibleSlotsFromSide / canInsertItem / isItemValidForSlot: only an open grate, only from above, only into slot 0. */
    public static boolean acceptsFrom(boolean closed, Direction side) { return !closed && side == Direction.UP; }

    /** The original block drops the same item whether it was open or closed (BlockMetalDevice.damageDropped: 6 -> 5). */
    public static boolean dropsTheSameItemWhenClosed() { return true; }
}
