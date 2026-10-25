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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.ArcaneRecipes;
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

    @Test void requiredArcaneDataIsNotLostAndPlainRequirementsStillAllowExtraData() {
        ItemStack required = new ItemStack(Items.STONE);
        required.set(DataComponents.CUSTOM_NAME, Component.literal("required"));
        ItemStack wrong = required.copy();
        wrong.set(DataComponents.CUSTOM_NAME, Component.literal("wrong"));
        var shaped = new ShapedArcaneRecipe("", new ItemStack(Items.DIAMOND), new AspectList().add(Aspect.AIR, 1), "A", 'A', required);
        var shapeless = new ShapelessArcaneRecipe("", new ItemStack(Items.DIAMOND), new AspectList().add(Aspect.AIR, 1), required);
        for (var recipe : List.<thaumcraft.api.crafting.IArcaneRecipe>of(shaped, shapeless)) {
            assertTrue(recipe.matches(matrix(required), null, null));
            assertFalse(recipe.matches(matrix(new ItemStack(Items.STONE)), null, null));
            assertFalse(recipe.matches(matrix(wrong), null, null));
        }
        var plain = new ShapelessArcaneRecipe("", new ItemStack(Items.DIAMOND), new AspectList().add(Aspect.AIR, 1), Items.STONE);
        assertTrue(plain.matches(matrix(required), null, null));
        var requiredData = new net.minecraft.nbt.CompoundTag(); requiredData.putInt("a", 1);
        var extraData = requiredData.copy(); extraData.putInt("b", 2);
        ItemStack dataRequirement = new ItemStack(Items.STONE), dataOffer = new ItemStack(Items.STONE);
        dataRequirement.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(requiredData));
        dataOffer.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(extraData));
        var subset = new ShapelessArcaneRecipe("", new ItemStack(Items.DIAMOND), new AspectList().add(Aspect.AIR, 1), dataRequirement);
        assertTrue(subset.matches(matrix(dataOffer), null, null));
        extraData.putInt("a", 9); dataOffer.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(extraData));
        assertFalse(subset.matches(matrix(dataOffer), null, null));
    }

    @Test void theModernProtectedArcaneComparatorsParticipateInMatching() {
        int[] calls = {0, 0};
        var shaped = new ShapedArcaneRecipe("", new ItemStack(Items.DIAMOND), new AspectList().add(Aspect.AIR, 1), "A", 'A', Items.STONE) {
            @Override protected boolean checkItemEquals(ItemStack required, ItemStack offered) { calls[0]++; return false; }
        };
        var shapeless = new ShapelessArcaneRecipe("", new ItemStack(Items.DIAMOND), new AspectList().add(Aspect.AIR, 1), Items.STONE) {
            @Override protected boolean checkItemEquals(ItemStack required, ItemStack offered) { calls[1]++; return false; }
        };
        assertFalse(shaped.matches(matrix(new ItemStack(Items.STONE)), null, null));
        assertFalse(shapeless.matches(matrix(new ItemStack(Items.STONE)), null, null));
        assertTrue(calls[0] > 0 && calls[1] > 0);
    }

    @Test void acceptedInfusionSnapshotsRetainRequiredDataThroughTheirCodec() {
        ItemStack accepted = new ItemStack(Items.STONE);
        accepted.set(DataComponents.CUSTOM_NAME, Component.literal("accepted"));
        var predicate = thaumcraft.common.crafting.InfusionComponentPredicate.snapshot(accepted);
        accepted.remove(DataComponents.CUSTOM_NAME);
        assertFalse(predicate.matches(accepted));
        ItemStack exposed = predicate.requiredStack().orElseThrow();
        exposed.remove(DataComponents.CUSTOM_NAME);
        assertFalse(predicate.matches(exposed));
        var ops = ArcaneRecipes.registries().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var encoded = thaumcraft.common.crafting.InfusionComponentPredicate.CODEC.encodeStart(ops, predicate).getOrThrow();
        var restored = thaumcraft.common.crafting.InfusionComponentPredicate.CODEC.parse(ops, encoded).getOrThrow();
        ItemStack matching = new ItemStack(Items.STONE);
        matching.set(DataComponents.CUSTOM_NAME, Component.literal("accepted"));
        assertTrue(restored.matches(matching));
        assertFalse(restored.matches(new ItemStack(Items.STONE)));
    }
}
