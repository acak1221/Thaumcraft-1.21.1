package thaumcraft.common.crafting;

import java.util.Map;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Native recipe-manager base for TC4 fixed and input-dependent arcane recipes. */
public interface NativeArcaneRecipe extends Recipe<ArcaneRecipeInput> {
    String research();

    /** Static base cost; dynamic recipes have no global cost and return an empty map. */
    Map<String, Integer> vis();

    default Map<String, Integer> vis(ArcaneRecipeInput input) { return vis(); }

    /** Invoked with the actual server actor again for every take, never a client unlock flag. */
    default boolean hasResearch(ArcaneRecipeInput input, Player actor) {
        return actor != null && (research().isEmpty() || KnowledgeAccess.get(actor).isResearchComplete(research()));
    }
}
