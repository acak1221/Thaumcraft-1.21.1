package thaumcraft.eldritch;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.world.EldritchBlockRules;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The original magic box sort (oracle TileMagicBox:201-226): two neighbouring slots are only
 * merged when they hold the same item with the same tags, the original guarded the merge with
 * {@code isItemEqual} and {@code areItemStackTagsEqual}.
 */
class MagicBoxSortRulesTest {
    @Test void twoStacksOfTheSameItemAreMerged() {
        ItemStack first = new ItemStack(Items.DIRT, 10);
        ItemStack second = new ItemStack(Items.DIRT, 5);
        assertTrue(EldritchBlockRules.magicBoxMerges(first, second));
    }

    @Test void twoStacksOfDifferentItemsAreNeverMerged() {
        ItemStack first = new ItemStack(Items.DIRT, 10);
        ItemStack second = new ItemStack(Items.STONE, 10);
        assertFalse(EldritchBlockRules.magicBoxMerges(first, second));
    }

    @Test void twoStacksWithDifferentTagsAreNeverMerged() {
        ItemStack first = new ItemStack(Items.DIRT, 10);
        ItemStack named = new ItemStack(Items.DIRT, 10);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("special"));
        assertFalse(EldritchBlockRules.magicBoxMerges(first, named));
        assertTrue(EldritchBlockRules.magicBoxMerges(first, new ItemStack(Items.DIRT, 10)));
    }

    @Test void aFullSlotTakesNoPartInAMerge() {
        assertTrue(EldritchBlockRules.magicBoxSlotHasRoom(new ItemStack(Items.DIRT, 63)));
        assertFalse(EldritchBlockRules.magicBoxSlotHasRoom(new ItemStack(Items.DIRT, 64)));
        assertEquals(64, EldritchBlockRules.MAGIC_BOX_MAX_STACK);
    }
}
