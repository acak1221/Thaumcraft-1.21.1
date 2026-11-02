package thaumcraft.common.crafting.transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.crafting.transfer.RecipeTransferPlan;
import static org.junit.jupiter.api.Assertions.*;

class RecipeTransferPlanTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void preparedPlanCannotBeRewrittenThroughCallerOrAccessorStacks() {
        var grid = empty(9); var sources = empty(36);
        ItemStack named = new ItemStack(Items.OAK_LOG, 8);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("exact-native-component")); sources.set(3, named);
        var nextGrid = empty(9); nextGrid.set(0, named.copyWithCount(1)); var nextSources = empty(36); nextSources.set(3, named.copyWithCount(7));
        var plan = new RecipeTransferPlan(ResourceLocation.parse("minecraft:oak_planks"), grid, sources, nextGrid, nextSources, 1);
        named.shrink(7); nextGrid.getFirst().set(DataComponents.CUSTOM_NAME, Component.literal("caller-change")); sources.clear();
        assertEquals(8, plan.expectedSources().get(3).getCount());
        assertEquals("exact-native-component", plan.nextMatrix().getFirst().getHoverName().getString());
        plan.nextSources().get(3).shrink(7); plan.expectedSources().get(3).remove(DataComponents.CUSTOM_NAME);
        assertEquals(7, plan.nextSources().get(3).getCount());
        assertEquals("exact-native-component", plan.expectedSources().get(3).getHoverName().getString());
    }

    @Test void malformedShapesAndUnboundedSetCountsCannotBecomeACommitPlan() {
        var id = ResourceLocation.parse("minecraft:oak_planks");
        assertThrows(IllegalArgumentException.class, () -> new RecipeTransferPlan(id, empty(8), empty(36), empty(9), empty(36), 1));
        assertThrows(IllegalArgumentException.class, () -> new RecipeTransferPlan(id, empty(9), empty(36), empty(10), empty(36), 1));
        assertThrows(IllegalArgumentException.class, () -> new RecipeTransferPlan(id, empty(9), empty(35), empty(9), empty(36), 1));
        assertThrows(IllegalArgumentException.class, () -> new RecipeTransferPlan(id, empty(9), empty(36), empty(9), empty(36), 0));
        assertThrows(IllegalArgumentException.class, () -> new RecipeTransferPlan(id, empty(9), empty(36), empty(9), empty(36), 65));
    }

    private static List<ItemStack> empty(int count) { return new ArrayList<>(Collections.nCopies(count, ItemStack.EMPTY)); }
}
