package thaumcraft.common.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** TC4 arcane checkItemEquals damage rule, which vanilla Ingredient.test omits. */
public final class ArcaneIngredientMatching {
    private ArcaneIngredientMatching() {}

    static boolean matches(Ingredient ingredient, ItemStack offered) {
        return matches(ingredient, offered, ArcaneIngredientMatching::matchesStack);
    }

    static boolean matches(Ingredient ingredient, ItemStack offered, java.util.function.BiPredicate<ItemStack, ItemStack> comparison) {
        if (!ingredient.test(offered)) return false;
        if (offered.isEmpty()) return true;
        // Original Shaped/ShapelessArcaneRecipe.checkItemEquals: the required
        // stack's damage must match, unless it is the legacy wildcard32767.
        // Plain modern item ingredients represent original damage-zero stacks.
        // Do not compare ALL components: untagged original ingredients accept
        // custom names/enchantments, and counts describe occupied cells, not cost.
        for (ItemStack required : ingredient.getItems()) {
            if (comparison.test(required.copy(), offered.copy())) return true;
        }
        return false;
    }

    /** Only explicit recipe data is required: ordinary item defaults are not legacy NBT. */
    public static boolean matchesStack(ItemStack required, ItemStack offered) {
        if (required == null || offered == null) return required == offered;
        if (required.isEmpty() || offered.isEmpty()) return required.isEmpty() && offered.isEmpty();
        if (!required.is(offered.getItem()) || required.getDamageValue() != Short.MAX_VALUE
                && required.getDamageValue() != offered.getDamageValue()) return false;
        for (var entry : required.getComponentsPatch().entrySet()) {
            if (entry.getKey() == net.minecraft.core.component.DataComponents.DAMAGE) continue;
            Object expected = entry.getValue().orElse(null), actual = offered.get(entry.getKey());
            if (entry.getKey() == net.minecraft.core.component.DataComponents.CUSTOM_DATA
                    && expected instanceof net.minecraft.world.item.component.CustomData requiredData) {
                if (!(actual instanceof net.minecraft.world.item.component.CustomData actualData)) return false;
                var expectedTag = requiredData.copyTag(); var actualTag = actualData.copyTag();
                // Original areItemStackTagsEqualForCrafting:138-145 compares whole top-level values.
                for (String key : expectedTag.getAllKeys()) if (!java.util.Objects.equals(expectedTag.get(key), actualTag.get(key))) return false;
            } else if (!java.util.Objects.equals(expected, actual)) return false;
        }
        return true;
    }
}
