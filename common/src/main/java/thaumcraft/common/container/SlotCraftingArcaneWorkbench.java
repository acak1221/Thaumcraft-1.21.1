package thaumcraft.common.container;

import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.crafting.ArcaneVis;

/**
 * Retained compatibility implementation of the old output slot.
 *
 * <p>The native workbench uses the server-atomic result transaction in
 * {@link ContainerArcaneWorkbench}; it does not instantiate this class. New
 * integrations must use that menu rather than extract a result before payment.</p>
 *
 * @deprecated retained for source compatibility; not the active native result slot
 */
@Deprecated(forRemoval = false)
public class SlotCraftingArcaneWorkbench extends Slot {
    /** Original ContainerArcaneWorkbench:57 - the wand lives beside the matrix. */
    public static final int WAND_SLOT = 10;
    private final Player thePlayer;
    private final Container craftMatrix;
    private int amountCrafted;

    public SlotCraftingArcaneWorkbench(Player player, Container craftMatrix, Container result, int index, int x, int y) {
        super(result, index, x, y);
        this.thePlayer = player;
        this.craftMatrix = craftMatrix;
    }

    /** An output slot never accepts a placed item. */
    @Override public boolean mayPlace(ItemStack stack) { return false; }

    @Override public ItemStack remove(int amount) {
        ItemStack stack = getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        amountCrafted += Math.min(amount, stack.getCount());
        ItemStack taken = stack.copyWithCount(Math.min(amount, stack.getCount()));
        stack.shrink(taken.getCount());
        if (stack.isEmpty()) set(ItemStack.EMPTY);
        setChanged();
        return taken;
    }

    /**
     * Original SlotCraftingArcaneWorkbench:36-63. The 1.7 slot consumed the
     * matrix by item type and then again cell by cell; the modern recipe
     * remainders already model that consumption exactly, so the double
     * decrement of the original is not reproduced.
     */
    @Override public void onTake(Player player, ItemStack stack) {
        if (amountCrafted > 0) {
            stack.onCraftedBy(player.level(), player, amountCrafted);
            amountCrafted = 0;
        }
        // Original SlotCraftingArcaneWorkbench:38 - the event sees the matrix
        // that produced the result.
        PlayerEvent.CRAFT_ITEM.invoker().craft(player, stack, craftMatrix);
        ArcaneCrafting.Match match = ArcaneCrafting.find(player, craftMatrix);
        if (match != null && match.arcane() && !match.prices().isEmpty()) {
            ItemStack wand = craftMatrix.getItem(WAND_SLOT);
            if (!wand.isEmpty()) ArcaneVis.debit(wand, match.prices());
        }
        // Original SlotCraftingArcaneWorkbench:49-63 - one item leaves every
        // filled matrix cell and its container item returns to the player.
        for (int slot = 0; slot < 9; slot++) {
            ItemStack cell = craftMatrix.getItem(slot);
            if (cell.isEmpty()) continue;
            craftMatrix.setItem(slot, cell.copyWithCount(cell.getCount() - 1));
            Item remaining = cell.getItem().getCraftingRemainingItem();
            if (remaining == null) continue;
            ItemStack container = new ItemStack(remaining);
            if (container.isEmpty()) continue;
            if (!player.getInventory().add(container)) player.drop(container, false);
        }
    }

    public Player thePlayer() { return thePlayer; }
    public Container craftMatrix() { return craftMatrix; }
}
