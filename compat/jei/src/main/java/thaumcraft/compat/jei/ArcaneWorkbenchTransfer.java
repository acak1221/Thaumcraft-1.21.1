package thaumcraft.compat.jei;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.RecipeTransferNetworking;

/**
 * Lets JEI's "+" button move the items of a recipe into the arcane workbench grid.
 *
 * <p>The request is the one the Thaumonomicon and the EMI plugin send: the server finds the items and
 * arranges them, and answers in the action bar when it cannot. Filling takes no vis, so a wand that
 * cannot pay is a warning on the button, not a refusal; research the player has not completed is a
 * refusal, as the server would refuse it.</p>
 *
 * @param <R> the JEI recipe: an arcane page, or a vanilla crafting recipe the workbench also makes
 */
final class ArcaneWorkbenchTransfer<R> implements IRecipeTransferHandler<ContainerArcaneWorkbench, R> {
    private final IRecipeTransferHandlerHelper helper;
    private final RecipeType<R> type;
    private final Function<R, ResourceLocation> id;
    private final Function<R, String> research;
    private final Function<R, Map<String, Integer>> vis;

    /**
     * @param id       the recipe id the server knows, or null for a recipe the workbench cannot take
     * @param research the research key the recipe needs, empty for none
     * @param vis      whole vis per primal tag
     */
    ArcaneWorkbenchTransfer(IRecipeTransferHandlerHelper helper, RecipeType<R> type, Function<R, ResourceLocation> id,
                            Function<R, String> research, Function<R, Map<String, Integer>> vis) {
        this.helper = helper; this.type = type; this.id = id; this.research = research; this.vis = vis;
    }

    @Override public Class<? extends ContainerArcaneWorkbench> getContainerClass() { return ContainerArcaneWorkbench.class; }
    @Override public Optional<MenuType<ContainerArcaneWorkbench>> getMenuType() { return Optional.of(ThaumcraftArcane.ARCANE_WORKBENCH_MENU.get()); }
    @Override public RecipeType<R> getRecipeType() { return type; }

    @Override public IRecipeTransferError transferRecipe(ContainerArcaneWorkbench menu, R recipe, IRecipeSlotsView slots, Player player,
                                                         boolean maxTransfer, boolean doTransfer) {
        ResourceLocation recipeId = id.apply(recipe);
        if (recipeId == null) return helper.createInternalError();
        String key = research.apply(recipe);
        if (key != null && !key.isEmpty() && !KnowledgeAccess.get(player).isResearchComplete(key)) {
            return helper.createUserErrorWithTooltip(Component.translatable("tc.port.emi.research_unknown"));
        }
        if (doTransfer) {
            RecipeTransferNetworking.request(recipeId, maxTransfer);
            return null;
        }
        return menu.transferWarning(vis.apply(recipe), player).<IRecipeTransferError>map(Warning::new).orElse(null);
    }

    /** JEI still transfers on a cosmetic error; it only tints the button and shows the line. */
    private record Warning(Component line) implements IRecipeTransferError {
        @Override public Type getType() { return Type.COSMETIC; }
        @Override public void getTooltip(ITooltipBuilder tooltip) { tooltip.add(line); }
    }
}
