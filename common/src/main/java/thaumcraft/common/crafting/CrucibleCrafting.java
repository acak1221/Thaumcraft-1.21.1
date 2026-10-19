package thaumcraft.common.crafting;

import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftAlchemy;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

public final class CrucibleCrafting {
    private CrucibleCrafting() {}

    /** Native getOwner resolves ItemEntity's thrower UUID, NOT a stack/custom-NBT username. */
    public static ServerPlayer thrower(ServerLevel level, ItemEntity entity) {
        if (entity.level() != level || !(entity.getOwner() instanceof ServerPlayer player) || player.isRemoved()) return null;
        return level.getServer().getPlayerList().getPlayer(player.getUUID()) == player ? player : null;
    }

    public static RecipeHolder<CrucibleRecipe> find(ServerLevel level, ItemEntity entity, AspectList aspects) {
        ServerPlayer player = thrower(level, entity);
        // KnowledgeAccess is attached to live players, not an offline-name cache.
        // Offline/unresolvable/no thrower: no researched craft, but ordinary
        // dissolving remains available in TileCrucible without any knowledge gate.
        if (player == null) return null;
        var input = new CrucibleRecipeInput(entity.getItem(), aspects);
        var ordered = level.getRecipeManager().getAllRecipesFor(ThaumcraftAlchemy.CRUCIBLE_RECIPE_TYPE.get()).stream()
                .sorted(Comparator.<RecipeHolder<CrucibleRecipe>>comparingInt(holder -> holder.value().sourceOrder())
                        .thenComparing(holder -> holder.id().toString())).toList();
        RecipeHolder<CrucibleRecipe> best = null;
        int highest = 0;
        for (var holder : ordered) {
            CrucibleRecipe recipe = holder.value();
            // CraftingManager:127-144: count DISTINCT aspects; equal counts do
            // not replace the first registered match. source_order records the
            // ConfigRecipes order explicitly; it is not a weighted priority.
            if (recipe.aspects().size() <= highest || !KnowledgeAccess.get(player).isResearchComplete(recipe.research())
                    || !recipe.matches(input, level)) continue;
            if (recipe.getResultItem(level.registryAccess()).isEmpty()
                    || !recipe.getResultItem(level.registryAccess()).isItemEnabled(level.enabledFeatures())) continue;
            highest = recipe.aspects().size();
            best = holder;
        }
        return best;
    }
}
