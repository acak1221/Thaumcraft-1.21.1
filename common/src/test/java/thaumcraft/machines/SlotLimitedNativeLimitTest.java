package thaumcraft.machines;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.common.container.SlotLimitedByClass;

class SlotLimitedNativeLimitTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void twoOrdinaryInsertionsCannotMergeNonstackableItems() {
        // Like the actual research/focal tiles, preserve a supplied whole stack: the SLOT must enforce insertion limits.
        var backing = new SimpleContainer(1) {
            @Override public void setItem(int index, ItemStack stack) { getItems().set(index, stack); setChanged(); }
        };
        var slot = new SlotLimitedByClass(Item.class, backing, 0, 0, 0);
        ItemStack first = new ItemStack(Items.BOW), second = new ItemStack(Items.BOW);
        slot.safeInsert(first); slot.safeInsert(second);
        assertTrue(first.isEmpty());
        assertEquals(1, second.getCount(), "a separate item remains owned by the cursor");
        assertEquals(1, slot.getItem().getCount(), "the physical input never becomes an invalid stack of two");
    }

    @Test void itemAndSlotLimitsBothApplyWithoutChangingRetainedStock() {
        var backing = new SimpleContainer(1);
        var small = new SlotLimitedByClass(Item.class, 3, backing, 0, 0, 0);
        var supplied = new ItemStack(Items.STONE, 8);
        small.safeInsert(supplied);
        assertEquals(3, small.getItem().getCount()); assertEquals(5, supplied.getCount());
        assertEquals(64, new SlotLimitedByClass(Item.class, 256, backing, 0, 0, 0).getMaxStackSize(new ItemStack(Items.STONE)));
        // Old copied research-note counts are handled by their explicit whole-stack transfer path, not clamped by this query.
        backing.getItems().set(0, new ItemStack(Items.BOW, 2));
        assertEquals(1, small.getMaxStackSize(backing.getItem(0)));
        assertEquals(2, backing.getItem(0).getCount(), "asking for an insertion limit never truncates recovered existing stock");
    }
}
