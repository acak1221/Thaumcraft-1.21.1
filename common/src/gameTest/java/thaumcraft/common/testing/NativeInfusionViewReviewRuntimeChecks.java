package thaumcraft.common.testing;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;

/** Real datapack load and public API enumeration; no replacement manager or fabricated registry. */
public final class NativeInfusionViewReviewRuntimeChecks {
    private NativeInfusionViewReviewRuntimeChecks() {}

    public static void emptyNativeTagDoesNotAbortPublicRecipeEnumeration(GameTestHelper test) {
        var level = test.getLevel();
        var manager = level.getRecipeManager();
        var fixtureId = ResourceLocation.fromNamespaceAndPath("thaumcraft_review", "empty_tag_infusion");
        var fixture = manager.byKey(fixtureId).orElseThrow().value();
        test.assertTrue(fixture instanceof InfusionStackRecipe, "the isolated infusion fixture passed the real datapack loader");
        var nativeRecipe = (InfusionStackRecipe) fixture;
        test.assertTrue(!nativeRecipe.input().isEmpty() && nativeRecipe.input().getItems().length == 0,
                "the native ingredient retains its empty optional tag rather than becoming an omitted ingredient");

        var views = ThaumcraftApi.getCraftingRecipes();
        var retained = views.stream().filter(value -> value instanceof InfusionRecipe recipe && recipe.nativeRecipe() == fixture).toList();
        test.assertTrue(retained.size() == 1, "public enumeration retains exactly one view of the unavailable tagged recipe");
        var view = (InfusionRecipe) retained.getFirst();
        test.assertTrue(view.getRecipeInput().isEmpty() && view.previewRecipe() == nativeRecipe,
                "empty display stock does not discard or rewrite the native recipe predicate");
        test.assertTrue(!nativeRecipe.matches(new InfusionRecipeInput(new ItemStack(Items.STONE),
                List.of(new ItemStack(Items.DIRT))), level), "an empty optional tag cannot grant an unpaid craft");

        var ordinaryId = ResourceLocation.fromNamespaceAndPath("thaumcraft", "infusion/boots_traveller");
        var ordinary = manager.byKey(ordinaryId).orElseThrow().value();
        var output = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft", "boots_traveller"));
        var found = ThaumcraftApi.getInfusionRecipe(new ItemStack(output));
        test.assertTrue(found != null && found.nativeRecipe() == ordinary && "BOOTSTRAVELLER".equals(found.getResearch()),
                "the empty optional tag cannot break public lookup of the unrelated real boots recipe");
        test.succeed();
    }
}
