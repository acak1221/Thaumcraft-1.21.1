package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.OuterLandsBossRooms;

/**
 * Original TileEldritchLock: the eldritch lock. Once it is activated it counts
 * down and then tears the surrounding room down.
 *
 * <p>Oracle {@code TileEldritchLock:49-121} - the lock counts to a hundred and one,
 * pumps every fifth tick and then opens: the arena of one of the four bosses of the
 * Outer Lands is built and the boss is spawned, the invisible barrier of the room
 * disappears and the lock itself with it.
 */
public final class TileEldritchLock extends BlockEntity {
    /** Original TileEldritchLock:33 - the countdown, minus one means inactive. */
    public int count = -1;
    byte facing = 0;

    /** Original TileEldritchLock:54 - the sound of the counting lock. */
    public static final SoundEvent PUMP = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "pump"));
    /** Original TileEldritchLock:53 - the lock pumps every fifth tick. */
    public static final int PUMP_INTERVAL = 5;
    /** Original TileEldritchLock:56 - the lock opens after a hundred and one ticks. */
    public static final int OPEN_AFTER = 100;

    public TileEldritchLock(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_TILE.get(), pos, state);
    }

    public byte getFacing() { return facing; }

    public void setFacing(byte face) {
        this.facing = face;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Original TileEldritchLock:49-60 - the lock countdown. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEldritchLock lock) {
        if (lock.count == -1) return;
        lock.count++;
        if (lock.count % PUMP_INTERVAL == 0) {
            // TileEldritchLock:54 - the pump of the opening lock.
            level.playSound(null, pos, PUMP, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (lock.count > OPEN_AFTER) {
            openLock(level, pos, lock);
        }
    }

    /**
     * Original TileEldritchLock:62-121 - the lock opens the boss room.
     *
     * <p>The arena of the boss and the boss itself are built by
     * {@link OuterLandsBossRooms}, which knows the four arenas of the original.
     */
    private static void openLock(Level level, BlockPos pos, TileEldritchLock lock) {
        if (!(level instanceof ServerLevel server)) return;
        OuterLandsBossRooms.openLock(server, pos);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        facing = tag.getByte("facing");
        count = tag.contains("count") ? tag.getInt("count") : -1;
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("facing", facing);
        tag.putInt("count", count);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
