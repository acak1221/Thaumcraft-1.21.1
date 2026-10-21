package thaumcraft.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.utils.ChestLootPoolRules;

/**
 * The weighted pool of a chest loot table that the original TC4 chest rows join.
 *
 * <p>The loader adapters append the original rows to the FIRST pool of the native table, exactly
 * like the original {@code ChestGenHooks}, which had ONE weighted list per chest category. The
 * selection is pure JSON logic, so it is pinned here.</p>
 */
class ChestLootPoolRulesTest {

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Test void picksTheFirstPoolOfANativeTable() {
        JsonObject pool = ChestLootPoolRules.firstPool(parse(
                "{\"pools\":[{\"name\":\"main\",\"entries\":[{\"type\":\"minecraft:item\"}]},"
                        + "{\"name\":\"second\",\"entries\":[]}]}"));
        assertNotNull(pool, "a table with pools has a first pool");
        assertEquals("main", pool.get("name").getAsString());
    }

    @Test void aTableWithoutPoolsHasNoPool() {
        assertNull(ChestLootPoolRules.firstPool(parse("{}")));
        assertNull(ChestLootPoolRules.firstPool(parse("{\"random_sequence\":{}}")));
    }

    /**
     * The empty pool list is the case the inverted guard of the NeoForge adapter used to reach
     * {@code pools.get(0)}: the index of an empty array is an IndexOutOfBoundsException.
     */
    @Test void anEmptyPoolListHasNoPoolAndDoesNotThrow() {
        assertNull(ChestLootPoolRules.firstPool(parse("{\"pools\":[]}")));
    }

    @Test void aMissingTableHasNoPool() {
        assertNull(ChestLootPoolRules.firstPool(null));
    }

    @Test void aNonObjectPoolIsIgnored() {
        assertNull(ChestLootPoolRules.firstPool(parse("{\"pools\":[\"not a pool\"]}")));
    }

    @Test void theFirstPoolKeepsItsOwnEntriesArray() {
        JsonObject pool = ChestLootPoolRules.firstPool(parse("{\"pools\":[{\"entries\":[]}]}"));
        assertNotNull(pool);
        JsonArray entries = pool.getAsJsonArray("entries");
        assertNotNull(entries);
        assertEquals(0, entries.size());
        // The returned pool is the live object of the encoded table, so the
        // adapter can append its rows to it.
        assertSame(pool.get("entries"), entries);
    }
}
