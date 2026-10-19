package thaumcraft.research;

import java.util.List;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.TableTopology;
import static org.junit.jupiter.api.Assertions.*;
import static thaumcraft.common.lib.research.TableTopology.Decision.*;

class TableTopologyTest {
    @Test void fourOriginalMainPartnerPairsPointBackWithoutChangingTheirMetadata() {
        assertEquals(Direction.NORTH, TableTopology.direction(2));
        assertEquals(2, TableTopology.mainVariant(Direction.NORTH));
        assertEquals(7, TableTopology.partnerVariant(2));
        assertEquals(Direction.SOUTH, TableTopology.direction(7));
        assertEquals(6, TableTopology.partnerVariant(3));
        assertEquals(Direction.NORTH, TableTopology.direction(6));
        assertEquals(9, TableTopology.partnerVariant(4));
        assertEquals(Direction.EAST, TableTopology.direction(9));
        assertEquals(8, TableTopology.partnerVariant(5));
        assertEquals(Direction.WEST, TableTopology.direction(8));
        assertEquals(0, TableTopology.placementVariant(Direction.NORTH));
        assertEquals(0, TableTopology.placementVariant(Direction.SOUTH));
        assertEquals(1, TableTopology.placementVariant(Direction.WEST));
        assertEquals(1, TableTopology.placementVariant(Direction.EAST));
    }

    @Test void conversionRetainsSecondaryAsymmetryAndNorthSouthWestEastSearchOrder() {
        assertTrue(TableTopology.canStartConversion(0));
        assertTrue(TableTopology.canStartConversion(1));
        assertFalse(TableTopology.canStartConversion(6));
        for (int variant : new int[]{7, 8, 9}) assertTrue(TableTopology.canStartConversion(variant));
        for (int variant : new int[]{2, 3, 4, 5}) assertFalse(TableTopology.canStartConversion(variant));
        assertEquals(List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST), TableTopology.SEARCH_ORDER);
        assertThrows(UnsupportedOperationException.class, () -> TableTopology.SEARCH_ORDER.clear());
        assertTrue(TableTopology.canBeConversionPartner(0));
        assertTrue(TableTopology.canBeConversionPartner(1));
        for (int variant : new int[]{2, 5, 6, 7, 8, 9}) assertFalse(TableTopology.canBeConversionPartner(variant));
    }

    @Test void mainAcceptsAnySecondaryMetadataWithoutAReciprocalDirectionCheck() {
        // Main north next to a west-pointing secondary is old source-valid.
        assertEquals(KEEP, TableTopology.inspect(2, true, true, 8, false));
        assertEquals(KEEP, TableTopology.inspect(5, true, true, 6, false));
        assertEquals(COLLAPSE, TableTopology.inspect(2, true, true, 0, false));
        assertEquals(COLLAPSE, TableTopology.inspect(2, true, true, 5, true));
        // No global metadata on arbitrary 1.21 blocks: a deliberate adaptation.
        assertEquals(COLLAPSE, TableTopology.inspect(2, true, false, 8, false));
    }

    @Test void secondaryNeedsAnAdjacentMainBEButNotItsBacklink() {
        assertEquals(KEEP, TableTopology.inspect(7, true, true, 4, true));
        assertEquals(COLLAPSE, TableTopology.inspect(7, true, true, 0, false));
        assertEquals(COLLAPSE, TableTopology.inspect(7, true, false, -1, false));
        for (int variant : new int[]{2, 3, 4, 5, 6, 7, 8, 9}) {
            assertEquals(WAIT, TableTopology.inspect(variant, false, false, -1, false));
        }
        assertEquals(KEEP, TableTopology.inspect(0, false, false, -1, false));
        assertEquals(KEEP, TableTopology.inspect(1, false, false, -1, false));
    }
}
