package thaumcraft.common.lib.inventoryscan;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumcraft.api.research.ScanResult;
import thaumcraft.common.items.relics.ItemThaumometer;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.EntityAspectBindings;
import thaumcraft.common.lib.research.ScanCallbacks;
import thaumcraft.common.lib.research.ScanManager;

/**
 * Server-owned hover sessions. The wire carries only a menu address and a request identity.
 *
 * <p>Moved into the mod from its companion add-on Thaumic Inventory Scanning for Thaumcraft 4 Reload
 * (the behaviour of BlayTheNinth's 1.7.10 add-on, see THIRD_PARTY_NOTICES.md); the session rules are
 * unchanged. The setting {@code inventory_scanning} switches it off: a new hover is then refused.</p>
 */
public final class InventoryScanServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Thaumcraft/InventoryScan");
    private static final int ACTIVE_PACKETS_PER_TICK = 8;
    private static final Map<MinecraftServer, ServerState> SERVERS = new IdentityHashMap<>();
    private static volatile BiConsumer<ServerPlayer, ScanReply> transport = (player, reply) -> {};

    private InventoryScanServer() {}

    public static void installTransport(BiConsumer<ServerPlayer, ScanReply> sender) {
        transport = Objects.requireNonNull(sender, "sender");
    }

    public static void receive(ServerPlayer player, ScanRequest request) {
        if (player == null || request == null) return;
        MinecraftServer server = player.server;
        requireServerThread(server);
        if (!actorValid(player)) return;
        ServerState state = SERVERS.computeIfAbsent(server, ignored -> new ServerState());
        PlayerState actor = state.players.computeIfAbsent(player, ignored -> new PlayerState());

        // A stop is never rejected by the active-packet limiter. A delayed old stop
        // cannot cancel a newer hover; a consumed request never becomes active again.
        if (!request.active()) {
            Pending pending = actor.pending;
            if (request.requestId() == actor.lastRequestId && (pending != null
                    || actor.admitting && actor.terminalStatus != ScanReply.CANCELLED))
                finish(player, actor, ScanReply.CANCELLED, pending == null ? 0 : progress(server, pending));
            return;
        }
        if (actor.processing) return;
        // Switched off (or the companion add-on owns the feature): admit nothing new. A stop was handled above.
        if (!InventoryScanning.enabled() && actor.pending == null) return;
        actor.processing = true;
        try {
            receiveActive(player, request, state, actor);
        } finally {
            actor.processing = false;
        }
    }

    private static void receiveActive(ServerPlayer player, ScanRequest request, ServerState state, PlayerState actor) {
        if (request.requestId() < 0) return;
        int now = player.server.getTickCount();
        if (actor.packetTick != now) {
            actor.packetTick = now;
            actor.activePackets = 0;
        }
        if (actor.activePackets >= ACTIVE_PACKETS_PER_TICK) return;
        actor.activePackets++;
        if (request.requestId() < actor.lastRequestId) return;

        if (request.requestId() == actor.lastRequestId) {
            Pending pending = actor.pending;
            if (pending == null) return; // success/failure/cancellation is terminal for this ID
            try {
                boolean accepted = now - pending.lastHeartbeat <= InventoryScanning.HEARTBEAT_TIMEOUT
                        && now - pending.started >= 0
                        && request.containerId() == pending.menu.containerId && request.slot() == pending.index
                        && valid(state, actor, player, pending);
                if (actor.pending != pending) return; // a reentrant stop already consumed this hover
                if (!accepted) {
                    finish(player, actor, ScanReply.CANCELLED, progress(player.server, pending));
                } else {
                    pending.lastHeartbeat = now;
                }
            } catch (RuntimeException failure) {
                fail(player, actor, failure);
            }
            return;
        }

        // The monotonically increasing ID is claimed before any menu/slot callback.
        // Refused requests cannot restart by sending the same heartbeat later.
        actor.lastRequestId = request.requestId();
        actor.pending = null;
        actor.terminalStatus = ScanReply.FAILED;
        actor.admitting = true;
        try {
            Pending pending = begin(player, request, now);
            if (actor.terminalStatus == ScanReply.CANCELLED) return;
            boolean accepted = pending != null && valid(state, actor, player, pending);
            if (actor.terminalStatus == ScanReply.CANCELLED) return;
            if (!accepted) {
                finish(player, actor, ScanReply.FAILED, 0);
                return;
            }
            // An item signature is a native registry ID, never count, NBT or a
            // client-chosen hash. Duplicate items need no aspect-provider call.
            String signature;
            ScanManager.ScanKind kind;
            if (pending.index == -1) {
                // Use the native player/variant descriptor, never an addon hash.
                // Its NBT providers may run code, so admission remains fenced and
                // the physical menu/cursor owner is checked again afterwards.
                signature = EntityAspectBindings.describe(player).signature();
                kind = ScanManager.ScanKind.ENTITY;
                if (actor.terminalStatus == ScanReply.CANCELLED) return;
                boolean ownerValid = valid(state, actor, player, pending);
                if (actor.terminalStatus == ScanReply.CANCELLED) return;
                if (!ownerValid) {
                    finish(player, actor, ScanReply.FAILED, 0);
                    return;
                }
            } else {
                signature = "@" + BuiltInRegistries.ITEM.getKey(pending.targetSnapshot.getItem());
                kind = ScanManager.ScanKind.ITEM;
            }
            if (ScanManager.alreadyScanned(player, signature, kind)) {
                finish(player, actor, ScanReply.ALREADY_SCANNED, 0);
                return;
            }
            actor.pending = pending;
            send(player, new ScanReply(actor.lastRequestId, 0, ScanReply.SCANNING));
        } catch (RuntimeException failure) {
            fail(player, actor, failure);
        } finally {
            actor.admitting = false;
        }
    }

    /** Called once by the native server post-tick event; packets never advance the clock. */
    public static void tick(MinecraftServer server) {
        requireServerThread(server);
        ServerState state = SERVERS.get(server);
        if (state == null) return;
        for (var entry : new ArrayList<>(state.players.entrySet())) {
            ServerPlayer player = entry.getKey();
            PlayerState actor = entry.getValue();
            if (state.players.get(player) != actor || actor.processing) continue;
            if (!actorValid(player)) {
                state.players.remove(player);
                continue;
            }
            Pending pending = actor.pending;
            if (pending == null) continue;
            actor.processing = true;
            try {
                int now = server.getTickCount();
                boolean accepted = now - pending.lastHeartbeat <= InventoryScanning.HEARTBEAT_TIMEOUT
                        && now - pending.started >= 0 && valid(state, actor, player, pending);
                if (actor.pending != pending) continue;
                if (!accepted) {
                    finish(player, actor, ScanReply.CANCELLED, progress(server, pending));
                } else if (now - pending.started >= InventoryScanning.SCAN_TICKS) {
                    complete(state, actor, player, pending);
                } else {
                    int progress = progress(server, pending);
                    if (progress != pending.lastReportedProgress) {
                        pending.lastReportedProgress = progress;
                        send(player, new ScanReply(actor.lastRequestId, progress, ScanReply.SCANNING));
                    }
                }
            } catch (RuntimeException failure) {
                fail(player, actor, failure);
            } finally {
                actor.processing = false;
            }
        }
    }

    public static void disconnect(ServerPlayer player) {
        if (player == null) return;
        requireServerThread(player.server);
        ServerState state = SERVERS.get(player.server);
        if (state != null) state.players.remove(player);
    }

    public static void stopped(MinecraftServer server) {
        requireServerThread(server);
        SERVERS.remove(server);
    }

    private static Pending begin(ServerPlayer player, ScanRequest request, int now) {
        AbstractContainerMenu menu = player.containerMenu;
        if (request.containerId() < 0 || request.slot() < -1 || menu.containerId != request.containerId()
                || !menu.stillValid(player)) return null;
        ItemStack tool = menu.getCarried();
        if (tool.getCount() != 1 || !(tool.getItem() instanceof ItemThaumometer)) return null;
        if (request.slot() == -1) {
            if (menu != player.inventoryMenu) return null;
            return new Pending(menu, player.serverLevel(), -1, null, ItemStack.EMPTY, tool, now);
        }
        if (request.slot() >= menu.slots.size()) return null;
        Slot slot = menu.slots.get(request.slot());
        if (slot instanceof ResultSlot || !slot.isActive() || slot.isFake() || !slot.mayPickup(player)) return null;
        ItemStack target = slot.getItem();
        if (target.isEmpty()) return null;
        return new Pending(menu, player.serverLevel(), request.slot(), slot, target, tool, now);
    }

    private static boolean valid(ServerState state, PlayerState actor, ServerPlayer player, Pending pending) {
        if (state.players.get(player) != actor || !actorValid(player) || player.serverLevel() != pending.level
                || player.containerMenu != pending.menu || !pending.menu.stillValid(player)) return false;
        ItemStack tool = pending.menu.getCarried();
        if (tool != pending.toolIdentity || tool.getCount() != 1 || !(tool.getItem() instanceof ItemThaumometer)
                || !ItemStack.matches(tool, pending.toolSnapshot)) return false;
        if (pending.index == -1) return pending.menu == player.inventoryMenu && ownerUnchanged(state, actor, player, pending);
        if (pending.index >= pending.menu.slots.size() || pending.menu.slots.get(pending.index) != pending.slot
                || pending.slot instanceof ResultSlot || !pending.slot.isActive() || pending.slot.isFake()
                || !pending.slot.mayPickup(player)) return false;
        ItemStack target = pending.slot.getItem();
        // Identity plus the complete count/components reject both replacements
        // and in-place mutations, including an equal-looking slot/menu ABA.
        return target == pending.targetIdentity && !target.isEmpty() && ItemStack.matches(target, pending.targetSnapshot)
                && ownerUnchanged(state, actor, player, pending);
    }

    private static boolean ownerUnchanged(ServerState state, PlayerState actor, ServerPlayer player, Pending pending) {
        return player.containerMenu == pending.menu && player.serverLevel() == pending.level
                && pending.menu.getCarried() == pending.toolIdentity
                && ItemStack.matches(pending.menu.getCarried(), pending.toolSnapshot)
                && state.players.get(player) == actor && actorValid(player)
                // Permission/getItem overrides may retire their own slot object.
                && (pending.index == -1 || pending.index < pending.menu.slots.size()
                    && pending.menu.slots.get(pending.index) == pending.slot);
    }

    private static void complete(ServerState state, PlayerState actor, ServerPlayer player, Pending pending) {
        // Claim completion BEFORE any third-party aspect provider can reenter
        // receive/tick. No failure or thrown callback is automatically retried.
        actor.pending = null;
        actor.terminalStatus = ScanReply.FAILED;
        int status = ScanReply.FAILED;
        try {
            if (valid(state, actor, player, pending) && !KnowledgeAccess.get(player).isRejected()) {
                ScanResult witness = pending.index == -1 ? ScanResult.entity(player)
                        : ScanResult.item(pending.targetSnapshot.copyWithCount(1));
                // Physical inventory targets do not dispatch phenomena callbacks.
                // Resolve once, using only the server snapshot, then recheck every
                // physical owner because item aspect providers may call arbitrary code.
                ScanCallbacks.Target target = ScanCallbacks.resolve(pending.level, witness);
                if (target != null && valid(state, actor, player, pending)) {
                    if (ScanManager.alreadyScanned(player, target.signature(), target.kind())) {
                        status = ScanReply.ALREADY_SCANNED;
                    } else if (ScanManager.completeScan(player, target.signature(), target.aspects(), target.kind(), target.clue())) {
                        status = ScanReply.SUCCESS;
                    }
                }
            }
        } catch (RuntimeException failure) {
            LOGGER.error("Inventory scan callback failed for player {} request {}; request consumed",
                    player.getUUID(), actor.lastRequestId, failure);
        } finally {
            actor.terminalStatus = status;
            if (state.players.get(player) == actor && actorValid(player))
                send(player, new ScanReply(actor.lastRequestId, InventoryScanning.SCAN_TICKS, status));
        }
    }

    private static boolean actorValid(ServerPlayer player) {
        return !player.isRemoved() && player.isAlive() && !player.isSpectator() && !player.hasDisconnected()
                && player.server.getPlayerList().getPlayer(player.getUUID()) == player;
    }

    private static int progress(MinecraftServer server, Pending pending) {
        return Math.max(0, Math.min(InventoryScanning.SCAN_TICKS, server.getTickCount() - pending.started));
    }

    private static void finish(ServerPlayer player, PlayerState actor, int status, int progress) {
        actor.pending = null;
        actor.terminalStatus = status;
        send(player, new ScanReply(actor.lastRequestId, progress, status));
    }

    private static void fail(ServerPlayer player, PlayerState actor, RuntimeException failure) {
        LOGGER.error("Inventory scan validation failed for player {} request {}; request consumed",
                player.getUUID(), actor.lastRequestId, failure);
        if (actor.pending == null && actor.terminalStatus == ScanReply.CANCELLED) return;
        finish(player, actor, ScanReply.FAILED, 0);
    }

    private static void send(ServerPlayer player, ScanReply reply) {
        try {
            transport.accept(player, reply);
        } catch (RuntimeException failure) {
            // A networking observer cannot make an already committed scan retry.
            LOGGER.error("Could not send inventory scan receipt for player {} request {}",
                    player.getUUID(), reply.requestId(), failure);
        }
    }

    private static void requireServerThread(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Inventory scanning requires the server thread");
    }

    private static final class ServerState {
        final Map<ServerPlayer, PlayerState> players = new IdentityHashMap<>();
    }

    private static final class PlayerState {
        int lastRequestId = -1;
        int packetTick;
        int activePackets;
        int terminalStatus = ScanReply.FAILED;
        boolean admitting;
        boolean processing;
        Pending pending;
    }

    private static final class Pending {
        final AbstractContainerMenu menu;
        final ServerLevel level;
        final int index;
        final Slot slot;
        final ItemStack targetIdentity;
        final ItemStack targetSnapshot;
        final ItemStack toolIdentity;
        final ItemStack toolSnapshot;
        final int started;
        int lastHeartbeat;
        int lastReportedProgress;

        Pending(AbstractContainerMenu menu, ServerLevel level, int index, Slot slot, ItemStack target, ItemStack tool, int tick) {
            this.menu = menu;
            this.level = level;
            this.index = index;
            this.slot = slot;
            targetIdentity = target;
            targetSnapshot = target.copy();
            toolIdentity = tool;
            toolSnapshot = tool.copy();
            started = lastHeartbeat = tick;
        }
    }
}
