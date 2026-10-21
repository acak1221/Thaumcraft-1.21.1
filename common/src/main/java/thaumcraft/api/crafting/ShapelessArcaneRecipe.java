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

    public ShapelessArcaneRecipe(String research, Block result, AspectList aspects, Object... recipe) {
        this(research, new ItemStack(result), aspects, recipe);
    }

    public ShapelessArcaneRecipe(String research, Item result, AspectList aspects, Object... recipe) {
        this(research, new ItemStack(result), aspects, recipe);
    }

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

    @Override public int getRecipeSize() { return this.input.size(); }

    @Override public ItemStack getRecipeOutput() { return this.output; }

    @Override public ItemStack getCraftingResult(ArcaneRecipeInput inventory) {
        return nativeRecipe().assemble(inventory, ArcaneRecipes.registries());
    }

    @Override public boolean matches(ArcaneRecipeInput inventory, Level level, Player player) {
        if (this.research != null && !this.research.isEmpty() && !ThaumcraftApiHelper.isResearchComplete(player, this.research)) return false;
        return nativeRecipe().matches(inventory, level);
    }

    /** The original ingredient list of the recipe. */
    public ArrayList<Object> getInput() { return this.input; }

    @Override public AspectList getAspects() { return this.aspects; }

    @Override public AspectList getAspects(ArcaneRecipeInput inventory) { return this.aspects; }

    @Override public String getResearch() { return this.research; }
}
