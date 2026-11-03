package thaumcraft.client.gui;

import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

/** Original GuiThaumatorium:82-92,204-225: two 16x8 recipe arrows beside the output. */
final class ThaumatoriumRecipeNavigation {
    static final int ARROW_X = 128, PREVIOUS_Y = 16, NEXT_Y = 24, ARROW_WIDTH = 16, ARROW_HEIGHT = 8;
    private List<Integer> recipes = List.of();
    private int page;

    void refresh(int recipeCount, IntPredicate available) {
        recipes = IntStream.range(0, recipeCount).filter(available).boxed().toList();
        page = Math.min(page, Math.max(0, recipes.size() - 1));
    }

    int recipeIndex() { return recipes.isEmpty() ? -1 : recipes.get(page); }
    int page() { return page; }
    int size() { return recipes.size(); }
    boolean hasPrevious() { return page > 0; }
    boolean hasNext() { return page + 1 < recipes.size(); }

    boolean click(double x, double y) {
        if (recipes.size() <= 1 || x < ARROW_X || x >= ARROW_X + ARROW_WIDTH) return false;
        if (y >= PREVIOUS_Y && y < PREVIOUS_Y + ARROW_HEIGHT) {
            if (hasPrevious()) --page;
            return true;
        }
        if (y >= NEXT_Y && y < NEXT_Y + ARROW_HEIGHT) {
            if (hasNext()) ++page;
            return true;
        }
        return false;
    }
}
