package thaumcraft.common.lib.research;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;

/**
 * The research of add-ons, as the native systems read it.
 *
 * <p>The original kept one registry, {@code ResearchCategories}, and every system of the mod read the
 * {@code ResearchItem} objects in it. The native port reads the {@link ResearchDefinition} records of the
 * extracted corpus instead. This class turns the {@code ResearchItem} objects an add-on registered into the
 * same records and lays them over the corpus through {@link ResearchCatalog}, so the book, the research
 * table, the purchase of secondary research, the clues of a scan, the completion and the knowledge sync see
 * them like an original entry.</p>
 *
 * <p>An add-on of the original could also change an original entry through its public view, for example
 * {@code ResearchCategories.getResearch("FOCUSFIRE").setSiblings(...)} or a page appended to its page array.
 * Such a change is laid over the original entry: the parents, the hidden parents, the siblings, the pages,
 * the flags, the complexity and the aspect cost of the public view replace the original values. The icon,
 * the conditions and the item triggers of an original entry are never taken from the public view.</p>
 *
 * <p>The layer is rebuilt when an add-on registers an entry and when a world starts on either side, which
 * picks up changes an add-on makes to an entry after registering it.</p>
 */
public final class AddonResearch {
    /** The flags of the original {@code ResearchItem}, by the names the corpus uses. */
    private static final List<String> FLAGS = List.of("Special", "Stub", "Secondary", "Round", "Virtual",
            "Concealed", "Hidden", "Lost", "AutoUnlock");

    private AddonResearch() {}

    /** Rebuilds the add-on layer of the catalogue from the public registry. */
    public static synchronized void refresh() {
        ResearchCatalog.loadBundled();
        Map<String, ResearchDefinition> added = new LinkedHashMap<>();
        Map<String, ResearchDefinition> changed = new LinkedHashMap<>();
        for (ResearchCategoryList list : List.copyOf(ResearchCategories.researchCategories.values())) {
            if (list == null) continue;
            for (ResearchItem item : List.copyOf(list.research.values())) {
                if (item == null || item.key == null) continue;
                ResearchDefinition original = item.definition();
                if (original != null) {
                    ResearchDefinition view = changedOriginal(item, original);
                    if (view != original) changed.put(original.key(), view);
                } else if (!ResearchCatalog.isBundled(item.key)) {
                    added.put(item.key, of(item));
                }
            }
        }
        ResearchCatalog.replaceAddonLayer(added, changed);
        thaumcraft.api.ThaumcraftApi.clearRecipeKeyCache();
    }

    /** The record of an add-on entry. */
    public static ResearchDefinition of(ResearchItem item) {
        Objects.requireNonNull(item, "item");
        List<String> items = new ArrayList<>();
        ItemStack[] triggers = item.getItemTriggers();
        if (triggers != null) for (ItemStack stack : triggers) {
            if (stack != null && !stack.isEmpty()) items.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        return new ResearchDefinition(item.key, item.category, item.displayColumn, item.displayRow,
                item.getComplexity(), 0, costs(item), flags(item), keys(item.parents), keys(item.parentsHidden),
                keys(item.siblings), aspectTags(item.getAspectTriggers()), keys(item.getEntityTriggers()), items,
                null, pages(item.getPages(), List.of()), List.of());
    }

    /** The original entry as the public view now describes it, or the original record itself when nothing changed. */
    static ResearchDefinition changedOriginal(ResearchItem item, ResearchDefinition original) {
        Map<String, Integer> costs = costs(item);
        Set<String> flags = new LinkedHashSet<>(original.flags());
        flags.removeAll(FLAGS);
        flags.addAll(flags(item));
        List<String> parents = keys(item.parents), hidden = keys(item.parentsHidden), siblings = keys(item.siblings);
        List<ResearchDefinition.Page> pages = pages(item.getPages(), original.pages());
        // ResearchItem.of clamps the complexity of its view to 1..3; the clamp alone is not a change.
        int complexity = item.getComplexity() == Math.min(3, Math.max(1, original.complexity()))
                ? original.complexity() : item.getComplexity();
        if (costs.equals(original.aspectCost()) && flags.equals(original.flags()) && parents.equals(original.parents())
                && hidden.equals(original.parentsHidden()) && siblings.equals(original.siblings())
                && pages.equals(original.pages()) && complexity == original.complexity()) return original;
        return new ResearchDefinition(original.key(), original.category(), original.column(), original.row(),
                complexity, original.warp(), costs, flags, parents, hidden, siblings, original.aspectTriggers(),
                original.entityTriggers(), original.itemTriggerExpressions(), original.iconExpression(), pages,
                original.conditions());
    }

    /**
     * Original {@code ResearchManager} item trigger: a scanned stack matches a trigger of an add-on entry when
     * {@code InventoryUtils.areItemStacksEqual(trigger, scanned, true, true, false)} holds, that is the same
     * item with the same data, the damage of a durable item ignored. The ore dictionary shortcut of the
     * original has no 1.21.1 counterpart; an add-on that wants a whole family lists every item.
     */
    static boolean matchesTrigger(String researchKey, ResearchClues.Context context) {
        ResearchItem item = ResearchCategories.getResearch(researchKey);
        if (item == null || item.definition() != null) return false;
        if (context.entityName() != null) {
            String[] entities = item.getEntityTriggers();
            if (entities == null) return false;
            for (String entity : entities) {
                if (entity == null) continue;
                // An add-on may name the entity as the original did ("Zombie", "Thaumcraft.Firebat") or by its
                // 1.21.1 registry id ("minecraft:zombie", "myaddon:my_entity").
                if (entity.equals(context.entityName()) || EntityAspectBindings.originalName(entity).equals(context.entityName())) return true;
            }
            return false;
        }
        ItemStack[] triggers = item.getItemTriggers();
        if (triggers == null || context.item().isEmpty()) return false;
        ItemStack scanned = comparable(context.item());
        for (ItemStack trigger : triggers) {
            if (trigger != null && !trigger.isEmpty() && ItemStack.isSameItemSameComponents(comparable(trigger), scanned)) return true;
        }
        return false;
    }

    /**
     * The items an add-on page makes: the output the original constructors recorded, the outputs of its recipe
     * objects (a single recipe, an array or a list of them) and of its recipe ids. Server safe; nothing is
     * crafted. Used by the original click-through lookup {@code ThaumcraftApi.getCraftingRecipeKey}.
     */
    public static List<ItemStack> outputs(ResearchPage page) {
        List<ItemStack> outputs = new ArrayList<>();
        if (page == null) return outputs;
        if (page.recipeOutput != null && !page.recipeOutput.isEmpty()) outputs.add(page.recipeOutput);
        collect(page.recipe, outputs);
        if (page.recipeIds != null) for (var id : page.recipeIds) {
            var recipe = id == null ? null : thaumcraft.api.crafting.NativeRecipes.byId(id);
            if (recipe == null) continue;
            ItemStack result = recipe.getResultItem(thaumcraft.api.crafting.ArcaneRecipes.registries());
            if (!result.isEmpty()) outputs.add(result);
        }
        return outputs;
    }

    private static void collect(Object recipe, List<ItemStack> outputs) {
        switch (recipe) {
            case null -> { }
            case Object[] array -> { for (Object element : array) collect(element, outputs); }
            case Iterable<?> list -> { for (Object element : list) collect(element, outputs); }
            case thaumcraft.api.crafting.IArcaneRecipe arcane -> add(arcane.getRecipeOutput(), outputs);
            case thaumcraft.api.crafting.CrucibleRecipe crucible -> add(crucible.getRecipeOutput(), outputs);
            case thaumcraft.api.crafting.InfusionRecipe infusion -> add(infusion.getRecipeOutput() instanceof ItemStack stack ? stack : null, outputs);
            case net.minecraft.world.item.crafting.Recipe<?> vanilla -> add(vanilla.getResultItem(thaumcraft.api.crafting.ArcaneRecipes.registries()), outputs);
            case net.minecraft.world.item.crafting.RecipeHolder<?> holder -> collect(holder.value(), outputs);
            default -> { }
        }
    }

    private static void add(ItemStack stack, List<ItemStack> outputs) {
        if (stack != null && !stack.isEmpty()) outputs.add(stack);
    }

    private static ItemStack comparable(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        if (copy.isDamageableItem()) copy.setDamageValue(0);
        return copy;
    }

    private static Map<String, Integer> costs(ResearchItem item) {
        Map<String, Integer> costs = new LinkedHashMap<>();
        if (item.tags == null) return costs;
        for (Aspect aspect : item.tags.getAspects()) {
            if (aspect == null) continue;
            int amount = item.tags.getAmount(aspect);
            if (amount > 0) costs.put(aspect.getTag(), amount);
        }
        return costs;
    }

    private static Set<String> flags(ResearchItem item) {
        Set<String> flags = new LinkedHashSet<>();
        if (item.isSpecial()) flags.add("Special");
        if (item.isStub()) flags.add("Stub");
        if (item.isSecondary()) flags.add("Secondary");
        if (item.isRound()) flags.add("Round");
        if (item.isVirtual()) flags.add("Virtual");
        if (item.isConcealed()) flags.add("Concealed");
        if (item.isHidden()) flags.add("Hidden");
        if (item.isLost()) flags.add("Lost");
        if (item.isAutoUnlock()) flags.add("AutoUnlock");
        return flags;
    }

    private static List<String> keys(String[] values) {
        if (values == null) return List.of();
        return Arrays.stream(values).filter(Objects::nonNull).toList();
    }

    private static List<String> aspectTags(Aspect[] aspects) {
        if (aspects == null) return List.of();
        return Arrays.stream(aspects).filter(Objects::nonNull).map(Aspect::getTag).toList();
    }

    /**
     * The pages of a public view. A page that is the public view of a corpus page keeps the corpus page; a
     * text page is an ordinary text page; every other page is drawn from the add-on's page object.
     */
    private static List<ResearchDefinition.Page> pages(ResearchPage[] pages, List<ResearchDefinition.Page> corpus) {
        if (pages == null) return List.of();
        List<ResearchDefinition.Page> result = new ArrayList<>();
        for (ResearchPage page : pages) {
            if (page == null) continue;
            ResearchDefinition.Page original = page.corpusPage();
            if (original != null && corpus.contains(original)) {
                result.add(original);
                continue;
            }
            String required = page.research;
            boolean text = page.type == ResearchPage.PageType.TEXT || page.type == ResearchPage.PageType.TEXT_CONCEALED;
            if (text && page.text == null) continue;
            result.add(new ResearchDefinition.Page(text ? "text" : "api", page.text, null, required, null, page));
        }
        return result;
    }
}
