package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftHoles;
import thaumcraft.common.items.wands.foci.PortableHoleRules;
import thaumcraft.common.lib.world.PortableHoles;

/**
 * Original TileHole (a TileMemory): the block this hole replaced, how long it has lasted, how long it will, how many blocks of the chain
 * are left and the side it runs from (-1 for the ring blocks, which do not spread). On its first tick a chain block makes the ring round
 * it and the next block of the chain (a failed next block ends the chain); when the time is up the old block comes back and a block
 * update is scheduled for it two ticks later. The old block's tile data is not kept: a block with a tile is never made into a hole.
 */
public final class TileHole extends BlockEntity {
    private static ClientEffects clientEffects;
    private BlockState old = Blocks.AIR.defaultBlockState();
    private int countdown, countdownMax = PortableHoleRules.DURATION, count, direction;

    public TileHole(BlockPos pos, BlockState state) { super(ThaumcraftHoles.HOLE_BLOCK_ENTITY.get(), pos, state); }

    public void configure(BlockState old, int duration, int count, int direction) {
        this.old = old;
        this.countdownMax = duration;
        this.count = count;
        this.direction = direction;
        setChanged();
    }

    public BlockState oldState() { return old; }
    public int countdown() { return countdown; }
    public int countdownMax() { return countdownMax; }
    public int count() { return count; }
    public int direction() { return direction; }

    /**
     * The client tick of the hole.
     *
     * <p>Original {@code TileHole.java:51-56}: the update tick of the original runs on both
     * sides and the client side draws the sparkles around the hole
     * ({@code TileHole.java:127-211}).</p>
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileHole hole) {
        if (clientEffects != null) clientEffects.tick(level, pos);
    }

    /** The client effect of the hole, installed by the client setup. */
    public interface ClientEffects { void tick(Level level, BlockPos pos); }

    public static void setClientEffects(ClientEffects effects) { clientEffects = effects; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileHole hole) {        if (!(level instanceof ServerLevel server)) return;
        if (PortableHoleRules.spreads(hole.countdown, hole.count, hole.direction)) {
            for (int[] offset : PortableHoleRules.ring(hole.direction)) {
                PortableHoles.create(server, pos.offset(offset[0], offset[1], offset[2]), -1, 1, hole.countdownMax);
            }
            int[] step = PortableHoleRules.step(hole.direction);
            if (!PortableHoles.create(server, pos.offset(step[0], step[1], step[2]), hole.direction, hole.count - 1, hole.countdownMax)) {
                hole.count = 0;
                hole.setChanged();
            }
        }
        hole.countdown = (short) (hole.countdown + 1);
        hole.setChanged();
        if (hole.countdown >= hole.countdownMax) {
            BlockState back = hole.old;
            // No neighbour update (as the original's flag 0), the clients told; the old block gets its own update two ticks on.
            server.setBlock(pos, back, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            server.scheduleTick(pos, back.getBlock(), 2);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("old", NbtUtils.writeBlockState(old));
        tag.putShort("countdown", (short) countdown);
        tag.putShort("countdownmax", (short) countdownMax);
        tag.putByte("count", (byte) count);
        tag.putByte("direction", (byte) direction);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        old = tag.contains("old") ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("old")) : Blocks.AIR.defaultBlockState();
        countdown = tag.getShort("countdown");
        countdownMax = tag.contains("countdownmax") ? tag.getShort("countdownmax") : PortableHoleRules.DURATION;
        count = tag.getByte("count");
        direction = tag.getByte("direction");
    }
}
