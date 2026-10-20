package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tag files (both the mod's own and the vanilla ones it extends) and the custom damage types. */
class DataTagAndDamageTypeTest {
    private static final Set<String> SCALING = Set.of("never", "always", "when_caused_by_living_non_player");
    private static final Set<String> EFFECTS = Set.of("hurt", "thorns", "drowning", "burning", "poking", "freezing");

    /** Every tag file as {@code data/<namespace>/tags/<registry>/<name>.json}. */
    private static List<String> tagFiles() throws Exception {
        List<String> files = new ArrayList<>();
        for (String namespace : new String[] {"thaumcraft", "minecraft"}) {
            for (String file : ResourceTree.files("data/" + namespace + "/tags")) if (file.endsWith(".json")) files.add("data/" + namespace + "/tags/" + file);
        }
        return files;
    }

    /** The registry directory a tag file lives in, e.g. {@code block} or {@code worldgen/biome}. */
    private static String registry(String path) {
        String rest = path.substring(path.indexOf("/tags/") + "/tags/".length());
        String[] parts = rest.split("/");
        return parts[0].equals("worldgen") ? parts[0] + "/" + parts[1] : parts[0];
    }

    private static String entryId(JsonElement entry) { return entry.isJsonPrimitive() ? entry.getAsString() : entry.getAsJsonObject().get("id").getAsString(); }

    private static boolean optional(JsonElement entry) { return entry.isJsonObject() && entry.getAsJsonObject().has("required") && !entry.getAsJsonObject().get("required").getAsBoolean(); }

    @Test void everyTagFileIsAWellFormedValueList() throws Exception {
        List<String> bad = new ArrayList<>();
        List<String> files = tagFiles();
        assertTrue(files.size() > 80, "tag files found: " + files.size());
        for (String path : files) {
            JsonObject tag = ResourceTree.object(path);
            if (!tag.has("values") || !tag.get("values").isJsonArray()) { bad.add(path + " has no values array"); continue; }
            if (tag.has("replace") && tag.get("replace").getAsBoolean()) bad.add(path + " replaces the whole vanilla tag");
            Set<String> seen = new HashSet<>();
            for (JsonElement entry : tag.getAsJsonArray("values")) {
                if (!entry.isJsonPrimitive() && !(entry.isJsonObject() && entry.getAsJsonObject().has("id"))) { bad.add(path + " has a malformed entry " + entry); continue; }
                String id = entryId(entry);
                if (!id.contains(":") && !id.startsWith("#")) bad.add(path + " entry without a namespace: " + id);
                if (!seen.add(id)) bad.add(path + " lists " + id + " twice");
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test void everyReferencedThaumcraftTagExists() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String path : tagFiles()) {
            for (JsonElement entry : ResourceTree.object(path).getAsJsonArray("values")) {
                String id = entryId(entry);
                if (!id.startsWith("#thaumcraft:")) continue;
                String target = "data/thaumcraft/tags/" + registry(path) + "/" + id.substring("#thaumcraft:".length()) + ".json";
                if (!ResourceTree.exists(target)) missing.add(path + " -> " + id);
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test void everyRequiredThaumcraftBlockOrItemInATagExists() throws Exception {
        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (String path : tagFiles()) {
            String registry = registry(path);
            if (!registry.equals("block") && !registry.equals("item")) continue;
            for (JsonElement entry : ResourceTree.object(path).getAsJsonArray("values")) {
                String id = entryId(entry);
                if (!id.startsWith("thaumcraft:") || optional(entry)) continue;
                checked++;
                String name = id.substring("thaumcraft:".length());
                boolean exists = registry.equals("block") ? ResourceTree.exists("assets/thaumcraft/blockstates/" + name + ".json")
                        : ResourceTree.exists("assets/thaumcraft/models/item/" + name + ".json");
                if (!exists) missing.add(path + " -> " + id);
            }
        }
        assertEquals(List.of(), missing, "a required tag entry that does not exist makes the whole tag fail to load");
        assertTrue(checked > 20, "tag entries checked: " + checked);
    }

    @Test void everyRequiredThaumcraftDamageTypeInATagIsDefined() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String path : tagFiles()) {
            if (!registry(path).equals("damage_type")) continue;
            for (JsonElement entry : ResourceTree.object(path).getAsJsonArray("values")) {
                String id = entryId(entry);
                if (id.startsWith("thaumcraft:") && !optional(entry) && !ResourceTree.exists("data/thaumcraft/damage_type/" + id.substring("thaumcraft:".length()) + ".json")) missing.add(path + " -> " + id);
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test void everyRequiredThaumcraftEnchantmentInATagIsDefined() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String path : tagFiles()) {
            if (!registry(path).equals("enchantment")) continue;
            for (JsonElement entry : ResourceTree.object(path).getAsJsonArray("values")) {
                String id = entryId(entry);
                if (id.startsWith("thaumcraft:") && !optional(entry) && !ResourceTree.exists("data/thaumcraft/enchantment/" + id.substring("thaumcraft:".length()) + ".json")) missing.add(path + " -> " + id);
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test void theCustomDamageTypesAreCompleteDefinitions() throws Exception {
        List<String> files = ResourceTree.stems("data/thaumcraft/damage_type", ".json");
        assertTrue(files.containsAll(List.of("focus_fireball", "focus_fireball_unattributed", "taint")), "custom damage types: " + files);
        List<String> bad = new ArrayList<>();
        for (String name : files) {
            JsonObject type = ResourceTree.object("data/thaumcraft/damage_type/" + name + ".json");
            if (!type.has("message_id") || type.get("message_id").getAsString().isBlank()) bad.add(name + " has no message_id");
            if (!type.has("exhaustion") || type.get("exhaustion").getAsFloat() < 0) bad.add(name + " has no non-negative exhaustion");
            if (!type.has("scaling") || !SCALING.contains(type.get("scaling").getAsString())) bad.add(name + " has an unknown scaling");
            if (type.has("effects") && !EFFECTS.contains(type.get("effects").getAsString())) bad.add(name + " has unknown effects");
        }
        assertEquals(List.of(), bad);
    }

    @Test void thaumcraftDeathMessagesExistInBothLanguages() throws Exception {
        JsonObject en = ResourceTree.object("assets/thaumcraft/lang/en_us.json");
        JsonObject ru = ResourceTree.object("assets/thaumcraft/lang/ru_ru.json");
        List<String> missing = new ArrayList<>();
        for (String name : ResourceTree.stems("data/thaumcraft/damage_type", ".json")) {
            String message = ResourceTree.object("data/thaumcraft/damage_type/" + name + ".json").get("message_id").getAsString();
            if (!message.startsWith("thaumcraft.")) continue;
            for (String key : new String[] {"death.attack." + message, "death.attack." + message + ".player"}) {
                if (!en.has(key)) missing.add("en_us " + key);
                if (!ru.has(key)) missing.add("ru_ru " + key);
            }
        }
        assertEquals(List.of(), missing);
        assertFalse(ResourceTree.stems("data/thaumcraft/damage_type", ".json").isEmpty());
    }

    @Test void taintCountsAsMagicDamage() throws Exception {
        List<String> values = new ArrayList<>();
        for (JsonElement entry : ResourceTree.object("data/thaumcraft/tags/damage_type/is_magic.json").getAsJsonArray("values")) values.add(entryId(entry));
        assertTrue(values.contains("thaumcraft:taint"));
        assertTrue(values.containsAll(List.of("minecraft:magic", "minecraft:indirect_magic")));
    }

    @Test void theFireballTypesBurnLikeFireAndFlyLikeProjectiles() throws Exception {
        for (String tag : new String[] {"is_fire", "is_projectile"}) {
            List<String> values = new ArrayList<>();
            for (JsonElement entry : ResourceTree.object("data/minecraft/tags/damage_type/" + tag + ".json").getAsJsonArray("values")) values.add(entryId(entry));
            assertTrue(values.containsAll(List.of("thaumcraft:focus_fireball", "thaumcraft:focus_fireball_unattributed")), tag);
        }
    }
}
