package thaumcraft.common.lib.visnet;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.FluxFiniteBlock;
import thaumcraft.common.config.ThaumcraftFlux;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.tiles.TileNodeConverter;
import thaumcraft.common.tiles.TileNodeEnergized;
import thaumcraft.common.tiles.TileNodeStabilizer;
import thaumcraft.api.visnet.VisNetHandler;

/** Original BlockAiry350-381, with nonloading identity-bound native authority.
 * Unavailable support is UNKNOWN, never proof that a tower was destroyed. */
public final class VisNodeFailures {
    private static final ThreadLocal<BlockEntity> MUTATION = new ThreadLocal<>();
    private static final ThreadLocal<Failure> FAILURE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> OBSERVING = new ThreadLocal<>();
    private VisNodeFailures() {}

    /** Native-engine reentry fence, not a clock-based generation cap. */
    public static final class Frame implements AutoCloseable {
        private final BlockEntity owner;
        private boolean closed;
        private Frame(BlockEntity owner) { this.owner = owner; }
        @Override public void close() {
            if (!closed) {
                if (MUTATION.get() != owner) throw new IllegalStateException("Wrong node mutation owner");
                closed = true;
                MUTATION.remove();
            }
        }
    }
    public static boolean isMutationActive() { return MUTATION.get() != null || FAILURE.get() != null; }
    public static Frame beginMutation(BlockEntity owner) {
        if (isMutationActive()) return null;
        // Fence BEFORE any virtual world/identity query.
        MUTATION.set(owner);
        try {
            if (owner.getLevel() instanceof ServerLevel server && nativeServer(server)) return new Frame(owner);
        } catch (RuntimeException | Error failure) {
            MUTATION.remove();
            throw failure;
        }
        MUTATION.remove();
        return null;
    }
    /** Hold the same fence across superclass graph-detach/read callbacks, even
     * for a detached native loadStatic target. It authorizes NO world mutation. */
    public static Frame beginLoading(BlockEntity owner) {
        if (isMutationActive()) throw new IllegalStateException("Node reload during an active native mutation");
        MUTATION.set(owner);
        try {
            Level level = owner.getLevel();
            if (level != null && !level.isClientSide && (!(level instanceof ServerLevel server) || !nativeServer(server)))
                throw new IllegalStateException("Node reload requires its native server level");
            return new Frame(owner);
        } catch (RuntimeException | Error failure) {
            MUTATION.remove();
            throw failure;
        }
    }
    public static boolean nativeServer(ServerLevel level) {
        return VisNetHandler.isNativeServerLevel(level);
    }
    public static boolean loaded(Level level, BlockPos pos) {
        if (level == null || level.isOutsideBuildHeight(pos)) return false;
        return level instanceof ServerLevel server
                ? server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null : level.hasChunkAt(pos);
    }
    /** Do not lazily manufacture a missing BE while observing a tower. */
    public static BlockEntity existing(Level level, BlockPos pos) {
        if (!loaded(level, pos)) return null;
        if (level instanceof ServerLevel server) {
            var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? null : chunk.getBlockEntities().get(pos);
        }
        return level.getBlockEntity(pos);
    }
    public static boolean installed(BlockEntity tile) {
        Level level = tile.getLevel();
        BlockState state = tile.getBlockState();
        return level != null && !tile.isRemoved() && loaded(level, tile.getBlockPos())
                && existing(level, tile.getBlockPos()) == tile && level.getBlockState(tile.getBlockPos()) == state
                && tile.getLevel() == level && !tile.isRemoved() && tile.getBlockState() == state;
    }
    /** hasNeighborSignal can read a conductor's neighbors as well (radius2). */
    public static boolean powerAreaLoaded(Level level, BlockPos pos) {
        if (!loaded(level, pos)) return false;
        for (int x = -2; x <= 2; x++) for (int z = -2 + Math.abs(x); z <= 2 - Math.abs(x); z++)
            if (!loaded(level, pos.offset(x, 0, z))) return false;
        return true;
    }

    private record Tower(ServerLevel level, BlockPos pos, BlockState belowState, BlockEntity below,
                         BlockState aboveState, BlockEntity above, boolean powered, boolean coherent) {}
    private static Tower observe(TileNodeEnergized source) {
        if (!(source.getLevel() instanceof ServerLevel level) || !nativeServer(level)
                || !installed(source) || !source.getBlockState().is(ThaumcraftNodeDevices.ENERGIZED_NODE.get())) return null;
        BlockPos pos = source.getBlockPos(), below = pos.below(), above = pos.above();
        if (!loaded(level, below) || !loaded(level, above) || !powerAreaLoaded(level, below)) return null;
        BlockState bs = level.getBlockState(below), as = level.getBlockState(above);
        BlockEntity be = existing(level, below), ae = existing(level, above);
        boolean powered = level.hasNeighborSignal(below);
        boolean coherent = !powered && (bs.is(ThaumcraftNodeDevices.NODE_STABILIZER.get())
                || bs.is(ThaumcraftNodeDevices.ADVANCED_NODE_STABILIZER.get()))
                && be instanceof TileNodeStabilizer && installed(be)
                && as.is(ThaumcraftNodeDevices.NODE_TRANSDUCER.get()) && ae instanceof TileNodeConverter && installed(ae);
        if (!installed(source) || !loaded(level, below) || !loaded(level, above) || !powerAreaLoaded(level, below)
                || level.getBlockState(below) != bs || existing(level, below) != be
                || level.getBlockState(above) != as || existing(level, above) != ae
                || level.hasNeighborSignal(below) != powered || !installed(source)) return null;
        return new Tower(level, pos, bs, be, as, ae, powered, coherent);
    }
    /** Pure observer used by debit admission. Never calls converter.checkStatus. */
    public static boolean isTowerCoherent(TileNodeEnergized source) {
        if (source == null || OBSERVING.get() != null) return false;
        OBSERVING.set(true);
        try {
            Tower tower = observe(source);
            return tower != null && tower.coherent();
        } finally { OBSERVING.remove(); }
    }

    /** Unforgeable short-lived permission for raw retirement of THIS source. */
    public static final class Failure {
        private final TileNodeEnergized source;
        private Failure(TileNodeEnergized source) { this.source = source; }
        public boolean authorizes(TileNodeEnergized candidate) { return candidate == source && FAILURE.get() == this; }
    }

    /** Production neighbor/load retry action. Not an arbitrary-position exploder.
     * Returns true only for a known loaded coherent tower; unknown/retired=false. */
    public static boolean checkTower(TileNodeEnergized source) {
        if (source == null || source.isQuarantined() || FAILURE.get() != null || TileNodeConverter.isTransitionActive()) return false;
        Failure operation = new Failure(source);
        FAILURE.set(operation);
        try {
            // Validation itself is a call-out frame; reentrant world queries
            // cannot start another check/refill before this fence is installed.
            Tower tower = observe(source);
            if (tower == null) return false;
            if (tower.coherent()) return true;
            ServerLevel level = tower.level();
            // Explosion rays (<8), candidate offsets7 and notification/power radius2.
            // Refuse before retiring anything if those native reads could load terrain.
            if (!failureAreaLoaded(level, tower.pos())) return false;
            Tower confirm = observe(source);
            if (confirm == null || confirm.coherent() || !sameTower(tower, confirm)) return false;
            if (!installed(source) || !source.retireForFailure(operation)) return false;
            // Flag/stock retirement is raw; native block removal comes BEFORE
            // graph-removal/neighbor/explosion callbacks, never after the blast.
            level.setBlock(tower.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (existing(level, tower.pos()) == source || !loaded(level, tower.pos())) return false;
            // No further unconditional write at the node coordinate: a callback
            // replacement, including a new source, is not ours to remove.
            level.explode(null, tower.pos().getX() + .5, tower.pos().getY() + .5, tower.pos().getZ() + .5,
                    3.0F, false, Level.ExplosionInteraction.NONE);
            placeTrials(level, tower.pos());
        } catch (RuntimeException failure) {
            // Retirement is irrevocable, including write-then-throw callbacks.
            // Never retry an already partially applied explosion/birth operation.
            Thaumcraft.LOG.warn("Energized node failure interrupted at {} (never retried after retirement)", source.getBlockPos(), failure);
        } finally { FAILURE.remove(); }
        return false;
    }
    private static boolean sameTower(Tower a, Tower b) {
        return a.level() == b.level() && a.pos().equals(b.pos()) && a.belowState() == b.belowState()
                && a.below() == b.below() && a.aboveState() == b.aboveState() && a.above() == b.above() && a.powered() == b.powered();
    }
    private static boolean failureAreaLoaded(ServerLevel level, BlockPos pos) {
        for (int x = (pos.getX() - 10) >> 4; x <= (pos.getX() + 10) >> 4; x++)
            for (int z = (pos.getZ() - 10) >> 4; z <= (pos.getZ() + 10) >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        return true;
    }
    private static void placeTrials(ServerLevel level, BlockPos origin) {
        for (int trial = 0; trial < 50; trial++) {
            // Original placement-loop order: SIX bound8 draws on EVERY trial.
            // Explosion's own native RNG is an earlier, independent prefix.
            int x = level.random.nextInt(8) - level.random.nextInt(8);
            int y = level.random.nextInt(8) - level.random.nextInt(8);
            int z = level.random.nextInt(8) - level.random.nextInt(8);
            BlockPos target = origin.offset(x, y, z);
            if (!loaded(level, target)) continue;
            BlockState air = level.getBlockState(target);
            if (!air.isAir() || existing(level, target) != null) continue;
            FluxFiniteBlock phase = target.getY() < origin.getY() ? ThaumcraftFlux.FLUX_GOO.get() : ThaumcraftFlux.FLUX_GAS.get();
            BlockState born = phase.withGeometry(phase.defaultBlockState().setValue(FluxFiniteBlock.LEVEL, 8), level, target);
            if (loaded(level, target) && level.getBlockState(target) == air && existing(level, target) == null)
                level.setBlock(target, born, Block.UPDATE_ALL); // metadata8 == NINE units, capacity STILL8.
        }
    }
}
