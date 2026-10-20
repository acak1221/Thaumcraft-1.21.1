package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.client.gui.ThaumcraftRecipeDisplay;

/**
 * Resource wiring of the ten alchemy recipes of ALCHEMICALDUPLICATION / ALCHEMICALMANUFACTURE / ENTROPICPROCESSING (ConfigRecipes:101-111)
 * and the shapeless Mirrored Glass (ConfigRecipes:196): the data as the original registered it, the registration order that fills the
 * slots 12..21 between Tallow and Pure Iron, and the book entries that point at them.
 */
class AlchemyLeftoversAssetsTest {
    private static final String CRUCIBLE = "data/thaumcraft/recipe/crucible/";

    private record Alt(String key, String file, String research, int order, List<String> catalyst, String result, int count, Map<String, Integer> aspects) {}

    private static final List<Alt> ALTS = List.of(
            new Alt("AltGunpowder", "alt_gunpowder", "ALCHEMICALDUPLICATION", 12, List.of("minecraft:gunpowder"), "minecraft:gunpowder", 2, Map.of("ignis", 4, "perditio", 4)),
            new Alt("AltSlime", "alt_slime", "ALCHEMICALDUPLICATION", 13, List.of("minecraft:slime_ball"), "minecraft:slime_ball", 2, Map.of("aqua", 2, "victus", 2)),
            new Alt("AltClay", "alt_clay", "ALCHEMICALDUPLICATION", 14, List.of("minecraft:clay_ball"), "minecraft:clay_ball", 2, Map.of("aqua", 1, "terra", 2)),
            new Alt("AltGlowstone", "alt_glowstone", "ALCHEMICALDUPLICATION", 15, List.of("minecraft:glowstone_dust", "#c:dusts/glowstone"), "minecraft:glowstone_dust", 2, Map.of("lux", 3, "sensus", 1)),
            new Alt("AltInk", "alt_ink", "ALCHEMICALDUPLICATION", 16, List.of("minecraft:ink_sac"), "minecraft:ink_sac", 2, Map.of("aqua", 2, "sensus", 2)),
            new Alt("AltWeb", "alt_web", "ALCHEMICALMANUFACTURE", 17, List.of("minecraft:string"), "minecraft:cobweb", 1, Map.of("vinculum", 2, "pannus", 2)),
            new Alt("AltMossyCobble", "alt_mossy_cobble", "ALCHEMICALMANUFACTURE", 18, List.of("minecraft:cobblestone"), "minecraft:mossy_cobblestone", 1, Map.of("herba", 2, "praecantatio", 1)),
            new Alt("AltIce", "alt_ice", "ALCHEMICALMANUFACTURE", 19, List.of("minecraft:snow_block"), "minecraft:ice", 1, Map.of("ordo", 1, "gelum", 1)),
            new Alt("AltCrackedBrick", "alt_cracked_brick", "ENTROPICPROCESSING", 20, List.of("minecraft:stone_bricks"), "minecraft:cracked_stone_bricks", 1, Map.of("perditio", 2)),
            new Alt("AltBonemeal", "alt_bonemeal", "ENTROPICPROCESSING", 21, List.of("minecraft:bone"), "minecraft:bone_meal", 4, Map.of("perditio", 1)));

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static List<String> catalysts(JsonElement catalyst) {
        List<String> found = new ArrayList<>();
        JsonArray options = catalyst.isJsonArray() ? catalyst.getAsJsonArray() : new JsonArray();
        if (!catalyst.isJsonArray()) options.add(catalyst);
        for (JsonElement option : options) {
            JsonObject object = option.getAsJsonObject();
            found.add(object.has("item") ? object.get("item").getAsString() : "#" + object.get("tag").getAsString());
        }
        return found;
    }

    @Test void eachRecipeKeepsTheOriginalCatalystResultAspectsAndGate() throws Exception {
        for (Alt alt : ALTS) {
            JsonObject recipe = ResourceTree.object(CRUCIBLE + alt.file() + ".json");
            assertEquals("thaumcraft:crucible", recipe.get("type").getAsString(), alt.key());
            assertEquals(alt.research(), recipe.get("research").getAsString(), alt.key());
            assertEquals(alt.order(), recipe.get("source_order").getAsInt(), alt.key());
            assertEquals(alt.catalyst(), catalysts(recipe.get("catalyst")), alt.key());
            assertEquals(alt.result(), recipe.getAsJsonObject("result").get("id").getAsString(), alt.key());
            assertEquals(alt.count(), recipe.getAsJsonObject("result").get("count").getAsInt(), alt.key());
            Map<String, Integer> aspects = new java.util.TreeMap<>();
            recipe.getAsJsonObject("aspects").entrySet().forEach(entry -> aspects.put(entry.getKey(), entry.getValue().getAsInt()));
            assertEquals(new java.util.TreeMap<>(alt.aspects()), aspects, alt.key());
        }
    }

    @Test void theDuplicationRecipesGiveTwoOfWhatWasThrownInAndTheOthersMakeSomethingNew() {
        for (Alt alt : ALTS) {
            boolean duplication = alt.research().equals("ALCHEMICALDUPLICATION");
            assertEquals(duplication, alt.catalyst().get(0).equals(alt.result()), alt.key() + " duplicates its catalyst only under ALCHEMICALDUPLICATION");
            if (duplication) assertEquals(2, alt.count(), alt.key());
        }
    }

    @Test void everyCrucibleRecipeHasItsOwnRegistrationOrderAndTheTenFillTheGapAfterTallow() throws Exception {
        Set<Integer> seen = new HashSet<>();
        for (String stem : ResourceTree.stems("data/thaumcraft/recipe/crucible", ".json")) {
            int order = ResourceTree.object(CRUCIBLE + stem + ".json").get("source_order").getAsInt();
            assertTrue(seen.add(order), stem + " reuses registration order " + order + ": the crucible picks the first registered of equal matches");
        }
        assertEquals(11, ResourceTree.object(CRUCIBLE + "tallow.json").get("source_order").getAsInt(), "Tallow is registered just before the Alt recipes");
        assertEquals(22, ResourceTree.object(CRUCIBLE + "pure_iron.json").get("source_order").getAsInt(), "Pure Iron just after them");
        for (int order = 12; order <= 21; order++) assertTrue(seen.contains(order), "registration order " + order);
    }

    @Test void theResearchNamesTheRecipesOnItsPagesAndTheBookCanResolveThem() throws Exception {
        Map<String, JsonObject> research = new java.util.HashMap<>();
        for (JsonElement element : ResourceTree.object("data/thaumcraft/tc4/research.json").getAsJsonArray("research")) {
            research.put(element.getAsJsonObject().get("key").getAsString(), element.getAsJsonObject());
        }
        for (Alt alt : ALTS) {
            List<String> pages = new ArrayList<>();
            for (JsonElement page : research.get(alt.research()).getAsJsonArray("pages")) {
                JsonElement key = page.getAsJsonObject().get("recipe_key");
                if (key != null && !key.isJsonNull()) pages.add(key.getAsString());
            }
            assertTrue(pages.contains(alt.key()), alt.research() + " has a page for " + alt.key());
            var target = ThaumcraftRecipeDisplay.aliases().get(alt.key());
            assertEquals("thaumcraft:crucible/" + alt.file(), target == null ? null : target.toString(), alt.key() + " alias in the book");
        }
        List<String> basic = new ArrayList<>();
        for (JsonElement page : research.get("BASICARTIFACE").getAsJsonArray("pages")) {
            JsonElement key = page.getAsJsonObject().get("recipe_key");
            if (key != null && !key.isJsonNull()) basic.add(key.getAsString());
        }
        assertTrue(basic.contains("MirrorGlass"), "BASICARTIFACE lists MirrorGlass");
        assertEquals("thaumcraft:arcane/mirrored_glass", String.valueOf(ThaumcraftRecipeDisplay.aliases().get("MirrorGlass")));
    }

    @Test void mirroredGlassIsAShapelessArcaneRecipeOfQuicksilverAndAGlassPane() throws Exception {
        JsonObject recipe = ResourceTree.object("data/thaumcraft/recipe/arcane/mirrored_glass.json");
        assertEquals("thaumcraft:arcane_shapeless", recipe.get("type").getAsString());
        assertEquals("BASICARTIFACE", recipe.get("research").getAsString());
        List<String> ingredients = new ArrayList<>();
        for (JsonElement ingredient : recipe.getAsJsonArray("ingredients")) ingredients.add(ingredient.getAsJsonObject().get("item").getAsString());
        assertEquals(List.of("thaumcraft:quick_silver", "minecraft:glass_pane"), ingredients);
        assertEquals("thaumcraft:mirrored_glass", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
        assertEquals(10, recipe.getAsJsonObject("vis").get("ignis").getAsInt());
        assertEquals(10, recipe.getAsJsonObject("vis").get("terra").getAsInt());
        assertEquals(2, recipe.getAsJsonObject("vis").size());
    }
}
