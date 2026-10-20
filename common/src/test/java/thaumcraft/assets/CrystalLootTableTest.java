package thaumcraft.assets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Original BlockCrystal.getDrops returns SIX individual stacks, with six different primals for metadata6. */
class CrystalLootTableTest {
    private static final String[] TYPES = {"air", "fire", "water", "earth", "order", "entropy"};

    private JsonObject load(String type) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream("data/thaumcraft/loot_table/blocks/crystal_" + type + ".json");
        assertNotNull(stream, "missing loot table for crystal_" + type);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test void everyPrimalCrystalDropsSixSeparateOwnShardsWithoutToolBranches() throws Exception {
        for (String type : TYPES) {
            assertEquals(java.util.Collections.nCopies(6, "thaumcraft:" + type + "_shard"), individualDrops(type));
        }
    }

    @Test void mixedCrystalReturnsOneOfEachOriginalPrimalAndNoBalancedShard() throws Exception {
        assertEquals(List.of("thaumcraft:air_shard", "thaumcraft:fire_shard", "thaumcraft:water_shard",
                "thaumcraft:earth_shard", "thaumcraft:order_shard", "thaumcraft:entropy_shard"), individualDrops("mixed"));
    }

    private List<String> individualDrops(String type) throws Exception {
        JsonObject table = load(type);
        assertEquals("minecraft:block", table.get("type").getAsString());
        assertFalse(table.has("functions"), "no table function can introduce a tool-dependent or multiplied drop");
        var pools = table.getAsJsonArray("pools");
        assertEquals(6, pools.size(), "original getDrops emits six separate ItemStacks, with independent explosion survival: " + type);
        var names = new ArrayList<String>();
        for (var element : pools) {
            var pool = element.getAsJsonObject();
            assertEquals(1, pool.get("rolls").getAsInt());
            assertFalse(pool.has("functions"), "no pool function can aggregate or multiply the original individual shard");
            var conditions = pool.getAsJsonArray("conditions");
            assertEquals(1, conditions.size());
            assertEquals("minecraft:survives_explosion", conditions.get(0).getAsJsonObject().get("condition").getAsString());
            var entries = pool.getAsJsonArray("entries");
            assertEquals(1, entries.size(), "a single unconditional entry (no Silk Touch branch): " + type);
            var entry = entries.get(0).getAsJsonObject();
            assertEquals("minecraft:item", entry.get("type").getAsString());
            names.add(entry.get("name").getAsString());
            assertFalse(entry.has("conditions"), "original getDrops ignores tools: " + type);
            assertFalse(entry.has("functions"), "default item count ONE, no aggregate set_count6, Fortune or NBT copying: " + type);
        }
        return names;
    }
}
