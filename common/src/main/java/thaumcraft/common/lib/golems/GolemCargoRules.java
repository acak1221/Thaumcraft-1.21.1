package thaumcraft.common.lib.golems;

import net.minecraft.world.item.ItemStack;

/**
 * The pure cargo rules of the golem.
 *
 * <p>The room a golem still has in its cargo is the original {@code EntityGolemBase.getCarrySpace} (TC4 4.2.3.5
 * oracle, {@code thaumcraft/common/entities/golems/EntityGolemBase.java:793-798}): an empty golem may take its whole
 * carry limit, a loaded one only the smaller of the room below its limit and the room below the stack size of the
 * carried item. The class is deliberately world-free, so the entity and the tests only have to call it.</p>
 */
public final class GolemCargoRules {
    private GolemCargoRules() {}

    /**
     * Original {@code getCarrySpace}: the room left in the golem's cargo.
     *
     * <p>The stack size of the carried item is the native one: 1.21.1 reads the data-driven
     * {@code MAX_STACK_SIZE} of the stack, where 1.7 read the item's own stack size (the bucket stacks to
     * sixteen in 1.21.1 and to one in 1.7).</p>
     *
     * @param carryLimit the carry limit of the golem's material, {@code null} cargo means an empty golem
     * @param carried the stack the golem already holds, {@code null} or empty means an empty golem
     */
    public static int carrySpace(int carryLimit, ItemStack carried) {
        if (carried == null || carried.isEmpty()) return carryLimit;
        return Math.min(carryLimit - carried.getCount(), carried.getMaxStackSize() - carried.getCount());
    }
}
