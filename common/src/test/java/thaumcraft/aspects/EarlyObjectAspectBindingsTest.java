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
        assertEquals(129, direct.size(), "previous direct item bindings plus actual Taint states, Vishroom, Crimson Rites, the copper nugget and the native copper cluster");
        assertEquals(10, complex.size(), "nine existing complex bindings plus the actual native Arcane Ear");
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
            assertEquals(110, rows.size());
            var previous = new JsonArray();
            int added = 0;
            int trapdoors = 0;
            int wheat = 0;
            int ears = 0;
            int pechFoci = 0;
            int crimsonRites = 0;
            int taintAndVishroom = 0;
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
                assertEquals(312, rows.get(index - 1).getAsJsonObject().get("original_source_line").getAsInt());
                assertEquals(336, rows.get(index + 1).getAsJsonObject().get("original_source_line").getAsInt());
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

    private static void assertCluster(JsonObject row, int metadata, String modernId, String aspects) {
        assertNotNull(row, modernId);
        assertEquals("direct", row.get("registration").getAsString());
        assertEquals("ConfigItems.itemNugget", row.get("original_target").getAsString());
        assertEquals(metadata, row.get("original_metadata").getAsInt());
        assertEquals(JsonParser.parseString("[\"" + modernId + "\"]"), row.get("modern_ids"));
        assertEquals(JsonParser.parseString(aspects), row.get("aspects"));
    }

    private static void assertAspects(Map<ResourceLocation, AspectList> bindings, String name, AspectList expected) {
        AspectList actual = bindings.get(id(name));
        assertNotNull(actual, name);
        assertEquals(expected.aspects, actual.aspects, name);
    }

    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
}
