package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNodeDevices;

/** Original TileNodeStabilizer18-50, BASIC ONLY. Its client count is animation,
 * not the ordinary node's authoritative cached lock or an invented vis store. */
public final class TileNodeStabilizer extends BlockEntity {
    private int animationCount;
    private boolean ticking;
    /** Original TileNodeStabilizer:24 - the lock is one for the node stabilizer and two for the advanced one. */
    private final int lock;

    public TileNodeStabilizer(BlockPos pos, BlockState state) {
        super(ThaumcraftNodeDevices.NODE_STABILIZER_BLOCK_ENTITY.get(), pos, state);
        this.lock = state.getBlock() == ThaumcraftNodeDevices.ADVANCED_NODE_STABILIZER.get() ? 2 : 1;
    }
    public int getLock() { return lock; }
    /** Whether this is the advanced node stabilizer, the original blockStoneDevice metadata ten. */
    public boolean isAdvanced() { return lock == 2; }
    public int getAnimationCount() { return animationCount; }
    public int getCount() { return animationCount; }
    public boolean isPowered() { return level != null && powerAreaLoaded(level, worldPosition) && level.hasNeighborSignal(worldPosition); }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileNodeStabilizer tile) {
        if (!level.isClientSide || tile.level != level || !tile.worldPosition.equals(pos) || state != tile.getBlockState() || tile.ticking || tile.isRemoved()) return;
        tile.ticking = true;
        try {
            if (!tile.current() || pos.getY() >= level.getMaxBuildHeight() - 1) return;
            BlockPos above = pos.above();
            if (!level.hasChunkAt(above) || !powerAreaLoaded(level, pos)) return; // Unknown is not absent/unpowered.
            // Original block+metadata predicate, NOT an extra above-BE gate.
            BlockState observed = level.getBlockState(above);
            boolean active = (observed.is(ThaumcraftContent.AURA_NODE.get())
                    || observed.is(ThaumcraftNodeDevices.ENERGIZED_NODE.get()))
                    && !tile.isPowered();
            if (!tile.current() || !level.hasChunkAt(above) || level.getBlockState(above) != observed
                    || !tile.current() || tile.level != level || tile.getBlockState() != state) return;
            if (active) { if (tile.animationCount < 37) ++tile.animationCount; }
            else if (tile.animationCount > 0) --tile.animationCount;
        } finally { tile.ticking = false; }
    }

    private boolean current() {
        Level world = level;
        BlockState state = getBlockState();
        return world != null && !isRemoved() && (state.is(ThaumcraftNodeDevices.NODE_STABILIZER.get())
                || state.is(ThaumcraftNodeDevices.ADVANCED_NODE_STABILIZER.get()))
                && !world.isOutsideBuildHeight(worldPosition) && world.hasChunkAt(worldPosition)
                && world.getBlockEntity(worldPosition) == this && world.getBlockState(worldPosition) == state
                && !isRemoved() && level == world && getBlockState() == state;
    }

    /** Vanilla hasNeighborSignal may inspect a neighboring conductor's six
     * direct-signal neighbors too. Preflight that horizontal radius2 diamond,
     * not getBlockState on an unavailable chunk, and never require a support BE. */
    static boolean powerAreaLoaded(Level level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return false;
        for (int x = -2; x <= 2; x++) for (int z = -2 + Math.abs(x); z <= 2 - Math.abs(x); z++) {
            if (!level.hasChunkAt(pos.offset(x, 0, z))) return false;
        }
        return true;
    }

    // No storage/custom NBT. Unknown fields (including forged count/lock/aspects)
    // are ignored as in the original stockless TileEntity, never spendable data.
    // Fresh load starts at0; a live client update does not import/reset animation.
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return new CompoundTag(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
