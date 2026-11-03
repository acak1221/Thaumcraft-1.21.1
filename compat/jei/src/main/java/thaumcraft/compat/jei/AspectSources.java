package thaumcraft.compat.jei;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Dynamic, per-player lookups. Unscanned items never enter a source page or its ingredient slots. */
final class AspectSources implements ISimpleRecipeManagerPlugin<Pages.Source> {
    private final IIngredientManager ingredients;
    private ClientLevel previousLevel;
    private Collection<ItemStack> previousIngredients;
    private Set<String> previousScans = Set.of();
    private List<ItemStack> known = List.of();
    private int previousSize;

    AspectSources(IIngredientManager ingredients) { this.ingredients = ingredients; }

    @Override public boolean isHandledInput(ITypedIngredient<?> input) {
        return input.getIngredient(VanillaTypes.ITEM_STACK).filter(AspectSources::scanned).isPresent();
    }

    @Override public boolean isHandledOutput(ITypedIngredient<?> output) {
        return output.getIngredient(AspectIngredient.TYPE).filter(stack -> !forAspect(stack.aspect()).isEmpty()).isPresent();
    }

    @Override public List<Pages.Source> getRecipesForInput(ITypedIngredient<?> input) {
        return input.getIngredient(VanillaTypes.ITEM_STACK).filter(AspectSources::scanned)
                .map(stack -> pages(stack, null)).orElseGet(List::of);
    }

    @Override public List<Pages.Source> getRecipesForOutput(ITypedIngredient<?> output) {
        return output.getIngredient(AspectIngredient.TYPE).map(stack -> forAspect(stack.aspect())).orElseGet(List::of);
    }

    @Override public List<Pages.Source> getAllRecipes() {
        List<Pages.Source> result = new ArrayList<>();
        for (ItemStack item : knownItems()) result.addAll(pages(item, null));
        return List.copyOf(result);
    }

    private List<Pages.Source> forAspect(Aspect aspect) {
        if (aspect == null) return List.of();
        List<Pages.Source> result = new ArrayList<>();
        for (ItemStack item : knownItems()) result.addAll(pages(item, aspect));
        return List.copyOf(result);
    }

    private static boolean scanned(ItemStack item) {
        var client = Minecraft.getInstance();
        return client.player != null && client.level != null && !item.isEmpty()
                && ThaumcraftApiHelper.hasScannedItem(client.player, item);
    }

    /** Cache only scan membership. Resolve live object tags for every lookup, including addon changes. */
    private List<ItemStack> knownItems() {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            previousLevel = null; previousIngredients = null; previousScans = Set.of(); known = List.of();
            return known;
        }
        var items = ingredients.getAllIngredients(VanillaTypes.ITEM_STACK);
        var scans = KnowledgeAccess.get(client.player).getScannedItems();
        if (previousLevel != client.level || previousIngredients != items || previousSize != items.size() || !previousScans.equals(scans)) {
            List<ItemStack> candidates = new ArrayList<>();
            for (ItemStack item : items) if (scanned(item)) candidates.add(item.copyWithCount(1));
            candidates.sort(Comparator.comparing(item -> item.getHoverName().getString()));
            known = List.copyOf(candidates); previousLevel = client.level; previousIngredients = items;
            previousSize = items.size(); previousScans = Set.copyOf(scans);
        }
        return known;
    }

    private static List<Pages.Source> pages(ItemStack item, Aspect filter) {
        // Recheck against the live player even when an earlier query cached membership.
        if (!scanned(item)) return List.of();
        ItemStack single = item.copyWithCount(1);
        var aspects = ObjectAspectResolver.get(single, Minecraft.getInstance().level);
        if (aspects == null) return List.of();
        List<Pages.Source> result = new ArrayList<>();
        for (Aspect aspect : aspects.getAspectsSorted()) {
            int amount = aspects.getAmount(aspect);
            if (amount > 0 && (filter == null || filter == aspect)) result.add(new Pages.Source(single, aspect, amount));
        }
        return List.copyOf(result);
    }
}
