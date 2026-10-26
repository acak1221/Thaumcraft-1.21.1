package thaumcraft.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import java.util.List;
import net.minecraft.world.inventory.Slot;
import thaumcraft.common.container.ContainerArcaneWorkbench;

/**
 * Lets EMI move the items of an arcane recipe into the workbench grid.
 *
 * <p>Menu slots of {@link ContainerArcaneWorkbench}: 0 the result, 1 the wand, 2-10 the grid in
 * {@code x + y * 3} order, then the 27 inventory slots and the 9 hotbar slots. EMI only moves
 * items with ordinary slot clicks; the wand still pays and the server still checks the research
 * when the result is taken.</p>
 */
final class ArcaneWorkbenchRecipeHandler implements StandardRecipeHandler<ContainerArcaneWorkbench> {
    private static final int GRID = 2, INVENTORY = 11, END = 47;

    @Override public List<Slot> getInputSources(ContainerArcaneWorkbench menu) {
        return menu.slots.subList(GRID, Math.min(END, menu.slots.size()));
    }

    @Override public List<Slot> getCraftingSlots(ContainerArcaneWorkbench menu) {
        return menu.slots.subList(GRID, INVENTORY);
    }

    @Override public Slot getOutputSlot(ContainerArcaneWorkbench menu) { return menu.slots.getFirst(); }

    @Override public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe instanceof EmiArcaneRecipe && recipe.getCategory() == ThaumcraftEmiPlugin.ARCANE;
    }
}
