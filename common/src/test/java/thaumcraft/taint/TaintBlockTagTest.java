package thaumcraft.taint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The taint block tag of the native port.
 *
 * <p>Oracle {@code thaumcraft/common/config/Config.java:177,877} builds the taint material and
 * {@code thaumcraft/common/blocks/BlockTaint.java:42} and {@code BlockTaintFibres.java:41} give it
 * to the taint block family, so every block of the family carries the taint material.</p>
 *
 * <p>Oracle {@code thaumcraft/common/items/wands/foci/ItemFocusTrade.java:122,152} refuses to swap
 * a block whose material is the taint material. The native port implements the same rule with the
 * block tag {@code thaumcraft:taint}
 * ({@code common/src/main/java/thaumcraft/common/lib/world/TradeSwaps.java:101}), but the tag file
 * did not exist, so the check never fired and the trade focus could swap tainted blocks.</p>
 */
class TaintBlockTagTest {
    private static final String RESOURCE = "data/thaumcraft/tags/block/taint.json";

    private static JsonObject tag() throws Exception {
        try (InputStream stream = TaintBlockTagTest.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            assertNotNull(stream, "the trade focus needs the taint block tag " + RESOURCE);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test void theTaintBlockTagExistsAndCarriesTheWholeTaintFamily() throws Exception {
        JsonObject tag = tag();
        assertEquals(false, tag.get("replace").getAsBoolean(), "the tag adds to the other taint tags");
        List<String> values = new ArrayList<>();
        for (JsonElement element : tag.getAsJsonArray("values")) values.add(element.getAsString());
        // Oracle BlockTaint:36-42 and BlockTaintFibres:36-41 are the taint material blocks.
        assertTrue(values.contains("thaumcraft:taint_crust"), "the taint crust");
        assertTrue(values.contains("thaumcraft:taint_soil"), "the taint soil");
        assertTrue(values.contains("thaumcraft:taint_fibres"), "the taint fibres");
        // Oracle ConfigBlocks.blockTaint metadata 2 is the taint flesh, the native flesh block.
        assertTrue(values.contains("thaumcraft:flesh_block"), "the taint flesh");
        assertEquals(4, values.size(), "the whole taint family and nothing else");
    }
}
