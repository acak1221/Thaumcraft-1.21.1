package thaumcraft.common.testing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEquipment;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Finite source arithmetic from actual registered recipes/tags, not granted
 * essentia or a claim that a client already crafted/smelted these inputs. */
public final class BoreSourceBudgetRuntimeChecks {
    private BoreSourceBudgetRuntimeChecks() {}

    public static void completeFinite112EssentiaBudget(GameTestHelper test) {
        NodeStabilizerRuntimeChecks.requireLoadedFootprint(test);
        var level = test.getLevel();
        var pick = new ItemStack(ThaumcraftEquipment.THAUMIUM_PICKAXE.get());
        var expectedPick = new LinkedHashMap<String, Integer>();
        expectedPick.put("metallum", 9); expectedPick.put("praecantatio", 4); expectedPick.put("arbor", 1); expectedPick.put("perfodio", 4);
        AspectList actualPick = ObjectAspectResolver.get(pick, level);
        test.assertTrue(map(actualPick).equals(expectedPick), "complete actual Thaumium source-tool recipe base + original perfodio4 bonus, not a nominal mining-only label");
        test.assertTrue(map(ObjectAspectResolver.get(pick, level)).equals(map(actualPick)), "repeat lookup preserves complete source tags");
        var sources = List.of(new ItemStack(Items.PISTON, 4), pick.copyWithCount(8), new ItemStack(Items.REDSTONE, 24), new ItemStack(ThaumcraftContent.EMPTY_PHIAL.get(), 16));
        var expectedPiston = Map.of("arbor", 2, "terra", 3, "perditio", 3, "metallum", 3, "machina", 2, "motus", 4);
        test.assertTrue(map(ObjectAspectResolver.get(sources.get(0), level)).equals(expectedPiston), "complete17-unit piston, not its6-unit complex addition alone");
        test.assertTrue(map(ObjectAspectResolver.get(sources.get(2), level)).equals(Map.of("potentia", 2, "machina", 1)), "raw redstone has both collateral tags");
        test.assertTrue(map(ObjectAspectResolver.get(sources.get(3), level)).equals(Map.of("vacuos", 1)), "empty source phial is consumed material, not prefilled essence");
        var produced = new LinkedHashMap<String, Integer>();
        for (ItemStack source : sources) {
            var tags = ObjectAspectResolver.get(source, level);
            for (Aspect aspect : tags.getAspects()) produced.merge(aspect.getTag(), Math.multiplyExact(tags.getAmount(aspect), source.getCount()), Math::addExact);
        }
        var expected = Map.of("arbor", 16, "terra", 12, "perditio", 12, "metallum", 84, "machina", 32,
                "motus", 16, "praecantatio", 32, "perfodio", 32, "potentia", 48, "vacuos", 16);
        test.assertTrue(produced.equals(expected) && produced.values().stream().mapToInt(Integer::intValue).sum() == 300,
                "four piston +8 actual source picks +24redstone +16empty phials have exactly300 units including all collateral");
        var recipe = (InfusionStackRecipe) level.getRecipeManager().byKey(TubeVariantsRuntimeChecks.id("infusion/arcane_bore")).orElseThrow().value();
        var after = new LinkedHashMap<>(produced);
        recipe.aspects().forEach((aspect, amount) -> {
            test.assertTrue(after.getOrDefault(aspect, 0) >= amount, "finite actual sources cover " + aspect);
            int remaining = after.get(aspect) - amount;
            if (remaining == 0) after.remove(aspect); else after.put(aspect, remaining);
        });
        test.assertTrue(after.equals(Map.of("arbor", 16, "terra", 12, "perditio", 12, "metallum", 84, "praecantatio", 32, "potentia", 32))
                && after.values().stream().mapToInt(Integer::intValue).sum() == 188, "112 paid demand leaves ALL188 collateral/surplus units, not cleared vessels");
        test.succeed();
    }

    private static Map<String, Integer> map(AspectList tags) {
        var out = new LinkedHashMap<String, Integer>();
        for (Aspect aspect : tags.getAspects()) out.put(aspect.getTag(), tags.getAmount(aspect));
        return out;
    }
}
