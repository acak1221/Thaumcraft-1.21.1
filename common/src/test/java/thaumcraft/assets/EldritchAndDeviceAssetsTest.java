package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.registry.registries.RegistrySupplier;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.config.ThaumcraftLegacyTiles;

/**
 * Resource wiring of the eldritch, cosmetic and device block families of
 * {@link ThaumcraftEldritchBlocks} and of the legacy tiles of
 * {@link ThaumcraftLegacyTiles}.
 *
 * <p>The two registration classes are read by reflection, so the test fails as
 * soon as a new block or item is registered without a blockstate, a block model,
 * an item model, a loot table or a name in both languages. Every model that a
 * blockstate names has to exist and every texture it uses has to be a file of
 * the mod.
 */
class EldritchAndDeviceAssetsTest {
    private static final String ASSETS = "assets/thaumcraft/";
    private static final String EN = ASSETS + "lang/en_us.json";
    private static final String RU = ASSETS + "lang/ru_ru.json";

    /** The original metadata values of the eldritch family, split by the hardness group of the native blocks. */
    private static final List<String> ELDRITCH_GROUPS = List.of("eldritch", "eldritch_crust", "eldritch_deco",
            "eldritch_doorway", "eldritch_spawner", "eldritch_crystal");

    /**
     * The items that place ONE metadata value of a family block. They have no block of their own,
     * because the original recipe result was a block with a metadata.
     */
    private static final Set<String> VARIANT_ITEMS = Set.of("paving_stone_travel", "paving_stone_warding",
            "warded_glass", "banner", "flux_scrubber", "wand_pedestal_focus", "obsidian_tile", "mirror_essentia");

    @Test void theTwoRegistrationClassesAreRead() {
        assertFalse(blockIds().isEmpty(), "no block was read from the two registration classes");
        assertFalse(itemIds().isEmpty(), "no item was read from the two registration classes");
    }

    @Test void everyRegisteredBlockHasABlockstateABlockModelALootTableAndAName() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        List<String> missing = new ArrayList<>();
        for (String id : blockIds()) {
            if (!ResourceTree.exists(ASSETS + "blockstates/" + id + ".json")) missing.add("blockstate " + id);
            if (!ResourceTree.exists(ASSETS + "models/block/" + id + ".json")) missing.add("block model " + id);
            if (!ResourceTree.exists("data/thaumcraft/loot_table/blocks/" + id + ".json")) missing.add("loot table " + id);
            if (!en.has("block.thaumcraft." + id)) missing.add("en_us block name " + id);
            if (!ru.has("block.thaumcraft." + id)) missing.add("ru_ru block name " + id);
        }
        assertEquals(List.of(), missing);
    }

    @Test void everyRegisteredItemHasAnItemModelAndAName() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        List<String> missing = new ArrayList<>();
        for (String id : itemIds()) {
            if (!ResourceTree.exists(ASSETS + "models/item/" + id + ".json")) missing.add("item model " + id);
            if (!en.has("item.thaumcraft." + id)) missing.add("en_us item name " + id);
            if (!ru.has("item.thaumcraft." + id)) missing.add("ru_ru item name " + id);
        }
        assertEquals(List.of(), missing);
    }

    @Test void everyBlockThatHasAnItemAlsoHasAnItemModelAndTheOtherWayRound() {
        Set<String> blocks = new TreeSet<>(blockIds());
        Set<String> items = new TreeSet<>(itemIds());
        List<String> missing = new ArrayList<>();
        for (String id : blocks) if (items.contains(id) && !ResourceTree.exists(ASSETS + "models/item/" + id + ".json")) {
            missing.add("item model " + id);
        }
        for (String id : items) if (!blocks.contains(id) && !VARIANT_ITEMS.contains(id)) missing.add("an item without a block: " + id);
        assertEquals(List.of(), missing);
    }

    @Test void everyBlockstateOfTheTwoFamiliesNamesAModelThatExists() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String id : blockIds()) {
            JsonObject state = ResourceTree.object(ASSETS + "blockstates/" + id + ".json");
            List<String> models = new ArrayList<>();
            if (state.has("variants")) for (var variant : state.getAsJsonObject("variants").entrySet()) {
                collect(variant.getValue(), models);
            }
            if (state.has("multipart")) for (JsonElement part : state.getAsJsonArray("multipart")) {
                collect(part.getAsJsonObject().get("apply"), models);
            }
            assertFalse(models.isEmpty(), id + " names no model at all");
            for (String model : models) if (!modelExists(model)) missing.add(id + " -> " + model);
        }
        assertEquals(List.of(), missing);
    }

    @Test void everyTextureOfTheModelsOfTheTwoFamiliesExists() throws Exception {
        List<String> missing = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        for (String id : blockIds()) {
            JsonObject state = ResourceTree.object(ASSETS + "blockstates/" + id + ".json");
            List<String> models = new ArrayList<>();
            if (state.has("variants")) for (var variant : state.getAsJsonObject("variants").entrySet()) {
                collect(variant.getValue(), models);
            }
            if (state.has("multipart")) for (JsonElement part : state.getAsJsonArray("multipart")) {
                collect(part.getAsJsonObject().get("apply"), models);
            }
            for (String model : models) checkTextures(model, visited, missing);
        }
        for (String id : itemIds()) {
            JsonObject model = ResourceTree.object(ASSETS + "models/item/" + id + ".json");
            checkTextures("thaumcraft:item/" + id, visited, missing);
        }
        assertEquals(List.of(), missing);
    }

    /** Walks the parent chain of a model and checks every texture it names. */
    private static void checkTextures(String reference, Set<String> visited, List<String> missing) throws Exception {
        if (!visited.add(reference)) return;
        if (!reference.startsWith("thaumcraft:")) return;
        String path = reference.substring("thaumcraft:".length());
        if (!ResourceTree.exists(ASSETS + "models/" + path + ".json")) {
            missing.add("missing model " + reference);
            return;
        }
        JsonObject model = ResourceTree.object(ASSETS + "models/" + path + ".json");
        if (model.has("parent")) checkTextures(model.get("parent").getAsString(), visited, missing);
        if (!model.has("textures")) return;
        for (var texture : model.getAsJsonObject("textures").entrySet()) {
            String value = texture.getValue().getAsString();
            if (value.startsWith("#")) continue;
            if (!value.startsWith("thaumcraft:")) continue;
            if (!ResourceTree.exists(ASSETS + "textures/" + value.substring("thaumcraft:".length()) + ".png")) {
                missing.add(path + " texture " + value);
            }
        }
    }

    @Test void theArcaneDoorCoversEveryStateOfItsBlock() throws Exception {
        JsonObject variants = ResourceTree.object(ASSETS + "blockstates/arcane_door.json").getAsJsonObject("variants");
        assertEquals(32, variants.size(), "facing x open x half x flip = 4 x 2 x 2 x 2");
        for (var variant : variants.entrySet()) {
            assertTrue(variant.getKey().contains("flip="), variant.getKey() + " does not name the hinge");
            assertTrue(variant.getKey().contains("half="), variant.getKey() + " does not name the half");
        }
    }

    @Test void theMirrorCoversEveryStateOfItsBlock() throws Exception {
        JsonObject variants = ResourceTree.object(ASSETS + "blockstates/mirror.json").getAsJsonObject("variants");
        assertEquals(12, variants.size(), "facing x essentia = 6 x 2");
        for (var variant : variants.entrySet()) {
            assertTrue(variant.getKey().contains("essentia="), variant.getKey() + " does not name the mirror kind");
        }
    }

    @Test void everySlabOfTheTwoFamiliesKnowsItsThreeStates() throws Exception {
        // The native slabs are vanilla slabs: the bottom and the top half. The original had separate
        // single and double blocks; the 1.21.1 slab block carries the two halves and the double half.
        for (String id : List.of("cosmetic_stone_slab_arcane", "cosmetic_stone_slab_eldritch",
                "cosmetic_wood_slab_greatwood", "cosmetic_wood_slab_silverwood")) {
            JsonObject variants = ResourceTree.object(ASSETS + "blockstates/" + id + ".json").getAsJsonObject("variants");
            assertEquals(3, variants.size(), id + ": the bottom half, the top half and the double half");
            for (String type : List.of("bottom", "top", "double")) {
                assertTrue(variants.has("type=" + type), id + " misses the state " + type);
            }
        }
    }

    /**
     * The original double slabs are four blocks of the two families (ConfigBlocks:300-301); they drop the single
     * slab, two wooden ones and one stone one (BlockCosmeticWoodSlab:56-58, BlockCosmeticStoneSlab:56-58).
     */
    @Test void everyDoubleSlabHasItsOwnBlockStateModelAndLootTable() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        for (String id : List.of("cosmetic_double_slab_wood_greatwood", "cosmetic_double_slab_wood_silverwood",
                "cosmetic_double_slab_stone_arcane", "cosmetic_double_slab_stone_eldritch")) {
            assertTrue(ResourceTree.exists(ASSETS + "blockstates/" + id + ".json"), id);
            assertTrue(ResourceTree.exists(ASSETS + "models/block/" + id + ".json"), id);
            assertTrue(ResourceTree.exists(ASSETS + "models/item/" + id + ".json"), id);
            assertTrue(ResourceTree.exists("data/thaumcraft/loot_table/blocks/" + id + ".json"), id);
            assertTrue(en.has("block.thaumcraft." + id), id);
            assertTrue(ru.has("block.thaumcraft." + id), id);
            JsonObject loot = ResourceTree.object("data/thaumcraft/loot_table/blocks/" + id + ".json");
            JsonObject entry = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject();
            int count = 1;
            for (var function : entry.getAsJsonArray("functions")) {
                if (function.getAsJsonObject().get("function").getAsString().equals("minecraft:set_count")) {
                    count = function.getAsJsonObject().get("count").getAsInt();
                }
            }
            assertEquals(id.startsWith("cosmetic_double_slab_wood") ? 2 : 1, count, id);
        }
    }

    @Test void everyDeviceOfTheTwoFamiliesKnowsItsSixteenVariants() throws Exception {
        for (String id : List.of("metal_device", "wooden_device", "stone_device")) {
            JsonObject variants = ResourceTree.object(ASSETS + "blockstates/" + id + ".json").getAsJsonObject("variants");
            assertEquals(16, variants.size(), id + ": the original metadata zero to fifteen");
        }
    }

    @Test void theEldritchFamilyKnowsTheOriginalMetadataOfItsGroup() throws Exception {
        List<List<String>> expected = List.of(
                List.of("variant=0", "variant=1", "variant=2", "variant=3"),
                List.of("variant=4", "variant=5"),
                List.of(""),
                List.of("variant=7", "variant=8"),
                List.of("variant=9", "variant=10"),
                List.of(""));
        for (int index = 0; index < ELDRITCH_GROUPS.size(); index++) {
            String id = ELDRITCH_GROUPS.get(index);
            JsonObject variants = ResourceTree.object(ASSETS + "blockstates/" + id + ".json").getAsJsonObject("variants");
            assertEquals(expected.get(index).size(), variants.size(), id + " covers the original metadata of its group");
            for (String variant : expected.get(index)) assertTrue(variants.has(variant), id + " misses " + variant);
        }
    }

    @Test void theCosmeticSolidFamilyKnowsTheOriginalMetadataOfItsGroup() throws Exception {
        // BlockCosmeticSolid:213-225 and :241-253 - the hardness groups of the oracle.
        Map<String, List<String>> expected = Map.of(
                "cosmetic_solid", List.of("variant=2", "variant=3", "variant=5", "variant=9", "variant=10",
                        "variant=11", "variant=12", "variant=13", "variant=14", "variant=15"),
                "cosmetic_solid_tough", List.of("variant=0", "variant=1", "variant=8"),
                "cosmetic_solid_thaumium", List.of("variant=4", "variant=6", "variant=7"));
        for (var group : expected.entrySet()) {
            JsonObject variants = ResourceTree.object(ASSETS + "blockstates/" + group.getKey() + ".json").getAsJsonObject("variants");
            assertEquals(group.getValue().size(), variants.size(), group.getKey() + " covers the original metadata of its group");
            for (String variant : group.getValue()) assertTrue(variants.has(variant), group.getKey() + " misses " + variant);
        }
        JsonObject opaque = ResourceTree.object(ASSETS + "blockstates/cosmetic_opaque.json").getAsJsonObject("variants");
        assertEquals(2, opaque.size(), "the cosmetic opaque keeps the amber block and the amber brick");
        JsonObject warded = ResourceTree.object(ASSETS + "blockstates/cosmetic_opaque_warded.json").getAsJsonObject("variants");
        assertEquals(1, warded.size(), "the warded glass is the metadata two of its own block");
    }

    @Test void theAiryFamilyKnowsTheOriginalMetadataOfItsGroup() throws Exception {
        assertEquals(8, ResourceTree.object(ASSETS + "blockstates/airy.json").getAsJsonObject("variants").size(),
                "the airy block of the ordinary hardness group");
        assertEquals(2, ResourceTree.object(ASSETS + "blockstates/airy_node.json").getAsJsonObject("variants").size(),
                "the node and the energised node");
        assertEquals(2, ResourceTree.object(ASSETS + "blockstates/airy_hole.json").getAsJsonObject("variants").size(),
                "the magic hole and the void hole");
        assertEquals(1, ResourceTree.object(ASSETS + "blockstates/airy_unbreakable.json").getAsJsonObject("variants").size(),
                "the unbreakable airy block");
    }

    @Test void theLootContainersKnowTheirThreeRarities() throws Exception {
        for (String id : List.of("loot_urn", "loot_crate")) {
            JsonObject variants = ResourceTree.object(ASSETS + "blockstates/" + id + ".json").getAsJsonObject("variants");
            assertEquals(3, variants.size(), id + ": the original rarity zero to two");
        }
    }

    @Test void theLootContainersDropThemselvesAndTheGlowingCrustDropsTheOriginal() throws Exception {
        for (String id : List.of("loot_urn", "loot_crate")) {
            String table = ResourceTree.text("data/thaumcraft/loot_table/blocks/" + id + ".json");
            assertTrue(table.contains("thaumcraft:" + id), id + " does not drop itself");
        }
        String crust = ResourceTree.text("data/thaumcraft/loot_table/blocks/eldritch_crust.json");
        assertTrue(crust.contains("thaumcraft:eldritch_crust"), "the glowing crust does not drop itself");
        assertTrue(crust.contains("thaumcraft:knowledge_fragment"), "the glyphed stone does not drop a knowledge fragment");
    }

    @Test void theBlocksThatDropNothingHaveAnEmptyLootTable() throws Exception {
        for (String id : List.of("eldritch", "eldritch_deco", "eldritch_doorway", "eldritch_spawner", "eldritch_crystal",
                "eldritch_nothing", "eldritch_portal", "airy", "airy_node", "airy_hole", "airy_unbreakable")) {
            JsonObject table = ResourceTree.object("data/thaumcraft/loot_table/blocks/" + id + ".json");
            assertEquals(0, table.getAsJsonArray("pools").size(), id + " drops something but the original drops nothing");
        }
    }

    // ------------------------------------------------------------------ helpers

    private static void collect(JsonElement apply, List<String> models) {
        if (apply.isJsonArray()) for (JsonElement element : apply.getAsJsonArray()) collect(element, models);
        else models.add(apply.getAsJsonObject().get("model").getAsString());
    }

    private static boolean modelExists(String reference) {
        int colon = reference.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : reference.substring(0, colon);
        String path = colon < 0 ? reference : reference.substring(colon + 1);
        return !namespace.equals("thaumcraft") || ResourceTree.exists(ASSETS + "models/" + path + ".json");
    }

    /** Every block id that the two registration classes register. */
    private static List<String> blockIds() {
        List<String> ids = new ArrayList<>();
        for (Class<?> owner : List.of(ThaumcraftEldritchBlocks.class, ThaumcraftLegacyTiles.class)) {
            for (Field field : owner.getDeclaredFields()) {
                Class<?> registered = registeredType(field);
                if (registered == null || !Block.class.isAssignableFrom(registered)) continue;
                ids.add(id(field));
            }
        }
        return ids;
    }

    /** Every item id that the two registration classes register. */
    private static List<String> itemIds() {
        List<String> ids = new ArrayList<>();
        for (Class<?> owner : List.of(ThaumcraftEldritchBlocks.class, ThaumcraftLegacyTiles.class)) {
            for (Field field : owner.getDeclaredFields()) {
                Class<?> registered = registeredType(field);
                if (registered == null || !Item.class.isAssignableFrom(registered)) continue;
                ids.add(id(field));
            }
        }
        return ids;
    }

    /** The class the registry entry holds, or null when the field is not a registry entry. */
    private static Class<?> registeredType(Field field) {
        if (!RegistrySupplier.class.isAssignableFrom(field.getType())) return null;
        Type generic = field.getGenericType();
        if (!(generic instanceof ParameterizedType parameterized)) return null;
        Type[] arguments = parameterized.getActualTypeArguments();
        if (arguments.length != 1 || !(arguments[0] instanceof Class<?> raw)) return null;
        return raw;
    }

    /** The registry id of a registry entry field. */
    private static String id(Field field) {
        try {
            field.setAccessible(true);
            RegistrySupplier<?> supplier = (RegistrySupplier<?>) field.get(null);
            return supplier.getId().getPath();
        } catch (IllegalAccessException e) {
            throw new AssertionError("cannot read " + field, e);
        }
    }
}
