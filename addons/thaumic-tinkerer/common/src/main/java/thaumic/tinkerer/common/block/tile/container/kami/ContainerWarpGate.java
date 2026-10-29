/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Jan 10, 2014, 5:45:28 PM (GMT)]
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.container.kami;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumic.tinkerer.common.block.tile.container.ContainerPlayerInv;
import thaumic.tinkerer.common.block.tile.container.slot.kami.SlotSkyPearl;
import thaumic.tinkerer.common.block.tile.kami.TileWarpGate;
import thaumic.tinkerer.common.content.KamiWorld;
import thaumic.tinkerer.common.item.kami.ItemSkyPearl;

/**
 * The menu of the Celestial Gateway: two rows of five pearl slots at (30 + 25 x, 27 + 25 y) above the player's inventory.
 * The "locked" flag travels with the menu (a data slot), so the button of the screen shows what the server has.
 */
public class ContainerWarpGate extends ContainerPlayerInv {
    /** The pearl slots come first; the player's inventory follows (indices 10 to 45). */
    public static final int GATE_SLOTS = TileWarpGate.SLOTS;

    public final TileWarpGate gate;

    public ContainerWarpGate(int containerId, Inventory playerInv, TileWarpGate gate) {
        super(KamiWorld.warpGateMenu(), containerId, playerInv);
        this.gate = gate;

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 5; x++) addSlot(new SlotSkyPearl(gate, y * 5 + x, 30 + x * 25, 27 + y * 25));
        }

        initPlayerInv();

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return gate.locked ? 1 : 0;
            }

            @Override
            public void set(int value) {
                gate.locked = value != 0;
            }
        });
    }

    /** The client side: the block position comes from the packet, the block entity from the client's world. */
    public static ContainerWarpGate fromNetwork(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity entity = playerInv.player.level().getBlockEntity(pos);
        if (entity instanceof TileWarpGate gate) return new ContainerWarpGate(containerId, playerInv, gate);
        BlockState state = KamiWorld.warpGateBlock().defaultBlockState();
        TileWarpGate detached = new TileWarpGate(pos, state);
        detached.setLevel(playerInv.player.level());
        return new ContainerWarpGate(containerId, playerInv, detached);
    }

    @Override
    public boolean stillValid(Player player) {
        return gate.stillValid(player);
    }

    /**
     * 1.7.10 {@code transferStackInSlot}: from a pearl slot into the inventory, but only slots 10 to 35 (the original merged
     * into {@code 10..36} exclusive: the hot bar and the last slot of the inventory are never filled); from the inventory,
     * only a Celestial Pearl moves, into the pearl slots; nothing else does.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();

            if (index < GATE_SLOTS) {
                if (!moveItemStackTo(stack, GATE_SLOTS, GATE_SLOTS + 26, false)) return ItemStack.EMPTY;
            } else if (original.getItem() instanceof ItemSkyPearl && !moveItemStackTo(stack, 0, GATE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;

            slot.onTake(player, stack);
        }

        return original;
    }
}
