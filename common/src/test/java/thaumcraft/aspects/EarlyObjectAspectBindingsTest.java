package thaumcraft.aspects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.research.EarlyObjectAspectBindings;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EarlyObjectAspectBindingsTest {
    /**
     * Original rows restored from the oracle after the ALL90 snapshot: the vanilla
     * object tags ConfigAspects176-299 and the milk bucket, the three loot bags and
     * the alchemy furnace. Each of them is checked on its own below.
     */
    private static final java.util.Set<Integer> RESTORED_ORIGINAL_ROWS = java.util.Set.of(
            176, 179, 182, 183, 187, 189, 190, 193, 196, 198, 199, 200, 201, 202, 204, 205,
            211, 212, 213, 223, 226, 227, 228, 229, 232, 233, 234, 235, 240, 241, 242, 245,
            253, 257, 259, 261, 263, 266, 269, 270, 271, 272, 273, 287, 288, 289, 290,
            299, 317, 407, 408, 409, 427,
            188, 274, 275, 276, 277, 279, 280, 281, 282, 283, 284, 285, 294, 300, 301, 307,
            309, 310, 313, 319, 320, 322, 323, 324, 325, 326, 327, 328, 329, 330, 334, 335,
            344, 345, 346);

    @Test
    void originalFuelGlowstoneAndMobDropsReplaceDivergentLegacyValues() {
        var direct = EarlyObjectAspectBindings.direct();
        assertAspects(direct, "minecraft:coal", new AspectList().add(Aspect.ENERGY, 2).add(Aspect.FIRE, 2));
        assertAspects(direct, "minecraft:charcoal", new AspectList().add(Aspect.ENERGY, 2).add(Aspect.FIRE, 2));
        assertAspects(direct, "minecraft:glowstone_dust", new AspectList().add(Aspect.SENSES, 1).add(Aspect.LIGHT, 2));
        assertAspects(direct, "minecraft:glowstone", new AspectList().add(Aspect.SENSES, 3).add(Aspect.LIGHT, 10));
        assertAspects(direct, "minecraft:redstone", new AspectList().add(Aspect.ENERGY, 2).add(Aspect.MECHANISM, 1));
        assertAspects(direct, "minecraft:gunpowder", new AspectList().add(Aspect.FIRE, 4).add(Aspect.ENTROPY, 4));
        assertAspects(direct, "minecraft:bone", new AspectList().add(Aspect.DEATH, 2).add(Aspect.FLESH, 1));
        assertAspects(direct, "minecraft:rotten_flesh", new AspectList().add(Aspect.MAN, 1).add(Aspect.FLESH, 2));
        assertAspects(direct, "minecraft:blaze_rod", new AspectList().add(Aspect.FIRE, 4).add(Aspect.MAGIC, 2));
        assertAspects(direct, "minecraft:gold_nugget", new AspectList().add(Aspect.METAL, 1));
        assertAspects(direct, "minecraft:iron_nugget", new AspectList().add(Aspect.METAL, 1));
        assertAspects(direct, "minecraft:wheat", new AspectList().add(Aspect.CROP, 2).add(Aspect.HUNGER, 1));
    }

    @Test
    void onlySixOriginalPrimalShardsAndLiteralNativeResourceBindingsAreSupplied() {
        var direct = EarlyObjectAspectBindings.direct();
        String[] names = {"air", "fire", "water", "earth", "order", "entropy"};
        Aspect[] primals = {Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY};
        for (int i = 0; i < names.length; i++) {
            assertAspects(direct, "thaumcraft:" + names[i] + "_shard",
                    new AspectList().add(Aspect.MAGIC, 1).add(primals[i], 2).add(Aspect.CRYSTAL, 1));
        }
        assertAspects(direct, "thaumcraft:quick_silver", new AspectList().add(Aspect.METAL, 3).add(Aspect.POISON, 1).add(Aspect.EXCHANGE, 2));
        assertAspects(direct, "thaumcraft:quicksilver_drop", new AspectList().add(Aspect.METAL, 1));
        assertAspects(direct, "thaumcraft:zombie_brain", new AspectList().add(Aspect.FLESH, 2).add(Aspect.MIND, 4).add(Aspect.UNDEAD, 2));
        assertAspects(direct, "thaumcraft:amber_gem", new AspectList().add(Aspect.TRAP, 2).add(Aspect.CRYSTAL, 2));
        assertAspects(direct, "thaumcraft:knowledge_fragment", new AspectList().add(Aspect.MIND, 8));
        assertAspects(direct, "thaumcraft:tainted_goo", new AspectList().add(Aspect.TAINT, 3).add(Aspect.SLIME, 1));
        assertAspects(direct, "thaumcraft:taint_tendril", new AspectList().add(Aspect.TAINT, 2).add(Aspect.GREED, 1).add(Aspect.HUNGER, 1));
        assertAspects(direct, "thaumcraft:gold_coin", new AspectList().add(Aspect.GREED, 1));
        assertAspects(direct, "thaumcraft:earth_infused_stone", new AspectList().add(Aspect.EARTH, 4).add(Aspect.CRYSTAL, 2));
        assertAspects(direct, "thaumcraft:greatwood_log", new AspectList().add(Aspect.TREE, 3).add(Aspect.MAGIC, 1));
        assertAspects(direct, "thaumcraft:silverwood_log", new AspectList().add(Aspect.TREE, 3).add(Aspect.MAGIC, 1).add(Aspect.ORDER, 1));
        assertAspects(direct, "thaumcraft:silverwood_sapling", new AspectList().add(Aspect.PLANT, 2).add(Aspect.TREE, 1).add(Aspect.MAGIC, 1));
        assertAspects(direct, "thaumcraft:shimmerleaf", new AspectList().add(Aspect.PLANT, 2).add(Aspect.EXCHANGE, 2).add(Aspect.MAGIC, 2));
        assertAspects(direct, "thaumcraft:cinderpearl", new AspectList().add(Aspect.PLANT, 2).add(Aspect.FIRE, 2).add(Aspect.MAGIC, 2));
        assertAspects(direct, "thaumcraft:greatwood_planks", new AspectList().add(Aspect.TREE, 1));
        assertAspects(direct, "thaumcraft:arcane_stone", new AspectList().add(Aspect.EARTH, 1).add(Aspect.MAGIC, 1));
        assertAspects(direct, "thaumcraft:arcane_stone_bricks", new AspectList().add(Aspect.EARTH, 1).add(Aspect.MAGIC, 1));
        assertAspects(direct, "thaumcraft:tallow_candle", new AspectList().add(Aspect.LIGHT, 2).add(Aspect.FLESH, 1).add(Aspect.MAGIC, 1));
    }

    @Test
    void explicitEmptyFilledPhialBaseAndOriginalDirectTorchAreNotRecipeFallbacks() {
        var direct = EarlyObjectAspectBindings.direct();
        assertAspects(direct, "thaumcraft:phial_empty", new AspectList().add(Aspect.VOID, 1));
        assertAspects(direct, "thaumcraft:phial_filled", new AspectList());
        assertAspects(direct, "minecraft:torch", new AspectList().add(Aspect.LIGHT, 1));
        assertAspects(direct, "minecraft:crafting_table", new AspectList().add(Aspect.CRAFT, 4));
        assertFalse(EarlyObjectAspectBindings.complex().containsKey(id("minecraft:torch")));
        assertFalse(EarlyObjectAspectBindings.complex().containsKey(id("minecraft:crafting_table")));
    }

    @Test
    void complexBindingsAreOnlyRawAdditionsWithoutDirectOverlapOrInventedModernBranches() {
        var direct = EarlyObjectAspectBindings.direct();
        var complex = EarlyObjectAspectBindings.complex();
        assertTrue(Collections.disjoint(direct.keySet(), complex.keySet()));
        assertAspects(complex, "minecraft:paper", new AspectList().add(Aspect.MIND, 1));
        assertAspects(complex, "minecraft:book", new AspectList().add(Aspect.MIND, 3));
        assertAspects(complex, "minecraft:glass_bottle", new AspectList().add(Aspect.VOID, 1));
        assertAspects(complex, "minecraft:bowl", new AspectList().add(Aspect.VOID, 1));
        assertAspects(complex, "minecraft:flint_and_steel", new AspectList().add(Aspect.FIRE, 4));
        assertAspects(complex, "minecraft:piston", new AspectList().add(Aspect.MECHANISM, 2).add(Aspect.MOTION, 4));
        assertAspects(complex, "minecraft:oak_trapdoor", new AspectList().add(Aspect.MOTION, 1));
        assertAspects(complex, "minecraft:furnace", new AspectList().add(Aspect.FIRE, 2));
        assertAspects(complex, "thaumcraft:arcane_ear", new AspectList().add(Aspect.SENSES, 4));
        assertEquals(32767, EarlyObjectAspectBindings.metadata(id("thaumcraft:goggles_revealing")));
        assertEquals(0, EarlyObjectAspectBindings.metadata(id("thaumcraft:arcane_ear")));
        for (String name : new String[]{"minecraft:blaze_powder", "minecraft:raw_iron", "minecraft:raw_gold",
                "minecraft:copper_ingot", "minecraft:netherite_ingot", "minecraft:deepslate_iron_ore",
                "minecraft:crimson_planks", "thaumcraft:balance_shard", "thaumcraft:salis_mundus",
                "thaumcraft:nitor", "thaumcraft:alumentum", "thaumcraft:crystal_air", "unknown:future_item",
                "minecraft:sticky_piston", "minecraft:spruce_trapdoor", "thaumcraft:hungry_chest"}) {
            assertFalse(direct.containsKey(id(name)), name);
            assertFalse(complex.containsKey(id(name)), name);
        }
    }

    @Test
    void mapsAreStructurallyImmutableAndEveryValueIsAnIndependentCopy() {
        var direct = EarlyObjectAspectBindings.direct();
        var complex = EarlyObjectAspectBindings.complex();
        assertThrows(UnsupportedOperationException.class, () -> direct.clear());
        assertThrows(UnsupportedOperationException.class, () -> complex.clear());
        direct.get(id("minecraft:coal")).add(Aspect.ENERGY, 1000);
        assertEquals(2, direct.get(id("minecraft:charcoal")).getAmount(Aspect.ENERGY));
        assertEquals(2, EarlyObjectAspectBindings.direct().get(id("minecraft:coal")).getAmount(Aspect.ENERGY));
        complex.get(id("minecraft:book")).add(Aspect.MIND, 1000);
        assertEquals(3, EarlyObjectAspectBindings.complex().get(id("minecraft:book")).getAmount(Aspect.MIND));
        direct.get(id("thaumcraft:phial_filled")).add(Aspect.VOID, 1000);
        assertEquals(0, EarlyObjectAspectBindings.direct().get(id("thaumcraft:phial_filled")).size());
        complex.get(id("minecraft:piston")).add(Aspect.MOTION, 1000);
        assertAspects(EarlyObjectAspectBindings.complex(), "minecraft:piston",
                new AspectList().add(Aspect.MECHANISM, 2).add(Aspect.MOTION, 4));
    }

    @Test
    void pistonTrapdoorAndWheatAdditionsLeaveEveryPreviousBindingUnchanged() throws Exception {
        var direct = EarlyObjectAspectBindings.direct();
        var complex = EarlyObjectAspectBindings.complex();
        assertEquals(209, direct.size(), "previous direct item bindings plus actual Taint states, Vishroom, Crimson Rites, the copper nugget, the native copper cluster and the original vanilla/content rows restored from the oracle");
        assertEquals(33, complex.size(), "nine existing complex bindings, the actual native Arcane Ear and the original complex vanilla rows of the oracle");
        assertAspects(direct, "thaumcraft:focus_pech", new AspectList().add(Aspect.MAGIC, 5).add(Aspect.POISON, 5)
                .add(Aspect.ENTROPY, 5).add(Aspect.ELDRITCH, 5).add(Aspect.WEAPON, 5));
        assertEquals(0, EarlyObjectAspectBindings.metadata(id("thaumcraft:focus_pech")), "original one-argument ItemStack(Item) selects exact metadata0, not wildcard");
        assertFalse(complex.containsKey(id("thaumcraft:focus_pech")), "the actual focus has an original complete direct tag, not a raw recipe addition");
        assertFalse(direct.containsKey(id("minecraft:piston")), "a raw addition must not suppress the recipe base");
        assertEquals(List.of(Aspect.MECHANISM, Aspect.MOTION), new ArrayList<>(complex.get(id("minecraft:piston")).aspects.keySet()));
        var stream = getClass().getClassLoader().getResourceAsStream("data/thaumcraft/tc4/early_object_aspects.json");
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var rows = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("bindings");
            assertEquals(198, rows.size());
            var previous = new JsonArray();
            int added = 0;
            int trapdoors = 0;
            int wheat = 0;
            int ears = 0;
            int pechFoci = 0;
            int crimsonRites = 0;
            int taintAndVishroom = 0;
            int restoredOriginalRows = 0;
            Map<Integer, JsonObject> clusters = new java.util.TreeMap<>();
            Map<Integer, JsonObject> copper = new java.util.TreeMap<>();
            for (int index = 0; index < rows.size(); index++) {
                var row = rows.get(index).getAsJsonObject();
                int sourceLine = row.get("original_source_line").getAsInt();
                if (sourceLine >= 381 && sourceLine <= 387 || sourceLine == 397) {
                    ++taintAndVishroom;
                    assertEquals("direct", row.get("registration").getAsString());
                    // These newly activated original rows do not enter the unchanged ALL90 digest.
                    String nativeId = switch (sourceLine) {
                        case 381 -> "taint_crust"; case 382 -> "taint_soil";
                        case 383 -> "taint_fibres"; case 384 -> "taint_grass";
                        case 385 -> "taint_plant"; case 386 -> "taint_spore_stalk";
                        case 387 -> "taint_spore_stalk_mature"; default -> "vishroom";
                    };
                    assertEquals(JsonParser.parseString("[\"thaumcraft:" + nativeId + "\"]"), row.get("modern_ids"));
                    AspectList expected = switch (sourceLine) {
                        case 381 -> new AspectList().add(Aspect.TREE, 1).add(Aspect.TAINT, 3);
                        case 382 -> new AspectList().add(Aspect.EARTH, 1).add(Aspect.TAINT, 3);
                        case 383 -> new AspectList().add(Aspect.LIFE, 1).add(Aspect.TAINT, 2);
                        case 384, 385 -> new AspectList().add(Aspect.PLANT, 1).add(Aspect.TAINT, 1);
                        case 386, 387 -> new AspectList().add(Aspect.BEAST, 1).add(Aspect.PLANT, 1).add(Aspect.TAINT, 2);
                        default -> new AspectList().add(Aspect.PLANT, 2).add(Aspect.POISON, 1).add(Aspect.MAGIC, 2);
                    };
                    assertAspects(direct, "thaumcraft:" + nativeId, expected);
                    continue;
                }
                if (sourceLine == 436) {
                    ++pechFoci;
                    assertEquals("direct", row.get("registration").getAsString());
                    assertEquals("ConfigItems.itemFocusPech", row.get("original_target").getAsString());
                    assertEquals(0, row.get("original_metadata").getAsInt());
                    assertEquals(JsonParser.parseString("[\"thaumcraft:focus_pech\"]"), row.get("modern_ids"));
                    assertEquals(JsonParser.parseString("{\"praecantatio\":5,\"venenum\":5,\"perditio\":5,\"alienis\":5,\"telum\":5}"), row.get("aspects"));
                    assertTrue(row.get("identifier_evidence").getAsString().contains("ConfigItems361-362"));
                    continue;
                }
                if (sourceLine == 434) {
                    ++ears;
                    assertEquals("complex", row.get("registration").getAsString());
                    assertEquals("ConfigBlocks.blockWoodenDevice", row.get("original_target").getAsString());
                    assertEquals(1, row.get("original_metadata").getAsInt());
                    assertEquals(JsonParser.parseString("[\"thaumcraft:arcane_ear\"]"), row.get("modern_ids"));
                    assertEquals(JsonParser.parseString("{\"sensus\":4}"), row.get("aspects"));
                    continue;
                }
                if (sourceLine == 433) {
                    assertEquals("wildcard:32767", row.get("original_metadata").getAsString(), "original ConfigAspects433 explicitly registers a wildcard durable goggles stack");
                    // Preserve the historical ALL90 digest with its old metadata typo normalized only for that comparison.
                    // The live corrected value is asserted independently above; all remaining row fields stay pinned.
                    var historical = row.deepCopy(); historical.addProperty("original_metadata", 0); previous.add(historical);
                    continue;
                }
                if (sourceLine == 449) {
                    ++crimsonRites;
                    assertEquals("direct", row.get("registration").getAsString());
                    assertEquals("ConfigItems.itemEldritchObject", row.get("original_target").getAsString());
                    assertEquals(1, row.get("original_metadata").getAsInt());
                    assertEquals(JsonParser.parseString("[\"thaumcraft:crimson_rites\"]"), row.get("modern_ids"));
                    assertAspects(direct, "thaumcraft:crimson_rites", new AspectList().add(Aspect.MIND, 5)
                            .add(Aspect.MAGIC, 3).add(Aspect.ELDRITCH, 3).add(Aspect.SOUL, 3));
                    continue;
                }
                if (sourceLine == 454) {
                    assertEquals("ConfigItems.itemEldritchObject", row.get("original_target").getAsString());
                    assertEquals(3, row.get("original_metadata").getAsInt());
                    assertEquals(JsonParser.parseString("[\"thaumcraft:primordial_pearl\"]"), row.get("modern_ids"));
                    assertAspects(direct, "thaumcraft:primordial_pearl", new AspectList().add(Aspect.AIR, 16)
                            .add(Aspect.EARTH, 16).add(Aspect.FIRE, 16).add(Aspect.WATER, 16).add(Aspect.ORDER, 16).add(Aspect.ENTROPY, 16));
                    continue;
                }
                if (sourceLine >= 360 && sourceLine <= 362) {
                    // Native iron, gold and cinnabar clusters (ConfigAspects:360-362), added after the ALL90 snapshot; checked on their own below.
                    clusters.put(sourceLine, row);
                    continue;
                }
                if (sourceLine == 149 || sourceLine == 153) {
                    // The copper nugget and the native copper cluster (ConfigAspects:149 and :153); copper is vanilla in
                    // 1.21.1, so the original conditional branch is carried. Checked on their own, outside the ALL90 digest.
                    copper.put(sourceLine, row);
                    continue;
                }
                if (RESTORED_ORIGINAL_ROWS.contains(sourceLine)) {
                    // These restored original rows do not enter the unchanged ALL90 digest.
                    ++restoredOriginalRows;
                    assertTrue(List.of("direct", "complex").contains(row.get("registration").getAsString()),
                            "original line " + sourceLine);
                    assertTrue(row.getAsJsonArray("modern_ids").size() >= 1, "original line " + sourceLine);
                    assertTrue(row.get("identifier_evidence").getAsString().toLowerCase(java.util.Locale.ROOT).contains("original")
                            || row.get("identifier_evidence").getAsString().contains("1.21.1"),
                            "original line " + sourceLine);
                    continue;
                }
                if (row.get("original_source_line").getAsInt() == 224) {
                    ++wheat;
                    assertEquals("direct", row.get("registration").getAsString());
                    assertEquals("Items.field_151015_O", row.get("original_target").getAsString());
                    assertEquals(0, row.get("original_metadata").getAsInt());
                    assertEquals(JsonParser.parseString("[\"minecraft:wheat\"]"), row.get("modern_ids"));
                    assertEquals(JsonParser.parseString("{\"messis\":2,\"fames\":1}"), row.get("aspects"));
                    continue;
                }
                if (row.get("original_source_line").getAsInt() == 336) {
                    ++trapdoors;
                    assertEquals("complex", row.get("registration").getAsString());
                    assertEquals("Blocks.field_150415_aT", row.get("original_target").getAsString());
                    assertEquals("wildcard:32767", row.get("original_metadata").getAsString());
                    assertEquals(JsonParser.parseString("[\"minecraft:oak_trapdoor\"]"), row.get("modern_ids"));
                    assertEquals(JsonParser.parseString("{\"motus\":1}"), row.get("aspects"));
                    continue;
                }
                if (row.get("original_source_line").getAsInt() != 332) { previous.add(row); continue; }
                ++added;
                assertEquals(330, rows.get(index - 1).getAsJsonObject().get("original_source_line").getAsInt());
                assertEquals(334, rows.get(index + 1).getAsJsonObject().get("original_source_line").getAsInt());
                assertEquals("complex", row.get("registration").getAsString());
                assertEquals("Blocks.field_150331_J", row.get("original_target").getAsString());
                assertEquals("wildcard:32767", row.get("original_metadata").getAsString());
                assertEquals(JsonParser.parseString("[\"minecraft:piston\"]"), row.get("modern_ids"));
                assertEquals(JsonParser.parseString("{\"machina\":2,\"motus\":4}"), row.get("aspects"));
                assertTrue(row.get("identifier_evidence").getAsString().contains("ajn/J"));
            }
            assertEquals(1, added);
            assertEquals(1, trapdoors);
            assertEquals(1, wheat);
            assertEquals(1, ears);
            assertEquals(1, pechFoci);
            assertEquals(1, crimsonRites);
            assertEquals(8, taintAndVishroom);
            assertEquals(88, restoredOriginalRows);
            assertEquals(3, clusters.size());
            assertCluster(clusters.get(360), 16, "thaumcraft:cluster_iron", "{\"ordo\":1,\"metallum\":6,\"terra\":1}");
            assertCluster(clusters.get(361), 31, "thaumcraft:cluster_gold", "{\"ordo\":1,\"metallum\":4,\"terra\":1,\"lucrum\":2}");
            assertCluster(clusters.get(362), 21, "thaumcraft:cluster_cinnabar", "{\"ordo\":1,\"metallum\":4,\"terra\":1,\"permutatio\":4,\"venenum\":2}");
            // The copper nugget and the native copper cluster: copper is vanilla in 1.21.1, so the original
            // conditional branch ConfigAspects148-153 is carried (ConfigItems:277 and ConfigAspects:149).
            assertEquals(2, copper.size());
            JsonObject copperNugget = copper.get(149);
            assertEquals("direct", copperNugget.get("registration").getAsString());
            assertEquals("ore_dictionary:nuggetCopper", copperNugget.get("original_target").getAsString());
            assertEquals(JsonParser.parseString("[\"thaumcraft:copper_nugget\"]"), copperNugget.get("modern_ids"));
            assertEquals(JsonParser.parseString("{\"metallum\":1}"), copperNugget.get("aspects"));
            assertAspects(direct, "thaumcraft:copper_nugget", new AspectList().add(Aspect.METAL, 1));
            assertCluster(copper.get(153), 17, "thaumcraft:cluster_copper", "{\"ordo\":1,\"metallum\":5,\"terra\":1,\"permutatio\":2}");
            assertAspects(direct, "thaumcraft:cluster_copper", new AspectList().add(Aspect.ORDER, 1)
                    .add(Aspect.METAL, 5).add(Aspect.EARTH, 1).add(Aspect.EXCHANGE, 2));
            // Pre-change compact JSON of ALL90 rows, not merely family counts.
            // This pins their exact targets, metadata, values, source anchors and order.
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(previous.toString().getBytes(StandardCharsets.UTF_8)));
            assertEquals("ba57886bc6e16a03fce05b37ecf8cd9ea4ba8207b32d2d0f6c3f20200f6cb1b6", digest);
        }
    }

    /**
     * Every vanilla identifier of the corpus must exist in the 1.21.1 item registry.
     * A renamed or missing identifier would silently drop the original object tag.
     */
    @Test
    void everyVanillaCorpusIdentifierExistsInTheTargetRegistry() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var stream = getClass().getClassLoader().getResourceAsStream("data/thaumcraft/tc4/early_object_aspects.json");
        assertNotNull(stream);
        var ids = new java.util.LinkedHashSet<String>();
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var rows = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("bindings");
            for (var element : rows) {
                for (var id : element.getAsJsonObject().getAsJsonArray("modern_ids")) {
                    if (id.getAsString().startsWith("minecraft:")) ids.add(id.getAsString());
                }
            }
        }
        assertTrue(ids.size() > 80, "the corpus carries the original vanilla object tags");
        for (String name : ids) {
            assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(name)), name);
        }
    }

    /**
     * The original rows restored from the oracle after the ALL90 snapshot keep the
     * literal tag of their original ConfigAspects line.
     */
    @Test
    void restoredOriginalRowsCarryTheOriginalLiteralTags() {
        record Row(int line, String kind, List<String> ids, AspectList aspects) {}
        List<Row> rows = List.of(
                new Row(176, "direct", List.of("minecraft:bedrock"), new AspectList().add(Aspect.VOID, 16).add(Aspect.ENTROPY, 16).add(Aspect.EARTH, 16).add(Aspect.DARKNESS, 16)),
                new Row(179, "direct", List.of("minecraft:farmland"), new AspectList().add(Aspect.EARTH, 1).add(Aspect.HARVEST, 2)),
                new Row(182, "direct", List.of("minecraft:clay_ball"), new AspectList().add(Aspect.WATER, 1).add(Aspect.EARTH, 1)),
                new Row(183, "direct", List.of("minecraft:end_stone"), new AspectList().add(Aspect.EARTH, 1).add(Aspect.DARKNESS, 1)),
                new Row(187, "direct", List.of("minecraft:terracotta"), new AspectList().add(Aspect.EARTH, 4).add(Aspect.FIRE, 1)),
                new Row(188, "direct", List.of("minecraft:white_terracotta", "minecraft:orange_terracotta", "minecraft:magenta_terracotta", "minecraft:light_blue_terracotta", "minecraft:yellow_terracotta", "minecraft:lime_terracotta", "minecraft:pink_terracotta", "minecraft:gray_terracotta", "minecraft:light_gray_terracotta", "minecraft:cyan_terracotta", "minecraft:purple_terracotta", "minecraft:blue_terracotta", "minecraft:brown_terracotta", "minecraft:green_terracotta", "minecraft:red_terracotta", "minecraft:black_terracotta"), new AspectList().add(Aspect.EARTH, 3).add(Aspect.FIRE, 1).add(Aspect.SENSES, 1)),
                new Row(189, "direct", List.of("minecraft:brick"), new AspectList().add(Aspect.EARTH, 1).add(Aspect.FIRE, 1)),
                new Row(190, "direct", List.of("minecraft:nether_brick"), new AspectList().add(Aspect.FIRE, 1)),
                new Row(193, "direct", List.of("minecraft:nether_bricks"), new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 1)),
                new Row(196, "direct", List.of("minecraft:mossy_cobblestone"), new AspectList().add(Aspect.EARTH, 1).add(Aspect.PLANT, 1).add(Aspect.MAGIC, 1)),
                new Row(198, "direct", List.of("minecraft:infested_stone"), new AspectList().add(Aspect.EARTH, 2).add(Aspect.BEAST, 1).add(Aspect.TRAP, 1)),
                new Row(199, "direct", List.of("minecraft:stone_bricks"), new AspectList().add(Aspect.EARTH, 2)),
                new Row(200, "direct", List.of("minecraft:mossy_stone_bricks"), new AspectList().add(Aspect.PLANT, 1)),
                new Row(201, "direct", List.of("minecraft:cracked_stone_bricks"), new AspectList().add(Aspect.ENTROPY, 1)),
                new Row(202, "direct", List.of("minecraft:chiseled_stone_bricks"), new AspectList().add(Aspect.ORDER, 1)),
                new Row(204, "direct", List.of("minecraft:chiseled_sandstone"), new AspectList().add(Aspect.MAGIC, 1)),
                new Row(205, "direct", List.of("minecraft:smooth_sandstone"), new AspectList().add(Aspect.ORDER, 1)),
                new Row(211, "direct", List.of("minecraft:wheat_seeds"), new AspectList().add(Aspect.PLANT, 1)),
                new Row(212, "direct", List.of("minecraft:melon_seeds"), new AspectList().add(Aspect.PLANT, 1)),
                new Row(213, "direct", List.of("minecraft:pumpkin_seeds"), new AspectList().add(Aspect.PLANT, 1)),
                new Row(223, "direct", List.of("minecraft:sugar_cane"), new AspectList().add(Aspect.PLANT, 1).add(Aspect.WATER, 1).add(Aspect.AIR, 1)),
                new Row(226, "direct", List.of("minecraft:carrot"), new AspectList().add(Aspect.CROP, 1).add(Aspect.HUNGER, 1).add(Aspect.SENSES, 1)),
                new Row(227, "direct", List.of("minecraft:potato"), new AspectList().add(Aspect.CROP, 1).add(Aspect.HUNGER, 1).add(Aspect.EARTH, 1)),
                new Row(228, "direct", List.of("minecraft:baked_potato"), new AspectList().add(Aspect.CROP, 1).add(Aspect.HUNGER, 2)),
                new Row(229, "direct", List.of("minecraft:poisonous_potato"), new AspectList().add(Aspect.CROP, 1).add(Aspect.POISON, 2)),
                new Row(232, "direct", List.of("minecraft:name_tag"), new AspectList().add(Aspect.MIND, 2).add(Aspect.BEAST, 2)),
                new Row(233, "direct", List.of("minecraft:iron_horse_armor"), new AspectList().add(Aspect.ARMOR, 2).add(Aspect.BEAST, 2)),
                new Row(234, "direct", List.of("minecraft:golden_horse_armor"), new AspectList().add(Aspect.ARMOR, 4).add(Aspect.BEAST, 2)),
                new Row(235, "direct", List.of("minecraft:diamond_horse_armor"), new AspectList().add(Aspect.ARMOR, 6).add(Aspect.BEAST, 2)),
                new Row(240, "direct", List.of("minecraft:packed_ice"), new AspectList().add(Aspect.COLD, 3).add(Aspect.EARTH, 1)),
                new Row(241, "direct", List.of("minecraft:snowball"), new AspectList().add(Aspect.COLD, 1)),
                new Row(242, "direct", List.of("minecraft:cookie"), new AspectList().add(Aspect.HUNGER, 1)),
                new Row(245, "direct", List.of("minecraft:cobweb"), new AspectList().add(Aspect.TRAP, 2).add(Aspect.CLOTH, 1)),
                new Row(253, "direct", List.of("minecraft:egg"), new AspectList().add(Aspect.SLIME, 1).add(Aspect.LIFE, 1).add(Aspect.BEAST, 1)),
                new Row(257, "direct", List.of("minecraft:cod"), new AspectList().add(Aspect.FLESH, 3).add(Aspect.LIFE, 1).add(Aspect.WATER, 1)),
                new Row(259, "direct", List.of("minecraft:chicken"), new AspectList().add(Aspect.FLESH, 3).add(Aspect.LIFE, 2).add(Aspect.BEAST, 1)),
                new Row(261, "direct", List.of("minecraft:porkchop"), new AspectList().add(Aspect.FLESH, 3).add(Aspect.LIFE, 1).add(Aspect.BEAST, 1)),
                new Row(263, "direct", List.of("minecraft:beef"), new AspectList().add(Aspect.FLESH, 4).add(Aspect.LIFE, 2).add(Aspect.BEAST, 1)),
                new Row(266, "direct", List.of("minecraft:saddle"), new AspectList().add(Aspect.BEAST, 2).add(Aspect.CLOTH, 3).add(Aspect.TRAVEL, 3)),
                new Row(269, "direct", List.of("minecraft:skeleton_skull"), new AspectList().add(Aspect.DEATH, 4).add(Aspect.SOUL, 4).add(Aspect.UNDEAD, 4)),
                new Row(270, "direct", List.of("minecraft:wither_skeleton_skull"), new AspectList().add(Aspect.DEATH, 4).add(Aspect.SOUL, 4).add(Aspect.UNDEAD, 4).add(Aspect.POISON, 4)),
                new Row(271, "direct", List.of("minecraft:zombie_head"), new AspectList().add(Aspect.DEATH, 4).add(Aspect.SOUL, 4).add(Aspect.FLESH, 4)),
                new Row(272, "direct", List.of("minecraft:player_head"), new AspectList().add(Aspect.DEATH, 4).add(Aspect.SOUL, 4).add(Aspect.MAN, 4)),
                new Row(273, "direct", List.of("minecraft:creeper_head"), new AspectList().add(Aspect.DEATH, 4).add(Aspect.SOUL, 4).add(Aspect.ENTROPY, 2).add(Aspect.FIRE, 2)),
                new Row(274, "direct", List.of("minecraft:music_disc_11"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.AIR, 4).add(Aspect.GREED, 4)),
                new Row(275, "direct", List.of("minecraft:music_disc_13"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.AIR, 4).add(Aspect.WATER, 4).add(Aspect.GREED, 4)),
                new Row(276, "direct", List.of("minecraft:music_disc_cat"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.AIR, 4).add(Aspect.BEAST, 4).add(Aspect.GREED, 4)),
                new Row(277, "direct", List.of("minecraft:music_disc_chirp"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.AIR, 4).add(Aspect.EARTH, 4).add(Aspect.GREED, 4)),
                new Row(279, "direct", List.of("minecraft:music_disc_mall"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.AIR, 4).add(Aspect.MAN, 4).add(Aspect.GREED, 4)),
                new Row(280, "direct", List.of("minecraft:music_disc_mellohi"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.AIR, 4).add(Aspect.CRAFT, 4).add(Aspect.GREED, 4)),
                new Row(281, "direct", List.of("minecraft:music_disc_stal"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.AIR, 4).add(Aspect.DARKNESS, 4).add(Aspect.GREED, 4)),
                new Row(282, "direct", List.of("minecraft:music_disc_strad"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.AIR, 4).add(Aspect.MECHANISM, 4).add(Aspect.GREED, 4)),
                new Row(283, "direct", List.of("minecraft:music_disc_ward"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.AIR, 4).add(Aspect.MAGIC, 4).add(Aspect.GREED, 4)),
                new Row(284, "direct", List.of("minecraft:music_disc_blocks"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.AIR, 4).add(Aspect.TOOL, 4).add(Aspect.GREED, 4)),
                new Row(285, "direct", List.of("minecraft:music_disc_wait"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.AIR, 4).add(Aspect.TRAP, 4).add(Aspect.GREED, 4)),
                new Row(287, "direct", List.of("minecraft:chainmail_helmet"), new AspectList().add(Aspect.METAL, 8)),
                new Row(288, "direct", List.of("minecraft:chainmail_chestplate"), new AspectList().add(Aspect.METAL, 12)),
                new Row(289, "direct", List.of("minecraft:chainmail_leggings"), new AspectList().add(Aspect.METAL, 11)),
                new Row(290, "direct", List.of("minecraft:chainmail_boots"), new AspectList().add(Aspect.METAL, 7)),
                new Row(294, "complex", List.of("minecraft:bookshelf"), new AspectList().add(Aspect.MIND, 8)),
                new Row(299, "direct", List.of("minecraft:spawner"), new AspectList().add(Aspect.BEAST, 4).add(Aspect.TRAVEL, 4).add(Aspect.UNDEAD, 4).add(Aspect.MAGIC, 4)),
                new Row(300, "complex", List.of("minecraft:ender_eye"), new AspectList().add(Aspect.SENSES, 4)),
                new Row(301, "complex", List.of("minecraft:arrow"), new AspectList().add(Aspect.WEAPON, 1)),
                new Row(307, "complex", List.of("minecraft:mushroom_stew"), new AspectList().add(Aspect.HUNGER, 4)),
                new Row(309, "complex", List.of("minecraft:iron_door"), new AspectList().add(Aspect.MECHANISM, 2).add(Aspect.MOTION, 1)),
                new Row(310, "complex", List.of("minecraft:oak_door"), new AspectList().add(Aspect.MECHANISM, 1).add(Aspect.MOTION, 1)),
                new Row(313, "complex", List.of("minecraft:fishing_rod"), new AspectList().add(Aspect.WATER, 1).add(Aspect.TOOL, 1)),
                new Row(317, "direct", List.of("minecraft:milk_bucket"), new AspectList().add(Aspect.METAL, 8).add(Aspect.VOID, 1).add(Aspect.HUNGER, 2).add(Aspect.HEAL, 2).add(Aspect.WATER, 2)),
                new Row(319, "complex", List.of("minecraft:brewing_stand"), new AspectList().add(Aspect.CRAFT, 2).add(Aspect.WATER, 2)),
                new Row(320, "complex", List.of("minecraft:stone_button"), new AspectList().add(Aspect.MECHANISM, 1)),
                new Row(322, "complex", List.of("minecraft:detector_rail"), new AspectList().add(Aspect.MECHANISM, 1).add(Aspect.SENSES, 1)),
                new Row(323, "complex", List.of("minecraft:powered_rail"), new AspectList().add(Aspect.MECHANISM, 1).add(Aspect.ENERGY, 1)),
                new Row(324, "complex", List.of("minecraft:activator_rail"), new AspectList().add(Aspect.MECHANISM, 2)),
                new Row(325, "complex", List.of("minecraft:oak_fence_gate"), new AspectList().add(Aspect.MECHANISM, 1).add(Aspect.TRAVEL, 1)),
                new Row(326, "complex", List.of("minecraft:oak_pressure_plate"), new AspectList().add(Aspect.MECHANISM, 1).add(Aspect.SENSES, 1)),
                new Row(327, "complex", List.of("minecraft:stone_pressure_plate"), new AspectList().add(Aspect.MECHANISM, 1).add(Aspect.SENSES, 1)),
                new Row(328, "complex", List.of("minecraft:light_weighted_pressure_plate"), new AspectList().add(Aspect.MECHANISM, 1).add(Aspect.SENSES, 1)),
                new Row(329, "complex", List.of("minecraft:heavy_weighted_pressure_plate"), new AspectList().add(Aspect.MECHANISM, 1).add(Aspect.SENSES, 1)),
                new Row(330, "complex", List.of("minecraft:lever"), new AspectList().add(Aspect.MECHANISM, 1)),
                new Row(334, "complex", List.of("minecraft:jukebox"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.MECHANISM, 2).add(Aspect.AIR, 4)),
                new Row(335, "complex", List.of("minecraft:note_block"), new AspectList().add(Aspect.SENSES, 4).add(Aspect.MECHANISM, 1).add(Aspect.AIR, 4)),
                new Row(344, "complex", List.of("minecraft:oak_button"), new AspectList().add(Aspect.MECHANISM, 1)),
                new Row(345, "complex", List.of("minecraft:carrot_on_a_stick"), new AspectList().add(Aspect.TRAVEL, 4)),
                new Row(346, "complex", List.of("minecraft:flower_pot"), new AspectList().add(Aspect.VOID, 1).add(Aspect.PLANT, 1)),
                new Row(407, "direct", List.of("thaumcraft:loot_bag_common"), new AspectList().add(Aspect.GREED, 8)),
                new Row(408, "direct", List.of("thaumcraft:loot_bag_uncommon"), new AspectList().add(Aspect.GREED, 16)),
                new Row(409, "direct", List.of("thaumcraft:loot_bag_rare"), new AspectList().add(Aspect.GREED, 32)),
                new Row(427, "direct", List.of("thaumcraft:alchemy_furnace"), new AspectList().add(Aspect.MAGIC, 8).add(Aspect.WATER, 8).add(Aspect.CRAFT, 8))
        );
        assertEquals(88, rows.size());
        var direct = EarlyObjectAspectBindings.direct();
        var complex = EarlyObjectAspectBindings.complex();
        for (Row row : rows) {
            Map<ResourceLocation, AspectList> table = "complex".equals(row.kind()) ? complex : direct;
            for (String name : row.ids()) {
                assertAspects(table, name, row.aspects(), "original ConfigAspects:" + row.line());
                assertTrue(EarlyObjectAspectBindings.metadata(id(name)) >= 0, name);
            }
            if ("direct".equals(row.kind())) {
                for (String name : row.ids()) {
                    assertFalse(complex.containsKey(id(name)), name);
                }
            }
        }
    }

    private static void assertCluster(JsonObject row, int metadata, String modernId, String aspects) {
        assertNotNull(row, modernId);
        assertEquals("direct", row.get("registration").getAsString());
        assertEquals("ConfigItems.itemNugget", row.get("original_target").getAsString());
        assertEquals(metadata, row.get("original_metadata").getAsInt());
        assertEquals(JsonParser.parseString("[\"" + modernId + "\"]"), row.get("modern_ids"));
        assertEquals(JsonParser.parseString(aspects), row.get("aspects"));
    }

    private static void assertAspects(Map<ResourceLocation, AspectList> bindings, String name, AspectList expected) {
        assertAspects(bindings, name, expected, name);
    }

    private static void assertAspects(Map<ResourceLocation, AspectList> bindings, String name, AspectList expected, String label) {
        AspectList actual = bindings.get(id(name));
        assertNotNull(actual, label);
        assertEquals(expected.aspects, actual.aspects, label);
    }

    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
}
