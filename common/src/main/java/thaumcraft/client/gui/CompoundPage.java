package thaumcraft.client.gui;

import java.util.List;
import thaumcraft.api.aspects.AspectList;

/**
 * The content of a COMPOUND_CRAFTING page: the list of the original, read once.
 *
 * <p>Original 4.2.3.5: {@code ResearchPage(List)} keeps the list as the recipe of the page, and
 * {@code GuiResearchRecipe.drawCompoundCraftingPage} reads it as {@code (AspectList) r.get(0)}, the vis the wand
 * spends to form the construct, {@code (Integer) r.get(1)}, {@code r.get(2)} and {@code r.get(3)}, the width, the
 * height and the depth, and {@code (List) r.get(4)}, the blocks in the order of {@link CompoundLayout#cellsInOrder()}
 * (the constructs of the original are {@code ConfigRecipes.initializeCompoundRecipes}). A list of another shape
 * stopped the original with a {@code ClassCastException}; the port does not show such a page.</p>
 *
 * <p>The entries are kept by reference, as the original did, and read when the page is drawn: an
 * {@code ItemStack}, a {@code String} ore dictionary name, a list of choices (shown one per second, like the
 * original {@code InventoryUtils.cycleItemStack}), the 1.21.1 forms of these, or null for a cell that shows nothing
 * (see {@code ThaumcraftRecipeDisplay}).</p>
 *
 * @param aspects the vis of the construct; null or empty draws no aspect row
 * @param layout  the size of the construct
 * @param entries the blocks, by reference; a missing entry is an empty cell
 * @param source  what identifies the page while the book is open (the list of an add-on page, the recipe key of an
 *                original page), so that an open book does not lay its pages out again every frame
 */
public record CompoundPage(AspectList aspects, CompoundLayout layout, List<?> entries, Object source) {
    /** The page of an original list, or null when the list does not have the shape the original read. */
    public static CompoundPage parse(Object recipe) {
        if (!(recipe instanceof List<?> list) || list.size() < 5) return null;
        Object aspects = list.get(0);
        if (aspects != null && !(aspects instanceof AspectList)) return null;
        if (!(list.get(1) instanceof Integer dx) || !(list.get(2) instanceof Integer dy) || !(list.get(3) instanceof Integer dz)) return null;
        if (!(list.get(4) instanceof List<?> entries)) return null;
        if (!CompoundLayout.valid(dx, dy, dz)) return null;
        return new CompoundPage((AspectList) aspects, new CompoundLayout(dx, dy, dz), entries, recipe);
    }

    /** The entry of a cell, or null when the list is shorter than the construct. */
    public Object entry(int index) {
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }
}
