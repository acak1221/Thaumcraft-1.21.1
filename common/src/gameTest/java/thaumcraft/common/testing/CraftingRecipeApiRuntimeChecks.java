package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.common.lib.research.ResearchRecipeCoverage;

/**
 * The public recipe list of the original API on a running server.
 *
 * <p>The original kept every recipe of the game in one static list and published it through
 * {@code ThaumcraftApi.getCraftingRecipes()} (oracle {@code thaumcraft/api/ThaumcraftApi.java:100-102});
 * the original lookups walked that list ({@code getInfusionRecipe} at oracle {@code :131-136},
 * {@code getCrucibleRecipe} at oracle {@code :145-150}). The audit a9 wrote that the native list "is always
 * empty" ({@code docs/analysis/2026-10-21-audit-a9-core.md:88-92}); the port installs the recipe manager of
 * the loaded level on the server start, so the list holds the recipes of the world.</p>
 *
 * <p>The check reads the running server: the data pack recipes of the port are only bound there.</p>
 */
public final class CraftingRecipeApiRuntimeChecks {
    private CraftingRecipeApiRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        List<Object> published = ThaumcraftApi.getCraftingRecipes();
        test.assertTrue(!published.isEmpty(), "the public recipe list is not empty on a running server: " + published.size());
        // Every packaged native recipe of the port is loaded and published.
        Set<String> packaged = ResearchRecipeCoverage.nativeRecipeIdsPresent();
        List<String> missing = new ArrayList<>();
        for (String id : packaged) {
            if (level.getRecipeManager().byKey(ResourceLocation.parse(id)).isEmpty()) missing.add(id);
        }
        test.assertTrue(missing.isEmpty(), "every packaged native recipe is loaded: " + missing);
        long nativeCount = level.getRecipeManager().getRecipeIds()
                .filter(id -> id.getNamespace().equals("thaumcraft")).count();
        test.assertTrue(nativeCount >= packaged.size(),
                "the level holds every native recipe of the port: " + nativeCount + " of " + packaged.size());
        // The original lookups find the native recipes of the world (oracle ThaumcraftApi.java:131-150).
        var crucible = ThaumcraftApi.getCrucibleRecipe(new ItemStack(Items.BONE_MEAL));
        test.assertTrue(crucible != null, "the original crucible lookup finds the native crucible recipe");
        var infusion = ThaumcraftApi.getInfusionRecipe(new ItemStack(BuiltInRegistries.ITEM
                .get(ResourceLocation.parse("thaumcraft:runic_girdle_kinetic"))));
        test.assertTrue(infusion != null, "the original infusion lookup finds the native infusion recipe");
        test.succeed();
    }
}
