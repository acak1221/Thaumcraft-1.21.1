package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.crafting.ArcaneRecipes;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.crafting.InfusionStackRecipe;

/**
 * The public recipe API answers from the native registries.
 *
 * <p>Original ThaumcraftApi:100-198 kept every recipe in one static list; the native port keeps the
 * recipes of a world in the recipe manager of the loaded level, so the API reads them from there.</p>
 */
class NativeRecipeApiTest {
    private static final RecipeType<FakeRecipe> FAKE_TYPE = new RecipeType<>() {
        @Override public String toString() { return "thaumcraft:test_fake"; }
    };

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void thePublicRecipeListAnswersFromTheRecipeManager() {
        RecipeManager manager = new RecipeManager(ArcaneRecipes.registries());
        RecipeHolder<?> holder = new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "test/fake"), new FakeRecipe());
        manager.replaceRecipes(List.of(holder));
        ThaumcraftApi.installRecipeManager(manager, ArcaneRecipes.registries());
        try {
            assertTrue(ThaumcraftApi.getCraftingRecipes().contains(holder.value()),
                    "the native recipes of the loaded level belong to the public recipe list");
        } finally {
            ThaumcraftApi.installRecipeManager(null, null);
        }
    }

    @Test void theNativeShapedArcaneRecipeBecomesTheOriginalPublicView() {
        ArcaneShapedRecipe arcane = new ArcaneShapedRecipe("TESTKEY", List.of("AA"),
                Map.of("A", Ingredient.of(Items.STONE)), new ItemStack(Items.DIRT, 2), Map.of("aer", 5), true);
        ShapedArcaneRecipe view = ShapedArcaneRecipe.of(arcane);
        assertEquals("TESTKEY", view.getResearch());
        assertEquals(Items.DIRT, view.getRecipeOutput().getItem());
        assertEquals(2, view.getRecipeOutput().getCount());
        assertEquals(5, view.getAspects().getAmount(Aspect.AIR));
        assertEquals(2, view.getRecipeSize(), "the two cells of the AA pattern");
    }

    @Test void theNativeShapelessArcaneRecipeBecomesTheOriginalPublicView() {
        ArcaneShapelessRecipe shapeless = new ArcaneShapelessRecipe("TESTKEY", List.of(Ingredient.of(Items.STONE)),
                new ItemStack(Items.DIRT), Map.of("ignis", 3));
        ShapelessArcaneRecipe view = ShapelessArcaneRecipe.of(shapeless);
        assertEquals("TESTKEY", view.getResearch());
        assertEquals(Items.DIRT, view.getRecipeOutput().getItem());
        assertEquals(3, view.getAspects().getAmount(Aspect.FIRE));
        assertEquals(1, view.getRecipeSize());
    }

    @Test void theNativeCrucibleRecipeBecomesTheOriginalPublicView() {
        thaumcraft.common.crafting.CrucibleRecipe crucible = new thaumcraft.common.crafting.CrucibleRecipe("TESTKEY",
                Ingredient.of(Items.GLOWSTONE_DUST), new ItemStack(Items.SAND), Map.of("terra", 2), 7);
        CrucibleRecipe view = CrucibleRecipe.of(crucible);
        assertEquals("TESTKEY", view.key);
        assertEquals(Items.SAND, view.getRecipeOutput().getItem());
        assertEquals(2, view.aspects.getAmount(Aspect.EARTH));
        assertTrue(view.catalystMatches(new ItemStack(Items.GLOWSTONE_DUST)));
    }

    @Test void theNativeInfusionRecipeBecomesTheOriginalPublicView() {
        InfusionStackRecipe infusion = new InfusionStackRecipe("TESTKEY", Ingredient.of(Items.STONE),
                List.of(Ingredient.of(Items.DIRT)), Map.of("terra", 5), 2, new ItemStack(Items.DIAMOND));
        InfusionRecipe view = InfusionRecipe.of(infusion);
        assertEquals("TESTKEY", view.getResearch());
        assertEquals(Items.DIAMOND, ((ItemStack) view.getRecipeOutput()).getItem());
        assertEquals(5, view.getAspects().getAmount(Aspect.EARTH));
        assertEquals(2, view.getInstability());
        assertSame(infusion, view.nativeRecipe(), "the view keeps the native recipe of the recipe manager");
    }

    @Test void theRecipeManagerOfTheLoadedLevelIsTheOnlySource() {
        assertNotNull(ArcaneRecipes.registries());
        ThaumcraftApi.installRecipeManager(null, null);
        assertTrue(ThaumcraftApi.getCraftingRecipes().stream().noneMatch(recipe -> recipe instanceof FakeRecipe),
                "without a loaded level the public list holds only the programmatic recipes");
    }

    /** A recipe of the recipe manager that is not one of the original kinds. */
    private static final class FakeRecipe implements Recipe<RecipeInput> {
        @Override public boolean matches(RecipeInput input, Level level) { return false; }
        @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
        @Override public boolean canCraftInDimensions(int width, int height) { return false; }
        @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return new ItemStack(Items.DIRT); }
        @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.create(); }
        @Override public RecipeSerializer<?> getSerializer() { return null; }
        @Override public RecipeType<?> getType() { return FAKE_TYPE; }
    }
}
