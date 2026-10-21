package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.common.crafting.ArcaneRecipeInput;

/** The recipe surface of the public API, on top of the native recipe implementations. */
class CraftingApiTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static ArcaneRecipeInput matrix(ItemStack... slots) {
        List<ItemStack> items = new ArrayList<>(List.of(slots));
        while (items.size() < 9) items.add(ItemStack.EMPTY);
        return new ArcaneRecipeInput(items);
    }

    @Test void aShapedRecipeIsBuiltAndMatchesTheOriginalMatrix() {
        AspectList aspects = new AspectList().add(Aspect.AIR, 2);
        ShapedArcaneRecipe recipe = new ShapedArcaneRecipe("", new ItemStack(Items.DIRT, 2), aspects, "A", 'A', new ItemStack(Items.STONE));
        assertEquals(1, recipe.getRecipeSize());
        assertEquals(Items.DIRT, recipe.getRecipeOutput().getItem());
        assertEquals(2, recipe.getRecipeOutput().getCount());
        assertEquals(2, recipe.getAspects().getAmount(Aspect.AIR));
        assertEquals("", recipe.getResearch());
        assertNotNull(recipe.nativeRecipe());
        assertTrue(recipe.matches(matrix(new ItemStack(Items.STONE)), null, null));
        assertFalse(recipe.matches(matrix(new ItemStack(Items.DIRT)), null, null));
    }

    @Test void aShapedRecipeNeedsAnIngredientForEverySymbol() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShapedArcaneRecipe("", new ItemStack(Items.DIRT), new AspectList(), "A", 'B', new ItemStack(Items.STONE)));
    }

    @Test void aShapelessRecipeIsBuiltAndMatchesTheOriginalMatrix() {
        AspectList aspects = new AspectList().add(Aspect.FIRE, 3);
        ShapelessArcaneRecipe recipe = new ShapelessArcaneRecipe("", new ItemStack(Items.DIRT), aspects, new ItemStack(Items.STONE));
        assertEquals(1, recipe.getRecipeSize());
        assertEquals(Items.DIRT, recipe.getRecipeOutput().getItem());
        assertTrue(recipe.matches(matrix(new ItemStack(Items.STONE)), null, null));
        assertFalse(recipe.matches(matrix(new ItemStack(Items.STONE), new ItemStack(Items.DIRT)), null, null));
    }

    @Test void anInfusionRecipeMatchesTheOriginalCentreAndComponents() {
        AspectList aspects = new AspectList().add(Aspect.EARTH, 10);
        InfusionRecipe recipe = new InfusionRecipe("", new ItemStack(Items.DIAMOND), 2, aspects,
                new ItemStack(Items.STONE), new ItemStack[]{new ItemStack(Items.DIRT)});
        assertEquals("", recipe.getResearch());
        assertEquals(2, recipe.getInstability());
        assertEquals(10, recipe.getAspects().getAmount(Aspect.EARTH));
        assertEquals(Items.DIAMOND, ((ItemStack) recipe.getRecipeOutput()).getItem());
        assertEquals(Items.STONE, recipe.getRecipeInput().getItem());
        assertEquals(1, recipe.getComponents().length);
        ArrayList<ItemStack> components = new ArrayList<>(List.of(new ItemStack(Items.DIRT)));
        assertTrue(recipe.matches(components, new ItemStack(Items.STONE), null, null));
        assertFalse(recipe.matches(new ArrayList<>(List.of(new ItemStack(Items.DIRT))), new ItemStack(Items.DIRT), null, null));
    }

    @Test void aCrucibleRecipeMatchesItsCatalystAndEssence() {
        AspectList aspects = new AspectList().add(Aspect.EARTH, 2);
        CrucibleRecipe recipe = new CrucibleRecipe("TESTKEY", new ItemStack(Items.DIRT), new ItemStack(Items.GLOWSTONE), aspects);
        assertEquals("TESTKEY", recipe.key);
        assertTrue(recipe.catalystMatches(new ItemStack(Items.GLOWSTONE)));
        assertFalse(recipe.catalystMatches(new ItemStack(Items.STONE)));
        assertTrue(recipe.matches(new AspectList().add(Aspect.EARTH, 2), new ItemStack(Items.GLOWSTONE)));
        assertFalse(recipe.matches(new AspectList().add(Aspect.EARTH, 1), new ItemStack(Items.GLOWSTONE)));
        AspectList remaining = recipe.removeMatching(new AspectList().add(Aspect.EARTH, 5));
        assertEquals(3, remaining.getAmount(Aspect.EARTH));
    }

    @Test void thePublicRecipeRegistryAnswersTheOriginalQueries() {
        AspectList aspects = new AspectList().add(Aspect.AIR, 1);
        ShapedArcaneRecipe recipe = ThaumcraftApi.addArcaneCraftingRecipe("APIKEY", new ItemStack(Items.DIRT), aspects,
                "A", 'A', new ItemStack(Items.STONE));
        assertNotNull(recipe);
        assertTrue(ThaumcraftApi.getCraftingRecipes().contains(recipe));
        CrucibleRecipe crucible = ThaumcraftApi.addCrucibleRecipe("APIKEY2", new ItemStack(Items.SAND),
                new ItemStack(Items.GLOWSTONE), new AspectList().add(Aspect.EARTH, 1));
        assertNotNull(crucible);
        assertNotNull(ThaumcraftApi.getCrucibleRecipe(new ItemStack(Items.SAND)));
        assertNotNull(ThaumcraftApi.getCrucibleRecipeFromHash(crucible.hash));
    }

    @Test void theSmeltingBonusRegistryAnswersTheOriginalQuery() {
        ThaumcraftApi.addSmeltingBonus(new ItemStack(Items.RAW_IRON), new ItemStack(Items.IRON_INGOT, 2));
        ItemStack bonus = ThaumcraftApi.getSmeltingBonus(new ItemStack(Items.RAW_IRON));
        assertNotNull(bonus);
        assertEquals(Items.IRON_INGOT, bonus.getItem());
        // The original stores the amount zero (oracle thaumcraft/api/ThaumcraftApi.java:80-82),
        // because the arcane furnace decides the amount with its own bellows rolls
        // (oracle TileArcaneFurnace.java:237-259). 1.21.1 cannot hold an amount-zero
        // stack: ItemStack.copy() of it returns the shared ItemStack.EMPTY, so the
        // native registry keeps the amount the addon registered.
        assertEquals(2, bonus.getCount());
    }
}
