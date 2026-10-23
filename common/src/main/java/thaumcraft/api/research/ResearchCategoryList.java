package thaumcraft.api.research;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The original TC4 research category: the cells of one research tab.
 *
 * <p>Forced 1.21.1 change: the original used a {@code HashMap}; the native port keeps the insertion order of
 * the original corpus, so the browser draws the same cells in the same order.</p>
 */
public class ResearchCategoryList {
    public int minDisplayColumn;
    public int minDisplayRow;
    public int maxDisplayColumn;
    public int maxDisplayRow;
    public ResourceLocation icon;
    public ResourceLocation background;
    public Map<String, ResearchItem> research = new LinkedHashMap<>();

    /** Creates an empty unregistered category with nullable icon/background references and zero research-grid bounds; no global state changes. */
    public ResearchCategoryList(ResourceLocation icon, ResourceLocation background) {
        this.icon = icon;
        this.background = background;
    }

    /** The research entry of this category, or null. */
    public ResearchItem getResearch(String key) {
        return key == null ? null : research.get(key);
    }

    /** The number of research entries in this category. */
    public int size() {
        return research.size();
    }
}
