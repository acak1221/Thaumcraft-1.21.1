package thaumcraft.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.FamilyVariant;
import thaumcraft.common.lib.world.SparseIntegerProperty;

/**
 * The variant property of a hardness group holds exactly the metadata of that group. The groups are
 * not consecutive ranges (the cosmetic stones of the group 0 are 2, 3, 5 and 9..15), and a property
 * that spans the range of such a group owns states that no block hosts: the client wants a model for
 * each of them and reports it as missing.
 */
class SparseIntegerPropertyTest {
    private static final List<Integer> COSMETIC_SOLID = List.of(2, 3, 5, 9, 10, 11, 12, 13, 14, 15);

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static Set<Integer> valuesOf(IntegerProperty property) { return new TreeSet<>(property.getPossibleValues()); }

    @Test void aSparseGroupOwnsExactlyItsValues() {
        IntegerProperty property = FamilyVariant.property(COSMETIC_SOLID);
        assertEquals(new TreeSet<>(COSMETIC_SOLID), valuesOf(property));
        assertEquals("variant", property.getName());
        for (int hole : new int[] {0, 1, 4, 6, 7, 8, 16}) assertFalse(property.getPossibleValues().contains(hole), "value " + hole);
    }

    @Test void aConsecutiveGroupStaysAPlainIntegerProperty() {
        IntegerProperty property = FamilyVariant.property(List.of(3, 4, 5, 6));
        assertEquals(IntegerProperty.class, property.getClass(), "no new property type where a range already is exact");
        assertEquals(Set.of(3, 4, 5, 6), valuesOf(property));
        assertEquals(IntegerProperty.create("variant", 3, 6), property);
    }

    @Test void theValuesAreParsedOnlyWhenTheyAreOwned() {
        IntegerProperty property = FamilyVariant.property(List.of(0, 5));
        assertEquals(Optional.of(5), property.getValue("5"));
        assertEquals(Optional.of(0), property.getValue("0"));
        for (String invalid : new String[] {"1", "4", "-1", "05", "x", "", "5.0", "99999999999"}) {
            assertEquals(Optional.empty(), property.getValue(invalid), "'" + invalid + "'");
        }
        assertEquals("5", property.getName(5));
    }

    @Test void aStateDefinitionOfASparseGroupHasNoStateOfAForeignValue() {
        IntegerProperty property = FamilyVariant.property(COSMETIC_SOLID);
        StateDefinition<Block, BlockState> definition = new StateDefinition.Builder<Block, BlockState>(Blocks.STONE)
                .add(property).create(Block::defaultBlockState, BlockState::new);
        Set<Integer> seen = new TreeSet<>();
        for (BlockState state : definition.getPossibleStates()) seen.add(state.getValue(property));
        assertEquals(new TreeSet<>(COSMETIC_SOLID), seen, "one state per hosted value and no other");
        assertEquals(COSMETIC_SOLID.size(), definition.getPossibleStates().size());

        BlockState state = definition.any().setValue(property, 5);
        assertEquals(5, state.getValue(property));
        assertThrows(IllegalArgumentException.class, () -> definition.any().setValue(property, 4), "a foreign value is refused");
        // The neighbours of the states are wired by the values, so cycling visits the hosted values in order.
        List<Integer> cycle = new ArrayList<>();
        BlockState walk = definition.any().setValue(property, 2);
        for (int index = 0; index < COSMETIC_SOLID.size(); index++) { cycle.add(walk.getValue(property)); walk = walk.cycle(property); }
        assertEquals(COSMETIC_SOLID, cycle);
        assertEquals(2, walk.getValue(property), "and returns to the start");
    }

    @Test void theGroupsOfTheOriginalStayDistinctProperties() {
        IntegerProperty a = FamilyVariant.property(List.of(0, 1, 8));
        IntegerProperty b = FamilyVariant.property(List.of(0, 1, 8));
        IntegerProperty c = FamilyVariant.property(List.of(0, 1, 7));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, IntegerProperty.create("variant", 0, 8), "a full range is not the sparse group");
        assertTrue(a instanceof IntegerProperty, "code that reads a variant property as an IntegerProperty keeps working");
    }

    @Test void aBadPropertyIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> SparseIntegerProperty.create("variant", List.of(3)));
        assertThrows(IllegalArgumentException.class, () -> SparseIntegerProperty.create("variant", List.of()));
        assertThrows(IllegalArgumentException.class, () -> SparseIntegerProperty.create("variant", List.of(1, 4, 4)));
    }

    @Test void clampingLandsOnAHostedValue() {
        // The callers that used to clamp into min..max would otherwise name a state that does not exist.
        List<Integer> hosted = List.of(2, 3, 5, 9);
        for (int value : hosted) assertEquals(value, FamilyVariant.snap(hosted, value), "a hosted value stays");
        assertEquals(3, FamilyVariant.snap(hosted, 4), "a tie goes to the lower value");
        assertEquals(5, FamilyVariant.snap(hosted, 6));
        assertEquals(5, FamilyVariant.snap(hosted, 7), "5 and 9 are equally far from 7: the lower one");
        assertEquals(9, FamilyVariant.snap(hosted, 8));
        assertEquals(2, FamilyVariant.snap(hosted, 0));
        assertEquals(9, FamilyVariant.snap(hosted, 40));
    }
}
