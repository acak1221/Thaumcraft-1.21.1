package thaumcraft.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchRecipeCoverage;

/**
 * The second kinetic runic girdle of the original, pinned against the oracle.
 *
 * <p>The original registers the kinetic runic girdle twice: ConfigRecipes:355 with the potion metadata
 * 16428 (0x402C) and ConfigRecipes:356 with the potion metadata 24620 (0x602C). The two metadata values
 * differ in the bit 0x2000, the flag of a BREWED potion. 1.7.10 clears that bit when gunpowder turns the
 * potion into a splash potion ({@code PotionHelper.gunpowderEffect} is {@code "+14&13-13"}, so the splash
 * bit 0x4000 may only be set while the brewed bit is clear), and the loot bag of the original only hands
 * out {@code i | 0x2000} or {@code i | 0x4000} (oracle Config.java:552-568). No item of 1.7.10 can
 * therefore carry the metadata 24620, so the second registration is dead in the original itself; the
 * research book only ever names the first one (oracle ConfigResearch.java:178).</p>
 *
 * <p>1.21.1 potions have no metadata, so the port has to carry the same recipe twice. The two native
 * files are identical, and the port says so in the code ({@link ResearchRecipeCoverage#documentedDuplicateRecipes()}).</p>
 */
class RunicGirdleKineticParityTest {
    private static final String RECIPE = "data/thaumcraft/recipe/infusion/";
    /** The metadata of the first registration of the oracle: 16428 = 0x402C. */
    private static final int FIRST_METADATA = 16428;
    /** The metadata of the second registration of the oracle: 24620 = 0x602C. */
    private static final int SECOND_METADATA = 24620;

    private static JsonObject recipe(String name) throws Exception {
        try (InputStream stream = RunicGirdleKineticParityTest.class.getClassLoader()
                .getResourceAsStream(RECIPE + name + ".json")) {
            if (stream == null) throw new AssertionError("missing resource " + RECIPE + name + ".json");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test void theTwoOriginalMetadatasDifferOnlyInTheBrewedFlag() {
        // The bit 0x2000 is the flag of a brewed potion, the bit 0x4000 the flag of a splash potion.
        assertEquals(0x4000, FIRST_METADATA & 0x4000, "the first registration is a splash potion");
        assertEquals(0, FIRST_METADATA & 0x2000, "the first registration is not a brewed potion");
        assertEquals(0x4000, SECOND_METADATA & 0x4000, "the second registration is a splash potion too");
        assertEquals(0x2000, SECOND_METADATA & 0x2000, "the second registration carries the brewed flag as well");
        assertEquals(FIRST_METADATA | 0x2000, SECOND_METADATA, "the two differ in the brewed flag only");
    }

    @Test void theTwoNativeRecipesAreTheIdenticalSplashPotionOfHarmingTwo() throws Exception {
        JsonObject first = recipe("runic_girdle_kinetic");
        JsonObject second = recipe("runic_girdle_kinetic_2");
        assertEquals(first, second, "1.21.1 potions have no metadata, so the two native recipes are the same");
        // The metadata 16428 is a splash potion (bit 0x4000) of an instant damage potion (the bits 0x04 and
        // 0x08 of PotionHelper.potionRequirements select the harm potion) with the amplifier bit 0x20 set,
        // that is the strong harming of 1.21.1.
        assertEquals("minecraft:splash_potion", first.getAsJsonArray("components").get(3).getAsJsonObject().get("item").getAsString());
        com.google.gson.JsonArray predicates = first.getAsJsonArray("component_predicates");
        assertEquals(1, predicates.size());
        assertEquals(3, predicates.get(0).getAsJsonObject().get("component").getAsInt());
        assertEquals("minecraft:strong_harming", predicates.get(0).getAsJsonObject().get("potion").getAsString());
        // The rest of the recipe is the original one: the girdle of the previous research, the balance shard,
        // four air shards, the aspects and the instability of ConfigRecipes:355.
        assertEquals("RUNICKINETIC", first.get("research").getAsString());
        assertEquals("thaumcraft:runic_girdle", first.getAsJsonObject("input").get("item").getAsString());
        assertEquals("thaumcraft:runic_girdle_kinetic", first.getAsJsonObject("result").get("id").getAsString());
        assertEquals(7, first.get("instability").getAsInt());
        assertEquals(Map.of("tutamen", 33, "praecantatio", 55, "aer", 64),
                first.getAsJsonObject("aspects").entrySet().stream().collect(
                        java.util.stream.Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().getAsInt())));
    }

    @Test void theDuplicateIsDocumentedInTheCodeWithTheOracleReference() {
        Map<String, String> duplicates = ResearchRecipeCoverage.documentedDuplicateRecipes();
        assertTrue(duplicates.containsKey("RunicGirdleKinetic_2"), "the duplicate is documented");
        String reason = duplicates.get("RunicGirdleKinetic_2");
        assertTrue(reason.contains("24620"), "the reason names the original metadata: " + reason);
        assertTrue(reason.contains("0x602C"), "the reason names the original metadata in binary: " + reason);
        assertTrue(reason.contains("Config.java:552-568"), "the reason names the loot bag of the original: " + reason);
        assertTrue(reason.contains("runic_girdle_kinetic_2"), "the reason names the native file: " + reason);
        // The living registration is not a duplicate and stays a normal mapping.
        assertFalse(duplicates.containsKey("RunicGirdleKinetic"), "the living registration is not a duplicate");
        assertEquals("thaumcraft:infusion/runic_girdle_kinetic", ResearchRecipeCoverage.nativeRecipeId("RunicGirdleKinetic"));
    }
}
