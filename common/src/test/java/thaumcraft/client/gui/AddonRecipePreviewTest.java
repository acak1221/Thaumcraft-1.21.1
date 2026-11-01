package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.common.crafting.InfusionStackRecipe;

class AddonRecipePreviewTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void freshNativeSnapshotsReuseThePageHolderInsteadOfAddingOnePerFrame() {
        var crucible = new CrucibleRecipe("TEST", new ItemStack(Items.IRON_INGOT), new ItemStack(Items.QUARTZ),
                new AspectList().add(Aspect.AIR, 2));
        var shapeless = new ShapelessArcaneRecipe("TEST", new ItemStack(Items.PAPER),
                new AspectList().add(Aspect.AIR, 2), Items.QUARTZ);
        var crucibleHolder = ThaumcraftRecipeDisplay.addonHolder(crucible, crucible.nativeRecipe());
        var shapelessHolder = ThaumcraftRecipeDisplay.addonHolder(shapeless, shapeless.nativeRecipe());
        for (int frame = 0; frame < 1000; frame++) {
            assertSame(crucibleHolder, ThaumcraftRecipeDisplay.addonHolder(crucible, crucible.nativeRecipe()));
            assertSame(shapelessHolder, ThaumcraftRecipeDisplay.addonHolder(shapeless, shapeless.nativeRecipe()));
        }
    }

    @Test void aLiveInfusionMutationUpdatesThePreviewAndInvalidatesTheOldPageHolder() {
        var costs = new AspectList().add(Aspect.AIR, 2);
        var output = new ItemStack(Items.IRON_INGOT);
        var recipe = new InfusionRecipe("TEST", output, 2, costs, new ItemStack(Items.STONE),
                new ItemStack[] {new ItemStack(Items.QUARTZ)});
        var first = recipe.previewRecipe();
        var holder = ThaumcraftRecipeDisplay.addonHolder(recipe, first);
        assertSame(holder, ThaumcraftRecipeDisplay.addonHolder(recipe, recipe.previewRecipe()));
        costs.add(Aspect.AIR, 5);
        output.setCount(4);
        var changed = recipe.previewRecipe();
        var changedHolder = ThaumcraftRecipeDisplay.addonHolder(recipe, changed);
        assertEquals(7, changed.aspects().get("aer"));
        assertEquals(4, changed.result().getCount());
        assertEquals(2, first.aspects().get("aer"));
        assertEquals(1, first.result().getCount());
        assertNotSame(holder, changedHolder);
        assertSame(changedHolder, ThaumcraftRecipeDisplay.addonHolder(recipe, recipe.previewRecipe()));
    }

    @Test void inputAwareVirtualGettersSupplyTheActualSample() {
        var recipe = new InfusionRecipe("TEST", new ItemStack(Items.IRON_INGOT), 2,
                new AspectList().add(Aspect.AIR, 2), new ItemStack(Items.STONE),
                new ItemStack[] {new ItemStack(Items.QUARTZ)}) {
            @Override public Object getRecipeOutput(ItemStack input) { return new ItemStack(Items.GOLD_INGOT, 3); }
            @Override public AspectList getAspects(ItemStack input) { return new AspectList().add(Aspect.FIRE, 9); }
            @Override public int getInstability(ItemStack input) { return 6; }
            @Override public ItemStack[] getComponents() { return new ItemStack[] {new ItemStack(Items.BLAZE_ROD)}; }
        };
        var preview = recipe.previewRecipe();
        assertTrue(preview.result().is(Items.GOLD_INGOT));
        assertEquals(3, preview.result().getCount());
        assertEquals(Map.of("ignis", 9), preview.aspects());
        assertEquals(6, preview.instability());
        assertTrue(preview.components().getFirst().test(new ItemStack(Items.BLAZE_ROD)));
    }

    @Test void aNativeViewRetainsItsDeclaredAlternativesAndPredicates() {
        var nativeRecipe = new InfusionStackRecipe("TEST", Ingredient.of(Items.STONE, Items.COBBLESTONE),
                List.of(Ingredient.of(Items.IRON_INGOT, Items.GOLD_INGOT)), Map.of("aer", 2), 1,
                new ItemStack(Items.DIAMOND));
        assertSame(nativeRecipe, InfusionRecipe.of(nativeRecipe).previewRecipe());
    }
}
