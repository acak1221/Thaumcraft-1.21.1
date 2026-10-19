package thaumcraft.common.blocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Native adaptation of Forge 1.7.10 BlockFluidFinite + BlockFluidBase, pinned to
 * MinecraftForge/MinecraftForge@9274e4fe435cb415099a8216c1b42235f185443e,
 * src/main/java/net/minecraftforge/fluids/{BlockFluidFinite,BlockFluidBase}.java.
 * Retains vertical-first flow, strict lower-than sharing and N/S/W/E remainder RNG.
 * No source multiplication, FlowingFluid, block entity, or unloaded-chunk reads.
 */
public abstract class FluxFiniteBlock extends Block {
    public static final int QUANTA = 8;
    // Original exceptional births use metadata8 (NINE units), not a capacity9.
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 8);
    /** Geometry only; neither full height nor an overfull birth changes capacity8. */
    public static final BooleanProperty FULL_HEIGHT = BooleanProperty.create("full_height");
    public static final TagKey<Block> DISPLACEMENT_BLOCKED = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "flux_displacement_blocked"));
    public static final TagKey<Block> DISPLACEMENT_ALLOWED = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "flux_displacement_allowed"));
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    // Server-thread, bounded synchronous frames only. Entries are removed in
    // finally; no world-lifetime cache, saved reservation or extra fluid store.
    private static final Map<ServerLevel, Set<BlockPos>> ACTIVE = new IdentityHashMap<>();
    private final int density;
    private final Direction vertical;
    private final int tickRate;

    protected FluxFiniteBlock(Properties properties, int density, int tickRate) {
        super(properties);
        this.density = density;
        this.vertical = density > 0 ? Direction.DOWN : Direction.UP;
        this.tickRate = tickRate;
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0).setValue(FULL_HEIGHT, false));
    }

    public final int density() { return density; }
    public final int tickRate() { return tickRate; }
    public final Direction verticalDirection() { return vertical; }
    public static int quanta(BlockState state) { return state.getBlock() instanceof FluxFiniteBlock ? state.getValue(LEVEL) + 1 : 0; }
    public static float quantaPercentage(BlockState state) { return quanta(state) / 8F; }

    /** getChunkNow is explicitly non-loading, unlike getBlockState on unknown chunks. */
    public static boolean loaded(ServerLevel level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static boolean readable(BlockGetter level, BlockPos pos) {
        if (level instanceof ServerLevel server) return loaded(server, pos);
        return !(level instanceof LevelReader reader) || (!reader.isOutsideBuildHeight(pos) && reader.hasChunk(pos.getX() >> 4, pos.getZ() >> 4));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LEVEL, FULL_HEIGHT); }
    @Override protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) { return state.getValue(LEVEL) < 2; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }

    // TC4 canCollideCheck only accepts a fluid ray with fullHit=true at level 7.
    // Ordinary mining rays must not turn these fluids into harvestable solid blocks.
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }

    public final BlockState withGeometry(BlockState state, BlockGetter level, BlockPos pos) {
        BlockPos origin = pos.relative(vertical.getOpposite());
        BlockPos destination = pos.relative(vertical);
        boolean full = false;
        if (readable(level, origin)) {
            BlockState other = level.getBlockState(origin);
            full = !other.getFluidState().isEmpty() || other.is(this)
                    || (vertical == Direction.DOWN && other.getBlock() instanceof FluxFiniteBlock);
        }
        // Original BlockGasRenderer:87-95 renders a full cube unless it has a solid ceiling.
        if (vertical == Direction.UP && readable(level, destination)) {
            full |= !level.getBlockState(destination).isFaceSturdy(level, destination, Direction.DOWN);
        }
        return state.setValue(FULL_HEIGHT, full);
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return withGeometry(state, level, pos);
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!level.isClientSide) level.scheduleTick(pos, this, tickRate);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide) level.scheduleTick(pos, this, tickRate);
    }

    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { tick(state, level, pos, random); }

    @Override protected final void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        try (Step step = begin(level, pos, state)) {
            if (step == null) return;
            flow(step, random);
            if (step.coherent()) afterFlow(step, random);
        }
    }

    /** Goo alone has the original post-flow tail, including lawful AIR/swap outcomes. */
    protected void afterFlow(Step step, RandomSource random) {}

    private int quantaBelow(Step step, BlockPos pos, int lessThan) {
        BlockState state = step.observe(pos);
        if (state == null) return -1;
        int amount = state.isAir() ? 0 : state.is(this) ? quanta(state) : -1;
        return amount >= lessThan ? -1 : amount;
    }

    private BlockState amountState(int amount) {
        if (amount < 0 || amount > QUANTA + 1) throw new IllegalArgumentException("Invalid finite flux amount " + amount);
        return amount == 0 ? Blocks.AIR.defaultBlockState() : defaultBlockState().setValue(LEVEL, amount - 1);
    }

    private void sourceAmount(Step step, int amount) { step.commit(step.change(step.pos, amountState(amount), Block.UPDATE_CLIENTS)); }

    private void flow(Step step, RandomSource random) {
        BlockPos pos = step.pos;
        int before = quanta(step.observe(pos));
        int remaining = flowVertically(step, before);
        if (remaining == 0 || !step.coherent()) return;
        boolean changed = remaining != before;
        if (remaining == 1) {
            if (changed) sourceAmount(step, remaining);
            return;
        }

        int[] neighbors = new int[4];
        int total = remaining;
        int count = 1;
        // Original displacements happen before testing the lower-than threshold.
        for (Direction direction : HORIZONTAL) {
            BlockPos target = pos.relative(direction);
            if (step.observe(target) != null && displace(step, target)) {
                if (!step.coherent()) return;
                step.commit(step.change(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
            }
            if (!step.coherent()) return;
        }
        for (int i = 0; i < HORIZONTAL.length; ++i) {
            neighbors[i] = quantaBelow(step, pos.relative(HORIZONTAL[i]), remaining - 1);
            if (neighbors[i] >= 0) {
                ++count;
                total += neighbors[i];
            }
        }
        if (count == 1) {
            if (changed) sourceAmount(step, remaining);
            return;
        }
        int each = total / count;
        int remainder = total % count;
        var writes = new ArrayList<Change>();
        var scheduled = new ArrayList<BlockPos>();
        for (int i = 0; i < HORIZONTAL.length; ++i) {
            if (neighbors[i] < 0) continue;
            int amount = each;
            // Intentionally preserve Forge's unusual >1 remainder test and RNG bound.
            if (remainder == count || remainder > 1 && random.nextInt(count - remainder) != 0) {
                ++amount;
                --remainder;
            }
            if (amount != neighbors[i]) {
                BlockPos target = pos.relative(HORIZONTAL[i]);
                writes.add(step.change(target, amountState(amount), Block.UPDATE_CLIENTS));
                scheduled.add(target);
            }
            --count;
        }
        if (remainder > 0) ++each;
        writes.add(step.change(pos, amountState(each), Block.UPDATE_CLIENTS));
        if (step.commit(writes.toArray(Change[]::new))) for (BlockPos target : scheduled) step.schedule(target, this);
    }

    private int flowVertically(Step step, int amount) {
        ServerLevel level = step.level;
        BlockPos pos = step.pos;
        BlockPos target = pos.relative(vertical);
        if (level.isOutsideBuildHeight(target)) {
            // Original finite fluid leaves the world at its vertical build boundary.
            step.commit(step.change(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
            return 0;
        }
        if (!loaded(level, target)) return amount;
        int otherAmount = quantaBelow(step, target, QUANTA);
        if (otherAmount >= 0) {
            int total = otherAmount + amount;
            int remainder = Math.max(0, total - QUANTA);
            // Commit BOTH actual balances before neighbor/effect callbacks.
            if (!step.commit(step.change(target, amountState(Math.min(total, QUANTA)), Block.UPDATE_ALL),
                    step.change(pos, amountState(remainder), total > QUANTA ? 0 : Block.UPDATE_ALL))) return 0;
            step.schedule(target, this);
            return remainder;
        }
        BlockState other = step.observe(target);
        if (other == null) return amount;
        if (other.getBlock() instanceof FluxFiniteBlock fluid) {
            boolean swap = vertical == Direction.DOWN ? fluid.density < density : fluid.density > density;
            if (swap) {
                if (step.commit(step.change(target, amountState(amount), Block.UPDATE_ALL), step.change(pos, other, Block.UPDATE_ALL))) {
                    step.schedule(target, this);
                    step.schedule(pos, fluid);
                }
                return 0;
            }
        } else if (displace(step, target)) {
            if (!step.coherent()) return 0;
            // Unlike AIR/same-fluid merge, original displacement transfers ALL9.
            if (step.commit(step.change(target, amountState(amount), Block.UPDATE_ALL),
                    step.change(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL))) step.schedule(target, this);
            return 0;
        }
        return amount;
    }

    /**
     * 1.7 material adapter: explicit door/sign/reeds/portal veto, optional taint-fibre
     * allow, then native blocksMotion for old material.blocksMovement. Vanilla
     * liquids have no Forge BlockFluidBase density and are displaceable, as before.
     * MaterialTaint:30-35 is non-solid but blocksMovement=true, so the other flux
     * phase is NOT horizontally displaced. Its vertical density swap is separate.
     */
    private boolean displace(Step step, BlockPos pos) {
        ServerLevel level = step.level;
        BlockState state = step.observe(pos);
        if (state == null) return false;
        if (state.isAir()) return true;
        if (state.is(this)) return false;
        if (state.is(DISPLACEMENT_BLOCKED)) return false;
        if (state.is(DISPLACEMENT_ALLOWED)) {
            Block.dropResources(state, level, pos);
            return true;
        }
        if (state.getBlock() instanceof FluxFiniteBlock || state.blocksMotion()
                || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)) return false;
        Block.dropResources(state, level, pos);
        return step.coherent();
    }

    /** Only geometry may vary without changing a cell's actual resource identity. */
    protected static boolean sameStock(BlockState first, BlockState second) {
        return first == second || first.getBlock() instanceof FluxFiniteBlock && first.getBlock() == second.getBlock()
                && first.getValue(LEVEL).equals(second.getValue(LEVEL));
    }

    protected static Step begin(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel server) || !loaded(server, pos) || !sameStock(state, server.getBlockState(pos))) return null;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Finite flux requires the server thread");
        Set<BlockPos> active = ACTIVE.computeIfAbsent(server, ignored -> new HashSet<>());
        var footprint = new HashSet<BlockPos>();
        footprint.add(pos.immutable());
        for (Direction direction : Direction.values()) footprint.add(pos.relative(direction));
        if (footprint.stream().anyMatch(active::contains)) return null;
        active.addAll(footprint);
        return new Step(server, pos.immutable(), footprint);
    }

    protected record Change(BlockPos pos, BlockState before, BlockState after, int flags) {}

    /** Native balances are written without neighbor/shape/packet call-outs, then
     * the original flags/order are delivered against the committed world. Native
     * Level.setBlock still owns chunk palette/height/light/onPlace bookkeeping.
     * Built-in flux has no BE/onRemove resource callback. This is NOT arbitrary
     * addon setBlock/onPlace atomicity or cross-file crash durability. */
    protected static final class Step implements AutoCloseable {
        final ServerLevel level;
        final BlockPos pos;
        private final Set<BlockPos> footprint;
        private final Map<BlockPos, BlockState> observed = new LinkedHashMap<>();
        private final Map<BlockPos, BlockState> unnotified = new LinkedHashMap<>();
        private boolean valid = true;

        Step(ServerLevel level, BlockPos pos, Set<BlockPos> footprint) {
            this.level = level; this.pos = pos; this.footprint = footprint;
            observed.put(pos, level.getBlockState(pos));
        }

        BlockState observe(BlockPos target) {
            if (!loaded(level, target)) return null;
            BlockState current = level.getBlockState(target);
            BlockState old = observed.putIfAbsent(target.immutable(), current);
            if (old != null && !sameStock(old, current)) valid = false;
            return current;
        }

        boolean coherent() {
            if (!valid) return false;
            for (var entry : observed.entrySet()) if (!loaded(level, entry.getKey())
                    || !sameStock(entry.getValue(), level.getBlockState(entry.getKey()))) return valid = false;
            return true;
        }

        Change change(BlockPos target, BlockState next, int flags) { return new Change(target.immutable(), observe(target), next, flags); }

        boolean commit(Change... changes) {
            for (Change change : changes) if (change.before == null || !loaded(level, change.pos)
                    || !sameStock(change.before, level.getBlockState(change.pos))) return valid = false;
            if (!coherent()) return false;
            var order = new ArrayList<Change>();
            // Debit/reassign the original source first, before any new recipient.
            for (Change change : changes) if (change.pos.equals(pos)) order.add(change);
            for (Change change : changes) if (!change.pos.equals(pos)) order.add(change);
            for (Change change : order) {
                if (!coherent()) return false;
                observed.put(change.pos, change.after);
                level.setBlock(change.pos, change.after, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                if (!coherent()) return false;
            }
            // Recompute geometry AFTER every balance is installed, not from a
            // half-applied vertical transfer. FULL_HEIGHT never changes quantity.
            for (Change change : changes) {
                BlockState current = level.getBlockState(change.pos);
                if (current.getBlock() instanceof FluxFiniteBlock fluid) {
                    BlockState geometry = fluid.withGeometry(current, level, change.pos);
                    if (!coherent()) return true;
                    level.setBlock(change.pos, geometry, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
                    if (!coherent()) return true;
                }
            }
            for (Change change : changes) {
                if (change.flags == 0) {
                    unnotified.putIfAbsent(change.pos, change.before);
                    continue; // Original overflow notifies the final source only.
                }
                if (!coherent()) break;
                BlockState current = level.getBlockState(change.pos);
                BlockState previous = unnotified.remove(change.pos);
                if (previous == null) previous = change.before;
                if (previous == current) continue;
                if ((change.flags & Block.UPDATE_CLIENTS) != 0) level.sendBlockUpdated(change.pos, previous, current, change.flags);
                // Vanilla's broad propagation can read unknown chunks. Deliver
                // the same notifications only to actually available neighbors.
                if ((change.flags & Block.UPDATE_NEIGHBORS) != 0) for (Direction direction : NeighborUpdater.UPDATE_ORDER) {
                    if (!coherent()) break;
                    BlockPos neighbor = change.pos.relative(direction);
                    if (!loaded(level, neighbor)) continue;
                    level.neighborChanged(neighbor, previous.getBlock(), change.pos);
                }
                // Keep vanilla's separate neighbor-then-shape stages and its
                // own ordering constants, rather than interleaving enum order.
                for (Direction direction : UPDATE_SHAPE_ORDER) {
                    if (!coherent()) break;
                    BlockPos neighbor = change.pos.relative(direction);
                    if (!loaded(level, neighbor)) continue;
                    level.neighborShapeChanged(direction.getOpposite(), level.getBlockState(change.pos), neighbor, change.pos,
                            change.flags & ~(Block.UPDATE_NEIGHBORS | Block.UPDATE_SUPPRESS_DROPS), 512);
                }
            }
            return true; // A post-commit callback never refunds/replays a debit.
        }

        void schedule(BlockPos target, FluxFiniteBlock block) {
            if (loaded(level, target) && level.getBlockState(target).is(block)) level.scheduleTick(target, block, block.tickRate);
        }

        @Override public void close() {
            Set<BlockPos> active = ACTIVE.get(level);
            if (active == null) return;
            active.removeAll(footprint);
            if (active.isEmpty()) ACTIVE.remove(level);
        }
    }
}
