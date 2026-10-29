/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.container;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumic.tinkerer.common.block.tile.TileEnchanter;
import thaumic.tinkerer.common.block.tile.container.slot.SlotTool;
import thaumic.tinkerer.common.block.tile.container.slot.SlotWand;
import thaumic.tinkerer.common.content.Enchantments;

/** The menu of the Osmotic Enchanter: the slot of the tool, the slot of the wand and the player's inventory. */
public class ContainerEnchanter extends ContainerPlayerInv {

    public final TileEnchanter enchanter;

    public ContainerEnchanter(int containerId, Inventory playerInv, TileEnchanter enchanter) {
        super(Enchantments.enchanterMenu(), containerId, playerInv);
        this.enchanter = enchanter;

        addSlot(new SlotTool(enchanter, TileEnchanter.SLOT_TOOL, 6, 6, () -> enchanter.working));
        addSlot(new SlotWand(enchanter, TileEnchanter.SLOT_WAND, 6, 31));

        initPlayerInv();
    }

    /** The client side: the block position comes from the packet, the block entity from the client's world. */
    public static ContainerEnchanter fromNetwork(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity entity = playerInv.player.level().getBlockEntity(pos);
        if (entity instanceof TileEnchanter enchanter) return new ContainerEnchanter(containerId, playerInv, enchanter);
        TileEnchanter detached = new TileEnchanter(pos, Enchantments.enchanterBlock().defaultBlockState());
        detached.setLevel(playerInv.player.level());
        return new ContainerEnchanter(containerId, playerInv, detached);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot clicked = slots.get(index);

        if (clicked != null && clicked.hasItem()) {
            ItemStack stack = clicked.getItem();

            if (stack.is(Items.BOOK)) return ItemStack.EMPTY;

            boolean wand = slots.get(TileEnchanter.SLOT_WAND).mayPlace(stack);

            moved = stack.copy();

            if (index < 2) {
                if (!moveItemStackTo(stack, 2, 38, false)) return ItemStack.EMPTY;
            } else if (wand) {
                if (!moveItemStackTo(stack, TileEnchanter.SLOT_WAND, TileEnchanter.SLOT_WAND + 1, false)) return ItemStack.EMPTY;
            } else if (slots.get(TileEnchanter.SLOT_TOOL).mayPlace(stack)) {
                if (!moveItemStackTo(stack, TileEnchanter.SLOT_TOOL, TileEnchanter.SLOT_TOOL + 1, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) clicked.setByPlayer(ItemStack.EMPTY);
            else clicked.setChanged();

            if (stack.getCount() == moved.getCount()) return ItemStack.EMPTY;

            clicked.onTake(player, stack);
        }

        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return enchanter.stillValid(player);
    }
}
