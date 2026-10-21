package thaumcraft.common.tiles;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.lib.visnet.EnergizedNodeReducer;
import thaumcraft.common.lib.visnet.VisNodeFailures;
import thaumcraft.common.tiles.TileNode.NodeModifier;
import thaumcraft.common.tiles.TileNode.NodeType;

/** Original TileNodeEnergized35-175: renewable per-ACTUAL-source-tick throughput,
 * not finite fuel, wand whole-vis or typed essentia. Pulse stock is NEVER saved. */
public final class TileNodeEnergized extends TileVisNode implements IAspectContainer {
    private AspectList auraBase = new AspectList(), visBase = new AspectList(), vis = new AspectList();
    private NodeType nodeType = NodeType.NORMAL;
    private NodeModifier nodeModifier;
    private String nodeId = "blank"; // Original converter does NOT copy the ordinary node's id.
    private long refillEpoch;
    private boolean initialized, ticking, notifying, failed, lifecycle;
    private CompoundTag rejected;

    public TileNodeEnergized(BlockPos pos, BlockState state) {
        super(ThaumcraftNodeDevices.ENERGIZED_NODE_BLOCK_ENTITY.get(), pos, state);
        TileNodeConverter.initializePreparedEnergized(this);
    }
    @Override public int getRange() { return 8; }
    @Override public boolean isSource() { return true; }
    @Override protected boolean isNetworkEnabled() { return initialized && !isQuarantined() && !lifecycle; }
    @Override public byte getAttunement() { return -1; }
    public AspectList getAuraBase() { return auraBase.copy(); }
    @Override public AspectList getAspects() { return visBase.copy(); }
    public AspectList getVisBase() { return visBase.copy(); }
    public AspectList getAvailableVis() { return vis.copy(); }
    public long getRefillEpoch() { return refillEpoch; }
    public NodeType getNodeType() { return nodeType; }
    public NodeModifier getNodeModifier() { return nodeModifier; }
    public String getNodeId() { return nodeId; }
    public boolean isQuarantined() { return rejected != null || failed; }
    @Override public void setRemoved() {
        if (lifecycle) return;
        lifecycle = true;
        try {
            requireLifecycleThread(level);
            vis = new AspectList(); refillEpoch = 0;
            super.setRemoved();
        } finally { lifecycle = false; }
    }
    @Override public void clearRemoved() {
        if (lifecycle) return;
        lifecycle = true;
        try {
            requireLifecycleThread(level);
            vis = new AspectList(); refillEpoch = 0;
            super.clearRemoved();
        } finally { lifecycle = false; }
    }
    @Override public void setLevel(Level next) {
        if (lifecycle) return;
        lifecycle = true;
        try {
            requireLifecycleThread(level); requireLifecycleThread(next);
            if (level != next) { vis = new AspectList(); refillEpoch = 0; }
            super.setLevel(next);
        } finally { lifecycle = false; }
    }
    private static void requireLifecycleThread(Level level) {
        if (level == null || level.isClientSide) return;
        if (!(level instanceof ServerLevel server)) throw new IllegalStateException("Energized lifecycle requires a server level");
        var owner = server.getServer();
        if (owner == null || !owner.isSameThread()) throw new IllegalStateException("Energized lifecycle requires the server thread");
        // Retirement must still work AFTER the server removes this level from
        // its live world map. Ticks/debits, unlike retirement, require identity.
    }

    /** Detached initialization only. A live source's capacity is not an arbitrary
     * accepted put/grant API, and the supplied mutable list is never retained. */
    @Override public void setAspects(AspectList values) {
        requireDetached();
        AspectList checked = EnergizedNodeReducer.checkedCopy(values);
        EnergizedNodeReducer.reduceToPrimals(checked);
        auraBase = checked;
        initialized = true;
        visBase = new AspectList();
        vis = new AspectList();
    }
    public void setNodeType(NodeType type) {
        requireDetached(); nodeType = java.util.Objects.requireNonNull(type); visBase = new AspectList(); vis = new AspectList(); refillEpoch = 0;
    }
    public void setNodeModifier(NodeModifier modifier) {
        requireDetached(); nodeModifier = modifier; visBase = new AspectList(); vis = new AspectList(); refillEpoch = 0;
    }
    private void requireDetached() {
        if (level != null || ticking || notifying || lifecycle || isConsumeActive() || VisNodeFailures.isMutationActive() || isQuarantined())
            throw new IllegalStateException("Energized aura initialization requires a detached inactive node");
    }
    void initializeConverted(AspectList base, NodeType type, NodeModifier modifier, AspectList capacity) {
        if (level != null || initialized) throw new IllegalStateException("Already initialized energized node");
        auraBase = EnergizedNodeReducer.checkedCopy(base);
        nodeType = java.util.Objects.requireNonNull(type);
        nodeModifier = modifier;
        visBase = EnergizedNodeReducer.checkedCopy(capacity);
        validateCapacity(auraBase, visBase, type, modifier);
        vis = new AspectList(); // Conversion setup is NOT the first source refill.
        initialized = true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileNodeEnergized source) {
        if (level.isClientSide || source.level != level || !source.worldPosition.equals(pos) || source.getBlockState() != state
                || source.ticking || source.notifying || source.lifecycle || source.isRemoved() || source.isQuarantined() || !source.initialized
                || source.isConsumeActive() || source.isNodeTickActive() || VisNodeFailures.isMutationActive()) return;
        try (var frame = VisNodeFailures.beginMutation(source)) {
            if (frame == null) return;
            source.ticking = true;
            try {
                if (!source.current() || !VisNodeFailures.checkTower(source) || !source.current()) return;
                TileVisNode.tick(level, pos, state, source); // Original super BEFORE reset/setup/refill.
                if (!source.current() || source.isConsumeActive() || source.isQuarantined()) return;
                if (source.nodeType == NodeType.UNSTABLE) {
                    int reset = level.random.nextInt(500);
                    if (!source.current()) return;
                    if (reset == 1) source.visBase = new AspectList();
                }
                boolean setup = source.visBase.size() == 0 && source.auraBase.size() > 0;
                if (setup) {
                    AspectList capacity;
                    try { capacity = calculateCapacity(source.auraBase, source.nodeType, source.nodeModifier, level.random); }
                    catch (IllegalArgumentException | ArithmeticException invalidAura) {
                        source.rejected = new CompoundTag();
                        source.rejected.putString("reason", "invalid live aspect graph or unrepresentable energized aura");
                        source.vis = new AspectList();
                        source.removeThisNode();
                        source.setChanged();
                        return;
                    }
                    if (!source.current() || source.isQuarantined()) return;
                    source.visBase = capacity;
                }
                // Commit the complete new epoch BEFORE any dirty/network callback.
                source.vis = source.visBase.copy();
                ++source.refillEpoch;
                if (setup) source.notifyCapacity();
            } finally { source.ticking = false; }
        }
    }
    private boolean current() {
        return !isQuarantined() && !lifecycle && VisNodeFailures.installed(this) && isCurrentNode()
                && getBlockState().is(ThaumcraftNodeDevices.ENERGIZED_NODE.get());
    }
    @Override protected int consumeFromSource(Aspect aspect, int amount) {
        if (!isSourceDebitActive() || amount <= 0 || !EnergizedNodeReducer.canonical(aspect) || !aspect.isPrimal()
                || !initialized || isQuarantined() || ticking || notifying || lifecycle || VisNodeFailures.isMutationActive()) return 0;
        long epoch = refillEpoch;
        if (!current() || !VisNodeFailures.isTowerCoherent(this) || !current() || refillEpoch != epoch) return 0;
        int drained = Math.min(vis.getAmount(aspect), amount);
        // NO call-outs between this raw debit and the actual committed report.
        if (drained > 0) vis.reduce(aspect, drained);
        return drained;
    }
    /** Read-only availability of the same stock the debit would take. */
    @Override protected int availableFromSource(Aspect aspect) {
        if (aspect == null || !EnergizedNodeReducer.canonical(aspect) || !aspect.isPrimal()
                || !initialized || isQuarantined() || ticking || notifying || lifecycle || VisNodeFailures.isMutationActive()) return 0;
        if (!current() || !VisNodeFailures.isTowerCoherent(this) || !current()) return 0;
        return vis.getAmount(aspect);
    }
    /** Only VisNodeFailures can mint this identity-bound, active permission. */
    public boolean retireForFailure(VisNodeFailures.Failure operation) {
        if (operation == null || !operation.authorizes(this) || failed || rejected != null) return false;
        failed = true;
        vis = new AspectList();
        return true;
    }
    private void notifyCapacity() {
        if (notifying) return;
        notifying = true;
        try {
            setChanged();
            if (current()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        } finally { notifying = false; }
    }

    static AspectList calculateCapacity(AspectList base, NodeType type, NodeModifier modifier, RandomSource random) {
        AspectList reduced = EnergizedNodeReducer.reduceToPrimals(base), out = new AspectList();
        for (Aspect aspect : reduced.getAspects()) {
            int amount = baseRate(reduced.getAmount(aspect), modifier);
            if (type == NodeType.UNSTABLE) {
                if (random == null) throw new IllegalStateException("Unstable setup requires the native world RNG");
                amount += random.nextInt(5) - 2;
            }
            if (amount >= 1) out.merge(aspect, amount);
        }
        return out;
    }
    private static int baseRate(int amount, NodeModifier modifier) {
        if (modifier != null) {
            float factor = switch (modifier) { case BRIGHT -> 1.2F; case PALE -> .8F; case FADING -> .5F; };
            float adjusted = (float) amount * factor;
            if ((double) adjusted > Integer.MAX_VALUE || adjusted < 0) throw new IllegalArgumentException("Unrepresentable modified aura");
            amount = (int) adjusted; // Original float multiplication/truncation BEFORE sqrt.
        }
        // Original MathHelper.sqrt_double returns FLOAT, then floor_double.
        return (int) Math.floor((double) (float) Math.sqrt((double) amount));
    }
    private static void validateCapacity(AspectList base, AspectList capacity, NodeType type, NodeModifier modifier) {
        AspectList reduced = EnergizedNodeReducer.reduceToPrimals(base);
        // Empty AEB is the original setup-on-next-source-tick sentinel, not stock.
        if (capacity.size() == 0) {
            for (Aspect aspect : reduced.getAspects()) baseRate(reduced.getAmount(aspect), modifier);
            return;
        }
        var expectedOrder = new ArrayList<Aspect>();
        for (Aspect aspect : reduced.getAspects()) {
            int rate = baseRate(reduced.getAmount(aspect), modifier), saved = capacity.getAmount(aspect);
            int low = type == NodeType.UNSTABLE ? Math.max(0, rate - 2) : rate;
            int high = type == NodeType.UNSTABLE ? rate + 2 : rate;
            if (saved < low || saved > high) throw new IllegalArgumentException("Inconsistent energized capacity");
            if (saved > 0) expectedOrder.add(aspect);
        }
        if (!new ArrayList<>(capacity.aspects.keySet()).equals(expectedOrder)) throw new IllegalArgumentException("Unknown or unordered energized capacity");
    }

    // Original178-210 inert IAspectContainer quirks retained: in particular
    // addToContainer returns0 BUT NEVER changes aura, capacity or pulse stock.
    // This source is NOT an essentia transport/source adapter or credit receiver.
    @Override public boolean doesContainerAccept(Aspect aspect) { return false; }
    @Override public int addToContainer(Aspect aspect, int amount) { return 0; }
    @Override public boolean takeFromContainer(Aspect aspect, int amount) { return false; }
    @Override @Deprecated public boolean takeFromContainer(AspectList values) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return false; }
    @Override @Deprecated public boolean doesContainerContain(AspectList values) { return false; }
    @Override public int containerContains(Aspect aspect) { return 0; }

    private static AspectList readList(CompoundTag tag, String label) {
        if (!tag.contains(label, Tag.TAG_LIST)) throw new IllegalArgumentException("Missing " + label);
        ListTag list = (ListTag) tag.get(label);
        if (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND) throw new IllegalArgumentException("Invalid " + label);
        AspectList out = new AspectList();
        for (Tag value : list) {
            if (!(value instanceof CompoundTag entry) || !entry.contains("key", Tag.TAG_STRING) || !entry.contains("amount", Tag.TAG_INT)
                    || entry.getAllKeys().size() != 2) throw new IllegalArgumentException("Malformed aspect entry");
            Aspect aspect = Aspect.getAspect(entry.getString("key"));
            int amount = entry.getInt("amount");
            if (!EnergizedNodeReducer.canonical(aspect) || amount <= 0 || out.aspects.containsKey(aspect))
                throw new IllegalArgumentException("Unknown, duplicate or nonpositive aspect");
            out.add(aspect, amount);
        }
        return EnergizedNodeReducer.checkedCopy(out);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (ticking || notifying || lifecycle || isNodeTickActive() || isConsumeActive() || VisNodeFailures.isMutationActive() || TileNodeConverter.isTransitionActive())
            throw new IllegalStateException("Energized reload cannot bypass an active native frame");
        try (var frame = VisNodeFailures.beginLoading(this)) { loadState(tag, registries); }
    }
    private void loadState(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        auraBase = new AspectList(); visBase = new AspectList(); vis = new AspectList();
        refillEpoch = 0; initialized = false; failed = false; rejected = null;
        nodeType = NodeType.NORMAL; nodeModifier = null; nodeId = "blank";
        try {
            if (tag.contains("RejectedEnergized")) throw new IllegalArgumentException("Previously quarantined node");
            if (!tag.contains("type", Tag.TAG_BYTE) || !tag.contains("modifier", Tag.TAG_BYTE) || !tag.contains("nodeId", Tag.TAG_STRING))
                throw new IllegalArgumentException("Missing energized node fields");
            int type = tag.getByte("type"), modifier = tag.getByte("modifier");
            if (type < 0 || type >= NodeType.values().length || modifier < -1 || modifier >= NodeModifier.values().length
                    || tag.getString("nodeId").isEmpty() || tag.getString("nodeId").length() > 256) throw new IllegalArgumentException("Invalid energized node fields");
            NodeType nt = NodeType.values()[type];
            NodeModifier nm = modifier == -1 ? null : NodeModifier.values()[modifier];
            AspectList base = readList(tag, "Aspects"), capacity = readList(tag, "AEB");
            validateCapacity(base, capacity, nt, nm);
            auraBase = base; visBase = capacity; nodeType = nt; nodeModifier = nm; nodeId = tag.getString("nodeId");
            initialized = true;
        } catch (RuntimeException invalid) {
            rejected = tag.contains("RejectedEnergized", Tag.TAG_COMPOUND) ? tag.getCompound("RejectedEnergized").copy() : tag.copy();
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        auraBase.writeToNBT(tag);
        visBase.writeToNBT(tag, "AEB");
        tag.putString("nodeId", nodeId);
        tag.putByte("type", (byte) nodeType.ordinal());
        tag.putByte("modifier", nodeModifier == null ? (byte) -1 : (byte) nodeModifier.ordinal());
        if (rejected != null) tag.put("RejectedEnergized", rejected.copy());
        else if (failed || !initialized) {
            CompoundTag invalid = new CompoundTag();
            invalid.putString("reason", failed ? "retired failure source" : "uninitialized energized node");
            tag.put("RejectedEnergized", invalid);
        }
        // No pulse/epoch fields: a restarted source begins with ZERO available.
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
