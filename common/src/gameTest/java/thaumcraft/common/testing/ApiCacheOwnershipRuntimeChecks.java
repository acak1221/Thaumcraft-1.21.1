package thaumcraft.common.testing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.NativeRecipes;
import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/** Uses the real recipe manager, research visibility and player; never constructs a Block. */
public final class ApiCacheOwnershipRuntimeChecks {
    private ApiCacheOwnershipRuntimeChecks() {}

    public static void recipeAnswer(GameTestHelper test, boolean wrongTypes) {
        var manager = NativeRecipes.manager();
        var registries = NativeRecipes.registries();
        var handler = ThaumcraftApi.internalMethods;
        try {
            // GameTest worlds need not have installed the addon facade's current world yet.
            ThaumcraftApi.installRecipeManager(test.getLevel().getRecipeManager(), test.getLevel().registryAccess());
            var player = test.makeMockServerPlayerInLevel();
            KnowledgeAccess.get(player).addResearch("ENCHFABRIC");
            // The legacy username bridge requires a logged-in connection, which a GameTest mock lacks.
            // Supply only that visibility seam; recipe discovery/cache behavior stays production code.
            ThaumcraftApi.internalMethods = new IInternalMethodHandler() {
                @Override public boolean isResearchComplete(String username, String research) {
                    return player.getGameProfile().getName().equals(username)
                            && KnowledgeAccess.get(player).isResearchComplete(research);
                }
            };
            ItemStack output = new ItemStack(BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse("thaumcraft:enchanted_fabric")));
            Object[] first = ThaumcraftApi.getCraftingRecipeKey(player, output);
            test.assertTrue(first != null, "real enchanted fabric recipe has a visible research page");
            String research = (String) first[0];
            int page = (Integer) first[1];
            test.assertTrue(research.equals("ENCHFABRIC"), "lookup resolves enchanted fabric research");
            first[0] = wrongTypes ? new Object() : "POISONED";
            first[1] = wrongTypes ? "not an Integer" : -123;
            Object[] second = ThaumcraftApi.getCraftingRecipeKey(player, output);
            test.assertTrue(second != null, "mutating the first answer cannot hide later answers");
            // These casts also guard against poisoned cache values throwing ClassCastException.
            test.assertTrue(research.equals((String) second[0]) && page == (Integer) second[1],
                    "first successful answer must not corrupt cached values or element types");
            second[0] = new Object();
            second[1] = "also not an Integer";
            Object[] third = ThaumcraftApi.getCraftingRecipeKey(player, output);
            test.assertTrue(third != null && research.equals((String) third[0]) && page == (Integer) third[1],
                    "cache-hit answers must also be independently mutable");
            test.succeed();
        } finally {
            ThaumcraftApi.internalMethods = handler;
            ThaumcraftApi.installRecipeManager(manager, registries);
        }
    }
}
