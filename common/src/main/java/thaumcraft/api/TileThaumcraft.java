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

    /**
     * The key the update tag always carries besides the custom data.
     *
     * <p>A 1.21.1 client ignores a block entity update whose tag is empty, while 1.7.10 always handed the description
     * packet to the tile: without the key, a container whose custom data became empty (an emptied jar writes nothing)
     * would keep its old contents on every client. {@link #readCustomNBT} receives the key and can ignore it.</p>
     */
    public static final String UPDATE_KEY = "thaumcraft:update";

    /**
     * The custom data a client receives with the chunk and with each update.
     *
     * <p>In 1.7.10 the description packet was sent with the chunk as well as after {@code markBlockForUpdate}; 1.21.1
     * sends the chunk with this tag instead, so a client sees the custom data from the moment the block comes into view.
     * To push a change later, call {@code setChanged()} and {@code level.sendBlockUpdated(pos, state, state, 3)}.</p>
     */
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.writeCustomNBT(tag);
        tag.putBoolean(UPDATE_KEY, true);
        return tag;
    }

    /** The original custom update packet: the custom data, read on the client by {@link #readCustomNBT}. */
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (block, registries) -> getUpdateTag(registries));
    }
}
