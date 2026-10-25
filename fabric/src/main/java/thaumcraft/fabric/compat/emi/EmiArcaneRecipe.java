package thaumcraft.fabric.compat.emi;

import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * An arcane workbench recipe: the 3x3 grid, the result and the primal vis the wand pays.
 *
 * <p>The grid list always has nine entries in workbench slot order (x + y * 3), so the
 * workbench fill handler can move each input into the slot it is drawn in.</p>
 */
final class EmiArcaneRecipe extends ThaumcraftEmiRecipe {
    private final List<EmiIngredient> grid;
    private final List<AspectEmiStack> vis;
    private final boolean shapeless;

    EmiArcaneRecipe(ResourceLocation id, String research, List<EmiIngredient> grid, ItemStack result,
                    Map<String, Integer> vis, boolean shapeless) {
        super(ThaumcraftEmiPlugin.ARCANE, id, 118, 0, research);
        this.grid = List.copyOf(grid);
        this.vis = vis(vis);
        this.shapeless = shapeless;
        // All nine cells, empty ones included: the fill handler maps input i to grid slot i.
        this.inputs = this.grid;
        setAspects(this.vis);
        this.outputs = List.of(EmiStack.of(result));
        this.height = 58 + rows(this.vis.size(), 6) * 18 + (hasResearch() ? 11 : 0);
    }

    /** A shaped pattern laid out from the top-left corner, as the workbench grid reads it. */
    static List<EmiIngredient> shapedGrid(List<String> pattern, Map<String, Ingredient> key) {
        List<EmiIngredient> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(EmiStack.EMPTY);
        for (int y = 0; y < pattern.size(); y++) {
            String row = pattern.get(y);
            for (int x = 0; x < row.length(); x++) {
                Ingredient ingredient = key.get(row.substring(x, x + 1));
                if (ingredient != null && !ingredient.isEmpty()) grid.set(x + y * 3, EmiIngredient.of(ingredient));
            }
        }
        return grid;
    }

    static List<EmiIngredient> shapelessGrid(List<Ingredient> ingredients) {
        List<EmiIngredient> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(i < ingredients.size() ? EmiIngredient.of(ingredients.get(i)) : EmiStack.EMPTY);
        return grid;
    }

    static List<EmiIngredient> stackGrid(List<ItemStack> stacks) {
        List<EmiIngredient> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(i < stacks.size() && !stacks.get(i).isEmpty() ? EmiStack.of(stacks.get(i)) : EmiStack.EMPTY);
        return grid;
    }

    List<EmiIngredient> grid() { return grid; }

    @Override public void addWidgets(WidgetHolder widgets) {
        for (int i = 0; i < 9; i++) widgets.addSlot(grid.get(i), i % 3 * 18, i / 3 * 18);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 18);
        if (shapeless) widgets.addTexture(EmiTexture.SHAPELESS, 97, 0);
        widgets.addSlot(outputs.getFirst(), 92, 14).large(true).recipeContext(this);
        int y = addAspects(widgets, vis, 0, 58, 6);
        addResearch(widgets, y + 2);
    }
}
