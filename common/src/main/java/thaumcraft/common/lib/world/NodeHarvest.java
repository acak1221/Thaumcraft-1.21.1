package thaumcraft.common.lib.world;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ThaumcraftEssences;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.utils.DeferredItemDrops;
import thaumcraft.common.tiles.TileNode;

/**
 * BlockAiry:265-279, ordinary aura nodes and real embedded Silverwood node knots.
 * 1.7 Forge calls onBlockHarvested even in creative, BEFORE removedByPlayer;
 * Block.dropBlockAsItem checks doTileDrops, not creative or Fortune. Native1.21
 * destroy runs after SUCCESSFUL removal, before its creative early return.
 *
 * These short-lived snapshots are NOT drops and are never persisted. onRemove
 * only witnesses the exact captured BE identity; destroy commits once, then the
 * existing durable queue owns the outputs. No unload/expiry/explosion harvest.
 * Weak level keys AND weak BE references avoid retaining a level via its BE.
 */
public final class NodeHarvest {
    private static final Map<ServerLevel, Map<BlockPos, ArrayDeque<Harvest>>> PENDING = new WeakHashMap<>();

    private static final class Harvest {
        final WeakReference<TileNode> node;
        final BlockState state;
        final AspectList current;
        final long tick;
        boolean removed;

        Harvest(TileNode node, BlockState state, long tick) {
            this.node = new WeakReference<>(node);
            this.state = state;
            this.current = node.getAspects(); // CURRENT vis, never base capacity.
            this.tick = tick;
        }
    }

    private NodeHarvest() {}

    /** Called after native playerWillDestroy callbacks, while the same BE lives. */
    public static void prepare(Level level, BlockPos pos, BlockState state, Player player, TileNode node) {
        if (!(level instanceof ServerLevel server) || player.level() != level || player.isSpectator()
                || node == null || node.isRemoved() || node.getLevel() != level
                || !TileNode.isNodeHost(state)
                || !server.hasChunkAt(pos)
                || server.getBlockEntity(pos) != node || server.getBlockState(pos) != state
                || !player.canInteractWithBlock(pos, 0) || !server.mayInteract(player, pos)) return;
        requireServerThread(server);
        var byPosition = PENDING.computeIfAbsent(server, ignored -> new HashMap<>());
        var attempts = byPosition.computeIfAbsent(pos.immutable(), ignored -> new ArrayDeque<>());
        attempts.removeIf(attempt -> attempt.tick != server.getGameTime() || !attempt.removed && attempt.node.get() == null
                || !attempt.removed && attempt.node.get() == node);
        // Sample the original drop gate before removal, not at a later retry.
        // No creative exemption: original onBlockHarvested explicitly drops it.
        if (server.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) attempts.addLast(new Harvest(node, state, server.getGameTime()));
        if (attempts.isEmpty()) byPosition.remove(pos);
        if (byPosition.isEmpty()) PENDING.remove(server);
    }

    /** Identity witness only; MUST NOT enqueue or spawn while LevelChunk removes. */
    public static void removing(Level level, BlockPos pos, BlockState state, BlockState replacement) {
        if (!(level instanceof ServerLevel server) || replacement.is(state.getBlock())) return;
        var attempts = attempts(server, pos);
        if (attempts == null) return;
        var current = server.getBlockEntity(pos);
        for (var attempt : attempts) {
            var captured = attempt.node.get();
            if (captured != null && attempt.tick == server.getGameTime() && attempt.state == state && captured == current) attempt.removed = true;
        }
    }

    /** Native Block.destroy is called only when the player's removeBlock succeeded. */
    public static void committed(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel server)) return;
        requireServerThread(server);
        var attempts = attempts(server, pos);
        if (attempts == null) return;
        // Nested harvests of a callback-created replacement unwind LIFO. Never
        // inspect/mutate the replacement BE or reuse the previous node's data.
        var attempt = attempts.pollLast();
        if (attempts.isEmpty()) {
            var byPosition = PENDING.get(server);
            byPosition.remove(pos);
            if (byPosition.isEmpty()) PENDING.remove(server);
        }
        if (attempt == null || !attempt.removed || attempt.state != state || attempt.tick != server.getGameTime()) return;
        var drops = new ArrayList<ItemStack>();
        for (Aspect aspect : attempt.current.getAspects()) {
            int amount = attempt.current.getAmount(aspect);
            if (amount < 5) continue;
            for (int index = 0; index <= amount / 10; index++) {
                ItemStack stack = new ItemStack(ThaumcraftEssences.WISP_ESSENCE.get());
                ThaumcraftEssences.WISP_ESSENCE.get().setAspects(stack, new AspectList().add(aspect, 2));
                drops.add(stack);
            }
        }
        // Original block-harvest scatter/RNG/motion/pickup10 are distinct from
        // inventory evacuation. Only safe level-post delivery and canceled-ADD
        // retention are shared; no spawn callback runs in the removal frame.
        if (!drops.isEmpty()) DeferredItemDrops.enqueueBlockHarvest(server, pos, drops);
    }

    private static ArrayDeque<Harvest> attempts(ServerLevel level, BlockPos pos) {
        var byPosition = PENDING.get(level);
        return byPosition == null ? null : byPosition.get(pos);
    }

    /** Root calls SERVER_LEVEL_POST, before DeferredItemDrops.tick(level). */
    public static void endTick(ServerLevel level) { PENDING.remove(level); }
    /** Root calls SERVER_STOPPED; no player or level is held strongly here. */
    public static void clear() { PENDING.clear(); }

    private static void requireServerThread(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Node harvest requires the server thread");
    }
}
