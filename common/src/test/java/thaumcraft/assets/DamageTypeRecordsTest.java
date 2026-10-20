package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The datapack records of the four original TC4 damage sources.
 *
 * <p>Oracle {@code thaumcraft/api/damagesource/DamageSourceThaumcraft.java:19-22}. The original built
 * the sources in the constructor of the class: {@code taint} and {@code dissolve} bypass the armour,
 * {@code taint} counts as magic damage and none of them is scaled by the difficulty. The hunger damage of
 * a 1.7.10 damage source is 0.3 unless the source changes it, and none of the four does.</p>
 */
class DamageTypeRecordsTest {
    /** The record of one damage type of the mod. */
    private static JsonObject record(String name) throws Exception {
        return ResourceTree.object("data/thaumcraft/damage_type/" + name + ".json");
    }

    /** The ids of one damage type tag. */
    private static List<String> tag(String path) throws Exception {
        List<String> values = new ArrayList<>();
        for (JsonElement entry : ResourceTree.object(path).getAsJsonArray("values")) {
            values.add(entry.isJsonPrimitive() ? entry.getAsString() : entry.getAsJsonObject().get("id").getAsString());
        }
        return values;
    }

    @Test void theFourOriginalSourcesHaveTheirOwnRecord() throws Exception {
        List<String> files = ResourceTree.stems("data/thaumcraft/damage_type", ".json");
        assertTrue(files.containsAll(List.of("taint", "tentacle", "swarm", "dissolve")), "damage types: " + files);
    }

    @Test void theDissolveRecordFollowsTheOriginal() throws Exception {
        JsonObject dissolve = record("dissolve");
        assertEquals("thaumcraft.dissolve", dissolve.get("message_id").getAsString());
        assertEquals(0.3F, dissolve.get("exhaustion").getAsFloat());
        assertEquals("never", dissolve.get("scaling").getAsString());
        // The original dissolves without burning and without pushing: the effects stay the plain hurt.
        assertFalse(dissolve.has("effects"), "the dissolve source has no special effect");
    }

    @Test void theTaintRecordFollowsTheOriginal() throws Exception {
        JsonObject taint = record("taint");
        assertEquals("thaumcraft.taint", taint.get("message_id").getAsString());
        assertEquals(0.3F, taint.get("exhaustion").getAsFloat());
        assertEquals("never", taint.get("scaling").getAsString());
        assertFalse(taint.has("effects"), "the taint source has no special effect");
    }

    @Test void theArmourBypassingSourcesAreTagged() throws Exception {
        List<String> values = tag("data/minecraft/tags/damage_type/bypasses_armor.json");
        // Oracle :19 and :22 call setDamageBypassesArmor on the taint and on the dissolve.
        assertTrue(values.containsAll(List.of("thaumcraft:taint", "thaumcraft:dissolve")), values.toString());
        // The tentacle and the swarm do not.
        assertFalse(values.contains("thaumcraft:tentacle"), values.toString());
        assertFalse(values.contains("thaumcraft:swarm"), values.toString());
    }

    @Test void onlyTheTaintSourceIsMagic() throws Exception {
        List<String> values = tag("data/thaumcraft/tags/damage_type/is_magic.json");
        // Oracle :19 calls setMagicDamage on the taint only.
        assertTrue(values.contains("thaumcraft:taint"), values.toString());
        assertFalse(values.contains("thaumcraft:dissolve"), values.toString());
    }

    @Test void theDeathMessagesExistInBothLanguages() throws Exception {
        for (String name : List.of("taint", "dissolve")) {
            String message = record(name).get("message_id").getAsString();
            for (String language : List.of("en_us", "ru_ru")) {
                JsonObject lang = ResourceTree.object("assets/thaumcraft/lang/" + language + ".json");
                assertTrue(lang.has("death.attack." + message), language + " is missing death.attack." + message);
                assertTrue(lang.has("death.attack." + message + ".player"), language + " is missing death.attack." + message + ".player");
            }
        }
    }
}
