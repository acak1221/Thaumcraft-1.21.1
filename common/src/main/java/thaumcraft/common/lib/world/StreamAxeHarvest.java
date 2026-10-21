package thaumcraft.common.lib.world;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.entities.golems.GolemUsePlatform;
import thaumcraft.common.items.equipment.ItemElementalAxe;
import thaumcraft.common.lib.network.BlockFxNetworking;

/**
 * Player chopping with the Axe of the Stream (original ItemElementalAxe.onBlockStartBreak).
 *
 * A non-sneaking break of a recognised log with the genuine axe in the main hand is
 * CLAIMED at the completed-break boundary of the real player's destroyBlock: the clicked
 * anchor is not broken and the caller returns false, so vanilla resynchronises the
 * client's predicted break. Instead the greedy-walk endpoint is removed by ONE nested
 * native destroyBlock of the same real player. That nested call dispatches the genuine
 * loader BREAK/BEFORE/AFTER protection events for the endpoint, applies real loot with
 * the held tool's components (Fortune/Silk), and wears the axe exactly once through the
 * ordinary mineBlock path (creative free, Unbreaking native). The anchor itself only
 * needs the ordinary reach that the packet handler already enforced; the remote endpoint
 * is the axe's own bounded ability, checked for loaded chunk, world/border bounds and
 * spawn protection, then by the protection events of the nested call.
 *
 * A claimed attempt never falls back to breaking the anchor: refusal, stale state or an
 * unloaded endpoint simply do nothing. Sneaking, non-log blocks, other actors and the
 * Excavation/golem-borrowed scopes keep the ordinary axe behaviour.
 *
 * Declared adaptations: the 150..299-tick neighbourhood update sweep is not replayed
 * (native leaf shape updates, and the magical logs' own decay start, do that work);
 * the feedback is the original crucible bubble channel of the broken log plus the
 * original bubble sound at the anchor.
 */
public final class StreamAxeHarvest {
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();
    private static final SoundEvent BUBBLE = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "bubble"));
    /** The original ItemElementalAxe:152 bubble colour, the java.awt.Color of 0.33/0.33/1.0. */
    private static final int BUBBLE_COLOUR = 0x5454ff;

    private StreamAxeHarvest() {}

    /** True when the ordinary break was claimed; the caller then returns false from destroyBlock. */
    public static boolean claim(ServerPlayerGameMode mode, ServerLevel level, ServerPlayer player, BlockPos anchor) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !(held.getItem() instanceof ItemElementalAxe) || player.isShiftKeyDown()) return false;
        // Nested endpoint break, a wand's Excavation attempt or a golem-borrowed actor must stay ordinary.
        if (ACTIVE.get() != null || ExcavationHarvest.hasActiveAttempt() || GolemUsePlatform.isActorBorrowed(player)) return false;
        if (!realActor(mode, level, player)) return false;
        BlockState state = level.getBlockState(anchor);
        if (!TreeLogTraversal.isLog(level, anchor, state)) return false;

        // From here the anchor is claimed whatever happens: no fallback break.
        BlockPos endpoint = TreeLogTraversal.endpoint(level, anchor, state);
        if (endpoint == null || !permitted(level, player, endpoint)) return true;
        BlockState endpointState = level.getBlockState(endpoint);
        if (!TreeLogTraversal.isLog(level, endpoint, endpointState) || endpointState.getBlock() != state.getBlock()) return true;

        Set<Integer> before = StreamAxeItemMotion.idsAround(level, endpoint);
        Scope scope = new Scope(mode, level, endpoint.immutable());
        ACTIVE.set(scope);
        try {
            // Never trust this boolean: native destroyBlock reports true even when removeBlock did not.
            mode.destroyBlock(endpoint);
        } finally {
            ACTIVE.remove();
        }
        if (scope.removed) {
            // Original ItemElementalAxe:152 - the crucible bubbles of the broken log, in the original colour.
            BlockFxNetworking.sendBubble(level, anchor, BUBBLE_COLOUR);
            level.playSound(null, anchor, BUBBLE, SoundSource.PLAYERS, 0.15F, 1.0F);
            StreamAxeItemMotion.followNewDrops(player, endpoint, before);
        }
        return true;
    }

    /** Mixin witness at the one native removeBlock callsite: the actual world mutation result. */
    public static void witnessRemoval(ServerPlayerGameMode mode, ServerLevel level, BlockPos pos, boolean removed) {
        Scope scope = ACTIVE.get();
        if (scope != null && !scope.witnessed && scope.mode == mode && scope.level == level && scope.endpoint.equals(pos)) {
            scope.witnessed = true;
            scope.removed = removed;
        }
    }

    private static boolean realActor(ServerPlayerGameMode mode, ServerLevel level, ServerPlayer player) {
        return player.server.isSameThread() && player.gameMode == mode && player.level() == level
                && player.isAlive() && !player.isRemoved() && !player.isSpectator() && !player.hasDisconnected()
                && player.connection != null && player.server.getPlayerList().getPlayer(player.getUUID()) == player;
    }

    private static boolean permitted(ServerLevel level, ServerPlayer player, BlockPos endpoint) {
        return level.isInWorldBounds(endpoint) && level.getWorldBorder().isWithinBounds(endpoint)
                && level.hasChunkAt(endpoint) && level.mayInteract(player, endpoint);
    }

    private static final class Scope {
        private final ServerPlayerGameMode mode;
        private final ServerLevel level;
        private final BlockPos endpoint;
        private boolean witnessed, removed;

        private Scope(ServerPlayerGameMode mode, ServerLevel level, BlockPos endpoint) {
            this.mode = mode; this.level = level; this.endpoint = endpoint;
        }
    }
}
