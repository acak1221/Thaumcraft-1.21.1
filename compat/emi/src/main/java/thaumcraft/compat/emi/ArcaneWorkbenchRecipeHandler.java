package thaumcraft.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.RecipeTransferNetworking;

/**
 * Lets EMI move the items of an arcane recipe into the workbench grid.
 *
 * <p>Data/API recipes and dynamic wand/sceptre variants use the same authoritative server
 * transfer as the Thaumonomicon. Filling takes no output or vis; the native workbench pays
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
        if (recipe instanceof EmiArcaneRecipe) return recipe.getCategory() == ThaumcraftEmiPlugin.ARCANE;
        var backing = recipe.getBackingRecipe();
        return backing != null && (backing.value() instanceof ShapedRecipe shaped && shaped.getWidth() <= 3 && shaped.getHeight() <= 3
                || backing.value() instanceof ShapelessRecipe shapeless && shapeless.getIngredients().size() <= 9);
    }

    @Override public boolean canCraft(EmiRecipe recipe, EmiCraftContext<ContainerArcaneWorkbench> context) {
        if (!supportsRecipe(recipe) || !knownResearch(recipe)) return false;
        // EMI's inventory matcher is greedy. It can reject overlapping ingredient tags even
        // when the server can arrange them, so a fill-button request must reach the core planner.
        // Keep the ordinary conservative predicate for EMI's "craftable" sidebar.
        return context.getType() == EmiCraftContext.Type.FILL_BUTTON
                || StandardRecipeHandler.super.canCraft(recipe, context);
    }

    @Override public boolean craft(EmiRecipe recipe, EmiCraftContext<ContainerArcaneWorkbench> context) {
        if (!canCraft(recipe, context)) return false;
        ResourceLocation id = recipe instanceof EmiArcaneRecipe ? recipe.getId()
                : recipe.getBackingRecipe() == null ? null : recipe.getBackingRecipe().id();
        if (id == null) return false;
        Minecraft.getInstance().setScreen(context.getScreen());
        RecipeTransferNetworking.request(id, context.getAmount() > 1);
        return true;
    }

    @Override public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe, EmiCraftContext<ContainerArcaneWorkbench> context) {
        if (!knownResearch(recipe)) return List.of(ClientTooltipComponent.create(
                Component.translatable("tc.port.emi.research_unknown").getVisualOrderText()));
        return StandardRecipeHandler.super.getTooltip(recipe, context);
    }

    private static boolean knownResearch(EmiRecipe recipe) {
        if (!(recipe instanceof EmiArcaneRecipe arcane) || arcane.research().isEmpty()) return true;
        var player = Minecraft.getInstance().player;
        return player != null && KnowledgeAccess.get(player).isResearchComplete(arcane.research());
    }
}
