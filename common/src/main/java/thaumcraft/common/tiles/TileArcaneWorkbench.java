package thaumcraft.common.tiles;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftArcane;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.lib.utils.DeferredItemDrops;

/** Original slots 0..8 = matrix, 10 = wand; slot 9 is deliberately NOT a saved/physical output. */
public final class TileArcaneWorkbench extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int WAND_SLOT = 10;
    private final NonNullList<ItemStack> items = NonNullList.withSize(11, ItemStack.EMPTY);
    private boolean evacuating;

    public TileArcaneWorkbench(BlockPos pos, BlockState state) {
        super(ThaumcraftArcane.ARCANE_WORKBENCH_BLOCK_ENTITY.get(), pos, state);
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot == 9 ? ItemStack.EMPTY : items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot == 9) return ItemStack.EMPTY;
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return slot == 9 ? ItemStack.EMPTY : ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot == 9) return;
        // Like TileMagicWorkbench:87-93, preserve the supplied whole stack.
        // Normal GUI/automation limits live on Slot/Container; silently applying
        // itemmax1 here would destroy data during the original wand transfer.
        items.set(slot, stack);
        setChanged();
    }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot != 9 && (slot != WAND_SLOT || ArcaneVis.isWorkbenchWand(stack)); }
    @Override public int[] getSlotsForFace(Direction side) { return new int[] {WAND_SLOT}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == WAND_SLOT && ArcaneVis.isWorkbenchWand(stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == WAND_SLOT; }

    @Override public boolean stillValid(Player player) {
        return level != null && !isRemoved() && player.level() == level && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition).is(ThaumcraftArcane.ARCANE_WORKBENCH.get())
                && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64;
    }

    /** One physical update after all server-side ingredient/vis checks have passed. */
    public void applyCraft(List<ItemStack> matrix, ItemStack wand) {
        if (matrix.size() != 9 || !(level instanceof ServerLevel server) || !server.getServer().isSameThread()) {
            throw new IllegalStateException("Server-thread 3x3 craft required");
        }
        for (int slot = 0; slot < 9; slot++) items.set(slot, matrix.get(slot));
        items.set(WAND_SLOT, wand);
        setChanged();
    }

    public void evacuate() {
        if (!(level instanceof ServerLevel server) || evacuating || isEmpty()) return;
        evacuating = true;
        try {
            // InventoryUtils:354-377 drops each full physical stack using its
            // own Random and explicitly excludes workbench result slot9.
            // Reuse the existing lossless level-post adapter: do not spawn and
            // reenter LevelChunk's BE replacement while onRemove is in flight.
            DeferredItemDrops.enqueue(server, worldPosition, items.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList());
            items.clear();
            setChanged();
        } finally {
            evacuating = false;
        }
    }

    @Override public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide && !isRemoved()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        // TileMagicWorkbench saved its computed result too, but InventoryUtils
        // explicitly excluded it from physical drops. That nonphysical cache is
        // now per-viewer and recomputed, rather than part of native saved inputs.
        items.set(9, ItemStack.EMPTY);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public Component getDisplayName() { return Component.translatable("container.arcaneworkbench"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ContainerArcaneWorkbench(id, inventory, this); }
}
