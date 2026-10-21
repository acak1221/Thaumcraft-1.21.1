package thaumcraft.common.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The ghost slot clicks of the original TC4 container.
 *
 * <p>Oracle {@code thaumcraft/common/container/ContainerGhostSlots.java} and
 * {@code thaumcraft/common/container/SlotGhost.java}: a ghost slot is a recipe preview, it never owns
 * stock and the player never takes it back ({@code SlotGhost.canTakeStack} is false).</p>
 */
class GhostSlotMenuTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** The menu of the test: one ghost slot and no player inventory. */
    private static final class GhostMenu extends ContainerGhostSlots {
        GhostMenu(SimpleContainer ghosts) { super(null, 0, null, 1, ghosts); }
        @Override protected void bindPlayerInventory(Inventory inventory) { /* the test has no player */ }
    }

    private static SimpleContainer ghosts() {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.STONE, 8));
        return container;
    }

    /**
     * Oracle {@code ContainerGhostSlots:110-117}: the creative clone copies the stack of the slot into a
     * full stack, but only a real slot hands it to the cursor. A ghost slot keeps its preview.
     */
    @Test void theCreativeCloneLeavesThePreviewOfAGhostSlotAlone() {
        SimpleContainer ghosts = ghosts();
        GhostMenu menu = new GhostMenu(ghosts);
        menu.clicked(0, 0, ClickType.CLONE, null);
        assertEquals(8, ghosts.getItem(0).getCount(), "the original never changes the preview of a ghost slot");
        assertTrue(menu.getCarried().isEmpty(), "the original never hands a ghost stack to the player");
    }

    /**
     * Oracle {@code ContainerGhostSlots:47-56}: the quick move of a ghost slot clears it with the left
     * button and adds sixteen items with the right button, never more than the limit of the slot.
     */
    @Test void theQuickMoveClearsOrFillsThePreview() {
        SimpleContainer ghosts = ghosts();
        GhostMenu menu = new GhostMenu(ghosts);
        menu.clicked(0, 1, ClickType.QUICK_MOVE, null);
        assertEquals(24, ghosts.getItem(0).getCount(), "the right button adds sixteen items");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, null);
        assertTrue(ghosts.getItem(0).isEmpty(), "the left button clears the preview");
    }

    /**
     * Oracle {@code ContainerGhostSlots:44-60} with the hotbar swap of 1.7.10: the swap needs
     * {@code canTakeStack}, and the ghost slot refuses it ({@code SlotGhost.canTakeStack} is false), so the
     * preview of a ghost slot survives a hotbar swap.
     */
    @Test void theHotbarSwapKeepsThePreviewOfAGhostSlot() {
        SimpleContainer ghosts = ghosts();
        GhostMenu menu = new GhostMenu(ghosts);
        menu.clicked(0, 0, ClickType.SWAP, null);
        assertEquals(8, ghosts.getItem(0).getCount(), "a ghost slot is never taken out by the player");
    }

    /**
     * Oracle {@code ContainerGhostSlots:122-130}: the throw removes one item of the preview with the left
     * button and the whole preview with the right button; the items are never dropped.
     */
    @Test void theThrowRemovesOneItemOrTheWholePreview() {
        SimpleContainer ghosts = ghosts();
        GhostMenu menu = new GhostMenu(ghosts);
        menu.clicked(0, 0, ClickType.THROW, null);
        assertEquals(7, ghosts.getItem(0).getCount());
        menu.clicked(0, 1, ClickType.THROW, null);
        assertTrue(ghosts.getItem(0).isEmpty());
        assertTrue(menu.getCarried().isEmpty(), "a thrown ghost item is never given to the player");
    }

    /**
     * Oracle {@code ContainerGhostSlots:59-95}: a held stack fills an empty preview, and a different held
     * item replaces the preview with a copy of the whole held stack.
     */
    @Test void aHeldStackFillsAnEmptyPreviewAndReplacesTheItemOfAPreview() {
        SimpleContainer ghosts = new SimpleContainer(1);
        GhostMenu menu = new GhostMenu(ghosts);
        menu.setCarried(new ItemStack(Items.DIRT, 5));
        menu.clicked(0, 0, ClickType.PICKUP, null);
        assertEquals(Items.DIRT, ghosts.getItem(0).getItem());
        assertEquals(5, ghosts.getItem(0).getCount());
        menu.setCarried(new ItemStack(Items.SAND, 3));
        menu.clicked(0, 0, ClickType.PICKUP, null);
        assertEquals(Items.SAND, ghosts.getItem(0).getItem());
        assertEquals(3, ghosts.getItem(0).getCount(), "a different item replaces the preview with the whole held stack");
    }
}
