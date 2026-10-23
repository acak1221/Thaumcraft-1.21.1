package thaumcraft.common.testing;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.items.ItemNugget;
import thaumcraft.common.lib.items.EquipmentRules;

/** Loaded identities, datapack ingredients/results and tags; never constructs a block. */
public final class NuggetOwnershipRuntimeChecks {
    private NuggetOwnershipRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        var level = test.getLevel();
        var tab = ThaumcraftContent.TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), false, level.registryAccess()));
        for (String metal : List.of("thaumium", "void")) {
            String path = metal + "_nugget";
            var id = ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
            var owner = ThaumcraftContent.MATERIALS.get(path);
            var item = BuiltInRegistries.ITEM.get(id);
            test.assertTrue(BuiltInRegistries.ITEM.stream().filter(value -> BuiltInRegistries.ITEM.getKey(value).equals(id)).count() == 1,
                    "exactly one registered item for " + id);
            test.assertTrue(item instanceof ItemNugget, id + " retains ItemNugget behavior");
            test.assertTrue(((ItemNugget) item).type() == (metal.equals("thaumium") ? EquipmentRules.NUGGET_THAUMIUM : EquipmentRules.NUGGET_VOID),
                    "original metadata role for " + id);
            var alias = metal.equals("thaumium") ? ThaumcraftWands.THAUMIUM_NUGGET : ThaumcraftWands.VOID_NUGGET;
            test.assertTrue(alias == owner && alias.get() == item && owner.get() == item, "one supplier and instance for " + id);
            test.assertTrue(tab.getDisplayItems().stream().filter(stack -> stack.getItem() == item).count() == 1,
                    "one creative entry for " + id);

            for (String tagId : List.of("c:nuggets/" + metal, "thaumcraft:original_ore/nugget_" + metal)) {
                var tag = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId))).orElseThrow();
                test.assertTrue(tag.stream().filter(holder -> holder.value() == item).count() == 1, tagId + " contains the survivor once");
            }
            BuiltInRegistries.ITEM.getTags().forEach(tag -> tag.getSecond().forEach(holder -> {
                if (BuiltInRegistries.ITEM.getKey(holder.value()).equals(id)) {
                    test.assertTrue(holder.value() == owner.get(), tag.getFirst() + " resolves to the survivor");
                }
            }));
            // Check every loaded recipe, including tag-expanded alternatives, not just one cap.
            int references = 0;
            for (var holder : level.getRecipeManager().getRecipes()) {
                var recipe = holder.value();
                var result = recipe.getResultItem(level.registryAccess());
                if (BuiltInRegistries.ITEM.getKey(result.getItem()).equals(id)) {
                    test.assertTrue(result.getItem() == owner.get(), holder.id() + " result identity");
                    references++;
                }
                for (var ingredient : recipe.getIngredients()) {
                    for (var stack : ingredient.getItems()) {
                        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)) {
                            test.assertTrue(stack.getItem() == owner.get(), holder.id() + " ingredient identity");
                            references++;
                        }
                    }
                }
            }
            test.assertTrue(references >= 2, "loaded split/join recipe references for " + id);
            var split = (CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", metal + "_nuggets")).orElseThrow().value();
            var input = CraftingInput.of(1, 1, List.of(new ItemStack(ThaumcraftContent.MATERIALS.get(metal + "_ingot").get())));
            test.assertTrue(split.assemble(input, level.registryAccess()).getItem() == item, "split recipe survivor " + id);
            var cap = (ArcaneShapedRecipe) level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("thaumcraft", "arcane/wand_cap_" + metal + "_inert")).orElseThrow().value();
            var grid = NonNullList.withSize(9, ItemStack.EMPTY);
            for (int slot : new int[] {0, 1, 2, 3, 5}) grid.set(slot, new ItemStack(item));
            test.assertTrue(cap.matches(new ArcaneRecipeInput(grid), level), "cap accepts behavioral nuggets " + id);
        }
        test.succeed();
    }
}
