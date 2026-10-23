package thaumcraft.api.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;

/**
 * The original TC4 shapeless arcane recipe.
 *
 * <p>The native port owns the real recipe in {@link ArcaneShapelessRecipe}; this class keeps the original
 * argument shape and delegates every answer to the native recipe.</p>
 *
 * <p>Forced 1.21.1 changes: the inventory became the native {@link ArcaneRecipeInput} and a recipe
 * argument that names an ore group became a namespaced item tag.</p>
 */
public class ShapelessArcaneRecipe implements IArcaneRecipe {
    private ItemStack output = null;
    private final ArrayList<Object> input = new ArrayList<>();
    public AspectList aspects = null;
    public String research;
    private final List<Ingredient> ingredients;
    private final ItemStack result;

    /** Builds an unregistered one-block-result recipe using whole-vis costs; delegates parsing/ownership to the stack constructor, requiring non-null result/recipe. */
    public ShapelessArcaneRecipe(String research, Block result, AspectList aspects, Object... recipe) {
        this(research, new ItemStack(result), aspects, recipe);
    }

    /** Builds an unregistered one-item-result recipe using whole-vis costs; delegates parsing/ownership to the stack constructor, requiring non-null result/recipe. */
    public ShapelessArcaneRecipe(String research, Item result, AspectList aspects, Object... recipe) {
        this(research, new ItemStack(result), aspects, recipe);
    }

    /** Parses an unregistered shapeless recipe, copying result and retaining nullable research/aspects (whole primal vis, not centivis).
     * Null result/recipe throws NPE; empty ingredient arrays and unsupported/null ingredients throw IAE.
     * Captures native ingredients/item tags separately from the mutable compatibility input list. */
    public ShapelessArcaneRecipe(String research, ItemStack result, AspectList aspects, Object... recipe) {
        this.research = research;
        this.aspects = aspects;
        this.result = result.copy();
        this.output = result.copy();
        List<Ingredient> ingredients = new ArrayList<>();
        for (Object in : recipe) {
            Ingredient ingredient = ArcaneRecipes.ingredient(in);
            ingredients.add(ingredient);
            this.input.add(ingredient);
        }
        if (ingredients.isEmpty()) throw new IllegalArgumentException("Invalid shapeless arcane recipe: no ingredient");
        this.ingredients = List.copyOf(ingredients);
    }

    /** The native recipe behind this view. */
    public ArcaneShapelessRecipe nativeRecipe() {
        return new ArcaneShapelessRecipe(research, ingredients, result, ArcaneRecipes.vis(aspects));
    }

    /** The public view of a native shapeless arcane recipe. */
    public static ShapelessArcaneRecipe of(ArcaneShapelessRecipe recipe) {
        Object[] arguments = new Object[recipe.ingredients().size()];
        for (int index = 0; index < arguments.length; index++) arguments[index] = recipe.ingredients().get(index);
        return new ShapelessArcaneRecipe(recipe.research(), recipe.result(), NativeRecipes.aspects(recipe.vis()), arguments);
    }

    /** Returns the current compatibility input-list size in ingredient entries, without mutation. */
    @Override public int getRecipeSize() { return this.input.size(); }

    /** Returns the live output stack reference, not the independent native assembly result. */
    @Override public ItemStack getRecipeOutput() { return this.output; }

    /** Returns a fresh assembled result without consuming inventory; inventory is not read by the native assembly path, including null. */
    @Override public ItemStack getCraftingResult(ArcaneRecipeInput inventory) {
        return nativeRecipe().assemble(inventory, ArcaneRecipes.registries());
    }

    /** Tests research and captured unordered ingredients without spending vis/items.
     * Inventory must be non-null (native matching may throw otherwise); level is unused by matching, and null player fails a nonempty research gate. */
    @Override public boolean matches(ArcaneRecipeInput inventory, Level level, Player player) {
        if (this.research != null && !this.research.isEmpty() && !ThaumcraftApiHelper.isResearchComplete(player, this.research)) return false;
        return nativeRecipe().matches(inventory, level);
    }

    /** The original ingredient list of the recipe. */
    public ArrayList<Object> getInput() { return this.input; }

    /**
     * The original comparison of two ingredient stacks.
     *
     * <p>Original {@code thaumcraft/api/crafting/ShapelessArcaneRecipe.java}: two stacks are the same when
     * they are the same item, when their tags are equal and when their damage values are equal; the
     * wildcard damage of the recipe accepts every damage of the item.</p>
     *
     * <p>Forced 1.21.1 change: the Forge ore dictionary of the original has no 1.21.1 counterpart, so the
     * shortcut of the original is gone; the tags are compared with the native data components.</p>
     */
    protected boolean checkItemEquals(ItemStack stack0, ItemStack stack1) {
        if (stack0 == null || stack1 == null) return stack0 == null && stack1 == null;
        if (stack0.getItem() != stack1.getItem()) return false;
        if (!stack0.getComponents().isEmpty() && !ThaumcraftApiHelper.areItemStackTagsEqualForCrafting(stack0, stack1)) return false;
        if (stack0.getDamageValue() == Short.MAX_VALUE) return true;
        return stack0.getDamageValue() == stack1.getDamageValue();
    }

    /** Returns the live nullable primal vis-cost list in whole vis, not centivis; no copy is made. */
    @Override public AspectList getAspects() { return this.aspects; }

    /** Returns the live nullable whole-vis cost list, ignoring inventory including null; no mutation occurs. */
    @Override public AspectList getAspects(ArcaneRecipeInput inventory) { return this.aspects; }

    /** Returns the stored nullable research key without mutation. */
    @Override public String getResearch() { return this.research; }
}
