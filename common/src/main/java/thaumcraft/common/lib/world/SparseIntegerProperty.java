package thaumcraft.common.lib.world;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * An integer block state property that holds exactly the listed values.
 *
 * <p>{@link IntegerProperty} always holds every integer between its minimum and its maximum. The
 * hardness groups of the original metadata are not consecutive (the tough cosmetic stones are the
 * metadata 0, 1 and 8, the thaumium stones 4, 6 and 7), so a group block that used the range of its
 * group also owned the states of the metadata of the other groups. Those states can never be
 * placed, but the client still looks for a model of every state of a block and reported each of
 * them as a missing model. This property owns the values of the group and nothing else.
 *
 * <p>It extends {@link IntegerProperty}, so the code that reads a "variant" property through that
 * class keeps working. The values that the parent class derives from its own range are never used:
 * the possible values, the parsing of a value and the equality are all answered from the list.
 * A vanilla property of the same range can still compare equal to this one when it is the receiver
 * of {@code equals}; nothing compares the properties of different blocks.
 */
public final class SparseIntegerProperty extends IntegerProperty {
    private final List<Integer> values;

    private SparseIntegerProperty(String name, List<Integer> values) {
        super(name, values.getFirst(), values.getLast());
        this.values = values;
    }

    /**
     * The property that holds exactly these values.
     *
     * @return a plain {@link IntegerProperty} when the values are consecutive, which keeps every
     *         family whose groups are ranges exactly as it was.
     */
    public static IntegerProperty create(String name, Collection<Integer> values) {
        if (values == null || values.size() < 2) {
            throw new IllegalArgumentException("A block state property needs at least two values: " + values);
        }
        List<Integer> sorted = values.stream().sorted().toList();
        for (int index = 1; index < sorted.size(); index++) {
            if (sorted.get(index).equals(sorted.get(index - 1))) {
                throw new IllegalArgumentException("Duplicate property value " + sorted.get(index));
            }
        }
        int first = sorted.getFirst();
        int last = sorted.getLast();
        if (last - first + 1 == sorted.size()) return IntegerProperty.create(name, first, last);
        return new SparseIntegerProperty(name, List.copyOf(sorted));
    }

    @Override public Collection<Integer> getPossibleValues() { return values; }

    @Override public Optional<Integer> getValue(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return values.contains(parsed) && Integer.toString(parsed).equals(value) ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException invalid) {
            return Optional.empty();
        }
    }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof SparseIntegerProperty sparse
                && getName().equals(sparse.getName()) && values.equals(sparse.values);
    }

    @Override public int generateHashCode() { return 31 * getName().hashCode() + values.hashCode(); }
}
