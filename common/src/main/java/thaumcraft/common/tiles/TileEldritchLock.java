package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;

/**
 * Original TileEldritchLock: the eldritch lock. Once it is activated it counts
 * down and then tears the surrounding room down.
 */
public final class TileEldritchLock extends BlockEntity {
    /** Original TileEldritchLock:29 - the countdown, minus one means inactive. */
    public int count = -1;
    byte facing = 0;

    public TileEldritchLock(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.ELDRITCH_TILE.get(), pos, state);
    }

    public byte getFacing() { return facing; }

    public void setFacing(byte face) {
        this.facing = face;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Original TileEldritchLock:40-52 - the lock countdown. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileEldritchLock lock) {
        if (lock.count == -1) return;
        lock.count++;
        if (lock.count % 5 == 0) {
            // TileEldritchLock:44 - the pump of the opening lock.
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (lock.count > 100) {
            lock.breakRoom(level, pos);
        }
    }

    /**
     * Original TileEldritchLock:54-96 - the activated lock removes the room
     * that surrounds it. The boss room generator itself belongs to the Outer
     * Lands port; the lock removes its own room here.
     */
    private void breakRoom(Level level, BlockPos pos) {
        count = -1;
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                for (int c = -2; c <= 2; c++) {
                    BlockPos target = new BlockPos(pos.getX() + a, pos.getY() + b, pos.getZ() + c);
                    if (level.getBlockState(target).isAir()) continue;
                    level.destroyBlock(target, false);
                }
            }
        }
        level.destroyBlock(pos, false);
        setChanged();
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
