package thaumcraft.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.internal.WeightedRandomLoot;

/** The public API for adding to the three loot bag lists: no rarity means common, unknown rarities are ignored, the stack is kept as given. */
class LootBagRegistrationTest {
    private List<WeightedRandomLoot> common, uncommon, rare;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach void snapshot() {
        common = new ArrayList<>(WeightedRandomLoot.lootBagCommon);
        uncommon = new ArrayList<>(WeightedRandomLoot.lootBagUncommon);
        rare = new ArrayList<>(WeightedRandomLoot.lootBagRare);
    }

    @AfterEach void restore() {
        WeightedRandomLoot.lootBagCommon.clear();
        WeightedRandomLoot.lootBagCommon.addAll(common);
        WeightedRandomLoot.lootBagUncommon.clear();
        WeightedRandomLoot.lootBagUncommon.addAll(uncommon);
        WeightedRandomLoot.lootBagRare.clear();
        WeightedRandomLoot.lootBagRare.addAll(rare);
    }

    @Test void withoutARarityTheItemGoesToTheCommonBagOnly() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        ThaumcraftApi.addLootBagItem(stack, 7);
        assertEquals(common.size() + 1, WeightedRandomLoot.lootBagCommon.size());
        assertEquals(uncommon.size(), WeightedRandomLoot.lootBagUncommon.size());
        assertEquals(rare.size(), WeightedRandomLoot.lootBagRare.size());
        WeightedRandomLoot added = WeightedRandomLoot.lootBagCommon.get(WeightedRandomLoot.lootBagCommon.size() - 1);
        assertSame(stack, added.item, "the stack is kept, not copied");
        assertEquals(7, added.itemWeight);
    }

    @Test void eachListedRarityGetsItsOwnEntryAndUnknownRaritiesAreIgnored() {
        ItemStack stack = new ItemStack(Items.EMERALD);
        ThaumcraftApi.addLootBagItem(stack, 3, 0, 2, 7, -1);
        assertEquals(common.size() + 1, WeightedRandomLoot.lootBagCommon.size());
        assertEquals(uncommon.size(), WeightedRandomLoot.lootBagUncommon.size());
        assertEquals(rare.size() + 1, WeightedRandomLoot.lootBagRare.size());
    }

    @Test void theUncommonBagIsRarityOne() {
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLD_INGOT), 5, 1);
        assertEquals(common.size(), WeightedRandomLoot.lootBagCommon.size());
        assertEquals(uncommon.size() + 1, WeightedRandomLoot.lootBagUncommon.size());
        assertEquals(rare.size(), WeightedRandomLoot.lootBagRare.size());
    }

    @Test void theSameItemInSeveralBagsGetsSeparateEntries() {
        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        ThaumcraftApi.addLootBagItem(stack, 2, 0, 1, 2);
        WeightedRandomLoot inCommon = WeightedRandomLoot.lootBagCommon.get(WeightedRandomLoot.lootBagCommon.size() - 1);
        WeightedRandomLoot inUncommon = WeightedRandomLoot.lootBagUncommon.get(WeightedRandomLoot.lootBagUncommon.size() - 1);
        WeightedRandomLoot inRare = WeightedRandomLoot.lootBagRare.get(WeightedRandomLoot.lootBagRare.size() - 1);
        assertEquals(false, inCommon == inUncommon || inUncommon == inRare || inCommon == inRare, "changing the weight in one bag must not change another");
        inCommon.itemWeight = 99;
        assertEquals(2, inUncommon.itemWeight);
        assertEquals(2, inRare.itemWeight);
    }

    @Test void aNullRarityListMeansCommonToo() {
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.COAL), 1, (int[]) null);
        assertEquals(common.size() + 1, WeightedRandomLoot.lootBagCommon.size());
    }
}
