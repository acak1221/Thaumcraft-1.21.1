package thaumcraft.common.lib;

import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.utils.value.IntValue;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.ItemJarNode;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.items.wands.WandData;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.tiles.TileJarNode;
import thaumcraft.common.tiles.TileNode;

/** Original WandManager event four: 26 glass, nine wooden slabs and 70 whole vis of every primal. */
public final class NodeJarFormation {
    private NodeJarFormation() {}
    public static final Map<String, Integer> FORM_VIS = Map.of(
            "aer", 70, "terra", 70, "ignis", 70, "aqua", 70, "ordo", 70, "perditio", 70);
    private static final int QUIET = Block.UPDATE_KNOWN_SHAPE;
    private static final ThreadLocal<Transfer> ACTIVE = new ThreadLocal<>();
    private static final class Transfer {
        final ServerLevel level; final BlockPos center;
        BlockEntity replacement, original; boolean writing, admitted, rollingBack, restored, factoryClaimed;
        Transfer(ServerLevel level, BlockPos center) { this.level = level; this.center = center.immutable(); }
    }

    /** Seed the actual native replacement before onPlace can observe a default, empty block entity. */
    public static BlockEntity preparedReplacement(BlockPos pos, BlockState state) {
        Transfer transfer = ACTIVE.get();
        if (transfer == null || !transfer.writing || transfer.factoryClaimed || !transfer.center.equals(pos)
                || transfer.replacement == null || transfer.replacement.getBlockState() != state) return null;
        // A LOAD callback can harvest and replace the block again before the outer setBlock returns.
        // Only the first native factory call may claim the filled owner.
        transfer.factoryClaimed = true;
        return transfer.replacement;
    }

    /** EntityBlock factories have no Level argument. Check it before native registration/LOAD. */
    public static void validatePreparedLevel(BlockEntity entity, Level level) {
        Transfer transfer = ACTIVE.get();
        if (transfer != null && transfer.writing && transfer.replacement == entity && transfer.level != level)
            throw new IllegalStateException("A prepared node owner cannot be attached in another level");
    }

    /** Native direct removeBlockEntity removes its map entry before calling setRemoved.
     * The attached exact owner still settles the handoff; a cached removed owner must not
     * coexist with a restored second payload. A rejected foreign attachment stays detached. */
    public static void removingPrepared(BlockEntity entity) {
        Transfer transfer = ACTIVE.get();
        if (transfer == null || !transfer.writing || transfer.replacement != entity || entity.getLevel() != transfer.level) return;
        if (transfer.rollingBack) transfer.restored = true;
        else transfer.admitted = true;
    }

    /** Native LOAD/save/drop callbacks can observe the replacement before setBlock returns.
     * That exact native owner settles the transfer even when the callback harvests it immediately. */
    public static boolean isTransferring(Level level, BlockPos pos) {
        Transfer transfer = ACTIVE.get();
        if (transfer == null || !transfer.writing || transfer.level != level || !transfer.center.equals(pos)) return false;
        // Level.getBlockEntity can CREATE the replacement during the old source's UNLOAD.
        // Observe only an owner already registered by the native chunk lifecycle.
        var chunk = transfer.level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null && chunk.getBlockEntities().get(pos) == transfer.replacement) {
            if (transfer.rollingBack) transfer.restored = true;
            else transfer.admitted = true;
        }
        return !(transfer.rollingBack ? transfer.restored : transfer.admitted);
    }

    public interface Protection {
        boolean breakBlock(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state);
        boolean placeBlock(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState before, BlockState after, InteractionHand hand, Direction face);
    }
    private static Protection protection = new Protection() {
        public boolean breakBlock(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) { return commonBreak(level, player, pos, state); }
        public boolean placeBlock(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState before, BlockState after, InteractionHand hand, Direction face) { return commonPlace(level, player, pos, after); }
    };
    public static void installProtection(Protection value) { protection = java.util.Objects.requireNonNull(value); }
    public static boolean commonBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        return !BlockEvent.BREAK.invoker().breakBlock(level, pos, state, player, noExperience()).isFalse();
    }
    public static boolean commonPlace(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        return !BlockEvent.PLACE.invoker().placeBlock(level, pos, state, player).isFalse();
    }

    /** Original 75 percent chance moves quality down one step; a fading node stays fading. */
    public static NodeModifier degradedModifier(NodeModifier modifier, float roll) {
        if (roll >= .75F) return modifier;
        if (modifier == NodeModifier.BRIGHT) return null;
        if (modifier == null) return NodeModifier.PALE;
        return NodeModifier.FADING;
    }

    public static boolean fits(Level level, BlockPos center) {
        for (int y = -1; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos pos = center.offset(x, y, z);
            if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)) return false;
            BlockState state = level.getBlockState(pos);
            if (x == 0 && y == 0 && z == 0) {
                if (!(level.getBlockEntity(pos) instanceof TileNode)) return false;
            } else if (level.getBlockEntity(pos) != null) return false;
            else if (y == 2) {
                if (!state.is(BlockTags.WOODEN_SLABS) || !(state.getBlock() instanceof SlabBlock)
                        || state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) return false;
            } else if (!state.is(Blocks.GLASS)) return false;
        }
        return true;
    }

    private static BlockPos candidate(Level level, BlockPos clicked) {
        // Original createNodeJar searches x-2..x, y-3..y, z-2..z; the node is at (x+1,y,z+1).
        for (int dx = -2; dx <= 0; dx++) for (int dy = -3; dy <= 0; dy++) for (int dz = -2; dz <= 0; dz++) {
            BlockPos center = clicked.offset(dx + 1, dy, dz + 1);
            if (fits(level, center)) return center;
        }
        return null;
    }

    private static boolean permitted(ServerLevel level, ServerPlayer player, BlockPos pos, Direction face, ItemStack wand) {
        return level.isInWorldBounds(pos) && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
                && player.level() == level && !player.isSpectator() && player.isAlive() && !player.hasDisconnected()
                && level.mayInteract(player, pos) && player.mayUseItemAt(pos, face, wand);
    }

    private static IntValue noExperience() {
        return new IntValue() {
            private int amount;
            @Override public int getAsInt() { return amount; }
            @Override public void accept(int value) { amount = value; }
        };
    }

    public static InteractionResult tryForm(UseOnContext context) {
        Level world = context.getLevel(); BlockPos clicked = context.getClickedPos();
        if (!world.hasChunkAt(clicked) || !world.getBlockState(clicked).is(Blocks.GLASS)) return InteractionResult.PASS;
        if (!(context.getItemInHand().getItem() instanceof ItemWandCasting) || context.getPlayer() == null) return InteractionResult.PASS;
        if (!KnowledgeAccess.get(context.getPlayer()).isResearchComplete("NODEJAR")) return InteractionResult.PASS;
        BlockPos center = candidate(world, clicked);
        if (center == null) return InteractionResult.PASS;
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (!(world instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)
                || !level.getServer().isSameThread() || ACTIVE.get() != null) return InteractionResult.FAIL;
        var transfer = new Transfer(level, center); ACTIVE.set(transfer);
        TileNode node = (TileNode) level.getBlockEntity(center);
        if (!node.beginTransduction()) { ACTIVE.remove(); return InteractionResult.FAIL; }
        try {
            var states = new LinkedHashMap<BlockPos, BlockState>();
            for (int y = -1; y <= 2; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos pos = center.offset(x, y, z); states.put(pos, level.getBlockState(pos));
            }
            CompoundTag sourceData = node.saveWithFullMetadata(level.registryAccess());
            ItemStack wand = context.getItemInHand(), before = wand.copy(), paid = wand.copy();
            Map<String, Integer> price = ArcaneVis.price(FORM_VIS, wand, player);
            if (!ArcaneVis.debit(paid, price)) return InteractionResult.FAIL;
            BlockState jarState = ThaumcraftEldritchBlocks.JAR_NODE.get().defaultBlockState();
            for (var entry : states.entrySet()) {
                if (!permitted(level, player, entry.getKey(), context.getClickedFace(), wand)
                        || !protection.breakBlock(level, player, entry.getKey(), entry.getValue())) return InteractionResult.FAIL;
            }
            if (!protection.placeBlock(level, player, center, states.get(center), jarState, context.getHand(), context.getClickedFace())) return InteractionResult.FAIL;
            if (!captureCurrent(context, level, player, node, sourceData, states, wand, before, price)) return InteractionResult.FAIL;
            var jar = new TileJarNode(center, jarState);
            jar.initializeNode(node.getAspects(), node.getNodeType(), degradedModifier(node.getNodeModifier(), level.random.nextFloat()), node.getId());
            transfer.replacement = jar;
            transfer.original = snapshot(node, sourceData, level);
            var recovery = prepareRecovery(level, states, node);
            if (!captureCurrent(context, level, player, node, sourceData, states, wand, before, price)) return InteractionResult.FAIL;
            var written = new LinkedHashMap<BlockPos, BlockState>();
            // Debit before native BE lifecycle callbacks can save/log out the player.
            wand.set(ThaumcraftContent.WAND_DATA.get(), ItemWandCasting.data(paid));
            transfer.writing = true;
            try {
                for (BlockPos pos : states.keySet()) {
                    if (pos.equals(center)) continue;
                    if (level.getBlockState(pos) != states.get(pos)) return InteractionResult.FAIL;
                    written.put(pos, Blocks.AIR.defaultBlockState());
                    if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET)) return InteractionResult.FAIL;
                }
                if (level.getBlockEntity(center) != node || level.getBlockState(center) != states.get(center)) return InteractionResult.FAIL;
                // Cached old source references exposed by UNLOAD must no longer own the stock.
                node.retireForJar();
                written.put(center, jarState);
                level.setBlock(center, jarState, QUIET);
                isTransferring(level, center);
            } catch (RuntimeException | Error failure) {
                Thaumcraft.LOG.warn("Node jar capture callback failed at {}; reconciling native ownership", center, failure);
            } finally {
                if (!transfer.admitted) {
                    refund(wand, price);
                    rollback(transfer, states, written, recovery);
                }
                transfer.writing = false;
            }
            if (!transfer.admitted) return InteractionResult.FAIL;
            notifyChanges(level, states);
            level.blockEvent(center, jarState.getBlock(), TileJarNode.ANIMATE_EVENT, 0);
            level.playSound(null, center, SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "wand")), SoundSource.BLOCKS, 1, 1);
            return InteractionResult.CONSUME;
        } finally { node.endTransduction(); ACTIVE.remove(); }
    }

    private static boolean captureCurrent(UseOnContext context, ServerLevel level, ServerPlayer player, TileNode node,
                                          CompoundTag sourceData, Map<BlockPos, BlockState> states, ItemStack wand,
                                          ItemStack before, Map<String, Integer> price) {
        return price.equals(ArcaneVis.price(FORM_VIS, wand, player))
                && states.keySet().stream().allMatch(pos -> permitted(level, player, pos, context.getClickedFace(), wand))
                && player.getItemInHand(context.getHand()) == wand && ItemStack.matches(wand, before)
                && player.level() == level && player.isAlive() && !player.hasDisconnected()
                && KnowledgeAccess.get(player).isResearchComplete("NODEJAR")
                && level.getBlockEntity(node.getBlockPos()) == node && sourceData.equals(node.saveWithFullMetadata(level.registryAccess()))
                && states.entrySet().stream().allMatch(e -> level.getBlockState(e.getKey()) == e.getValue());
    }

    /** Release is free, as in TileJarNode:222-241. The filled replacement precedes all client/neighbour feedback. */
    public static InteractionResult release(TileJarNode jar, Level world, ItemStack wand, Player actor, Direction face) {
        if (!(wand.getItem() instanceof ItemWandCasting) || actor == null) return InteractionResult.FAIL;
        BlockPos pos = jar.getBlockPos();
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (!(world instanceof ServerLevel level) || !(actor instanceof ServerPlayer player)
                || !level.getServer().isSameThread() || ACTIVE.get() != null || level.getBlockEntity(pos) != jar) return InteractionResult.FAIL;
        var transfer = new Transfer(level, pos); ACTIVE.set(transfer);
        try {
            BlockState state = jar.getBlockState(), target = ThaumcraftContent.AURA_NODE.get().defaultBlockState();
            CompoundTag before = jar.saveWithFullMetadata(level.registryAccess());
            ItemStack heldBefore = wand.copy();
            InteractionHand hand = player.getOffhandItem() == wand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (!permitted(level, player, pos, face, wand) || !protection.breakBlock(level, player, pos, state)
                    || !protection.placeBlock(level, player, pos, state, target, hand, face)
                    || !permitted(level, player, pos, face, wand)
                    || !ItemStack.matches(wand, heldBefore) || player.getItemInHand(hand) != wand
                    || level.getBlockEntity(pos) != jar || level.getBlockState(pos) != state
                    || !before.equals(jar.saveWithFullMetadata(level.registryAccess()))) return InteractionResult.FAIL;
            var node = new TileNode(ThaumcraftContent.NODE_BLOCK_ENTITY.get(), pos, target);
            node.restoreFromJar(jar.getAspects(), jar.getNodeType(), jar.getNodeModifier(), jar.getId());
            transfer.replacement = node; transfer.original = snapshot(jar, before, level);
            var recovery = prepareRecovery(level, Map.of(pos, state), jar);
            if (!permitted(level, player, pos, face, wand) || player.getItemInHand(hand) != wand || !ItemStack.matches(wand, heldBefore)
                    || level.getBlockEntity(pos) != jar || !before.equals(jar.saveWithFullMetadata(level.registryAccess()))) return InteractionResult.FAIL;
            transfer.writing = true;
            try {
                jar.retireForRelease();
                level.setBlock(pos, target, QUIET);
                isTransferring(level, pos);
            } catch (RuntimeException | Error failure) {
                Thaumcraft.LOG.warn("Node jar release callback failed at {}; reconciling native ownership", pos, failure);
            } finally {
                if (!transfer.admitted) rollback(transfer, Map.of(pos, state), Map.of(pos, target), recovery);
                transfer.writing = false;
            }
            if (!transfer.admitted) return InteractionResult.FAIL;
            notifyChanges(level, Map.of(pos, state));
            level.levelEvent(2001, pos, Block.getId(state));
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1, .9F + level.random.nextFloat() * .2F);
            return InteractionResult.CONSUME;
        } finally { ACTIVE.remove(); }
    }

    private static BlockEntity snapshot(BlockEntity source, CompoundTag data, ServerLevel level) {
        BlockEntity copy = source instanceof TileNode
                ? new TileNode(source.getType(), source.getBlockPos(), source.getBlockState())
                : new TileJarNode(source.getBlockPos(), source.getBlockState());
        copy.loadWithComponents(data, level.registryAccess());
        return copy;
    }

    private static Map<BlockPos, DeferredItemDrops.PreparedBatch> prepareRecovery(ServerLevel level, Map<BlockPos, BlockState> states, BlockEntity source) {
        var recovery = new LinkedHashMap<BlockPos, DeferredItemDrops.PreparedBatch>();
        for (var entry : states.entrySet()) {
            ItemStack stack;
            if (entry.getKey().equals(source.getBlockPos())) {
                stack = new ItemStack(ThaumcraftEldritchBlocks.JAR_NODE_ITEM.get());
                var item = (ItemJarNode) stack.getItem();
                if (source instanceof TileNode node) {
                    item.setAspects(stack, node.getAspects()); item.setNodeAttributes(stack, node.getNodeType(), node.getNodeModifier(), node.getId());
                } else if (source instanceof TileJarNode jar) {
                    item.setAspects(stack, jar.getAspects()); item.setNodeAttributes(stack, jar.getNodeType(), jar.getNodeModifier(), jar.getId());
                }
            } else stack = new ItemStack(entry.getValue().getBlock().asItem());
            recovery.put(entry.getKey(), DeferredItemDrops.prepareDirectedBatch(level, entry.getKey(), java.util.List.of(
                    new DeferredItemDrops.DirectedDrop(stack, Vec3.atCenterOf(entry.getKey()), Vec3.ZERO, 10))));
        }
        return recovery;
    }

    private static void refund(ItemStack wand, Map<String, Integer> price) {
        WandData data = ItemWandCasting.data(wand);
        for (var cost : price.entrySet()) data = data.withAmount(cost.getKey(), Math.addExact(data.amount(cost.getKey()), cost.getValue()));
        wand.set(ThaumcraftContent.WAND_DATA.get(), data);
    }

    private static void rollback(Transfer transfer, Map<BlockPos, BlockState> originals, Map<BlockPos, BlockState> written,
                                 Map<BlockPos, DeferredItemDrops.PreparedBatch> recovery) {
        ServerLevel level = transfer.level;
        transfer.rollingBack = true;
        transfer.factoryClaimed = false;
        transfer.replacement = transfer.original; // Reverse native factory also gets the complete original, never a default node.
        // Restore shell first: the original owner's LOAD observes the complete structure and refunded wand.
        for (var entry : written.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.equals(transfer.center)) continue;
            if (level.getBlockState(pos) == originals.get(pos)) continue;
            if (level.getBlockState(pos) == entry.getValue()) {
                try { level.setBlock(pos, originals.get(pos), QUIET); }
                catch (RuntimeException | Error failure) { Thaumcraft.LOG.warn("Node jar shell restoration callback failed at {}", pos, failure); }
            }
            if (level.getBlockState(pos) != originals.get(pos)) {
                // Never erase a foreign block installed by a lifecycle listener. Retain our removed material instead.
                recovery.get(pos).commitRaw();
            }
        }
        if (!written.containsKey(transfer.center)) return;
        BlockState old = originals.get(transfer.center), current = level.getBlockState(transfer.center);
        try {
            if (current == old) {
                level.setBlockEntity(transfer.original);
            } else if (current == written.get(transfer.center) || current.isAir()) {
                level.setBlock(transfer.center, old, QUIET);
            }
        } catch (RuntimeException | Error failure) {
            Thaumcraft.LOG.warn("Node jar source restoration callback failed at {}; reconciling native ownership", transfer.center, failure);
        }
        isTransferring(level, transfer.center);
        if (!transfer.restored) {
            // A hostile LOAD/UNLOAD handler can reject the restoration too. Preserve the sole payload in saved item custody.
            recovery.get(transfer.center).commitRaw();
        }
    }

    private static void notifyChanges(ServerLevel level, Map<BlockPos, BlockState> originals) {
        for (var entry : originals.entrySet()) {
            BlockState state = level.getBlockState(entry.getKey());
            try {
                level.sendBlockUpdated(entry.getKey(), entry.getValue(), state, Block.UPDATE_CLIENTS);
                level.updateNeighborsAt(entry.getKey(), state.getBlock());
            } catch (RuntimeException | Error failure) {
                Thaumcraft.LOG.warn("Committed node jar feedback failed at {}", entry.getKey(), failure);
            }
        }
    }
}
