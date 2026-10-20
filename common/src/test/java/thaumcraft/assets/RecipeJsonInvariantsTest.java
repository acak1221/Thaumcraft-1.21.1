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
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Structural invariants of every recipe the port ships, read as plain JSON (the recipe codecs cannot be exercised under a bare game
 * bootstrap because Thaumcraft's items are not registered there): known research and aspects, sane instability, every result and
 * every Thaumcraft ingredient has an item model, every Thaumcraft tag it names exists.
 */
class RecipeJsonInvariantsTest {
    private static final String RECIPES = "data/thaumcraft/recipe";
    /** A result that is named at run time, not by a language key. */
    private static final Set<String> NO_LANG_KEY = Set.of("unknown_research_note");

    private static List<String> recipePaths() throws Exception {
        List<String> paths = new ArrayList<>();
        for (String file : ResourceTree.files(RECIPES)) if (file.endsWith(".json")) paths.add(RECIPES + "/" + file);
        return paths;
    }

    private static Set<String> researchKeys() throws Exception {
        Set<String> keys = new HashSet<>();
        for (JsonElement element : ResourceTree.object("data/thaumcraft/tc4/research.json").getAsJsonArray("research")) keys.add(element.getAsJsonObject().get("key").getAsString());
        return keys;
    }

    private static Set<String> aspects() throws Exception {
        Set<String> tags = new HashSet<>();
        for (JsonElement aspect : ResourceTree.object("data/thaumcraft/tc4/aspects.json").getAsJsonArray("aspects")) tags.add(aspect.getAsJsonObject().get("tag").getAsString());
        return tags;
    }

    /** Every {@code item} / {@code id} / {@code tag} string below the element. */
    private static void references(JsonElement element, Set<String> items, Set<String> tags) {
        if (element.isJsonObject()) {
            for (var entry : element.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if ((entry.getKey().equals("item") || entry.getKey().equals("id")) && value.isJsonPrimitive() && value.getAsString().contains(":")) items.add(value.getAsString());
                else if (entry.getKey().equals("tag") && value.isJsonPrimitive()) tags.add(value.getAsString());
                else references(value, items, tags);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) references(child, items, tags);
        }
    }

    @Test void everyRecipeFileHasATypeFromAKnownNamespace() throws Exception {
        List<String> bad = new ArrayList<>();
        for (String path : recipePaths()) {
            JsonObject recipe = ResourceTree.object(path);
            String type = recipe.has("type") ? recipe.get("type").getAsString() : "";
            if (!(type.startsWith("thaumcraft:") || type.equals("minecraft:crafting_shaped") || type.equals("minecraft:crafting_shapeless") || type.equals("minecraft:smelting"))) bad.add(path + " has type '" + type + "'");
        }
        assertEquals(List.of(), bad);
        assertTrue(recipePaths().size() > 200, "the recipe tree was not found");
    }

    @Test void everyRecipeThatNamesResearchNamesKnownResearch() throws Exception {
        Set<String> research = researchKeys();
        List<String> unknown = new ArrayList<>();
        for (String path : recipePaths()) {
            JsonObject recipe = ResourceTree.object(path);
            if (!recipe.has("research")) continue;
            JsonElement key = recipe.get("research");
            if (!key.isJsonPrimitive() || !research.contains(key.getAsString())) unknown.add(path + " -> " + key);
        }
        assertEquals(List.of(), unknown);
    }

    @Test void everyAspectAmountUsesAKnownAspectAndIsPositive() throws Exception {
        Set<String> aspects = aspects();
        List<String> bad = new ArrayList<>();
        for (String path : recipePaths()) {
            JsonObject recipe = ResourceTree.object(path);
            for (String field : new String[] {"aspects", "vis"}) {
                if (!recipe.has(field)) continue;
                JsonObject amounts = recipe.getAsJsonObject(field);
                assertFalse(amounts.entrySet().isEmpty(), path + " has an empty " + field);
                for (var amount : amounts.entrySet()) {
                    if (!aspects.contains(amount.getKey())) bad.add(path + " " + field + " unknown aspect " + amount.getKey());
                    else if (amount.getValue().getAsInt() <= 0) bad.add(path + " " + field + " non-positive " + amount.getKey());
                }
            }
        }
        assertEquals(List.of(), bad);
    }

    @Test void infusionInstabilityStaysWithinTheCodecRange() throws Exception {
        List<String> bad = new ArrayList<>();
        int checked = 0;
        for (String path : recipePaths()) {
            JsonObject recipe = ResourceTree.object(path);
            if (!recipe.has("instability")) continue;
            checked++;
            int instability = recipe.get("instability").getAsInt();
            if (instability < 0 || instability > 24) bad.add(path + " instability " + instability);
        }
        assertEquals(List.of(), bad);
        assertTrue(checked >= 70, "infusion recipes with an instability: " + checked);
    }

    @Test void everyThaumcraftItemARecipeUsesOrMakesHasAnItemModel() throws Exception {
        Set<String> items = new TreeSet<>();
        for (String path : recipePaths()) references(ResourceTree.json(path), items, new TreeSet<>());
        List<String> missing = new ArrayList<>();
        for (String item : items) if (item.startsWith("thaumcraft:") && !ResourceTree.exists("assets/thaumcraft/models/item/" + item.substring("thaumcraft:".length()) + ".json")) missing.add(item);
        assertEquals(List.of(), missing);
        assertTrue(items.size() > 150, "recipe item references found: " + items.size());
    }

    @Test void everyThaumcraftResultHasAnItemNameInBothLanguages() throws Exception {
        JsonObject en = ResourceTree.object("assets/thaumcraft/lang/en_us.json");
        JsonObject ru = ResourceTree.object("assets/thaumcraft/lang/ru_ru.json");
        List<String> missing = new ArrayList<>();
        for (String path : recipePaths()) {
            JsonObject recipe = ResourceTree.object(path);
            if (!recipe.has("result") || !recipe.get("result").isJsonObject()) continue;
            String id = recipe.getAsJsonObject("result").get("id").getAsString();
            if (!id.startsWith("thaumcraft:") || NO_LANG_KEY.contains(id.substring("thaumcraft:".length()))) continue;
            String name = id.substring("thaumcraft:".length());
            boolean inEnglish = en.has("item.thaumcraft." + name) || en.has("block.thaumcraft." + name);
            boolean inRussian = ru.has("item.thaumcraft." + name) || ru.has("block.thaumcraft." + name);
            if (!inEnglish || !inRussian) missing.add(path + " -> " + id);
        }
        assertEquals(List.of(), missing);
    }

    @Test void everyThaumcraftItemTagARecipeNamesExists() throws Exception {
        Set<String> tags = new TreeSet<>();
        for (String path : recipePaths()) references(ResourceTree.json(path), new TreeSet<>(), tags);
        List<String> missing = new ArrayList<>();
        for (String tag : tags) if (tag.startsWith("thaumcraft:") && !ResourceTree.exists("data/thaumcraft/tags/item/" + tag.substring("thaumcraft:".length()) + ".json")) missing.add(tag);
        assertEquals(List.of(), missing);
        assertFalse(tags.isEmpty());
    }

    @Test void theEnchantmentInfusionsAreTheOriginalTwentyFour() throws Exception {
        List<String> enchantments = new ArrayList<>();
        for (String file : ResourceTree.files(RECIPES + "/infusion_enchantment")) if (file.endsWith(".json")) enchantments.add(ResourceTree.object(RECIPES + "/infusion_enchantment/" + file).get("enchantment").getAsString());
        assertEquals(24, enchantments.size(), "InfEnchRepair, InfEnchHaste and InfEnch0..21 of ConfigRecipes");
        assertEquals(24, new HashSet<>(enchantments).size(), "one infusion per enchantment");
        assertTrue(enchantments.containsAll(List.of("thaumcraft:haste", "thaumcraft:repair")));
        assertEquals(22, enchantments.stream().filter(id -> id.startsWith("minecraft:")).count());
    }

    @Test void theTwoThaumcraftEnchantmentsAreDefinedAsData() throws Exception {
        for (String name : new String[] {"haste", "repair"}) {
            JsonObject enchantment = ResourceTree.object("data/thaumcraft/enchantment/" + name + ".json");
            assertEquals("#thaumcraft:enchantable/" + name, enchantment.get("supported_items").getAsString());
            assertTrue(ResourceTree.exists("data/thaumcraft/tags/item/enchantable/" + name + ".json"), name);
            assertEquals("enchantment." + name, enchantment.getAsJsonObject("description").get("translate").getAsString());
        }
    }
}
