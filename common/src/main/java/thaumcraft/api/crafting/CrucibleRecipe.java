package thaumcraft.api.crafting;

import java.util.ArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The original TC4 crucible recipe.
 *
 * <p>The native port owns the real recipe in {@code thaumcraft.common.crafting.CrucibleRecipe}; this class
 * keeps the original argument shape and delegates every answer to the native recipe.</p>
 *
 * <p>Forced 1.21.1 change: an ore-name catalyst became a namespaced item tag, because 1.21.1 has no ore
 * dictionary.</p>
 */
public class CrucibleRecipe {
    private ItemStack recipeOutput;
    public Object catalyst;
    public AspectList aspects;
    public String key;
    public int hash;
    private final Ingredient catalystIngredient;

    /** Creates an unregistered recipe, retaining result/tags/catalyst references and hashing essentia-unit costs.
     * Null result/tags throw NPE; catalyst must be a nonempty ItemStack, Item, Block, Ingredient or resolvable item-tag string.
     * Unlike the 1.7.10 Object catalyst shape, ArrayList alternatives are rejected with IAE even though hashing retains that branch.
     * Use Ingredient or a namespaced native item tag for 1.21.1 alternatives; null/unsupported catalysts throw IAE. */
    public CrucibleRecipe(String researchKey, ItemStack result, Object cat, AspectList tags) {
        this.recipeOutput = result;
        this.aspects = tags;
        this.key = researchKey;
        this.catalyst = cat;
        this.catalystIngredient = ArcaneRecipes.ingredient(cat);
        String hc = researchKey + result.toString();
        for (Aspect tag : tags.getAspects()) {
            hc = hc + tag.getTag() + tags.getAmount(tag);
        }
        if (cat instanceof ItemStack stack) {
            hc = hc + stack.toString();
        } else if (cat instanceof ArrayList<?> list && !list.isEmpty()) {
            for (Object entry : list) hc = hc + entry.toString();
        }
        this.hash = hc.hashCode();
    }

    /** The native recipe behind this view. */
    public thaumcraft.common.crafting.CrucibleRecipe nativeRecipe() {
        return new thaumcraft.common.crafting.CrucibleRecipe(key, catalystIngredient, recipeOutput, ArcaneRecipes.aspectCosts(aspects), hash & 0x7FFFFFFF);
    }

    /** The public view of a native crucible recipe. */
    public static CrucibleRecipe of(thaumcraft.common.crafting.CrucibleRecipe recipe) {
        return new CrucibleRecipe(recipe.research(), recipe.result(), recipe.catalyst(),
                NativeRecipes.aspects(recipe.aspects()));
    }

    /** True when the catalyst and the essence both fit this recipe. */
    public boolean matches(AspectList itags, ItemStack cat) {
        if (!catalystMatches(cat)) return false;
        if (itags == null) return false;
        for (Aspect tag : this.aspects.getAspects()) {
            if (itags.getAmount(tag) >= this.aspects.getAmount(tag)) continue;
            return false;
        }
        return true;
    }

    /** Tests the captured native catalyst predicate without consuming anything; null/empty stacks return false. */
    public boolean catalystMatches(ItemStack cat) {
        if (cat == null || cat.isEmpty()) return false;
        return catalystIngredient.test(cat);
    }

    /** The original removal of the recipe's essence from the crucible's contents. */
    public AspectList removeMatching(AspectList itags) {
        AspectList temptags = new AspectList();
        temptags.aspects.putAll(itags.aspects);
        for (Aspect tag : this.aspects.getAspects()) {
            temptags.remove(tag, this.aspects.getAmount(tag));
        }
        return temptags;
    }

    /** Returns the live output stack reference retained at construction; no copy is made. */
    public ItemStack getRecipeOutput() { return this.recipeOutput; }

    /** The original item comparison, without the ore-dictionary shortcut. */
    public static boolean itemMatches(ItemStack target, ItemStack input, boolean strict) {
        return ThaumcraftApiHelper.itemMatches(target, input, strict);
    }
}
