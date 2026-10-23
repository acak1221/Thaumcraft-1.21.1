package thaumcraft.api.visnet;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.WorldCoordinates;
import thaumcraft.api.aspects.Aspect;

/** Original TileVisNode's first/40-tick graph phase, with native identity,
 * lifecycle and consume-frame fences. Links are weak, transient and reciprocal. */
public abstract class TileVisNode extends BlockEntity {
    private WeakReference<TileVisNode> parent;
    private final List<WeakReference<TileVisNode>> children = new ArrayList<>();
    private int nodeCounter;
    private long attachmentVersion;
    private boolean registered, nodeTickActive, notifying, removing, settingParent, sourceDebitActive;
    private Level tickLevel;
    private BlockState tickState;
    private WorldCoordinates clientParent;
    private boolean clientLinksRejected;
    public boolean nodeRefresh;

    protected TileVisNode(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    /** Returns this node's connection radius in blocks; nonpositive ranges cannot establish a usable graph link. */
    public abstract int getRange();
    public abstract boolean isSource();
    /** Returns the dimensionless channel token; the base value -1 is uncolored/unattuned, without mutation. */
    public byte getAttunement() { return -1; }
    protected boolean isNetworkEnabled() { return true; }
    /** Returns a new immutable location in block coordinates, or null when unattached; does not alter graph state. */
    public final WorldCoordinates getLocation() { return level == null ? null : new WorldCoordinates(level.dimension(), worldPosition); }
    /** Returns the count of eligible node-tick phases entered, in game ticks, reset on level change/reload; no mutation. */
    public final int getNodeCounter() { return nodeCounter; }
    public final long getAttachmentVersion() { return attachmentVersion; }
    /** Returns whether a node tick, notification, removal or parent-change frame is active; no mutation. */
    public final boolean isNodeTickActive() { return nodeTickActive || notifying || removing || settingParent; }
    /** Returns whether this level has an active vis consume frame; an unattached node returns false. */
    public final boolean isConsumeActive() { return VisNetHandler.isConsumeActive(level); }
    protected final boolean isSourceDebitActive() { return sourceDebitActive; }
    protected final boolean isCurrentNode() { return VisNetHandler.isCurrent(this); }
    /** Default identity is strict. A native display-only state adapter may
     * accept its own non-routing hint without accepting changed orientation. */
    protected boolean matchesTickState(BlockState expected) { return getBlockState() == expected; }
    final boolean hasTickIdentity() { return !nodeTickActive || level == tickLevel && matchesTickState(tickState); }

    /** Raw source hook: debit before return, without notification callbacks.
     * Alleging isSource=true without a real debit is an error, not a producer. */
    protected int consumeFromSource(Aspect aspect, int amount) { throw new IllegalStateException("Vis source has no native debit implementation"); }
    /** Read-only availability of the source for an aspect; the default source holds none.
     * A source that can debit has to report the same amount here, so that a dry
     * run of a drain answers exactly what the real drain would take. */
    protected int availableFromSource(Aspect aspect) { return 0; }
    public final int consumeVis(Aspect aspect, int amount) { return VisNetHandler.consume(this, aspect, amount); }
    /** Default no-op effect notification, accepting null aspect without sending packets; subclasses may supply effects. */
    public void triggerConsumeEffect(Aspect aspect) {}
    /** Default no-op link-change callback; graph notification may already have marked/synchronized the block before invoking it. */
    public void parentChanged() {}

    public final WeakReference<TileVisNode> getParent() {
        TileVisNode value = VisNetHandler.parentFor(this);
        return value == null ? null : new WeakReference<>(value);
    }
    /** Returns an immutable snapshot of live child references; clearing these new weak references cannot remove stored edges. */
    public final List<WeakReference<TileVisNode>> getChildren() {
        List<WeakReference<TileVisNode>> snapshot = new ArrayList<>();
        for (WeakReference<TileVisNode> ref : children) {
            TileVisNode child = ref.get();
            if (child != null) snapshot.add(new WeakReference<>(child));
        }
        return List.copyOf(snapshot); // Clearing a returned WeakReference cannot edit an edge.
    }
    public final WeakReference<TileVisNode> getRootSource() {
        TileVisNode root = VisNetHandler.rootFor(this);
        return root == null ? null : new WeakReference<>(root);
    }
    public final void setParent(WeakReference<TileVisNode> ref) {
        if (settingParent || isConsumeActive()) return;
        settingParent = true;
        try {
            TileVisNode next = ref == null ? null : ref.get();
            VisNetHandler.setParent(this, next);
        } finally { settingParent = false; }
    }
    public final void removeThisNode() { VisNetHandler.removeNode(this, true); }

    public static void tick(Level level, BlockPos pos, BlockState state, TileVisNode node) {
        if (level.isClientSide || node.level != level || !node.worldPosition.equals(pos) || node.getBlockState() != state
                || node.isRemoved() || node.isNodeTickActive() || node.isConsumeActive()) return;
        node.nodeTickActive = true; node.tickLevel = level; node.tickState = state;
        try {
            if (VisNetHandler.canTick(node) && node.isCurrentNode()) {
                int phase = node.nodeCounter++;
                if (phase % 40 == 0 || node.nodeRefresh) VisNetHandler.refresh(node);
            }
        } finally { node.nodeTickActive = false; node.tickLevel = null; node.tickState = null; }
    }

    @Override public void setRemoved() {
        if (removing) return;
        removing = true;
        try {
            VisNetHandler.requireThread(level);
            super.setRemoved(); // Retire BEFORE any detach/notification callback.
            VisNetHandler.removeNode(this, true);
        } finally { removing = false; }
    }
    @Override public void clearRemoved() { super.clearRemoved(); registered = false; nodeRefresh = true; }
    @Override public void setLevel(Level next) {
        if (level != next && isConsumeActive()) throw new IllegalStateException("Vis node relocation during an active consume frame");
        if (level != null && level != next) {
            VisNetHandler.requireThread(level);
            VisNetHandler.removeNode(this, false);
            nodeCounter = 0;
        }
        super.setLevel(next);
    }

    // Package-only raw graph operations. No weak ref is dereferenced twice.
    final TileVisNode rawParent() { return parent == null ? null : parent.get(); }
    final List<TileVisNode> rawChildren() {
        List<TileVisNode> list = new ArrayList<>();
        for (WeakReference<TileVisNode> ref : children) { TileVisNode child = ref.get(); if (child != null) list.add(child); }
        return list;
    }
    final void rawParent(TileVisNode next) { parent = next == null ? null : new WeakReference<>(next); ++attachmentVersion; }
    final void rawAddChild(TileVisNode child) {
        children.removeIf(ref -> { TileVisNode value = ref.get(); return value == null || value == child; });
        children.add(new WeakReference<>(child)); ++attachmentVersion;
    }
    final void rawRemoveChild(TileVisNode child) {
        if (children.removeIf(ref -> { TileVisNode value = ref.get(); return value == null || value == child; })) ++attachmentVersion;
    }
    final void rawClearChildren() { if (!children.isEmpty()) { children.clear(); ++attachmentVersion; } }
    final boolean registered() { return registered; }
    final void registered(boolean value) { registered = value; }
    final WorldCoordinates clientParent() { return clientLinksRejected ? null : clientParent; }
    final int debit(Aspect aspect, int amount) {
        sourceDebitActive = true;
        try { return consumeFromSource(aspect, amount); }
        finally { sourceDebitActive = false; }
    }
    final void notifyLinkChange() {
        if (notifying) return;
        notifying = true;
        try {
            Level world = level; BlockState state = getBlockState();
            if (VisNetHandler.isCurrent(this)) {
                setChanged();
                if (VisNetHandler.isCurrent(this) && level == world && getBlockState() == state && !world.isClientSide) world.sendBlockUpdated(worldPosition, state, state, 3);
            }
            parentChanged();
        } finally { notifying = false; }
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (isConsumeActive() || sourceDebitActive || isNodeTickActive()) throw new IllegalStateException("Vis node reload during active graph/consume callback");
        VisNetHandler.requireThread(level);
        VisNetHandler.removeNode(this, false);
        super.loadAdditional(tag, registries);
        parent = null; children.clear(); registered = false; nodeRefresh = false; nodeCounter = 0; ++attachmentVersion;
        clientParent = null; clientLinksRejected = false;
        // Disk/server load never trusts a persisted link. Only live client tags
        // can describe a visual parent, with native full coordinates/dimension.
        if (level != null && level.isClientSide && tag.contains("HasVisParent")) {
            if (!tag.contains("HasVisParent", Tag.TAG_BYTE) || tag.getByte("HasVisParent") < 0 || tag.getByte("HasVisParent") > 1) clientLinksRejected = true;
            else if (tag.getBoolean("HasVisParent")) {
                if (!tag.contains("VisParent", Tag.TAG_COMPOUND)) clientLinksRejected = true;
                else {
                    clientParent = WorldCoordinates.readNBT(tag.getCompound("VisParent")).orElse(null);
                    int range = getRange();
                    clientLinksRejected = clientParent == null || clientParent.dimension != level.dimension()
                            || !level.isInWorldBounds(clientParent.pos()) || range <= 0
                            || clientParent.getDistanceSquared(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) <= 0
                            || clientParent.getDistanceSquared(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) > (double) range * range;
                }
            } else if (tag.contains("VisParent")) clientLinksRejected = true;
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        TileVisNode value = VisNetHandler.parentFor(this);
        tag.putBoolean("HasVisParent", value != null);
        if (value != null) { CompoundTag at = new CompoundTag(); value.getLocation().writeNBT(at); tag.put("VisParent", at); }
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
