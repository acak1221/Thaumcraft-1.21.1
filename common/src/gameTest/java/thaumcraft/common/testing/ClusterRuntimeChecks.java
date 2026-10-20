package thaumcraft.common.testing;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.crafting.CrucibleRecipeInput;
import thaumcraft.common.lib.research.ObjectAspectCatalog;

/**
 * The three native clusters (original ItemNugget metadata 16 iron, 31 gold, 21 cinnabar) on real content: the crucible recipes that purify
 * ore into a cluster and triple a nugget (ConfigRecipes:112-113, 126-127), the furnace recipes that melt a cluster into two ingots
 * (ConfigRecipes:469-471, run through a real vanilla furnace) and the aspects of ConfigAspects:360-362. Inputs are explicit; the recipes
 * and the tags are never supplied. NOT a check of the harvest-time cluster drop (Pickaxe of the Core, Excavation dowsing: not ported),
 * a crucible brewed with a real pot or the icons.
 */
public final class ClusterRuntimeChecks {
    private ClusterRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("thaumcraft", path); }

    private static AspectList brew(Object... pairs) {
        AspectList list = new AspectList();
        for (int index = 0; index < pairs.length; index += 2) list.add(Aspect.getAspect((String) pairs[index]), (Integer) pairs[index + 1]);
        return list;
    }

    private static CrucibleRecipe crucible(ServerLevel level, String path) {
        return (CrucibleRecipe) level.getRecipeManager().byKey(id("crucible/" + path)).orElseThrow(() -> new AssertionError("no crucible recipe " + path)).value();
    }

    public static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        purify(test, level, "pure_iron", "PUREIRON", 22, "thaumcraft:cluster_iron", "minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:raw_iron", "minecraft:gold_ore");
        purify(test, level, "pure_gold", "PUREGOLD", 23, "thaumcraft:cluster_gold", "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:raw_gold", "minecraft:iron_ore");
        triple(test, level, "trans_iron", "TRANSIRON", 24, "minecraft:iron_nugget", Map.of("metallum", 2), brew("metallum", 2), brew("metallum", 1), "minecraft:gold_nugget");
        triple(test, level, "trans_gold", "TRANSGOLD", 25, "minecraft:gold_nugget", Map.of("metallum", 2, "lucrum", 1), brew("metallum", 2, "lucrum", 1), brew("metallum", 2), "minecraft:iron_nugget");
        aspects(test);
        furnaces(test, level);
    }

    private static void purify(GameTestHelper test, ServerLevel level, String path, String research, int order, String cluster, String ore, String deepslate, String raw, String foreign) {
        var recipe = crucible(level, path);
        test.assertTrue(recipe.research().equals(research) && recipe.result().is(item(cluster)) && recipe.result().getCount() == 1
                        && recipe.aspects().equals(Map.of("metallum", 1, "ordo", 1)) && recipe.sourceOrder() == order,
                path + ": " + research + ", one " + cluster + " for metallum 1 + ordo 1, original order " + order + ": " + recipe.aspects());
        AspectList full = brew("metallum", 1, "ordo", 1);
        for (String source : new String[] {ore, deepslate, raw}) {
            test.assertTrue(recipe.matches(new CrucibleRecipeInput(new ItemStack(item(source)), full), level), source + " purifies into " + cluster);
        }
        test.assertTrue(!recipe.matches(new CrucibleRecipeInput(new ItemStack(item(foreign)), full), level), foreign + " does not");
        test.assertTrue(!recipe.matches(new CrucibleRecipeInput(new ItemStack(item(cluster)), full), level), "a finished cluster is not purified again");
        test.assertTrue(!recipe.matches(new CrucibleRecipeInput(new ItemStack(item(ore)), brew("metallum", 1)), level), "without order it does not");
        test.assertTrue(!recipe.matches(new CrucibleRecipeInput(new ItemStack(item(ore)), brew("ordo", 1)), level), "without metal it does not");
    }

    private static void triple(GameTestHelper test, ServerLevel level, String path, String research, int order, String nugget, Map<String, Integer> cost,
                               AspectList full, AspectList thin, String foreign) {
        var recipe = crucible(level, path);
        test.assertTrue(recipe.research().equals(research) && recipe.result().is(item(nugget)) && recipe.result().getCount() == 3
                        && recipe.aspects().equals(cost) && recipe.sourceOrder() == order,
                path + ": " + research + ", three " + nugget + " for " + cost + ", original order " + order + ": " + recipe.aspects());
        test.assertTrue(recipe.matches(new CrucibleRecipeInput(new ItemStack(item(nugget)), full), level), "a nugget in a full brew makes three");
        test.assertTrue(!recipe.matches(new CrucibleRecipeInput(new ItemStack(item(nugget)), thin), level), "a thin brew does not");
        test.assertTrue(!recipe.matches(new CrucibleRecipeInput(new ItemStack(item(foreign)), full), level), foreign + " does not");
        test.assertTrue(!recipe.matches(new CrucibleRecipeInput(new ItemStack(item(nugget.replace("nugget", "ingot"))), full), level), "an ingot is not a nugget");
    }

    private static void aspects(GameTestHelper test) {
        // ConfigAspects:360-362 verbatim, the gold cluster's greed being lucrum.
        expect(test, "cluster_iron", brew("ordo", 1, "metallum", 6, "terra", 1));
        expect(test, "cluster_gold", brew("ordo", 1, "metallum", 4, "terra", 1, "lucrum", 2));
        expect(test, "cluster_cinnabar", brew("ordo", 1, "metallum", 4, "terra", 1, "permutatio", 4, "venenum", 2));
    }

    private static void expect(GameTestHelper test, String name, AspectList expected) {
        AspectList actual = ObjectAspectCatalog.get(id(name));
        test.assertTrue(ObjectAspectCatalog.registeredIds().contains(id(name)) && actual.aspects.equals(expected.aspects),
                name + " carries its original aspects: " + actual.aspects + " vs " + expected.aspects);
    }

    private static void furnaces(GameTestHelper test, ServerLevel level) {
        String[][] melts = {{"thaumcraft:cluster_iron", "minecraft:iron_ingot"}, {"thaumcraft:cluster_gold", "minecraft:gold_ingot"},
                {"thaumcraft:cluster_cinnabar", "thaumcraft:quick_silver"}};
        BlockPos base = test.absolutePos(new BlockPos(1, 2, 1));
        for (int index = 0; index < melts.length; ++index) {
            Item cluster = item(melts[index][0]);
            Item ingot = item(melts[index][1]);
            var input = new SingleRecipeInput(new ItemStack(cluster));
            var holder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).orElseThrow(() -> new AssertionError("no smelting recipe for " + cluster));
            var result = holder.value().assemble(input, level.registryAccess());
            test.assertTrue(result.is(ingot) && result.getCount() == 2 && holder.value().getExperience() == 1.0F && holder.value().getCookingTime() == 200,
                    "the original furnace gives two " + ingot + " for one " + cluster + " (XP 1.0, 200 ticks): " + result + " " + holder.value().getExperience());
            BlockPos pos = base.offset(index * 2, 0, 0);
            level.setBlock(pos, Blocks.FURNACE.defaultBlockState(), Block.UPDATE_ALL);
            var furnace = (FurnaceBlockEntity) level.getBlockEntity(pos);
            furnace.setItem(0, new ItemStack(cluster, 2));
            furnace.setItem(1, new ItemStack(Items.COAL, 4));
            AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
            test.assertTrue(level.getBlockState(pos).getValue(AbstractFurnaceBlock.LIT), "the furnace lights for " + cluster);
            for (int tick = 1; tick < 200; ++tick) AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
            test.assertTrue(furnace.getItem(2).is(ingot) && furnace.getItem(2).getCount() == 2 && furnace.getItem(0).getCount() == 1,
                    "a real furnace melts one " + cluster + " into two " + ingot + " in 200 ticks: " + furnace.getItem(2) + ", left " + furnace.getItem(0));
            furnace.clearContent();
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        test.succeed();
    }
}
