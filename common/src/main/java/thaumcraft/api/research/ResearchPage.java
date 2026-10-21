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

    public ResearchPage(String text) {
        this.type = PageType.TEXT;
        this.text = text;
    }

    public ResearchPage(String research, String text) {
        this.type = PageType.TEXT_CONCEALED;
        this.research = research;
        this.text = text;
    }

    public ResearchPage(Recipe<?> recipe) {
        this.type = PageType.NORMAL_CRAFTING;
        this.recipe = recipe;
        this.recipeOutput = recipe.getResultItem(registries()).copy();
    }

    public ResearchPage(Recipe<?>[] recipe) {
        this.type = PageType.NORMAL_CRAFTING;
        this.recipe = recipe;
    }

    public ResearchPage(IArcaneRecipe[] recipe) {
        this.type = PageType.ARCANE_CRAFTING;
        this.recipe = recipe;
    }

    public ResearchPage(CrucibleRecipe[] recipe) {
        this.type = PageType.CRUCIBLE_CRAFTING;
        this.recipe = recipe;
    }

    public ResearchPage(InfusionRecipe[] recipe) {
        this.type = PageType.INFUSION_CRAFTING;
        this.recipe = recipe;
    }

    public ResearchPage(List<Recipe<?>> recipe) {
        this.type = PageType.COMPOUND_CRAFTING;
        this.recipe = recipe;
    }

    public ResearchPage(IArcaneRecipe recipe) {
        this.type = PageType.ARCANE_CRAFTING;
        this.recipe = recipe;
        this.recipeOutput = recipe.getRecipeOutput();
    }

    public ResearchPage(CrucibleRecipe recipe) {
        this.type = PageType.CRUCIBLE_CRAFTING;
        this.recipe = recipe;
        this.recipeOutput = recipe.getRecipeOutput();
    }

    /** The original smelting page; without a world the output stays unknown. */
    public ResearchPage(ItemStack input) {
        this.type = PageType.SMELTING;
        this.recipe = input;
    }

    /** The native smelting page: the output is read from the live recipe manager. */
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

    public ResearchPage(InfusionRecipe recipe) {
        this.type = PageType.INFUSION_CRAFTING;
        this.recipe = recipe;
        this.recipeOutput = recipe.getRecipeOutput() instanceof ItemStack stack ? stack : recipe.getRecipeInput();
    }

    public ResearchPage(InfusionEnchantmentRecipe recipe) {
        this.type = PageType.INFUSION_ENCHANTMENT;
        this.recipe = recipe;
    }

    public ResearchPage(ResourceLocation image, String caption) {
        this.type = PageType.IMAGE;
        this.image = image;
        this.text = caption;
    }

    public ResearchPage(AspectList as) {
        this.type = PageType.ASPECTS;
        this.aspects = as;
    }

    /** The native corpus page behind this view. */
    public static ResearchPage of(thaumcraft.common.lib.research.ResearchDefinition.Page page) {
        ResearchPage result = new ResearchPage(page.textKey() == null ? "" : page.textKey());
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
