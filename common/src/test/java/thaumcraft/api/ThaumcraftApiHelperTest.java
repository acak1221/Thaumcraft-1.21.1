package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** The original helper rules of the TC4 API, on the native 1.21.1 types. */
class ThaumcraftApiHelperTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void cullingDropsTheLeastValuableAspectUntilSixRemain() {
        AspectList tags = new AspectList();
        tags.add(Aspect.AIR, 1).add(Aspect.FIRE, 1).add(Aspect.WATER, 1).add(Aspect.EARTH, 1)
                .add(Aspect.ORDER, 1).add(Aspect.ENTROPY, 1).add(Aspect.VOID, 1);
        AspectList culled = ThaumcraftApiHelper.cullTags(tags);
        assertEquals(6, culled.size());
        // A primal aspect is worth 0.9, a compound one more; the first primal is the lowest.
        assertEquals(0, culled.getAmount(Aspect.AIR));
        assertEquals(1, culled.getAmount(Aspect.VOID));
        assertEquals(7, tags.size(), "The original never changes the list it culls");
    }

    @Test void thePrimalAspectsAreWorthLessThanTheCompoundOnes() {
        AspectList tags = new AspectList().add(Aspect.AIR, 10).add(Aspect.VOID, 1).add(Aspect.FIRE, 10)
                .add(Aspect.WATER, 10).add(Aspect.EARTH, 10).add(Aspect.ORDER, 10).add(Aspect.ENTROPY, 10);
        AspectList culled = ThaumcraftApiHelper.cullTags(tags);
        assertEquals(6, culled.size());
        // The compound aspect is worth more per unit than a primal one, so the cheap compound is dropped.
        assertEquals(0, culled.getAmount(Aspect.VOID));
        assertEquals(10, culled.getAmount(Aspect.AIR));
    }

    @Test void cullingOnlyTouchesMoreThanSixAspects() {
        AspectList tags = new AspectList().add(Aspect.AIR, 10).add(Aspect.VOID, 1);
        assertEquals(2, ThaumcraftApiHelper.cullTags(tags).size());
    }

    @Test void itemEqualityIgnoresTheStackCount() {
        assertTrue(ThaumcraftApiHelper.areItemsEqual(new ItemStack(Items.STONE), new ItemStack(Items.STONE, 3)));
        assertFalse(ThaumcraftApiHelper.areItemsEqual(new ItemStack(Items.STONE), new ItemStack(Items.DIRT)));
        assertTrue(ThaumcraftApiHelper.areItemsEqual(ItemStack.EMPTY, ItemStack.EMPTY));
    }

    @Test void itemMatchingHonoursTheWildcardMetadata() {
        ItemStack stone = new ItemStack(Items.STONE);
        assertTrue(ThaumcraftApiHelper.itemMatches(stone, new ItemStack(Items.STONE), false));
        assertFalse(ThaumcraftApiHelper.itemMatches(stone, new ItemStack(Items.DIRT), false));
    }

    @Test void theAllAspectListsHoldEveryAspectOfThePort() {
        assertEquals(48, ThaumcraftApiHelper.getAllAspects(2).size());
        assertEquals(42, ThaumcraftApiHelper.getAllCompoundAspects(2).size());
        assertEquals(2, ThaumcraftApiHelper.getAllAspects(2).getAmount(Aspect.AIR));
        assertTrue(ThaumcraftApiHelper.getAllCompoundAspects(2).getAspects().length > 0);
    }

    @Test void theDataComponentComparisonIsTheNativeTagComparison() {
        assertTrue(ThaumcraftApiHelper.areItemStackTagsEqualForCrafting(new ItemStack(Items.STONE), new ItemStack(Items.STONE)));
        assertFalse(ThaumcraftApiHelper.areItemStackTagsEqualForCrafting(null, new ItemStack(Items.STONE)));
        assertFalse(ThaumcraftApiHelper.areItemStackTagsEqualForCrafting(new ItemStack(Items.STONE), null));
    }

    @Test void theStackInRowAndColumnReadsTheNativeRecipeInput() {
        ItemStack stone = new ItemStack(Items.STONE);
        var input = new thaumcraft.common.crafting.ArcaneRecipeInput(java.util.List.of(
                stone, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        assertEquals(Items.STONE, ThaumcraftApiHelper.getStackInRowAndColumn(input, 0, 0).getItem());
        assertNotNull(ThaumcraftApiHelper.getStackInRowAndColumn(input, 2, 2));
    }
}
