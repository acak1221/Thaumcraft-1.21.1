package thaumcraft.common.lib.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.WorldGenLevel;
import thaumcraft.common.config.ThaumcraftLoot;

/**
 * The loot of the chests of the original.
 *
 * <p>The original fills the chests of its structures with the Forge 1.7.10 chest category
 * {@code "dungeonChest"} (oracle {@code WorldGenHilltopStones:86-93},
 * {@code WorldGenMound:2518-2536} and {@code WorldGenGreatwoodTrees:369-372}), and the category
 * is the list of the vanilla 1.7.10 dungeon chest plus the additions of Forge:</p>
 *
 * <pre>
 * ChestGenHooks (Forge 1.7.10, net/minecraftforge/common/ChestGenHooks.java)
 *   addInfo(DUNGEON_CHEST, WorldGenDungeons.field_111189_a, 8, 8);
 *   getInfo(DUNGEON_CHEST).addItem(new WeightedRandomChestContent(enchanted_book, 1, 1, 1));
 * </pre>
 *
 * <p>The list of the vanilla dungeon chest is the static array of
 * {@code net.minecraft.world.gen.feature.WorldGenDungeons} (the 1.7.10 client jar,
 * {@code asd.class}): the saddle, the iron ingot, the bread, the wheat, the gunpowder, the
 * string, the bucket, the golden apple, the redstone, the two music discs, the name tag and
 * the three horse armours. The port carries that list in
 * {@link ThaumcraftLoot#dungeonEntries}, with the TC4 additions of the original.</p>
 *
 * <p>Every roll of the original is
 * {@code WeightedRandomChestContent.fillChestWithLoot(rand, info.getItems(rand), chest,
 * info.getCount(rand))}: the entries are generated again for every roll (the enchanted book
 * is rolled again), the count of the category is eight (the minimum and the maximum of the
 * category are both eight, so the count consumes no random draw), every item is drawn with
 * the weight of its entry, its amount is drawn between the minimum and the maximum of the
 * entry and it is written into a random slot of the chest, so a second roll overwrites the
 * slots of the first one.</p>
 */
final class WorldgenLootHelper {

    private WorldgenLootHelper() {
    }

    /** The count of the Forge category "dungeonChest": the minimum and the maximum are both eight. */
    static final int DUNGEON_CHEST_COUNT = 8;

    /**
     * One roll of the dungeon chest of the original.
     *
     * <p>Oracle {@code WorldGenMound:2533}: the entries are generated again for this roll, the
     * count is the count of the category and every item of the roll is written into a random
     * slot of the chest.</p>
     */
    static void fillDungeonChest(WorldGenLevel level, ChestBlockEntity chest, RandomSource random) {
        List<ThaumcraftLoot.DungeonEntry> entries = ThaumcraftLoot.dungeonEntries(level, random);
        int total = 0;
        for (ThaumcraftLoot.DungeonEntry entry : entries) total = Math.addExact(total, entry.weight());
        for (int roll = 0; roll < DUNGEON_CHEST_COUNT; roll++) {
            int ticket = random.nextInt(total);
            ThaumcraftLoot.DungeonEntry chosen = null;
            for (ThaumcraftLoot.DungeonEntry entry : entries) {
                ticket -= entry.weight();
                if (ticket < 0) {
                    chosen = entry;
                    break;
                }
            }
            if (chosen == null) throw new IllegalStateException("Original dungeon descriptor weights are inconsistent");
            int count = chosen.minCount() + random.nextInt(chosen.maxCount() - chosen.minCount() + 1);
            ItemStack stack = chosen.baseStack();
            if (stack.getMaxStackSize() >= count) {
                stack.setCount(count);
                chest.setItem(random.nextInt(chest.getContainerSize()), stack);
            } else {
                for (int i = 0; i < count; i++) {
                    ItemStack single = stack.copy();
                    single.setCount(1);
                    chest.setItem(random.nextInt(chest.getContainerSize()), single);
                }
            }
        }
    }

    /**
     * The chest of a structure of the original, rolled the given number of times.
     *
     * <p>Oracle {@code WorldGenHilltopStones:86-93}: the chest of the hilltop stones is rolled
     * twice, and the second roll overwrites the slots of the first one.</p>
     */
    static void fillDungeonChest(WorldGenLevel level, BlockPos pos, RandomSource random, int rolls) {
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) return;
        for (int roll = 0; roll < rolls; roll++) fillDungeonChest(level, chest, random);
    }
}
