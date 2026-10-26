package thaumcraft.api.research;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionEnchantmentRecipe;
import thaumcraft.api.crafting.InfusionRecipe;

/**
 * The original TC4 research page.
 *
 * <p>Forced 1.21.1 changes: the vanilla {@code IRecipe} became {@link Recipe}, the furnace recipe registry
 * needs a {@link Level} to resolve a smelting output and the translated text is read through
 * {@link Component}.</p>
 */
public class ResearchPage {
    public PageType type = PageType.TEXT;
    public String text = null;
    public String research = null;
    public ResourceLocation image = null;
    public AspectList aspects = null;
    public Object recipe = null;
    public ItemStack recipeOutput = null;
    /** The native corpus recipe reference of this page, when it has one. */
    public String recipeKey = null;
    /**
     * The ids of the 1.21.1 recipes this page shows, for a page made by {@link #recipe(ResourceLocation...)};
     * null otherwise. Several ids are shown one per second, like an original recipe array.
     */
    public ResourceLocation[] recipeIds = null;
    /** The corpus page this object is the public view of; null for a page an add-on made. */
    private thaumcraft.common.lib.research.ResearchDefinition.Page corpusPage;

    /** Creates an unregistered text page retaining the nullable translation key; does not translate or change research state. */
    public ResearchPage(String text) {
        this.type = PageType.TEXT;
        this.text = text;
    }

    /** Creates an unregistered concealed text page retaining nullable research/text keys; does not unlock research. */
    public ResearchPage(String research, String text) {
        this.type = PageType.TEXT_CONCEALED;
        this.research = research;
        this.text = text;
    }

    /** Retains the vanilla recipe and copies its registry-resolved output; null recipe throws NPE, with no recipe registration or consumption. */
    public ResearchPage(Recipe<?> recipe) {
        this.type = PageType.NORMAL_CRAFTING;
        this.recipe = recipe;
        this.recipeOutput = recipe.getResultItem(registries()).copy();
    }

    /** Creates a vanilla crafting page retaining the nullable recipe array by reference; output remains null and no registration occurs. */
    public ResearchPage(Recipe<?>[] recipe) {
        this.type = PageType.NORMAL_CRAFTING;
        this.recipe = recipe;
    }

    /** Creates an arcane crafting page retaining the nullable recipe array by reference; output remains null and no registration occurs. */
    public ResearchPage(IArcaneRecipe[] recipe) {
        this.type = PageType.ARCANE_CRAFTING;
        this.recipe = recipe;
    }

    /** Creates a crucible crafting page retaining the nullable recipe array by reference; output remains null and no registration occurs. */
    public ResearchPage(CrucibleRecipe[] recipe) {
        this.type = PageType.CRUCIBLE_CRAFTING;
        this.recipe = recipe;
    }

    /** Creates an infusion crafting page retaining the nullable recipe array by reference; output remains null and no registration occurs. */
    public ResearchPage(InfusionRecipe[] recipe) {
        this.type = PageType.INFUSION_CRAFTING;
        this.recipe = recipe;
    }

    /** Creates a compound crafting page retaining the nullable list by reference; output remains null and no registration occurs. */
    public ResearchPage(List<Recipe<?>> recipe) {
        this.type = PageType.COMPOUND_CRAFTING;
        this.recipe = recipe;
    }

    /** Retains an arcane recipe and its returned output reference; null recipe throws NPE, without registering or consuming anything. */
    public ResearchPage(IArcaneRecipe recipe) {
        this.type = PageType.ARCANE_CRAFTING;
        this.recipe = recipe;
        this.recipeOutput = recipe.getRecipeOutput();
    }

    /** Retains a crucible recipe and its live output reference; null recipe throws NPE, without registering or consuming anything. */
    public ResearchPage(CrucibleRecipe recipe) {
        this.type = PageType.CRUCIBLE_CRAFTING;
        this.recipe = recipe;
        this.recipeOutput = recipe.getRecipeOutput();
    }

    /** The original smelting page: the furnace input and its result.
     * In 1.7.10 the global furnace registry resolved the output here; 1.21.1 keeps smelting recipes per world, so
     * recipeOutput stays null and the book resolves the result in the recipe manager of the client world when the
     * page is drawn. Use {@link #ResearchPage(ItemStack, Level)} when the output is needed on the server. */
    public ResearchPage(ItemStack input) {
        this.type = PageType.SMELTING;
        this.recipe = input;
    }

    /** Stores the nullable input by reference and copies the live smelting output when resolvable;
     * null/empty input, null level, no server or no recipe leaves recipeOutput null, without consuming input. */
    public ResearchPage(ItemStack input, Level level) {
        this.type = PageType.SMELTING;
        this.recipe = input;
        if (level != null && input != null && !input.isEmpty() && level.getServer() != null) {
            var manager = level.getServer().getRecipeManager();
            var smelting = manager.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                    new net.minecraft.world.item.crafting.SingleRecipeInput(input), level);
            smelting.ifPresent(holder -> this.recipeOutput = holder.value().getResultItem(level.registryAccess()).copy());
        }
    }

    /** Retains an infusion recipe and its stack output (or central input for patches) by reference; null recipe throws NPE, without registration. */
    public ResearchPage(InfusionRecipe recipe) {
        this.type = PageType.INFUSION_CRAFTING;
        this.recipe = recipe;
        this.recipeOutput = recipe.getRecipeOutput() instanceof ItemStack stack ? stack : recipe.getRecipeInput();
    }

    /** Creates an enchantment infusion page retaining the nullable recipe reference; output stays null, with no registration. */
    public ResearchPage(InfusionEnchantmentRecipe recipe) {
        this.type = PageType.INFUSION_ENCHANTMENT;
        this.recipe = recipe;
    }

    /** Creates an image page retaining nullable texture/caption references without loading an image or changing research. */
    public ResearchPage(ResourceLocation image, String caption) {
        this.type = PageType.IMAGE;
        this.image = image;
        this.text = caption;
    }

    /** Creates an aspect page retaining the nullable aspect-unit list by reference; does not spend aspects or register research. */
    public ResearchPage(AspectList as) {
        this.type = PageType.ASPECTS;
        this.aspects = as;
    }

    /**
     * A page that shows the 1.21.1 recipes with these ids: crafting, arcane, crucible, infusion or
     * enchantment infusion recipes, data-driven or not. This is the native way to show an add-on's JSON
     * recipes; the recipe objects of the original constructors still work for recipes registered through
     * {@code ThaumcraftApi}. The ids are resolved in the recipe manager of the client world when the page is
     * drawn; a page none of whose ids resolves is not shown. Not a 1.7.10 constructor.
     */
    public static ResearchPage recipe(ResourceLocation... ids) {
        ResearchPage page = new ResearchPage((String) null);
        page.type = PageType.NORMAL_CRAFTING;
        page.recipeIds = ids == null ? new ResourceLocation[0] : ids.clone();
        return page;
    }

    /** The corpus page this object is the public view of, or null for a page an add-on made. */
    public thaumcraft.common.lib.research.ResearchDefinition.Page corpusPage() { return corpusPage; }

    /** The native corpus page behind this view. */
    public static ResearchPage of(thaumcraft.common.lib.research.ResearchDefinition.Page page) {
        ResearchPage result = new ResearchPage(page.textKey() == null ? "" : page.textKey());
        result.corpusPage = page;
        result.type = switch (page.kind() == null ? "text" : page.kind()) {
            case "text" -> PageType.TEXT;
            case "recipe_reference" -> PageType.ARCANE_CRAFTING;
            // The corpus keeps the original expression of the eight array and smelting
            // pages. The original built them from the recipe arrays themselves, so the
            // page kind of the expression is the kind of the original page.
            case "legacy_expression" -> legacyType(page.javaExpression());
            default -> PageType.TEXT;
        };
        result.research = page.requiredResearch();
        result.recipeKey = page.recipeKey();
        return result;
    }

    /**
     * The original page kind of a retained corpus expression.
     *
     * <p>The eight expressions of the corpus are the original array and smelting pages:
     * {@code oracle thaumcraft/common/config/ConfigResearch.java:199} (BANNERS, an arcane
     * recipe array), {@code :374} (ORE, a normal recipe array) and the crucible, the jar label,
     * the primal arrow, the runic augmentation and the sceptre pages. The original built them from
     * the recipe arrays, so the kind of the page follows the kind of the array.</p>
     */
    public static PageType legacyType(String expression) {
        if (expression == null) return PageType.TEXT;
        if (expression.contains("CrucibleRecipe")) return PageType.CRUCIBLE_CRAFTING;
        if (expression.contains("InfusionRecipe")) return PageType.INFUSION_CRAFTING;
        if (expression.contains("IArcaneRecipe")) return PageType.ARCANE_CRAFTING;
        if (expression.contains("IRecipe")) return PageType.NORMAL_CRAFTING;
        if (expression.contains("new ItemStack")) return PageType.SMELTING;
        return PageType.IMAGE;
    }

    /** The original translated text; an unknown key falls back to the raw text. */
    public String getTranslatedText() {
        if (text == null) return "";
        String translated = Component.translatable(text).getString();
        return translated.isEmpty() ? text : translated;
    }

    /** The live registry view a recipe needs to answer its result. */
    public static net.minecraft.core.RegistryAccess registries() {
        return net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
    }

    /** Display-kind tokens, not recipe registration hooks, numeric IDs or progression stages. */
    public static enum PageType {
        TEXT,
        TEXT_CONCEALED,
        IMAGE,
        CRUCIBLE_CRAFTING,
        ARCANE_CRAFTING,
        ASPECTS,
        NORMAL_CRAFTING,
        INFUSION_CRAFTING,
        COMPOUND_CRAFTING,
        INFUSION_ENCHANTMENT,
        SMELTING;
    }
}
