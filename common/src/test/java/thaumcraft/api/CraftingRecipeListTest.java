package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
import thaumcraft.api.crafting.ArcaneRecipes;

/**
 * The public recipe list of the original API, checked against the oracle.
 *
 * <p>The original kept every recipe of the game in one static list and published it through
 * {@code ThaumcraftApi.getCraftingRecipes()} (oracle {@code thaumcraft/api/ThaumcraftApi.java:100-102});
 * the lookup helpers of the original walked that list ({@code getInfusionRecipe} at oracle
 * {@code :131-136}, {@code getCrucibleRecipe} at oracle {@code :145-150}). The audit a9 wrote that the
 * native list "is always empty" ({@code docs/analysis/2026-10-21-audit-a9-core.md:88-92}); that was the
 * state before the port installed the recipe manager of the loaded level.</p>
 *
 * <p>Forced 1.21.1 change: 1.21.1 keeps the recipes of a world in the recipe manager of the loaded
 * level, so the native list answers from that manager. The recipes of the port themselves need the
 * registered recipe types, which a bare unit test does not have; the runtime check of the game
 * ({@code thaumcraft.common.testing.CraftingRecipeApiRuntimeChecks}) counts the recipes of a running
 * server and asks the original lookups for the native crucible and infusion recipes.</p>
 */
class CraftingRecipeListTest {
    private static final RecipeType<FakeRecipe> FAKE_TYPE = new RecipeType<>() {
        @Override public String toString() { return "thaumcraft:test_fake"; }
    };

    @BeforeAll static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void thePublicListHoldsEveryRecipeOfTheLoadedLevel() {
        RecipeManager manager = new RecipeManager(ArcaneRecipes.registries());
        manager.replaceRecipes(List.of(
                new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "test/first"), new FakeRecipe()),
                new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "test/second"), new FakeRecipe()),
                new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("thaumcraft", "test/third"), new FakeRecipe())));
        int before = ThaumcraftApi.getCraftingRecipes().size();
        ThaumcraftApi.installRecipeManager(manager, ArcaneRecipes.registries());
        try {
            List<Object> published = ThaumcraftApi.getCraftingRecipes();
            assertEquals(before + 3, published.size(), "the public list holds every recipe of the loaded level");
            assertTrue(published.stream().anyMatch(recipe -> recipe instanceof FakeRecipe),
                    "the recipes of the level belong to the public list");
        } finally {
            ThaumcraftApi.installRecipeManager(null, null);
        }
    }

    @Test void withoutALoadedLevelTheListHoldsOnlyTheProgrammaticRecipes() {
        ThaumcraftApi.installRecipeManager(null, null);
        // The port registers its recipes as data pack recipes, so the programmatic list stays empty until a
        // level is loaded; the runtime installs the recipe manager of the server on its start.
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
