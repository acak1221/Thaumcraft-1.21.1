package thaumic.tinkerer.testing.parity;

import java.util.List;
import java.util.Map;

/**
 * What the port registered, reduced to plain values (no Minecraft types), so the comparison can be tested without a
 * world. The GameTest builds it from the live registries; the unit tests build it from the oracle and edit it.
 * Test-only code of the port (no 1.7.10 counterpart), CC BY-NC-SA 3.0 like the add-on.
 */
public record PortSnapshot(Map<String, PortResearch> research, List<PoolEntry> researchPool, List<PortRecipe> recipes) {

    /** A page: kind as in the oracle ({@code text}, {@code arcane}, ...), the text key suffix, the shown recipes as "id xN". */
    public record PortPage(String kind, String text, List<String> recipes) {}

    public record PortResearch(String key, String category, Map<String, Integer> aspects, int column, int row, int complexity,
            List<String> parents, List<String> parentsHidden, List<String> siblings, Map<String, Boolean> flags, int warp,
            List<PortPage> pages, String icon) {}

    /** A research of Thaumcraft or the add-on that a Kami entry may require (the six tabs the original looked at). */
    public record PoolEntry(String key, String category, boolean lost, boolean virtual) {}

    /**
     * A recipe of the port: {@code kind} as in the oracle; {@code ingredients} are the cells of a pattern in row-major
     * order (an empty cell is skipped), input + components of an infusion or the catalyst; each ingredient lists the
     * ids that can fill it (a tag has many).
     */
    public record PortRecipe(String id, String name, String kind, String research, String output, int count,
            Map<String, Integer> aspects, Integer instability, int width, int height, List<List<String>> cells,
            List<List<String>> components) {
        public int ingredientCount() {
            return cells.size() + components.size();
        }
    }
}
