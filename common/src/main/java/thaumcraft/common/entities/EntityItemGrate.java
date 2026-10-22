package thaumcraft.common.entities;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftProjectiles;

/**
 * TC4 EntityItemGrate, the item of the item filter block.
 * <p>
 * The original is an {@code EntityItem} that overrides the vanilla
 * {@code pushOutOfBlocks} (the 1.7.10 {@code Entity.func_145771_j}) and answers
 * {@code true} when the block at the cell of the item is the metal device of the
 * fifth or the sixth metadata (the item filter and its hatch).
 * <p>
 * The 1.7.10 vanilla {@code EntityItem.onUpdate} stores that answer in the field
 * {@code Entity.field_70145_X} - the MCP name of the field is {@code noClip},
 * "whether this entity won't clip with collision or not" - and the 1.7.10
 * {@code Entity.moveEntity} skips the whole collision handling of the move when
 * the flag is set. The original therefore lets the item of the filter fall
 * through the plate of the filter <b>without</b> changing its motion, and it
 * returns <b>before</b> the vanilla push out runs, so the vanilla push out never
 * touches the motion of the item.
 * <p>
 * The 1.21.1 counterpart of the 1.7.10 {@code noClip} flag is the public field
 * {@code Entity.noPhysics}: the 1.21.1 {@code Entity.move} sets the position
 * without any collision when it is set. The native tree therefore sets the flag
 * while the item is inside the grate and never pushes the item out.
 * <p>
 * Declared native adaptations: the native 1.21.1 tree has no metal device
 * block yet, so the grate block is recognised through an installed predicate
 * that the device subsystem supplies.
 */
public final class EntityItemGrate extends ItemEntity {
    private static Predicate<BlockState> grateTest = state -> false;

    public EntityItemGrate(EntityType<? extends EntityItemGrate> type, Level level) {
        super(type, level);
    }

    public EntityItemGrate(Level level) {
        this(ThaumcraftProjectiles.ITEM_GRATE.get(), level);
    }

    public EntityItemGrate(Level level, double x, double y, double z, ItemStack stack) {
        this(level);
        setItem(stack);
        setPos(x, y, z);
    }

    /**
     * The device subsystem installs the real grate test. The default never
     * matches, because the native metal device block is not registered yet.
     */
    public static void setGrateTest(Predicate<BlockState> test) {
        grateTest = Objects.requireNonNull(test);
    }

    public static boolean isGrate(BlockState state) {
        return grateTest.test(state);
    }

    /**
     * The height the original asks the block of the item with.
     *
     * <p>Original: the 1.7.10 {@code EntityItem.onUpdate} calls the overridden
     * push out with {@code (this.posX, (this.boundingBox.minY + this.boundingBox.maxY) / 2.0,
     * this.posZ)}, so the cell of the block is the cell of the <b>middle</b> of the
     * bounding box of the item and not the cell of its feet.</p>
     */
    public static double cellHeight(double minY, double maxY) {
        return (minY + maxY) / 2.0D;
    }

    /**
     * The cell the original asks the block of the item with.
     *
     * <p>Original {@code EntityItemGrate:27-30}: the cell is
     * {@code floor(x), floor(y), floor(z)} with the height of
     * {@link #cellHeight(double, double)}.</p>
     */
    public static BlockPos grateCell(double x, double minY, double maxY, double z) {
        return BlockPos.containing(x, cellHeight(minY, maxY), z);
    }

    /**
     * The answer of the overridden push out of the original.
     *
     * <p>Original {@code EntityItemGrate:26-36}: the override answers {@code true}
     * when the block at the cell of the item is the metal device of the fifth or
     * the sixth metadata and it answers the result of the vanilla push out in every
     * other case. The 1.7.10 {@code EntityItem} turns the {@code true} into the
     * {@code noClip} flag of the entity, so the item does not clip with the
     * collision of the block while it is inside the grate.</p>
     *
     * @param insideGrate whether the block at the cell of the item is the grate
     * @return {@code true} when the item must not clip with the collision
     */
    public static boolean noClipInsideGrate(boolean insideGrate) {
        return insideGrate;
    }

    /**
     * Whether the vanilla push out of the 1.7.10 runs for the item.
     *
     * <p>Original {@code EntityItemGrate:31-35}: the override returns <b>before</b>
     * the vanilla push out is called, so the vanilla push out never runs and the
     * motion of the item is never changed by it.</p>
     */
    public static boolean appliesVanillaPushOut(boolean insideGrate) {
        return !insideGrate;
    }

    /** The cell of the item, the middle of its bounding box included. */
    private BlockPos grateCell() {
        var box = getBoundingBox();
        return grateCell(getX(), box.minY, box.maxY, getZ());
    }

    /** Whether the item is inside the grate at the moment. */
    private boolean insideGrate() {
        if (level() == null) return false;
        return isGrate(level().getBlockState(grateCell()));
    }

    /**
     * The 1.21.1 {@code Entity.move} is the counterpart of the 1.7.10
     * {@code Entity.moveEntity}: the 1.7.10 method skips the collision handling
     * of the move when the {@code noClip} flag of the entity is set, the 1.21.1
     * method does the same when the {@code noPhysics} flag is set. The original
     * sets that flag for the item while it is inside the grate.
     */
    @Override public void move(MoverType type, Vec3 delta) {
        if (noClipInsideGrate(insideGrate())) this.noPhysics = true;
        super.move(type, delta);
    }

    /**
     * The 1.21.1 {@code ItemEntity.tick} moves an item that is inside a block to
     * the closest free space. The 1.7.10 original has no such step, and the item
     * of the filter must stay where the filter dropped it, so the step is skipped
     * while the item is inside the grate.
     */
    @Override protected void moveTowardsClosestSpace(double x, double y, double z) {
        if (insideGrate()) return;
        super.moveTowardsClosestSpace(x, y, z);
    }
}
