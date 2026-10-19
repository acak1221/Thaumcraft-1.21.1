package thaumcraft.common.crafting;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The central stack and the occupied peripheral pedestals, in scan order. */
public record InfusionRecipeInput(ItemStack central, List<ItemStack> components) implements RecipeInput {
    public InfusionRecipeInput {
        central = central.copy();
        components = components.stream().map(ItemStack::copy).toList();
    }

    @Override public ItemStack getItem(int slot) { return slot == 0 ? central : components.get(slot - 1); }
    @Override public int size() { return components.size() + 1; }
}
