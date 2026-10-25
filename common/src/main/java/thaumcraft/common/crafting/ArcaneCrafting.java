package thaumcraft.common.crafting;

import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Native recipe-manager lookup; no client-provided recipe, output, research or price is accepted. */
public final class ArcaneCrafting {
    private ArcaneCrafting() {}

    public record Match(RecipeHolder<?> recipe, ArcaneRecipeInput input, ItemStack output,
                        NonNullList<ItemStack> remainders, Map<String, Integer> prices, boolean arcane) {}

    public static Match find(Player player, Container contents) {
        var level = player.level();
        ArcaneRecipeInput grid = ArcaneRecipeInput.snapshot(contents);
        var positioned = CraftingInput.ofPositioned(3, 3, grid.items());
        var vanilla = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, positioned.input(), level);
        // ContainerArcaneWorkbench:60-69 gives ordinary crafting priority, even
        // when a matching arcane recipe also exists. No wand needed for vanilla.
        if (vanilla.isPresent()) {
            var holder = vanilla.get();
            ItemStack output = holder.value().assemble(positioned.input(), level.registryAccess());
            if (output.isEmpty() || !output.isItemEnabled(level.enabledFeatures())) return null;
            var remaining = holder.value().getRemainingItems(positioned.input());
            NonNullList<ItemStack> mapped = NonNullList.withSize(9, ItemStack.EMPTY);
            for (int y = 0; y < positioned.input().height(); y++) {
                for (int x = 0; x < positioned.input().width(); x++) {
                    mapped.set(x + positioned.left() + (y + positioned.top()) * 3,
                            remaining.get(x + y * positioned.input().width()).copy());
                }
            }
            return new Match(holder, grid, output, mapped, Map.of(), false);
        }
        for (var holder : level.getRecipeManager().getAllRecipesFor(ThaumcraftArcane.ARCANE_RECIPE_TYPE.get())) {
            var recipe = holder.value();
            if (!recipe.matches(grid, level) || !recipe.hasResearch(grid, player)) continue;
            ItemStack output = recipe.assemble(grid, level.registryAccess());
            if (output.isEmpty() || !output.isItemEnabled(level.enabledFeatures())) continue;
            return new Match(holder, grid, output, recipe.getRemainingItems(grid),
                    ArcaneVis.price(recipe.vis(grid), contents.getItem(TileArcaneWorkbench.WAND_SLOT), player), true);
        }
        int registration = 0;
        for (Object candidate : ProgrammaticRecipes.registrations()) {
            int index = registration++;
            if (!(candidate instanceof thaumcraft.api.crafting.IArcaneRecipe original)
                    || !original.matches(grid, level, player)) continue;
            var recipe = new ProgrammaticArcaneRecipe(original);
            ItemStack output = recipe.assemble(grid, level.registryAccess());
            if (output.isEmpty() || !output.isItemEnabled(level.enabledFeatures())) continue;
            var holder = new RecipeHolder<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thaumcraft", "api/arcane/" + index), recipe);
            return new Match(holder, grid, output, recipe.getRemainingItems(grid),
                    ArcaneVis.price(recipe.vis(grid), contents.getItem(TileArcaneWorkbench.WAND_SLOT), player), true);
        }
        return null;
    }
}
