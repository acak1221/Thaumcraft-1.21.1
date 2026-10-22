package thaumcraft.common.lib.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import thaumcraft.common.config.ThaumcraftLoot;

/**
 * The loot of the chests of the original (TC4 4.2.3.5) against the 1.21.1 port.
 *
 * <p>The original fills the chests of its structures with the Forge 1.7.10 chest category
 * {@code "dungeonChest"} (oracle {@code WorldGenHilltopStones:86-93},
 * {@code WorldGenMound:2518-2536} and {@code WorldGenGreatwoodTrees:369-372}). The category is
 * the list of the vanilla 1.7.10 dungeon chest plus the additions of Forge, and the count of
 * the category is eight.</p>
 *
 * <p>No block and no world is constructed here: the list of the entries is a pure function of
 * the item registry.</p>
 */
class DungeonChestLootParityTest {

    private static List<ThaumcraftLoot.DungeonEntry> vanillaEntries() {
        List<ThaumcraftLoot.DungeonEntry> entries = new ArrayList<>();
        ThaumcraftLoot.addVanillaDungeonEntries(entries);
        return entries;
    }

    private static void assertEntry(List<ThaumcraftLoot.DungeonEntry> entries, int index, Item item,
            int minimum, int maximum, int weight) {
        ThaumcraftLoot.DungeonEntry entry = entries.get(index);
        assertTrue(entry.baseStack().is(item), "the entry " + index + " is the " + item);
        assertEquals(minimum, entry.minCount(), "the minimum of the entry " + index);
        assertEquals(maximum, entry.maxCount(), "the maximum of the entry " + index);
        assertEquals(weight, entry.weight(), "the weight of the entry " + index);
    }

    /**
     * The list of the vanilla 1.7.10 dungeon chest (the static array of
     * {@code net.minecraft.world.gen.feature.WorldGenDungeons}, read from the official 1.7.10
     * client jar, the class {@code asd}): the saddle, the iron ingot, the bread, the wheat, the
     * gunpowder, the string, the bucket, the golden apple, the redstone, the two music discs,
     * the name tag and the three horse armours, with their amounts and their weights.
     */
    @Test void theDungeonChestListIsTheVanillaOneOfTheOriginal() {
        List<ThaumcraftLoot.DungeonEntry> entries = vanillaEntries();
        assertEquals(15, entries.size(), "the fifteen entries of the vanilla dungeon chest");
        assertEntry(entries, 0, Items.SADDLE, 1, 1, 10);
        assertEntry(entries, 1, Items.IRON_INGOT, 1, 4, 10);
        assertEntry(entries, 2, Items.BREAD, 1, 1, 10);
        assertEntry(entries, 3, Items.WHEAT, 1, 4, 10);
        assertEntry(entries, 4, Items.GUNPOWDER, 1, 4, 10);
        assertEntry(entries, 5, Items.STRING, 1, 4, 10);
        assertEntry(entries, 6, Items.BUCKET, 1, 1, 10);
        assertEntry(entries, 7, Items.GOLDEN_APPLE, 1, 1, 1);
        assertEntry(entries, 8, Items.REDSTONE, 1, 4, 10);
        assertEntry(entries, 9, Items.MUSIC_DISC_13, 1, 1, 10);
        assertEntry(entries, 10, Items.MUSIC_DISC_CAT, 1, 1, 10);
        assertEntry(entries, 11, Items.NAME_TAG, 1, 1, 10);
        assertEntry(entries, 12, Items.GOLDEN_HORSE_ARMOR, 1, 1, 2);
        assertEntry(entries, 13, Items.IRON_HORSE_ARMOR, 1, 1, 5);
        assertEntry(entries, 14, Items.DIAMOND_HORSE_ARMOR, 1, 1, 1);
    }

    /**
     * The Forge 1.7.10 category is registered with the minimum and the maximum of eight
     * ({@code addInfo(DUNGEON_CHEST, WorldGenDungeons.field_111189_a, 8, 8)}), so one roll of
     * the chest writes eight items and the count consumes no random draw.
     */
    @Test void oneRollOfTheDungeonChestWritesEightItems() {
        assertEquals(8, WorldgenLootHelper.DUNGEON_CHEST_COUNT);
    }
}
