package thaumcraft.compat.jei;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.aspects.Aspect;

/** The recipe pages the JEI categories draw; the infusion pages are {@code RecipeViews.Infusion}. */
final class Pages {
    private Pages() {}

    /** The nine workbench cells in slot order ({@code Ingredient.EMPTY} for an empty one), the result and the vis. */
    record Arcane(ResourceLocation id, String research, List<Ingredient> grid, ItemStack result, Map<String, Integer> vis, boolean shapeless) {}

    /** The catalyst thrown into the essentia the crucible holds. */
    record Crucible(ResourceLocation id, String research, Ingredient catalyst, ItemStack result, Map<String, Integer> aspects) {}

    /** A compound aspect and the two the research table combines into it. */
    record Combination(Aspect aspect) {}
}
