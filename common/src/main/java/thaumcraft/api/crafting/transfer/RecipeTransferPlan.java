package thaumcraft.api.crafting.transfer;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A server-prepared inventory rearrangement. Expected values are compare-and-set fences,
 * not permission to recreate missing items. No result stack or wand debit is included.
 */
public record RecipeTransferPlan(ResourceLocation recipeId, List<ItemStack> expectedMatrix,
        List<ItemStack> expectedSources, List<ItemStack> nextMatrix, List<ItemStack> nextSources, int sets) {
    public RecipeTransferPlan {
        java.util.Objects.requireNonNull(recipeId);
        if (expectedMatrix.size() != 9 || nextMatrix.size() != 9 || expectedSources.size() != nextSources.size()
                || expectedSources.size() > 4096 || sets < 1 || sets > 64) throw new IllegalArgumentException("Invalid recipe transfer plan");
        expectedMatrix = copies(expectedMatrix); expectedSources = copies(expectedSources);
        nextMatrix = copies(nextMatrix); nextSources = copies(nextSources);
    }

    private static List<ItemStack> copies(List<ItemStack> values) { return values.stream().map(ItemStack::copy).toList(); }
    @Override public List<ItemStack> expectedMatrix() { return copies(expectedMatrix); }
    @Override public List<ItemStack> expectedSources() { return copies(expectedSources); }
    @Override public List<ItemStack> nextMatrix() { return copies(nextMatrix); }
    @Override public List<ItemStack> nextSources() { return copies(nextSources); }
}
