package thaumcraft.common.lib.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Machine-checkable coverage of the original TC4 recipe catalogue against the native recipe registry.
 *
 * <p>The bundled corpus (data/thaumcraft/tc4/research.json) names the ORIGINAL 4.2.3.5 recipe keys
 * (AdvAlchemyConstruct, InfEnch0, WardedGlass, ...). The native recipe JSON files use their own file names,
 * so every original key is mapped here onto the native recipe id (namespace:path, exactly the id the recipe
 * manager uses, for example thaumcraft:arcane/alchemical_construct). This mapping is the contract the research
 * book uses to display the recipe of a research page.
 *
 * <p>Three original key groups exist:
 * <ul>
 *   <li>{@link #nativeRecipeIds()} - original recipes that have a native recipe JSON file.</li>
 *   <li>{@link #constructBlueprints()} - the nine original <em>wand constructs</em>. The original does not register
 *       those as recipes at all: ConfigRecipes:83-100 stores a plain List and the research page casts it as
 *       (List)recipes.get("..."), never as an IRecipe. They are built in the world, not crafted, so they carry a
 *       blueprint description instead of a recipe id.</li>
 *   <li>{@link #deferredRecipes()} - original recipes whose result item/block does not exist in the 1.21.1 port
 *       yet, or whose original registration was conditional on a third-party ore mod. Each entry states why.</li>
 * </ul>
 *
 * <p>Everything here is pure data plus classpath scanning, so it runs in a plain unit test without a game runtime.
 */
public final class ResearchRecipeCoverage {
    /** Classpath root of the native recipe JSON tree; the ids below are the matching namespace:path ids. */
    private static final String RECIPE_ROOT = "data/thaumcraft/recipe";
    private static final String NAMESPACE = "thaumcraft";

    /** Original TC4 recipe key to native recipe id. */
    private static final Map<String, String> NATIVE_RECIPE_IDS = buildNativeRecipeIds();
    /** Original TC4 wand construct (not a recipe in the original either) to its original layout. */
    private static final Map<String, String> CONSTRUCT_BLUEPRINTS = buildConstructBlueprints();
    /** Original TC4 recipe key without a native recipe yet, to the reason why. */
    private static final Map<String, String> DEFERRED = buildDeferred();
    /** Native recipe ids that are actually present in the packaged recipe tree. */
    private static final Set<String> NATIVE_RECIPE_FILES = scanNativeRecipeIds();

    private ResearchRecipeCoverage() {}

    /** Original TC4 recipe key to the native recipe id the research book resolves it with. */
    public static Map<String, String> nativeRecipeIds() { return NATIVE_RECIPE_IDS; }

    /** The original wand constructs, which the original stored as plain lists instead of recipes. */
    public static Map<String, String> constructBlueprints() { return CONSTRUCT_BLUEPRINTS; }

    /** Original recipe keys with no native recipe yet, with the reason why each one is still open. */
    public static Map<String, String> deferredRecipes() { return DEFERRED; }

    /** Native recipe ids that exist in the packaged data/thaumcraft/recipe tree. */
    public static Set<String> nativeRecipeIdsPresent() { return NATIVE_RECIPE_FILES; }

    /** The native recipe id of an original key, or null when it has none. */
    public static String nativeRecipeId(String originalKey) { return NATIVE_RECIPE_IDS.get(originalKey); }

    /**
     * The research key the native recipe of an original key is gated by, or null when the native recipe has no
     * research gate (an ordinary crafting-table recipe) or the original key has no native recipe at all.
     */
    public static String nativeResearchGate(String originalKey) {
        String id = NATIVE_RECIPE_IDS.get(originalKey);
        if (id == null) return null;
        String path = id.substring(id.indexOf(':') + 1);
        try (InputStream stream = ResearchRecipeCoverage.class.getClassLoader()
                .getResourceAsStream(RECIPE_ROOT + "/" + path + ".json")) {
            if (stream == null) return null;
            JsonObject recipe = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonElement gate = recipe.get("research");
            return gate == null || gate.isJsonNull() ? null : gate.getAsString();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read the native recipe " + id, exception);
        }
    }

    /** The research gate of every mapped native recipe. Original keys without a gate map to null. */
    public static Map<String, String> nativeResearchGates() {
        Map<String, String> gates = new LinkedHashMap<>();
        for (String key : NATIVE_RECIPE_IDS.keySet()) gates.put(key, nativeResearchGate(key));
        return Collections.unmodifiableMap(gates);
    }

    /** True when the original key resolves to a native recipe file or to a documented original wand construct. */
    public static boolean isResolved(String originalKey) {
        if (originalKey == null) return false;
        if (CONSTRUCT_BLUEPRINTS.containsKey(originalKey)) return true;
        String id = NATIVE_RECIPE_IDS.get(originalKey);
        return id != null && NATIVE_RECIPE_FILES.contains(id);
    }

    /**
     * Every original recipe key referenced by the bundled corpus that has no native recipe and is not one of the
     * nine original wand constructs. Sorted, without duplicates.
     */
    public static List<String> missingRecipeKeys() {
        ResearchCatalog.loadBundled();
        Set<String> missing = new TreeSet<>();
        for (ResearchDefinition definition : ResearchCatalog.all()) {
            for (ResearchDefinition.Page page : definition.pages()) {
                String key = page.recipeKey();
                if (key != null && !isResolved(key)) missing.add(key);
            }
        }
        return List.copyOf(missing);
    }

    /** The research entries whose pages still reference an unresolved original recipe key. Sorted. */
    public static List<String> missingResearchKeys() {
        ResearchCatalog.loadBundled();
        Set<String> missing = new TreeSet<>();
        for (ResearchDefinition definition : ResearchCatalog.all()) {
            for (ResearchDefinition.Page page : definition.pages()) {
                String key = page.recipeKey();
                if (key != null && !isResolved(key)) { missing.add(definition.key()); break; }
            }
        }
        return List.copyOf(missing);
    }

    /** How many research pages reference each original recipe key. Sorted by key. */
    public static Map<String, Integer> recipeKeyCounts() {
        ResearchCatalog.loadBundled();
        Map<String, Integer> counts = new TreeMap<>();
        for (ResearchDefinition definition : ResearchCatalog.all()) {
            for (ResearchDefinition.Page page : definition.pages()) {
                String key = page.recipeKey();
                if (key != null) counts.merge(key, 1, Integer::sum);
            }
        }
        return Collections.unmodifiableMap(counts);
    }

    /** A human readable summary of the whole catalogue coverage. */
    public static String report() {
        ResearchCatalog.loadBundled();
        List<String> missing = missingRecipeKeys();
        List<String> research = missingResearchKeys();
        StringBuilder text = new StringBuilder();
        text.append("TC4 recipe coverage: ").append(NATIVE_RECIPE_IDS.size()).append(" original keys mapped to a native recipe, ")
                .append(CONSTRUCT_BLUEPRINTS.size()).append(" original wand constructs (")
                .append(CONSTRUCT_BLUEPRINTS.keySet().stream().filter(NATIVE_RECIPE_IDS::containsKey).count())
                .append(" of them anchored on a native blueprint), ")
                .append(missing.size()).append(" unresolved, ")
                .append(NATIVE_RECIPE_FILES.size()).append(" native recipe files packaged.\n");
        if (!missing.isEmpty()) {
            text.append("Unresolved original recipe keys:\n");
            for (String key : missing) {
                text.append("  ").append(key).append(" - ").append(DEFERRED.getOrDefault(key, "no native recipe")).append('\n');
            }
            text.append("Research entries affected: ").append(String.join(", ", research)).append('\n');
        } else {
            text.append("Every original recipe key resolves to a native recipe or an original wand construct.\n");
        }
        return text.toString();
    }

    private static Map<String, String> buildNativeRecipeIds() {
        Map<String, String> ids = new LinkedHashMap<>();
        ids.put("AdvAlchemyConstruct", "thaumcraft:arcane/advanced_alchemical_construct");
        ids.put("AdvAlchemyFurnace", "thaumcraft:arcane/advanced_alchemical_construct");
        ids.put("AdvancedGolem", "thaumcraft:infusion/golem_advanced");
        ids.put("AlchemicalConstruct", "thaumcraft:arcane/alchemical_construct");
        ids.put("AlchemyFurnace", "thaumcraft:arcane/alchemy_furnace");
        ids.put("Alembic", "thaumcraft:arcane/alembic");
        ids.put("AltBonemeal", "thaumcraft:crucible/alt_bonemeal");
        ids.put("AltClay", "thaumcraft:crucible/alt_clay");
        ids.put("AltCrackedBrick", "thaumcraft:crucible/alt_cracked_brick");
        ids.put("AltGlowstone", "thaumcraft:crucible/alt_glowstone");
        ids.put("AltGunpowder", "thaumcraft:crucible/alt_gunpowder");
        ids.put("AltIce", "thaumcraft:crucible/alt_ice");
        ids.put("AltInk", "thaumcraft:crucible/alt_ink");
        ids.put("AltMossyCobble", "thaumcraft:crucible/alt_mossy_cobble");
        ids.put("AltSlime", "thaumcraft:crucible/alt_slime");
        ids.put("AltWeb", "thaumcraft:crucible/alt_web");
        ids.put("Alumentum", "thaumcraft:crucible/alumentum");
        ids.put("ArcaneBore", "thaumcraft:infusion/arcane_bore");
        ids.put("ArcaneBoreBase", "thaumcraft:arcane/arcane_bore_base");
        ids.put("ArcaneDoor", "thaumcraft:arcane/arcane_door");
        ids.put("ArcaneEar", "thaumcraft:arcane/arcane_ear");
        ids.put("ArcaneLamp", "thaumcraft:arcane/arcane_lamp");
        ids.put("ArcanePedestal", "thaumcraft:arcane/arcane_pedestal");
        ids.put("ArcaneSpa", "thaumcraft:arcane/arcane_spa");
        ids.put("ArcaneStone1", "thaumcraft:arcane/arcane_stone");
        ids.put("ArcaneStone2", "thaumcraft:arcane_stone_bricks");
        ids.put("ArcaneStone3", "thaumcraft:arcane_stone_brick_stairs");
        ids.put("ArcaneStone4", "thaumcraft:arcane_stone_brick_slab");
        ids.put("BathSalts", "thaumcraft:crucible/bath_salts");
        ids.put("Bellows", "thaumcraft:arcane/bellows");
        ids.put("BlockFlesh", "thaumcraft:flesh_block");
        ids.put("BlockTallow", "thaumcraft:tallow_block");
        ids.put("BlockThaumium", "thaumcraft:thaumium_block");
        ids.put("BoneBow", "thaumcraft:arcane/bone_bow");
        ids.put("BootsTraveller", "thaumcraft:infusion/boots_traveller");
        ids.put("BottleTaint", "thaumcraft:crucible/bottle_taint");
        ids.put("Centrifuge", "thaumcraft:arcane/centrifuge");
        ids.put("CoreAlchemy", "thaumcraft:infusion/golem_core_alchemy");
        ids.put("CoreBlank", "thaumcraft:arcane/golem_core_blank");
        ids.put("CoreButcher", "thaumcraft:crucible/golem_core_butcher");
        ids.put("CoreEmpty", "thaumcraft:crucible/golem_core_empty");
        ids.put("CoreFill", "thaumcraft:crucible/golem_core_fill");
        ids.put("CoreFishing", "thaumcraft:infusion/golem_core_fishing");
        ids.put("CoreGather", "thaumcraft:crucible/golem_core_gather");
        ids.put("CoreGuard", "thaumcraft:crucible/golem_core_guard");
        ids.put("CoreHarvest", "thaumcraft:crucible/golem_core_harvest");
        ids.put("CoreLiquid", "thaumcraft:crucible/golem_core_liquid");
        ids.put("CoreLumber", "thaumcraft:infusion/golem_core_lumber");
        ids.put("CoreSorting", "thaumcraft:infusion/golem_core_sorting");
        ids.put("CoreUse", "thaumcraft:infusion/golem_core_use");
        ids.put("Deconstructor", "thaumcraft:arcane/deconstruction_table");
        ids.put("EldritchEye", "thaumcraft:infusion/eldritch_eye");
        ids.put("ElementalAxe", "thaumcraft:infusion/elemental_axe");
        ids.put("ElementalHoe", "thaumcraft:infusion/elemental_hoe");
        ids.put("ElementalPick", "thaumcraft:infusion/elemental_pickaxe");
        ids.put("ElementalShovel", "thaumcraft:infusion/elemental_shovel");
        ids.put("ElementalSword", "thaumcraft:infusion/elemental_sword");
        ids.put("EnchantedFabric", "thaumcraft:arcane/enchanted_fabric");
        ids.put("EssentiaCrystalizer", "thaumcraft:arcane/essentia_crystallizer");
        ids.put("EssentiaReservoir", "thaumcraft:infusion/essentia_reservoir");
        ids.put("EtherealBloom", "thaumcraft:crucible/ethereal_bloom");
        ids.put("Filter", "thaumcraft:arcane/vis_filter");
        ids.put("FocalManipulator", "thaumcraft:arcane/focal_manipulator");
        ids.put("FocusExcavation", "thaumcraft:arcane/focus_excavation");
        ids.put("FocusFire", "thaumcraft:arcane/focus_fire");
        ids.put("FocusFrost", "thaumcraft:arcane/focus_frost");
        ids.put("FocusHellbat", "thaumcraft:infusion/focus_hellbat");
        ids.put("FocusPortableHole", "thaumcraft:infusion/focus_portable_hole");
        ids.put("FocusPouch", "thaumcraft:arcane/focus_pouch");
        ids.put("FocusPrimal", "thaumcraft:arcane/focus_primal");
        ids.put("FocusShock", "thaumcraft:arcane/focus_shock");
        ids.put("FocusTrade", "thaumcraft:arcane/focus_trade");
        ids.put("FocusWarding", "thaumcraft:infusion/focus_warding");
        ids.put("Goggles", "thaumcraft:arcane/goggles");
        ids.put("GolemBell", "thaumcraft:arcane/golem_bell");
        ids.put("GolemClay", "thaumcraft:crucible/golem_clay");
        ids.put("GolemFetter", "thaumcraft:arcane/golem_fetter");
        ids.put("GolemFlesh", "thaumcraft:crucible/golem_flesh");
        ids.put("GolemIron", "thaumcraft:crucible/golem_iron");
        ids.put("GolemStone", "thaumcraft:crucible/golem_stone");
        ids.put("GolemStraw", "thaumcraft:crucible/golem_straw");
        ids.put("GolemTallow", "thaumcraft:crucible/golem_tallow");
        ids.put("GolemThaumium", "thaumcraft:crucible/golem_thaumium");
        ids.put("GolemWood", "thaumcraft:crucible/golem_wood");
        ids.put("GoldKey", "thaumcraft:arcane/gold_key");
        ids.put("Grate", "thaumcraft:item_grate");
        ids.put("HelmGoggles", "thaumcraft:infusion/fortress_goggles");
        ids.put("HoverGirdle", "thaumcraft:infusion/hover_girdle");
        ids.put("HoverHarness", "thaumcraft:infusion/hover_harness");
        ids.put("HungryChest", "thaumcraft:arcane/hungry_chest");
        ids.put("InfEnch0", "thaumcraft:infusion_enchantment/protection");
        ids.put("InfEnch1", "thaumcraft:infusion_enchantment/fire_protection");
        ids.put("InfEnch10", "thaumcraft:infusion_enchantment/bane_of_arthropods");
        ids.put("InfEnch11", "thaumcraft:infusion_enchantment/knockback");
        ids.put("InfEnch12", "thaumcraft:infusion_enchantment/fire_aspect");
        ids.put("InfEnch13", "thaumcraft:infusion_enchantment/looting");
        ids.put("InfEnch14", "thaumcraft:infusion_enchantment/efficiency");
        ids.put("InfEnch15", "thaumcraft:infusion_enchantment/silk_touch");
        ids.put("InfEnch16", "thaumcraft:infusion_enchantment/unbreaking");
        ids.put("InfEnch17", "thaumcraft:infusion_enchantment/fortune");
        ids.put("InfEnch18", "thaumcraft:infusion_enchantment/power");
        ids.put("InfEnch19", "thaumcraft:infusion_enchantment/punch");
        ids.put("InfEnch2", "thaumcraft:infusion_enchantment/blast_protection");
        ids.put("InfEnch20", "thaumcraft:infusion_enchantment/flame");
        ids.put("InfEnch21", "thaumcraft:infusion_enchantment/infinity");
        ids.put("InfEnch3", "thaumcraft:infusion_enchantment/projectile_protection");
        ids.put("InfEnch4", "thaumcraft:infusion_enchantment/feather_falling");
        ids.put("InfEnch5", "thaumcraft:infusion_enchantment/respiration");
        ids.put("InfEnch6", "thaumcraft:infusion_enchantment/aqua_affinity");
        ids.put("InfEnch7", "thaumcraft:infusion_enchantment/thorns");
        ids.put("InfEnch8", "thaumcraft:infusion_enchantment/sharpness");
        ids.put("InfEnch9", "thaumcraft:infusion_enchantment/smite");
        ids.put("InfEnchHaste", "thaumcraft:infusion_enchantment/haste");
        ids.put("InfEnchRepair", "thaumcraft:infusion_enchantment/repair");
        ids.put("InfusionAltar", "thaumcraft:arcane/infusion_matrix");
        ids.put("InfusionMatrix", "thaumcraft:arcane/infusion_matrix");
        ids.put("IronKey", "thaumcraft:arcane/iron_key");
        ids.put("JarBrain", "thaumcraft:infusion/jar_brain");
        ids.put("JarLabel", "thaumcraft:jar_label");
        ids.put("JarLabelNull", "thaumcraft:jar_label_clear");
        ids.put("JarVoid", "thaumcraft:arcane/jar_void");
        ids.put("KnowFrag", "thaumcraft:unknown_research_note");
        ids.put("LampFertility", "thaumcraft:infusion/fertility_lamp");
        ids.put("LampGrowth", "thaumcraft:infusion/growth_lamp");
        ids.put("Levitator", "thaumcraft:arcane/levitator");
        ids.put("LiquidDeath", "thaumcraft:crucible/bucket_of_death");
        ids.put("MaskAngryGhost", "thaumcraft:infusion/fortress_mask_ghost");
        ids.put("MaskGrinningDevil", "thaumcraft:infusion/fortress_mask_devil");
        ids.put("MaskSippingFiend", "thaumcraft:infusion/fortress_mask_fiend");
        ids.put("Mirror", "thaumcraft:infusion/mirror");
        ids.put("MirrorGlass", "thaumcraft:arcane/mirrored_glass");
        ids.put("MirrorHand", "thaumcraft:infusion/hand_mirror");
        ids.put("MnemonicMatrix", "thaumcraft:arcane/mnemonic_matrix");
        ids.put("MundaneAmulet", "thaumcraft:mundane_amulet");
        ids.put("MundaneBelt", "thaumcraft:mundane_belt");
        ids.put("MundaneRing", "thaumcraft:mundane_ring");
        ids.put("Nitor", "thaumcraft:crucible/nitor");
        ids.put("NodeChargeRelay", "thaumcraft:arcane/vis_charge_relay");
        ids.put("NodeRelay", "thaumcraft:arcane/vis_relay");
        ids.put("NodeStabilizer", "thaumcraft:arcane/node_stabilizer");
        ids.put("NodeTransducer", "thaumcraft:arcane/node_transducer");
        ids.put("PaveTravel", "thaumcraft:arcane/paving_stone_travel");
        ids.put("PaveWard", "thaumcraft:arcane/paving_stone_warding");
        ids.put("Phial", "thaumcraft:phial_empty");
        ids.put("PlankGreatwood", "thaumcraft:greatwood_planks");
        ids.put("PlankSilverwood", "thaumcraft:silverwood_planks");
        ids.put("PrimalCharm", "thaumcraft:arcane/primal_charm");
        ids.put("PrimalCrusher", "thaumcraft:infusion/primal_crusher");
        ids.put("PureGold", "thaumcraft:crucible/pure_gold");
        ids.put("PureIron", "thaumcraft:crucible/pure_iron");
        ids.put("Resonator", "thaumcraft:arcane/resonator");
        ids.put("RobeBoots", "thaumcraft:arcane/robe_boots");
        ids.put("RobeChest", "thaumcraft:arcane/robe_chestplate");
        ids.put("RobeLegs", "thaumcraft:arcane/robe_leggings");
        ids.put("RunicAmulet", "thaumcraft:infusion/runic_amulet");
        ids.put("RunicAmuletEmergency", "thaumcraft:infusion/runic_amulet_emergency");
        ids.put("RunicGirdle", "thaumcraft:infusion/runic_girdle");
        ids.put("RunicGirdleKinetic", "thaumcraft:infusion/runic_girdle_kinetic");
        ids.put("RunicRing", "thaumcraft:infusion/runic_ring");
        ids.put("RunicRingCharged", "thaumcraft:infusion/runic_ring_charged");
        ids.put("RunicRingHealing", "thaumcraft:infusion/runic_ring_healing");
        ids.put("SaneSoap", "thaumcraft:crucible/sanity_soap");
        ids.put("SanityCheck", "thaumcraft:infusion/sanity_checker");
        ids.put("Scribe1", "thaumcraft:scribing_tools_phial");
        ids.put("Scribe2", "thaumcraft:scribing_tools_bottle");
        ids.put("Scribe3", "thaumcraft:scribing_tools_refill");
        ids.put("SinStone", "thaumcraft:infusion/compass_stone");
        ids.put("Table", "thaumcraft:table");
        ids.put("Tallow", "thaumcraft:crucible/tallow");
        ids.put("TallowCandle", "thaumcraft:tallow_candle");
        ids.put("Thaumatorium", "thaumcraft:arcane/alchemical_construct");
        ids.put("Thaumium", "thaumcraft:crucible/thaumium");
        ids.put("ThaumiumAxe", "thaumcraft:thaumium_axe");
        ids.put("ThaumiumBoots", "thaumcraft:thaumium_boots");
        ids.put("ThaumiumChest", "thaumcraft:thaumium_chestplate");
        ids.put("ThaumiumFortressChest", "thaumcraft:infusion/fortress_chest");
        ids.put("ThaumiumFortressHelm", "thaumcraft:infusion/fortress_helm");
        ids.put("ThaumiumFortressLegs", "thaumcraft:infusion/fortress_legs");
        ids.put("ThaumiumHelm", "thaumcraft:thaumium_helmet");
        ids.put("ThaumiumHoe", "thaumcraft:thaumium_hoe");
        ids.put("ThaumiumLegs", "thaumcraft:thaumium_leggings");
        ids.put("ThaumiumPick", "thaumcraft:thaumium_pickaxe");
        ids.put("ThaumiumShovel", "thaumcraft:thaumium_shovel");
        ids.put("ThaumiumSword", "thaumcraft:thaumium_sword");
        ids.put("Thaumometer", "thaumcraft:thaumometer");
        ids.put("TinyArmor", "thaumcraft:arcane/golem_decoration_armor");
        ids.put("TinyBowtie", "thaumcraft:arcane/golem_decoration_bowtie");
        ids.put("TinyDart", "thaumcraft:arcane/golem_decoration_dart");
        ids.put("TinyFez", "thaumcraft:arcane/golem_decoration_fez");
        ids.put("TinyGlasses", "thaumcraft:arcane/golem_decoration_glasses");
        ids.put("TinyHammer", "thaumcraft:arcane/golem_decoration_mace");
        ids.put("TinyHat", "thaumcraft:arcane/golem_decoration_tophat");
        ids.put("TinyVisor", "thaumcraft:arcane/golem_decoration_visor");
        ids.put("TransGold", "thaumcraft:crucible/trans_gold");
        ids.put("TransIron", "thaumcraft:crucible/trans_iron");
        ids.put("TravelTrunk", "thaumcraft:infusion/traveling_trunk");
        ids.put("Tube", "thaumcraft:arcane/essentia_tube");
        ids.put("TubeBuffer", "thaumcraft:arcane/essentia_buffer");
        ids.put("TubeFilter", "thaumcraft:arcane/essentia_filter");
        ids.put("TubeOneway", "thaumcraft:arcane/essentia_oneway");
        ids.put("TubeRestrict", "thaumcraft:arcane/essentia_restrict");
        ids.put("TubeValve", "thaumcraft:arcane/essentia_valve");
        ids.put("UpgradeAir", "thaumcraft:arcane/golem_upgrade_air");
        ids.put("UpgradeEarth", "thaumcraft:arcane/golem_upgrade_earth");
        ids.put("UpgradeEntropy", "thaumcraft:arcane/golem_upgrade_entropy");
        ids.put("UpgradeFire", "thaumcraft:arcane/golem_upgrade_fire");
        ids.put("UpgradeOrder", "thaumcraft:arcane/golem_upgrade_order");
        ids.put("UpgradeWater", "thaumcraft:arcane/golem_upgrade_water");
        ids.put("VisAmulet", "thaumcraft:infusion/vis_amulet");
        ids.put("VoidAxe", "thaumcraft:void_axe");
        ids.put("VoidBoots", "thaumcraft:void_boots");
        ids.put("VoidChest", "thaumcraft:void_chestplate");
        ids.put("VoidHelm", "thaumcraft:void_helmet");
        ids.put("VoidHoe", "thaumcraft:void_hoe");
        ids.put("VoidLegs", "thaumcraft:void_leggings");
        ids.put("VoidMetal", "thaumcraft:crucible/void_ingot");
        ids.put("VoidPick", "thaumcraft:void_pickaxe");
        ids.put("VoidRobeChest", "thaumcraft:infusion/void_robe_chest");
        ids.put("VoidRobeHelm", "thaumcraft:infusion/void_robe_helm");
        ids.put("VoidRobeLegs", "thaumcraft:infusion/void_robe_legs");
        ids.put("VoidSeed", "thaumcraft:crucible/void_seed");
        ids.put("VoidShovel", "thaumcraft:void_shovel");
        ids.put("VoidSword", "thaumcraft:void_sword");
        ids.put("WandBasic", "thaumcraft:wand_wood_iron");
        ids.put("WandCapGold", "thaumcraft:arcane/wand_cap_gold");
        ids.put("WandCapIron", "thaumcraft:wand_cap_iron");
        ids.put("WandCapThaumium", "thaumcraft:infusion/wand_cap_thaumium");
        ids.put("WandCapThaumiumInert", "thaumcraft:arcane/wand_cap_thaumium_inert");
        ids.put("WandCapVoid", "thaumcraft:infusion/wand_cap_void");
        ids.put("WandCapVoidInert", "thaumcraft:arcane/wand_cap_void_inert");
        ids.put("WandPed", "thaumcraft:infusion/wand_pedestal");
        ids.put("WandRodBlaze", "thaumcraft:infusion/wand_rod_blaze");
        ids.put("WandRodBlazeStaff", "thaumcraft:arcane/staff_rod_blaze");
        ids.put("WandRodBone", "thaumcraft:infusion/wand_rod_bone");
        ids.put("WandRodBoneStaff", "thaumcraft:arcane/staff_rod_bone");
        ids.put("WandRodGreatwood", "thaumcraft:arcane/wand_rod_greatwood");
        ids.put("WandRodGreatwoodStaff", "thaumcraft:arcane/staff_rod_greatwood");
        ids.put("WandRodIce", "thaumcraft:infusion/wand_rod_ice");
        ids.put("WandRodIceStaff", "thaumcraft:arcane/staff_rod_ice");
        ids.put("WandRodObsidian", "thaumcraft:infusion/wand_rod_obsidian");
        ids.put("WandRodObsidianStaff", "thaumcraft:arcane/staff_rod_obsidian");
        ids.put("WandRodPrimalStaff", "thaumcraft:infusion/staff_rod_primal");
        ids.put("WandRodQuartz", "thaumcraft:infusion/wand_rod_quartz");
        ids.put("WandRodQuartzStaff", "thaumcraft:arcane/staff_rod_quartz");
        ids.put("WandRodReed", "thaumcraft:infusion/wand_rod_reed");
        ids.put("WandRodReedStaff", "thaumcraft:arcane/staff_rod_reed");
        ids.put("WandRodSilverwood", "thaumcraft:infusion/wand_rod_silverwood");
        ids.put("WandRodSilverwoodStaff", "thaumcraft:arcane/staff_rod_silverwood");
        ids.put("WardedGlass", "thaumcraft:arcane/warded_glass");
        ids.put("WardedJar", "thaumcraft:arcane/warded_jar");
        return Collections.unmodifiableMap(ids);
    }

    private static Map<String, String> buildConstructBlueprints() {
        Map<String, String> blueprints = new LinkedHashMap<>();
        blueprints.put("AdvAlchemyFurnace", "original 3x2x3 wand construct, anchored on the native arcane/advanced_alchemical_construct blueprint (ConfigRecipes:100)");
        blueprints.put("ArcTable", "original 1x2x1 wand construct: wand + arcane table (ConfigRecipes:85)");
        blueprints.put("Crucible", "original 1x2x1 wand construct: wand + bucket (ConfigRecipes:87)");
        blueprints.put("InfernalFurnace", "original 3x3x3 wand construct (ConfigRecipes:89)");
        blueprints.put("InfusionAltar", "original 3x3x3 wand construct, anchored on the native arcane/infusion_matrix blueprint (ConfigRecipes:93)");
        blueprints.put("NodeJar", "original 3x4x3 wand construct (ConfigRecipes:95)");
        blueprints.put("ResTable", "original 1x2x2 wand construct: wand + inkwell + two tables (ConfigRecipes:86)");
        blueprints.put("Thaumatorium", "original 1x3x1 wand construct, anchored on the native arcane/alchemical_construct blueprint (ConfigRecipes:97)");
        blueprints.put("Thaumonomicon", "original 1x2x1 wand construct: wand + book (ConfigRecipes:83)");
        return Collections.unmodifiableMap(blueprints);
    }

    private static Map<String, String> buildDeferred() {
        Map<String, String> reasons = new LinkedHashMap<>();
        reasons.put("ArcanePressurePlate", "the original result is blockWoodenDevice metadata 2, the arcane pressure plate; the native wooden_device family keeps it as a variant state and has no separate arcane pressure plate item, and a 1.21.1 recipe result cannot carry a block state");
        reasons.put("FluxScrubber", "the original result is blockStoneDevice metadata 14, the flux scrubber; the native stone_device family keeps it as a variant state and has no separate flux scrubber item, and a 1.21.1 recipe result cannot carry a block state");
        reasons.put("MirrorEssentia", "the original result is blockMirror metadata 6, the essentia mirror; the native mirror block keeps it as the essentia block state and has no separate essentia mirror item, and a 1.21.1 recipe result cannot carry a block state");
        reasons.put("NodeStabilizerAdv", "no native advanced node stabilizer; ThaumcraftNodeDevices registers only the ordinary node stabilizer");
        reasons.put("PureCopper", "third-party ore: the original registers this crucible recipe only when Config.foundCopperIngot, and 1.21.1 has no pure-copper item");
        reasons.put("PureLead", "third-party ore: the original registers this crucible recipe only when Config.foundLeadIngot, and 1.21.1 has no lead at all");
        reasons.put("PureSilver", "third-party ore: the original registers this crucible recipe only when Config.foundSilverIngot, and 1.21.1 has no silver at all");
        reasons.put("PureTin", "third-party ore: the original registers this crucible recipe only when Config.foundTinIngot, and 1.21.1 has no tin at all");
        reasons.put("TransCopper", "third-party ore: the original registers this crucible recipe only when Config.foundCopperIngot");
        reasons.put("TransLead", "third-party ore: the original registers this crucible recipe only when Config.foundLeadIngot");
        reasons.put("TransSilver", "third-party ore: the original registers this crucible recipe only when Config.foundSilverIngot");
        reasons.put("TransTin", "third-party ore: the original registers this crucible recipe only when Config.foundTinIngot");
        reasons.put("WandCapCopper", "no native copper wand cap item; the original needs a third-party copper ingot");
        reasons.put("WandCapSilver", "no native silver wand cap item; the original needs a third-party silver ingot");
        reasons.put("WandCapSilverInert", "no native silver wand cap item; the original needs a third-party silver ingot");
        reasons.put("WandPedFocus", "the original result is blockStoneDevice metadata 8, the focus that sits on the wand pedestal; the native wand_pedestal block keeps it as the focus block state and has no separate focus item, and a 1.21.1 recipe result cannot carry a block state");
        return Collections.unmodifiableMap(reasons);
    }

    /** Collects every packaged native recipe id, from an unpacked resource tree or from a jar. */
    private static Set<String> scanNativeRecipeIds() {
        Set<String> ids = new LinkedHashSet<>();
        ClassLoader loader = ResearchRecipeCoverage.class.getClassLoader();
        try {
            Enumeration<URL> roots = loader.getResources(RECIPE_ROOT);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                if ("file".equals(root.getProtocol())) {
                    try { collectFromDirectory(Path.of(root.toURI()), ids); }
                    catch (URISyntaxException exception) { throw new IllegalStateException("Cannot read native recipe tree", exception); }
                } else if ("jar".equals(root.getProtocol())) {
                    collectFromJar(root, ids);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot scan the native recipe tree", exception);
        }
        return Collections.unmodifiableSet(ids);
    }

    private static void collectFromDirectory(Path root, Set<String> ids) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".json") || !Files.isRegularFile(file)) continue;
                String relative = root.relativize(file).toString().replace('\\', '/');
                ids.add(NAMESPACE + ":" + relative.substring(0, relative.length() - ".json".length()));
            }
        }
    }

    private static void collectFromJar(URL root, Set<String> ids) throws IOException {
        JarURLConnection connection = (JarURLConnection) root.openConnection();
        connection.setUseCaches(false);
        try (JarFile jar = connection.getJarFile()) {
            String prefix = RECIPE_ROOT + "/";
            for (JarEntry entry : Collections.list(jar.entries())) {
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(prefix) || !name.endsWith(".json")) continue;
                ids.add(NAMESPACE + ":" + name.substring(prefix.length(), name.length() - ".json".length()));
            }
        }
    }
}
