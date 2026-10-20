package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * Original TileBrainbox: the brainbox of the thaumatorium. It points at the
 * machine half that it serves.
 */
public final class TileBrainbox extends BlockEntity {
    /** The face of the machine half the brainbox serves. */
    public Direction facing = null;

    public TileBrainbox(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.BRAINBOX_TILE.get(), pos, state);
    }

    public Direction getFacing() { return facing; }

    public void setFacing(Direction face) {
        this.facing = face;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        facing = tag.contains("facing") ? Direction.from3DDataValue(tag.getByte("facing")) : null;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (facing != null) tag.putByte("facing", (byte) facing.get3DDataValue());
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
