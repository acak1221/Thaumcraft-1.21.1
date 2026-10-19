package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.lib.visnet.EnergizedNodeReducer;
import thaumcraft.common.lib.visnet.VisNodeFailures;
import thaumcraft.common.tiles.TileNode.NodeModifier;
import thaumcraft.common.tiles.TileNode.NodeType;

/** Original TileNodeConverter33-157. In particular conversion precedes count++,
 * and neighbor checkStatus resets an energized count to1000 (even unpowered). */
public final class TileNodeConverter extends BlockEntity {
    private int count = -1, status;
    private boolean ticking, checking, notifying, pendingCheck;
    private CompoundTag rejected;
    private static ClientEffects clientEffects;
    private static final ThreadLocal<Transition> TRANSITION = new ThreadLocal<>();

    public interface ClientEffects {
        void bolt(Level level, double fromX, double fromY, double fromZ, double toX, double toY, double toZ);
        /** C owns original burst1 AND craftfail sound volume.5/pitch1 here. */
        void burst(Level level, double x, double y, double z, float size);
    }
    public static void setClientEffects(ClientEffects effects) { clientEffects = effects; }
    public TileNodeConverter(BlockPos pos, BlockState state) { super(ThaumcraftNodeDevices.NODE_TRANSDUCER_BLOCK_ENTITY.get(), pos, state); }
    public int getCount() { return count; }
    public int getStatus() { return status; }
    public boolean isQuarantined() { return rejected != null; }
    public boolean isPowered() { return level != null && VisNodeFailures.powerAreaLoaded(level, worldPosition) && level.hasNeighborSignal(worldPosition); }
    public boolean hasStabilizer() {
        if (level == null) return false;
        BlockPos support = worldPosition.below(2);
        if (!VisNodeFailures.powerAreaLoaded(level, support)) return false;
        BlockState state = level.getBlockState(support);
        BlockEntity tile = VisNodeFailures.existing(level, support);
        boolean powered = level.hasNeighborSignal(support);
        return !powered && state.is(ThaumcraftNodeDevices.NODE_STABILIZER.get()) && tile instanceof TileNodeStabilizer
                && VisNodeFailures.installed(tile) && VisNodeFailures.powerAreaLoaded(level, support)
                && level.getBlockState(support) == state && VisNodeFailures.existing(level, support) == tile && !level.hasNeighborSignal(support);
    }
    private boolean current() {
        return !isQuarantined() && VisNodeFailures.installed(this) && getBlockState().is(ThaumcraftNodeDevices.NODE_TRANSDUCER.get());
    }
    private boolean towerAreaLoaded() {
        return VisNodeFailures.loaded(level, worldPosition.below()) && VisNodeFailures.powerAreaLoaded(level, worldPosition)
                && VisNodeFailures.powerAreaLoaded(level, worldPosition.below(2));
    }

    /** Neighbor action ONLY. Observers must use getters, never advance/reset it. */
    public void checkStatus() {
        if (level == null || checking || ticking || notifying || isQuarantined() || isTransitionActive()) return;
        if (level.isClientSide) {
            checking = true;
            try { if (current()) checkStatusInternal(); } finally { checking = false; }
        } else {
            if (VisNetHandler.isConsumeActive(level)) { pendingCheck = true; return; }
            try (var frame = VisNodeFailures.beginMutation(this)) {
                if (frame == null) { pendingCheck = true; return; }
                checking = true;
                try { if (current()) checkStatusInternal(); } finally { checking = false; }
            }
        }
    }
    private void checkStatusInternal() {
        if (!current() || !towerAreaLoaded()) { pendingCheck = true; return; }
        BlockPos below = worldPosition.below();
        BlockState nodeState = level.getBlockState(below);
        BlockEntity node = VisNodeFailures.existing(level, below);
        boolean stable = hasStabilizer(), powered = isPowered();
        if (!current() || !towerAreaLoaded() || level.getBlockState(below) != nodeState || VisNodeFailures.existing(level, below) != node
                || hasStabilizer() != stable || isPowered() != powered || !current()) { pendingCheck = true; return; }
        pendingCheck = false;
        if (count == -1) count = 0;
        boolean ordinary = nodeState.is(ThaumcraftContent.AURA_NODE.get()) && node instanceof TileNode && VisNodeFailures.installed(node);
        boolean energized = nodeState.is(ThaumcraftNodeDevices.ENERGIZED_NODE.get()) && node instanceof TileNodeEnergized && VisNodeFailures.installed(node);
        if (status == 2 && count > 50 && !(stable && energized)) {
            if (!level.isClientSide && energized) {
                TileNodeEnergized source = (TileNodeEnergized) node;
                VisNodeFailures.checkTower(source);
                if (!current()) return;
                if (VisNodeFailures.installed(source) && !source.isQuarantined()) { pendingCheck = true; return; }
            }
            // Original explodify would delete an unrelated replacement here.
            // Native authority fix: only the captured energized identity can fail.
            status = 0;
            count = 50;
            notifyState();
        } else if (powered && ordinary && stable) {
            status = 1;
            notifyState();
        } else if (energized) {
            status = 2;
            count = 1000; // Deliberate original NEIGHBOR-check quirk, not a getter.
            notifyState();
        } else {
            boolean changed = status != 0;
            status = 0;
            if (changed) notifyState();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileNodeConverter tile) {
        if (level.isClientSide || tile.level != level || !tile.worldPosition.equals(pos) || tile.getBlockState() != state
                || tile.ticking || tile.checking || tile.notifying || tile.isRemoved() || tile.isQuarantined()
                || isTransitionActive() || VisNetHandler.isConsumeActive(level)) return;
        try (var frame = VisNodeFailures.beginMutation(tile)) {
            if (frame == null) return;
            tile.ticking = true;
            try { if (tile.current()) tile.tickServer(); }
            finally { tile.ticking = false; }
        }
    }
    private void tickServer() {
        if (count == -1 || pendingCheck) checkStatusInternal();
        if (!current() || !towerAreaLoaded() || pendingCheck) return;
        BlockPos below = worldPosition.below();
        BlockEntity node = VisNodeFailures.existing(level, below);
        // Original checks BEFORE increment/decrement and BEFORE current power.
        if (status == 1 && count >= 1000 && node instanceof TileNode normal && normal.canTransduce()) {
            if (replaceNode(normal, true) && current()) {
                checkStatusInternal();
                conversionEffect();
            }
        }
        if (!current()) return;
        node = VisNodeFailures.existing(level, below);
        if (status == 2 && count <= 50 && node instanceof TileNodeEnergized source && !source.isQuarantined()) {
            if (replaceNode(source, false) && current()) {
                // Unlike the conversion branch, original reversion does NOT call
                // checkStatus; status0 then lets the same tick decrement50->49.
                status = 0;
                conversionEffect();
            }
        }
        if (!current() || !towerAreaLoaded()) return;
        boolean powered = isPowered();
        if (!current() || !towerAreaLoaded() || isPowered() != powered || !current()) return;
        if (status == 0 || !powered) {
            if (count > 0) { --count; dirtyCount(); }
        } else if (count < 1000) {
            ++count;
            int afterCount = count;
            node = VisNodeFailures.existing(level, below);
            if (node instanceof TileNode normal && normal.canTransduce()) {
                Aspect[] keys = normal.getAspects().getAspects(); // Includes retained ZERO keys.
                if (keys.length > 0) {
                    Aspect selected = keys[level.random.nextInt(keys.length)];
                    if (current() && count == afterCount && VisNodeFailures.existing(level, below) == normal && normal.canTransduce()) {
                        normal.takeFromContainer(selected, 1); // Attempt1, not guaranteed payment, not1000 upfront.
                        if (current() && VisNodeFailures.installed(normal) && (count % 5 == 0 || normal.getAspects().visSize() == 0))
                            level.sendBlockUpdated(below, normal.getBlockState(), normal.getBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
            if (current()) dirtyCount();
        }
    }
    private void dirtyCount() {
        if (notifying || !current()) return;
        // Persist the original counter without inventing a per-tick comparator
        // notification (BlockEntity.setChanged would produce one).
        if (level instanceof ServerLevel server) {
            var chunk = server.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
            if (chunk != null) chunk.setUnsaved(true);
        }
    }
    private void notifyState() {
        if (level == null || level.isClientSide || notifying || !current()) return;
        notifying = true;
        try {
            setChanged();
            if (current()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        } finally { notifying = false; }
    }
    private void conversionEffect() {
        if (!current()) return;
        level.blockEvent(worldPosition, getBlockState().getBlock(), 10, 10);
        if (current()) notifyState();
    }

    private static final class Transition {
        final ServerLevel level;
        final BlockPos pos;
        final BlockState state;
        final BlockEntity old;
        final AspectList base, capacity;
        final NodeType type;
        final NodeModifier modifier;
        final boolean energizing;
        BlockEntity created;
        boolean claiming;
        Transition(ServerLevel level, BlockPos pos, BlockState state, BlockEntity old, AspectList base,
                   AspectList capacity, NodeType type, NodeModifier modifier, boolean energizing) {
            this.level = level; this.pos = pos; this.state = state; this.old = old; this.base = base;
            this.capacity = capacity; this.type = type; this.modifier = modifier; this.energizing = energizing;
        }
    }
    public static boolean isTransitionActive() { return TRANSITION.get() != null; }
    private boolean replaceNode(BlockEntity old, boolean energizing) {
        TileNode normal = energizing && old instanceof TileNode node ? node : null;
        if (normal != null && !normal.beginTransduction()) return false;
        try { return replaceCapturedNode(old, energizing); }
        finally { if (normal != null) normal.endTransduction(); }
    }
    private boolean replaceCapturedNode(BlockEntity old, boolean energizing) {
        if (!(level instanceof ServerLevel server) || !current() || !VisNodeFailures.installed(old) || isTransitionActive()) return false;
        AspectList base;
        NodeType type;
        NodeModifier modifier;
        if (energizing && old instanceof TileNode node) {
            if (!node.transductionCurrent()) return false;
            base = node.getAspectsBase(); type = node.getNodeType(); modifier = node.getNodeModifier();
        } else if (!energizing && old instanceof TileNodeEnergized node) {
            if (node.isConsumeActive() || node.isNodeTickActive() || node.isQuarantined()) return false;
            base = node.getAuraBase(); type = node.getNodeType(); modifier = node.getNodeModifier();
        } else return false;
        AspectList capacity;
        try {
            base = EnergizedNodeReducer.checkedCopy(base);
            capacity = energizing ? TileNodeEnergized.calculateCapacity(base, type, modifier, level.random) : new AspectList();
        } catch (IllegalArgumentException | ArithmeticException invalidAura) { return false; }
        if (!current() || !VisNodeFailures.installed(old)) return false;
        if (old instanceof TileNode node && (!node.transductionCurrent() || !node.getAspectsBase().aspects.equals(base.aspects)
                || node.getNodeType() != type || node.getNodeModifier() != modifier)) return false;
        if (old instanceof TileNodeEnergized node && (!node.getAuraBase().aspects.equals(base.aspects)
                || node.getNodeType() != type || node.getNodeModifier() != modifier)) return false;
        BlockState destination = energizing ? ThaumcraftNodeDevices.ENERGIZED_NODE.get().defaultBlockState() : ThaumcraftContent.AURA_NODE.get().defaultBlockState();
        Transition transition = new Transition(server, old.getBlockPos(), destination, old, base, capacity, type, modifier, energizing);
        TRANSITION.set(transition);
        try {
            // The destination constructor consumes the immutable prepared seed,
            // BEFORE placement/neighbor callbacks. Never initialize a BE found
            // afterwards: that BE could be a callback-created replacement.
            if (!current() || !VisNodeFailures.installed(old)) return false;
            server.setBlock(transition.pos, destination, Block.UPDATE_ALL);
        } catch (RuntimeException callbackFailure) {
            Thaumcraft.LOG.warn("Node transduction callback failed at {} (no rollback or replacement overwrite)", transition.pos, callbackFailure);
        } finally { TRANSITION.remove(); }
        return transition.created != null && VisNodeFailures.loaded(server, transition.pos)
                && VisNodeFailures.existing(server, transition.pos) == transition.created
                && server.getBlockState(transition.pos) == destination && VisNodeFailures.installed(transition.created);
    }
    static void initializePreparedEnergized(TileNodeEnergized node) {
        Transition transition = prepared(node, true);
        if (transition == null) return;
        node.initializeConverted(transition.base, transition.type, transition.modifier, transition.capacity);
        transition.created = node;
    }
    static boolean initializePreparedReversion(TileNode node) {
        Transition transition = prepared(node, false);
        if (transition == null) return false;
        node.initializeReverted(transition.base, transition.type, transition.modifier);
        transition.created = node;
        return true;
    }
    private static Transition prepared(BlockEntity node, boolean energizing) {
        Transition transition = TRANSITION.get();
        if (transition == null || transition.created != null || transition.claiming || transition.energizing != energizing
                || node.getLevel() != null || !transition.pos.equals(node.getBlockPos()) || transition.state != node.getBlockState()) return null;
        transition.claiming = true; // Fence even the factory's validation callbacks.
        if (!VisNodeFailures.loaded(transition.level, transition.pos) || transition.level.getBlockState(transition.pos) != transition.state
                || VisNodeFailures.existing(transition.level, transition.pos) != null) return null;
        return transition;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileNodeConverter tile) {
        if (!level.isClientSide || tile.level != level || !tile.worldPosition.equals(pos) || state != tile.getBlockState()
                || tile.ticking || tile.checking || tile.notifying || tile.isRemoved() || tile.isQuarantined()) return;
        tile.ticking = true;
        try {
            if (!tile.current()) return;
            if (tile.count == -1 || tile.pendingCheck) tile.checkStatusInternal();
            if (!tile.current() || !tile.towerAreaLoaded() || tile.pendingCheck) return;
            boolean animate = false;
            if (tile.status == 0 || !tile.isPowered()) { if (tile.count > 0) { --tile.count; animate = true; } }
            else if (tile.count < 1000) { ++tile.count; animate = true; }
            if (animate && tile.count > 50 && tile.current()) tile.clientBolts();
        } finally { tile.ticking = false; }
    }
    private void clientBolts() {
        var random = level.random;
        if (random.nextBoolean()) {
            double x = (float) worldPosition.getX() + .25F + random.nextFloat() * .5F;
            double z = (float) worldPosition.getZ() + .25F + random.nextFloat() * .5F;
            if (clientEffects != null) clientEffects.bolt(level, x, worldPosition.getY() + .5, z,
                    worldPosition.getX() + .5, worldPosition.getY() - .5, worldPosition.getZ() + .5);
        }
        if (random.nextBoolean() && hasStabilizer()) {
            double x = (float) worldPosition.getX() + .25F + random.nextFloat() * .5F;
            double z = (float) worldPosition.getZ() + .25F + random.nextFloat() * .5F;
            if (clientEffects != null) clientEffects.bolt(level, x, worldPosition.getY() - 1.5, z,
                    worldPosition.getX() + .5, worldPosition.getY() - .5, worldPosition.getZ() + .5);
        }
    }
    @Override public boolean triggerEvent(int id, int data) {
        if (id == 10 && data == 10) {
            if (level != null && level.isClientSide && current() && clientEffects != null)
                clientEffects.burst(level, worldPosition.getX() + .5, worldPosition.getY() - .5, worldPosition.getZ() + .5, 1);
            return true;
        }
        return super.triggerEvent(id, data);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (ticking || checking || notifying || isTransitionActive() || VisNodeFailures.isMutationActive()
                || level != null && VisNetHandler.isConsumeActive(level)) throw new IllegalStateException("Converter reload cannot bypass an active native frame");
        try (var frame = VisNodeFailures.beginLoading(this)) { loadState(tag, registries); }
    }
    private void loadState(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        count = -1; status = 0; pendingCheck = false; rejected = null;
        if (tag.contains("RejectedConverter")) {
            rejected = tag.contains("RejectedConverter", Tag.TAG_COMPOUND) ? tag.getCompound("RejectedConverter").copy() : tag.copy();
            return;
        }
        if (!tag.contains("count", Tag.TAG_INT) || !tag.contains("status", Tag.TAG_INT)) { rejected = tag.copy(); return; }
        int savedCount = tag.getInt("count"), savedStatus = tag.getInt("status");
        if (savedCount < -1 || savedCount > 1000 || savedStatus < 0 || savedStatus > 2 || savedCount == -1 && savedStatus != 0) { rejected = tag.copy(); return; }
        count = savedCount; status = savedStatus;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("status", status);
        tag.putInt("count", count);
        if (rejected != null) tag.put("RejectedConverter", rejected.copy());
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
