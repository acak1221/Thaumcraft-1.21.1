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

    /** Builds an unregistered one-block-result recipe using whole-vis costs; delegates parsing/ownership to the stack constructor, requiring non-null result/recipe. */
    public ShapedArcaneRecipe(String research, Block result, AspectList aspects, Object... recipe) {
        this(research, new ItemStack(result), aspects, recipe);
    }

    /** Builds an unregistered one-item-result recipe using whole-vis costs; delegates parsing/ownership to the stack constructor, requiring non-null result/recipe. */
    public ShapedArcaneRecipe(String research, Item result, AspectList aspects, Object... recipe) {
        this(research, new ItemStack(result), aspects, recipe);
    }

    /** Parses an unregistered shaped recipe, copying result and retaining nullable research/aspects (whole primal vis, not centivis).
     * Null result/recipe throws NPE; malformed rows/keys/ingredients throw parsing exceptions, including IAE for unsupported ingredients.
     * An optional leading Boolean controls mirroring; character mappings use native ingredients/item tags, not old ore names. */
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

    /** Returns a fresh assembled result without consuming inventory; inventory is not read by the native assembly path, including null. */
    @Override public ItemStack getCraftingResult(ArcaneRecipeInput inventory) {
        return getRecipeOutput().copy();
    }

    /** Returns the live input-array length in grid cells (including blanks); a public-field null assignment throws NPE. */
    @Override public int getRecipeSize() { return this.input.length; }

    /** Returns the live nullable public output reference, not the independent native assembly result. */
    @Override public ItemStack getRecipeOutput() { return this.output; }

    /** Tests research and translated/mirrored grid predicates without spending vis or items; may lazily cache the native recipe.
     * Inventory must be non-null (native matching may throw otherwise); level is unused by matching, and null player fails a nonempty research gate. */
    @Override public boolean matches(ArcaneRecipeInput inventory, Level level, Player player) {
        if (this.research != null && !this.research.isEmpty() && !ThaumcraftApiHelper.isResearchComplete(player, this.research)) return false;
        return nativeRecipe().matches(inventory, level, this::checkItemEquals);
    }

    /** Sets horizontal-mirroring permission and returns this; the native recipe is rebuilt lazily on next access. */
    public ShapedArcaneRecipe setMirrored(boolean mirror) {
        this.mirrored = mirror;
        return this;
    }

    /**
     * The original comparison of two ingredient stacks.
     *
     * <p>Original {@code thaumcraft/api/crafting/ShapedArcaneRecipe.java}: two stacks are the same when
     * they are the same item, when their tags are equal and when their damage values are equal; the
     * wildcard damage of the recipe accepts every damage of the item.</p>
     *
     * <p>Forced 1.21.1 change: the Forge ore dictionary of the original has no 1.21.1 counterpart, so the
     * shortcut of the original is gone; the tags are compared with the native data components.</p>
     */
    protected boolean checkItemEquals(ItemStack stack0, ItemStack stack1) {
        return thaumcraft.common.crafting.ArcaneIngredientMatching.matchesStack(stack0, stack1);
    }

    /** The original ingredient cells of the pattern; a blank cell is null. */
    public Object[] getInput() { return this.input; }

    /** Returns the live nullable primal vis-cost list in whole vis, not centivis; no copy is made. */
    @Override public AspectList getAspects() { return this.aspects; }

    /** Returns the live nullable whole-vis cost list, ignoring inventory including null; no mutation occurs. */
    @Override public AspectList getAspects(ArcaneRecipeInput inventory) { return this.aspects; }

    /** Returns the stored nullable research key without mutation. */
    @Override public String getResearch() { return this.research; }
}
