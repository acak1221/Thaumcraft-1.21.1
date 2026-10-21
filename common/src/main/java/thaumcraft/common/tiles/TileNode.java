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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
import thaumcraft.common.lib.utils.AspectOrbQueue;
import thaumcraft.common.lib.research.AspectOrbRules;
import thaumcraft.common.lib.research.HungryNodeRules;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.ObjectAspectResolver;
import thaumcraft.common.lib.research.DeconstructionRules;
import thaumcraft.common.lib.world.HungryNodeTrace;

/**
 * TC4 NORMAL/UNSTABLE/HUNGRY node state and lifecycle. Source: original TileNode.setAspects,
 * handleDischarge, handleRecharge and onUsingWandTick. Basic stabilizer locking,
 * UNSTABLE emissions/FADING repair and HUNGRY entity/block ecology are included; other special-node ecology,
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
    private boolean ticking, notifying, transducing, pearlPlanning;
    private Level tickingLevel;
    private BlockState tickingState;
    private AspectOrbQueue.Prepared orbIntent;
    private AspectList orbStock;
    private Aspect orbAspect;
    private int orbBefore, orbTick;

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
    /** Original ScanManager235-274, reading current stock without consuming it. */
    public AspectList getScanAspects() {
        return thaumcraft.common.lib.research.NodeScanAspects.get(aspects.copy(), nodeType);
    }
    public NodeModifier getNodeModifier() { return nodeModifier; }
    public boolean isNatural() { return natural; }
    public byte getLock() { return nodeLock; }
    public int getTickCount() { return count; }
    /** Actual cached interval, including -1; reading never initializes it. */
    public int getRegenerationTicks() { return regeneration; }
    public int getRechargeWait() { return rechargeWait; }

    boolean canTransduce() { return !transducing && !ticking && !notifying && !pearlPlanning && current(); }
    boolean beginTransduction() {
        if (transducing || ticking || notifying || pearlPlanning) return false;
        transducing = true;
        try { if (current()) return true; }
        catch (RuntimeException | Error failure) { transducing = false; throw failure; }
        transducing = false;
        return false;
    }
    boolean transductionCurrent() { return transducing && !ticking && !notifying && !pearlPlanning && current(); }
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

    /**
     * Original TileJarNode:222-241 - the wand restores the stored node: its aspects,
     * its node type, its node modifier and its identity.
     */
    public void restoreFromJar(AspectList values, NodeType type, NodeModifier modifier, String id) {
        setAspects(values);
        nodeType = type == null ? NodeType.NORMAL : type;
        nodeModifier = modifier;
        if (id != null && !id.isEmpty()) scanId = id;
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
    public void initializeNatural(AspectList values, NodeModifier modifier) { initializeNatural(values, NodeType.NORMAL, modifier); }
    public void initializeNatural(AspectList values, NodeType type, NodeModifier modifier) {
        initializeGenerated(values, type, modifier);
    }

    /** Original createNodeAt, shared by natural, embedded and actual primal-generated nodes. */
    public void initializeGenerated(AspectList values, NodeType type, NodeModifier modifier) {
        if (level == null) { initializeGeneratedFields(values, type, modifier); return; }
        if (!(level instanceof ServerLevel server)) throw new IllegalStateException("Generated nodes require a native server context");
        initializeGenerated(values, type, modifier, server);
    }

    /** Explicit generation context avoids server chunk/packet call-outs from a WorldGenRegion worker. */
    public void initializeGenerated(AspectList values, NodeType type, NodeModifier modifier,
                                    net.minecraft.world.level.WorldGenLevel owner) {
        if (!tryInitializeGenerated(values, type, modifier, owner))
            throw new IllegalStateException("Generated node initialization requires its available current owner");
    }

    /** A runtime generator may encounter an existing node owned by an active
     * tick/pearl/conversion frame. Consume the generation roll, then refuse
     * initialization without stealing that aura or crashing the caller. */
    public boolean tryInitializeGenerated(AspectList values, NodeType type, NodeModifier modifier,
                                         net.minecraft.world.level.WorldGenLevel owner) {
        if (owner instanceof ServerLevel server) {
            if (!server.getServer().isSameThread() || server.getServer().getLevel(server.dimension()) != server)
                throw new IllegalStateException("Generated node initialization requires a native server thread");
        } else if (!(owner instanceof net.minecraft.server.level.WorldGenRegion)) {
            throw new IllegalStateException("Generated node initialization requires a native generation region");
        }
        if (ticking || notifying || transducing || pearlPlanning || orbIntent != null) return false;
        if (owner instanceof ServerLevel server) {
            if (level != server || !current()) return false;
            initializeGeneratedFields(values, type, modifier); changed();
        } else {
            if (isRemoved() || !owner.hasChunkAt(worldPosition)
                    || !owner.ensureCanWrite(worldPosition) || owner.getBlockEntity(worldPosition) != this
                    || owner.getBlockState(worldPosition) != getBlockState()
                    || !getBlockState().is(ThaumcraftContent.AURA_NODE.get()) && !getBlockState().is(ThaumcraftContent.SILVERWOOD_KNOT.get()))
                return false;
            initializeGeneratedFields(values, type, modifier);
            owner.getChunk(worldPosition).setUnsaved(true);
        }
        return true;
    }

    private void initializeGeneratedFields(AspectList values, NodeType type, NodeModifier modifier) {
        java.util.Objects.requireNonNull(type, "node type");
        if (ticking || notifying || transducing || pearlPlanning || orbIntent != null)
            throw new IllegalStateException("Node aura is captured by an active native lifecycle frame");
        natural = true; nodeType = type; nodeModifier = modifier;
        count = 0; nodeLock = 0; regeneration = -1; rechargeWait = 0; lastActive = 0; catchUp = false;
        aspects = new AspectList();
        if (values != null) for (Aspect aspect : values.getAspects()) aspects.add(aspect, Math.max(0, values.getAmount(aspect)));
        aspectsBase = aspects.copy();
    }

    /** Opaque original metadata3 pearl transition; only the exact paid native item receipt can apply it. */
    public static final class PrimalPearlChange {
        private final TileNode owner;
        private final ServerLevel world;
        private final BlockState state;
        private final AspectList currentRef, baseRef, beforeCurrent, beforeBase, afterCurrent, afterBase;
        private final NodeType type;
        private final NodeModifier beforeModifier, afterModifier;
        private final int tick;
        private boolean applied;
        private PrimalPearlChange(TileNode owner, ServerLevel world, BlockState state,
                                  AspectList currentRef, AspectList baseRef, AspectList beforeCurrent, AspectList beforeBase,
                                  AspectList afterCurrent, AspectList afterBase, NodeModifier beforeModifier, NodeModifier afterModifier) {
            this.owner = owner; this.world = world; this.state = state;
            this.currentRef = currentRef; this.baseRef = baseRef; this.beforeCurrent = beforeCurrent; this.beforeBase = beforeBase;
            this.afterCurrent = afterCurrent; this.afterBase = afterBase; this.beforeModifier = beforeModifier; this.afterModifier = afterModifier;
            this.type = owner.nodeType; this.tick = owner.count;
        }
    }
    public PrimalPearlChange preparePrimalPearl(RandomSource random, boolean researched) {
        if (random == null || pearlPlanning || ticking || notifying || transducing || orbIntent != null) return null;
        pearlPlanning = true;
        PrimalPearlChange prepared;
        try {
            if (!(level instanceof ServerLevel world) || !world.getServer().isSameThread()
                    || world.getServer().getLevel(world.dimension()) != world || !current()) return null;
            AspectList currentRef = aspects, baseRef = aspectsBase;
            AspectList beforeCurrent = currentRef.copy(), beforeBase = baseRef.copy();
            AspectList afterCurrent = beforeCurrent.copy(), afterBase = beforeBase.copy();
            NodeModifier beforeModifier = nodeModifier, afterModifier = beforeModifier;
            // Original ItemEldritchObject:151-172 iterates CURRENT keys, then all six registered primals.
            for (Aspect aspect : beforeCurrent.getAspects()) {
                int capacity = afterBase.getAmount(aspect);
                if (!aspect.isPrimal()) {
                    if (random.nextBoolean()) pearlBase(afterBase, aspect, (short)(capacity - 1));
                } else pearlBase(afterBase, aspect, (short)(capacity - 2 + random.nextInt(researched ? 9 : 6)));
            }
            for (Aspect aspect : Aspect.getPrimalAspects()) {
                int draw = random.nextInt(researched ? 4 : 3);
                if (draw <= 0 || draw <= afterBase.getAmount(aspect)) continue;
                pearlBase(afterBase, aspect, (short)draw);
                // Original addToContainer can REDUCE an overfull current value after BASE was lowered.
                int left = Math.max(0, 1 + afterCurrent.getAmount(aspect) - afterBase.getAmount(aspect));
                afterCurrent.add(aspect, 1 - left);
            }
            if (afterModifier == NodeModifier.FADING && random.nextBoolean()) afterModifier = NodeModifier.PALE;
            else if (afterModifier == NodeModifier.PALE && random.nextBoolean()) afterModifier = null;
            else if (afterModifier == null && random.nextInt(5) == 0) afterModifier = NodeModifier.BRIGHT;
            prepared = new PrimalPearlChange(this, world, getBlockState(), currentRef, baseRef,
                    beforeCurrent, beforeBase, afterCurrent, afterBase, beforeModifier, afterModifier);
        } finally { pearlPlanning = false; }
        return matchesPreparedPrimalPearl(prepared) ? prepared : null;
    }
    private static void pearlBase(AspectList base, Aspect aspect, short target) {
        int previous = base.getAmount(aspect);
        if (previous < target) base.merge(aspect, target);
        else {
            // Original reduce refuses negative target (and retains a successful zero key).
            long debit = (long)previous - target;
            if (debit <= Integer.MAX_VALUE) base.reduce(aspect, (int)debit);
        }
    }
    public boolean matchesPreparedPrimalPearl(PrimalPearlChange prepared) {
        if (prepared == null || prepared.owner != this || prepared.applied || pearlPlanning || ticking || notifying || transducing || orbIntent != null) return false;
        pearlPlanning = true;
        try {
            ServerLevel world = prepared.world;
            if (level != world || !world.getServer().isSameThread() || world.getServer().getLevel(world.dimension()) != world
                    || getBlockState() != prepared.state || !current()) return false;
            // End with local state checks: no world call occurs after the observations being authorized.
            return !prepared.applied && aspects == prepared.currentRef && aspectsBase == prepared.baseRef
                    && nodeType == prepared.type && nodeModifier == prepared.beforeModifier && count == prepared.tick
                    && aspects.aspects.equals(prepared.beforeCurrent.aspects) && aspectsBase.aspects.equals(prepared.beforeBase.aspects);
        } finally { pearlPlanning = false; }
    }
    public void applyPreparedPrimalPearl(PrimalPearlChange prepared) {
        thaumcraft.common.items.ItemPrimordialPearl.requirePreparedNodeMutation(this, prepared);
        // Caller validated the exact opaque state immediately before debit. This phase makes no queries/copies/callbacks.
        if (prepared.owner != this || prepared.applied || aspects != prepared.currentRef || aspectsBase != prepared.baseRef)
            throw new IllegalStateException("Paid primordial pearl transition lost its exact node state");
        aspects = prepared.afterCurrent; aspectsBase = prepared.afterBase; nodeModifier = prepared.afterModifier; prepared.applied = true;
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
        if (level.isClientSide || node.isRemoved() || node.ticking || node.notifying || node.transducing || node.pearlPlanning
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
            boolean change = node.hungryFirst(); // Original BEFORE count++, including the old cached lock.
            if (!node.current()) return;
            ++node.count;
            node.checkLock(); // Original129-147: AFTER count++, BEFORE discharge/recharge.
            if (!node.current()) return;
            change |= node.handleDischarge();
            if (!node.current()) return;
            change |= node.rechargeAndPrune();
            if (!node.current()) return;
            int ecologyTick = node.count;
            NodeType ecologyType = node.nodeType;
            if (thaumcraft.common.lib.world.NodeEcology.taint(node,
                    () -> node.current() && node.count == ecologyTick && node.nodeType == ecologyType)) {
                if (!node.current()) return;
                node.nodeType = NodeType.TAINTED; node.regeneration = -1; node.changed();
            }
            if (!node.current()) return;
            change |= node.handleStability();
            if (!node.current()) return;
            node.runBiomeEcology();
            if (!node.current()) return;
            node.hungrySecond();
            if (change && node.current()) node.changed();
        } finally { node.ticking = false; node.tickingLevel = null; node.tickingState = null; }
    }

    private void runBiomeEcology() {
        int tick = count; NodeType type = nodeType;
        java.util.function.BooleanSupplier owner = () -> current() && count == tick && nodeType == type;
        thaumcraft.common.lib.world.NodeEcology.dark(this, owner);
        if (owner.getAsBoolean()) thaumcraft.common.lib.world.NodeEcology.pure(this, owner);
    }

    /** Client-first visual/prediction adapter; authoritative stock mutation remains server-only. */
    private static final ThreadLocal<java.util.Set<Entity>> HUNGRY_DAMAGE_FRAMES = ThreadLocal.withInitial(
            () -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()));
    private static volatile java.util.function.Consumer<TileNode> hungryClientTick = node -> {};
    public static void setHungryClientTick(java.util.function.Consumer<TileNode> handler) { hungryClientTick = java.util.Objects.requireNonNull(handler); }
    public static void clientTick(Level world, BlockPos pos, BlockState state, TileNode node) {
        if (!world.isClientSide || node.nodeType != NodeType.HUNGRY || node.ticking || node.notifying || node.transducing || node.pearlPlanning
                || node.level != world || !node.worldPosition.equals(pos) || node.getBlockState() != state || !node.current()) return;
        node.ticking = true; node.tickingLevel = world; node.tickingState = state;
        try { hungryClientTick.accept(node); } finally { node.ticking = false; node.tickingLevel = null; node.tickingState = null; }
    }

    /** Original First runs before ++count and does not check a stabilizer lock. */
    private boolean hungryFirst() {
        if (nodeType != NodeType.HUNGRY || !thaumcraft.common.config.ThaumcraftSettings.server().hardNodes()) return false;
        ServerLevel world = (ServerLevel) level;
        boolean change = false;
        Vec3 centre = Vec3.atCenterOf(worldPosition);
        for (Entity entity : world.getEntitiesOfClass(Entity.class, new AABB(worldPosition).inflate(HungryNodeRules.RANGE))) {
            if (!current() || nodeType != NodeType.HUNGRY) return change;
            if (!hungryTarget(world, entity) || entity instanceof Player player && player.getAbilities().invulnerable) continue;
            if (entity.isAlive() && !entity.isInvulnerable() && centre.distanceToSqr(entity.position()) < HungryNodeRules.DAMAGE_DISTANCE_SQUARED) {
                change |= hungryBite(world, entity, centre);
            }
            // Corpses may remain natively indexed; discarded or teleported owners are never moved.
            if (current() && hungryTarget(world, entity)) {
                Vec3 pull = HungryNodeRules.pull(centre.subtract(entity.position()));
                Vec3 motion = entity.getDeltaMovement().add(pull);
                if (Double.isFinite(motion.x) && Double.isFinite(motion.y) && Double.isFinite(motion.z)) entity.setDeltaMovement(motion);
            }
        }
        return change;
    }
    /** One native entity damage/reward frame across every HUNGRY node; ItemEntity
     * invokes onDestroyed before discard, so a second node can otherwise observe
     * a still-live zero-health source during that callback. Pull remains independent. */
    private boolean hungryBite(ServerLevel world, Entity entity, Vec3 centre) {
        var frames = HUNGRY_DAMAGE_FRAMES.get();
        if (!frames.add(entity)) return false;
        try {
            // An ItemEntity is consumed as its WHOLE real stack by native health damage,
            // but original ScanManager prices only ONE item; retain that intentional quirk.
            ItemStack actual = entity instanceof ItemEntity item ? item.getItem() : null;
            if (actual != null && actual.isEmpty()) return false;
            ItemStack snapshot = actual == null ? null : ItemStack.parse(world.registryAccess(), actual.copyWithCount(1).save(world.registryAccess())).orElseThrow();
            int ownedCount = actual == null ? 0 : actual.getCount();
            if (snapshot != null) snapshot.setCount(ownedCount);
            if (!current() || !hungryTarget(world, entity) || !entity.isAlive() || entity.isInvulnerable()
                    || centre.distanceToSqr(entity.position()) >= HungryNodeRules.DAMAGE_DISTANCE_SQUARED
                    || entity instanceof Player player && player.getAbilities().invulnerable
                    || actual != null && (!(entity instanceof ItemEntity item) || item.getItem() != actual || !ItemStack.matches(actual, snapshot))) return false;
            entity.hurt(world.damageSources().drown(), 1F);
            if (!current() || nodeType != NodeType.HUNGRY) return false;
            if (!entity.isAlive() && entity.level() == world
                    && (actual == null || entity instanceof ItemEntity item && item.getItem() == actual && hungryItemDeathMatches(actual, snapshot)))
                return hungryMeal(entity, snapshot, actual);
            return false;
        } finally { frames.remove(entity); }
    }
    /** Native BlockItem.onDestroyed clears CONTAINER even on ordinary dirt.
     * Accept exactly that paid-death transition, retaining count and every other
     * component; pricing still uses the original one-item receipt. Native child
     * contents remain separate physical drops, never an additional meal here. */
    private static boolean hungryItemDeathMatches(ItemStack actual, ItemStack before) {
        if (ItemStack.matches(actual, before)) return true;
        if (!(before.getItem() instanceof BlockItem)
                || !ItemContainerContents.EMPTY.equals(actual.get(DataComponents.CONTAINER))) return false;
        ItemStack expected = before.copy();
        expected.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        return ItemStack.matches(actual, expected);
    }
    private boolean hungryTarget(ServerLevel world, Entity entity) {
        return entity.level() == world && !entity.isRemoved() && world.hasChunkAt(entity.blockPosition()) && world.getEntity(entity.getUUID()) == entity;
    }
    private boolean hungryMeal(Entity dead, ItemStack paidItem, ItemStack consumedItem) {
        AspectList tags;
        if (paidItem != null) tags = ObjectAspectResolver.scanTags(paidItem.copyWithCount(1), level);
        else { var binding = EntityAspectBindings.describe(dead); tags = binding == null ? null : binding.aspects(); }
        if (tags == null || tags.size() == 0 || !current() || nodeType != NodeType.HUNGRY) return false;
        AspectList primals = DeconstructionRules.primals(tags.copy());
        AspectList currentRef = aspects, baseRef = aspectsBase, beforeCurrent = aspects.copy(), beforeBase = aspectsBase.copy();
        var meal = HungryNodeRules.meal(primals, beforeCurrent, beforeBase, level.random::nextInt);
        if (meal == null || !current() || nodeType != NodeType.HUNGRY || dead.level() != level || dead.isAlive()
                || paidItem != null && (!(dead instanceof ItemEntity item) || item.getItem() != consumedItem || !hungryItemDeathMatches(consumedItem, paidItem))
                || aspects != currentRef || aspectsBase != baseRef
                || !aspects.aspects.equals(beforeCurrent.aspects) || !aspectsBase.aspects.equals(beforeBase.aspects)) return false;
        if (meal.refill()) { aspects.add(meal.aspect(), 1); return true; }
        if (meal.grow()) { aspectsBase.add(meal.aspect(), 1); return true; }
        return false;
    }
    /** Original Second is OUTSIDE hardNode and lock gates, after all other node handlers. */
    private void hungrySecond() {
        if (nodeType != NodeType.HUNGRY || count % HungryNodeRules.BLOCK_INTERVAL != 0) return;
        var hit = HungryNodeTrace.randomHit(level, worldPosition, level.random);
        if (hit == null || !current() || nodeType != NodeType.HUNGRY) return;
        BlockPos target = hit.getBlockPos();
        if (worldPosition.distSqr(target) >= 256D || !level.hasChunkAt(target)) return;
        BlockState before = level.getBlockState(target);
        float hardness = before.getDestroySpeed(level, target);
        if (!before.isAir() && HungryNodeRules.breakable(hardness) && current() && nodeType == NodeType.HUNGRY
                && level.hasChunkAt(target) && level.getBlockState(target) == before)
            level.destroyBlock(target, true);
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
        return world != null && (!ticking || world == tickingLevel && state == tickingState) && !isRemoved() && (state.is(ThaumcraftContent.AURA_NODE.get()) || state.is(ThaumcraftContent.SILVERWOOD_KNOT.get()))
                && !world.isOutsideBuildHeight(worldPosition) && world.hasChunkAt(worldPosition)
                && world.getBlockEntity(worldPosition) == this && world.getBlockState(worldPosition) == state
                && !isRemoved() && level == world && getBlockState() == state;
    }

    /** A stronger nearby normal node can bully a weaker one, as in TC4. */
    private boolean handleDischarge() {
        if (nodeLock == 1 || nodeModifier == NodeModifier.FADING) return false;
        boolean shiny = nodeType == NodeType.HUNGRY || nodeModifier == NodeModifier.BRIGHT;
        int interval = nodeModifier == null ? 2 : shiny ? 1 : nodeModifier == NodeModifier.PALE ? 3 : 2;
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
                || other.level != level || !other.current() || !current()) return false;
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

    /** Original100-tick stability precedes its separate FADING repair roll. */
    private boolean handleStability() {
        if (count % AspectOrbRules.EMISSION_INTERVAL != 0) return false;
        boolean change = false;
        if (nodeType == NodeType.UNSTABLE) {
            int before = count; boolean chosen = level.random.nextBoolean();
            if (!current() || count != before || nodeType != NodeType.UNSTABLE) return false;
            if (chosen) {
                if (nodeLock == 0) change = emitOrb();
                else {
                    byte lock = nodeLock; int roll = level.random.nextInt(AspectOrbRules.stabilityBound(lock));
                    if (current() && count == before && nodeType == NodeType.UNSTABLE && nodeLock == lock && roll == AspectOrbRules.STABILITY_ROLL) { nodeType = NodeType.NORMAL; change = true; }
                }
            }
        }
        change |= repairFading();
        return change;
    }
    private boolean emitOrb() {
        AspectList stock = aspects, before = aspects.copy(); int tick = count;
        Aspect aspect = AspectOrbRules.chooseCurrentPrimal(before, level.random::nextInt);
        if (aspect == null || !current() || aspects != stock || count != tick || nodeType != NodeType.UNSTABLE || nodeLock != 0 || aspects.getAmount(aspect) != before.getAmount(aspect)) return false;
        var plan = AspectOrbQueue.prepare((ServerLevel) level, worldPosition, aspect);
        if (plan == null || !current() || aspects != stock || count != tick || nodeType != NodeType.UNSTABLE || nodeLock != 0 || aspects.getAmount(aspect) != before.getAmount(aspect)) return false;
        orbIntent = plan; orbStock = stock; orbAspect = aspect; orbBefore = before.getAmount(aspect); orbTick = tick;
        try { return plan.commit(this); }
        finally { orbIntent = null; orbStock = null; orbAspect = null; }
    }
    /** Read-only admission for the exact private native emission frame. */
    public boolean canCommitOrbEmission(AspectOrbQueue.Prepared plan) {
        return ticking && orbIntent == plan && !transducing && !notifying && current() && nodeType == NodeType.UNSTABLE && nodeLock == 0
                && count == orbTick && aspects == orbStock && orbAspect != null && aspects.getAmount(orbAspect) == orbBefore && orbBefore > 0
                && level instanceof ServerLevel server && plan.isFor(server, worldPosition, orbAspect);
    }
    /** Callback-free actual current-aura debit. No public plan can enter this scope. */
    public void commitOrbEmissionRaw(AspectOrbQueue.Prepared plan) {
        AspectOrbQueue.requireRaw(plan);
        if (!ticking || orbIntent != plan || aspects != orbStock || orbAspect == null || aspects.getAmount(orbAspect) != orbBefore || !aspects.reduce(orbAspect, 1)) throw new IllegalStateException("Stale node emission owner");
        orbIntent = null;
    }

    /** Original setNodeModifier250-252 only assigns the modifier: retain cached
     * FADING0 until a later native cache invalidation. */
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
                // Original knot metadata2 -> ordinary silverwood1 retains its axis; airy nodes disappear without a free node item.
                BlockState owner = getBlockState();
                if (owner.is(ThaumcraftContent.SILVERWOOD_KNOT.get())) {
                    level.setBlock(worldPosition, thaumcraft.common.config.ThaumcraftNaturals.SILVERWOOD_LOG.get().defaultBlockState()
                            .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                                    owner.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)), 3);
                } else level.removeBlock(worldPosition, false);
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
