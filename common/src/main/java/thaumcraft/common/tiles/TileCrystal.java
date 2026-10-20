package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftCrystalClusters;

/** Original inert TileCrystal: one SHORT orientation, constructor1, missing saved key reads0. No growth or tick state. */
public final class TileCrystal extends BlockEntity {
    public short orientation = 1;
    public TileCrystal(BlockPos position, BlockState state) { super(ThaumcraftCrystalClusters.CRYSTAL_TILE.get(), position, state); }
    public void setOrientation(Direction side) {
        orientation = (short) side.get3DDataValue(); setChanged();
        if (level != null && !level.isClientSide && level.getBlockEntity(worldPosition) == this)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); orientation = tag.getShort("orientation");
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); tag.putShort("orientation", orientation);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
