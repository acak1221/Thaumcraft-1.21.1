package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.crafting.ArcaneRecipes;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.NativeRecipes;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;

class NativeInfusionViewReviewTest {
    private static final TagKey<Item> EMPTY_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("thaumcraft_review", "absent_optional_ingredient"));

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void nativeViewRetainsAValidButCurrentlyEmptyCentralTag() {
        InfusionStackRecipe recipe = recipe(Ingredient.of(EMPTY_TAG), Ingredient.of(Items.DIRT));
        assertFalse(recipe.input().isEmpty(), "a declared native tag is not the empty ingredient sentinel");
        assertEquals(0, recipe.input().getItems().length, "the optional tag currently has no matching item");

        InfusionRecipe view = assertDoesNotThrow(() -> InfusionRecipe.of(recipe));
        assertSame(recipe, view.nativeRecipe());
        assertSame(recipe, view.previewRecipe(), "retain the native tag predicate, even with no display sample");
        assertTrue(view.getRecipeInput().isEmpty());
        assertFalse(view.nativeRecipe().matches(new InfusionRecipeInput(new ItemStack(Items.STONE),
                List.of(new ItemStack(Items.DIRT))), null));
    }

    @Test void nativeViewRetainsAnEmptyComponentTagWithoutBreakingOtherRecipeViews() {
        InfusionStackRecipe recipe = recipe(Ingredient.of(Items.STONE), Ingredient.of(EMPTY_TAG));
        InfusionRecipe view = assertDoesNotThrow(() -> (InfusionRecipe) NativeRecipes.view(recipe));
        assertSame(recipe, view.nativeRecipe());
        assertEquals(1, view.getComponents().length);
        assertTrue(view.getComponents()[0].isEmpty());
        assertFalse(view.nativeRecipe().matches(new InfusionRecipeInput(new ItemStack(Items.STONE),
                List.of(new ItemStack(Items.DIRT))), null));
        InfusionRecipe unrelated = assertDoesNotThrow(() -> (InfusionRecipe) NativeRecipes.view(
                recipe(Ingredient.of(Items.STONE), Ingredient.of(Items.DIRT))));
        assertTrue(unrelated.nativeRecipe().matches(new InfusionRecipeInput(new ItemStack(Items.STONE),
                List.of(new ItemStack(Items.DIRT))), null));
    }

    @Test void emptyTagIsAcceptedByTheRealNativeDatapackCodec() {
        var json = JsonParser.parseString("""
                {"input":{"tag":"thaumcraft_review:absent_optional_ingredient"},
                 "components":[{"item":"minecraft:dirt"}],
                 "aspects":{"terra":1},"instability":0,"result":{"id":"minecraft:diamond"}}
                """);
        var ops = RegistryOps.create(JsonOps.INSTANCE, ArcaneRecipes.registries());
        InfusionStackRecipe recipe = InfusionStackRecipe.CODEC.codec().parse(ops, json).getOrThrow();
        assertEquals(0, recipe.input().getItems().length);
        assertDoesNotThrow(() -> InfusionRecipe.of(recipe));
    }

    @Test void programmaticRecipesStillRejectEmptyPhysicalIngredients() {
        var cost = new AspectList().add(Aspect.EARTH, 1);
        assertThrows(IllegalArgumentException.class, () -> new InfusionRecipe("", new ItemStack(Items.DIAMOND),
                0, cost, ItemStack.EMPTY, new ItemStack[]{new ItemStack(Items.DIRT)}));
        assertThrows(IllegalArgumentException.class, () -> new InfusionRecipe("", new ItemStack(Items.DIAMOND),
                0, cost, new ItemStack(Items.STONE), new ItemStack[]{ItemStack.EMPTY}));
    }

    private static InfusionStackRecipe recipe(Ingredient central, Ingredient component) {
        return new InfusionStackRecipe("", central, List.of(component), Map.of("terra", 1), 0,
                new ItemStack(Items.DIAMOND));
    }
}
