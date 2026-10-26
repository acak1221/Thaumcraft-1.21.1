package thaumcraft.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.RecipeViews;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftInfusion;
import thaumcraft.common.config.ThaumcraftRunic;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.crafting.NativeArcaneRecipe;
import thaumcraft.common.crafting.ProgrammaticRecipes;

/**
 * Optional JEI integration: the arcane, crucible and infusion recipes and the aspect combinations,
 * the same pages the EMI plugin lists ({@link RecipeViews} builds the ones that are not one grid).
 *
 * <p>JEI loads this class itself: on Fabric through the {@code jei_mod_plugin} entrypoint of
 * fabric.mod.json, on NeoForge through {@link JeiPlugin}. Nothing else references it, so the mod
 * runs unchanged without JEI. Every recipe is read from the recipes the server synchronized, so
 * datapack edits appear as well. Like the EMI plugin, the source is compiled into each loader
 * module, not into common.</p>
 */
@JeiPlugin
public final class ThaumcraftJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("Thaumcraft/JEI");

    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath("thaumcraft", "jei"); }

    @Override public void registerIngredients(IModIngredientRegistration registration) {
        List<AspectIngredient.Stack> aspects = new ArrayList<>();
        for (Aspect aspect : Aspect.aspects.values()) aspects.add(new AspectIngredient.Stack(aspect, 1));
        registration.register(AspectIngredient.TYPE, aspects, new AspectIngredient.Helper(), new AspectIngredient.Renderer(), AspectIngredient.Stack.CODEC);
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        Categories.all(registration.getJeiHelpers().getGuiHelper()).forEach(registration::addRecipeCategories);
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ThaumcraftArcane.ARCANE_WORKBENCH_ITEM.get(), Categories.ARCANE);
        registration.addRecipeCatalyst(ThaumcraftAlchemy.CRUCIBLE_ITEM.get(), Categories.CRUCIBLE);
        registration.addRecipeCatalyst(ThaumcraftInfusion.MATRIX_ITEM.get(), Categories.INFUSION, Categories.INFUSION_ENCHANTMENT);
        registration.addRecipeCatalyst(ThaumcraftContent.RESEARCH_TABLE_ITEM.get(), Categories.COMBINATION);
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        List<Pages.Combination> combinations = new ArrayList<>();
        for (Aspect aspect : Aspect.aspects.values()) {
            if (!aspect.isPrimal() && aspect.getComponents() != null) combinations.add(new Pages.Combination(aspect));
        }
        registration.addRecipes(Categories.COMBINATION, combinations);

        var level = Minecraft.getInstance().level;
        if (level == null) {
            LOGGER.warn("No client level while JEI reloads; Thaumcraft recipes are not listed");
            return;
        }
        RecipeManager manager = level.getRecipeManager();
        HolderLookup.Provider registries = level.registryAccess();
        registration.addRecipes(Categories.ARCANE, guarded("arcane", manager.getAllRecipesFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get()),
                holder -> arcane(holder, registries)));
        registration.addRecipes(Categories.CRUCIBLE, guarded("crucible", ProgrammaticRecipes.crucibles(manager), holder -> List.of(
                new Pages.Crucible(holder.id(), holder.value().research(), holder.value().catalyst(), holder.value().getResultItem(registries),
                        holder.value().aspects()))));
        List<RecipeViews.Infusion> infusions = new ArrayList<>(guarded("infusion", manager.getAllRecipesFor(ThaumcraftInfusion.INFUSION_RECIPE_TYPE.get()),
                holder -> List.of(RecipeViews.infusion(holder.id(), holder.value(), registries))));
        infusions.addAll(guarded("runic augmentation", manager.getAllRecipesFor(ThaumcraftRunic.AUGMENT_TYPE.get()),
                holder -> RecipeViews.runic(holder.id(), holder.value())));
        registration.addRecipes(Categories.INFUSION, infusions);
        registration.addRecipes(Categories.INFUSION_ENCHANTMENT, guarded("enchantment infusion",
                manager.getAllRecipesFor(ThaumcraftInfusion.ENCHANTMENT_RECIPE_TYPE.get()),
                holder -> RecipeViews.enchantment(holder.id(), holder.value(), registries).map(List::of).orElse(List.of())));
    }

    /** One broken datapack or addon recipe must not hide every other Thaumcraft recipe. */
    private static <T, R> List<R> guarded(String kind, List<T> holders, Function<T, List<R>> convert) {
        List<R> pages = new ArrayList<>();
        for (T holder : holders) {
            try {
                pages.addAll(convert.apply(holder));
            } catch (RuntimeException broken) {
                LOGGER.warn("Skipping {} recipe {} in JEI", kind, holder instanceof RecipeHolder<?> recipe ? recipe.id() : holder, broken);
            }
        }
        return pages;
    }

    private static List<Pages.Arcane> arcane(RecipeHolder<NativeArcaneRecipe> holder, HolderLookup.Provider registries) {
        NativeArcaneRecipe recipe = holder.value();
        if (recipe instanceof ArcaneShapedRecipe shaped) {
            return List.of(new Pages.Arcane(holder.id(), shaped.research(), shapedGrid(shaped.pattern(), shaped.key()),
                    shaped.getResultItem(registries), shaped.vis(), false));
        }
        if (recipe instanceof ArcaneShapelessRecipe shapeless) {
            List<Ingredient> grid = new ArrayList<>();
            for (int i = 0; i < 9; i++) grid.add(i < shapeless.ingredients().size() ? shapeless.ingredients().get(i) : Ingredient.EMPTY);
            return List.of(new Pages.Arcane(holder.id(), shapeless.research(), grid, shapeless.getResultItem(registries), shapeless.vis(), true));
        }
        return RecipeViews.constructs(recipe, registries).stream().map(view -> new Pages.Arcane(view.id(), view.research(),
                view.grid().stream().map(stack -> stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack)).toList(),
                view.result(), view.vis(), false)).toList();
    }

    /** A shaped pattern laid out from the top-left corner, as the workbench grid reads it. */
    private static List<Ingredient> shapedGrid(List<String> pattern, Map<String, Ingredient> key) {
        List<Ingredient> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(Ingredient.EMPTY);
        for (int y = 0; y < pattern.size(); y++) {
            String row = pattern.get(y);
            for (int x = 0; x < row.length(); x++) {
                Ingredient ingredient = key.get(row.substring(x, x + 1));
                if (ingredient != null && !ingredient.isEmpty()) grid.set(x + y * 3, ingredient);
            }
        }
        return grid;
    }
}
