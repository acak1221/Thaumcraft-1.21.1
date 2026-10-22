package thaumcraft.common.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** TC4 arcane checkItemEquals damage rule, which vanilla Ingredient.test omits. */
final class ArcaneIngredientMatching {
    private ArcaneIngredientMatching() {}

    static boolean matches(Ingredient ingredient, ItemStack offered) {
        if (!ingredient.test(offered)) return false;
        if (offered.isEmpty()) return true;
        // Original Shaped/ShapelessArcaneRecipe.checkItemEquals: the required
        // stack's damage must match, unless it is the legacy wildcard32767.
        // Plain modern item ingredients represent original damage-zero stacks.
        // Do not compare ALL components: untagged original ingredients accept
        // custom names/enchantments, and counts describe occupied cells, not cost.
        for (ItemStack required : ingredient.getItems()) {
            if (required.is(offered.getItem()) && (required.getDamageValue() == Short.MAX_VALUE
                    || required.getDamageValue() == offered.getDamageValue())) return true;
        }
        return false;
    }
}
