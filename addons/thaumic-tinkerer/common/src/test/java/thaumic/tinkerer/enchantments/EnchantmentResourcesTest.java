package thaumic.tinkerer.enchantments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.ThaumcraftApi;
import thaumic.tinkerer.common.enchantment.ModEnchantments;
import thaumic.tinkerer.common.enchantment.core.EnchantmentManager;
import thaumic.tinkerer.common.lib.LibEnchantIDs;
import thaumic.tinkerer.common.lib.LibEnchantNames;
import thaumic.tinkerer.common.lib.LibResearch;

/**
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 *
 * <p>The data of the Enchantments group: the data pack records of the fourteen enchantments are read with the codec of the
 * game (a record that does not parse stops the whole enchantment registry from loading), their item tags and exclusive sets
 * agree with the rules of the Enchanter, and every texture, language key, model, loot table and tag that the code and the
 * research refer to exists.</p>
 */
class EnchantmentResourcesTest {
    private static final String NS = "thaumictinkerer";
    private static final Map<String, Integer> MAX_LEVEL = Map.ofEntries(Map.entry("ascent_boost", 4),
            Map.entry("slow_fall", 3), Map.entry("auto_smelt", 1), Map.entry("desintegrate", 1), Map.entry("quick_draw", 2),
            Map.entry("vampirism", 2), Map.entry("dispersed_strike", 5), Map.entry("final_strike", 5),
            Map.entry("focused_strike", 5), Map.entry("pounce", 5), Map.entry("shatter", 5), Map.entry("shockwave", 5),
            Map.entry("tunnel", 5), Map.entry("valiance", 5));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        EnchantmentManager.initEnchantmentData();
    }

    private static JsonObject json(String path) {
        try (InputStream in = EnchantmentResourcesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new AssertionError(path, e);
        }
    }

    private static boolean exists(String path) {
        return EnchantmentResourcesTest.class.getResource(path) != null;
    }

    private static List<String> names() {
        return List.of(LibEnchantNames.ALL);
    }

    // ---- the data pack records -------------------------------------------------------------------------------------

    /** The codec of the game on lookups that know the items and invent tags and other enchantments: parse errors only. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static RegistryOps<JsonElement> ops() {
        Map<ResourceKey<?>, Holder.Reference<?>> created = new HashMap<>();
        RegistryOps.RegistryInfoLookup lookup = new RegistryOps.RegistryInfoLookup() {
            @Override
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
                HolderOwner<T> owner = new HolderOwner<T>() {};
                HolderGetter<T> getter = new HolderGetter<T>() {
                    @Override
                    public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
                        if (((Object) registryKey).equals(Registries.ITEM)) {
                            Optional present = BuiltInRegistries.ITEM.getHolder((ResourceKey) key);
                            if (present.isPresent()) return present;
                        }
                        return Optional.of((Holder.Reference<T>) created.computeIfAbsent(key,
                                k -> Holder.Reference.createStandAlone(owner, (ResourceKey) k)));
                    }

                    @Override
                    public Optional<HolderSet.Named<T>> get(TagKey<T> tag) {
                        return Optional.of(HolderSet.emptyNamed(owner, tag));
                    }
                };
                return Optional.of(new RegistryOps.RegistryInfo<T>(owner, getter, Lifecycle.stable()));
            }
        };
        return RegistryOps.create(JsonOps.INSTANCE, lookup);
    }

    private static Enchantment parse(String name) {
        DataResult<Enchantment> result = Enchantment.DIRECT_CODEC.parse(ops(), json("/data/" + NS + "/enchantment/" + name + ".json"));
        return result.getOrThrow(message -> new AssertionError(name + ": " + message));
    }

    @Test
    void everyEnchantmentRecordParsesWithTheCodecOfTheGame() {
        for (String name : names()) {
            Enchantment enchantment = parse(name);
            assertEquals(MAX_LEVEL.get(name), enchantment.getMaxLevel(), name + " max level of 1.7.10");
            assertEquals(1, enchantment.getMinLevel(), name);
            assertEquals("enchantment." + NS + "." + name, ((net.minecraft.network.chat.contents.TranslatableContents)
                    enchantment.description().getContents()).getKey(), name);
            assertTrue(enchantment.effects().isEmpty(), name + ": the behaviour is in Java");
            // EnchantmentMod(id, max, type) called super(id, 0, type): weight 0 is nothing at the 1.7.10 anvil (nothing per level
            // from a tool, one per level from a book, which is what max(1, 0 / 2) of 1.21.1 gives for an anvil cost of 0)
            assertEquals(0, enchantment.definition().anvilCost(), name + " anvil cost");
            // without primary items the Enchanter (isPrimaryItem) asks the item tag, like an anvil (isSupportedItem)
            assertTrue(enchantment.definition().primaryItems().isEmpty(), name + " has no primary items");
        }
    }

    @Test
    void theEquipmentSlotsAreTheOnesTheEnchantmentActsIn() {
        Map<String, EquipmentSlotGroup> slots = Map.of("ascent_boost", EquipmentSlotGroup.LEGS, "pounce", EquipmentSlotGroup.LEGS,
                "slow_fall", EquipmentSlotGroup.FEET, "shockwave", EquipmentSlotGroup.FEET, "quick_draw", EquipmentSlotGroup.MAINHAND,
                "vampirism", EquipmentSlotGroup.MAINHAND, "tunnel", EquipmentSlotGroup.MAINHAND, "shatter", EquipmentSlotGroup.MAINHAND,
                "auto_smelt", EquipmentSlotGroup.MAINHAND, "desintegrate", EquipmentSlotGroup.MAINHAND);
        for (Map.Entry<String, EquipmentSlotGroup> entry : slots.entrySet()) {
            assertEquals(List.of(entry.getValue()), parse(entry.getKey()).definition().slots(), entry.getKey());
        }
    }

    @Test
    void theEnchantmentsAreNotInAnyTagThatMakesThemObtainableExceptTreasure() {
        Set<String> treasure = values("/data/minecraft/tags/enchantment/treasure.json");
        for (String name : names()) {
            assertTrue(treasure.contains(NS + ":" + name), name + " is a treasure: found only through the Enchanter");
        }
        // the add-on does not touch the tags of the enchanting table, the villagers and the loot
        for (String tag : List.of("in_enchanting_table", "non_treasure", "tradeable", "on_random_loot", "on_traded_equipment",
                "on_mob_spawn_equipment")) {
            assertFalse(exists("/data/" + NS + "/tags/enchantment/" + tag + ".json"), tag);
        }
    }

    // ---- tags ------------------------------------------------------------------------------------------------------

    private static Set<String> values(String path) {
        Set<String> values = new HashSet<>();
        for (JsonElement value : json(path).getAsJsonArray("values")) {
            values.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString());
        }
        return values;
    }

    @Test
    void everyItemTagOfAnEnchantmentIsAValidTagFileOfExistingOrOptionalItems() {
        for (String name : names()) {
            String path = "/data/" + NS + "/tags/item/enchantable/" + name + ".json";
            JsonObject file = json(path);
            TagFile.CODEC.parse(JsonOps.INSTANCE, file).getOrThrow(message -> new AssertionError(path + ": " + message));
            for (JsonElement value : file.getAsJsonArray("values")) {
                boolean optional = value.isJsonObject() && !value.getAsJsonObject().get("required").getAsBoolean();
                String id = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();
                if (id.startsWith("#")) {
                    // a tag of the game or of the add-on: it exists as a file of the game's data (the jar) or ours
                    String tag = id.substring(1);
                    String namespace = tag.substring(0, tag.indexOf(':'));
                    String tagPath = tag.substring(tag.indexOf(':') + 1);
                    assertTrue(exists("/data/" + namespace + "/tags/item/" + tagPath + ".json"), path + " refers to the tag " + id);
                } else if (!optional) {
                    assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)), path + " needs the item " + id);
                } else {
                    assertTrue(id.startsWith("thaumcraft:") || id.startsWith(NS + ":"), path + " an optional item of the two mods: " + id);
                }
            }
        }
    }

    @Test
    void theTagsKeepOutTheItemsTheOriginalRefused() {
        Set<String> autoSmelt = values("/data/" + NS + "/tags/item/enchantable/auto_smelt.json");
        assertFalse(autoSmelt.contains("thaumcraft:elemental_axe"), "Flaming Touch is not for the Axe of the Stream");
        assertFalse(autoSmelt.contains("#minecraft:axes"), "the tag of the axes holds the Axe of the Stream");
        assertTrue(autoSmelt.contains("minecraft:diamond_axe") && autoSmelt.contains("#minecraft:pickaxes"));

        assertFalse(values("/data/" + NS + "/tags/item/enchantable/vampirism.json").contains(NS + ":blood_sword"),
                "Vampirism is not for the Cursed Spirit's Blade");
        for (String strike : List.of("dispersed_strike", "focused_strike", "final_strike", "valiance")) {
            assertTrue(values("/data/" + NS + "/tags/item/enchantable/" + strike + ".json").contains(NS + ":blood_sword"), strike);
        }

        for (String boots : List.of("slow_fall", "shockwave")) {
            Set<String> values = values("/data/" + NS + "/tags/item/enchantable/" + boots + ".json");
            assertFalse(values.contains(NS + ":ichorcloth_boots_gem"), boots + " is not for the gem boots");
            assertFalse(values.contains("#minecraft:foot_armor"), boots + ": the tag of the boots holds the gem boots");
            assertTrue(values.contains("minecraft:diamond_boots"));
        }
        assertTrue(values("/data/" + NS + "/tags/item/enchantable/ascent_boost.json").contains("#minecraft:leg_armor"));
        assertTrue(values("/data/" + NS + "/tags/item/enchantable/pounce.json").contains("#minecraft:leg_armor"));
        assertTrue(values("/data/" + NS + "/tags/item/enchantable/quick_draw.json").contains("#minecraft:enchantable/bow"));
        for (String digger : List.of("desintegrate", "shatter", "tunnel")) {
            Set<String> values = values("/data/" + NS + "/tags/item/enchantable/" + digger + ".json");
            assertTrue(values.containsAll(List.of("#minecraft:pickaxes", "#minecraft:shovels", "#minecraft:axes")), digger);
        }
    }

    @Test
    void theTagsHoldTheThaumcraftItemsOfTheTypesTheOriginalAccepted() {
        // EnumEnchantmentType.weapon was ItemSword and digger was ItemTool: the Crimson Blade is a sword and the Primal Crusher a
        // tool of Thaumcraft that no vanilla tag lists
        for (String sword : List.of("vampirism", "final_strike", "valiance", "focused_strike", "dispersed_strike")) {
            assertTrue(values("/data/" + NS + "/tags/item/enchantable/" + sword + ".json").contains("thaumcraft:crimson_sword"), sword);
        }
        for (String digger : List.of("shatter", "tunnel", "desintegrate", "auto_smelt")) {
            assertTrue(values("/data/" + NS + "/tags/item/enchantable/" + digger + ".json").contains("thaumcraft:primal_crusher"), digger);
        }
        // armor_legs / armor_feet was any ItemArmor of the slot: the legs and boots of the Crimson Cult are in no vanilla tag
        for (String legs : List.of("ascent_boost", "pounce")) {
            assertTrue(values("/data/" + NS + "/tags/item/enchantable/" + legs + ".json").containsAll(
                    List.of("thaumcraft:cultist_robe_legs", "thaumcraft:cultist_plate_legs", "thaumcraft:cultist_leader_legs")), legs);
        }
        for (String boots : List.of("slow_fall", "shockwave")) {
            assertTrue(values("/data/" + NS + "/tags/item/enchantable/" + boots + ".json").contains("thaumcraft:cultist_boots"), boots);
        }
    }

    @Test
    void theVanillaEnchantableTagsHoldTheBladeAndTheCrusherForTheEnchantmentsOfTheGame() {
        // the Cursed Spirit's Blade (an ItemSword with Thaumium enchantability) took every vanilla weapon enchantment; it is in
        // none of the tags of the sword items (Vampirism must not go on it), so the tags of the enchantments list it
        for (String tag : List.of("sword", "sharp_weapon", "fire_aspect", "durability")) {
            Set<String> values = values("/data/minecraft/tags/item/enchantable/" + tag + ".json");
            assertTrue(values.contains(NS + ":blood_sword"), tag + " lists the Cursed Spirit's Blade");
            assertTrue(values.contains("thaumcraft:crimson_sword"), tag + " lists the Crimson Blade");
        }
        for (String tag : List.of("mining", "mining_loot", "durability")) {
            assertTrue(values("/data/minecraft/tags/item/enchantable/" + tag + ".json").contains("thaumcraft:primal_crusher"), tag);
        }
        assertFalse(values("/data/minecraft/tags/item/swords.json").contains(NS + ":blood_sword"),
                "the blade stays out of #minecraft:swords, which Vampirism's tag holds");
    }

    // ---- exclusive sets agree with the rules --------------------------------------------------------------------

    private static final List<ResourceKey<Enchantment>> WEAPON_PARTNERS = List.of(Enchantments.SHARPNESS, Enchantments.SMITE,
            Enchantments.BANE_OF_ARTHROPODS, Enchantments.KNOCKBACK, Enchantments.FIRE_ASPECT, Enchantments.LOOTING,
            Enchantments.SWEEPING_EDGE, Enchantments.MENDING, Enchantments.UNBREAKING, ThaumcraftApi.ENCHANTMENT_REPAIR,
            ModEnchantments.dispersedStrikes, ModEnchantments.focusedStrike, ModEnchantments.finalStrike, ModEnchantments.valiance,
            ModEnchantments.vampirism);
    private static final List<ResourceKey<Enchantment>> DIGGER_PARTNERS = List.of(Enchantments.EFFICIENCY, Enchantments.SILK_TOUCH,
            Enchantments.FORTUNE, Enchantments.MENDING, Enchantments.UNBREAKING, ThaumcraftApi.ENCHANTMENT_REPAIR,
            ModEnchantments.shatter, ModEnchantments.tunnel, ModEnchantments.desintegrate, ModEnchantments.autoSmelt);
    private static final List<ResourceKey<Enchantment>> BOOT_PARTNERS = List.of(Enchantments.FEATHER_FALLING, Enchantments.PROTECTION,
            Enchantments.UNBREAKING, ThaumcraftApi.ENCHANTMENT_REPAIR, ThaumcraftApi.ENCHANTMENT_HASTE, ModEnchantments.slowFall,
            ModEnchantments.shockwave);
    private static final List<ResourceKey<Enchantment>> BOW_PARTNERS = List.of(Enchantments.POWER, Enchantments.PUNCH,
            Enchantments.FLAME, Enchantments.INFINITY, Enchantments.MENDING, Enchantments.UNBREAKING, ModEnchantments.quickDraw);
    private static final List<ResourceKey<Enchantment>> LEG_PARTNERS = List.of(Enchantments.PROTECTION, Enchantments.UNBREAKING,
            ThaumcraftApi.ENCHANTMENT_REPAIR, ModEnchantments.ascentBoost, ModEnchantments.pounce);

    private static Set<ResourceKey<Enchantment>> exclusiveSet(ResourceKey<Enchantment> enchantment) {
        Set<ResourceKey<Enchantment>> set = new HashSet<>();
        if (!enchantment.location().getNamespace().equals(NS)) return set;
        JsonObject record = json("/data/" + NS + "/enchantment/" + enchantment.location().getPath() + ".json");
        if (!record.has("exclusive_set")) return set;
        JsonArray array = record.getAsJsonArray("exclusive_set");
        for (JsonElement element : array) set.add(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(element.getAsString())));
        return set;
    }

    /** What {@code Enchantment.areCompatible} makes of the exclusive sets of the two records. */
    private static boolean excludedByData(ResourceKey<Enchantment> a, ResourceKey<Enchantment> b) {
        return exclusiveSet(a).contains(b) || exclusiveSet(b).contains(a);
    }

    private static void assertSameAsRules(List<ResourceKey<Enchantment>> group) {
        for (ResourceKey<Enchantment> a : group) {
            if (!a.location().getNamespace().equals(NS)) continue;
            for (ResourceKey<Enchantment> b : group) {
                if (a.equals(b)) continue;
                boolean byRules = EnchantmentManager.rulesForbid(a, List.of(b));
                assertEquals(byRules, excludedByData(a, b),
                        a.location().getPath() + " and " + b.location() + ": the Enchanter's rules say " + (byRules ? "no" : "yes")
                                + ", the exclusive sets of the data say " + (byRules ? "yes" : "no"));
            }
        }
    }

    @Test
    void theExclusiveSetsOfTheDataAreTheRulesOfTheEnchanterForEveryPairOnTheSameKindOfItem() {
        assertSameAsRules(WEAPON_PARTNERS);
        assertSameAsRules(DIGGER_PARTNERS);
        assertSameAsRules(BOOT_PARTNERS);
        assertSameAsRules(BOW_PARTNERS);
        assertSameAsRules(LEG_PARTNERS);
    }

    @Test
    void everyEnchantmentTheExclusiveSetsNameExists() {
        Set<String> known = new HashSet<>();
        for (String name : names()) known.add(NS + ":" + name);
        for (String name : names()) {
            for (ResourceKey<Enchantment> excluded : exclusiveSet(LibEnchantIDs.key(name))) {
                String id = excluded.location().toString();
                if (id.startsWith(NS + ":")) assertTrue(known.contains(id), name + " names " + id);
                else assertTrue(id.startsWith("minecraft:") || id.startsWith("thaumcraft:"), name + " names " + id);
            }
        }
    }

    // ---- language, textures, block --------------------------------------------------------------------------------

    @Test
    void everyLanguageHasTheNamesAndTextsOfTheEnchantmentsAndTheResearch() {
        List<String> keys = new ArrayList<>();
        for (String name : names()) keys.add("enchantment." + NS + "." + name);
        List<String> research = new ArrayList<>(List.of(LibResearch.KEY_ENCHANTER, LibResearch.KEY_ENCHANT_ASCENT_BOOST,
                LibResearch.KEY_ENCHANT_SLOW_FALL, LibResearch.KEY_ENCHANT_AUTO_SMELT, LibResearch.KEY_ENCHANT_DESINTEGRATE,
                LibResearch.KEY_ENCHANT_QUICK_DRAW, LibResearch.KEY_ENCHANT_VAMPIRISM, LibResearch.KEY_ENCHANT_FOCUSED_STRIKE,
                LibResearch.KEY_ENCHANT_DISPERSED_STRIKE, LibResearch.KEY_ENCHANT_FINAL_STRIKE, LibResearch.KEY_ENCHANT_POUNCE,
                LibResearch.KEY_ENCHANT_SHATTER, LibResearch.KEY_ENCHANT_SHOCKWAVE, LibResearch.KEY_ENCHANT_TUNNEL,
                LibResearch.KEY_ENCHANT_VALIANCE));
        for (String key : research) {
            keys.add("tc.research_name." + key);
            keys.add("tc.research_text." + key);
            keys.add("ttresearch.page." + key + ".0");
        }
        keys.add("ttresearch.page." + LibResearch.KEY_ENCHANTER + ".1");
        keys.add("ttresearch.page." + LibResearch.KEY_ENCHANTER + ".2");
        keys.addAll(List.of("block." + NS + ".enchanter", "ttmisc.startEnchant", "ttmisc.baseCost", "ttmisc.clickToRemove"));

        for (String language : List.of("en_us", "ru_ru")) {
            JsonObject lang = json("/assets/" + NS + "/lang/" + language + ".json");
            for (String key : keys) {
                assertTrue(lang.has(key) && !lang.get(key).getAsString().isEmpty(), language + " lacks " + key);
            }
        }
    }

    @Test
    void everyEnchantmentOfTheTableHasItsIconAndTheGuiArtworkExists() {
        for (ResourceKey<Enchantment> enchantment : EnchantmentManager.enchantments()) {
            ResourceLocation texture = EnchantmentManager.data(enchantment, 1).texture;
            assertTrue(exists("/assets/" + texture.getNamespace() + "/" + texture.getPath()), enchantment.location() + " -> " + texture);
        }
        assertTrue(exists("/assets/" + NS + "/textures/gui/enchanter.png"));
        assertTrue(exists("/assets/" + NS + "/textures/misc/r_enchanting.png"), "the icon of the research tab");
    }

    @Test
    void theEnchanterHasAStateAModelsALootTableAndIsMinedWithAPickaxe() {
        assertTrue(exists("/assets/" + NS + "/blockstates/enchanter.json"));
        assertTrue(exists("/assets/" + NS + "/models/item/enchanter.json"));
        JsonObject state = json("/assets/" + NS + "/blockstates/enchanter.json");
        assertEquals(NS + ":block/enchanter", state.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
        JsonObject model = json("/assets/" + NS + "/models/block/enchanter.json");
        for (Map.Entry<String, JsonElement> texture : model.getAsJsonObject("textures").entrySet()) {
            String id = texture.getValue().getAsString();
            assertTrue(exists("/assets/" + NS + "/textures/" + id.substring(NS.length() + 1) + ".png"), id);
        }
        // the box of 1.7.10 setBlockBounds(0, 0, 0, 1, 0.75, 1)
        JsonObject element = model.getAsJsonArray("elements").get(0).getAsJsonObject();
        assertEquals(12, element.getAsJsonArray("to").get(1).getAsInt());

        JsonObject loot = json("/data/" + NS + "/loot_table/blocks/enchanter.json");
        assertEquals("minecraft:block", loot.get("type").getAsString());
        assertEquals(NS + ":enchanter", loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0)
                .getAsJsonObject().get("name").getAsString());
        assertTrue(values("/data/" + NS + "/tags/block/mineable/tile_blocks/pickaxe.json").contains(NS + ":enchanter"));
    }
}
