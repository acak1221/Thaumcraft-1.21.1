package thaumcraft.compat.jei;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.ScannedAspectSources;

/** Dynamic, per-player lookups. Unscanned items never enter a source page or its ingredient slots. */
final class AspectSources implements ISimpleRecipeManagerPlugin<Pages.Source> {
    private final ScannedAspectSources sources;
    private final IFocusFactory focuses;
    private final Map<Aspect, Converted> converted = new IdentityHashMap<>();
    private Converted all;

    private record Converted(List<ScannedAspectSources.Source> original, List<Pages.Source> pages) {}

    AspectSources(IIngredientManager ingredients, IFocusFactory focuses) {
        this.focuses = focuses;
        sources = new ScannedAspectSources(() -> ingredients.getAllIngredients(VanillaTypes.ITEM_STACK));
        // JEI returns a fresh wrapper for getAllIngredients. Its listener detects actual
        // mutations, including add/remove replacements that leave the size unchanged.
        ingredients.registerIngredientListener(new IIngredientManager.IIngredientListener() {
            @Override public <V> void onIngredientsAdded(IIngredientHelper<V> helper, Collection<ITypedIngredient<V>> added) {
                invalidate(helper);
            }

            @Override public <V> void onIngredientsRemoved(IIngredientHelper<V> helper, Collection<ITypedIngredient<V>> removed) {
                invalidate(helper);
            }

            private void invalidate(IIngredientHelper<?> helper) {
                if (helper.getIngredientType() == VanillaTypes.ITEM_STACK) sources.invalidateIngredients();
            }
        });
    }

    @Override public boolean isHandledInput(ITypedIngredient<?> input) {
        return input.getIngredient(VanillaTypes.ITEM_STACK).filter(AspectSources::scanned).isPresent();
    }

    @Override public boolean isHandledOutput(ITypedIngredient<?> output) {
        return output.getIngredient(AspectIngredient.TYPE).filter(stack -> stack.aspect() != null).isPresent();
    }

    @Override public List<Pages.Source> getRecipesForInput(ITypedIngredient<?> input) {
        return input.getIngredient(VanillaTypes.ITEM_STACK).filter(AspectSources::scanned)
                .map(stack -> pages(sources.forItem(stack))).orElseGet(List::of);
    }

    @Override public List<Pages.Source> getRecipesForOutput(ITypedIngredient<?> output) {
        return output.getIngredient(AspectIngredient.TYPE).map(stack -> {
            var rows = sources.forAspect(stack.aspect());
            if (sources.isPending()) {
                var focus = focuses.createFocus(RecipeIngredientRole.OUTPUT, AspectIngredient.TYPE,
                        new AspectIngredient.Stack(stack.aspect(), 1));
                return List.of(Pages.Source.loading(sources, focus));
            }
            Converted cached = converted.get(stack.aspect());
            if (cached == null || cached.original() != rows) {
                cached = new Converted(rows, pages(rows));
                converted.put(stack.aspect(), cached);
            }
            return cached.pages();
        }).orElseGet(List::of);
    }

    @Override public List<Pages.Source> getAllRecipes() {
        var rows = sources.all();
        if (all == null || all.original() != rows) all = new Converted(rows, pages(rows));
        return all.pages();
    }

    private static List<Pages.Source> pages(List<ScannedAspectSources.Source> sources) {
        return sources.stream().map(source -> new Pages.Source(source.item(), source.aspect(), source.amount())).toList();
    }

    private static boolean scanned(ItemStack item) {
        var client = Minecraft.getInstance();
        return client.player != null && client.level != null && !item.isEmpty()
                && ThaumcraftApiHelper.hasScannedItem(client.player, item);
    }
}
