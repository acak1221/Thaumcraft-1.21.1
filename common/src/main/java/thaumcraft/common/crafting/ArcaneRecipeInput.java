package thaumcraft.common.crafting;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Untrimmed original 3x3 matrix: explicit blank borders in TC4 patterns are significant. */
public record ArcaneRecipeInput(List<ItemStack> items) implements RecipeInput {
    public ArcaneRecipeInput {
        if (items.size() != 9) throw new IllegalArgumentException("Arcane matrix must be 3x3");
        items = items.stream().map(ItemStack::copy).toList();
    }

    public static ArcaneRecipeInput snapshot(Container table) {
        return new ArcaneRecipeInput(java.util.stream.IntStream.range(0, 9).mapToObj(table::getItem).toList());
    }

    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public int size() { return 9; }
}
