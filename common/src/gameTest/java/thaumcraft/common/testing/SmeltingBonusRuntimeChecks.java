package thaumcraft.common.testing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.ThaumcraftApi;

/**
 * The original TC4 4.2.3.5 smelting bonus catalogue on a real server.
 *
 * <p>The original registers eighteen bonus entries in {@code ConfigRecipes.initializeSmelting} (oracle
 * {@code thaumcraft/common/config/ConfigRecipes.java:493-510}) and the ARCANE FURNACE pays them for every
 * item it ejects (oracle {@code thaumcraft/common/tiles/TileArcaneFurnace.java:237-259}): one unit with a
 * chance of one in four without a bellows, one roll per bellows with a chance of 44 % after that. The port
 * had neither the catalogue nor the lookup, so the arcane furnace never paid a bonus at all.</p>
 *
 * <p>The check reads the registry of the running server: the data pack tags of the original ore names are
 * bound there, which the bare unit test cannot do. The input identities and the expected bonus come from
 * the original registrations, never from the port.</p>
 */
public final class SmeltingBonusRuntimeChecks {
    private SmeltingBonusRuntimeChecks() {}

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    public static void run(GameTestHelper test) {
        // The four ore-name registrations of oracle ConfigRecipes:493-496. The original
        // resolves them through the Forge ore dictionary; 1.21.1 resolves them through the
        // native tags of the original ore names.
        bonus(test, "minecraft:iron_ore", "minecraft:iron_nugget", "oreIron");
        bonus(test, "minecraft:gold_ore", "minecraft:gold_nugget", "oreGold");
        bonus(test, "minecraft:copper_ore", "thaumcraft:copper_nugget", "oreCopper");
        bonus(test, "thaumcraft:cinnabar_ore", "thaumcraft:quicksilver_drop", "oreCinnabar");
        // The four cluster registrations of oracle ConfigRecipes:500-503.
        bonus(test, "thaumcraft:cluster_iron", "minecraft:iron_nugget", "the iron cluster");
        bonus(test, "thaumcraft:cluster_gold", "minecraft:gold_nugget", "the gold cluster");
        bonus(test, "thaumcraft:cluster_copper", "thaumcraft:copper_nugget", "the copper cluster");
        bonus(test, "thaumcraft:cluster_cinnabar", "thaumcraft:quicksilver_drop", "the cinnabar cluster");
        // The four raw meat registrations of oracle ConfigRecipes:507-510. The raw fish
        // entry carries the wildcard metadata of the original Items.fish.
        bonus(test, "minecraft:chicken", "thaumcraft:nugget_chicken", "raw chicken");
        bonus(test, "minecraft:beef", "thaumcraft:nugget_beef", "raw beef");
        bonus(test, "minecraft:porkchop", "thaumcraft:nugget_pork", "raw pork");
        for (String fish : new String[] {"minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:pufferfish"}) {
            bonus(test, fish, "thaumcraft:nugget_fish", "raw fish");
        }
        // The original has no entry for raw iron: 1.7.10 had no raw iron at all.
        ItemStack rawIron = new ItemStack(Items.RAW_IRON);
        test.assertTrue(ThaumcraftApi.getSmeltingBonus(rawIron) == null,
                "the original registers no bonus for raw iron: " + ThaumcraftApi.getSmeltingBonus(rawIron));
        test.succeed();
    }

    private static void bonus(GameTestHelper test, String inputId, String outputId, String label) {
        ItemStack input = new ItemStack(item(inputId));
        ItemStack bonus = ThaumcraftApi.getSmeltingBonus(input);
        test.assertTrue(bonus != null && bonus.is(item(outputId)),
                label + " pays the original bonus " + outputId + ": " + bonus);
    }
}
