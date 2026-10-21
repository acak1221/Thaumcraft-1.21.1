package thaumcraft.common.lib.world;

import net.minecraft.world.level.block.state.properties.Property;

/**
 * The variant property of the original metadata families.
 *
 * <p>The original carried the metadata values of a family in one block. Minecraft 1.21.1 binds
 * the state space to the block, so the port split every family into one block per group of
 * metadata values, and every one of those blocks must carry only the states of its own group.
 * The property of a group therefore has the range of that group, and it is chosen before the
 * block is built, because Minecraft builds the state definition inside the block constructor.
 *
 * <p>The property instance differs from block to block, so the value of a state is read through
 * the property of that state, never through a shared constant.
 */
public final class FamilyVariant {
    /** The name of the property that carries the original metadata value. */
    public static final String NAME = "variant";

    /**
     * The property that the block under construction carries.
     *
     * <p>The state definition of a block is built inside the block constructor, before the
     * fields of the block exist, so the property of the group is handed over here by the factory
     * that builds the block.
     */
    private static final ThreadLocal<Property<?>> PENDING = new ThreadLocal<>();

    private FamilyVariant() {}

    /** The property of the block that is being built. */
    public static ThreadLocal<Property<?>> pending() { return PENDING; }

    /**
     * The variant property of the given state.
     *
     * @return the property of that state, or null when the state has no variant property.
     */
    public static Property<?> of(net.minecraft.world.level.block.state.BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (NAME.equals(property.getName())) return property;
        }
        return null;
    }

    /** The original metadata value of the given state. */
    public static int value(net.minecraft.world.level.block.state.BlockState state) {
        Property<?> property = of(state);
        if (property == null) throw new IllegalStateException("The state " + state + " has no variant property");
        return (Integer) state.getValue(property);
    }

    /** The state with the given original metadata value. */
    public static net.minecraft.world.level.block.state.BlockState with(net.minecraft.world.level.block.state.BlockState state, int value) {
        Property<?> property = of(state);
        if (property == null) throw new IllegalStateException("The state " + state + " has no variant property");
        @SuppressWarnings("unchecked")
        Property<Integer> typed = (Property<Integer>) property;
        return state.setValue(typed, value);
    }
}
