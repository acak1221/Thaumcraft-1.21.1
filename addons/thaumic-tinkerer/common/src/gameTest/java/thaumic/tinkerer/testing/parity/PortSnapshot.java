package thaumic.tinkerer.testing.parity;

import java.util.List;
import java.util.Map;

/**
 * What the port registered, reduced to plain values (no Minecraft types), so the comparison can be tested without a
 * world. The GameTest builds it from the live registries; the unit tests build it from the oracle and edit it.
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 */
public record PortSnapshot(Map<String, PortResearch> research, List<PoolEntry> researchPool, List<PortRecipe> recipes,
        Map<String, Map<String, Integer>> objectTags) {

    /**
     * A snapshot without the object tags: they are then not compared (the unit tests that build a port for one purpose).
     * The GameTest always reads them from the registry.
     */
    public PortSnapshot(Map<String, PortResearch> research, List<PoolEntry> researchPool, List<PortRecipe> recipes) {
        this(research, researchPool, recipes, null);
    }

    /** The same snapshot with other recipes. */
    public PortSnapshot withRecipes(List<PortRecipe> other) {
        return new PortSnapshot(research, researchPool, other, objectTags);
    }

    /** The same snapshot with other research. */
    public PortSnapshot withResearch(Map<String, PortResearch> other) {
        return new PortSnapshot(other, researchPool, recipes, objectTags);
    }

    /** A page: kind as in the oracle ({@code text}, {@code arcane}, ...), the text key suffix, the shown recipes as "id xN". */
    public record PortPage(String kind, String text, List<String> recipes) {}

    /** {@code iconAspect} is the data component {@code thaumictinkerer:infused_aspect} of the icon stack (the infused seeds), else null. */
    public record PortResearch(String key, String category, Map<String, Integer> aspects, int column, int row, int complexity,
            List<String> parents, List<String> parentsHidden, List<String> siblings, Map<String, Boolean> flags, int warp,
            List<PortPage> pages, String icon, String iconAspect) {
        public PortResearch(String key, String category, Map<String, Integer> aspects, int column, int row, int complexity,
                List<String> parents, List<String> parentsHidden, List<String> siblings, Map<String, Boolean> flags, int warp,
                List<PortPage> pages, String icon) {
            this(key, category, aspects, column, row, complexity, parents, parentsHidden, siblings, flags, warp, pages, icon, null);
        }
    }

    /** A research of Thaumcraft or the add-on that a Kami entry may require (the six tabs the original looked at). */
    public record PoolEntry(String key, String category, boolean lost, boolean virtual) {}

    /**
     * A recipe of the port: {@code kind} as in the oracle; {@code cells} are the slots of a shaped recipe's full grid in
     * row-major order ({@code width * height} of them, an empty slot being an empty list: where the gaps are is part of
     * the shape), or the single slot of the central item of an infusion or the catalyst of a crucible recipe; each slot
     * lists the ids that can fill it (a tag has many). {@code outputAspect} is the data component
     * {@code thaumictinkerer:infused_aspect} of the result stack (the six infused seeds), else null.
     */
    public record PortRecipe(String id, String name, String kind, String research, String output, int count,
            Map<String, Integer> aspects, Integer instability, int width, int height, List<List<String>> cells,
            List<List<String>> components, String outputAspect) {
        public PortRecipe(String id, String name, String kind, String research, String output, int count,
                Map<String, Integer> aspects, Integer instability, int width, int height, List<List<String>> cells,
                List<List<String>> components) {
            this(id, name, kind, research, output, count, aspects, instability, width, height, cells, components, null);
        }

        /** The slots that hold something. */
        public int ingredientCount() {
            return (int) cells.stream().filter(c -> !c.isEmpty()).count() + components.size();
        }
    }
}
