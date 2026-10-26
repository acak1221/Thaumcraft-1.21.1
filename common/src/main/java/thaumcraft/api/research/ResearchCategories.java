package thaumcraft.api.research;

import java.util.Collection;
import java.util.LinkedHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.common.config.ThaumcraftResearchCategories;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;

/**
 * The original TC4 research registry.
 *
 * <p>The native port publishes the original corpus through {@link ResearchCatalog}; this class is the public
 * view of it and keeps the original registration methods, so an add-on may still add its own research.</p>
 */
public class ResearchCategories {
    private static final Logger LOG = LoggerFactory.getLogger("Thaumcraft");
    /** The original public map; it stays mutable so an add-on may add a category or a research entry. */
    public static LinkedHashMap<String, ResearchCategoryList> researchCategories = new LinkedHashMap<>();
    private static boolean nativeLoaded;
    private static boolean nativeBuilding;

    private ResearchCategories() {}

    /** Publishes the original corpus into the public map. Repeated calls do nothing. */
    public static synchronized void loadNative() {
        if (nativeLoaded || nativeBuilding) return;
        nativeBuilding = true;
        try {
            ResearchCatalog.loadBundled();
            for (ResearchDefinition definition : ResearchCatalog.bundled()) {
                if (!researchCategories.containsKey(definition.category())) {
                    // Original ConfigResearch.initCategories:67-73 registers every
                    // category with the texture of its tab icon and its background
                    // before the first research entry is added.
                    researchCategories.put(definition.category(), ThaumcraftResearchCategories.create(definition.category()));
                }
            }
            for (ResearchDefinition definition : ResearchCatalog.bundled()) {
                addResearch(ResearchItem.of(definition));
            }
        } finally {
            nativeBuilding = false;
            nativeLoaded = true;
        }
    }

    /** Initializes the global native corpus then returns the live category, or null for an absent key (including null unless manually registered). */
    public static ResearchCategoryList getResearchList(String key) {
        loadNative();
        return researchCategories.get(key);
    }

    /** The original translated category name. */
    public static String getCategoryName(String key) {
        return Component.translatable("tc.research_category." + key).getString();
    }

    /** Initializes the global native corpus and returns the first live matching entry, or null for null/absent keys in ordinary registrations. */
    public static ResearchItem getResearch(String key) {
        loadNative();
        for (ResearchCategoryList cat : researchCategories.values()) {
            ResearchItem ri = cat.research.get(key);
            if (ri != null) return ri;
        }
        return null;
    }

    /** Every research entry of the public registry. */
    public static Collection<ResearchItem> allResearch() {
        loadNative();
        var result = new java.util.ArrayList<ResearchItem>();
        for (ResearchCategoryList cat : researchCategories.values()) result.addAll(cat.research.values());
        return result;
    }

    /**
     * Initializes native research then registers a category only if absent, retaining nullable key/icon/background without copying or validation.
     *
     * <p>An add-on category gets its own tab in the Thaumonomicon after the six original ones, in registration
     * order, as in the original. {@code icon} is drawn over the whole 16x16 tab icon whatever its pixel size;
     * {@code background} is the scrolling map sheet, read with the geometry of the original 512x512 sheet
     * ({@code thaumcraft:textures/gui/gui_researchback.png}). The tab tooltip is the translation of
     * {@code tc.research_category.<key>}.</p>
     */
    public static synchronized void registerCategory(String key, ResourceLocation icon, ResourceLocation background) {
        loadNative();
        if (researchCategories.get(key) == null) {
            researchCategories.put(key, new ResearchCategoryList(icon, background));
        }
    }

    /** The keys of every category, the six original ones first, then add-on categories in registration order. */
    public static java.util.List<String> categoryKeys() {
        loadNative();
        return java.util.List.copyOf(researchCategories.keySet());
    }

    /**
     * The original registration: a cell may only be used once, unless the research is virtual.
     *
     * <p>An add-on entry registered here is research of the game, not only of this map: the Thaumonomicon,
     * the research table, the purchase of secondary research, scan clues ({@code setItemTriggers},
     * {@code setEntityTriggers}, {@code setAspectTriggers}), completion and sync read it. Its name and text are the
     * translations of {@code tc.research_name.<key>} and {@code tc.research_text.<key>}; its page texts are
     * translation keys. Register on both sides during mod initialization, like the original, so a client and
     * its server know the same keys.</p>
     */
    public static synchronized void addResearch(ResearchItem ri) {
        loadNative();
        if (ri == null || ri.key == null) return;
        ResearchCategoryList rl = researchCategories.get(ri.category);
        if (rl == null || rl.research.containsKey(ri.key)) return;
        if (!ri.isVirtual()) {
            for (ResearchItem rr : rl.research.values()) {
                if (rr.displayColumn != ri.displayColumn || rr.displayRow != ri.displayRow) continue;
                LOG.error("[Thaumcraft] Research [{}] not added as it overlaps with existing research [{}]", ri.getName(), rr.getName());
                return;
            }
        }
        rl.research.put(ri.key, ri);
        if (ri.displayColumn < rl.minDisplayColumn) rl.minDisplayColumn = ri.displayColumn;
        if (ri.displayRow < rl.minDisplayRow) rl.minDisplayRow = ri.displayRow;
        if (ri.displayColumn > rl.maxDisplayColumn) rl.maxDisplayColumn = ri.displayColumn;
        if (ri.displayRow > rl.maxDisplayRow) rl.maxDisplayRow = ri.displayRow;
        if (ri.definition() == null && !nativeBuilding) thaumcraft.common.lib.research.AddonResearch.refresh();
    }
}
