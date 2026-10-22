package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftMasonry;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.crafting.NativeArcaneRecipe;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.crafting.InfusionEnchantmentRecipe;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.infusion.InfusionStructure;
import thaumcraft.common.lib.research.ResearchDefinition;

/**
 * Client-only views of the very same RecipeHolders used by native crafting.
 * Only explicitly mapped TC4 references with actual native recipes are shown.
 * There is deliberately no output search, expression evaluator, synthetic recipe,
 * or fallback for the remaining recipe references/expressions in research.json.
 */
public final class ThaumcraftRecipeDisplay {
    private static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchbook_overlay.png");
    // Original 4.2.3.5 ConfigRecipes.java lines below, not research-key guesses.
    private static final Map<String, ResourceLocation> ALIASES = Map.ofEntries(
            alias("Table", "table"),                             // 454
            alias("Thaumometer", "thaumometer"),                 // 458
            alias("Goggles", "arcane/goggles"),                  // 261
            alias("Phial", "phial_empty"),                       // 453
            alias("WandCapIron", "wand_cap_iron"),                // 430
            alias("WandBasic", "wand_wood_iron"),                  // 429
            alias("Scribe1", "scribing_tools_phial"),             // 455
            alias("Scribe2", "scribing_tools_bottle"),            // 456
            alias("Scribe3", "scribing_tools_refill"),            // 457
            alias("EnchantedFabric", "arcane/enchanted_fabric"),  // 256, research ENCHFABRIC
            alias("RobeChest", "arcane/robe_chestplate"),        // 257, same ENCHFABRIC gate
            alias("RobeLegs", "arcane/robe_leggings"),          // 258
            alias("RobeBoots", "arcane/robe_boots"),            // 259
            alias("WardedJar", "arcane/warded_jar"),             // 223, research DISTILESSENTIA
            alias("JarVoid", "arcane/jar_void"),                // 224, empty normal jar / obsidian / blaze powder
            alias("Alumentum", "crucible/alumentum"),           // 115
            alias("Nitor", "crucible/nitor"),                   // 116
            alias("Thaumium", "crucible/thaumium"),             // 117
            alias("ThaumiumHelm", "thaumium_helmet"),         // 459; ordinary, not research-gated crafting
            alias("ThaumiumChest", "thaumium_chestplate"),   // 460
            alias("ThaumiumLegs", "thaumium_leggings"),     // 461
            alias("ThaumiumBoots", "thaumium_boots"),       // 462
            alias("ThaumiumShovel", "thaumium_shovel"),     // 463
            alias("ThaumiumPick", "thaumium_pickaxe"),      // 464
            alias("ThaumiumAxe", "thaumium_axe"),           // 465
            alias("ElementalAxe", "infusion/elemental_axe"), // 364, pristine thaumium axe / two water shards / diamond / Greatwood log
            alias("ElementalPick", "infusion/elemental_pickaxe"), // ConfigRecipes:346, pristine thaumium pickaxe / two fire shards / diamond / Greatwood log
            alias("ElementalHoe", "infusion/elemental_hoe"), // ConfigRecipes:349, pristine thaumium hoe / order shard / entropy shard / diamond / Greatwood log
            alias("ElementalShovel", "infusion/elemental_shovel"), // ConfigRecipes:348, pristine thaumium shovel / two earth shards / diamond / Greatwood log
            alias("ElementalSword", "infusion/elemental_sword"), // ConfigRecipes:347, pristine thaumium sword / two air shards / diamond / Greatwood log
            alias("ThaumiumHoe", "thaumium_hoe"),           // 466
            alias("ThaumiumSword", "thaumium_sword"),       // 467
            alias("VoidHelm", "void_helmet"),               // 449; ordinary, not research-gated crafting
            alias("VoidChest", "void_chestplate"),          // 450
            alias("VoidLegs", "void_leggings"),             // 451
            alias("VoidBoots", "void_boots"),               // 452
            alias("ThaumiumFortressHelm", "infusion/fortress_helm"),   // ConfigRecipes:360, pristine thaumium helmet / 2 thaumium ingots / 2 gold / emerald
            alias("ThaumiumFortressChest", "infusion/fortress_chest"), // 361, pristine thaumium chestplate / 4 thaumium ingots / gold / leather
            alias("ThaumiumFortressLegs", "infusion/fortress_legs"),   // 362, pristine thaumium leggings / 3 thaumium ingots / gold / leather
            alias("HelmGoggles", "infusion/fortress_goggles"),         // 367, any fortress helm / slime ball / goggles
            alias("MaskGrinningDevil", "infusion/fortress_mask_devil"),// 368
            alias("MaskAngryGhost", "infusion/fortress_mask_ghost"),   // 369
            alias("MaskSippingFiend", "infusion/fortress_mask_fiend"), // 370
            alias("VoidRobeHelm", "infusion/void_robe_helm"),   // ConfigRecipes:363, pristine void helmet / goggles / 4 enchanted fabric / salis mundus
            alias("VoidRobeChest", "infusion/void_robe_chest"), // 364, pristine void chestplate / robe chestplate / void and thaumium ingot / salis mundus / fabric / leather
            alias("VoidRobeLegs", "infusion/void_robe_legs"),   // 365, the same with the robe leggings
            alias("VoidShovel", "void_shovel"),             // 453
            alias("VoidPick", "void_pickaxe"),              // 454
            alias("VoidAxe", "void_axe"),                   // 455
            alias("VoidHoe", "void_hoe"),                   // 456
            alias("VoidSword", "void_sword"),               // 457
            alias("Tallow", "crucible/tallow"),                // 120
            alias("AltGunpowder", "crucible/alt_gunpowder"),   // ConfigRecipes:101, two gunpowder from one
            alias("AltSlime", "crucible/alt_slime"),
            alias("AltClay", "crucible/alt_clay"),
            alias("AltGlowstone", "crucible/alt_glowstone"),
            alias("AltInk", "crucible/alt_ink"),
            alias("AltWeb", "crucible/alt_web"),
            alias("AltMossyCobble", "crucible/alt_mossy_cobble"),
            alias("AltIce", "crucible/alt_ice"),               // catalyst is the snow BLOCK, not the layer
            alias("AltCrackedBrick", "crucible/alt_cracked_brick"),
            alias("AltBonemeal", "crucible/alt_bonemeal"),
            alias("MirrorGlass", "arcane/mirrored_glass"),     // ConfigRecipes:196, shapeless quicksilver / glass pane
            alias("ArcaneStone1", "arcane/arcane_stone"),      // 205
            alias("ArcaneStone2", "arcane_stone_bricks"),      // 206
            alias("ArcaneStone3", "arcane_stone_brick_stairs"), // 207
            alias("ArcaneStone4", "arcane_stone_brick_slab"),   // 208
            alias("Filter", "arcane/vis_filter"),             // 279
            alias("AlchemyFurnace", "arcane/alchemy_furnace"), // 280
            alias("Alembic", "arcane/alembic"),               // 281
            alias("Bellows", "arcane/bellows"),               // 282
            alias("Tube", "arcane/essentia_tube"),           // 283
            alias("Resonator", "arcane/resonator"),          // 284
            alias("TubeValve", "arcane/essentia_valve"),     // 285, genuinely shapeless
            alias("TubeFilter", "arcane/essentia_filter"),   // 286, TUBEFILTER
            alias("TubeRestrict", "arcane/essentia_restrict"), // 287, stone not cobblestone
            alias("TubeOneway", "arcane/essentia_oneway"),    // 288, original dyeBlue includes lapis
            alias("TubeBuffer", "arcane/essentia_buffer"),    // 289, distinct shaped CENTRIFUGE gate
            alias("AlchemicalConstruct", "arcane/alchemical_construct"),
            alias("AdvAlchemyConstruct", "arcane/advanced_alchemical_construct"), // 290, FOUR basic tubes
            alias("MnemonicMatrix", "arcane/mnemonic_matrix"),
            alias("Centrifuge", "arcane/centrifuge"),         // 292, no advanced-construct substitution
            alias("EssentiaCrystalizer", "arcane/essentia_crystallizer"), // 293, dispenser/balanced shards
            alias("EssentiaReservoir", "infusion/essentia_reservoir"), // 372, buffer centre, two void ingots and four jars
            alias("Levitator", "arcane/levitator"), // 194, greatwood planks, earth and air shards, nitor, iron
            alias("ArcaneEar", "arcane/arcane_ear"),
            alias("ArcaneLamp", "arcane/arcane_lamp"),
            alias("NodeStabilizer", "arcane/node_stabilizer"), // 199, quartz BLOCK / stone BRICKS / Nitor ITEM
            alias("NodeTransducer", "arcane/node_transducer"), // 200, literal iron/redstone BLOCK/comparator
            alias("NodeRelay", "arcane/vis_relay"), // 201, balanced shard, TWO relays
            alias("PrimalCharm", "arcane/primal_charm"), // 190, six distinct shards + balanced + gold
            alias("FocalManipulator", "arcane/focal_manipulator"), // 203, arcane stone NOT bricks
            alias("ArcaneBoreBase", "arcane/arcane_bore_base"), // 255, literal greatwood / iron / dispenser
            alias("ArcaneBore", "infusion/arcane_bore"), // 375, eight components / pristine diamond tools
            alias("HungryChest", "arcane/hungry_chest"), // 262, original plankWood / wooden trapdoor
            alias("GolemStraw", "crucible/golem_straw"), // 164, hay catalyst, not infusion
            alias("GolemWood", "crucible/golem_wood"), // 165, greatwood log
            alias("GolemTallow", "crucible/golem_tallow"), // 166, actual tallow block
            alias("GolemClay", "crucible/golem_clay"), // 167, clay block
            alias("GolemFlesh", "crucible/golem_flesh"), // 168, actual original blockTaint:2
            alias("GolemStone", "crucible/golem_stone"), // 169, ordinary stone bricks
            alias("GolemIron", "crucible/golem_iron"), // 170, iron block
            alias("GolemThaumium", "crucible/golem_thaumium"), // 171, actual thaumium block
            alias("BlockThaumium", "thaumium_block"), // 439, ordinary ingot compression
            alias("BlockTallow", "tallow_block"), // 441, ordinary exact-tallow compression
            alias("BlockFlesh", "flesh_block"), // 438, nine rotten flesh; recovery is loot, not reverse crafting
            alias("CoreBlank", "arcane/golem_core_blank"), // 264, brick items / Nitor item
            alias("CoreGather", "crucible/golem_core_gather"), // 172, blank100 -> Gather2
            alias("CoreFill", "crucible/golem_core_fill"), // 173, fames/vacuos
            alias("CoreEmpty", "crucible/golem_core_empty"), // 174, lucrum/vacuos
            alias("CoreSorting", "infusion/golem_core_sorting"), // 371, actual zombie brain / four ordered components
            alias("CoreHarvest", "crucible/golem_core_harvest"), // 175, meto/messis; no ordinary replanting
            alias("CoreGuard", "crucible/golem_core_guard"), // 176, telum/vinculum; ordinary hostile-only target policy
            alias("CoreLumber", "infusion/golem_core_lumber"), // 372, Harvest center / Axe of the Stream + three iron axes (damage0)
            alias("CoreFishing", "infusion/golem_core_fishing"), // 373, Harvest center / rod + three exact fish species
            alias("CoreLiquid", "crucible/golem_core_liquid"), // 178, Aqua/Vacuos; actual water-to-crucible unit
            alias("CoreAlchemy", "infusion/golem_core_alchemy"), // 370, Decanting center / empty jar + three real water potions
            alias("CoreButcher", "crucible/golem_core_butcher"), // 177, actual Guard catalyst / Corpus5 Bestia5
            alias("CoreUse", "infusion/golem_core_use"), // 374, Empty center / damage0 flint and steel + shears
            alias("AdvancedGolem", "infusion/golem_advanced"), // 326, any golem placer patched with advanced / redstone, glowstone dust, gunpowder, empty jar, zombie brain
            alias("TinyHat", "arcane/golem_decoration_tophat"), // 252, black wool / gold ingot
            alias("TinyFez", "arcane/golem_decoration_fez"), // 253, red wool / string
            alias("TinyBowtie", "arcane/golem_decoration_bowtie"), // 254, black wool / string
            alias("TinyGlasses", "arcane/golem_decoration_glasses"), // 255, glass / iron ingot
            alias("TinyVisor", "arcane/golem_decoration_visor"), // 257, iron ingots / any leather helmet
            alias("TinyArmor", "arcane/golem_decoration_armor"), // 258, iron ingots / any leather tunic
            alias("TinyDart", "arcane/golem_decoration_dart"), // 256, arrows around a dispenser
            alias("TinyHammer", "arcane/golem_decoration_mace"), // 259, seven iron ingots
            alias("TravelTrunk", "infusion/traveling_trunk"), // hungry chest / iron ingot, greatwood planks, wooden golem placer, greatwood planks
            alias("GolemFetter", "arcane/golem_fetter"), // arcane stone / iron ingot / arcane stone bricks / redstone block
            alias("GolemBell", "arcane/golem_bell"), // 263, quartz items / stickWood
            alias("UpgradeAir", "arcane/golem_upgrade_air"), // 265, eight gold nuggets around the air shard (damage 0)
            alias("UpgradeEarth", "arcane/golem_upgrade_earth"), // 266, earth shard is damage 3, not 1
            alias("UpgradeFire", "arcane/golem_upgrade_fire"), // 267, fire shard is damage 1
            alias("UpgradeWater", "arcane/golem_upgrade_water"), // 268, water shard is damage 2
            alias("UpgradeOrder", "arcane/golem_upgrade_order"), // 269, order shard is damage 4
            alias("UpgradeEntropy", "arcane/golem_upgrade_entropy"), // 270, entropy shard damage 5
            alias("WandCapGold", "arcane/wand_cap_gold"),      // 229
            alias("WandCapThaumiumInert", "arcane/wand_cap_thaumium_inert"), // 236
            alias("WandCapThaumium", "infusion/wand_cap_thaumium"), // 328
            alias("WandCapVoidInert", "arcane/wand_cap_void_inert"), // 218
            alias("WandCapVoid", "infusion/wand_cap_void"), // 310
            alias("VoidMetal", "crucible/void_ingot"), // 99
            alias("VoidSeed", "crucible/void_seed"), // 100
            alias("PureIron", "crucible/pure_iron"), // 112, iron ore -> native iron cluster
            alias("PureGold", "crucible/pure_gold"), // 113, gold ore -> native gold cluster
            alias("TransIron", "crucible/trans_iron"), // 126, one iron nugget -> three
            alias("TransGold", "crucible/trans_gold"), // 127, one gold nugget -> three
            alias("WandRodGreatwood", "arcane/wand_rod_greatwood"), // 238
            alias("FocusFire", "arcane/focus_fire"),             // 247
            alias("FocusFrost", "arcane/focus_frost"),           // 248
            alias("FocusShock", "arcane/focus_shock"),           // 249
            alias("FocusPrimal", "arcane/focus_primal"),
            alias("BathSalts", "crucible/bath_salts"),
            alias("ArcaneSpa", "arcane/arcane_spa"),
            alias("SaneSoap", "crucible/sanity_soap"),
            alias("FocusPortableHole", "infusion/focus_portable_hole"), // ConfigRecipes:320, ender pearl / 3 nether quartz / earth, air and entropy shards
            alias("FocusExcavation", "arcane/focus_excavation"), // 251
            alias("FocusTrade", "arcane/focus_trade"),
            alias("FocusWarding", "infusion/focus_warding"),
            alias("FocusHellbat", "infusion/focus_hellbat"),
            alias("FocusPouch", "arcane/focus_pouch"),
            alias("MundaneAmulet", "mundane_amulet"),
            alias("MundaneRing", "mundane_ring"),
            alias("MundaneBelt", "mundane_belt"),
            alias("VisAmulet", "infusion/vis_amulet"),
            alias("HoverGirdle", "infusion/hover_girdle"),
            alias("BootsTraveller", "infusion/boots_traveller"),
            alias("HoverHarness", "infusion/hover_harness"),
            alias("RunicAmulet", "infusion/runic_amulet"),
            alias("RunicAmuletEmergency", "infusion/runic_amulet_emergency"),
            alias("RunicRing", "infusion/runic_ring"),
            alias("RunicRingCharged", "infusion/runic_ring_charged"),
            alias("RunicRingHealing", "infusion/runic_ring_healing"),
            alias("RunicGirdle", "infusion/runic_girdle"),
            alias("RunicGirdleKinetic", "infusion/runic_girdle_kinetic"),
            // Documented gap: the original registration of ConfigRecipes:356 is dead in the original itself
            // (the potion metadata 24620 carries the brewed flag 0x2000 and the splash flag 0x4000 at the same
            // time, and 1.7.10 never produces such an item), so the native file is an identical copy of the
            // file above. The reason is kept in ResearchRecipeCoverage.documentedDuplicateRecipes().
            alias("RunicGirdleKinetic_2", "infusion/runic_girdle_kinetic_2"),
            alias("Deconstructor", "arcane/deconstruction_table"),
            alias("LampGrowth", "infusion/growth_lamp"),
            alias("LampFertility", "infusion/fertility_lamp"),
            alias("KnowFrag", "unknown_research_note"),
            alias("InfEnchRepair", "infusion_enchantment/repair"),
            alias("InfEnchHaste", "infusion_enchantment/haste"),
            alias("InfEnch0", "infusion_enchantment/protection"), // ConfigRecipes:300
            alias("InfEnch1", "infusion_enchantment/fire_protection"), // ConfigRecipes:301
            alias("InfEnch2", "infusion_enchantment/blast_protection"), // ConfigRecipes:302
            alias("InfEnch3", "infusion_enchantment/projectile_protection"), // ConfigRecipes:303
            alias("InfEnch4", "infusion_enchantment/feather_falling"), // ConfigRecipes:304
            alias("InfEnch5", "infusion_enchantment/respiration"), // ConfigRecipes:305
            alias("InfEnch6", "infusion_enchantment/aqua_affinity"), // ConfigRecipes:306
            alias("InfEnch7", "infusion_enchantment/thorns"), // ConfigRecipes:307
            alias("InfEnch8", "infusion_enchantment/sharpness"), // ConfigRecipes:308
            alias("InfEnch9", "infusion_enchantment/smite"), // ConfigRecipes:309
            alias("InfEnch10", "infusion_enchantment/bane_of_arthropods"), // ConfigRecipes:310
            alias("InfEnch11", "infusion_enchantment/knockback"), // ConfigRecipes:311
            alias("InfEnch12", "infusion_enchantment/fire_aspect"), // ConfigRecipes:312
            alias("InfEnch13", "infusion_enchantment/looting"), // ConfigRecipes:313
            alias("InfEnch14", "infusion_enchantment/efficiency"), // ConfigRecipes:314
            alias("InfEnch15", "infusion_enchantment/silk_touch"), // ConfigRecipes:315
            alias("InfEnch16", "infusion_enchantment/unbreaking"), // ConfigRecipes:316
            alias("InfEnch17", "infusion_enchantment/fortune"), // ConfigRecipes:317
            alias("InfEnch18", "infusion_enchantment/power"), // ConfigRecipes:318
            alias("InfEnch19", "infusion_enchantment/punch"), // ConfigRecipes:319
            alias("InfEnch20", "infusion_enchantment/flame"), // ConfigRecipes:320
            alias("InfEnch21", "infusion_enchantment/infinity"), // ConfigRecipes:321
            alias("InfusionMatrix", "arcane/infusion_matrix"),  // 221
            alias("ArcanePedestal", "arcane/arcane_pedestal"),  // 222
            alias("WandRodSilverwood", "infusion/wand_rod_silverwood"), // 336
            alias("WandRodObsidian", "infusion/wand_rod_obsidian"), // 311
            alias("WandRodIce", "infusion/wand_rod_ice"), // 312
            alias("WandRodQuartz", "infusion/wand_rod_quartz"), // 313
            alias("WandRodReed", "infusion/wand_rod_reed"), // 314
            alias("WandRodBlaze", "infusion/wand_rod_blaze"), // 315
            alias("WandRodBone", "infusion/wand_rod_bone"), // 316
            alias("WandRodGreatwoodStaff", "arcane/staff_rod_greatwood"), // 220
            alias("WandRodObsidianStaff", "arcane/staff_rod_obsidian"), // 221
            alias("WandRodSilverwoodStaff", "arcane/staff_rod_silverwood"), // 222
            alias("WandRodIceStaff", "arcane/staff_rod_ice"), // 223
            alias("WandRodQuartzStaff", "arcane/staff_rod_quartz"), // 224
            alias("WandRodReedStaff", "arcane/staff_rod_reed"), // 225
            alias("WandRodBlazeStaff", "arcane/staff_rod_blaze"), // 226
            alias("WandRodBoneStaff", "arcane/staff_rod_bone"), // 227
            alias("WandRodPrimalStaff", "infusion/staff_rod_primal"), // 318
            alias("TallowCandle", "tallow_candle"),            // 477
            alias("Grate", "item_grate"),                      // 433, iron bars over a wooden trapdoor
            alias("Clusters0", "clusters/air"),
            alias("Clusters1", "clusters/fire"),
            alias("Clusters2", "clusters/water"),
            alias("Clusters3", "clusters/earth"),
            alias("Clusters4", "clusters/order"),
            alias("Clusters5", "clusters/entropy"),
            alias("Clusters6", "clusters/mixed"),
            alias("JarLabel", "jar_label"));                    // 416-418; dynamic bind/clear not static diagrams

    private final RecipeHolder<?> recipe;
    private final List<Ingredient> ingredients;
    private final int gridWidth;
    private final boolean shapeless;
    private final List<ThaumcraftRecipeDisplay> alternatives;
    private final boolean infusionAltar;
    private final boolean thaumatorium;
    private final boolean advancedFurnace;
    /** The three sceptre variants of the SCEPTRE page, null for every other page. */
    private final SceptreVariant[] sceptreVariants;
    /** The synthetic result of a page whose recipe has no native counterpart, null otherwise. */
    private final ItemStack syntheticResult;
    /** The synthetic inputs of such a page, in the order of the original recipe. */
    private final List<ItemStack> syntheticInputs;

    private ThaumcraftRecipeDisplay(RecipeHolder<?> recipe, List<Ingredient> ingredients, int gridWidth, boolean shapeless) {
        this.recipe = recipe;
        this.ingredients = List.copyOf(ingredients);
        this.gridWidth = gridWidth;
        this.shapeless = shapeless;
        alternatives = List.of();
        infusionAltar = false;
        thaumatorium = false;
        advancedFurnace = false;
        sceptreVariants = null;
        syntheticResult = ItemStack.EMPTY;
        syntheticInputs = List.of();
    }

    private ThaumcraftRecipeDisplay(List<ThaumcraftRecipeDisplay> alternatives) {
        var first = alternatives.getFirst();
        recipe = first.recipe;
        ingredients = first.ingredients;
        gridWidth = first.gridWidth;
        shapeless = first.shapeless;
        this.alternatives = List.copyOf(alternatives);
        infusionAltar = false;
        thaumatorium = false;
        advancedFurnace = false;
        sceptreVariants = null;
        syntheticResult = ItemStack.EMPTY;
        syntheticInputs = List.of();
    }

    private ThaumcraftRecipeDisplay(RecipeHolder<?> matrixRecipe, boolean thaumatorium) {
        this(matrixRecipe, thaumatorium, false);
    }

    private ThaumcraftRecipeDisplay(RecipeHolder<?> matrixRecipe, boolean thaumatorium, boolean advancedFurnace) {
        recipe = matrixRecipe;
        ingredients = List.of();
        gridWidth = 3;
        shapeless = false;
        alternatives = List.of();
        infusionAltar = !thaumatorium && !advancedFurnace;
        this.advancedFurnace = advancedFurnace;
        this.thaumatorium = thaumatorium;
        sceptreVariants = null;
        syntheticResult = ItemStack.EMPTY;
        syntheticInputs = List.of();
    }

    /**
     * The SCEPTRE page of the original.
     *
     * <p>Original {@code thaumcraft/common/config/ConfigResearch.java:110-144}: the page holds the
     * three sceptre recipes as an array of arcane recipes, and every one of them is the grid
     * {@code " TF"}, {@code " RT"}, {@code "T  "} of three wand caps, one wand rod and the primal
     * charm. The native recipe {@code thaumcraft:arcane/wand_sceptre} is a <em>special</em>
     * recipe whose ingredients are empty, so the diagram of the page is built from the display
     * fixture of that very recipe.</p>
     */
    private ThaumcraftRecipeDisplay(RecipeHolder<?> sceptreRecipe, SceptreVariant[] variants) {
        recipe = sceptreRecipe;
        ingredients = List.of();
        gridWidth = 3;
        shapeless = false;
        alternatives = List.of();
        infusionAltar = false;
        thaumatorium = false;
        advancedFurnace = false;
        sceptreVariants = variants;
        syntheticResult = ItemStack.EMPTY;
        syntheticInputs = List.of();
    }

    /**
     * A page whose original recipe has no native counterpart, drawn from the declared inputs
     * and the declared result of the original.
     */
    private ThaumcraftRecipeDisplay(ItemStack result, List<ItemStack> inputs) {
        recipe = null;
        ingredients = List.of();
        gridWidth = 3;
        shapeless = true;
        alternatives = List.of();
        infusionAltar = false;
        thaumatorium = false;
        advancedFurnace = false;
        sceptreVariants = null;
        syntheticResult = result;
        syntheticInputs = List.copyOf(inputs);
    }

    private static Map.Entry<String, ResourceLocation> alias(String original, String nativeId) {
        return Map.entry(original, ResourceLocation.fromNamespaceAndPath("thaumcraft", nativeId));
    }

    /**
     * The original recipe key to native recipe id map. The bundled coverage map is the
     * authority: it carries every original key the corpus references, including the five
     * research pages that had no recipe at all before the catalogue was closed.
     */
    private static final Map<String, ResourceLocation> RESOLVED = resolveAliases();

    /**
     * One variant of the SCEPTRE page: the rod and the cap of the sceptre.
     *
     * <p>Original {@code thaumcraft/common/config/ConfigResearch.java:110-144}: the three sceptre
     * recipes of the page are the iron cap with the wood rod, the gold cap with the greatwood
     * rod and the thaumium cap with the silverwood rod.</p>
     */
    public record SceptreVariant(String rod, String cap) {}

    /** The three variants of the SCEPTRE page, in the order of the original. */
    public static final List<SceptreVariant> SCEPTRE_VARIANTS = List.of(
            new SceptreVariant("wood", "iron"),
            new SceptreVariant("greatwood", "gold"),
            new SceptreVariant("silverwood", "thaumium"));

    private static Map<String, ResourceLocation> resolveAliases() {
        var merged = new java.util.LinkedHashMap<String, ResourceLocation>(ALIASES);
        for (var entry : thaumcraft.common.lib.research.ResearchRecipeCoverage.nativeRecipeIds().entrySet()) {
            var id = ResourceLocation.tryParse(entry.getValue());
            if (id != null) merged.put(entry.getKey(), id);
        }
        return Map.copyOf(merged);
    }

    public static Map<String, ResourceLocation> aliases() { return RESOLVED; }
    public RecipeHolder<?> recipe() { return recipe; }
    public boolean sameRecipes(ThaumcraftRecipeDisplay other) {
        if (recipe != other.recipe || infusionAltar != other.infusionAltar || thaumatorium != other.thaumatorium || alternatives.size() != other.alternatives.size()) return false;
        if (!ItemStack.matches(syntheticResult, other.syntheticResult)) return false;
        if (syntheticInputs.size() != other.syntheticInputs.size()) return false;
        for (int i = 0; i < syntheticInputs.size(); i++) if (!ItemStack.matches(syntheticInputs.get(i), other.syntheticInputs.get(i))) return false;
        if (sceptreVariants == null ? other.sceptreVariants != null : !java.util.Arrays.equals(sceptreVariants, other.sceptreVariants)) return false;
        for (int i = 0; i < alternatives.size(); i++) if (!alternatives.get(i).sameRecipes(other.alternatives.get(i))) return false;
        return true;
    }

    /** Null means unavailable/unsupported, never an empty crafting page. No extra research gates are added here. */
    public static ThaumcraftRecipeDisplay resolve(ResearchDefinition.Page page, Level level) {
        return resolve(page, null, level);
    }

    /**
     * The alternatives of an original array page.
     *
     * <p>Original {@code thaumcraft/client/gui/GuiResearchRecipe.java:647-655} reads one entry of
     * the array per second and draws it as the page of that recipe, so the native page cycles
     * through its alternatives once per second as well.</p>
     */
    private static ThaumcraftRecipeDisplay alternatives(Level level, List<String> nativeIds) {
        List<ThaumcraftRecipeDisplay> choices = new ArrayList<>();
        for (String id : nativeIds) {
            var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", id)).orElse(null);
            if (holder == null) continue;
            var choice = resolve(holder, level);
            if (choice != null) choices.add(choice);
        }
        return choices.isEmpty() ? null : new ThaumcraftRecipeDisplay(choices);
    }

    /**
     * The SCEPTRE page of the original.
     *
     * <p>Original {@code thaumcraft/common/config/ConfigResearch.java:110-144} and
     * {@code thaumcraft/client/gui/GuiResearchRecipe.java:647-655}: the page holds the three
     * sceptre recipes as an array and the book shows one of them per second. The native recipe
     * {@code thaumcraft:arcane/wand_sceptre} is a special recipe with empty ingredients, so the
     * diagram of the page is built from the display fixture of that very recipe, which carries
     * the three variants of the original.</p>
     */
    public static ThaumcraftRecipeDisplay sceptre(Level level) {
        var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/wand_sceptre")).orElse(null);
        if (holder == null || !(holder.value() instanceof thaumcraft.common.lib.crafting.ArcaneSceptreRecipe)) return null;
        return new ThaumcraftRecipeDisplay(holder, SCEPTRE_VARIANTS.toArray(SceptreVariant[]::new));
    }

    /**
     * The JARLABEL page of the original.
     *
     * <p>Original {@code thaumcraft/common/config/ConfigRecipes.java:417-425}: for every aspect of
     * the original the book holds a recipe that binds a blank jar label to a phial of eight units
     * of that aspect. The page of {@code ConfigResearch.java:302-305} shows those recipes as an
     * array, one per second. The native recipe {@code thaumcraft:jar_label_bind} accepts any
     * filled phial, so the page needs the synthetic inputs of the original: the blank label and
     * the phial of the aspect of the entry.</p>
     *
     * <p>The number of the entries is the number of the aspects of the original, which is
     * <b>forty eight</b> in 4.2.3.5 and not sixteen.</p>
     */
    /**
     * The aspects of the jar label recipes of the original, in the order of the original.
     *
     * <p>Original {@code thaumcraft/common/config/ConfigRecipes.java:417-425} builds one jar label
     * recipe for every registered aspect, so the page of the original holds one entry per aspect
     * of the 4.2.3.5 tree, which is <b>forty eight</b> and not sixteen.</p>
     */
    public static List<String> jarLabelAspects() {
        return Aspect.aspects.values().stream().map(Aspect::getTag).toList();
    }

    public static ThaumcraftRecipeDisplay jarLabels(Level level) {
        if (level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "jar_label_bind")).isEmpty()) return null;
        List<ThaumcraftRecipeDisplay> choices = new ArrayList<>();
        for (String tag : jarLabelAspects()) {
            var label = thaumcraft.common.items.ItemJarLabel.blank();
            var phial = thaumcraft.common.items.ItemEssence.filled(tag);
            var result = thaumcraft.common.items.ItemJarLabel.bound(tag);
            if (label.isEmpty() || phial.isEmpty() || result.isEmpty()) continue;
            choices.add(new ThaumcraftRecipeDisplay(result, List.of(label, phial)));
        }
        return choices.isEmpty() ? null : new ThaumcraftRecipeDisplay(choices);
    }

    /**
     * The page resolver with the key of its research.
     *
     * <p>The original builds two different arrays with the same Java expression
     * ({@code rc.toArray(new IRecipe[0])}): the seven crystal cluster recipes of the ORE page
     * (oracle {@code thaumcraft/common/config/ConfigResearch.java:370-374}) and the sixteen jar
     * label recipes of the JARLABEL page (oracle {@code :302-305}). The key of the research
     * tells the two apart.</p>
     */
    public static ThaumcraftRecipeDisplay resolve(ResearchDefinition.Page page, String researchKey, Level level) {
        if (level == null) return null;
        // ConfigResearch:250-254 explicitly constructs this six-recipe array.
        // This is a literal whitelist entry, NOT evaluation of extracted Java.
        if ("legacy_expression".equals(page.kind()) && "cruc.toArray(new CrucibleRecipe[0])".equals(page.javaExpression())) {
            List<ThaumcraftRecipeDisplay> choices = new ArrayList<>();
            for (String primal : List.of("air", "fire", "water", "earth", "order", "entropy")) {
                var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "crucible/balanced_shard_" + primal)).orElse(null);
                if (holder != null && holder.value() instanceof CrucibleRecipe) {
                    var choice = resolve(holder, level);
                    if (choice != null) choices.add(choice);
                }
            }
            return choices.isEmpty() ? null : new ThaumcraftRecipeDisplay(choices);
        }
        // Original ConfigResearch:193-199: the BANNERS page shows the sixteen banner recipes
        // as an array of arcane recipes.
        if ("legacy_expression".equals(page.kind()) && "banners.toArray(new IArcaneRecipe[0])".equals(page.javaExpression())) {
            return alternatives(level, List.of("arcane/banner_white", "arcane/banner_orange", "arcane/banner_magenta",
                    "arcane/banner_light_blue", "arcane/banner_yellow", "arcane/banner_lime", "arcane/banner_pink",
                    "arcane/banner_gray", "arcane/banner_light_gray", "arcane/banner_cyan", "arcane/banner_purple",
                    "arcane/banner_blue", "arcane/banner_brown", "arcane/banner_green", "arcane/banner_red",
                    "arcane/banner_black"));
        }
        // Original ConfigResearch:221-226: the PRIMALARROW page shows the six primal arrow recipes.
        if ("legacy_expression".equals(page.kind()) && "rcbb.toArray(new IArcaneRecipe[0])".equals(page.javaExpression())) {
            return alternatives(level, List.of("arcane/primal_arrow_air", "arcane/primal_arrow_fire",
                    "arcane/primal_arrow_water", "arcane/primal_arrow_earth", "arcane/primal_arrow_order",
                    "arcane/primal_arrow_entropy"));
        }
        // Original ConfigResearch:370-374: the ORE page shows the seven crystal cluster recipes.
        if ("legacy_expression".equals(page.kind()) && "rc.toArray(new IRecipe[0])".equals(page.javaExpression())
                && "ORE".equals(researchKey)) {
            return alternatives(level, List.of("clusters/air", "clusters/fire", "clusters/water", "clusters/earth",
                    "clusters/order", "clusters/entropy", "clusters/mixed"));
        }
        // Original ConfigResearch:110-144: the SCEPTRE page shows the three sceptre recipes.
        if ("legacy_expression".equals(page.kind()) && "scer.toArray(new IArcaneRecipe[0])".equals(page.javaExpression())) {
            return sceptre(level);
        }
        // Original ConfigResearch:302-305: the JARLABEL page shows one jar label recipe per
        // aspect of the original.
        if ("legacy_expression".equals(page.kind()) && "rc.toArray(new IRecipe[0])".equals(page.javaExpression())
                && "JARLABEL".equals(researchKey)) {
            return jarLabels(level);
        }
        if ("legacy_expression".equals(page.kind()) && "raug.toArray(new InfusionRecipe[0])".equals(page.javaExpression())) {
            var holder = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion_runic_augment/runic_augmentation")).orElse(null);
            return holder != null && holder.value() instanceof thaumcraft.common.crafting.InfusionRunicAugmentRecipe
                    ? resolve(holder, level) : null;
        }
        if (!"recipe_reference".equals(page.kind()) || page.recipeKey() == null) return null;
        // Original ConfigRecipes:93 is a wand construct, NOT an executable
        // extracted expression or a registered fake crafting recipe. Display
        // only this implemented blueprint, bound to its actual native devices.
        if ("InfusionAltar".equals(page.recipeKey())) {
            var matrix = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/infusion_matrix")).orElse(null);
            return matrix != null && matrix.value() instanceof ArcaneShapedRecipe
                    && !matrix.value().getResultItem(level.registryAccess()).isEmpty()
                    && ThaumcraftInfusion.PEDESTAL.get().asItem() != net.minecraft.world.item.Items.AIR
                    ? new ThaumcraftRecipeDisplay(matrix, false) : null;
        }
        // The original Thaumatorium recipe is a wand-formed column of two constructs over a crucible.
        // Anchor its blueprint to the real construct recipe, without manufacturing a formed-machine item.
        if ("Thaumatorium".equals(page.recipeKey())) {
            var construct = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/alchemical_construct")).orElse(null);
            return construct != null && construct.value() instanceof ArcaneShapedRecipe
                    && !construct.value().getResultItem(level.registryAccess()).isEmpty()
                    && ThaumcraftAlchemy.CRUCIBLE_ITEM.get() != net.minecraft.world.item.Items.AIR
                    ? new ThaumcraftRecipeDisplay(construct, true) : null;
        }
        if ("AdvAlchemyFurnace".equals(page.recipeKey())) {
            var construct = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/advanced_alchemical_construct")).orElse(null);
            return construct != null && construct.value() instanceof ArcaneShapedRecipe
                    && !construct.value().getResultItem(level.registryAccess()).isEmpty()
                    ? new ThaumcraftRecipeDisplay(construct, false, true) : null;
        }
        ResourceLocation id = RESOLVED.get(page.recipeKey());
        if (id == null) return null;
        RecipeHolder<?> holder = level.getRecipeManager().byKey(id).orElse(null);
        if (holder == null) return null;
        return resolve(holder, level);
    }

    private static ThaumcraftRecipeDisplay resolve(RecipeHolder<?> holder, Level level) {
        var nativeRecipe = holder.value();
        if (nativeRecipe instanceof thaumcraft.common.crafting.InfusionRunicAugmentRecipe infusion
                && nativeRecipe.getType() == thaumcraft.common.config.ThaumcraftRunic.AUGMENT_TYPE.get()) {
            ItemStack sample = infusion.sample(0);
            if (sample.isEmpty() || !sample.isItemEnabled(level.enabledFeatures())) return null;
            var declared = infusion.components(sample);
            for (Ingredient ingredient : declared) {
                if (ingredient.isEmpty() || java.util.Arrays.stream(ingredient.getItems())
                        .noneMatch(stack -> !stack.isEmpty() && stack.isItemEnabled(level.enabledFeatures()))) return null;
            }
            return new ThaumcraftRecipeDisplay(holder, declared, 3, false);
        }
        if (nativeRecipe instanceof InfusionStackRecipe infusion && nativeRecipe.getType() == ThaumcraftInfusion.INFUSION_RECIPE_TYPE.get()) {
            List<Ingredient> declared = new ArrayList<>();
            declared.add(infusion.input());
            declared.addAll(infusion.components());
            for (Ingredient ingredient : declared) {
                if (ingredient.isEmpty() || java.util.Arrays.stream(ingredient.getItems())
                        .noneMatch(stack -> !stack.isEmpty() && stack.isItemEnabled(level.enabledFeatures()))) return null;
            }
            ItemStack output = infusion.getResultItem(level.registryAccess());
            return output.isEmpty() || !output.isItemEnabled(level.enabledFeatures()) ? null
                    : new ThaumcraftRecipeDisplay(holder, infusion.components(), 3, false);
        }
        if (nativeRecipe instanceof InfusionEnchantmentRecipe infusion && nativeRecipe.getType() == ThaumcraftInfusion.ENCHANTMENT_RECIPE_TYPE.get()) {
            for (Ingredient ingredient : infusion.components()) {
                if (ingredient.isEmpty() || java.util.Arrays.stream(ingredient.getItems())
                        .noneMatch(stack -> !stack.isEmpty() && stack.isItemEnabled(level.enabledFeatures()))) return null;
            }
            ItemStack output = infusion.getResultItem(level.registryAccess());
            return output.isEmpty() || !output.isItemEnabled(level.enabledFeatures()) ? null
                    : new ThaumcraftRecipeDisplay(holder, infusion.components(), 3, false);
        }
        int width, height;
        boolean shapeless = false;
        if (nativeRecipe instanceof CrucibleRecipe && nativeRecipe.getType() == ThaumcraftAlchemy.CRUCIBLE_RECIPE_TYPE.get()) {
            width = height = 1;
        } else if (nativeRecipe instanceof ArcaneShapedRecipe arcane && nativeRecipe.getType() == ThaumcraftArcane.ARCANE_RECIPE_TYPE.get()) {
            width = arcane.pattern().getFirst().length();
            height = arcane.pattern().size();
        } else if (nativeRecipe instanceof ArcaneShapelessRecipe && nativeRecipe.getType() == ThaumcraftArcane.ARCANE_RECIPE_TYPE.get()) {
            width = height = 3;
            shapeless = true;
        } else if (nativeRecipe instanceof ShapedRecipe shaped && nativeRecipe.getType() == RecipeType.CRAFTING) {
            width = shaped.getWidth();
            height = shaped.getHeight();
        } else if (nativeRecipe instanceof ShapelessRecipe && nativeRecipe.getType() == RecipeType.CRAFTING) {
            width = height = 3;
            shapeless = true;
        } else return null;
        var ingredients = nativeRecipe.getIngredients();
        if (width < 1 || width > 3 || height < 1 || height > 3 || ingredients.isEmpty()
                || ingredients.size() > 9 || !shapeless && ingredients.size() != width * height) return null;
        boolean hasInput = false;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue; // Literal blank cells, not unresolved tags.
            hasInput = true;
            if (java.util.Arrays.stream(ingredient.getItems()).noneMatch(stack -> !stack.isEmpty() && stack.isItemEnabled(level.enabledFeatures()))) return null;
        }
        ItemStack result = nativeRecipe.getResultItem(level.registryAccess());
        if (!hasInput || result.isEmpty() || !result.isItemEnabled(level.enabledFeatures())) return null;
        return new ThaumcraftRecipeDisplay(holder, ingredients, width, shapeless);
    }

    /**
     * Original GuiResearchRecipe:503-773 layout, including top-left shaped
     * placement and row-major shapeless placement. UVs are doubled because the
     * retained overlay is 512px, while TC4 addressed it as a 256px GUI texture.
     * The entire diagram fits the current book page; tooltips stay screen-sized.
     */
    public Hover render(GuiGraphics graphics, Font font, Level level, Player player,
                         int x, int y, int width, int height, int mouseX, int mouseY, long cycle) {
        if (!alternatives.isEmpty()) return alternatives.get((int) Math.floorMod(cycle, (long) alternatives.size()))
                .render(graphics, font, level, player, x, y, width, height, mouseX, mouseY, cycle);
        if (sceptreVariants != null) return renderSceptre(graphics, font, level, player,
                x, y, width, height, mouseX, mouseY, cycle);
        if (!syntheticResult.isEmpty()) return renderSynthetic(graphics, font, level, player,
                x, y, width, height, mouseX, mouseY, cycle);
        if (advancedFurnace) return AdvancedFurnaceBookDiagram.render(graphics, font, x, y, width, height, mouseX, mouseY);
        if (thaumatorium) return ThaumatoriumBookDiagram.render(graphics, font, x, y, width, height, mouseX, mouseY);
        if (infusionAltar) return renderInfusionAltar(graphics, font, x, y, width, height, mouseX, mouseY);
        if (recipe.value() instanceof thaumcraft.common.crafting.InfusionRunicAugmentRecipe infusion)
            return RunicAugmentationDisplay.render(infusion, graphics, font, level, x, y, width, height, mouseX, mouseY, cycle);
        if (recipe.value() instanceof InfusionStackRecipe infusion) return renderInfusion(infusion, graphics, font, level,
                x, y, width, height, mouseX, mouseY, cycle);
        if (recipe.value() instanceof InfusionEnchantmentRecipe infusion) return renderEnchantmentInfusion(infusion, graphics, font, level,
                x, y, width, height, mouseX, mouseY, cycle);
        if (recipe.value() instanceof CrucibleRecipe crucible) return renderCrucible(crucible, graphics, font, level,
                x, y, width, height, mouseX, mouseY, cycle);
        boolean arcane = recipe.value() instanceof NativeArcaneRecipe;
        int diagramHeight = arcane ? 200 : 174;
        float scale = Math.min(width / 112F, height / (float) diagramHeight);
        if (scale <= 0) return null;
        float originX = x + (width - 112 * scale) / 2, originY = y + (height - diagramHeight * scale) / 2;
        float mx = (mouseX - originX) / scale, my = (mouseY - originY) / scale;
        Hover hover = null;
        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY, 0);
        graphics.pose().scale(scale, scale, 1);
        RenderSystem.enableBlend();
        graphics.blit(OVERLAY, 4, arcane ? 54 : 64, 104, 104, arcane ? 224 : 120, 30, 104, 104, 512, 512);
        graphics.blit(OVERLAY, 40, arcane ? 14 : 24, 32, 32, 40, 6, 32, 32, 512, 512);
        RenderSystem.disableBlend();
        String type = arcane ? "recipe.type.arcane" : shapeless ? "recipe.type.workbenchshapeless" : "recipe.type.workbench";
        drawCenteredFit(graphics, font, Component.translatable(type), 56, 0, 112, 0xFF505050);
        ItemStack result = recipe.value().getResultItem(level.registryAccess()).copy();
        int resultY = arcane ? 22 : 32;
        drawItem(graphics, font, result, 48, resultY);
        if (inside(mx, my, 48, resultY, 16, 16)) hover = new Hover(result, List.of());
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient.isEmpty()) continue;
            // Never reduce an Ingredient to an item ID: alternatives retain all
            // their components and tags cycle once per second, as in TC4.
            ItemStack[] choices = ingredient.getItems();
            var enabled = java.util.Arrays.stream(choices).filter(stack -> !stack.isEmpty() && stack.isItemEnabled(level.enabledFeatures())).toList();
            if (enabled.isEmpty()) continue;
            ItemStack stack = enabled.get((int) Math.floorMod(cycle, (long) enabled.size())).copy();
            int sx = 16 + i % gridWidth * 32, sy = (arcane ? 66 : 76) + i / gridWidth * 32;
            drawItem(graphics, font, stack, sx, sy);
            if (inside(mx, my, sx, sy, 16, 16)) hover = new Hover(stack, List.of());
        }
        if (arcane) {
            NativeArcaneRecipe nativeRecipe = (NativeArcaneRecipe) recipe.value();
            Map<String, Integer> vis = nativeRecipe.vis();
            // Stable descending amount, with the original primal insertion order
            // for ties in the currently ported fabric/jar recipes.
            List<String> tags = WandData.PRIMALS.stream().filter(tag -> vis.getOrDefault(tag, 0) > 0)
                    .sorted(Comparator.<String>comparingInt(vis::get).reversed()).toList();
            drawCenteredFit(graphics, font, Component.translatable("tc.port.book_recipe.base_vis"), 56, 162, 112, 0xFF505050);
            RenderSystem.enableBlend();
            for (int i = 0; i < tags.size(); i++) {
                String tag = tags.get(i);
                Aspect aspect = Aspect.getAspect(tag);
                int sx = (112 - (tags.size() * 18 - 2)) / 2 + i * 18;
                int color = aspect.getColor();
                graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
                graphics.blit(aspect.getImage(), sx, 174, 0, 0, 16, 16, 16, 16);
                graphics.setColor(1, 1, 1, 1);
                drawCenteredFit(graphics, font, Component.literal(Integer.toString(vis.get(tag))), sx + 8, 190, 18, 0xFF43301F);
                if (inside(mx, my, sx, 174, 16, 25)) hover = visHover(nativeRecipe, tag, player);
            }
            RenderSystem.disableBlend();
        }
        graphics.pose().popPose();
        return hover;
    }

    /**
     * The SCEPTRE page of the original: the arcane diagram of one of the three sceptre recipes.
     *
     * <p>Original {@code thaumcraft/client/gui/GuiResearchRecipe.java:503-639} draws the arcane
     * page: the header of the arcane workbench, the result of the recipe at the top right, the
     * inputs of the shaped arcane grid and the row of the aspects with their vis cost. The grid
     * of the sceptre is the one of the original, " TF", " RT", "T  ".</p>
     */
    private Hover renderSceptre(GuiGraphics graphics, Font font, Level level, Player player,
                                int x, int y, int width, int height, int mouseX, int mouseY, long cycle) {
        if (!(recipe.value() instanceof thaumcraft.common.lib.crafting.ArcaneSceptreRecipe sceptre)) return null;
        SceptreVariant variant = SCEPTRE_VARIANTS.get((int) Math.floorMod(cycle, (long) SCEPTRE_VARIANTS.size()));
        var input = thaumcraft.common.lib.crafting.ArcaneSceptreRecipe.displayInput(variant.rod(), variant.cap());
        ItemStack result = sceptre.assemble(input, level.registryAccess());
        Map<String, Integer> vis = sceptre.vis(input);
        float scale = Math.min(width / 112F, height / 200F);
        if (scale <= 0) return null;
        float originX = x + (width - 112 * scale) / 2, originY = y + (height - 200 * scale) / 2;
        float mx = (mouseX - originX) / scale, my = (mouseY - originY) / scale;
        Hover hover = null;
        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY, 0);
        graphics.pose().scale(scale, scale, 1);
        RenderSystem.enableBlend();
        graphics.blit(OVERLAY, 4, 54, 104, 104, 224, 30, 104, 104, 512, 512);
        graphics.blit(OVERLAY, 40, 14, 32, 32, 40, 6, 32, 32, 512, 512);
        RenderSystem.disableBlend();
        drawCenteredFit(graphics, font, Component.translatable("recipe.type.arcane"), 56, 0, 112, 0xFF505050);
        drawItem(graphics, font, result, 48, 22);
        if (inside(mx, my, 48, 22, 16, 16)) hover = new Hover(result, List.of());
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            int sx = 16 + slot % 3 * 32, sy = 66 + slot / 3 * 32;
            drawItem(graphics, font, stack, sx, sy);
            if (inside(mx, my, sx, sy, 16, 16)) hover = new Hover(stack, List.of());
        }
        List<String> tags = WandData.PRIMALS.stream().filter(tag -> vis.getOrDefault(tag, 0) > 0)
                .sorted(Comparator.<String>comparingInt(vis::get).reversed()).toList();
        drawCenteredFit(graphics, font, Component.translatable("tc.port.book_recipe.base_vis"), 56, 162, 112, 0xFF505050);
        RenderSystem.enableBlend();
        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            Aspect aspect = Aspect.getAspect(tag);
            int sx = (112 - (tags.size() * 18 - 2)) / 2 + i * 18;
            int color = aspect.getColor();
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
            graphics.blit(aspect.getImage(), sx, 174, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
            drawCenteredFit(graphics, font, Component.literal(Integer.toString(vis.get(tag))), sx + 8, 190, 18, 0xFF43301F);
            if (inside(mx, my, sx, 174, 16, 25)) hover = visHover(vis, tag, player);
        }
        RenderSystem.disableBlend();
        graphics.pose().popPose();
        return hover;
    }

    /**
     * A page whose original recipe has no native counterpart.
     *
     * <p>Original {@code thaumcraft/client/gui/GuiResearchRecipe.java:642-773} draws the
     * workbench page: the header, the result of the recipe and the inputs of a shapeless recipe
     * in the row major order.</p>
     */
    private Hover renderSynthetic(GuiGraphics graphics, Font font, Level level, Player player,
                                  int x, int y, int width, int height, int mouseX, int mouseY, long cycle) {
        float scale = Math.min(width / 112F, height / 174F);
        if (scale <= 0) return null;
        float originX = x + (width - 112 * scale) / 2, originY = y + (height - 174 * scale) / 2;
        float mx = (mouseX - originX) / scale, my = (mouseY - originY) / scale;
        Hover hover = null;
        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY, 0);
        graphics.pose().scale(scale, scale, 1);
        RenderSystem.enableBlend();
        graphics.blit(OVERLAY, 4, 64, 104, 104, 120, 30, 104, 104, 512, 512);
        graphics.blit(OVERLAY, 40, 24, 32, 32, 40, 6, 32, 32, 512, 512);
        RenderSystem.disableBlend();
        drawCenteredFit(graphics, font, Component.translatable("recipe.type.workbenchshapeless"), 56, 0, 112, 0xFF505050);
        drawItem(graphics, font, syntheticResult, 48, 32);
        if (inside(mx, my, 48, 32, 16, 16)) hover = new Hover(syntheticResult, List.of());
        for (int i = 0; i < syntheticInputs.size() && i < 9; i++) {
            ItemStack stack = syntheticInputs.get(i);
            if (stack.isEmpty()) continue;
            int sx = 16 + i % 3 * 32, sy = 76 + i / 3 * 32;
            drawItem(graphics, font, stack, sx, sy);
            if (inside(mx, my, sx, sy, 16, 16)) hover = new Hover(stack, List.of());
        }
        graphics.pose().popPose();
        return hover;
    }

    /** Native layer-plan adaptation of the implemented original3x3x3 wand construct. */
    private Hover renderInfusionAltar(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
        float scale = Math.min(width / 120F, height / 226F);
        if (scale <= 0) return null;
        float originX = x + (width - 120 * scale) / 2, originY = y + (height - 226 * scale) / 2;
        float mx = (mouseX - originX) / scale, my = (mouseY - originY) / scale;
        Hover hover = null;
        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY, 0);
        graphics.pose().scale(scale, scale, 1);
        drawCenteredFit(graphics, font, Component.translatable("recipe.type.construct"), 60, 0, 116, 0xFF505050);
        for (int layer = 0; layer < 3; layer++) {
            int sy = 24 + layer * 54;
            graphics.drawString(font, Integer.toString(3 - layer), 8, sy + 20, 0xFF505050, false);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                int sx = 36 + (dx + 1) * 16, cellY = sy + (dz + 1) * 16;
                graphics.fill(sx, cellY, sx + 16, cellY + 16, 0x22634F36);
                ItemStack stack = ItemStack.EMPTY;
                if (layer == 0 && dx == 0 && dz == 0) stack = new ItemStack(ThaumcraftInfusion.MATRIX_ITEM.get());
                else if (layer == 2 && dx == 0 && dz == 0) stack = new ItemStack(ThaumcraftInfusion.PEDESTAL_ITEM.get());
                else if (layer > 0 && dx != 0 && dz != 0) stack = new ItemStack(layer == 1
                        ? ThaumcraftMasonry.ARCANE_STONE.get() : ThaumcraftMasonry.ARCANE_STONE_BRICKS.get());
                if (!stack.isEmpty()) drawItem(graphics, font, stack, sx, cellY);
                if (inside(mx, my, sx, cellY, 16, 16)) hover = stack.isEmpty()
                        ? new Hover(ItemStack.EMPTY, List.of(Component.translatable("tc.port.book_recipe.required_air")))
                        : new Hover(stack, List.of());
            }
        }
        var vis = InfusionStructure.formationVisCost();
        int index = 0;
        for (var cost : vis.entrySet()) {
            Aspect aspect = Aspect.getAspect(cost.getKey());
            int sx = 6 + index++ * 18, color = aspect.getColor();
            graphics.flush();
            RenderSystem.enableBlend();
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
            graphics.blit(aspect.getImage(), sx, 192, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
            drawCenteredFit(graphics, font, Component.literal(Integer.toString(cost.getValue())), sx + 8, 210, 18, 0xFF43301F);
            if (inside(mx, my, sx, 192, 16, 27)) hover = new Hover(ItemStack.EMPTY, List.of(Component.literal(aspect.getName()),
                    Component.translatable("tc.port.book_recipe.base_vis")));
        }
        graphics.pose().popPose();
        return hover;
    }

    /** Original GuiResearchRecipe:941-1105, STACK outputs, declared input and circular components. */
    private Hover renderInfusion(InfusionStackRecipe infusion, GuiGraphics graphics, Font font, Level level,
                                 int x, int y, int width, int height, int mouseX, int mouseY, long cycle) {
        float scale = Math.min(width / 120F, height / 206F);
        if (scale <= 0) return null;
        float originX = x + (width - 120 * scale) / 2, originY = y + (height - 206 * scale) / 2;
        float mx = (mouseX - originX) / scale, my = (mouseY - originY) / scale;
        Hover hover = null;
        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY, 0);
        graphics.pose().scale(scale, scale, 1);
        drawCenteredFit(graphics, font, Component.translatable("recipe.type.infusion"), 56, 0, 112, 0xFF505050);
        drawCenteredFit(graphics, font, Component.translatable("tc.inst").append(" ")
                .append(Component.translatable("tc.inst." + Math.min(5, infusion.instability() / 2))), 56, 194, 112, 0xFF505050);
        graphics.flush();
        RenderSystem.enableBlend();
        graphics.blit(OVERLAY, 0, 20, 112, 34, 0, 6, 112, 34, 512, 512);
        graphics.blit(OVERLAY, 0, 58, 120, 88, 400, 154, 120, 88, 512, 512);
        RenderSystem.disableBlend();
        ItemStack result = infusion.getResultItem(level.registryAccess()).copy();
        drawItem(graphics, font, result, 48, 28);
        if (inside(mx, my, 48, 28, 16, 16)) hover = new Hover(result, List.of());
        ItemStack center = declaredChoice(infusion.input(), level, cycle);
        drawItem(graphics, font, center, 48, 94);
        if (inside(mx, my, 48, 94, 16, 16)) hover = new Hover(center, List.of());
        int count = infusion.components().size();
        // Source's 360 / length is integer division before assignment to float.
        // Seven shards therefore use51-degree increments, not a normalized ring.
        int slice = count == 0 ? 0 : 360 / count;
        var selected = infusion.selectedComponents();
        for (int i = 0; i < count; i++) {
            double angle = (-90 + i * slice) / 180F * (float) Math.PI;
            int sx = 56 + (int)(Math.cos(angle) * 40) - 8;
            int sy = 102 + (int)(Math.sin(angle) * 40) - 8;
            ItemStack component = selected.get(i).displayStack(declaredChoice(infusion.components().get(i), level, cycle));
            if (!component.isEmpty()) infusion.componentDamage(i).ifPresent(component::setDamageValue);
            drawItem(graphics, font, component, sx, sy);
            if (inside(mx, my, sx, sy, 16, 16)) hover = new Hover(component, List.of());
        }
        var tags = infusion.aspects().keySet().stream().sorted().toList();
        int rows = (tags.size() - 1) / 5, shift = (5 - tags.size() % 5) * 10;
        for (int i = 0; i < tags.size(); i++) {
            Aspect aspect = Aspect.getAspect(tags.get(i));
            int sx = 8 + i % 5 * 20 + (i / 5 >= rows && (rows > 1 || tags.size() < 5) ? shift : 0);
            int sy = 164 - 10 * rows + i / 5 * 20;
            int color = aspect.getColor();
            graphics.flush();
            RenderSystem.enableBlend();
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
            graphics.blit(aspect.getImage(), sx, sy, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
            graphics.pose().pushPose();
            graphics.pose().translate(sx + 16, sy + 11, 200);
            graphics.pose().scale(.5F, .5F, 1);
            String amount = Integer.toString(infusion.aspects().get(aspect.getTag()));
            graphics.drawString(font, amount, -font.width(amount), 0, 0xFFFFFFFF, true);
            graphics.pose().popPose();
            if (inside(mx, my, sx, sy, 16, 16)) hover = new Hover(ItemStack.EMPTY,
                    List.of(Component.literal(aspect.getName()), Component.translatable("tc.aspect." + aspect.getTag())));
        }
        graphics.pose().popPose();
        return hover;
    }

    /** Original GuiResearchRecipe:1112-1235: cycling enchantment levels, XP, component ring and essentia. */
    private Hover renderEnchantmentInfusion(InfusionEnchantmentRecipe infusion, GuiGraphics graphics, Font font, Level level,
                                             int x, int y, int width, int height, int mouseX, int mouseY, long cycle) {
        float scale = Math.min(width / 120F, height / 206F);
        if (scale <= 0) return null;
        float originX = x + (width - 120 * scale) / 2, originY = y + (height - 206 * scale) / 2;
        float mx = (mouseX - originX) / scale, my = (mouseY - originY) / scale;
        var target = infusion.enchantment(level.registryAccess());
        int nextLevel = 1 + (int)Math.floorMod(cycle, (long)target.value().getMaxLevel());
        // A valid unenchanted exemplar supplies only the previous target level.
        // The original page describes the enchantment, rather than prescribing a central item.
        ItemStack prior = new ItemStack(infusion.getResultItem(level.registryAccess()).getItem());
        if (nextLevel > 1) prior.enchant(target, nextLevel - 1);
        var costs = infusion.aspectCost(prior, level.registryAccess());
        Hover hover = null;
        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY, 0);
        graphics.pose().scale(scale, scale, 1);
        drawCenteredFit(graphics, font, Component.translatable("recipe.type.infusionenchantment"), 56, 0, 112, 0xFF505050);
        drawCenteredFit(graphics, font, Enchantment.getFullname(target, nextLevel), 56, 24, 112, 0xFF705090);
        drawCenteredFit(graphics, font, Component.translatable("tc.port.book_recipe.xp_levels", infusion.xpLevels(prior, level.registryAccess())),
                56, 40, 112, 0xFF508850);
        drawCenteredFit(graphics, font, Component.translatable("tc.inst").append(" ")
                .append(Component.translatable("tc.inst." + Math.min(5, infusion.instability() / 2))), 56, 194, 112, 0xFF505050);
        graphics.flush();
        RenderSystem.enableBlend();
        graphics.blit(OVERLAY, 0, 58, 120, 88, 400, 154, 120, 88, 512, 512);
        RenderSystem.disableBlend();
        int count = infusion.components().size(), slice = count == 0 ? 0 : 360 / count;
        var selected = infusion.selectedComponents();
        for (int i = 0; i < count; i++) {
            double angle = (-90 + i * slice) / 180F * (float)Math.PI;
            int sx = 56 + (int)(Math.cos(angle) * 40) - 8, sy = 102 + (int)(Math.sin(angle) * 40) - 8;
            ItemStack component = selected.get(i).displayStack(declaredChoice(infusion.components().get(i), level, cycle));
            drawItem(graphics, font, component, sx, sy);
            if (inside(mx, my, sx, sy, 16, 16)) hover = new Hover(component, List.of());
        }
        var tags = infusion.aspects().keySet().stream().sorted().toList();
        int rows = (tags.size() - 1) / 5, shift = (5 - tags.size() % 5) * 10;
        for (int i = 0; i < tags.size(); i++) {
            Aspect aspect = Aspect.getAspect(tags.get(i));
            int sx = 8 + i % 5 * 20 + (i / 5 >= rows && (rows > 1 || tags.size() < 5) ? shift : 0);
            int sy = 164 - 10 * rows + i / 5 * 20, color = aspect.getColor();
            graphics.flush();
            RenderSystem.enableBlend();
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
            graphics.blit(aspect.getImage(), sx, sy, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
            graphics.pose().pushPose();
            graphics.pose().translate(sx + 16, sy + 11, 200);
            graphics.pose().scale(.5F, .5F, 1);
            String amount = Integer.toString(costs.getAmount(aspect));
            graphics.drawString(font, amount, -font.width(amount), 0, 0xFFFFFFFF, true);
            graphics.pose().popPose();
            if (inside(mx, my, sx, sy, 16, 16)) hover = new Hover(ItemStack.EMPTY,
                    List.of(Component.literal(aspect.getName()), Component.translatable("tc.aspect." + aspect.getTag())));
        }
        graphics.pose().popPose();
        return hover;
    }

    private static ItemStack declaredChoice(Ingredient ingredient, Level level, long cycle) {
        var enabled = java.util.Arrays.stream(ingredient.getItems())
                .filter(stack -> !stack.isEmpty() && stack.isItemEnabled(level.enabledFeatures())).toList();
        return enabled.isEmpty() ? ItemStack.EMPTY : enabled.get((int)Math.floorMod(cycle, (long)enabled.size())).copyWithCount(1);
    }

    /** Original GuiResearchRecipe:776-877, including the two-layer cauldron diagram. */
    private Hover renderCrucible(CrucibleRecipe crucible, GuiGraphics graphics, Font font, Level level,
                                 int x, int y, int width, int height, int mouseX, int mouseY, long cycle) {
        float scale = Math.min(width / 112F, height / 188F);
        if (scale <= 0) return null;
        float originX = x + (width - 112 * scale) / 2, originY = y + (height - 188 * scale) / 2;
        float mx = (mouseX - originX) / scale, my = (mouseY - originY) / scale;
        Hover hover = null;
        graphics.pose().pushPose();
        graphics.pose().translate(originX, originY, 0);
        graphics.pose().scale(scale, scale, 1);
        drawCenteredFit(graphics, font, Component.translatable("recipe.type.crucible"), 56, 0, 112, 0xFF505050);
        graphics.flush();
        RenderSystem.enableBlend();
        graphics.blit(OVERLAY, 0, 28, 112, 34, 0, 6, 112, 34, 512, 512);
        graphics.blit(OVERLAY, 0, 92, 112, 96, 0, 40, 112, 96, 512, 512);
        graphics.blit(OVERLAY, 42, 76, 22, 26, 200, 168, 22, 26, 512, 512);
        RenderSystem.disableBlend();
        ItemStack output = crucible.getResultItem(level.registryAccess());
        drawItem(graphics, font, output, 48, 36);
        if (inside(mx, my, 48, 36, 16, 16)) hover = new Hover(output, List.of());
        ItemStack[] choices = crucible.catalyst().getItems();
        if (choices.length > 0) {
            ItemStack catalyst = choices[(int) Math.floorMod(cycle, (long) choices.length)].copyWithCount(1);
            drawItem(graphics, font, catalyst, 26, 72);
            if (inside(mx, my, 26, 72, 16, 16)) hover = new Hover(catalyst, List.of());
        }
        var tags = crucible.aspects().keySet().stream().sorted().toList();
        int rows = (tags.size() - 1) / 3, shift = (3 - tags.size() % 3) * 10;
        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            Aspect aspect = Aspect.getAspect(tag);
            int sx = 28 + i % 3 * 20 + (i / 3 >= rows && (rows > 1 || tags.size() < 3) ? shift : 0);
            int sy = 128 - 10 * rows + i / 3 * 20;
            graphics.flush();
            RenderSystem.enableBlend();
            int color = aspect.getColor();
            graphics.setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
            graphics.blit(aspect.getImage(), sx, sy, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
            graphics.pose().pushPose();
            graphics.pose().translate(sx + 16, sy + 11, 200);
            graphics.pose().scale(.5F, .5F, 1);
            String amount = Integer.toString(crucible.aspects().get(tag));
            graphics.drawString(font, amount, -font.width(amount), 0, 0xFFFFFFFF, true);
            graphics.pose().popPose();
            if (inside(mx, my, sx, sy, 16, 16)) hover = new Hover(ItemStack.EMPTY,
                    List.of(Component.literal(aspect.getName()), Component.translatable("tc.aspect." + tag)));
        }
        graphics.pose().popPose();
        return hover;
    }

    private static void drawItem(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
    }

    private static void drawCenteredFit(GuiGraphics graphics, Font font, Component text, int center, int y, int width, int color) {
        float scale = Math.min(1, width / (float) Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(center, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, -font.width(text) / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private static boolean inside(float mx, float my, int x, int y, int width, int height) {
        return mx >= x && my >= y && mx < x + width && my < y + height;
    }

    private static Hover visHover(NativeArcaneRecipe recipe, String tag, Player player) {
        return visHover(recipe.vis(), tag, player);
    }

    /** The hover of an aspect of an arcane recipe, with the declared base cost of the page. */
    private static Hover visHover(Map<String, Integer> vis, String tag, Player player) {
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("tc.aspect." + tag));
        lines.add(Component.translatable("tc.port.book_recipe.base_cost", vis.get(tag)).withStyle(ChatFormatting.GRAY));
        ItemStack wand = ItemStack.EMPTY;
        if (player != null) {
            if (ArcaneVis.isCraftingWand(player.getMainHandItem())) wand = player.getMainHandItem();
            else if (ArcaneVis.isCraftingWand(player.getOffhandItem())) wand = player.getOffhandItem();
        }
        if (!wand.isEmpty()) {
            // Exactly the native crafting price (currently wood/iron's penalty),
            // not hypothetical armour/bauble/other-wand discount arithmetic.
            int cost = ArcaneVis.price(vis, wand, player).get(tag);
            lines.add(Component.translatable("tc.port.book_recipe.held_wand", wand.getHoverName()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tc.port.book_recipe.wand_cost", BigDecimal.valueOf(cost, 2).stripTrailingZeros().toPlainString()));
        } else lines.add(Component.translatable("tc.port.book_recipe.no_wand_context").withStyle(ChatFormatting.GRAY));
        return new Hover(ItemStack.EMPTY, List.copyOf(lines));
    }

    public record Hover(ItemStack stack, List<Component> lines) {
        public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
            if (!stack.isEmpty()) graphics.renderTooltip(font, stack, mouseX, mouseY);
            else graphics.renderTooltip(font, lines.stream()
                    .flatMap(line -> font.split(line, Math.max(40, Math.min(260, graphics.guiWidth() - 24))).stream()).toList(), mouseX, mouseY);
        }
    }
}
