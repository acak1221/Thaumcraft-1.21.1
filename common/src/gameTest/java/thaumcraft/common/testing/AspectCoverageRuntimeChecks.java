package thaumcraft.common.testing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ResearchSettings;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/**
 * The remaining original ConfigAspects rows, the copy rows of the early resource, and the port's own
 * aspect inference: what it fills, and that switching it off gives back exactly the TC4 rules.
 */
public final class AspectCoverageRuntimeChecks {
    /** Vanilla items that stay without aspects on purpose: operator and debug tools, technical blocks. */
    private static final Set<Item> TECHNICAL = Set.of(Items.COMMAND_BLOCK, Items.REPEATING_COMMAND_BLOCK, Items.CHAIN_COMMAND_BLOCK,
            Items.COMMAND_BLOCK_MINECART, Items.BARRIER, Items.LIGHT, Items.STRUCTURE_VOID, Items.STRUCTURE_BLOCK, Items.JIGSAW,
            Items.KNOWLEDGE_BOOK, Items.DEBUG_STICK);

    private AspectCoverageRuntimeChecks() {}

    public static void run(GameTestHelper test) {
        Level level = test.getLevel();
        ResearchSettings before = ThaumcraftSettings.server();
        try {
            ThaumcraftSettings.configureServer(withInference(before, true));
            // The original rows, whatever the setting: ConfigAspects bytecode values.
            same(test, level, Items.PODZOL, new AspectList().add(Aspect.EARTH, 1).add(Aspect.PLANT, 1), "dirt metadata 2 is podzol");
            same(test, level, Items.COARSE_DIRT, new AspectList().add(Aspect.EARTH, 2), "dirt wildcard, not the legacy terra1");
            same(test, level, Items.OAK_LEAVES, new AspectList().add(Aspect.PLANT, 1), "treeLeaves, not the legacy herba2 aer1");
            same(test, level, Items.PUMPKIN, new AspectList().add(Aspect.CROP, 2), "pumpkin messis2");
            same(test, level, Items.MELON_SLICE, new AspectList().add(Aspect.HUNGER, 1), "the 1.7.10 melon item is the slice");
            same(test, level, Items.COOKED_COD, new AspectList().add(Aspect.CRAFT, 1).add(Aspect.FLESH, 4).add(Aspect.HUNGER, 3), "cooked fish");
            same(test, level, Items.SALMON, new AspectList().add(Aspect.FLESH, 3).add(Aspect.LIFE, 1).add(Aspect.WATER, 1), "fish metadata 1");
            same(test, level, Items.BONE_MEAL, new AspectList().add(Aspect.SENSES, 1), "dyeWhite of the dye loop");
            same(test, level, Items.MOSSY_STONE_BRICKS, new AspectList().add(Aspect.EARTH, 1).add(Aspect.PLANT, 1), "copy of stonebrick, minus terra1, plus herba1");
            same(test, level, Items.CUT_SANDSTONE, new AspectList().add(Aspect.EARTH, 2).add(Aspect.ENTROPY, 3).add(Aspect.ORDER, 1), "sandstone metadata 2");
            same(test, level, Items.CHIPPED_ANVIL, new AspectList().add(Aspect.METAL, 64).add(Aspect.CRAFT, 2).add(Aspect.TOOL, 2), "anvil int[] {0, 1, 2}");
            same(test, level, Items.WATER_BUCKET, new AspectList().add(Aspect.METAL, 8).add(Aspect.VOID, 1).add(Aspect.WATER, 4), "copy of the bucket");
            same(test, level, Items.COPPER_INGOT, new AspectList().add(Aspect.METAL, 3).add(Aspect.EXCHANGE, 1), "ingotCopper");
            check(test, ObjectAspectResolver.baseTags(new ItemStack(Items.ENCHANTED_BOOK), level).aspects
                    .equals(ObjectAspectResolver.baseTags(new ItemStack(Items.BOOK), level).aspects), "the enchanted book adds the book's tags");
            AspectList table = ObjectAspectResolver.baseTags(new ItemStack(ThaumcraftContent.TABLE_ITEM.get()), level);
            check(test, table.getAmount(Aspect.TOOL) >= 1, "the table is generated from its recipe plus instrumentum1: " + table.aspects);
            check(test, ObjectAspectResolver.baseTags(new ItemStack(ThaumcraftContent.RESEARCH_TABLE_ITEM.get()), level).aspects
                    .equals(table.copy().add(Aspect.MIND, 4).aspects), "the research table copies the table and adds cognitio4");
            AspectList chest = ObjectAspectResolver.baseTags(new ItemStack(Items.CHEST), level);
            check(test, chest.getAmount(Aspect.VOID) == 4 && chest.getAmount(Aspect.TREE) > 0, "the chest complex row adds vacuos4 to its recipe: " + chest.aspects);

            // The inference: tag families, weathering, forms, recipes the original did not read, spawn eggs.
            same(test, level, Items.DEEPSLATE_IRON_ORE, new AspectList().add(Aspect.EARTH, 1).add(Aspect.METAL, 3), "oreIron family");
            same(test, level, Items.CHERRY_PLANKS, new AspectList().add(Aspect.TREE, 1), "plankWood family");
            same(test, level, Items.BLACK_DYE, new AspectList().add(Aspect.SENSES, 1), "dye family");
            same(test, level, Items.TUFF, new AspectList().add(Aspect.EARTH, 2), "stone family");
            check(test, ObjectAspectResolver.baseTags(new ItemStack(Items.OXIDIZED_COPPER), level).aspects
                    .equals(ObjectAspectResolver.baseTags(new ItemStack(Items.COPPER_BLOCK), level).aspects), "oxidized copper is copper");
            // The waxed block has a crafting recipe (the block and a honeycomb), so TC4 derivation gives it.
            check(test, ObjectAspectResolver.baseTags(new ItemStack(Items.WAXED_OXIDIZED_COPPER), level).getAmount(Aspect.METAL) > 0,
                    "waxed oxidized copper derives from oxidized copper");
            check(test, ObjectAspectResolver.baseTags(new ItemStack(Items.MUTTON), level).aspects
                    .equals(ObjectAspectResolver.baseTags(new ItemStack(Items.PORKCHOP), level).aspects), "mutton is a form of the porkchop");
            check(test, ObjectAspectResolver.baseTags(new ItemStack(Items.SPRUCE_TRAPDOOR), level).getAmount(Aspect.MOTION) == 1,
                    "the trapdoor family adds the original motus1 to the spruce trapdoor");
            check(test, ObjectAspectResolver.baseTags(new ItemStack(Items.NETHERITE_SCRAP), level).getAmount(Aspect.METAL) > 0,
                    "netherite scrap cooks from ancient debris");
            check(test, ObjectAspectResolver.baseTags(new ItemStack(Items.RAW_IRON), level).getAmount(Aspect.METAL) == 3,
                    "raw iron takes three quarters of the iron ingot it smelts into");
            check(test, ObjectAspectResolver.baseTags(new ItemStack(Items.NETHERITE_PICKAXE), level).size() > 0, "smithing");
            check(test, ObjectAspectResolver.baseTags(new ItemStack(Items.ZOMBIE_SPAWN_EGG), level).size() > 0, "a spawn egg takes its creature");

            List<String> empty = new ArrayList<>();
            List<String> report = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) continue;
                AspectList tags = ObjectAspectResolver.get(new ItemStack(item), level);
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                if (tags.size() == 0) empty.add(id);
                StringBuilder line = new StringBuilder(id);
                for (Aspect aspect : tags.getAspectsSorted()) line.append(' ').append(aspect.getTag()).append(tags.getAmount(aspect));
                report.add(line.toString());
                if (tags.size() == 0 && id.startsWith("minecraft:") && !TECHNICAL.contains(item) && !(item instanceof SpawnEggItem)) {
                    test.fail("a vanilla item has no aspects with the inference on: " + id);
                }
            }
            String out = System.getProperty("thaumcraft.aspectCoverage");
            if (out != null) {
                Files.createDirectories(Path.of(out));
                Files.write(Path.of(out, "aspects.txt"), report);
                Files.write(Path.of(out, "without.txt"), empty);
            }

            // Off: the TC4 resolver exactly. The original rows stay, the inferred values go.
            ThaumcraftSettings.configureServer(withInference(before, false));
            same(test, level, Items.PODZOL, new AspectList().add(Aspect.EARTH, 1).add(Aspect.PLANT, 1), "an original row does not depend on the setting");
            for (Item item : List.of(Items.DEEPSLATE_IRON_ORE, Items.MUTTON, Items.OXIDIZED_COPPER, Items.RAW_IRON, Items.ZOMBIE_SPAWN_EGG)) {
                check(test, ObjectAspectResolver.baseTags(new ItemStack(item), level).size() == 0,
                        "without the inference TC4 gives " + BuiltInRegistries.ITEM.getKey(item) + " nothing");
            }
            test.succeed();
        } catch (java.io.IOException failure) {
            throw new RuntimeException(failure);
        } finally {
            ThaumcraftSettings.configureServer(before);
        }
    }

    private static ResearchSettings withInference(ResearchSettings s, boolean on) {
        return new ResearchSettings(s.cheatSheet(), s.wardedStone(), s.mirrors(), s.hardNodes(), s.wuss(), s.difficulty(), s.aspectCap(), on);
    }

    private static void same(GameTestHelper test, Level level, Item item, AspectList expected, String why) {
        AspectList actual = ObjectAspectResolver.baseTags(new ItemStack(item), level);
        check(test, actual.aspects.equals(expected.aspects), why + ": " + BuiltInRegistries.ITEM.getKey(item) + " " + actual.aspects);
    }

    private static void check(GameTestHelper test, boolean condition, String message) {
        if (!condition) test.fail(message);
    }
}
