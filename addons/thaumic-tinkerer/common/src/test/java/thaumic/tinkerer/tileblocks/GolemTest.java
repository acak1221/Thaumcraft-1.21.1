package thaumic.tinkerer.tileblocks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumic.tinkerer.common.core.golems.EnumGolemCores;
import thaumic.tinkerer.common.core.golems.EnumGolemDecorations;
import thaumic.tinkerer.common.core.golems.GolemAccess;
import thaumic.tinkerer.common.core.golems.GolemConnectorMethods;
import thaumic.tinkerer.common.core.golems.UnavailableGolemAccess;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The golem names and the methods of the Golem Connector's peripheral, against an in-memory golem.</p>
 */
class GolemTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** A golem in memory, as the API will describe one. */
    private static final class FakeGolem implements GolemAccess {
        String type = "Clay";
        int core = 2;
        String decoration = "HGB";
        float health = 12.5F;
        double x = 10.7, y = 64.2, z = -3.9;
        BlockPos home = new BlockPos(1, 2, 3);
        int homeFacing = 4;
        List<Marker> markers = new ArrayList<>(List.of(new Marker(5, 6, 7, "minecraft:overworld", (byte) 1, (byte) 3)));

        @Override public boolean exists() { return true; }
        @Override public String typeName() { return type; }
        @Override public int core() { return core; }
        @Override public String decoration() { return decoration; }
        @Override public float health() { return health; }
        @Override public double x() { return x; }
        @Override public double y() { return y; }
        @Override public double z() { return z; }
        @Override public BlockPos home() { return home; }
        @Override public int homeFacing() { return homeFacing; }
        @Override public List<Marker> markers() { return markers; }
        @Override public void setHome(BlockPos home, int facing) { this.home = home; this.homeFacing = facing; }
        @Override public void setMarkers(List<Marker> markers) { this.markers = new ArrayList<>(markers); }
    }

    private static GolemConnectorMethods connector(GolemAccess golem) {
        return new GolemConnectorMethods(golem, new BlockPos(100, 70, 200), "minecraft:overworld");
    }

    private static Map<String, Object> table(int x, int y, int z, String dim, int side, int color) {
        Map<String, Object> marker = new HashMap<>();
        marker.put("posX", (double) x);
        marker.put("posY", (double) y);
        marker.put("posZ", (double) z);
        marker.put("dim", dim);
        marker.put("side", (double) side);
        marker.put("color", (double) color);
        return marker;
    }

    @Test
    void theCoreAndAccessoryTablesAreTheOriginals() {
        assertEquals(EnumGolemCores.CoreFill, EnumGolemCores.getFromByte((byte) 0));
        assertEquals(EnumGolemCores.CoreFishing, EnumGolemCores.getFromByte((byte) 11));
        assertEquals(EnumGolemCores.CoreBlank, EnumGolemCores.getFromByte((byte) 100));
        assertNull(EnumGolemCores.getFromByte((byte) 12));
        assertEquals("item.ItemGolemCore.7.name", EnumGolemCores.CoreChop.getName());
        assertEquals(EnumGolemDecorations.TopHat, EnumGolemDecorations.getFromChar('H'));
        assertEquals(EnumGolemDecorations.DartGun, EnumGolemDecorations.getFromChar('R'), "the dart gun is written R");
        assertNull(EnumGolemDecorations.getFromChar('D'), "D is only the enum's own character");
        assertEquals("item.ItemGolemDecoration.6.name", EnumGolemDecorations.Plating.getName());
    }

    @Test
    void aConnectorWithoutAGolemAnswersEmpty() {
        GolemConnectorMethods methods = connector(UnavailableGolemAccess.INSTANCE);
        assertEquals(0, ((Integer[]) methods.call("getPosition")).length);
        assertEquals(0, ((Float[]) methods.call("getHealth")).length);
        for (String name : new String[] { "getDecorations", "getType", "getCore", "getHome", "getMarkers", "getMarkerCount" }) {
            assertEquals(0, methods.call(name).length, name);
        }
        assertEquals(0, methods.call("setHome", 1.0, 2.0, 3.0, 4.0).length);
        assertEquals(0, methods.call("addMarker", table(1, 2, 3, "x", 1, 1)).length);
        assertEquals(0, methods.call("setMarkers", new HashMap<>()).length);
    }

    @Test
    void everyMethodNameIsKnown() {
        GolemConnectorMethods methods = connector(new FakeGolem());
        for (String name : GolemConnectorMethods.METHOD_NAMES) {
            // an unknown method would answer null; known ones answer an array (arguments checked below)
            if (name.equals("setHome") || name.equals("setMarkers") || name.equals("addMarker") || name.equals("saveMarker")
                    || name.equals("deleteMarker") || name.equals("getMarker")) continue;
            assertTrue(methods.call(name) != null, name);
        }
        assertNull(methods.call("noSuchMethod"));
        assertEquals(15, GolemConnectorMethods.METHOD_NAMES.length);
    }

    @Test
    void theReadMethodsDescribeTheGolem() {
        FakeGolem golem = new FakeGolem();
        GolemConnectorMethods methods = connector(golem);
        assertArrayEquals(new Integer[] { 10, 64, -3 }, methods.call("getPosition"));
        assertArrayEquals(new String[] { "Clay" }, methods.call("getType"));
        assertArrayEquals(new Float[] { 12.5F }, methods.call("getHealth"));
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, methods.call("getHome"));
        Object[] decorations = methods.call("getDecorations");
        assertEquals(1, decorations.length);
        assertEquals(3, ((Map<?, ?>) decorations[0]).size());
        assertEquals(1, methods.call("getCore").length);
        golem.core = -1;
        assertEquals(1, methods.call("getCore").length, "a golem without a core reports the blank core");
    }

    @Test
    void theErrorsAreTheOriginalsWords() {
        FakeGolem golem = new FakeGolem();
        GolemConnectorMethods methods = connector(golem);
        golem.decoration = "HZ";
        assertEquals("Golem is wearing unknown accessory: Z",
                assertThrows(GolemConnectorMethods.MethodException.class, () -> methods.call("getDecorations")).getMessage());
        golem.core = 50;
        assertEquals("Golem has Unknown core: 50",
                assertThrows(GolemConnectorMethods.MethodException.class, () -> methods.call("getCore")).getMessage());
        assertEquals("Invalid arguments",
                assertThrows(GolemConnectorMethods.MethodException.class, () -> methods.call("setHome", 1.0)).getMessage());
        assertEquals("setMarkers takes 1 argument",
                assertThrows(GolemConnectorMethods.MethodException.class, () -> methods.call("setMarkers")).getMessage());
        assertEquals("marker 7 does not exist",
                assertThrows(GolemConnectorMethods.MethodException.class, () -> methods.call("getMarker", 7.0)).getMessage());
        assertEquals("marker 3 does not exist",
                assertThrows(GolemConnectorMethods.MethodException.class, () -> methods.call("deleteMarker", 3.0)).getMessage());
    }

    @Test
    void setHomeAndTheMarkerEditingWork() {
        FakeGolem golem = new FakeGolem();
        GolemConnectorMethods methods = connector(golem);

        assertArrayEquals(new Integer[] { 7, 8, 9, 2 }, methods.call("setHome", 7.0, 8.0, 9.0, 2.0));
        assertEquals(new BlockPos(7, 8, 9), golem.home);
        assertEquals(2, golem.homeFacing);

        assertEquals(1.0, ((Object[]) methods.call("getMarkerCount"))[0]);
        Object[] added = methods.call("addMarker", table(10, 11, 12, "minecraft:the_nether", 2, 5));
        Map<?, ?> addedMarker = (Map<?, ?>) added[0];
        assertEquals(10, addedMarker.get("posX"));
        assertEquals("minecraft:the_nether", addedMarker.get("dim"));
        assertEquals(2.0, ((Object[]) methods.call("getMarkerCount"))[0]);

        Object[] saved = methods.call("saveMarker", 0.0, table(1, 1, 1, "minecraft:overworld", 0, -1));
        assertEquals(1, ((Map<?, ?>) saved[0]).get("posX"));
        assertEquals(1, golem.markers.get(0).x());

        methods.call("deleteMarker", 0.0);
        assertEquals(1, golem.markers.size());
        assertEquals(10, golem.markers.get(0).x());

        Map<Double, Object> two = new HashMap<>();
        two.put(1.0, table(0, 0, 0, "a", 1, 1));
        two.put(2.0, table(9, 9, 9, "b", 2, 2));
        Object[] set = methods.call("setMarkers", two);
        assertEquals(2, golem.markers.size());
        assertEquals(2, ((Map<?, ?>) ((Object[]) set)[0]).size(), "a table of the markers, numbered from one");
    }

    @Test
    void aNewMarkerStartsAtTheConnector() {
        Object[] result = connector(new FakeGolem()).call("newMarker");
        Map<?, ?> marker = (Map<?, ?>) result[0];
        assertEquals(100, marker.get("posX"));
        assertEquals(70, marker.get("posY"));
        assertEquals(200, marker.get("posZ"));
        assertEquals("minecraft:overworld", marker.get("dim"));
        assertEquals(-1, marker.get("color"));
        assertEquals(1, marker.get("side"));
        // and it needs no golem
        assertEquals(1, connector(UnavailableGolemAccess.INSTANCE).call("newMarker").length);
    }
}
