package thaumcraft.assets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Every registry entry of the port has to be complete: an item needs an item model and a name,
 * a block needs a blockstate, a block model, a loot table and a name, an entity needs a name and
 * a container needs a title. The ids are read from the registration classes by reflection
 * ({@link RegistryInventory}), so a registration that lands without its resources fails here
 * instead of showing up as a missing-model cube or a raw translation key in game.
 *
 * <p>The names are the ones 1.21.1 resolves: {@code item.thaumcraft.<id>},
 * {@code block.thaumcraft.<id>}, {@code entity.thaumcraft.<id>} and {@code container.<id>}. The
 * older 1.7.10 spellings ({@code item.thaumcraft.<id>.name}, {@code tile.thaumcraft.<id>.name})
 * are dead keys in 1.21.1 and never satisfy the game.</p>
 */
class RegisteredAssetCoverageTest {
    private static final String ASSETS = "assets/thaumcraft/";
    private static final String EN = ASSETS + "lang/en_us.json";
    private static final String RU = ASSETS + "lang/ru_ru.json";
    private static final String LOOT = "data/thaumcraft/loot_table/blocks/";

    /** Blocks whose drops come from code, or that never drop an item. */
    private static final Set<String> NO_LOOT_TABLE = Set.of(
            "arcane_light", "arcane_workbench", "aura_node", "flux_gas", "flux_goo", "hole", "jar", "jar_void", "mana_pod",
            "shock_trap", "table", "warded", "purifying_fluid");

    /** A container title: the key the game resolves for the title of a menu. */
    private static final Pattern CONTAINER_KEY = Pattern.compile("container\\.[A-Za-z0-9_.]+");

    @Test void theRegistryIsRead() {
        assertTrue(RegistryInventory.itemIds().size() > 200, "items read: " + RegistryInventory.itemIds().size());
        assertTrue(RegistryInventory.blockIds().size() > 100, "blocks read: " + RegistryInventory.blockIds().size());
        assertTrue(RegistryInventory.entityIds().size() > 40, "entities read: " + RegistryInventory.entityIds().size());
    }

    @Test void everyRegisteredItemHasAnItemModelAndANameInBothLanguages() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        List<String> missing = new ArrayList<>();
        for (String id : RegistryInventory.itemIds()) {
            if (!ResourceTree.exists(ASSETS + "models/item/" + id + ".json")) missing.add("item model " + id);
            // A block item is named by its block, every other item by itself.
            if (!named(en, id)) missing.add("en_us item name " + id);
            if (!named(ru, id)) missing.add("ru_ru item name " + id);
        }
        assertEquals(List.of(), missing);
    }

    /** True when the language file names the item, either as an item or as the block it places. */
    private static boolean named(JsonObject lang, String id) {
        return lang.has("item.thaumcraft." + id) || lang.has("block.thaumcraft." + id);
    }

    @Test void everyRegisteredBlockHasABlockstateItsModelsALootTableAndANameInBothLanguages() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        List<String> missing = new ArrayList<>();
        for (String id : RegistryInventory.blockIds()) {
            String blockstate = ASSETS + "blockstates/" + id + ".json";
            if (!ResourceTree.exists(blockstate)) {
                missing.add("blockstate " + id);
            } else {
                for (String model : modelsOf(blockstate)) {
                    if (!ResourceTree.exists(ASSETS + "models/" + model + ".json")) missing.add("block model " + model + " of " + id);
                }
            }
            if (!NO_LOOT_TABLE.contains(id) && !ResourceTree.exists(LOOT + id + ".json")) missing.add("loot table " + id);
            if (!en.has("block.thaumcraft." + id)) missing.add("en_us block name " + id);
            if (!ru.has("block.thaumcraft." + id)) missing.add("ru_ru block name " + id);
        }
        assertEquals(List.of(), missing);
    }

    /** Every model a blockstate names, without the namespace. */
    private static List<String> modelsOf(String blockstate) throws Exception {
        JsonObject state = ResourceTree.object(blockstate);
        List<String> models = new ArrayList<>();
        if (state.has("variants")) for (var variant : state.getAsJsonObject("variants").entrySet()) collect(variant.getValue(), models);
        if (state.has("multipart")) for (JsonElement part : state.getAsJsonArray("multipart")) collect(part.getAsJsonObject().get("apply"), models);
        List<String> paths = new ArrayList<>();
        for (String model : models) {
            String path = model.startsWith("thaumcraft:") ? model.substring("thaumcraft:".length()) : model;
            paths.add(path);
        }
        return paths;
    }

    private static void collect(JsonElement apply, List<String> models) {
        if (apply.isJsonArray()) for (JsonElement element : apply.getAsJsonArray()) collect(element, models);
        else models.add(apply.getAsJsonObject().get("model").getAsString());
    }

    @Test void everyRegisteredEntityHasANameInBothLanguages() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        List<String> missing = new ArrayList<>();
        for (String id : RegistryInventory.entityIds()) {
            if (!en.has("entity.thaumcraft." + id)) missing.add("en_us entity name " + id);
            if (!ru.has("entity.thaumcraft." + id)) missing.add("ru_ru entity name " + id);
        }
        assertEquals(List.of(), missing);
    }

    @Test void everyContainerTitleTheCodeUsesExistsInBothLanguages() throws Exception {
        JsonObject en = ResourceTree.object(EN);
        JsonObject ru = ResourceTree.object(RU);
        List<String> missing = new ArrayList<>();
        List<String> titles = containerTitles();
        assertFalse(titles.isEmpty(), "no container title was found in the compiled classes");
        for (String title : titles) {
            if (!en.has(title)) missing.add("en_us " + title);
            if (!ru.has(title)) missing.add("ru_ru " + title);
        }
        assertEquals(List.of(), missing);
    }

    /**
     * Every {@code container.*} string the compiled code uses. The constant pool of the class
     * files is read, so a container that is added later is covered without touching this test.
     */
    private static List<String> containerTitles() throws Exception {
        Set<String> titles = new TreeSet<>();
        for (String file : ResourceTree.filesOnAnyRoot("thaumcraft")) {
            if (!file.endsWith(".class")) continue;
            for (String constant : utf8Constants(file)) {
                if (CONTAINER_KEY.matcher(constant).matches()) titles.add(constant);
            }
        }
        return List.copyOf(titles);
    }

    /** The UTF-8 constants of one compiled class. */
    private static List<String> utf8Constants(String file) throws IOException {
        List<String> constants = new ArrayList<>();
        try (InputStream stream = ResourceTree.class.getClassLoader().getResourceAsStream("thaumcraft/" + file)) {
            if (stream == null) return constants;
            DataInputStream data = new DataInputStream(stream);
            if (data.readInt() != 0xCAFEBABE) return constants;
            data.readUnsignedShort(); // minor
            data.readUnsignedShort(); // major
            int count = data.readUnsignedShort();
            for (int index = 1; index < count; index++) {
                int tag = data.readUnsignedByte();
                switch (tag) {
                    case 1 -> constants.add(data.readUTF());
                    case 7, 8, 16, 19, 20 -> data.skipBytes(2);
                    case 15 -> data.skipBytes(3);
                    case 3, 4, 9, 10, 11, 12, 17, 18 -> data.skipBytes(4);
                    case 5, 6 -> {
                        data.skipBytes(8);
                        index++; // the second half of a long or a double
                    }
                    default -> throw new IOException("unknown constant pool tag " + tag + " in " + file);
                }
            }
        }
        return constants;
    }

    /** The compiled classes of the mod, so a new container title is caught. */
    @Test void theCompiledClassesAreFound() throws Exception {
        int classes = 0;
        for (String file : ResourceTree.filesOnAnyRoot("thaumcraft")) {
            if (file.endsWith(".class")) classes++;
        }
        assertTrue(classes > 500, "compiled classes found: " + classes);
    }
}
