package thaumcraft.common.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import thaumcraft.api.aspects.AspectList;

/** One dropped catalyst and a snapshot of the crucible's actual contents. */
public record CrucibleRecipeInput(ItemStack catalyst, AspectList aspects) implements RecipeInput {
    public CrucibleRecipeInput {
        catalyst = catalyst.copyWithCount(catalyst.isEmpty() ? 0 : 1);
        aspects = aspects.copy();
    }

    @Override public ItemStack getItem(int slot) {
        if (slot != 0) throw new IndexOutOfBoundsException(slot);
        return catalyst;
    }
    @Override public int size() { return 1; }
}
