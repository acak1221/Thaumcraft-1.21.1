package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blocks.BlockItemGrate;
import thaumcraft.common.blocks.BlockMetalDevice;
import thaumcraft.common.config.ThaumcraftGrate;
import thaumcraft.common.entities.EntityItemGrate;
import thaumcraft.common.lib.GrateRules;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileGrate: a one-slot, always empty inventory. Whatever a hopper or dropper pushes into it from above while the grate is open
 * does not stay: it comes out below as a dropped item, centred, 0.6 above the floor of the block and moving down at 0.1 per tick.
 */
public final class TileGrate extends BlockEntity implements WorldlyContainer {
    private static final int[] TOP = {0};
    private static final int[] NONE = {};

    public TileGrate(BlockPos pos, BlockState state) { super(ThaumcraftGrate.GRATE_BLOCK_ENTITY.get(), pos, state); }

    /**
     * The original filter is closed while the block metadata is six and open while it is
     * five (oracle BlockMetalDevice:677-686 and TileGrate:65-67). The native tree carries
     * the filter twice: the dedicated item grate block with its own closed property and the
     * two metal device variants.
     */
    private boolean closed() {
        net.minecraft.world.level.block.state.BlockState state = getBlockState();
        if (state.hasProperty(BlockItemGrate.CLOSED)) return state.getValue(BlockItemGrate.CLOSED);
        if (state.getBlock() instanceof BlockMetalDevice) {
            return state.getValue(BlockMetalDevice.VARIANT) == EldritchBlockRules.DEVICE_METAL_GRATE_CLOSED;
        }
        return false;
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return true; }
    @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
    @Override public int getMaxStackSize() { return 64; }
    @Override public boolean stillValid(Player player) { return false; }
    @Override public void clearContent() { }

    @Override public void setItem(int slot, ItemStack stack) {
        if (!(level instanceof ServerLevel server) || stack.isEmpty()) return;
        // Oracle TileGrate:41-47 - the original drops the item as an
        // EntityItemGrate, the item that is pushed out of the filter block.
        EntityItemGrate item = new EntityItemGrate(server, worldPosition.getX() + 0.5D, worldPosition.getY() + GrateRules.SPAWN_HEIGHT, worldPosition.getZ() + 0.5D, stack.copy());
        // A new ItemEntity starts with a random sideways drift; the original overwrote it with a straight fall.
        item.setDeltaMovement(0.0D, GrateRules.FALL_SPEED, 0.0D);
        server.addFreshEntity(item);
    }

    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return !closed(); }
    @Override public int[] getSlotsForFace(Direction side) { return GrateRules.acceptsFrom(closed(), side) ? TOP : NONE; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return side != null && GrateRules.acceptsFrom(closed(), side); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
}
