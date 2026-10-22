package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneShapelessRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileArcaneWorkbench;

/** Explicit server fixtures, not survival acquisition. All recipe replacement is scoped and restored. */
public final class CraftingAuditRuntimeChecks {
    private CraftingAuditRuntimeChecks() {}

    public static void damagedIngredient(GameTestHelper test, boolean shapeless) {
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open();
            var manager = f.level.getRecipeManager();
            var originals = new ArrayList<>(manager.getRecipes());
            try {
                if (shapeless) {
                    // The stock shapeless recipes have no damageable ingredient; exercise the same
                    // original default-damage-zero contract using a clearly synthetic recipe.
                    manager.replaceRecipes(List.of(new RecipeHolder<>(ResourceLocation.parse("thaumcraft:audit_damage"),
                            new ArcaneShapelessRecipe("DECONSTRUCTOR", List.of(Ingredient.of(Items.GOLDEN_PICKAXE)),
                                    new ItemStack(Items.DIAMOND), Map.of("perditio", 20)))));
                }
                f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get());
                f.check(f.use(f.base).consumesAction(), "open real workbench");
                var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
                var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
                KnowledgeAccess.get(f.player).addResearch("DECONSTRUCTOR");
                int toolSlot = shapeless ? 8 : 5;
                if (!shapeless) {
                    var recipe = (ArcaneShapedRecipe) manager.byKey(ResourceLocation.parse("thaumcraft:arcane/deconstruction_table")).orElseThrow().value();
                    for (int y = 0; y < recipe.pattern().size(); y++) for (int x = 0; x < recipe.pattern().get(y).length(); x++) {
                        var ingredient = recipe.key().get(recipe.pattern().get(y).substring(x, x + 1));
                        if (ingredient != null) table.setItem(x + y * 3, ingredient.getItems()[0].copy());
                    }
                }
                table.setItem(toolSlot, new ItemStack(Items.GOLDEN_PICKAXE));
                table.setItem(10, wand());
                menu.broadcastChanges();
                f.check(menu.craftable(), "undamaged ingredient establishes positive recipe control");
                f.check(menu.requiredVis("perditio") == 2200, "original20 whole-vis times iron-cap1.1 is2200 centivis");
                KnowledgeAccess.get(f.player).removeResearch("DECONSTRUCTOR");
                menu.broadcastChanges();
                refuseUnchanged(f, table, menu, "missing research preserves every input and vis");
                KnowledgeAccess.get(f.player).addResearch("DECONSTRUCTOR");
                var shortWand = wand();
                shortWand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(shortWand).withAmount("perditio", 2199));
                table.setItem(10, shortWand); menu.broadcastChanges();
                refuseUnchanged(f, table, menu, "one-centivis shortage preserves every input and vis");
                table.setItem(10, wand()); menu.broadcastChanges();
                table.getItem(toolSlot).setDamageValue(1);
                menu.broadcastChanges();
                var before = table.saveWithoutMetadata(f.level.registryAccess());
                menu.clicked(0, 0, ClickType.PICKUP, f.player);
                // Check conservation before the result, so the pre-fix failure records the paid input loss.
                f.check(before.equals(table.saveWithoutMetadata(f.level.registryAccess())),
                        "damaged ingredient refusal must preserve EVERY input and wand vis");
                f.check(menu.getCarried().isEmpty() && !menu.craftable(), "original rejects damage1 where ingredient damage is0");
                table.getItem(toolSlot).setDamageValue(0);
                menu.broadcastChanges();
                var inputs = ArcaneRecipeInput.snapshot(table);
                var output = menu.getSlot(0).getItem().copy();
                int price = menu.requiredVis("perditio");
                int vis = ItemWandCasting.getVis(table.getItem(10), "perditio");
                menu.clicked(0, 0, ClickType.PICKUP, f.player);
                f.check(ItemStack.matches(output, menu.getCarried()) && !output.isEmpty(), "fresh tool produces exactly the expected output");
                for (int slot = 0; slot < 9; slot++) {
                    var expected = inputs.getItem(slot).copy();
                    if (!expected.isEmpty()) expected.shrink(1);
                    f.check(ItemStack.matches(expected, table.getItem(slot)), "success consumes precisely one input at slot " + slot);
                }
                f.check(ItemWandCasting.getVis(table.getItem(10), "perditio") == vis - price, "success pays the exact server-priced vis once");
            } finally { manager.replaceRecipes(originals); }
        }
        test.succeed();
    }

    public static void gridBoundaries(GameTestHelper test) {
        var key = Map.of("A", Ingredient.of(Items.STICK), "B", Ingredient.of(Items.DIAMOND));
        for (boolean mirror : List.of(false, true)) {
            var recipe = new ArcaneShapedRecipe("", List.of("AB"), key, new ItemStack(Items.EMERALD), Map.of("aer", 1), mirror);
            for (int x = 0; x < 2; x++) for (int y = 0; y < 3; y++) for (boolean reversed : List.of(false, true)) {
                var cells = new ArrayList<ItemStack>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
                cells.set(x + y * 3, new ItemStack(reversed ? Items.DIAMOND : Items.STICK));
                cells.set(x + 1 + y * 3, new ItemStack(reversed ? Items.STICK : Items.DIAMOND));
                test.assertTrue(recipe.matches(new ArcaneRecipeInput(cells), test.getLevel()) == (!reversed || mirror), "translated pattern and explicit mirror flag");
                for (int slot = 0; slot < 9; slot++) if (cells.get(slot).isEmpty()) {
                    cells.set(slot, new ItemStack(Items.DIRT, 64));
                    test.assertTrue(!recipe.matches(new ArcaneRecipeInput(cells), test.getLevel()), "extra outside pattern refuses: " + slot);
                    cells.set(slot, ItemStack.EMPTY);
                }
            }
        }
        try (var f = new TubeVariantsRuntimeChecks.Fixture(test)) {
            f.open(); f.put(f.base, ThaumcraftArcane.ARCANE_WORKBENCH.get()); f.use(f.base);
            var table = (TileArcaneWorkbench) f.level.getBlockEntity(f.base);
            var menu = (ContainerArcaneWorkbench) f.player.containerMenu;
            for (String id : List.of("enchanted_fabric", "essentia_restrict")) {
                table.clearContent();
                var recipe = (thaumcraft.common.crafting.NativeArcaneRecipe) f.level.getRecipeManager()
                        .byKey(ResourceLocation.parse("thaumcraft:arcane/" + id)).orElseThrow().value();
                KnowledgeAccess.get(f.player).addResearch(recipe.research());
                if (recipe instanceof ArcaneShapedRecipe shaped) {
                    for (int y = 0; y < shaped.pattern().size(); y++) for (int x = 0; x < shaped.pattern().get(y).length(); x++) {
                        var ingredient = shaped.key().get(shaped.pattern().get(y).substring(x, x + 1));
                        if (ingredient != null) table.setItem(x + y * 3, ingredient.getItems()[0].copyWithCount(2));
                    }
                } else {
                    int slot = 0;
                    for (var ingredient : recipe.getIngredients()) table.setItem(slot++, ingredient.getItems()[0].copyWithCount(2));
                }
                table.setItem(10, wand()); menu.broadcastChanges();
                f.check(menu.craftable(), "valid positive control: " + id);
                int extra = 8;
                f.check(table.getItem(extra).isEmpty(), "extra cell really is outside pattern");
                table.setItem(extra, new ItemStack(Items.DIRT, 64)); menu.broadcastChanges();
                var before = table.saveWithoutMetadata(f.level.registryAccess());
                menu.clicked(0, 0, ClickType.PICKUP, f.player);
                f.check(menu.getCarried().isEmpty() && !menu.craftable() && before.equals(table.saveWithoutMetadata(f.level.registryAccess())),
                        "extra stack refuses physical craft without consuming ANY ingredient or vis: " + id);
            }
        }
        test.succeed();
    }

    private static ItemStack wand() {
        var stack = new ItemStack(ThaumcraftContent.WAND.get());
        var vis = new java.util.LinkedHashMap<String, Integer>();
        for (String tag : thaumcraft.common.items.wands.WandData.PRIMALS) vis.put(tag, 5000);
        stack.set(ThaumcraftContent.WAND_DATA.get(), new thaumcraft.common.items.wands.WandData("greatwood", "iron", vis));
        return stack;
    }

    private static void refuseUnchanged(TubeVariantsRuntimeChecks.Fixture f, TileArcaneWorkbench table,
                                        ContainerArcaneWorkbench menu, String why) {
        var before = table.saveWithoutMetadata(f.level.registryAccess());
        menu.clicked(0, 0, ClickType.PICKUP, f.player);
        f.check(menu.getCarried().isEmpty() && !menu.craftable()
                && before.equals(table.saveWithoutMetadata(f.level.registryAccess())), why);
    }
}
