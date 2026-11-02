package thaumcraft.client.gui;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.crafting.transfer.RecipeTransferRecipes;
import thaumcraft.common.lib.network.RecipeTransferNetworking;

/** Client convenience only: the server resolves every requested ID and pays from its own inventory state. */
public final class RecipeTransferClient {
    private RecipeTransferClient() {}

    public static Optional<ResourceLocation> recipeId(ThaumcraftRecipeDisplay display, long cycle) {
        var client = Minecraft.getInstance();
        if (client.level == null || display == null) return Optional.empty();
        try {
            return display.selectedTransferRecipeId(client.level, cycle)
                    .filter(id -> RecipeTransferRecipes.supported(client.level, id));
        } catch (RuntimeException unsupportedAddonRecipe) {
            // A display-only or malformed add-on recipe must not break the rest of the research book.
            return Optional.empty();
        }
    }

    public static boolean request(ResourceLocation recipeId, boolean maximum) {
        var client = Minecraft.getInstance();
        if (recipeId == null || client.player == null || client.level == null) return false;
        RecipeTransferNetworking.request(recipeId, maximum, true);
        return true;
    }
}
