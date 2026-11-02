package thaumcraft.api.crafting.transfer;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Implement on the actual open menu. This API has no dependency on any storage addon.
 * Matrix slots are 0..8; other slots (including the wand) are never transfer ingredients.
 */
public interface RecipeTransferTarget {
    Container recipeTransferContents();

    /** Rechecks actor, open session, block/reach, and any external inventory permissions. */
    boolean canTransferRecipe(ServerPlayer actor);

    /** Stable ordered source cells. The default uses only the player's 36 main/hotbar cells. */
    default List<ItemStack> recipeTransferSourceSnapshot(ServerPlayer actor) {
        return actor.getInventory().items.stream().map(ItemStack::copy).toList();
    }

    /**
     * Atomically validate expected cells and replace BOTH matrix and source cells before
     * notifications. Return true once committed, even if a later callback retires the menu.
     * An addon providing another source must implement equivalent conservation/rollback;
     * a simulated network extraction is not a paid ingredient.
     */
    boolean applyRecipeTransfer(ServerPlayer actor, RecipeTransferPlan plan);
}
