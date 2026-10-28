/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.block.camo;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of a camouflageable device (1.7.10: {@code TileCamo}, which stored a block and its metadata).
 *
 * <p>1.21.1 has no metadata, so the camouflage is the whole {@link BlockState} that was right-clicked on (saved as a
 * block-state tag). The state is part of both the saved data and the update packet. The block's own {@code CAMO}
 * property mirrors "has a camouflage" so that the device model is hidden while the block entity renderer draws the
 * camouflage (see {@link CamoDeviceBlock}).</p>
 */
public abstract class CamoDeviceBlockEntity extends BlockEntity {
    /** 1.7.10 keys of {@code TileCamo}; the metadata key is gone, {@code camo} now holds a block-state tag. */
    private static final String TAG_CAMO = "camo";

    @Nullable
    private BlockState camo;

    protected CamoDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The block the device pretends to be, or null for its own look. */
    @Nullable
    public BlockState getCamo() {
        return camo;
    }

    /**
     * Sets or clears the camouflage on the server: updates the block's {@code CAMO} property, saves the chunk and
     * resends the block entity to the clients (1.7.10: {@code markBlockForUpdate}).
     */
    public void setCamo(@Nullable BlockState newCamo) {
        if (newCamo != null && newCamo.isAir()) newCamo = null;
        this.camo = newCamo;
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.hasProperty(CamoDeviceBlock.CAMO) && state.getValue(CamoDeviceBlock.CAMO) != (camo != null)) {
            state = state.setValue(CamoDeviceBlock.CAMO, camo != null);
            level.setBlock(worldPosition, state, Block.UPDATE_ALL);
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
    }

    /** The custom data of the device, saved with the chunk and sent to the clients; subclasses call {@code super}. */
    protected void writeCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        if (camo != null) tag.put(TAG_CAMO, NbtUtils.writeBlockState(camo));
    }

    /** Reads what {@link #writeCustomData} wrote. */
    protected void readCustomData(CompoundTag tag, HolderLookup.Provider registries) {
        camo = null;
        if (tag.contains(TAG_CAMO, 10)) {
            BlockState read = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound(TAG_CAMO));
            camo = read.isAir() ? null : read;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeCustomData(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readCustomData(tag, registries);
    }

    /** A client ignores a block entity update whose tag is empty, so the update always carries this key. */
    public static final String UPDATE_KEY = "thaumictinkerer:update";

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeCustomData(tag, registries);
        tag.putBoolean(UPDATE_KEY, true);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
