package thaumcraft.common.testing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.tiles.TileArcaneFurnace;

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
        furnacePaysTheBonus(test);
        test.succeed();
    }

    /**
     * The arcane furnace pays the bonus of the item it ejects.
     *
     * <p>Oracle {@code thaumcraft/common/tiles/TileArcaneFurnace.java:237-259}: the furnace asks the api for
     * the bonus of the smelted item and pays one unit with a chance of one in four without a bellows, one
     * roll per bellows with a chance of 44 % after that. The bonus is paid as a second item entity next to
     * the smelted result.</p>
     *
     * <p>The check calls the ejection of the tile directly, because the payment is the behaviour under test:
     * the furnace itself is a world structure of three by three by three blocks and its cooking needs the
     * aspect machinery. The chance of one in four is paid out over many smelts, so the check asks for a
     * payout that the original could not refuse.</p>
     */
    private static void furnacePaysTheBonus(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        BlockPos pos = test.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(pos, ThaumcraftLegacyTiles.ARCANE_FURNACE.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity entity = level.getBlockEntity(pos);
        test.assertTrue(entity instanceof TileArcaneFurnace, "the arcane furnace core has its tile");
        if (!(entity instanceof TileArcaneFurnace furnace)) return;
        furnace.getFacing(level);
        ItemStack input = new ItemStack(Items.IRON_ORE);
        ItemStack result = new ItemStack(Items.IRON_INGOT);
        int smelts = 64;
        for (int smelt = 0; smelt < smelts; ++smelt) furnace.ejectItem(level, result.copy(), input);
        int nuggets = 0;
        for (ItemEntity dropped : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3))) {
            if (dropped.getItem().is(Items.IRON_NUGGET)) nuggets += dropped.getItem().getCount();
        }
        test.assertTrue(nuggets > 0,
                "the arcane furnace pays the original smelting bonus of the iron ore: " + nuggets + " nuggets of " + smelts + " smelts");
        test.assertTrue(nuggets <= smelts,
                "the original pays at most one bonus unit per smelt without a bellows: " + nuggets + " of " + smelts);
    }

    private static void bonus(GameTestHelper test, String inputId, String outputId, String label) {
        ItemStack input = new ItemStack(item(inputId));
        ItemStack bonus = ThaumcraftApi.getSmeltingBonus(input);
        test.assertTrue(bonus != null && bonus.is(item(outputId)),
                label + " pays the original bonus " + outputId + ": " + bonus);
    }
}
