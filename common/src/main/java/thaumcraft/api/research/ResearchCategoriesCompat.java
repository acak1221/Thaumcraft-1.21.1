package thaumcraft.api.research;

/**
 * The legacy bridge between the original research registry and the legacy entry view.
 *
 * <p>The native port publishes the original corpus through {@link ResearchCategories}; this bridge only
 * changes the shape of the answer.</p>
 */
public class ResearchCategoriesCompat {
    private ResearchCategoriesCompat() {}

    /** The legacy view of one research entry, or null when the key is unknown. */
    public static ResearchEntry getResearch(String key) {
        ResearchItem item = ResearchCategories.getResearch(key);
        return item == null ? null : ResearchEntry.of(item);
    }

    /** The category with the original knowledge formula, or null. */
    public static ResearchCategory getResearchCategory(String key) {
        ResearchCategoryList category = ResearchCategories.getResearchList(key);
        return category instanceof ResearchCategory value ? value : null;
    }
}
