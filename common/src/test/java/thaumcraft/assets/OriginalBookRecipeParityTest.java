package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Fresh ConfigRecipes 4.2.3.5 comparisons found by the complete Thaumonomicon page audit. */
class OriginalBookRecipeParityTest {
    @ParameterizedTest
    @MethodSource("restoredIngredients")
    void theBookAndGameplayUseTheOriginalIngredient(int sourceLine, String recipe, String pointer, String originalItem) throws Exception {
        JsonElement value = ResourceTree.object("data/thaumcraft/recipe/" + recipe + ".json");
        for (String part : pointer.split("/")) {
            value = value.isJsonArray() ? value.getAsJsonArray().get(Integer.parseInt(part)) : value.getAsJsonObject().get(part);
        }
        assertEquals(originalItem, value.getAsString(), "TC4 ConfigRecipes:" + sourceLine + ": " + recipe + " " + pointer);
    }

    private static Stream<Arguments> restoredIngredients() {
        // MCP stable 12-1.7.10: field_150336_V=brick_block, field_151028_Y=iron_helmet,
        // field_151030_Z=iron_chestplate, field_151042_j=iron_ingot and field_151172_bF=carrot.
        // ConfigItems.itemResource metadata 17 is the Void Seed, not the ingot (metadata 16).
        return Stream.of(
                Arguments.of(167, "crucible/golem_clay", "catalyst/item", "minecraft:bricks"),
                Arguments.of(276, "arcane/golem_decoration_visor", "key/H/item", "minecraft:iron_helmet"),
                Arguments.of(277, "arcane/golem_decoration_armor", "key/A/item", "minecraft:iron_chestplate"),
                Arguments.of(362, "infusion/mirror_essentia", "components/0/item", "minecraft:iron_ingot"),
                Arguments.of(362, "infusion/mirror_essentia", "components/1/item", "minecraft:iron_ingot"),
                Arguments.of(362, "infusion/mirror_essentia", "components/2/item", "minecraft:iron_ingot"),
                Arguments.of(378, "infusion/fertility_lamp", "components/4/item", "minecraft:carrot"),
                Arguments.of(394, "infusion/eldritch_eye", "components/0/item", "thaumcraft:void_seed"));
    }

    @Test void theThaumometerUsesTwoWildcardShardsAboveAndBelowItsGlass() throws Exception {
        var recipe = ResourceTree.object("data/thaumcraft/recipe/thaumometer.json");
        assertEquals(List.of(" S ", "IGI", " S "),
                recipe.getAsJsonArray("pattern").asList().stream().map(value -> value.getAsString()).toList(),
                "original ConfigRecipes:458 uses two centred shards, not four corner shards");
        var key = recipe.getAsJsonObject("key");
        assertEquals("minecraft:gold_ingot", key.getAsJsonObject("I").get("item").getAsString());
        assertEquals("minecraft:glass", key.getAsJsonObject("G").get("item").getAsString());
        assertEquals("thaumcraft:shards", key.getAsJsonObject("S").get("tag").getAsString());
        var shards = ResourceTree.object("data/thaumcraft/tags/item/shards.json");
        assertEquals(Set.of("thaumcraft:air_shard", "thaumcraft:fire_shard", "thaumcraft:water_shard",
                        "thaumcraft:earth_shard", "thaumcraft:order_shard", "thaumcraft:entropy_shard", "thaumcraft:balance_shard"),
                shards.getAsJsonArray("values").asList().stream().map(value -> value.getAsString()).collect(Collectors.toSet()),
                "the original wildcard accepts every shard metadata, including balanced");
    }

    @Test void theBoneRodRequiresUndeadRatherThanDeathEssentia() throws Exception {
        var recipe = ResourceTree.object("data/thaumcraft/recipe/infusion/wand_rod_bone.json");
        var aspects = recipe.getAsJsonObject("aspects").entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getAsInt()));
        assertEquals(Map.of("perditio", 12, "praecantatio", 6, "exanimis", 6), aspects,
                "original ConfigRecipes:334 uses Aspect.UNDEAD (exanimis), not Aspect.DEATH (mortuus)");
    }
}
