package thaumcraft.common.entities;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ThaumcraftProjectiles;

/**
 * TC4 EntityItemGrate, the item of the item filter block.
 * <p>
 * The original is an {@code EntityItem} that may not stay inside the grate:
 * its {@code pushOutOfBlocks} reports the metal device block of the fifth and
 * sixth metadata (the item filter and its hatch) as a solid block, so the item
 * is pushed out of the filter instead of falling through it.
 * <p>
 * Declared native adaptations: the native 1.21.1 tree has no metal device
 * block yet, so the grate block is recognised through an installed predicate
 * that the device subsystem supplies; the push out itself is the native port
 * of the 1.7 helper that the aspect orb already uses.
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

    @Override public void tick() {
        super.tick();
        if (isRemoved()) return;
        pushOutOfGrate();
    }

    /** The original reports the grate as a solid block, so the item is pushed out of it. */
    private void pushOutOfGrate() {
        double middleY = (getBoundingBox().minY + getBoundingBox().maxY) / 2D;
        BlockPos cell = BlockPos.containing(getX(), middleY, getZ());
        if (!isGrate(level().getBlockState(cell))) return;
        Direction exit = Direction.UP;
        double closest = 9999D;
        for (Direction side : new Direction[]{Direction.WEST, Direction.EAST, Direction.UP, Direction.NORTH, Direction.SOUTH}) {
            if (isGrate(level().getBlockState(cell.relative(side)))) continue;
            double fraction = switch (side.getAxis()) {
                case X -> getX() - cell.getX();
                case Y -> middleY - cell.getY();
                case Z -> getZ() - cell.getZ();
            };
            double distance = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1D - fraction : fraction;
            if (distance < closest) {
                closest = distance;
                exit = side;
            }
        }
        float speed = random.nextFloat() * .2F + .1F;
        double impulse = exit.getAxisDirection().getStep() * speed;
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(exit.getAxis() == Direction.Axis.X ? impulse : motion.x,
                exit.getAxis() == Direction.Axis.Y ? impulse : motion.y,
                exit.getAxis() == Direction.Axis.Z ? impulse : motion.z);
    }
}
