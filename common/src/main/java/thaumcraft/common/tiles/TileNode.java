package thaumcraft.common.tiles;

import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;

/**
 * TC4 normal-node state and lifecycle. Source: original TileNode.setAspects,
 * handleDischarge, handleRecharge and onUsingWandTick. Basic stabilizer locking
 * and supported NORMAL/FADING repair are included; special-node ecology,
 * advanced stabilization and jars remain separate unimplemented units. Real
 * energized conversion/network routing live in the separate M09b node family.
 */
public final class TileNode extends BlockEntity implements IAspectContainer {
    // Storage vocabulary, not an implementation of the full thaumcraft.api.nodes API.
    public enum NodeType { NORMAL, UNSTABLE, DARK, TAINTED, HUNGRY, PURE }
    public enum NodeModifier { BRIGHT, PALE, FADING }

    private AspectList aspects = new AspectList();
    private AspectList aspectsBase = new AspectList();
    private String scanId = UUID.randomUUID().toString();
    private NodeType nodeType = NodeType.NORMAL;
    private NodeModifier nodeModifier;
    private boolean natural;
    private int count;
    private byte nodeLock;
    private int regeneration = -1;
    private int rechargeWait;
    private long lastActive;
    private boolean catchUp;
    private boolean ticking, notifying, transducing;
    private Level tickingLevel;
    private BlockState tickingState;

    public TileNode(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // M09b's exact native replacement factory seeds ZERO current before any
        // placement callback; never refill a reverted node from this fixture.
        if (TileNodeConverter.initializePreparedReversion(this)) return;
        // Retained creative fixture only. Worldgen ALWAYS replaces both lists.
        for (Aspect aspect : Aspect.getPrimalAspects()) aspects.add(aspect, 25);
        aspectsBase = aspects.copy();
    }
    @Override public AspectList getAspects() { return aspects.copy(); }
    public AspectList getAspectsBase() { return aspectsBase.copy(); }
    public int getNodeVisBase(Aspect aspect) { return aspectsBase.getAmount(aspect); }
    public String getScanId() { return scanId; }
    public NodeType getNodeType() { return nodeType; }
    public NodeModifier getNodeModifier() { return nodeModifier; }
    public boolean isNatural() { return natural; }
    public byte getLock() { return nodeLock; }
    public int getTickCount() { return count; }
    /** Actual cached interval, including -1; reading never initializes it. */
    public int getRegenerationTicks() { return regeneration; }
    public int getRechargeWait() { return rechargeWait; }

    boolean canTransduce() { return !transducing && !ticking && !notifying && current(); }
    boolean beginTransduction() {
        if (transducing || ticking || notifying) return false;
        transducing = true;
        try { if (current()) return true; }
        catch (RuntimeException | Error failure) { transducing = false; throw failure; }
        transducing = false;
        return false;
    }
    boolean transductionCurrent() { return transducing && !ticking && !notifying && current(); }
    void endTransduction() { transducing = false; }
    /** Detached native reversion only. Original converter69-72 sets BASE then
     * drains every current key toZERO; UUID is freshly generated, not preserved. */
    void initializeReverted(AspectList base, NodeType type, NodeModifier modifier) {
        if (level != null) throw new IllegalStateException("Reversion initialization requires a detached node");
        aspectsBase = base.copy();
        aspects = new AspectList();
        for (Aspect aspect : base.getAspects()) aspects.add(aspect, 0);
        nodeType = type;
        nodeModifier = modifier;
    }

    /** Like original setAspects: this initializes BOTH current vis and capacity. */
    @Override public void setAspects(AspectList values) {
        if (transducing) throw new IllegalStateException("Node aura is captured by an active transduction");
        aspects = new AspectList();
        if (values != null) for (Aspect aspect : values.getAspects()) {
            int amount = Math.max(0, values.getAmount(aspect));
            aspects.add(aspect, amount);
        }
        aspectsBase = aspects.copy();
        changed();
    }

    /** Original TileNode:201-210: refill current vis only, up to its base. */
    @Override public int addToContainer(Aspect aspect, int amount) {
        if (transducing) return amount;
        int left = Math.max(0, amount + aspects.getAmount(aspect) - aspectsBase.getAmount(aspect));
        aspects.add(aspect, amount - left);
        changed();
        return left;
    }

    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        if (transducing) return false;
        boolean taken = aspects.reduce(aspect, amount);
        if (taken) changed(); // Retain zero keys for the existing recharge/prune lifecycle.
        return taken;
    }

    // Original TileNode:463-484 deliberately does NOT implement useful contains
    // queries or bulk extraction, despite accepting every aspect for refilling.
    @Override public boolean doesContainerAccept(Aspect aspect) { return true; }
    @Override @Deprecated public boolean takeFromContainer(AspectList values) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return false; }
    @Override @Deprecated public boolean doesContainerContain(AspectList values) { return false; }
    @Override public int containerContains(Aspect aspect) { return 0; }

    /** Called only after the real feature has placed the existing aura-node block. */
    public void initializeNatural(AspectList values, NodeModifier modifier) {
        if (transducing) throw new IllegalStateException("Node aura is captured by an active transduction");
        natural = true;
        nodeType = NodeType.NORMAL;
        nodeModifier = modifier;
        count = 0;
        nodeLock = 0;
        regeneration = -1;
        rechargeWait = 0;
        lastActive = 0;
        catchUp = false;
        setAspects(values);
    }

    /** Wand API: compounds stay in the node, but TC4 wands accept only primals. */
    public int takeVis(String tag, int amount) {
        if (transducing) return 0;
        Aspect aspect = Aspect.getAspect(tag);
        if (aspect == null || !aspect.isPrimal() || amount <= 0) return 0;
        int taken = Math.min(amount, aspects.getAmount(aspect));
        // reduce retains a zero entry. remove would bypass original pruning and
        // permanently prevent that aspect from recharging even before pruning.
        if (taken > 0) { aspects.reduce(aspect, taken); changed(); }
        return taken;
    }
    public void drainIntoWand(ServerPlayer player, ItemStack stack) {
        if (transducing) return;
        if (level == null || level.isClientSide || player.level() != level
                || !player.canInteractWithBlock(worldPosition, 0.0D)
                || !(stack.getItem() instanceof ItemWandCasting)) return;
        var knowledge = KnowledgeAccess.get(player);
        var material = ItemWandCasting.data(stack);
        boolean preserve = !player.isShiftKeyDown() && knowledge.isResearchComplete("NODEPRESERVE")
                && !material.rod().equals("wood") && !material.cap().equals("iron");
        ArrayList<Aspect> eligible = new ArrayList<>();
        for (Aspect aspect : aspects.getPrimalAspects()) {
            if (aspects.getAmount(aspect) > (preserve ? 1 : 0)
                    && ItemWandCasting.getVis(stack, aspect.getTag()) < ItemWandCasting.getMaxVis(stack)) eligible.add(aspect);
        }
        if (eligible.isEmpty()) return;
        Aspect aspect = eligible.get(level.random.nextInt(eligible.size()));
        int tap = 1;
        if (knowledge.isResearchComplete("NODETAPPER1")) tap++;
        if (knowledge.isResearchComplete("NODETAPPER2")) tap++;
        int available = Math.min(aspects.getAmount(aspect), tap);
        if (preserve && available == aspects.getAmount(aspect)) available--;
        if (available <= 0) return;
        int left = ItemWandCasting.addRealVis(stack, aspect.getTag(), available * 100, true);
        // Original addVis returns leftover / 100, so a partially filled final
        // unit consumes one node vis unit; preserve that TC4 rounding rule.
        int transferred = available - left / 100;
        if (transferred > 0) takeVis(aspect.getTag(), transferred);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileNode node) {
        if (level.isClientSide || node.isRemoved() || node.nodeType != NodeType.NORMAL || node.ticking || node.notifying || node.transducing
                || node.level != level || !node.worldPosition.equals(pos) || state != node.getBlockState()) return;
        node.tickingLevel = level;
        node.tickingState = state;
        node.ticking = true;
        try {
            // Acquire the reentry fence BEFORE even an overridable Level query.
            if (!(level instanceof ServerLevel server)) throw new IllegalStateException("Node ticks require the server thread");
            var owner = server.getServer();
            if (owner == null || !owner.isSameThread()) throw new IllegalStateException("Node ticks require the server thread");
            if (owner.getLevel(server.dimension()) != server) return; // A forged Level subclass is not a native server world.
            if (!node.current()) return;
            ++node.count;
            node.checkLock(); // Original129-147: AFTER count++, BEFORE discharge/recharge.
            if (!node.current()) return;
            boolean change = node.handleDischarge();
            if (!node.current()) return;
            change |= node.rechargeAndPrune();
            if (!node.current()) return;
            change |= node.repairFading();
            if (change && node.current()) node.changed();
        } finally { node.ticking = false; node.tickingLevel = null; node.tickingState = null; }
    }

    /** Original824-842: block+metadata only; no stabilizer-BE admission gate.
     * Native min build height replaces old y>0. Power/removal keeps the original
     * cached policy until the first/next50 tick, never an immediate neighbor hook. */
    private void checkLock() {
        if (!(count <= 1 || count % 50 == 0) || worldPosition.getY() <= level.getMinBuildHeight()
                || !getBlockState().is(ThaumcraftContent.AURA_NODE.get())) return;
        BlockPos below = worldPosition.below();
        if (!level.hasChunkAt(below) || !TileNodeStabilizer.powerAreaLoaded(level, below)) return;
        boolean powered = level.hasNeighborSignal(below);
        BlockState support = powered ? null : level.getBlockState(below);
        byte next = !powered && support.is(ThaumcraftNodeDevices.NODE_STABILIZER.get()) ? (byte) 1 : 0;
        // Queries are call-outs, not permission to modify a retired/replaced node.
        // Abandon a changed observation, without a new between-samples lock poll.
        if (!current() || !level.hasChunkAt(below) || !TileNodeStabilizer.powerAreaLoaded(level, below) || level.hasNeighborSignal(below) != powered
                || support != null && level.getBlockState(below) != support || !current()) return;
        if (next != nodeLock) { nodeLock = next; regeneration = -1; }
    }

    private boolean current() {
        Level world = level;
        BlockState state = getBlockState();
        return world != null && (!ticking || world == tickingLevel && state == tickingState) && !isRemoved() && state.is(ThaumcraftContent.AURA_NODE.get())
                && !world.isOutsideBuildHeight(worldPosition) && world.hasChunkAt(worldPosition)
                && world.getBlockEntity(worldPosition) == this && world.getBlockState(worldPosition) == state
                && !isRemoved() && level == world && getBlockState() == state;
    }

    /** A stronger nearby normal node can bully a weaker one, as in TC4. */
    private boolean handleDischarge() {
        if (nodeLock == 1 || nodeModifier == NodeModifier.FADING) return false;
        boolean shiny = nodeModifier == NodeModifier.BRIGHT;
        int interval = shiny ? 1 : nodeModifier == NodeModifier.PALE ? 3 : 2;
        if (count % interval != 0) return false;
        var random = level.random;
        int x = random.nextInt(5) - random.nextInt(5);
        int y = random.nextInt(5) - random.nextInt(5);
        int z = random.nextInt(5) - random.nextInt(5);
        if (nodeModifier == NodeModifier.PALE && random.nextBoolean()) return false;
        if (x == 0 && y == 0 && z == 0) return false;
        if (!current()) return false;
        BlockPos otherPos = worldPosition.offset(x, y, z);
        // Never load a neighbouring chunk to look for a node.
        if (level.isOutsideBuildHeight(otherPos) || !level.hasChunkAt(otherPos)
                || !(level.getBlockEntity(otherPos) instanceof TileNode other)
                || other.level != level || other.nodeType != NodeType.NORMAL || !other.current() || !current()) return false;
        if (other.transducing || other.nodeLock > 0) return false; // Original lock check BEFORE aspect-selection RNG; atomic capture cannot be bullied.
        int otherAverage = (other.aspects.visSize() + other.aspectsBase.visSize()) / 2;
        int thisAverage = (aspects.visSize() + aspectsBase.visSize()) / 2;
        if (otherAverage >= thisAverage || other.aspects.size() == 0) return false;
        Aspect aspect = other.aspects.getAspects()[random.nextInt(other.aspects.size())];
        if (!current() || !other.current() || other.transducing || other.nodeLock > 0) return false;
        if (!other.aspects.reduce(aspect, 1)) return false;
        if (aspects.getAmount(aspect) < getNodeVisBase(aspect)) {
            addCurrent(aspect, 1);
        } else if (random.nextInt(1 + (int)(getNodeVisBase(aspect) / (shiny ? 1.5 : 1.0))) == 0) {
            aspectsBase.add(aspect, 1);
            if (nodeModifier == NodeModifier.PALE && random.nextInt(100) == 0) {
                nodeModifier = null;
                regeneration = -1;
            }
            if (random.nextInt(3) == 0) other.decreaseBase(aspect);
        }
        other.rechargeWait = other.regeneration / 2;
        other.changed();
        return true;
    }

    private int regenerationInterval() {
        int interval = nodeModifier == null ? 600 : switch (nodeModifier) {
            case BRIGHT -> 400;
            case PALE -> 900;
            case FADING -> 0;
        };
        return nodeLock == 1 ? interval * 2 : interval;
    }

    /** Original726-747's FADING branch ONLY. UNSTABLE/orbs and other special
     * nodes stay deferred. Original setNodeModifier250-252 only assigns the
     * modifier: retain cached FADING0 until a later native cache invalidation. */
    private boolean repairFading() {
        if (count % 100 != 0 || nodeModifier != NodeModifier.FADING || nodeLock == 0) return false;
        int before = count;
        if (level.random.nextInt(12500) != 69 || !current() || count != before || nodeModifier != NodeModifier.FADING || nodeLock == 0) return false;
        nodeModifier = NodeModifier.PALE;
        return true;
    }

    private void addCurrent(Aspect aspect, int amount) {
        int room = getNodeVisBase(aspect) - aspects.getAmount(aspect);
        if (room > 0) aspects.add(aspect, Math.min(room, amount));
    }

    private void decreaseBase(Aspect aspect) {
        // Original setNodeVisBase takes a short, including its narrowing quirk.
        aspectsBase.aspects.put(aspect, (int)(short)(getNodeVisBase(aspect) - 1));
    }

    private boolean restoreOne() {
        ArrayList<Aspect> depleted = new ArrayList<>();
        // Intentionally iterate CURRENT keys, not base keys. A pruned tag cannot
        // grow back just because a historical base entry remains in the save.
        for (Aspect aspect : aspects.getAspects()) {
            if (aspects.getAmount(aspect) < getNodeVisBase(aspect)) depleted.add(aspect);
        }
        if (depleted.isEmpty()) return false;
        addCurrent(depleted.get(level.random.nextInt(depleted.size())), 1);
        return true;
    }

    private boolean rechargeAndPrune() {
        if (regeneration < 0) regeneration = regenerationInterval();
        boolean change = false;
        if (catchUp) {
            catchUp = false;
            // Original wall-clock catch-up: one vis per interval * 75 ms,
            // bounded by base vis. This is NOT an unloaded full refill.
            long interval = regeneration * 75L;
            long amount = interval > 0 ? (System.currentTimeMillis() - lastActive) / interval : 0;
            int attempts = (int)Math.min(Math.max(0, amount), Math.max(0, aspectsBase.visSize()));
            for (int i = 0; i < attempts; i++) change |= restoreOne();
        }
        if (count % 1200 == 0) {
            for (Aspect aspect : aspects.getAspects()) {
                if (aspects.getAmount(aspect) > 0) continue;
                decreaseBase(aspect);
                regeneration = -1;
                change = true;
                if (level.random.nextInt(20) == 0 || getNodeVisBase(aspect) <= 0) {
                    aspects.remove(aspect);
                    if (level.random.nextInt(5) == 0) {
                        if (nodeModifier == NodeModifier.BRIGHT) nodeModifier = null;
                        else if (nodeModifier == null) nodeModifier = NodeModifier.PALE;
                        if (nodeModifier == NodeModifier.PALE && level.random.nextInt(5) == 0) {
                            nodeModifier = NodeModifier.FADING;
                        }
                    }
                    break;
                }
            }
            if (aspects.size() == 0) {
                // Original airy-node death, with no free block/item drop.
                level.removeBlock(worldPosition, false);
                return true;
            }
        }
        if (rechargeWait > 0) --rechargeWait;
        if (regeneration > 0 && rechargeWait == 0 && count % regeneration == 0) {
            lastActive = System.currentTimeMillis();
            setChanged();
            if (!current()) return change;
            change |= restoreOne();
        }
        return change;
    }

    private void changed() {
        if (notifying) return;
        notifying = true;
        try {
            setChanged();
            if (level != null && !level.isClientSide && current()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        } finally { notifying = false; }
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (TileNodeConverter.isTransitionActive() || thaumcraft.common.lib.visnet.VisNodeFailures.isMutationActive())
            throw new IllegalStateException("Node reload cannot bypass an active transduction frame");
        if (level != null && !level.isClientSide && (ticking || notifying || !(level instanceof ServerLevel server) || !server.getServer().isSameThread())) {
            throw new IllegalStateException("Node reload cannot bypass an active server tick or notification");
        }
        super.loadAdditional(tag, registries);
        aspects = new AspectList();
        aspects.readFromNBT(tag);
        aspectsBase = new AspectList();
        if (tag.contains("AspectsBase", Tag.TAG_LIST)) aspectsBase.readFromNBT(tag, "AspectsBase");
        else aspectsBase = aspects.copy(); // existing native checkpoint saves, not 1.7 migration
        String savedId = tag.getString("scanId");
        if (!savedId.isEmpty()) scanId = savedId;
        int type = tag.getByte("type");
        nodeType = type >= 0 && type < NodeType.values().length ? NodeType.values()[type] : NodeType.NORMAL;
        int modifier = tag.contains("modifier") ? tag.getByte("modifier") : -1;
        nodeModifier = modifier >= 0 && modifier < NodeModifier.values().length ? NodeModifier.values()[modifier] : null;
        natural = tag.getBoolean("natural");
        lastActive = tag.getLong("lastActive");
        count = 0;
        nodeLock = 0; // Transient policy; first real tick observes the loaded world.
        rechargeWait = 0;
        regeneration = -1;
        int interval = regenerationInterval();
        catchUp = interval > 0 && lastActive > 0 && System.currentTimeMillis() > lastActive + interval * 75L;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        aspects.writeToNBT(tag);
        aspectsBase.writeToNBT(tag, "AspectsBase");
        tag.putString("scanId", scanId);
        tag.putByte("type", (byte)nodeType.ordinal());
        tag.putByte("modifier", nodeModifier == null ? (byte)-1 : (byte)nodeModifier.ordinal());
        tag.putBoolean("natural", natural);
        tag.putLong("lastActive", lastActive);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
