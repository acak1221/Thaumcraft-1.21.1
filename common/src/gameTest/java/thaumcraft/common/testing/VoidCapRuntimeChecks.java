package thaumcraft.common.testing;

import java.util.List;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.crafting.ArcaneRecipeInput;
import thaumcraft.common.crafting.ArcaneShapedRecipe;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.crafting.CrucibleRecipeInput;
import thaumcraft.common.crafting.InfusionRecipeInput;
import thaumcraft.common.crafting.InfusionStackRecipe;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/**
 * Void metal and the void cap on real content: the two crucible recipes of VOIDMETAL (wheat seeds become a void seed, a void seed becomes
 * a void ingot), the nugget crafting both ways, the inert cap's arcane recipe (five void nuggets, the original order and prices), the
 * infusion that charges it (four Salis Mundus, instability 8) and a wand assembled with the void cap (price rod x 9, locked behind
 * CAP_void, a fifth off the vis it pays). Players and inputs are explicit; the recipes and the pricing are never supplied. NOT a
 * client check (the models and icons are unseen), a full altar ritual or a crucible brewed with a real pot.
 */
public final class VoidCapRuntimeChecks {
    private VoidCapRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        crucible(test, level);
        nuggets(test, level);
        cap(test, level);
        assembly(test, level);
    }

    private static AspectList aspects(String tag, int amount) { return new AspectList().add(Aspect.getAspect(tag), amount); }

    private static void crucible(GameTestHelper test, ServerLevel level) {
        var seed = (CrucibleRecipe) level.getRecipeManager().byKey(id("crucible/void_seed")).orElseThrow(() -> new AssertionError("no void seed recipe")).value();
        test.assertTrue(seed.research().equals("VOIDMETAL") && seed.result().is(item("thaumcraft:void_seed")) && seed.result().getCount() == 1
                        && seed.aspects().equals(Map.of("tenebrae", 8, "vacuos", 8, "alienis", 2)) && seed.sourceOrder() == 10,
                "the void seed: VOIDMETAL, tenebrae 8 + vacuos 8 + alienis 2, one seed: " + seed.aspects());
        AspectList brew = aspects("tenebrae", 8).add(Aspect.getAspect("vacuos"), 8).add(Aspect.getAspect("alienis"), 2);
        test.assertTrue(seed.matches(new CrucibleRecipeInput(new ItemStack(item("minecraft:wheat_seeds")), brew), level), "wheat seeds in a full brew make a void seed");
        test.assertTrue(!seed.matches(new CrucibleRecipeInput(new ItemStack(item("minecraft:beetroot_seeds")), brew), level), "other seeds do not");
        AspectList thin = aspects("tenebrae", 8).add(Aspect.getAspect("vacuos"), 8).add(Aspect.getAspect("alienis"), 1);
        test.assertTrue(!seed.matches(new CrucibleRecipeInput(new ItemStack(item("minecraft:wheat_seeds")), thin), level), "one eldritch short does not");
        var ingot = (CrucibleRecipe) level.getRecipeManager().byKey(id("crucible/void_ingot")).orElseThrow(() -> new AssertionError("no void ingot recipe")).value();
        test.assertTrue(ingot.research().equals("VOIDMETAL") && ingot.result().is(item("thaumcraft:void_ingot")) && ingot.result().getCount() == 1
                        && ingot.aspects().equals(Map.of("metallum", 8)) && ingot.sourceOrder() == 9,
                "the void ingot: VOIDMETAL, metallum 8, one ingot: " + ingot.aspects());
        test.assertTrue(ingot.matches(new CrucibleRecipeInput(new ItemStack(item("thaumcraft:void_seed")), aspects("metallum", 8)), level), "a void seed in metal makes the ingot");
        test.assertTrue(!ingot.matches(new CrucibleRecipeInput(new ItemStack(item("thaumcraft:void_seed")), aspects("metallum", 7)), level), "seven metal does not");
        test.assertTrue(!ingot.matches(new CrucibleRecipeInput(new ItemStack(item("minecraft:wheat_seeds")), aspects("metallum", 8)), level), "plain seeds do not");
    }

    private static void nuggets(GameTestHelper test, ServerLevel level) {
        var split = (CraftingRecipe) level.getRecipeManager().byKey(id("void_nuggets")).orElseThrow(() -> new AssertionError("no nugget recipe")).value();
        var one = CraftingInput.of(1, 1, List.of(new ItemStack(item("thaumcraft:void_ingot"))));
        test.assertTrue(split.matches(one, level), "an ingot splits");
        ItemStack nuggets = split.assemble(one, level.registryAccess());
        test.assertTrue(nuggets.is(item("thaumcraft:void_nugget")) && nuggets.getCount() == 9, "into nine void nuggets: " + nuggets);
        var join = (CraftingRecipe) level.getRecipeManager().byKey(id("void_ingot_from_nuggets")).orElseThrow(() -> new AssertionError("no ingot recipe")).value();
        List<ItemStack> nine = java.util.Collections.nCopies(9, new ItemStack(item("thaumcraft:void_nugget")));
        var grid = CraftingInput.of(3, 3, nine);
        test.assertTrue(join.matches(grid, level) && join.assemble(grid, level.registryAccess()).is(item("thaumcraft:void_ingot")), "nine nuggets make an ingot");
        test.assertTrue(!join.matches(CraftingInput.of(3, 3, java.util.Collections.nCopies(9, new ItemStack(item("thaumcraft:thaumium_nugget")))), level),
                "thaumium nuggets do not make a void ingot");
    }

    private static void cap(GameTestHelper test, ServerLevel level) {
        var inert = (ArcaneShapedRecipe) level.getRecipeManager().byKey(id("arcane/wand_cap_void_inert")).orElseThrow(() -> new AssertionError("no inert cap recipe")).value();
        test.assertTrue(inert.research().equals("CAP_void") && inert.pattern().equals(List.of("NNN", "N N")) && inert.result().is(item("thaumcraft:wand_cap_void_inert"))
                        && inert.result().getCount() == 1 && inert.vis().equals(Map.of("perditio", 27, "ordo", 27, "ignis", 18, "aer", 18)),
                "the inert cap: CAP_void, five nuggets in a ring, 27 entropy + 27 order + 18 fire + 18 air: " + inert.vis());
        NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int slot : new int[] {0, 1, 2, 3, 5}) grid.set(slot, new ItemStack(item("thaumcraft:void_nugget")));
        test.assertTrue(inert.matches(new ArcaneRecipeInput(grid), level), "five void nuggets in the ring make the inert cap");
        for (int slot : new int[] {0, 1, 2, 3, 5}) grid.set(slot, new ItemStack(item("thaumcraft:thaumium_nugget")));
        test.assertTrue(!inert.matches(new ArcaneRecipeInput(grid), level), "thaumium nuggets do not");
        var charge = (InfusionStackRecipe) level.getRecipeManager().byKey(id("infusion/wand_cap_void")).orElseThrow(() -> new AssertionError("no charging recipe")).value();
        test.assertTrue(charge.research().equals("CAP_void") && charge.instability() == 8 && charge.result().is(item("thaumcraft:wand_cap_void")) && charge.result().getCount() == 1
                        && charge.aspects().equals(Map.of("potentia", 18, "vacuos", 18, "alienis", 18, "auram", 18)) && charge.components().size() == 4,
                "the charging infusion: instability 8, 18 each of energy, void, eldritch and aura, four components: " + charge.aspects());
        ItemStack centre = new ItemStack(item("thaumcraft:wand_cap_void_inert"));
        List<ItemStack> four = java.util.Collections.nCopies(4, new ItemStack(item("thaumcraft:salis_mundus")));
        test.assertTrue(charge.matches(new InfusionRecipeInput(centre, four), level), "the inert cap and four Salis Mundus complete it");
        test.assertTrue(!charge.matches(new InfusionRecipeInput(centre, four.subList(0, 3)), level), "three do not");
        test.assertTrue(!charge.matches(new InfusionRecipeInput(new ItemStack(item("thaumcraft:wand_cap_thaumium_inert")), four), level), "the thaumium cap's blank is not the centre");
    }

    private static void assembly(GameTestHelper test, ServerLevel level) {
        ArcaneWandRecipe recipe = (ArcaneWandRecipe) level.getRecipeManager().byKey(id("arcane/wand_assembly")).orElseThrow().value();
        NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
        grid.set(6, ThaumcraftWands.capItem("void"));
        grid.set(2, ThaumcraftWands.capItem("void"));
        grid.set(4, ThaumcraftWands.rodItem("greatwood"));
        var input = new ArcaneRecipeInput(grid);
        test.assertTrue(recipe.matches(input, level), "a greatwood rod and two void caps make a wand");
        ItemStack wand = recipe.assemble(input, level.registryAccess());
        WandData data = ItemWandCasting.data(wand);
        test.assertTrue(data.rod().equals("greatwood") && data.cap().equals("void") && data.vis().isEmpty(), "the wand is greatwood with the void cap: " + data);
        test.assertTrue(wand.get(ThaumcraftWands.WAND_CRAFT_COST.get()) == 27 && recipe.vis(input).values().stream().allMatch(price -> price == 27) && recipe.vis(input).size() == 6,
                "the price is rod 3 x cap 9 = 27 of each primal");
        var player = test.makeMockServerPlayerInLevel();
        var knowledge = KnowledgeAccess.get(player);
        knowledge.addResearch("ROD_greatwood");
        test.assertTrue(!recipe.hasResearch(input, player), "locked behind CAP_void");
        knowledge.addResearch("CAP_void");
        test.assertTrue(recipe.hasResearch(input, player), "open with CAP_void");
        knowledge.removeResearch("CAP_void");
        knowledge.removeResearch("ROD_greatwood");
        // The cap's 0.8 takes a fifth off every vis the wand pays (original ItemWandCasting pricing: whole vis x 100 x the cap's modifier).
        Map<String, Integer> ten = Map.of("aer", 10);
        int[] expected = {1100, 1000, 900, 800};
        String[] caps = {"iron", "gold", "thaumium", "void"};
        for (int index = 0; index < caps.length; index++) {
            ItemStack capped = new ItemStack(ThaumcraftContent.WAND.get());
            ItemWandCasting.withData(capped, new WandData("greatwood", caps[index], Map.of()));
            int paid = ArcaneVis.price(ten, capped).get("aer");
            test.assertTrue(paid == expected[index], "ten vis through a " + caps[index] + " cap cost " + paid + " centivis, not " + expected[index]);
        }
        test.succeed();
    }
}
