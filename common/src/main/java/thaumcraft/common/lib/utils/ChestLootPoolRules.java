package thaumcraft.common.lib.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The weighted pool of a chest loot table that the original TC4 chest rows join.
 *
 * <p>The original {@code ChestGenHooks} had ONE weighted list per chest category and appended the
 * TC4 rows to it. 1.21.1 loot tables are immutable and carry several specialised pools, so both
 * loaders append the rows to the FIRST pool of the native table: the TC4 rows then compete with
 * the existing entries of that pool instead of guaranteeing extra loot.</p>
 *
 * <p>The selection is pure JSON logic, so it lives here and is shared by the loader adapters.</p>
 */
public final class ChestLootPoolRules {
    private ChestLootPoolRules() {}

    /**
     * The first pool of an encoded loot table.
     *
     * @param encoded the encoded loot table, may be null
     * @return the first pool, or null when the table has no pools at all
     */
    public static JsonObject firstPool(JsonObject encoded) {
        if (encoded == null) return null;
        JsonElement pools = encoded.get("pools");
        if (pools == null || !pools.isJsonArray()) return null;
        JsonArray array = pools.getAsJsonArray();
        if (array.isEmpty() || !array.get(0).isJsonObject()) return null;
        return array.get(0).getAsJsonObject();
    }
}
