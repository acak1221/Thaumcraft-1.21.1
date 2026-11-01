package thaumcraft.client.gui;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.lib.research.ResearchBrowserData;
import thaumcraft.common.lib.research.ResearchCatalog;
import thaumcraft.common.lib.research.ResearchDefinition;

/**
 * The search box of the Thaumonomicon, a setting adopted from TC4Tweaks (which took it from
 * Witching Gadgets). TC4 itself has no search.
 *
 * <p>Like the original idea it only ever looks at research the player has completed, so it reveals
 * nothing: a research matches by its name or its key, and through the items its readable recipe
 * pages make ("where is the recipe for this?"). An open book rebuilds its index when knowledge,
 * the research catalogue or the world's recipe collection changes.</p>
 */
public final class ResearchSearch {
    /** One searchable line: a completed research, or an item one of its pages makes. */
    public record Entry(ResearchDefinition research, String text, ItemStack item) {}

    private final List<Entry> entries;

    private ResearchSearch(List<Entry> entries) { this.entries = List.copyOf(entries); }

    /** One open book's index; knowledge snapshots can change while the map stays open after a purchase. */
    static final class Cache {
        private Set<String> completed;
        private Level level;
        private Object catalogue, recipes;
        private ResearchSearch index;
        private String query;
        private List<Entry> results = List.of();

        List<Entry> find(String query, Set<String> completed, Level level) {
            if (query.isBlank()) return List.of();
            Object catalogue = ResearchCatalog.all();
            Object recipes = level == null ? null : level.getRecipeManager().getRecipes();
            if (index == null || this.level != level || !completed.equals(this.completed)
                    || this.catalogue != catalogue || this.recipes != recipes) {
                this.completed = Set.copyOf(completed);
                this.level = level;
                this.catalogue = catalogue; this.recipes = recipes;
                index = ResearchSearch.index(this.completed, level);
                this.query = null;
            }
            if (!query.equals(this.query)) {
                this.query = query;
                results = index.find(query);
            }
            return results;
        }

        void clear() { completed = null; level = null; catalogue = recipes = null; index = null; query = null; results = List.of(); }
    }

    /** The completed, visible research, then the results of their visible recipe pages. */
    public static ResearchSearch index(Set<String> completed, Level level) {
        List<Entry> entries = new ArrayList<>();
        for (ResearchDefinition research : ResearchCatalog.all()) {
            if (!completed.contains(research.key()) || !ResearchBrowserData.isVisible(research, completed)) continue;
            entries.add(new Entry(research, Component.translatable(research.nameKey()).getString(), ItemStack.EMPTY));
            if (level == null) continue;
            for (ResearchDefinition.Page page : research.pages()) {
                if (!page.isVisible(completed)) continue;
                ThaumcraftRecipeDisplay display;
                try {
                    display = ThaumcraftRecipeDisplay.resolve(page, research.key(), level);
                } catch (RuntimeException unresolved) {
                    continue; // a page the book itself cannot draw is not searchable either
                }
                if (display == null) continue;
                for (ItemStack result : display.results(level))
                    entries.add(new Entry(research, result.getHoverName().getString(), result.copyWithCount(1)));
            }
        }
        return new ResearchSearch(entries);
    }

    /**
     * The matches of a query: research whose name matches first, then items, each item once.
     * A research that matched by name is not listed again for its items, nor is an item that has the
     * name of a listed research.
     */
    public List<Entry> find(String query) {
        String wanted = normalize(query);
        if (wanted.isEmpty()) return List.of();
        List<Entry> byName = new ArrayList<>(), byItem = new ArrayList<>();
        Set<String> named = new HashSet<>(), items = new HashSet<>();
        for (Entry entry : entries) {
            if (!entry.item().isEmpty()) continue;
            if (matches(entry.text(), wanted) || matches(entry.research().key(), wanted)) {
                byName.add(entry);
                named.add(entry.research().key());
                items.add(normalize(entry.text())); // an item named like a listed research adds nothing
            }
        }
        for (Entry entry : entries) {
            if (entry.item().isEmpty() || named.contains(entry.research().key())) continue;
            if (matches(entry.text(), wanted) && items.add(normalize(entry.text()))) byItem.add(entry);
        }
        Comparator<Entry> alphabetical = Comparator.comparing(entry -> normalize(entry.text()));
        byName.sort(alphabetical);
        byItem.sort(alphabetical);
        List<Entry> found = new ArrayList<>(byName);
        found.addAll(byItem);
        return found;
    }

    /** Whether a normalized query occurs in the text, ignoring case, accents and formatting codes. */
    static boolean matches(String text, String normalizedQuery) {
        return !normalizedQuery.isEmpty() && normalize(text).contains(normalizedQuery);
    }

    /** Lower case without formatting codes, accents or surrounding spaces; ё reads as е. */
    static String normalize(String text) {
        if (text == null) return "";
        String plain = text.replaceAll("§.", "").strip().toLowerCase(Locale.ROOT).replace('ё', 'е');
        return Normalizer.normalize(plain, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
