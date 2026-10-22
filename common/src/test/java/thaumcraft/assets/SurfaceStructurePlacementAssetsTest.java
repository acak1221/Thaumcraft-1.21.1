package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.ThaumcraftNormalNodeFeature;

/**
 * The datapack wiring of the TC4 surface structures and of the wild aura nodes.
 *
 * <p>Oracle {@code ThaumcraftWorldGenerator.generateSurface:403-457} draws one structure
 * per chunk from an exclusive chain (mound, eldritch ring, hilltop stones) and the wild
 * aura nodes with the rarity of {@code Config.nodeRarity}. 1.21.1 has no world generator,
 * so the structures became features; the loader modules wire them into the overworld
 * biomes (Fabric: the biome modification API, NeoForge: its own biome modifiers), the
 * shared datapack only carries the features themselves.
 */
class SurfaceStructurePlacementAssetsTest {
    /** The placed features of the exclusive chain of the original and the wild nodes. */
    private static final List<String> SURFACE_STRUCTURES = List.of(
            "thaumcraft:tc4_eldritch_ring",
            "thaumcraft:hilltop_stones",
            "thaumcraft:village_banker_homes",
            "thaumcraft:village_wizard_towers");
    private static final String AURA_NODES = "thaumcraft:normal_aura_nodes";

    @Test void theSharedDatapackCarriesNoBiomeModifierFiles() throws Exception {
        // 1.21.1 has no biome modifier registry: the vanilla worldgen/biome_modifier
        // folder is not read by the game, so a file there would place nothing.
        List<String> files = ResourceTree.files("data/thaumcraft/worldgen/biome_modifier");
        assertEquals(List.of(), files, "a biome modifier file of the shared datapack is never read");
    }

    @Test void theWildAuraNodesKeepTheOriginalRarity() throws Exception {
        JsonObject placed = ResourceTree.object("data/thaumcraft/worldgen/placed_feature/normal_aura_nodes.json");
        assertEquals(AURA_NODES, placed.get("feature").getAsString());
        JsonArray placement = placed.getAsJsonArray("placement");
        assertFalse(placement.isEmpty(), "an empty placement never places a node");
        Integer chance = null;
        boolean count = false;
        boolean inSquare = false;
        boolean biome = false;
        for (JsonElement element : placement) {
            JsonObject modifier = element.getAsJsonObject();
            switch (modifier.get("type").getAsString()) {
                case "minecraft:count" -> count = modifier.get("count").getAsInt() == 1;
                case "minecraft:rarity_filter" -> chance = modifier.get("chance").getAsInt();
                case "minecraft:in_square" -> inSquare = true;
                case "minecraft:biome" -> biome = true;
                default -> { }
            }
        }
        // Oracle ThaumcraftWorldGenerator:190 - one chunk in Config.nodeRarity. The rarity
        // roll and the place of the node inside the chunk are the first two draws of the
        // random of the chunk of the original, and the totem of the chunk reads the same
        // stream, so the two features stay exclusive the way the auraGen flag of the
        // original makes them exclusive (oracle :419 and :455). The feature therefore draws
        // them itself and the placed feature only asks for the call.
        assertEquals(null, chance, "the rarity of the wild nodes is the draw of the feature");
        assertEquals(36, ThaumcraftNormalNodeFeature.NODE_RARITY);
        assertTrue(count, "the original calls the wild node pass once per chunk");
        assertFalse(inSquare, "the place of the node inside the chunk is the draw of the feature");
        assertTrue(biome, "the node belongs to the biomes of the node tag");
    }

    @Test void theSurfaceStructuresHaveTheirOwnPlacement() throws Exception {
        List<String> empty = new ArrayList<>();
        for (String structure : SURFACE_STRUCTURES) {
            String name = structure.substring(structure.indexOf(':') + 1);
            JsonObject placed = ResourceTree.object("data/thaumcraft/worldgen/placed_feature/" + name + ".json");
            if (!placed.has("placement") || placed.getAsJsonArray("placement").isEmpty()) empty.add(name);
        }
        assertEquals(List.of(), empty, "a placed feature without a placement is never placed");
    }

    @Test void originalNetherWildAuraIsActuallyScheduledByTheSharedBiomeTag() throws Exception {
        // Oracle ThaumcraftWorldGenerator:130-132,548-555. Both loader adapters
        // select this tag; allowing Nether only in the Feature body places nothing.
        List<String> values = new ArrayList<>();
        for (JsonElement entry : ResourceTree.object("data/thaumcraft/tags/worldgen/biome/normal_aura_node_biomes.json")
                .getAsJsonArray("values")) values.add(entry.getAsString());
        assertTrue(values.contains("#minecraft:is_nether"));
        assertFalse(values.contains("#minecraft:is_end"));
    }

    @Test void everyPlacedFeatureHasItsConfiguredFeature() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String file : ResourceTree.files("data/thaumcraft/worldgen/placed_feature")) {
            if (!file.endsWith(".json")) continue;
            String feature = ResourceTree.object("data/thaumcraft/worldgen/placed_feature/" + file)
                    .get("feature").getAsString();
            String name = feature.substring(feature.indexOf(':') + 1);
            if (!ResourceTree.exists("data/thaumcraft/worldgen/configured_feature/" + name + ".json")) {
                missing.add(file + " -> " + feature);
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test void theSurfaceStructureBiomesCoverTheOverworld() throws Exception {
        List<String> values = new ArrayList<>();
        for (JsonElement entry : ResourceTree.object("data/thaumcraft/tags/worldgen/biome/surface_structure_biomes.json")
                .getAsJsonArray("values")) values.add(entry.getAsString());
        assertTrue(values.contains("#minecraft:is_overworld"), "the overworld biomes: " + values);
        assertTrue(values.containsAll(List.of("thaumcraft:magical_forest", "thaumcraft:eerie", "thaumcraft:taint")),
                "the TC4 biomes of the overworld: " + values);
    }
}
