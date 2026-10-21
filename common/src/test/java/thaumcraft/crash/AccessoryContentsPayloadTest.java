package thaumcraft.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import thaumcraft.common.lib.network.AccessoryContentsPayload;

/**
 * The accessory snapshot of the original accessory slots.
 *
 * <p>The four worn slots are the whole payload, so a snapshot of another size is refused instead of
 * being applied to the worn inventory of a player.</p>
 */
class AccessoryContentsPayloadTest {

    @Test void aSnapshotNeedsExactlyFourSlots() {
        List<ItemStack> four = List.of(new ItemStack(Items.AIR), new ItemStack(Items.AIR),
                new ItemStack(Items.AIR), new ItemStack(Items.AIR));
        assertEquals(4, new AccessoryContentsPayload(four).stacks().size());
        assertThrows(IllegalArgumentException.class,
                () -> new AccessoryContentsPayload(List.of(new ItemStack(Items.AIR))));
    }

    @Test void aSnapshotCopiesItsStacks() {
        ItemStack stack = new ItemStack(Items.LEATHER);
        AccessoryContentsPayload payload = new AccessoryContentsPayload(
                List.of(stack, new ItemStack(Items.AIR), new ItemStack(Items.AIR), new ItemStack(Items.AIR)));
        assertNotSame(stack, payload.stacks().get(0), "the snapshot never aliases the caller's stack");
    }
}
