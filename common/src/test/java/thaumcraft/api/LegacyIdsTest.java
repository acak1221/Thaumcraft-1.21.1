package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.blocks.BlockAdvancedAlchemyFurnace;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.world.EldritchTestRegistry;

/**
 * The original TC4 field names and metadata resolve to the native 1.21.1 items, blocks and states.
 *
 * <p>The unit tests have no running game, so the content of the port is not in the registries. The ids the
 * production code registers are read from the very {@code DeferredRegister}s that {@code Thaumcraft.init()}
 * registers: every one of {@code thaumcraft.common.config}. Vanilla ids are checked against the bootstrapped
 * vanilla registries, the eldritch family against the real blocks the test platform registers
 * ({@link EldritchTestRegistry}).</p>
 */
class LegacyIdsTest {
    private static final String TABLE = "data/thaumcraft/tc4/legacy_ids.json";
    private static final ResourceLocation ITEM_REGISTRY = ResourceLocation.withDefaultNamespace("item");
    private static final ResourceLocation BLOCK_REGISTRY = ResourceLocation.withDefaultNamespace("block");
    private static JsonObject table;
    private static Map<ResourceLocation, Set<ResourceLocation>> declared;

    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        try (InputStream stream = LegacyIdsTest.class.getClassLoader().getResourceAsStream(TABLE)) {
            assertNotNull(stream, TABLE + " must be on the classpath");
            table = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        declared = declaredIds();
    }

    /** Every id that a DeferredRegister of thaumcraft.common.config declares, by registry. */
    private static Map<ResourceLocation, Set<ResourceLocation>> declaredIds() throws Exception {
        Map<ResourceLocation, Set<ResourceLocation>> ids = new HashMap<>();
        List<String> failures = new ArrayList<>();
        for (String name : configClasses()) {
            Class<?> type;
            try {
                type = Class.forName(name, true, LegacyIdsTest.class.getClassLoader());
            } catch (Throwable failure) {
                failures.add(name + ": " + failure);
                continue;
            }
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !DeferredRegister.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                for (RegistrySupplier<?> entry : (DeferredRegister<?>) field.get(null)) {
                    ids.computeIfAbsent(entry.getRegistryId(), key -> new HashSet<>()).add(entry.getId());
                }
            }
        }
        assertTrue(failures.isEmpty(), "config classes that could not be initialised: " + failures);
        return ids;
    }

    private static List<String> configClasses() throws Exception {
        URL anchor = ThaumcraftContent.class.getResource("ThaumcraftContent.class");
        assertNotNull(anchor);
        List<String> names = new ArrayList<>();
        String prefix = "thaumcraft/common/config/";
        if ("jar".equals(anchor.getProtocol())) {
            String path = anchor.getPath();
            try (JarFile jar = new JarFile(Path.of(URI.create(path.substring(0, path.indexOf('!')))).toFile())) {
                jar.stream().map(entry -> entry.getName())
                        .filter(entry -> entry.startsWith(prefix) && entry.endsWith(".class") && !entry.contains("$")
                                && entry.indexOf('/', prefix.length()) < 0)
                        .forEach(entry -> names.add(entry.substring(0, entry.length() - 6).replace('/', '.')));
            }
        } else {
            try (Stream<Path> files = Files.list(Path.of(anchor.toURI()).getParent())) {
                files.map(file -> file.getFileName().toString())
                        .filter(file -> file.endsWith(".class") && !file.contains("$"))
                        .forEach(file -> names.add("thaumcraft.common.config." + file.substring(0, file.length() - 6)));
            }
        }
        names.sort(String::compareTo);
        assertTrue(names.size() > 50, "the config package must be found: " + names.size());
        return names;
    }

    private static List<JsonObject> rows() {
        List<JsonObject> rows = new ArrayList<>();
        for (JsonElement element : table.getAsJsonArray("rows")) rows.add(element.getAsJsonObject());
        return rows;
    }

    private static ResourceLocation location(JsonObject row, String key) {
        return row.has(key) && !row.get(key).isJsonNull() ? ResourceLocation.parse(row.get(key).getAsString()) : null;
    }

    /**
     * ThaumcraftLegacyTiles.register() adds these items to its item register after opening it
     * (ThaumcraftLegacyTiles.java register(): wand_pedestal, arcane_furnace, wand_pedestal_focus), so a register
     * read without a game does not list them yet. They count as registered when that is still the reason.
     */
    private static final Set<ResourceLocation> ADDED_BY_REGISTER = Set.of(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand_pedestal"),
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane_furnace"),
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand_pedestal_focus"));

    private static boolean registered(ResourceLocation registry, ResourceLocation id) {
        if ("minecraft".equals(id.getNamespace())) {
            return ITEM_REGISTRY.equals(registry) ? BuiltInRegistries.ITEM.containsKey(id) : BuiltInRegistries.BLOCK.containsKey(id);
        }
        if (ITEM_REGISTRY.equals(registry) && ADDED_BY_REGISTER.contains(id)) return true;
        return declared.getOrDefault(registry, Set.of()).contains(id);
    }

    @Test void theProductionRegistersAreRead() throws Exception {
        assertTrue(declared.get(ITEM_REGISTRY).contains(ResourceLocation.fromNamespaceAndPath("thaumcraft", "thaumium_ingot")));
        assertTrue(declared.get(BLOCK_REGISTRY).contains(ResourceLocation.fromNamespaceAndPath("thaumcraft", "jar")));
        assertTrue(declared.get(ITEM_REGISTRY).size() > 300, "items: " + declared.get(ITEM_REGISTRY).size());
        for (ResourceLocation id : ADDED_BY_REGISTER) {
            assertFalse(declared.get(ITEM_REGISTRY).contains(id), id + " is declared statically now; drop it from the list");
            assertTrue(Files.readString(Path.of("src/main/java/thaumcraft/common/config/ThaumcraftLegacyTiles.java"))
                    .contains("ITEMS.register(\"" + id.getPath() + "\""), id + " must still be added by ThaumcraftLegacyTiles.register()");
            assertNotNull(LegacyIdsTest.class.getClassLoader().getResource("assets/thaumcraft/models/item/" + id.getPath() + ".json"), id + " model");
        }
    }

    @Test void everyRowNamesARegisteredNativeItemOrBlock() {
        Set<String> missing = new TreeSet<>();
        for (JsonObject row : rows()) {
            String where = row.get("field").getAsString() + ":" + row.get("meta").getAsInt();
            boolean block = "block".equals(row.get("kind").getAsString());
            List<ResourceLocation> ids = new ArrayList<>();
            ResourceLocation id = location(row, "id");
            if (id != null) ids.add(id);
            if (row.has("ids")) row.getAsJsonArray("ids").forEach(value -> ids.add(ResourceLocation.parse(value.getAsString())));
            for (ResourceLocation value : ids) {
                if (!registered(block ? BLOCK_REGISTRY : ITEM_REGISTRY, value)) missing.add(where + " " + value);
            }
            ResourceLocation item = location(row, "item");
            if (block && item != null && !registered(ITEM_REGISTRY, item)) missing.add(where + " item " + item);
            if (id == null && !row.has("ids") && row.get("meta").getAsInt() != ItemApi.WILDCARD) {
                assertTrue(row.has("note") && !row.get("note").getAsString().isBlank(), where + " needs a reason");
            }
        }
        assertTrue(missing.isEmpty(), "rows that name no registered native object: " + missing);
    }

    @Test void everyStateNamesARealPropertyOfItsBlock() throws Exception {
        Set<String> problems = new TreeSet<>();
        Set<String> withoutBlockstate = new TreeSet<>();
        for (JsonObject row : rows()) {
            if (!row.has("state")) continue;
            ResourceLocation id = location(row, "id");
            Map<String, Set<String>> known = blockstateValues(id);
            for (Map.Entry<String, JsonElement> property : row.getAsJsonObject("state").entrySet()) {
                Set<String> values = known.get(property.getKey());
                if (values == null) withoutBlockstate.add(id + " " + property.getKey());
                else if (!values.contains(property.getValue().getAsString())) {
                    problems.add(row.get("field").getAsString() + ":" + row.get("meta").getAsInt() + " " + id + " "
                            + property.getKey() + "=" + property.getValue().getAsString());
                }
            }
        }
        assertTrue(problems.isEmpty(), "states the blockstate file does not list: " + problems);
        // Their models ignore the property, so the blockstate file cannot tell; the block classes do.
        assertEquals(Set.of("thaumcraft:advanced_alchemical_furnace part", "thaumcraft:death_fluid level"), withoutBlockstate);
        assertTrue(BlockAdvancedAlchemyFurnace.PART.getPossibleValues().containsAll(List.of(0, 1, 2, 3, 4)));
        assertTrue(LiquidBlock.LEVEL.getPossibleValues().contains(0));
    }

    /** property -> values of a native blockstate file: the variant keys and the multipart conditions. */
    private static Map<String, Set<String>> blockstateValues(ResourceLocation id) throws Exception {
        String path = "assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json";
        Map<String, Set<String>> values = new HashMap<>();
        try (InputStream stream = LegacyIdsTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.has("variants")) {
                for (String key : json.getAsJsonObject("variants").keySet()) {
                    for (String pair : key.split(",")) {
                        if (pair.isEmpty()) continue;
                        String[] parts = pair.split("=", 2);
                        values.computeIfAbsent(parts[0], k -> new HashSet<>()).add(parts[1]);
                    }
                }
            }
            if (json.has("multipart")) {
                for (JsonElement part : json.getAsJsonArray("multipart")) {
                    JsonObject when = part.getAsJsonObject().getAsJsonObject("when");
                    if (when == null) continue;
                    List<JsonObject> clauses = new ArrayList<>();
                    if (when.has("OR")) when.getAsJsonArray("OR").forEach(clause -> clauses.add(clause.getAsJsonObject()));
                    else clauses.add(when);
                    for (JsonObject clause : clauses) {
                        for (Map.Entry<String, JsonElement> entry : clause.entrySet()) {
                            for (String value : entry.getValue().getAsString().split("\\|")) {
                                values.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).add(value);
                            }
                        }
                    }
                }
            }
        }
        return values;
    }

    @Test void theApiAnswersEveryRowOfTheTable() {
        for (JsonObject row : rows()) {
            String field = row.get("field").getAsString();
            int meta = row.get("meta").getAsInt();
            boolean block = "block".equals(row.get("kind").getAsString());
            assertTrue(ItemApi.isLegacyName(field), field);
            ResourceLocation itemForm = block ? location(row, "item") : location(row, "id");
            assertEquals(itemForm, ItemApi.legacyItemId(field, meta), field + ":" + meta);
            assertEquals(block ? location(row, "id") : null, ItemApi.legacyBlockId(field, meta), field + ":" + meta);
            assertDoesNotThrow(() -> ItemApi.getItem(field, meta), field + ":" + meta);
            assertDoesNotThrow(() -> ItemApi.getBlock(field, meta), field + ":" + meta);
            assertDoesNotThrow(() -> ItemApi.legacyBlockState(field, meta), field + ":" + meta);
        }
        assertEquals(table.getAsJsonObject("counts").get("fields").getAsInt(), ItemApi.legacyNames().size());
    }

    @Test void originalItemNamesResolveToTheirNativeIds() {
        assertEquals(id("thaumium_ingot"), ItemApi.legacyItemId("itemResource", 2));
        assertEquals(id("alumentum"), ItemApi.legacyItemId("itemResource", 0));
        assertEquals(id("salis_mundus"), ItemApi.legacyItemId("itemResource", 14));
        assertEquals(id("balance_shard"), ItemApi.legacyItemId("itemShard", 6));
        assertEquals(id("air_shard"), ItemApi.legacyItemId("itemShard", 0));
        assertEquals(id("phial_filled"), ItemApi.legacyItemId("itemEssence", 1));
        assertEquals(id("golem_thaumium"), ItemApi.legacyItemId("itemGolemPlacer", 7));
        assertEquals(id("staff_rod_primal"), ItemApi.legacyItemId("itemWandRod", 100));
        assertEquals(id("discount_ring_perditio"), ItemApi.legacyItemId("itemBaubleBlanks", 8));
        assertEquals(ResourceLocation.withDefaultNamespace("iron_nugget"), ItemApi.legacyItemId("itemNugget", 0));
        assertEquals(id("cluster_cinnabar"), ItemApi.legacyItemId("itemNugget", 21));
        // A field without subtypes answers every metadata: the original metadata was only the damage.
        assertEquals(id("thaumium_sword"), ItemApi.legacyItemId("itemSwordThaumium", 120));
        assertNull(ItemApi.legacyBlockId("itemResource", 2));
    }

    @Test void originalBlockNamesResolveToTheirNativeBlocksStatesAndItems() {
        assertEquals(id("jar"), ItemApi.legacyBlockId("blockJar", 0));
        assertEquals(id("jar"), ItemApi.legacyItemId("blockJar", 0));
        assertEquals(id("jar_void"), ItemApi.legacyBlockId("blockJar", 3));
        assertEquals(id("thaumium_block"), ItemApi.legacyBlockId("blockCosmeticSolid", 4));
        assertEquals(id("tallow_block"), ItemApi.legacyBlockId("blockCosmeticSolid", 5));
        assertEquals(id("arcane_stone"), ItemApi.legacyBlockId("blockCosmeticSolid", 6));
        assertEquals(id("arcane_stone_bricks"), ItemApi.legacyBlockId("blockCosmeticSolid", 7));
        assertEquals(id("cosmetic_solid_tough"), ItemApi.legacyBlockId("blockCosmeticSolid", 1));
        assertEquals(id("obsidian_tile"), ItemApi.legacyItemId("blockCosmeticSolid", 1));
        assertEquals(id("cosmetic_solid"), ItemApi.legacyBlockId("blockCosmeticSolid", 15));
        assertEquals(id("golem_fetter"), ItemApi.legacyBlockId("blockCosmeticSolid", 10));
        assertEquals(id("research_table"), ItemApi.legacyItemId("blockTable", 2));
        assertEquals(id("taint_grass"), ItemApi.legacyItemId("blockTaintFibres", 1));
        assertEquals(id("taint_fibres"), ItemApi.legacyBlockId("blockTaintFibres", 1));
        assertEquals(id("greatwood_slab"), ItemApi.legacyBlockId("blockSlabWood", 0));
        // A block without an item answers no item form.
        assertEquals(id("energized_node"), ItemApi.legacyBlockId("blockAiry", 5));
        assertNull(ItemApi.legacyItemId("blockAiry", 5));
        assertNull(ItemApi.getBlock("blockAiry", 5));
    }

    @Test void theRegisteredEldritchFamilyAnswersRealStates() {
        Assumptions.assumeFalse(EldritchTestRegistry.family().isEmpty(), "the eldritch family is not registered");
        int[] groups = {0, 0, 0, 0, 1, 1, 2, 3, 3, 4, 4};
        for (int meta = 0; meta <= 10; meta++) {
            BlockState state = ItemApi.legacyBlockState("blockEldritch", meta);
            assertNotNull(state, "blockEldritch:" + meta);
            Block expected = EldritchTestRegistry.family().get(groups[meta]);
            assertSame(expected, state.getBlock(), "blockEldritch:" + meta);
            Property<?> variant = state.getBlock().getStateDefinition().getProperty("variant");
            if (variant != null) assertEquals(meta, state.getValue(variant), "blockEldritch:" + meta);
            assertSame(state, BlocksTC.state("blockEldritch", meta));
            assertSame(expected, BlocksTC.get("blockEldritch", meta));
        }
        assertSame(EldritchTestRegistry.family().get(0), BlocksTC.get("blockEldritch"));
    }

    @Test void aVanillaVariantGivesARealStack() {
        ItemStack nugget = ItemApi.getItem("itemNugget", 0);
        assertNotNull(nugget);
        assertSame(Items.IRON_NUGGET, nugget.getItem());
        assertEquals(1, nugget.getCount());
        assertNull(nugget.get(DataComponents.BLOCK_STATE));
        assertSame(Items.IRON_NUGGET, ItemsTC.get("itemNugget"));
        assertSame(Items.IRON_NUGGET, ItemsTC.get("itemNugget", 0));
        assertEquals(ResourceLocation.withDefaultNamespace("iron_nugget"), ItemsTC.id("itemNugget"));
    }

    @Test void wildcardsFollowTheOriginalOreDictionaryValue() {
        assertEquals(7, ItemApi.legacyItemIds("itemShard", ItemApi.WILDCARD).size());
        assertNull(ItemApi.legacyItemId("itemShard", ItemApi.WILDCARD));
        assertNull(ItemApi.getItem("itemShard", ItemApi.WILDCARD));
        assertEquals(id("inkwell"), ItemApi.legacyItemId("itemInkwell", ItemApi.WILDCARD));
        assertEquals(List.of(id("inkwell")), ItemApi.legacyItemIds("itemInkwell", ItemApi.WILDCARD));
        assertEquals(id("advanced_alchemical_furnace"), ItemApi.legacyBlockId("blockAlchemyFurnace", ItemApi.WILDCARD));
        assertEquals(ResourceLocation.withDefaultNamespace("iron_nugget"), ItemApi.legacyItemIds("itemNugget", ItemApi.WILDCARD).get(0));
        for (String field : ItemApi.legacyNames()) {
            assertTrue(ItemApi.legacyMetadata(field).contains(ItemApi.WILDCARD), field + " needs a wildcard row");
        }
    }

    @Test void unknownNamesAndMetadataReturnNullAndNeverThrow() {
        assertNull(ItemApi.getItem("itemNotAField", 0));
        assertNull(ItemApi.getBlock("blockNotAField", 0));
        assertNull(ItemApi.getItem("Not A Name", 0));
        assertNull(ItemApi.getItem("itemResource", 99));
        assertNull(ItemApi.getItem("itemResource", -1));
        // ConfigItems names are items and ConfigBlocks names are blocks, as in the original reflection.
        assertNull(ItemApi.getItem("blockJar", 0));
        assertNull(ItemApi.getBlock("itemResource", 2));
        assertNull(ItemApi.legacyItemId("itemNotAField", 0));
        assertNull(ItemApi.legacyItemId(null, 0));
        assertNull(ItemApi.legacyBlockState("itemNotAField", 0));
        assertTrue(ItemApi.legacyItemIds("itemNotAField", ItemApi.WILDCARD).isEmpty());
        assertFalse(ItemApi.isLegacyName("thaumium_ingot"));
        assertFalse(ItemApi.isLegacyName(null));
        // Declared but never assigned in 4.2.3.5: no counterpart, but a known name.
        assertTrue(ItemApi.isLegacyName("itemPhotoPlate"));
        assertNull(ItemApi.legacyItemId("itemPhotoPlate", 0));
        assertNull(ItemApi.getItem("itemPhotoPlate", 0));
        assertNull(ItemsTC.get("itemPhotoPlate"));
        assertNull(ItemsTC.id("itemPhotoPlate"));
        assertNull(BlocksTC.id("itemResource"));
    }

    @Test void nativeNamesKeepResolvingAsBefore() {
        assertEquals(id("thaumium_ingot"), ItemsTC.id("thaumium_ingot"));
        assertEquals(id("thaumium_ingot"), ItemsTC.id("thaumcraft:thaumium_ingot"));
        assertEquals(id("jar"), BlocksTC.id("jar"));
        assertSame(Items.STONE, ItemApi.getItem("minecraft:stone", 0).getItem());
        assertSame(Items.STONE, ItemsTC.get("minecraft:stone", 5));
        assertEquals(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), BlocksTC.state("minecraft:stone", 0));
        assertEquals(id("thaumometer"), ItemsTC.id("itemThaumometer"));
        assertEquals(id("jar"), ItemsTC.id("blockJar"));
        assertEquals(id("jar"), BlocksTC.id("blockJar"));
        assertEquals(id("cosmetic_solid_tough"), BlocksTC.id("blockCosmeticSolid"));
        assertTrue(ItemsTC.isLegacyName("itemResource"));
        assertTrue(BlocksTC.isLegacyName("blockJar"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
    }
}
