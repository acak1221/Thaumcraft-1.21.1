package thaumcraft.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.crafting.ShapelessNBTOreRecipe;
import static org.junit.jupiter.api.Assertions.*;

class ShapelessNBTOreRecipeTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static ShapelessNBTOreRecipe.Requirement requirement(ItemStack stack, int count) {
        return new ShapelessNBTOreRecipe.Requirement(Ingredient.of(stack), count, DataComponentPatch.EMPTY);
    }

    private static CraftingInput grid(ItemStack... stacks) {
        List<ItemStack> items = new ArrayList<>(List.of(stacks));
        while (items.size() < 9) items.add(ItemStack.EMPTY);
        return CraftingInput.of(3, 3, items);
    }

    @Test void anIngredientNeedsItsOwnStackSizeAndNoExtraData() {
        var required = List.of(requirement(new ItemStack(Items.PAPER), 1));
        assertTrue(ShapelessNBTOreRecipe.matches(required, grid(new ItemStack(Items.PAPER))));
        // The original compares the stack size, not only the item.
        assertFalse(ShapelessNBTOreRecipe.matches(required, grid(new ItemStack(Items.PAPER, 2))));
        assertFalse(ShapelessNBTOreRecipe.matches(required, grid(new ItemStack(Items.DIAMOND))));
    }
    @Test void theOriginalDataEqualityRejectsAChangedStack() {
        var required = List.of(requirement(new ItemStack(Items.PAPER), 1));
        ItemStack named = new ItemStack(Items.PAPER);
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("special"));
        assertFalse(ShapelessNBTOreRecipe.matches(required, grid(named)));
    }
    @Test void theWildcardCountAcceptsAnyStackSize() {
        var required = List.of(requirement(new ItemStack(Items.PAPER), ShapelessNBTOreRecipe.WILDCARD_COUNT));
        assertTrue(ShapelessNBTOreRecipe.matches(required, grid(new ItemStack(Items.PAPER, 5))));
    }
    @Test void theGridIsConsumedGreedilyAndMayNotLeaveARequirementOver() {
        var required = List.of(requirement(new ItemStack(Items.PAPER), 1), requirement(new ItemStack(Items.PAPER), 1));
        assertFalse(ShapelessNBTOreRecipe.matches(required, grid(new ItemStack(Items.PAPER))));
        assertTrue(ShapelessNBTOreRecipe.matches(required, grid(new ItemStack(Items.PAPER), new ItemStack(Items.PAPER))));
        assertFalse(ShapelessNBTOreRecipe.matches(required,
                grid(new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER))));
    }
    @Test void emptySlotsAreIgnoredAndUnknownStacksFailTheRecipe() {
        var required = List.of(requirement(new ItemStack(Items.PAPER), 1));
        assertTrue(ShapelessNBTOreRecipe.matches(required, grid(ItemStack.EMPTY, new ItemStack(Items.PAPER))));
        assertFalse(ShapelessNBTOreRecipe.matches(required, grid(new ItemStack(Items.PAPER), new ItemStack(Items.DIAMOND))));
    }
}
