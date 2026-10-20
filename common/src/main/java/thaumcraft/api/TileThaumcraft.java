package thaumcraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The original TC4 base tile entity with its own custom NBT section and a custom update packet.
 *
 * <p>Forced 1.21.1 changes: a block entity needs its registered {@link BlockEntityType}, the NBT hooks take
 * a registry provider and the original {@code S35PacketUpdateTileEntity} became the native
 * {@link ClientboundBlockEntityDataPacket}.</p>
 */
public class TileThaumcraft extends BlockEntity {
    public TileThaumcraft(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.readCustomNBT(tag);
    }

    /** The original per-tile custom data hook. */
    public void readCustomNBT(CompoundTag tag) { }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.writeCustomNBT(tag);
    }

    /** The original per-tile custom data hook. */
    public void writeCustomNBT(CompoundTag tag) { }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        CompoundTag tag = new CompoundTag();
        this.writeCustomNBT(tag);
        return ClientboundBlockEntityDataPacket.create(this, (block, registries) -> tag);
    }
}
