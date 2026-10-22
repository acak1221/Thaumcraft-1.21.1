package thaumcraft.research;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.lib.research.ResearchRecipeCoverage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The original TC4 research corpus references 272 original recipe keys. This test pins the exact
 * catalogue coverage: every referenced key must resolve to a native recipe file or to one of the nine
 * original wand constructs, and the remainder must be exactly the documented list below.
 */
class ResearchRecipeCoverageTest {
    /**
     * The original recipes that still have no native recipe.
     *
     * <p>Two groups remain:
     * <ul>
     *   <li>{@link #THIRD_PARTY_ORE} - the eight original recipes the oracle registers only when another mod
     *       supplies the matching ore (Config.foundTinIngot, foundSilverIngot, foundLeadIngot).
     *       Vanilla 1.21.1 has no tin, no silver and no lead at all, so the original never registers them
     *       either. They stay deferred on purpose. Copper is vanilla, so its three original recipes
     *       (PureCopper, TransCopper and WandCapCopper) are carried by this port.</li>
     *   <li>The remaining entry is blocked on a native BLOCK that still does not exist; the reason for
     *       it is kept in {@link ResearchRecipeCoverage#deferredRecipes()}.</li>
     * </ul>
     */
    private static final List<String> EXPECTED_MISSING = List.of(
            "MirrorEssentia",
            "PureLead",
            "PureSilver",
            "PureTin",
            "TransLead",
            "TransSilver",
            "TransTin",
            "WandCapSilver",
            "WandCapSilverInert");

    /**
     * The original recipes that the oracle registers only when another mod supplies tin, silver or lead.
     * They are the part of the remainder that no amount of native content can close.
     */
    private static final List<String> THIRD_PARTY_ORE = List.of(
            "PureLead", "PureSilver", "PureTin",
            "TransLead", "TransSilver", "TransTin",
            "WandCapSilver", "WandCapSilverInert");

    /** The research entries whose pages still show an unresolved original recipe. */
    private static final List<String> EXPECTED_RESEARCH = List.of(
            "CAP_silver", "MIRRORESSENTIA", "PURELEAD", "PURESILVER", "PURETIN",
            "TRANSLEAD", "TRANSSILVER", "TRANSTIN");

    /** The original keys the corpus itself stores as a plain List, not as a recipe. */
    private static final List<String> EXPECTED_CONSTRUCTS = List.of(
            "AdvAlchemyFurnace", "ArcTable", "Crucible", "InfernalFurnace", "InfusionAltar", "NodeJar",
            "ResTable", "Thaumatorium", "Thaumonomicon");

    /**
     * The original recipes that produce ONE metadata value of a family block. The 1.21.1 port has one
     * item per metadata value, so they resolve instead of staying deferred.
     */
    private static final List<String> VARIANT_RECIPES = List.of("PaveTravel", "PaveWard", "WardedGlass",
            "ArcanePressurePlate", "FluxScrubber", "WandPedFocus");


    /**
     * Every original recipe key of the oracle {@code ConfigRecipes.java}, in the order of its
     * registration. The list is the machine-derived expansion of the 279 {@code put} call sites of
     * the oracle (the five loop generated families expanded). The catalogue has to be closed over
     * the three classes of the coverage map: a native recipe, an original wand construct, or a
     * documented deferral.
     */
    private static final List<String> ORIGINAL_CATALOGUE = List.of(
            "Thaumonomicon", "ArcTable", "ResTable", "Crucible", "InfernalFurnace", "InfusionAltar",
            "NodeJar", "Thaumatorium", "AdvAlchemyFurnace", "BalancedShard_0", "BalancedShard_1", "BalancedShard_2",
            "BalancedShard_3", "BalancedShard_4", "BalancedShard_5", "Alumentum", "Nitor", "Thaumium",
            "VoidMetal", "VoidSeed", "Tallow", "AltGunpowder", "AltSlime", "AltClay",
            "AltGlowstone", "AltInk", "AltWeb", "AltMossyCobble", "AltIce", "AltCrackedBrick",
            "AltBonemeal", "PureIron", "PureGold", "PureCopper", "PureTin", "PureSilver",
            "PureLead", "TransIron", "TransGold", "TransCopper", "TransTin", "TransSilver",
            "TransLead", "EtherealBloom", "LiquidDeath", "BottleTaint", "GolemStraw", "GolemWood",
            "GolemTallow", "GolemClay", "GolemFlesh", "GolemStone", "GolemIron", "GolemThaumium",
            "CoreGather", "CoreFill", "CoreEmpty", "CoreHarvest", "CoreGuard", "CoreButcher",
            "CoreLiquid", "BathSalts", "SaneSoap", "Banner_0", "Banner_1", "Banner_10",
            "Banner_11", "Banner_12", "Banner_13", "Banner_14", "Banner_15", "Banner_2",
            "Banner_3", "Banner_4", "Banner_5", "Banner_6", "Banner_7", "Banner_8",
            "Banner_9", "PrimalCharm", "ArcaneDoor", "WardedGlass", "IronKey", "FluxScrubber",
            "GoldKey", "ArcanePressurePlate", "NodeStabilizer", "NodeTransducer", "NodeRelay", "NodeChargeRelay",
            "FocalManipulator", "GolemFetter", "ArcaneStone1", "ArcaneStone2", "ArcaneStone3", "ArcaneStone4",
            "PaveTravel", "ArcaneLamp", "ArcaneSpa", "PaveWard", "Levitator", "ArcaneEar",
            "MirrorGlass", "BoneBow", "PrimalArrow_0", "PrimalArrow_1", "PrimalArrow_2", "PrimalArrow_3",
            "PrimalArrow_4", "PrimalArrow_5", "InfusionMatrix", "ArcanePedestal", "WardedJar", "JarVoid",
            "WandCapGold", "WandCapCopper", "WandCapSilverInert", "WandCapThaumiumInert", "WandCapVoidInert", "WandRodGreatwood",
            "WandRodGreatwoodStaff", "WandRodObsidianStaff", "WandRodSilverwoodStaff", "WandRodIceStaff", "WandRodQuartzStaff", "WandRodReedStaff",
            "WandRodBlazeStaff", "WandRodBoneStaff", "FocusFire", "FocusFrost", "FocusShock", "FocusTrade",
            "FocusExcavation", "FocusPrimal", "FocusPouch", "Deconstructor", "ArcaneBoreBase", "EnchantedFabric",
            "RobeChest", "RobeLegs", "RobeBoots", "Goggles", "HungryChest", "GolemBell",
            "CoreBlank", "UpgradeAir", "UpgradeEarth", "UpgradeFire", "UpgradeWater", "UpgradeOrder",
            "UpgradeEntropy", "TinyHat", "TinyFez", "TinyBowtie", "TinyGlasses", "TinyDart",
            "TinyVisor", "TinyArmor", "TinyHammer", "Filter", "AlchemyFurnace", "Alembic",
            "Bellows", "Tube", "Resonator", "TubeValve", "TubeFilter", "TubeRestrict",
            "TubeOneway", "TubeBuffer", "AlchemicalConstruct", "AdvAlchemyConstruct", "Centrifuge", "EssentiaCrystalizer",
            "MnemonicMatrix", "InfEnchRepair", "InfEnchHaste", "InfEnch0", "InfEnch1", "InfEnch2",
            "InfEnch3", "InfEnch4", "InfEnch5", "InfEnch6", "InfEnch7", "InfEnch8",
            "InfEnch9", "InfEnch10", "InfEnch11", "InfEnch12", "InfEnch13", "InfEnch14",
            "InfEnch15", "InfEnch16", "InfEnch17", "InfEnch18", "InfEnch19", "InfEnch20",
            "InfEnch21", "WandCapSilver", "WandCapThaumium", "WandCapVoid", "WandRodObsidian", "WandRodIce",
            "WandRodQuartz", "WandRodReed", "WandRodBlaze", "WandRodBone", "WandRodSilverwood", "WandRodPrimalStaff",
            "FocusHellbat", "FocusPortableHole", "FocusWarding", "WandPed", "WandPedFocus", "NodeStabilizerAdv",
            "JarBrain", "AdvancedGolem", "HoverHarness", "HoverGirdle", "VisAmulet", "RunicAmulet",
            "RunicAmuletEmergency", "RunicRing", "RunicRingCharged", "RunicRingHealing", "RunicGirdle", "RunicGirdleKinetic",
            "RunicGirdleKinetic_2", "Mirror", "MirrorHand", "MirrorEssentia", "ElementalAxe", "ElementalPick",
            "ElementalSword", "ElementalShovel", "ElementalHoe", "BootsTraveller", "CoreAlchemy", "CoreSorting",
            "CoreLumber", "CoreFishing", "CoreUse", "ArcaneBore", "TravelTrunk", "LampGrowth",
            "LampFertility", "ThaumiumFortressHelm", "ThaumiumFortressChest", "ThaumiumFortressLegs", "VoidRobeHelm", "VoidRobeChest",
            "VoidRobeLegs", "HelmGoggles", "MaskGrinningDevil", "MaskAngryGhost", "MaskSippingFiend", "SanityCheck",
            "EssentiaReservoir", "SinStone", "PrimalCrusher", "EldritchEye", "MundaneAmulet", "MundaneRing",
            "MundaneBelt", "JarLabel", "JarLabelNull", "WandBasic", "WandCapIron", "KnowFrag",
            "PlankGreatwood", "PlankSilverwood", "BlockFlesh", "BlockThaumium", "BlockTallow", "Clusters0",
            "Clusters1", "Clusters2", "Clusters3", "Clusters4", "Clusters5", "Clusters6",
            "Grate", "Phial", "Table", "Scribe1", "Scribe2", "Scribe3",
            "Thaumometer", "ThaumiumHelm", "ThaumiumChest", "ThaumiumLegs", "ThaumiumBoots", "ThaumiumShovel",
            "ThaumiumPick", "ThaumiumAxe", "ThaumiumHoe", "ThaumiumSword", "VoidHelm", "VoidChest",
            "VoidLegs", "VoidBoots", "VoidShovel", "VoidPick", "VoidAxe", "VoidHoe",
            "VoidSword", "TallowCandle");

    @BeforeAll
    static void load() { ResearchCatalog.loadBundled(); }

    @Test
    void everyOriginalRecipeKeyIsClassifiedAndTheRemainderIsPinned() {
        assertEquals(EXPECTED_MISSING, ResearchRecipeCoverage.missingRecipeKeys(),
                "The original recipe catalogue is no longer closed over the native registry; update this list.");
        assertEquals(EXPECTED_RESEARCH, ResearchRecipeCoverage.missingResearchKeys());
        assertEquals(Set.copyOf(EXPECTED_MISSING), ResearchRecipeCoverage.deferredRecipes().keySet(),
                "Every unresolved key needs exactly one documented reason.");
        for (String key : EXPECTED_MISSING) {
            assertFalse(ResearchRecipeCoverage.deferredRecipes().get(key).isBlank(), key + " needs a reason");
        }
    }

    /**
     * The catalogue of the original is closed: every one of the 308 original recipe keys is either a
     * native recipe, an original wand construct, or a documented deferral, and nothing else is
     * classified. The test pins the exact key set, so a key that disappears from the oracle, and a
     * key that appears in it, both fail here.
     */
    @Test
    void theWholeOriginalCatalogueIsClassified() {
        Set<String> classified = new TreeSet<>(ResearchRecipeCoverage.nativeRecipeIds().keySet());
        classified.addAll(ResearchRecipeCoverage.constructBlueprints().keySet());
        classified.addAll(ResearchRecipeCoverage.deferredRecipes().keySet());
        assertEquals(new TreeSet<>(ORIGINAL_CATALOGUE), classified,
                "The classified original keys are not the 308 keys of the oracle ConfigRecipes.");
        assertEquals(308, ORIGINAL_CATALOGUE.size());
        assertEquals(293, ResearchRecipeCoverage.nativeRecipeIds().size());
        assertEquals(9, ResearchRecipeCoverage.constructBlueprints().size());
        assertEquals(9, ResearchRecipeCoverage.deferredRecipes().size());
        // Every key of the catalogue is either resolved or has exactly one documented reason.
        for (String key : ORIGINAL_CATALOGUE) {
            boolean resolved = ResearchRecipeCoverage.isResolved(key);
            boolean deferred = ResearchRecipeCoverage.deferredRecipes().containsKey(key);
            assertTrue(resolved || deferred, key + " is neither resolved nor deferred");
            if (deferred) assertFalse(resolved, key + " must not be both resolved and deferred");
        }
    }

    @Test
    void theThirdPartyOreRecipesStayDeferredAndAreReportedAsSuch() {
        Map<String, String> deferred = ResearchRecipeCoverage.deferredRecipes();
        for (String key : THIRD_PARTY_ORE) {
            assertTrue(deferred.containsKey(key), key + " must stay deferred");
            assertTrue(deferred.get(key).contains("third-party ore"), key + " must report the third-party ore condition");
        }
        // The third-party ore group is the part of the remainder that no native content can close.
        assertTrue(THIRD_PARTY_ORE.containsAll(EXPECTED_MISSING.stream()
                .filter(key -> deferred.get(key).contains("third-party ore")).toList()));
    }

    @Test
    void everyMappedOriginalKeyResolvesToAPackagedNativeRecipe() {
        Map<String, String> ids = ResearchRecipeCoverage.nativeRecipeIds();
        assertEquals(293, ids.size());
        Set<String> present = ResearchRecipeCoverage.nativeRecipeIdsPresent();
        for (Map.Entry<String, String> entry : ids.entrySet()) {
            String id = entry.getValue();
            assertTrue(id.startsWith("thaumcraft:"), entry.getKey() + " -> " + id);
            assertTrue(present.contains(id), "Missing native recipe file for " + entry.getKey() + ": " + id);
            assertTrue(ResearchRecipeCoverage.isResolved(entry.getKey()), entry.getKey());
            assertEquals(id, ResearchRecipeCoverage.nativeRecipeId(entry.getKey()));
        }
        assertTrue(present.size() >= ids.size());
    }

    /**
     * The original recipes that the research book shows as an ARRAY of recipes: the sixteen banners
     * and the six primal arrows (ConfigRecipes:184-189 and :217-220). They carry no recipe key of
     * their own, but they have a native recipe each.
     */
    @Test
    void theBannersAndThePrimalArrowsAreMappedToTheirNativeRecipes() {
        Map<String, String> ids = ResearchRecipeCoverage.nativeRecipeIds();
        Set<String> present = ResearchRecipeCoverage.nativeRecipeIdsPresent();
        for (int index = 0; index < 16; index++) {
            String key = "Banner_" + index;
            assertTrue(ids.containsKey(key), key + " must be mapped to a native recipe");
            assertTrue(present.contains(ids.get(key)), key + " -> " + ids.get(key));
        }
        for (int index = 0; index < 6; index++) {
            String key = "PrimalArrow_" + index;
            assertTrue(ids.containsKey(key), key + " must be mapped to a native recipe");
            assertTrue(present.contains(ids.get(key)), key + " -> " + ids.get(key));
        }
        assertEquals(36, ResearchRecipeCoverage.outsideBookRecipeKeys().size());
        assertEquals(Set.of("BANNERS", "PRIMALARROW"), ResearchRecipeCoverage.pageRecipeArrays().keySet());
    }

    /**
     * The original registers fourteen more recipes that no research page names by key: the six
     * balanced shards (ConfigRecipes:89), the six aspect clusters and the mixed cluster
     * (ConfigRecipes:442 and :445) and the second kinetic runic girdle (ConfigRecipes:351).
     *
     * <p>The port shipped a native recipe for each of them, but the coverage map did not name the
     * original key, so the catalogue was not closed over the original. This test pins the mapping
     * and the native recipe of every one of them.</p>
     */
    @Test
    void theOriginalRecipesOutsideTheResearchBookAreMappedToTheirNativeRecipes() {
        Map<String, String> ids = ResearchRecipeCoverage.nativeRecipeIds();
        Set<String> present = ResearchRecipeCoverage.nativeRecipeIdsPresent();
        List<String> shards = List.of("air", "fire", "water", "earth", "order", "entropy");
        for (int index = 0; index < shards.size(); index++) {
            String key = "BalancedShard_" + index;
            assertTrue(ids.containsKey(key), key + " must be mapped to a native recipe");
            assertEquals("thaumcraft:crucible/balanced_shard_" + shards.get(index), ids.get(key));
            assertTrue(present.contains(ids.get(key)), key + " -> " + ids.get(key));
        }
        for (int index = 0; index < shards.size(); index++) {
            String key = "Clusters" + index;
            assertTrue(ids.containsKey(key), key + " must be mapped to a native recipe");
            assertEquals("thaumcraft:clusters/" + shards.get(index), ids.get(key));
            assertTrue(present.contains(ids.get(key)), key + " -> " + ids.get(key));
        }
        assertEquals("thaumcraft:clusters/mixed", ids.get("Clusters6"));
        assertTrue(present.contains(ids.get("Clusters6")));
        assertEquals("thaumcraft:infusion/runic_girdle_kinetic_2", ids.get("RunicGirdleKinetic_2"));
        assertTrue(present.contains(ids.get("RunicGirdleKinetic_2")));
        // All fourteen are outside the research book.
        Set<String> outside = ResearchRecipeCoverage.outsideBookRecipeKeys();
        for (int index = 0; index < shards.size(); index++) {
            assertTrue(outside.contains("BalancedShard_" + index));
            assertTrue(outside.contains("Clusters" + index));
        }
        assertTrue(outside.contains("Clusters6"));
        assertTrue(outside.contains("RunicGirdleKinetic_2"));
    }

    @Test
    void theNineOriginalWandConstructsAreExactlyTheCorpusListPages() {
        Set<String> corpusLists = new TreeSet<>();
        for (ResearchDefinition definition : ResearchCatalog.all()) {
            for (ResearchDefinition.Page page : definition.pages()) {
                if (page.recipeKey() == null) continue;
                String expression = page.javaExpression();
                if (expression != null && expression.startsWith("(List)recipes.get(")) corpusLists.add(page.recipeKey());
            }
        }
        assertEquals(Set.copyOf(EXPECTED_CONSTRUCTS), corpusLists,
                "The original stores those keys as plain lists, never as recipes.");
        assertEquals(Set.copyOf(EXPECTED_CONSTRUCTS), ResearchRecipeCoverage.constructBlueprints().keySet());
        for (String key : EXPECTED_CONSTRUCTS) {
            assertTrue(ResearchRecipeCoverage.isResolved(key), key);
            assertFalse(ResearchRecipeCoverage.deferredRecipes().containsKey(key), key);
        }
    }

    @Test
    void theCorpusReferenceCountsStayTheOriginalOnes() {
        Map<String, Integer> counts = ResearchRecipeCoverage.recipeKeyCounts();
        assertEquals(272, counts.size());
        assertEquals(273, counts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(2, counts.get("Thaumometer"));
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (!"Thaumometer".equals(entry.getKey())) assertEquals(1, entry.getValue(), entry.getKey());
        }
    }

    @Test
    void everyCorpusRecipeKeyIsMappedConstructedOrDeferred() {
        Set<String> classified = new TreeSet<>(ResearchRecipeCoverage.nativeRecipeIds().keySet());
        classified.addAll(ResearchRecipeCoverage.constructBlueprints().keySet());
        classified.addAll(ResearchRecipeCoverage.deferredRecipes().keySet());
        Set<String> corpus = new TreeSet<>(ResearchRecipeCoverage.recipeKeyCounts().keySet());
        // The only original recipes outside the corpus are the ones the book shows as an array.
        Set<String> outside = new TreeSet<>(classified);
        outside.removeAll(corpus);
        assertEquals(ResearchRecipeCoverage.outsideBookRecipeKeys(), outside);
        assertTrue(corpus.stream().allMatch(classified::contains));
    }

    @Test
    void everyMappedNativeRecipeKeepsARealResearchGate() {
        Set<String> corpusResearch = new TreeSet<>();
        for (ResearchDefinition definition : ResearchCatalog.all()) corpusResearch.add(definition.key());
        Map<String, String> gates = ResearchRecipeCoverage.nativeResearchGates();
        assertEquals(ResearchRecipeCoverage.nativeRecipeIds().keySet(), gates.keySet());
        for (Map.Entry<String, String> entry : gates.entrySet()) {
            String gate = entry.getValue();
            if (gate == null) continue;
            assertTrue(corpusResearch.contains(gate),
                    entry.getKey() + " -> " + gate + " is not a research key of the original corpus");
        }
    }

    @Test
    void onlyOrdinaryCraftingRecipesStayWithoutAResearchGate() throws Exception {
        Map<String, String> expected = new java.util.TreeMap<>();
        try (var stream = getClass().getResourceAsStream("/thaumcraft/audit/recipe-gates.txt")) {
            assertNotNull(stream);
            for (String line : new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().toList()) {
                if (line.startsWith("#") || line.isBlank()) continue;
                String[] fields = line.split("=", 2);
                for (String key : fields[1].split(",")) assertNull(expected.put(key, fields[0]), "duplicate fixture " + key);
            }
        }
        assertEquals(244, expected.size(), "nonempty, frozen original gate corpus");
        Set<String> gatedTypes = Set.of("thaumcraft:arcane_shaped", "thaumcraft:arcane_shapeless",
                "thaumcraft:crucible", "thaumcraft:infusion_stack", "thaumcraft:infusion_enchantment",
                "thaumcraft:infusion_runic_augment");
        Set<String> actualGated = new TreeSet<>();
        for (Map.Entry<String, String> entry : ResearchRecipeCoverage.nativeRecipeIds().entrySet()) {
            String type = recipeType(entry.getValue());
            String gate = ResearchRecipeCoverage.nativeResearchGate(entry.getKey());
            if (gatedTypes.contains(type)) {
                actualGated.add(entry.getKey());
                assertTrue(expected.containsKey(entry.getKey()), "unreviewed gated recipe " + entry.getKey());
                assertEquals(expected.get(entry.getKey()), gate, entry.getKey());
            } else {
                assertNull(gate, "ordinary recipe " + entry.getKey());
            }
        }
        assertEquals(expected.keySet(), actualGated, "removing mappings must not make this pass vacuously");
    }

    private static String recipeType(String recipeId) {
        String path = recipeId.substring(recipeId.indexOf(':') + 1);
        try (var stream = ResearchRecipeCoverageTest.class.getClassLoader()
                .getResourceAsStream("data/thaumcraft/recipe/" + path + ".json")) {
            assertNotNull(stream, "Missing native recipe file " + recipeId);
            return com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject().get("type").getAsString();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read " + recipeId, exception);
        }
    }

    @Test
    void theRecipesThatProduceOneMetadataValueResolveToTheirVariantItem() {
        Map<String, String> ids = ResearchRecipeCoverage.nativeRecipeIds();
        Set<String> present = ResearchRecipeCoverage.nativeRecipeIdsPresent();
        for (String key : VARIANT_RECIPES) {
            assertTrue(ids.containsKey(key), key + " must be mapped to a native recipe");
            assertFalse(ResearchRecipeCoverage.deferredRecipes().containsKey(key), key + " must not stay deferred");
            assertTrue(present.contains(ids.get(key)), key + " -> " + ids.get(key));
            assertTrue(ResearchRecipeCoverage.isResolved(key), key);
        }
        assertEquals("thaumcraft:arcane/paving_stone_travel", ids.get("PaveTravel"));
        assertEquals("thaumcraft:arcane/paving_stone_warding", ids.get("PaveWard"));
        assertEquals("thaumcraft:arcane/warded_glass", ids.get("WardedGlass"));
        assertEquals("thaumcraft:arcane/arcane_pressure_plate", ids.get("ArcanePressurePlate"));
        assertEquals("thaumcraft:arcane/flux_scrubber", ids.get("FluxScrubber"));
        assertEquals("thaumcraft:infusion/wand_pedestal_focus", ids.get("WandPedFocus"));
    }

    /**
     * The copper content of the original is carried because copper is vanilla in 1.21.1: the copper
     * nugget, the native copper cluster, the copper wand cap and their three original recipes.
     */
    @Test
    void theVanillaCopperContentOfTheOriginalIsCarried() {
        Map<String, String> ids = ResearchRecipeCoverage.nativeRecipeIds();
        assertEquals("thaumcraft:arcane/wand_cap_copper", ids.get("WandCapCopper"));
        assertEquals("thaumcraft:crucible/pure_copper", ids.get("PureCopper"));
        assertEquals("thaumcraft:crucible/trans_copper", ids.get("TransCopper"));
        for (String key : List.of("WandCapCopper", "PureCopper", "TransCopper")) {
            assertFalse(ResearchRecipeCoverage.deferredRecipes().containsKey(key), key + " must not stay deferred");
            assertTrue(ResearchRecipeCoverage.isResolved(key), key);
        }
    }

    @Test
    void theReportNamesTheUnresolvedKeysAndTheirReasons() {
        String report = ResearchRecipeCoverage.report();
        assertTrue(report.contains("TC4 recipe coverage:"), report);
        assertTrue(report.contains("Unresolved original recipe keys:"), report);
        for (String key : EXPECTED_MISSING) {
            assertTrue(report.contains(key), "Missing from the report: " + key);
        }
    }
}
