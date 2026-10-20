package thaumcraft.assets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Original BlockCrystal.getDrops: six shards of the crystal's own type, ignoring Silk Touch and Fortune. */
class CrystalLootTableTest {
    private static final String[] TYPES = {"air", "fire", "water", "earth", "order", "entropy"};

    private JsonObject load(String type) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream("data/thaumcraft/loot_table/blocks/crystal_" + type + ".json");
        assertNotNull(stream, "missing loot table for crystal_" + type);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void everyCrystalDropsSixShardsOfItsOwnType() throws Exception {
        for (String type : TYPES) {
            JsonObject table = load(type);
            assertEquals("minecraft:block", table.get("type").getAsString());
            var pools = table.getAsJsonArray("pools");
            assertEquals(1, pools.size(), "one pool: " + type);
            var pool = pools.get(0).getAsJsonObject();
            assertEquals(1, pool.get("rolls").getAsInt());
            var entries = pool.getAsJsonArray("entries");
            assertEquals(1, entries.size(), "a single unconditional entry (no Silk Touch branch): " + type);
            var entry = entries.get(0).getAsJsonObject();
            assertEquals("minecraft:item", entry.get("type").getAsString());
            assertEquals("thaumcraft:" + type + "_shard", entry.get("name").getAsString());
            assertFalse(entry.has("conditions"), "original getDrops ignores tools: " + type);
            var functions = entry.getAsJsonArray("functions");
            assertEquals(1, functions.size(), "only set_count; no Fortune bonus in the original: " + type);
            var count = functions.get(0).getAsJsonObject();
            assertEquals("minecraft:set_count", count.get("function").getAsString());
            assertEquals(6, count.get("count").getAsInt());
        }
    }
}
