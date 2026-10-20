package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Original TileMemory: the block a hole replaced, its metadata and the data of the tile entity it had.
 * <p>
 * The original keeps the old block, the old metadata and the whole tile entity compound so that the block can
 * be put back with its data. 1.21.1 has no block metadata, so the old block and its metadata are kept as the
 * old block state, which is the same information in the modern form.
 * <p>
 * Oracle: {@code thaumcraft/common/tiles/TileMemory.java}, lines 17-68.
 */
public class TileMemory extends BlockEntity {
    private BlockState oldState = Blocks.AIR.defaultBlockState();
    private CompoundTag tileEntityCompound;

    public TileMemory(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    /** TileMemory:26-32 - remember a block and the data of the tile entity it had. */
    public TileMemory(BlockEntityType<?> type, BlockPos pos, BlockState state, BlockState old, CompoundTag data) {
        super(type, pos, state);
        this.oldState = old == null ? Blocks.AIR.defaultBlockState() : old;
        this.tileEntityCompound = data == null ? null : data.copy();
    }

    /** The block this memory replaced. */
    public BlockState oldState() { return oldState; }
    /** The saved data of the old tile entity, or null when there was none. */
    public CompoundTag tileEntityCompound() { return tileEntityCompound == null ? null : tileEntityCompound.copy(); }

    /**
     * TileMemory:39-49 - put the old block back and restore the saved tile entity data into it.
     */
    public void recreateTileEntity() {
        if (level == null) return;
        if (tileEntityCompound != null && level.getBlockEntity(worldPosition) != null) {
            level.setBlock(worldPosition, oldState, Block.UPDATE_CLIENTS);
            CompoundTag data = tileEntityCompound.copy();
            data.putInt("x", worldPosition.getX());
            data.putInt("y", worldPosition.getY());
            data.putInt("z", worldPosition.getZ());
            BlockEntity restored = level.getBlockEntity(worldPosition);
            if (restored != null) restored.loadWithComponents(data, level.registryAccess());
        }
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("oldblock", NbtUtils.writeBlockState(oldState));
        if (tileEntityCompound != null) tag.put("TileEntity", tileEntityCompound.copy());
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        oldState = tag.contains("oldblock")
                ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("oldblock"))
                : Blocks.AIR.defaultBlockState();
        tileEntityCompound = tag.contains("TileEntity") ? tag.getCompound("TileEntity").copy() : null;
    }
}
