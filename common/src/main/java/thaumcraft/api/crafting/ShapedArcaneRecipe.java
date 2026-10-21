package thaumcraft.api.crafting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;

/**
 * The original TC4 shaped arcane recipe.
 *
 * <p>The native port owns the real recipe in {@link ArcaneShapedRecipe}; this class keeps the original
 * argument shape and delegates every answer to the native recipe.</p>
 *
 * <p>Forced 1.21.1 changes: the inventory became the native {@link ArcaneRecipeInput} and a recipe
 * argument that names an ore group became a namespaced item tag.</p>
 */
public class ShapedArcaneRecipe implements IArcaneRecipe {
    public ItemStack output = null;
    public Object[] input = null;
    public AspectList aspects = null;
    public String research;
    public int width = 0;
    public int height = 0;
    private boolean mirrored = true;
    private final ItemStack result;
    private final List<String> rows;
    private final Map<String, Ingredient> key;
    private ArcaneShapedRecipe nativeRecipe;
    private boolean nativeMirrored = true;

    public ShapedArcaneRecipe(String research, Block result, AspectList aspects, Object... recipe) {
        this(research, new ItemStack(result), aspects, recipe);
    }

    public ShapedArcaneRecipe(String research, Item result, AspectList aspects, Object... recipe) {
        this(research, new ItemStack(result), aspects, recipe);
    }

    public ShapedArcaneRecipe(String research, ItemStack result, AspectList aspects, Object... recipe) {
        this.research = research;
        this.aspects = aspects;
        this.result = result.copy();
        this.output = result.copy();
        Object[] arguments = recipe;
        int[] index = {0};
        if (arguments.length > 0 && arguments[0] instanceof Boolean flag) {
            this.mirrored = flag;
            if (arguments.length > 1 && arguments[1] instanceof Object[] nested) {
                arguments = nested;
                index[0] = 0;
            } else {
                index[0] = 1;
            }
        }
        this.rows = List.copyOf(ArcaneRecipes.rows(arguments, index));
        this.height = this.rows.size();
        this.width = this.rows.get(0).length();
        for (String row : this.rows) {
            if (row.length() != this.width) throw new IllegalArgumentException("Invalid shaped arcane recipe: unequal rows");
        }
        Map<String, Ingredient> ingredients = new LinkedHashMap<>();
        while (index[0] < arguments.length) {
            Object symbol = arguments[index[0]];
            Object value = arguments[index[0] + 1];
            if (!(symbol instanceof Character character)) throw new IllegalArgumentException("Invalid shaped arcane recipe: " + symbol);
            index[0] += 2;
            if (character == ' ') continue;
            ingredients.put(String.valueOf(character), ArcaneRecipes.ingredient(value));
        }
        this.key = Map.copyOf(ingredients);
        for (String row : this.rows) {
            for (int x = 0; x < row.length(); x++) {
                String symbol = row.substring(x, x + 1);
                if (!symbol.equals(" ") && !this.key.containsKey(symbol)) {
                    throw new IllegalArgumentException("Invalid shaped arcane recipe: no ingredient for " + symbol);
                }
            }
        }
        this.input = new Object[this.width * this.height];
        int cell = 0;
        for (String row : this.rows) {
            for (int x = 0; x < row.length(); x++) {
                String symbol = row.substring(x, x + 1);
                this.input[cell++] = symbol.equals(" ") ? null : this.key.get(symbol);
            }
        }
    }

    /** The native recipe behind this view; it is rebuilt when the mirroring changes. */
    public ArcaneShapedRecipe nativeRecipe() {
        if (nativeRecipe == null || nativeMirrored != this.mirrored) {
            nativeRecipe = new ArcaneShapedRecipe(research, rows, key, result, ArcaneRecipes.vis(aspects), this.mirrored);
            nativeMirrored = this.mirrored;
        }
        return nativeRecipe;
    }

    /**
     * The public view of a native shaped arcane recipe.
     *
     * <p>Forced 1.21.1 change: the native recipe of the recipe manager is the real one; the view keeps
     * the original argument shape and answers through it.</p>
     */
    public static ShapedArcaneRecipe of(ArcaneShapedRecipe recipe) {
        List<Object> arguments = new ArrayList<>(recipe.pattern());
        for (var entry : recipe.key().entrySet()) {
            arguments.add(entry.getKey().charAt(0));
            arguments.add(entry.getValue());
        }
        ShapedArcaneRecipe view = new ShapedArcaneRecipe(recipe.research(), recipe.result(),
                NativeRecipes.aspects(recipe.vis()), arguments.toArray());
        view.setMirrored(recipe.mirrored());
        return view;
    }

    @Override public ItemStack getCraftingResult(ArcaneRecipeInput inventory) {
        return nativeRecipe().assemble(inventory, ArcaneRecipes.registries());
    }

    @Override public int getRecipeSize() { return this.input.length; }

    @Override public ItemStack getRecipeOutput() { return this.output; }

    @Override public boolean matches(ArcaneRecipeInput inventory, Level level, Player player) {
        if (this.research != null && !this.research.isEmpty() && !ThaumcraftApiHelper.isResearchComplete(player, this.research)) return false;
        return nativeRecipe().matches(inventory, level);
    }

    public ShapedArcaneRecipe setMirrored(boolean mirror) {
        this.mirrored = mirror;
        return this;
    }

    /** The original ingredient cells of the pattern; a blank cell is null. */
    public Object[] getInput() { return this.input; }

    @Override public AspectList getAspects() { return this.aspects; }

    @Override public AspectList getAspects(ArcaneRecipeInput inventory) { return this.aspects; }

    @Override public String getResearch() { return this.research; }
}
