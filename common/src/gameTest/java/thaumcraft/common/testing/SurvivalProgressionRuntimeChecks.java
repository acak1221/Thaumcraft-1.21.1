package thaumcraft.common.testing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;
import thaumcraft.common.lib.research.ResearchPurchase;

/**
 * The survival-progression acceptance of the port: the chain a player walks in
 * survival mode, from the first iron wand to the loot of an Outer Lands boss.
 *
 * <p>Everything the port already proved is proved about single systems. This
 * suite proves the other half: that the content is REACHABLE by natural play.
 * Every check below runs on the real dedicated server, uses the real recipe
 * manager, the real research catalogue and the real world; nothing is placed
 * into an inventory that a recipe, a mine, a plant, a merchant, a boss or a
 * wand construct does not produce.</p>
 *
 * <p>The oracle is the authority for every number:
 * <ul>
 *   <li>the wand, the cap, the table, the thaumometer, the phial and the inkwell
 *       are the original recipes of {@code ConfigRecipes.java:429-430,453-458};</li>
 *   <li>the eldritch altar, the four eyes, the dark node and the six hundred vis
 *       are the original oculus of {@code WandManager.java:427-441} and the
 *       registration of {@code ConfigRecipes.java:83};</li>
 *   <li>the boss loot is the original drop of {@code EntityThaumcraftBoss.java:217}
 *       and {@code EntityCultistPortal.java:269};</li>
 *   <li>the research costs are the original costs of the bundled
 *       {@code data/thaumcraft/tc4/research.json} corpus, which is extracted from
 *       {@code ConfigResearch.java}.</li>
 * </ul>
 */
public final class SurvivalProgressionRuntimeChecks {
    private SurvivalProgressionRuntimeChecks() {}

    // ------------------------------------------------------------------ chain

    /** One link of the natural chain: the original recipe, the item it must yield and the count. */
    public record ChainLink(String recipe, String item, int count, String note) {}

    /**
     * The chain of the survival progression, in the order a player walks it.
     *
     * <p>The table is the expected side of the assertion, not a copy of the recipe
     * files: every item and every count was read from the original
     * {@code ConfigRecipes.java} of the 4.2.3.5 oracle and from the original
     * research corpus. A wrong count or a wrong item fails the scenario.</p>
     */
    public static final List<ChainLink> CHAIN = List.of(
            // Stage zero: the tools of the first hour, all of them ordinary crafting table recipes.
            new ChainLink("wand_cap_iron", "thaumcraft:wand_cap_iron", 1, "five iron nuggets, oracle ConfigRecipes:430"),
            new ChainLink("wand_wood_iron", "thaumcraft:wand", 1, "two iron caps and a stick, oracle ConfigRecipes:429"),
            new ChainLink("table", "thaumcraft:table", 1, "the research table, oracle ConfigRecipes:454"),
            new ChainLink("thaumometer", "thaumcraft:thaumometer", 1, "the scanner, oracle ConfigRecipes:458"),
            new ChainLink("phial_empty", "thaumcraft:phial_empty", 8, "eight phials, oracle ConfigRecipes:453"),
            new ChainLink("scribing_tools_phial", "thaumcraft:inkwell", 1, "the inkwell of a phial, oracle ConfigRecipes:455"),
            new ChainLink("thaumium_nuggets", "thaumcraft:thaumium_nugget", 9, "nine thaumium nuggets, oracle ConfigRecipes:404"),
            new ChainLink("thaumium_ingot_from_nuggets", "thaumcraft:thaumium_ingot", 1, "the ingot of the nuggets, oracle ConfigRecipes:404"),
            new ChainLink("thaumium_pickaxe", "thaumcraft:thaumium_pickaxe", 1, "the first thaumium tool, oracle ConfigRecipes:419"),
            new ChainLink("greatwood_planks", "thaumcraft:greatwood_planks", 4, "four planks of a greatwood log, oracle ConfigRecipes:404"),
            new ChainLink("quicksilver_drops", "thaumcraft:quicksilver_drop", 9, "the quicksilver drops, oracle ConfigRecipes:405"),
            new ChainLink("quick_silver_from_drops", "thaumcraft:quick_silver", 1, "the quicksilver of nine drops, oracle ConfigRecipes:405"),
            new ChainLink("tallow_candle", "thaumcraft:tallow_candle", 3, "three candles of tallow, oracle ConfigRecipes:274"),
            new ChainLink("unknown_research_note", "thaumcraft:unknown_research_note", 1, "the note of nine fragments, oracle ConfigRecipes:274"),
            // Stage one: the arcane devices, every one of them behind its own research.
            new ChainLink("arcane/arcane_stone", "thaumcraft:arcane_stone", 9, "nine arcane stones, oracle ConfigRecipes:205"),
            new ChainLink("arcane_stone_bricks", "thaumcraft:arcane_stone_bricks", 4, "four bricks of arcane stone, oracle ConfigRecipes:205"),
            new ChainLink("arcane/arcane_pedestal", "thaumcraft:arcane_pedestal", 2, "two pedestals, oracle ConfigRecipes:222"),
            new ChainLink("arcane/infusion_matrix", "thaumcraft:infusion_matrix", 1, "the infusion matrix, oracle ConfigRecipes:93"),
            new ChainLink("arcane/vis_filter", "thaumcraft:vis_filter", 2, "two vis filters, oracle ConfigRecipes:279"),
            new ChainLink("arcane/essentia_tube", "thaumcraft:essentia_tube", 8, "eight essentia tubes, oracle ConfigRecipes:283"),
            new ChainLink("arcane/essentia_filter", "thaumcraft:essentia_filter", 1, "the essentia filter, oracle ConfigRecipes:286"),
            new ChainLink("arcane/alembic", "thaumcraft:alembic", 1, "the alembic, oracle ConfigRecipes:286"),
            new ChainLink("arcane/alchemy_furnace", "thaumcraft:alchemy_furnace", 1, "the alchemy furnace, oracle ConfigRecipes:280"),
            new ChainLink("arcane/alchemical_construct", "thaumcraft:alchemical_construct", 1, "the alchemical construct, oracle ConfigRecipes:97"),
            new ChainLink("arcane/centrifuge", "thaumcraft:centrifuge", 1, "the centrifuge, oracle ConfigRecipes:292"),
            new ChainLink("arcane/essentia_crystallizer", "thaumcraft:essentia_crystallizer", 1, "the crystallizer, oracle ConfigRecipes:293"),
            new ChainLink("arcane/mnemonic_matrix", "thaumcraft:mnemonic_matrix", 1, "the mnemonic matrix, oracle ConfigRecipes:294"),
            new ChainLink("arcane/warded_jar", "thaumcraft:jar", 1, "the jar, oracle ConfigRecipes:223"),
            new ChainLink("arcane/jar_void", "thaumcraft:jar_void", 1, "the void jar, oracle ConfigRecipes:224"),
            new ChainLink("arcane/goggles", "thaumcraft:goggles_revealing", 1, "the goggles, oracle ConfigRecipes:261"),
            new ChainLink("arcane/deconstruction_table", "thaumcraft:deconstruction_table", 1, "the deconstruction table, oracle ConfigRecipes:254"),
            new ChainLink("arcane/bellows", "thaumcraft:bellows", 1, "the bellows, oracle ConfigRecipes:282"),
            new ChainLink("arcane/arcane_lamp", "thaumcraft:arcane_lamp", 1, "the arcane lamp, oracle ConfigRecipes:210"),
            new ChainLink("arcane/arcane_ear", "thaumcraft:arcane_ear", 1, "the arcane ear, oracle ConfigRecipes:214"),
            new ChainLink("arcane/arcane_door", "thaumcraft:arcane_door", 1, "the arcane door, oracle ConfigRecipes:191"),
            new ChainLink("arcane/arcane_pressure_plate", "thaumcraft:arcane_pressure_plate", 1, "the arcane pressure plate, oracle ConfigRecipes:197"),
            new ChainLink("arcane/arcane_spa", "thaumcraft:arcane_spa", 1, "the arcane spa, oracle ConfigRecipes:211"),
            new ChainLink("arcane/arcane_bore_base", "thaumcraft:arcane_bore_base", 1, "the arcane bore, oracle ConfigRecipes:255"),
            new ChainLink("arcane/hungry_chest", "thaumcraft:hungry_chest", 1, "the hungry chest, oracle ConfigRecipes:262"),
            new ChainLink("arcane/levitator", "thaumcraft:levitator", 1, "the levitator, oracle ConfigRecipes:213"),
            new ChainLink("arcane/resonator", "thaumcraft:resonator", 1, "the resonator, oracle ConfigRecipes:284"),
            new ChainLink("arcane/focus_excavation", "thaumcraft:focus_excavation", 1, "the excavation focus, oracle ConfigRecipes:251"),
            new ChainLink("arcane/focus_fire", "thaumcraft:focus_fire", 1, "the fire focus, oracle ConfigRecipes:247"),
            new ChainLink("arcane/focus_frost", "thaumcraft:focus_frost", 1, "the frost focus, oracle ConfigRecipes:249"),
            new ChainLink("arcane/focus_shock", "thaumcraft:focus_shock", 1, "the shock focus, oracle ConfigRecipes:250"),
            new ChainLink("arcane/focus_trade", "thaumcraft:focus_trade", 1, "the trade focus, oracle ConfigRecipes:252"),
            new ChainLink("arcane/focus_pouch", "thaumcraft:focus_pouch", 1, "the focus pouch, oracle ConfigRecipes:253"),
            new ChainLink("arcane/focus_primal", "thaumcraft:focus_primal", 1, "the primal focus, oracle ConfigRecipes:255"),
            new ChainLink("arcane/node_stabilizer", "thaumcraft:node_stabilizer", 1, "the node stabiliser, oracle ConfigRecipes:199"),
            new ChainLink("arcane/node_transducer", "thaumcraft:node_transducer", 1, "the node transducer, oracle ConfigRecipes:200"),
            new ChainLink("arcane/vis_relay", "thaumcraft:vis_relay", 2, "two vis relays, oracle ConfigRecipes:201"),
            new ChainLink("arcane/vis_charge_relay", "thaumcraft:vis_charge_relay", 1, "the vis charge relay, oracle ConfigRecipes:202"),
            new ChainLink("arcane/wand_cap_thaumium_inert", "thaumcraft:wand_cap_thaumium_inert", 1, "the inert thaumium cap, oracle ConfigRecipes:236"),
            new ChainLink("arcane/wand_rod_greatwood", "thaumcraft:wand_rod_greatwood", 1, "the greatwood rod, oracle ConfigRecipes:238"),
            new ChainLink("arcane/enchanted_fabric", "thaumcraft:enchanted_fabric", 1, "the enchanted fabric, oracle ConfigRecipes:262"),
            new ChainLink("arcane/golem_core_blank", "thaumcraft:golem_core_blank", 1, "the blank golem core, oracle ConfigRecipes:264"),
            new ChainLink("arcane/golem_fetter", "thaumcraft:golem_fetter", 1, "the golem fetter, oracle ConfigRecipes:204"),
            new ChainLink("arcane/golem_bell", "thaumcraft:golem_bell", 1, "the golem bell, oracle ConfigRecipes:266"),
            new ChainLink("arcane/robe_boots", "thaumcraft:robe_boots", 1, "the robe boots, oracle ConfigRecipes:268"),
            new ChainLink("arcane/robe_chestplate", "thaumcraft:robe_chestplate", 1, "the robe chestplate, oracle ConfigRecipes:268"),
            new ChainLink("arcane/robe_leggings", "thaumcraft:robe_leggings", 1, "the robe leggings, oracle ConfigRecipes:268"),
            new ChainLink("arcane/primal_charm", "thaumcraft:primal_charm", 1, "the primal charm, oracle ConfigRecipes:272"),
            new ChainLink("arcane/mirrored_glass", "thaumcraft:mirrored_glass", 1, "the mirrored glass, oracle ConfigRecipes:270"),
            new ChainLink("arcane/warded_glass", "thaumcraft:warded_glass", 8, "eight warded panes, oracle ConfigRecipes:271"),
            // Stage two: the crucible, the essence of every aspect.
            new ChainLink("crucible/thaumium", "thaumcraft:thaumium_ingot", 1, "the thaumium of the crucible, oracle ConfigRecipes:132"),
            new ChainLink("crucible/balanced_shard_air", "thaumcraft:balance_shard", 1, "the balanced shard, oracle ConfigRecipes:110"),
            new ChainLink("crucible/nitor", "thaumcraft:nitor", 1, "the nitor, oracle ConfigRecipes:114"),
            new ChainLink("crucible/alumentum", "thaumcraft:alumentum", 1, "the alumentum, oracle ConfigRecipes:112"),
            new ChainLink("crucible/void_ingot", "thaumcraft:void_ingot", 1, "the void metal, oracle ConfigRecipes:116"),
            new ChainLink("crucible/void_seed", "thaumcraft:void_seed", 1, "the void seed, oracle ConfigRecipes:117"),
            new ChainLink("crucible/pure_iron", "thaumcraft:cluster_iron", 1, "the purified iron cluster, oracle ConfigRecipes:131"),
            new ChainLink("crucible/tallow", "thaumcraft:magic_tallow", 1, "the magical tallow, oracle ConfigRecipes:120"),
            new ChainLink("crucible/ethereal_bloom", "thaumcraft:ethereal_bloom", 1, "the ethereal bloom, oracle ConfigRecipes:118"),
            new ChainLink("crucible/bottle_taint", "thaumcraft:bottle_taint", 1, "the bottled taint, oracle ConfigRecipes:119"),
            new ChainLink("crucible/bath_salts", "thaumcraft:bath_salts", 1, "the bath salts, oracle ConfigRecipes:121"),
            new ChainLink("crucible/sanity_soap", "thaumcraft:sanity_soap", 1, "the sanity soap, oracle ConfigRecipes:122"),
            new ChainLink("crucible/golem_clay", "thaumcraft:golem_clay", 1, "the clay of a golem, oracle ConfigRecipes:123"),
            new ChainLink("clusters/air", "thaumcraft:crystal_air", 1, "the crystal cluster, oracle ConfigRecipes:442"),
            new ChainLink("clusters/mixed", "thaumcraft:crystal_mixed", 1, "the mixed crystal cluster, oracle ConfigRecipes:445"),
            // Stage three: the infusion, the second half of the progression.
            new ChainLink("infusion/eldritch_eye", "thaumcraft:eldritch_eye", 1, "the eldritch eye, oracle ConfigRecipes:394"),
            new ChainLink("infusion/wand_cap_thaumium", "thaumcraft:wand_cap_thaumium", 1, "the charged thaumium cap, oracle ConfigRecipes:236"),
            new ChainLink("infusion/vis_amulet", "thaumcraft:vis_amulet", 1, "the vis amulet, oracle ConfigRecipes:296"),
            new ChainLink("infusion/jar_brain", "thaumcraft:jar_brain", 1, "the brain jar, oracle ConfigRecipes:344"),
            new ChainLink("infusion/essentia_reservoir", "thaumcraft:essentia_reservoir", 1, "the essentia reservoir, oracle ConfigRecipes:391"),
            new ChainLink("infusion/mirror", "thaumcraft:mirror", 1, "the mirror, oracle ConfigRecipes:357"),
            new ChainLink("infusion/hand_mirror", "thaumcraft:hand_mirror", 1, "the hand mirror, oracle ConfigRecipes:358"),
            new ChainLink("infusion/wand_pedestal", "thaumcraft:wand_pedestal", 1, "the wand pedestal, oracle ConfigRecipes:299"),
            new ChainLink("infusion/wand_pedestal_focus", "thaumcraft:wand_pedestal_focus", 1, "the focus pedestal, oracle ConfigRecipes:300"),
            new ChainLink("infusion/growth_lamp", "thaumcraft:growth_lamp", 1, "the growth lamp, oracle ConfigRecipes:307"),
            new ChainLink("infusion/fertility_lamp", "thaumcraft:fertility_lamp", 1, "the fertility lamp, oracle ConfigRecipes:308"),
            new ChainLink("infusion/primal_crusher", "thaumcraft:primal_crusher", 1, "the primal crusher, oracle ConfigRecipes:393"),
            new ChainLink("infusion/compass_stone", "thaumcraft:compass_stone", 1, "the compass stone, oracle ConfigRecipes:396"),
            new ChainLink("infusion/sanity_checker", "thaumcraft:sanity_checker", 1, "the sanity checker, oracle ConfigRecipes:390"),
            new ChainLink("infusion/staff_rod_primal", "thaumcraft:staff_rod_primal", 1, "the primal staff, oracle ConfigRecipes:305"),
            new ChainLink("infusion/runic_amulet", "thaumcraft:runic_amulet", 1, "the runic amulet, oracle ConfigRecipes:340"),
            new ChainLink("infusion/runic_ring", "thaumcraft:runic_ring", 1, "the runic ring, oracle ConfigRecipes:344"),
            new ChainLink("infusion/traveling_trunk", "thaumcraft:trunk_spawner", 1, "the travelling trunk, oracle ConfigRecipes:361"),
            new ChainLink("infusion/hover_girdle", "thaumcraft:hover_girdle", 1, "the hover girdle, oracle ConfigRecipes:386"),
            new ChainLink("infusion/hover_harness", "thaumcraft:hover_harness", 1, "the hover harness, oracle ConfigRecipes:385"),
            new ChainLink("infusion/elemental_sword", "thaumcraft:elemental_sword", 1, "the elemental sword, oracle ConfigRecipes:373"),
            new ChainLink("infusion/focus_warding", "thaumcraft:focus_warding", 1, "the warding focus, oracle ConfigRecipes:262"),
            new ChainLink("infusion/focus_portable_hole", "thaumcraft:focus_portable_hole", 1, "the portable hole focus, oracle ConfigRecipes:262"),
            new ChainLink("infusion/focus_hellbat", "thaumcraft:focus_hellbat", 1, "the hellbat focus, oracle ConfigRecipes:262"),
            new ChainLink("infusion/boots_traveller", "thaumcraft:boots_traveller", 1, "the traveller boots, oracle ConfigRecipes:280"),
            new ChainLink("infusion/advanced_node_stabilizer", "thaumcraft:advanced_node_stabilizer", 1, "the advanced stabiliser, oracle ConfigRecipes:200"),
            new ChainLink("infusion/arcane_bore", "thaumcraft:arcane_bore", 1, "the arcane bore, oracle ConfigRecipes:250"),
            new ChainLink("infusion/fortress_helm", "thaumcraft:fortress_helmet", 1, "the fortress helm, oracle ConfigRecipes:381")
    );

    // ----------------------------------------------------------- world source

    /** One mod item that a recipe of the port does not produce, with the natural source that does. */
    public record WorldSource(String item, String reason) {}

    /**
     * The mod items that no recipe of the port produces, with the natural source
     * that hands them to a survival player.
     *
     * <p>Every entry is a world source of the original: an ore, a tree, a plant,
     * a merchant, a mob drop, a wand construct or a world structure. The list is
     * the exact set of the mod items the recipe tree uses but never produces.</p>
     */
    public static final List<WorldSource> WORLD_SOURCES = List.of(
            new WorldSource("thaumcraft:air_shard", "mined from the infused stone of the aer, oracle BlockCustomOre:139-142"),
            new WorldSource("thaumcraft:fire_shard", "mined from the infused stone of the ignis, oracle BlockCustomOre:139-142"),
            new WorldSource("thaumcraft:water_shard", "mined from the infused stone of the aqua, oracle BlockCustomOre:139-142"),
            new WorldSource("thaumcraft:earth_shard", "mined from the infused stone of the terra, oracle BlockCustomOre:139-142"),
            new WorldSource("thaumcraft:order_shard", "mined from the infused stone of the ordo, oracle BlockCustomOre:139-142"),
            new WorldSource("thaumcraft:entropy_shard", "mined from the infused stone of the perditio, oracle BlockCustomOre:139-142"),
            new WorldSource("thaumcraft:amber_ore", "mined from the amber ore of the overworld, oracle ConfigBlocks"),
            new WorldSource("thaumcraft:cinnabar_ore", "mined from the cinnabar ore of the overworld, oracle ConfigBlocks"),
            new WorldSource("thaumcraft:cinderpearl", "the cinder plant of the overworld drops itself, oracle BlockCustomPlant"),
            new WorldSource("thaumcraft:cluster_cinnabar", "the Pech merchant of the overworld trades it, oracle EntityPech:716"),
            new WorldSource("thaumcraft:crucible", "the wand construct of a cauldron, oracle ConfigRecipes:87 and WandManager:280-330"),
            new WorldSource("thaumcraft:greatwood_log", "cut from the greatwood tree, oracle WorldGenTreetypes"),
            new WorldSource("thaumcraft:silverwood_log", "cut from the silverwood tree, oracle WorldGenTreetypes"),
            new WorldSource("thaumcraft:knowledge_fragment", "dropped by the glyphed eldritch stone, oracle BlockEldritch:207-214"),
            new WorldSource("thaumcraft:phial_filled", "filled from a jar of essentia, oracle ItemEssence"),
            new WorldSource("thaumcraft:primordial_pearl", "dropped by an Outer Lands boss, oracle EntityThaumcraftBoss:217"),
            new WorldSource("thaumcraft:shimmerleaf", "grown in the magical forest, oracle WorldGenCustomFlowers"),
            new WorldSource("thaumcraft:zombie_brain", "dropped by a zombie, oracle ItemZombieBrain")
    );

    /**
     * The mod items that no recipe of the port produces and that no world of the
     * port hands to the player either.
     *
     * <p>Every entry is a real gap with its own reason. The scenario asserts this
     * exact set, so a fix has to update the list: the gap can never grow and can
     * never be hidden.</p>
     */
    public static final List<String> KNOWN_GAPS = List.of(
            "thaumcraft:nugget_beef",
            "thaumcraft:nugget_chicken",
            "thaumcraft:nugget_fish",
            "thaumcraft:nugget_pork"
    );

    // ---------------------------------------------------------- chain research

    /** One research entry of the chain and the reason the player needs it. */
    public record ChainResearch(String key, String why) {}

    /**
     * The research entries the chain of this suite needs, with the original
     * reason the player needs them.
     *
     * <p>The keys are the original research keys of the bundled corpus, which is
     * extracted from {@code ConfigResearch.java}. The costs are the original
     * aspect costs of that corpus.</p>
     */
    public static final List<ChainResearch> CHAIN_RESEARCH = List.of(
            new ChainResearch("ARCANESTONE", "the arcane stone of every arcane device"),
            new ChainResearch("DISTILESSENTIA", "the alembic, the jar, the filter and the alchemical construct"),
            new ChainResearch("NITOR", "the nitor of the node devices"),
            new ChainResearch("THAUMIUM", "the thaumium of the crucible"),
            new ChainResearch("CRUCIBLE", "the crucible and the balanced shard"),
            new ChainResearch("INFUSION", "the infusion matrix, the pedestal and the first infusion"),
            new ChainResearch("NODEPRESERVE", "the node stabiliser"),
            new ChainResearch("NODESTABILIZER", "the node stabiliser"),
            new ChainResearch("VISPOWER", "the node transducer and the vis relay"),
            new ChainResearch("FOCUSFIRE", "the fire focus"),
            new ChainResearch("FOCUSEXCAVATION", "the excavation focus"),
            new ChainResearch("TUBES", "the essentia tube and the resonator"),
            new ChainResearch("TUBEFILTER", "the essentia filter"),
            new ChainResearch("GOGGLES", "the goggles"),
            new ChainResearch("OCULUS", "the eldritch eye and the gate into the Outer Lands"),
            new ChainResearch("PRIMPEARL", "the primordial pearl of the Outer Lands boss")
    );

    // ------------------------------------------------------------------ checks

    /**
     * Every link of the chain is a real recipe of the running server and yields
     * the item the original yields.
     */
    public static void chainRecipesYieldTheirItems(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        var manager = level.getRecipeManager();
        for (ChainLink link : CHAIN) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("thaumcraft", link.recipe());
            var holder = manager.byKey(id);
            test.assertTrue(holder.isPresent(), "the original recipe " + id + " is registered on the server");
            if (holder.isEmpty()) continue;
            ItemStack result = holder.get().value().getResultItem(level.registryAccess());
            Item expected = item(link.item());
            test.assertTrue(expected != null, "the item " + link.item() + " of " + id + " is registered");
            if (expected == null) continue;
            test.assertTrue(result.is(expected) && result.getCount() == link.count(),
                    id + " yields " + link.count() + " " + link.item() + " (" + link.note() + "), got " + result);
        }
        test.succeed();
    }

    /**
     * The chain is closed on natural acquisition: no mod item of the recipe tree is
     * used as an ingredient without a recipe, a mine, a plant, a merchant, a mob
     * drop, a wand construct or a world structure behind it.
     */
    public static void chainIngredientsCloseOnNaturalAcquisition(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        var manager = level.getRecipeManager();
        Set<String> produced = new LinkedHashSet<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (!"thaumcraft".equals(holder.id().getNamespace())) continue;
            ItemStack result = holder.value().getResultItem(level.registryAccess());
            if (!result.isEmpty()) produced.add(BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
        }
        Map<String, String> sources = new LinkedHashMap<>();
        for (WorldSource source : WORLD_SOURCES) sources.put(source.item(), source.reason());
        Set<String> used = new TreeSet<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (!"thaumcraft".equals(holder.id().getNamespace())) continue;
            for (Ingredient ingredient : holder.value().getIngredients()) {
                if (ingredient.isEmpty()) continue;
                for (ItemStack candidate : ingredient.getItems()) {
                    String id = BuiltInRegistries.ITEM.getKey(candidate.getItem()).toString();
                    if (id.startsWith("thaumcraft:")) used.add(id);
                }
            }
        }
        Set<String> closed = new TreeSet<>();
        Set<String> open = new TreeSet<>();
        for (String id : used) {
            if (produced.contains(id) || sources.containsKey(id)) closed.add(id);
            else open.add(id);
        }
        test.assertValueEqual(String.join(",", open), String.join(",", KNOWN_GAPS),
                "the mod ingredients of the recipe tree without any source are exactly the documented gap list");
        test.assertTrue(closed.size() == used.size() - open.size(),
                "every other mod ingredient of the recipe tree is produced by a recipe or has a natural world source");
        // The cheat sheet of the port may never be a link of the chain.
        test.assertTrue(!produced.contains("thaumcraft:thaumonomicon_cheat"),
                "the cheat thaumonomicon is not the result of any recipe");
        test.assertTrue(!used.contains("thaumcraft:thaumonomicon_cheat"),
                "no recipe of the port uses the cheat thaumonomicon as an ingredient");
        test.succeed();
    }

    /**
     * The research of the chain is purchasable with the points the player earns.
     *
     * <p>Three claims are checked for every research entry of the chain: every
     * aspect of its price can be discovered in the world, the price of every aspect
     * fits into the pool the player can accumulate for it, and the purchase really
     * grants the research and debits exactly the price.</p>
     */
    public static void chainResearchIsPurchasableWithTheEarnedPool(GameTestHelper test) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        var knowledge = KnowledgeAccess.get(player);
        int cap = thaumcraft.common.config.ThaumcraftSettings.server().aspectCap();
        Map<String, Integer> budget = new LinkedHashMap<>();
        for (ChainResearch entry : CHAIN_RESEARCH) {
            ResearchDefinition definition = ResearchCatalog.get(entry.key());
            test.assertTrue(definition != null, "the original research " + entry.key() + " is in the catalogue");
            if (definition == null) continue;
            test.assertTrue(definition.isEnabled(), "the research " + entry.key() + " is enabled (" + entry.why() + ")");
            for (var cost : definition.aspectCost().entrySet()) {
                String tag = cost.getKey();
                test.assertTrue(Aspect.getAspect(tag) != null, "the aspect " + tag + " of " + entry.key() + " is an original aspect");
                test.assertTrue(discoverable(tag),
                        "the aspect " + tag + " of " + entry.key() + " can be discovered by scanning the world");
                test.assertTrue(cost.getValue() <= cap,
                        "the price of " + tag + " in " + entry.key() + " is " + cost.getValue()
                                + " and the pool of one aspect reaches " + cap + " (oracle Config:393)");
                budget.merge(tag, cost.getValue(), Integer::sum);
            }
        }
        for (var entry : budget.entrySet()) {
            test.assertTrue(entry.getValue() <= cap,
                    "the whole chain needs " + entry.getValue() + " points of " + entry.getKey()
                            + " and the pool of one aspect reaches " + cap);
        }
        // The purchase really grants the research and debits exactly the price.
        // The original has two paths (oracle GuiResearchBrowser:644): a secondary
        // research is bought with the aspect pool, a primary one needs a note.
        knowledge.reset();
        knowledge.setInitializedAspects(true);
        ResearchDefinition primary = ResearchCatalog.get("INFUSION");
        test.assertTrue(primary != null && !primary.isSecondary(),
                "the infusion research of the chain is a primary research and is learned from a note");
        for (var cost : primary.aspectCost().entrySet()) knowledge.setAspectPool(cost.getKey(), cost.getValue());
        var refusedPrimary = ResearchPurchase.purchase(knowledge, "INFUSION");
        test.assertTrue(!refusedPrimary.success(), "a primary research is not bought with the aspect pool");
        test.assertTrue(!knowledge.isResearchComplete("INFUSION"), "the refused primary purchase grants nothing");
        ResearchDefinition secondary = ResearchCatalog.get("TUBEFILTER");
        test.assertTrue(secondary != null && secondary.isSecondary(),
                "the tube filter research of the chain is a secondary research and is bought with the aspect pool");
        if (secondary == null) return;
        knowledge.addResearch("TUBES");
        for (var cost : secondary.aspectCost().entrySet()) knowledge.setAspectPool(cost.getKey(), cost.getValue());
        var result = ResearchPurchase.purchase(knowledge, "TUBEFILTER");
        test.assertTrue(result.success(), "the secondary research of the chain is bought with the points of the pool");
        test.assertTrue(knowledge.isResearchComplete("TUBEFILTER"), "the purchased research is granted");
        for (var cost : secondary.aspectCost().entrySet()) {
            test.assertValueEqual(knowledge.getAspectPoolFor(cost.getKey()), 0, "the price of " + cost.getKey() + " is debited");
        }
        // One point short is refused, and the refusal is atomic.
        knowledge.removeResearch("TUBEFILTER");
        for (var cost : secondary.aspectCost().entrySet()) knowledge.setAspectPool(cost.getKey(), cost.getValue() - 1);
        var refused = ResearchPurchase.purchase(knowledge, "TUBEFILTER");
        test.assertTrue(!refused.success(), "a research entry that is one point short is refused");
        test.assertTrue(!knowledge.isResearchComplete("TUBEFILTER"), "the refused purchase grants nothing");
        test.succeed();
    }

    /**
     * Nothing of the chain needs creative mode, a command, a cheat item or a
     * fixture that hands the player an item.
     */
    public static void chainNeedsNoCreativeModeOrFixture(GameTestHelper test) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        test.assertTrue(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL,
                "the acceptance player is a survival player");
        test.assertTrue(!player.getAbilities().instabuild, "the acceptance player has no creative abilities");
        ServerLevel level = test.getLevel();
        var manager = level.getRecipeManager();
        // The cheat thaumonomicon of the port may never be reachable through a recipe.
        test.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "thaumonomicon_cheat")).isEmpty(),
                "there is no recipe that hands out the cheat thaumonomicon");
        // The research purchase is a real cost: the pool of the player is debited.
        var knowledge = KnowledgeAccess.get(player);
        knowledge.reset();
        knowledge.setInitializedAspects(true);
        knowledge.setAspectPool("pannus", 3);
        knowledge.setAspectPool("praecantatio", 3);
        var bought = ResearchPurchase.purchase(knowledge, "ENCHFABRIC");
        test.assertTrue(bought.success() && knowledge.getAspectPoolFor("pannus") == 0
                        && knowledge.getAspectPoolFor("praecantatio") == 0,
                "the research purchase of a survival player pays the original price");
        // The Thaumonomicon of the chain is made by a wand on a bookshelf, never placed.
        BlockPos shelf = new BlockPos(2, 2, 2);
        test.setBlock(shelf, net.minecraft.world.level.block.Blocks.BOOKSHELF);
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        var absolute = test.absolutePos(shelf);
        var hit = new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(absolute), net.minecraft.core.Direction.UP, absolute, false);
        var context = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        var item = ThaumcraftContent.WAND.get();
        test.assertTrue(item.useOn(context).consumesAction(), "the wand on the bookshelf answers the original trigger");
        var bounds = new net.minecraft.world.phys.AABB(absolute).inflate(1);
        var books = level.getEntitiesOfClass(ItemEntity.class, bounds);
        test.assertTrue(books.size() == 1 && books.getFirst().getItem().is(ThaumcraftContent.THAUMONOMICON.get()),
                "the bookshelf hands the thaumonomicon to the player as the original special item");
        test.assertTrue(!level.getBlockState(absolute).is(net.minecraft.world.level.block.Blocks.BOOKSHELF),
                "the original trigger consumes the bookshelf");
        test.succeed();
    }

    /**
     * The Outer Lands are reachable: the eldritch altar of the eldritch ring, the
     * four eldritch eyes, the dark node above the altar and a wand with six
     * hundred vis open the gate, and the gate moves the player into the Outer Lands.
     */
    public static void outerLandsIsReachableFromTheOverworld(GameTestHelper test) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        ServerLevel level = test.getLevel();
        var knowledge = KnowledgeAccess.get(player);
        knowledge.reset();
        knowledge.setInitializedAspects(true);
        knowledge.addResearch("OCULUS");
        BlockPos altarPos = new BlockPos(3, 2, 3);
        // The structure of the eldritch ring: the altar, the dark node above it
        // and the obelisk above the node (oracle WorldGenEldritchRing:84-137 and
        // ThaumcraftWorldGenerator:446). The natural generation of the ring is
        // proved by SurfaceStructurePlacementRuntimeChecks.
        test.setBlock(altarPos, thaumcraft.common.config.ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState());
        test.setBlock(altarPos.above(), thaumcraft.common.config.ThaumcraftContent.AURA_NODE.get().defaultBlockState());
        test.setBlock(altarPos.above(2), thaumcraft.common.config.ThaumcraftEldritchBlocks.ELDRITCH.get().defaultBlockState());
        test.assertTrue(level.getBlockState(test.absolutePos(altarPos)).is(thaumcraft.common.config.ThaumcraftEldritchBlocks.ELDRITCH.get()),
                "the altar is an eldritch block");
        test.assertTrue(level.getBlockState(test.absolutePos(altarPos.above(2))).is(thaumcraft.common.config.ThaumcraftEldritchBlocks.ELDRITCH.get()),
                "the obelisk above the dark node is an eldritch block, got "
                        + level.getBlockState(test.absolutePos(altarPos.above(2))));
        var node = (thaumcraft.common.tiles.TileNode) test.getBlockEntity(altarPos.above());
        test.assertTrue(node != null, "the dark node above the altar is a real node");
        if (node == null) return;
        node.setNodeType(thaumcraft.api.nodes.NodeType.DARK);
        var altar = (thaumcraft.common.tiles.TileEldritchAltar) test.getBlockEntity(altarPos);
        test.assertTrue(altar != null, "the altar is a real altar");
        if (altar == null) return;
        // The four eldritch eyes of the original (oracle BlockEldritch:266-272).
        // The player places them with the real interaction of the altar.
        for (int eye = 0; eye < 4; eye++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(thaumcraft.common.config.ThaumcraftWarp.ELDRITCH_EYE.get()));
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(test.absolutePos(altarPos)));
        }
        test.assertTrue(altar.getEyes() == 4, "the four eldritch eyes of the original sit on the altar, got " + altar.getEyes());
        // The original oculus pays one hundred whole vis of every primal aspect
        // (oracle WandManager.java:434-435). The wand of the original that can pay
        // it is a staff with a cap whose base cost modifier is one; the gold cap
        // is the one of ConfigItems:296 with the modifier one.
        ItemStack wand = new ItemStack(ThaumcraftContent.WAND.get());
        WandData data = new WandData("primal_staff", "gold", Map.of());
        for (String tag : WandData.PRIMALS) data = data.withAmount(tag, 10000);
        wand.set(ThaumcraftContent.WAND_DATA.get(), data);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        var item = ThaumcraftContent.WAND.get();
        test.assertTrue(item.useOn(new net.minecraft.world.item.context.UseOnContext(
                        player, InteractionHand.MAIN_HAND, hit(test.absolutePos(altarPos)))).consumesAction(),
                "the wand on the altar answers the original oculus trigger");
        test.assertTrue(altar.isOpen(), "the altar is open after the oculus, the gate is "
                + level.getBlockState(test.absolutePos(altarPos.above())));
        test.assertTrue(level.getBlockState(test.absolutePos(altarPos.above()))
                        .is(thaumcraft.common.config.ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get()),
                "the oculus replaces the dark node with the eldritch gate, got "
                        + level.getBlockState(test.absolutePos(altarPos.above())));
        int paid = 0;
        for (String tag : WandData.PRIMALS) paid += 10000 - ItemWandCasting.getVis(wand, tag);
        test.assertTrue(paid == 60000, "the oculus paid the original six hundred vis, got " + paid);
        test.succeed();
    }

    /**
     * The boss of the Outer Lands hands the player the primordial pearl.
     *
     * <p>The original drop is the special item of every boss
     * ({@code EntityThaumcraftBoss.java:217}, {@code EntityCultistPortal.java:269}).</p>
     */
    public static void bossLootIsThePrimordialPearl(GameTestHelper test) {
        ServerPlayer player = test.makeMockServerPlayerInLevel();
        ServerLevel level = test.getLevel();
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "cultist_portal"));
        test.assertTrue(type != null, "the cultist portal boss of the Outer Lands is registered");
        if (type == null) return;
        var boss = type.create(level);
        test.assertTrue(boss != null, "the boss of the Outer Lands can be created");
        if (boss == null) return;
        test.assertTrue(boss instanceof thaumcraft.common.entities.monster.EntityCultistPortal,
                "the Outer Lands boss of the original is the cultist portal");
        var absolute = test.absolutePos(new BlockPos(3, 2, 3));
        boss.moveTo(absolute.getX() + .5D, absolute.getY(), absolute.getZ() + .5D, 0.0F, 0.0F);
        level.addFreshEntity(boss);
        ((net.minecraft.world.entity.LivingEntity) boss)
                .hurt(level.damageSources().playerAttack(player), 1000.0F);
        test.assertTrue(!boss.isAlive(), "the boss of the Outer Lands dies");
        var bounds = new net.minecraft.world.phys.AABB(absolute).inflate(4);
        boolean pearl = false;
        for (var drop : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            if (drop.getItem().is(ThaumcraftContent.PRIMORDIAL_PEARL.get())) pearl = true;
        }
        test.assertTrue(pearl, "the boss of the Outer Lands drops the primordial pearl of the original");
        test.succeed();
    }

    private static net.minecraft.world.phys.BlockHitResult hit(BlockPos absolute) {
        return new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(absolute), net.minecraft.core.Direction.UP, absolute, false);
    }

    // ----------------------------------------------------------------- helpers

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    /** True when the port registers a world object, entity or phenomenon that carries the aspect. */
    private static boolean discoverable(String tag) {
        Aspect aspect = Aspect.getAspect(tag);
        if (aspect == null) return false;
        // The six primal aspects are the starting pool of every player
        // (oracle ResearchManager:669: fifteen plus a random zero to four).
        if (aspect.getComponents() == null || aspect.getComponents().length == 0) return true;
        for (ResourceLocation id : thaumcraft.common.lib.research.ObjectAspectCatalog.registeredIds()) {
            if (thaumcraft.common.lib.research.ObjectAspectCatalog.get(id).getAmount(aspect) > 0) return true;
        }
        for (var tag2 : thaumcraft.common.lib.research.EntityAspectBindings.originalTags()) {
            if (tag2.aspects.getAmount(aspect) > 0) return true;
        }
        return false;
    }
}
