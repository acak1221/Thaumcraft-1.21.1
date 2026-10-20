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
            alias("ThaumiumHoe", "thaumium_hoe"),           // 466
            alias("ThaumiumSword", "thaumium_sword"),       // 467
            alias("Tallow", "crucible/tallow"),                // 120
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
            alias("AlchemicalConstruct", "arcane/alchemical_construct"), // 290, FOUR basic tubes
            alias("Centrifuge", "arcane/centrifuge"),         // 292, no advanced-construct substitution
            alias("EssentiaCrystalizer", "arcane/essentia_crystallizer"), // 293, dispenser/balanced shards
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
            alias("WandRodGreatwood", "arcane/wand_rod_greatwood"), // 238
            alias("FocusFire", "arcane/focus_fire"),             // 247
            alias("FocusFrost", "arcane/focus_frost"),           // 248
            alias("FocusShock", "arcane/focus_shock"),           // 249
            alias("FocusExcavation", "arcane/focus_excavation"), // 251
            alias("InfusionMatrix", "arcane/infusion_matrix"),  // 221
            alias("ArcanePedestal", "arcane/arcane_pedestal"),  // 222
            alias("WandRodSilverwood", "infusion/wand_rod_silverwood"), // 336
            alias("WandRodObsidian", "infusion/wand_rod_obsidian"), // 311
            alias("WandRodIce", "infusion/wand_rod_ice"), // 312
            alias("WandRodQuartz", "infusion/wand_rod_quartz"), // 313
            alias("WandRodReed", "infusion/wand_rod_reed"), // 314
            alias("WandRodBlaze", "infusion/wand_rod_blaze"), // 315
            alias("WandRodBone", "infusion/wand_rod_bone"), // 316
            alias("TallowCandle", "tallow_candle"),            // 477
            alias("JarLabel", "jar_label"));                    // 416-418; dynamic bind/clear not static diagrams

    private final RecipeHolder<?> recipe;
    private final List<Ingredient> ingredients;
    private final int gridWidth;
    private final boolean shapeless;
    private final List<ThaumcraftRecipeDisplay> alternatives;
    private final boolean infusionAltar;

    private ThaumcraftRecipeDisplay(RecipeHolder<?> recipe, List<Ingredient> ingredients, int gridWidth, boolean shapeless) {
        this.recipe = recipe;
        this.ingredients = List.copyOf(ingredients);
        this.gridWidth = gridWidth;
        this.shapeless = shapeless;
        alternatives = List.of();
        infusionAltar = false;
    }

    private ThaumcraftRecipeDisplay(List<ThaumcraftRecipeDisplay> alternatives) {
        var first = alternatives.getFirst();
        recipe = first.recipe;
        ingredients = first.ingredients;
        gridWidth = first.gridWidth;
        shapeless = first.shapeless;
        this.alternatives = List.copyOf(alternatives);
        infusionAltar = false;
    }

    private ThaumcraftRecipeDisplay(RecipeHolder<?> matrixRecipe) {
        recipe = matrixRecipe;
        ingredients = List.of();
        gridWidth = 3;
        shapeless = false;
        alternatives = List.of();
        infusionAltar = true;
    }

    private static Map.Entry<String, ResourceLocation> alias(String original, String nativeId) {
        return Map.entry(original, ResourceLocation.fromNamespaceAndPath("thaumcraft", nativeId));
    }

    public static Map<String, ResourceLocation> aliases() { return ALIASES; }
    public RecipeHolder<?> recipe() { return recipe; }
    public boolean sameRecipes(ThaumcraftRecipeDisplay other) {
        if (recipe != other.recipe || infusionAltar != other.infusionAltar || alternatives.size() != other.alternatives.size()) return false;
        for (int i = 0; i < alternatives.size(); i++) if (!alternatives.get(i).sameRecipes(other.alternatives.get(i))) return false;
        return true;
    }

    /** Null means unavailable/unsupported, never an empty crafting page. No extra research gates are added here. */
    public static ThaumcraftRecipeDisplay resolve(ResearchDefinition.Page page, Level level) {
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
        if (!"recipe_reference".equals(page.kind()) || page.recipeKey() == null) return null;
        // Original ConfigRecipes:93 is a wand construct, NOT an executable
        // extracted expression or a registered fake crafting recipe. Display
        // only this implemented blueprint, bound to its actual native devices.
        if ("InfusionAltar".equals(page.recipeKey())) {
            var matrix = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/infusion_matrix")).orElse(null);
            return matrix != null && matrix.value() instanceof ArcaneShapedRecipe
                    && !matrix.value().getResultItem(level.registryAccess()).isEmpty()
                    && ThaumcraftInfusion.PEDESTAL.get().asItem() != net.minecraft.world.item.Items.AIR
                    ? new ThaumcraftRecipeDisplay(matrix) : null;
        }
        ResourceLocation id = ALIASES.get(page.recipeKey());
        if (id == null) return null;
        RecipeHolder<?> holder = level.getRecipeManager().byKey(id).orElse(null);
        if (holder == null) return null;
        return resolve(holder, level);
    }

    private static ThaumcraftRecipeDisplay resolve(RecipeHolder<?> holder, Level level) {
        var nativeRecipe = holder.value();
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
        if (infusionAltar) return renderInfusionAltar(graphics, font, x, y, width, height, mouseX, mouseY);
        if (recipe.value() instanceof InfusionStackRecipe infusion) return renderInfusion(infusion, graphics, font, level,
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
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("tc.aspect." + tag));
        lines.add(Component.translatable("tc.port.book_recipe.base_cost", recipe.vis().get(tag)).withStyle(ChatFormatting.GRAY));
        ItemStack wand = ItemStack.EMPTY;
        if (player != null) {
            if (ArcaneVis.isCraftingWand(player.getMainHandItem())) wand = player.getMainHandItem();
            else if (ArcaneVis.isCraftingWand(player.getOffhandItem())) wand = player.getOffhandItem();
        }
        if (!wand.isEmpty()) {
            // Exactly the native crafting price (currently wood/iron's penalty),
            // not hypothetical armour/bauble/other-wand discount arithmetic.
            int cost = ArcaneVis.price(recipe.vis(), wand, player).get(tag);
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
