package thaumcraft.common.testing;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.inventoryscan.ScanReply;
import thaumcraft.common.lib.inventoryscan.ScanRequest;
import thaumcraft.common.lib.research.PlayerKnowledge;

/**
 * Native connected actors, menus, slots, cursor and Thaumcraft knowledge; real post-ticks own time.
 *
 * <p>The checks of the companion add-on Thaumic Inventory Scanning, moved here with the feature
 * ({@code thaumcraft.common.lib.inventoryscan}). They need a real loader connection, which the loader
 * test mod installs; a loader that installs none runs none of them.</p>
 */
public final class InventoryScanRuntimeChecks {
    @FunctionalInterface public interface Login {
        void join(ServerPlayer player, Connection connection);
    }
    private static Login login;
    private static BiConsumer<ServerPlayer, ScanRequest> dispatch;
    private static Function<GameTestHelper, GameTestInfo> testInfo;
    private static final Map<GameTestInfo, Driver> DRIVERS = new java.util.IdentityHashMap<>();
    private static final Map<ServerPlayer, List<ScanReply>> SENT = new java.util.IdentityHashMap<>();
    private InventoryScanRuntimeChecks() {}

    /** Installed only by the loader test mod, never by the released mod. */
    public static void installPlatform(Login actorLogin, BiConsumer<ServerPlayer, ScanRequest> requestDispatch,
                                       Function<GameTestHelper, GameTestInfo> lifetimeOwner) {
        login = actorLogin; dispatch = requestDispatch; testInfo = lifetimeOwner;
    }

    /**
     * Called by the loader test mod's reply transport for every receipt it really sends to an actor.
     * The mod's channels travel inside Architectury's own wire payload, so the receipts are observed
     * where the server hands them to the network, not parsed back out of the outbound packets.
     */
    public static void sent(ServerPlayer player, ScanReply reply) {
        SENT.computeIfAbsent(player, ignored -> new ArrayList<>()).add(reply);
    }

    public static void twentyFiveNativeTicks(GameTestHelper test) {
        var f = new Fixture(test);
        f.begin(0);
        for (int i = 0; i < 100; i++) dispatch.accept(f.player, f.request);
        f.check(f.knowledge.save().equals(f.beforeKnowledge), "one hundred real C2S heartbeats cannot advance the server clock or award early");
        f.watch(27, true, false, () -> {
            f.check(f.knowledge.hasScannedItem("@minecraft:stone"), "the native post-tick hook awards stone after 25 server ticks");
            f.exactPools(Map.of("terra", 2)); f.expectStatus(ScanReply.SUCCESS);
            f.check(f.replies.stream().filter(r -> r.status() == ScanReply.SUCCESS).allMatch(r -> r.progress() == 25), "success receipt reports exactly 25 server ticks");
            f.unchangedInventory(); f.pass();
        });
    }

    /** The setting {@code inventory_scanning} off: the server admits no hover, answers nothing and awards nothing. */
    public static void settingOffAdmitsNothing(GameTestHelper test) {
        var f = new Fixture(test);
        try (var original = thaumcraft.common.config.ThaumcraftTweaks.override(thaumcraft.common.config.ThaumcraftTweaks.ORIGINAL)) {
            f.begin(0);
            f.check(f.knowledge.save().equals(f.beforeKnowledge), "a refused hover changes no knowledge");
        }
        // The saved form of the knowledge is compared again under the settings the later ticks run with.
        f.markKnowledge();
        f.check(f.replies.isEmpty(), "a switched-off server sends no receipt for a new hover, got " + f.replies);
        // No heartbeat: the refused request claimed no identity, so replaying it with the setting back on
        // would be an ordinary new hover. Left alone, it must never turn into a session.
        f.watch(28, false, true, () -> {
            f.check(f.replies.isEmpty(), "the refused hover never becomes a session");
            f.unchangedInventory(); f.pass();
        });
    }

    public static void serverCursorRequired(GameTestHelper test) {
        var f = new Fixture(test);
        f.player.getInventory().setItem(0, f.tool.copy());
        f.menu.setCarried(ItemStack.EMPTY); f.rememberInventory(); f.begin(0);
        f.watch(28, true, true, () -> { f.expectStatus(ScanReply.FAILED); f.unchangedInventory(); f.pass(); });
    }

    public static void invalidAddressesAndResultSlots(GameTestHelper test) {
        var f = new Fixture(test);
        f.issue(f.menu.containerId + 1, 0); f.issue(f.menu.containerId, -2);
        f.issue(f.menu.containerId, f.menu.slots.size());
        var old = f.menu;
        f.player.containerMenu = f.player.inventoryMenu; f.menu = f.player.inventoryMenu;
        f.menu.setCarried(f.tool); f.menu.getSlot(0).set(new ItemStack(Items.STONE, 3));
        f.issue(f.menu.containerId, 0);
        f.check(f.replies.size() == 4 && f.replies.stream().allMatch(r -> r.status() == ScanReply.FAILED), "wrong window, negative/oversized slot and real crafting ResultSlot are all refused");
        f.player.containerMenu = old; f.menu = old; f.rememberInventory(); f.markKnowledge();
        f.watch(28, false, true, () -> { f.unchangedInventory(); f.pass(); });
    }

    public static void permissionAndSlotIdentity(GameTestHelper test) {
        var f = new Fixture(test);
        var storage = new SimpleContainer(1); storage.setItem(0, new ItemStack(Items.STONE, 64));
        f.menu.slots.set(0, new Slot(storage, 0, 0, 0) {
            @Override public boolean mayPickup(Player player) { return false; }
        });
        f.begin(0); f.expectStatus(ScanReply.FAILED);
        f.watch(28, false, true, () -> { f.pass(); });
    }

    public static void menuIdentityAba(GameTestHelper test) {
        var f = new Fixture(test); f.begin(0);
        f.at(8, () -> {
            f.unchangedInventory();
            var replacement = ChestMenu.oneRow(f.menu.containerId, f.player.getInventory());
            replacement.getSlot(0).set(new ItemStack(Items.STONE, 64)); replacement.setCarried(f.tool);
            f.player.containerMenu = replacement; f.menu = replacement; f.rememberInventory();
        });
        f.watch(30, true, true, () -> { f.expectStatus(ScanReply.CANCELLED); f.unchangedInventory(); f.pass(); });
    }

    public static void itemIdentityComponentsAndCount(GameTestHelper test) { changedItem(test, 0); }
    private static void changedItem(GameTestHelper test, int mode) {
        var f = new Fixture(test); f.begin(0);
        f.at(8, () -> {
            f.unchangedInventory();
            ItemStack target = f.menu.getSlot(0).getItem();
            if (mode == 0) f.menu.getSlot(0).set(target.copy());
            else if (mode == 1) target.set(DataComponents.CUSTOM_NAME, Component.literal("changed during scan"));
            else target.shrink(1);
            f.rememberInventory();
        });
        f.watch(29, true, true, () -> {
            f.expectStatus(ScanReply.CANCELLED); f.unchangedInventory(); f.close();
            if (mode < 2) changedItem(test, mode + 1); else test.succeed();
        });
    }

    public static void cursorReplacementCancels(GameTestHelper test) {
        var f = new Fixture(test); f.begin(0);
        f.at(8, () -> { f.unchangedInventory(); f.menu.setCarried(f.tool.copy()); f.rememberInventory(); });
        f.watch(29, true, true, () -> { f.expectStatus(ScanReply.CANCELLED); f.unchangedInventory(); f.pass(); });
    }

    public static void missingParentIsAtomic(GameTestHelper test) {
        var f = new Fixture(test);
        f.knowledge.reset(); f.knowledge.setInitializedAspects(true);
        f.menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 5)); f.rememberInventory(); f.begin(0);
        f.watch(27, true, true, () -> {
            f.expectStatus(ScanReply.FAILED); f.unchangedInventory();
            f.knowledge.addDiscoveredAspect(Aspect.MAN.getTag()); f.knowledge.addDiscoveredAspect(Aspect.HUNGER.getTag());
            f.begin(0); f.watch(27, true, false, () -> {
                f.check(f.knowledge.hasScannedItem("@minecraft:diamond"), "fresh request succeeds after the actual missing parent prerequisites are supplied");
                f.exactPools(Map.of("vitreus", 6, "lucrum", 6)); f.expectStatus(ScanReply.SUCCESS); f.unchangedInventory(); f.pass();
            });
        });
    }

    public static void repeatAndOldRequestDoNotAward(GameTestHelper test) {
        var f = new Fixture(test); f.begin(0);
        f.watch(27, true, false, () -> {
            f.expectStatus(ScanReply.SUCCESS); f.markKnowledge();
            var consumed = f.request; dispatch.accept(f.player, consumed); f.capture();
            f.check(f.knowledge.save().equals(f.beforeKnowledge), "replaying the consumed request cannot award again");
            f.begin(0); f.watch(27, true, true, () -> {
                f.expectStatus(ScanReply.ALREADY_SCANNED); f.exactPools(Map.of("terra", 2)); f.unchangedInventory(); f.pass();
            });
        });
    }

    public static void selfScanAndNoContainerSpoof(GameTestHelper test) {
        var f = new Fixture(test);
        f.begin(-1); f.expectStatus(ScanReply.FAILED);
        f.player.containerMenu = f.player.inventoryMenu; f.menu = f.player.inventoryMenu;
        f.menu.setCarried(f.tool); f.rememberInventory(); f.begin(-1);
        f.watch(27, true, false, () -> {
            f.check(f.knowledge.getScannedEntities().equals(java.util.Set.of("@entity:player_" + f.player.getGameProfile().getName())), "self scan records only the authenticated actor's own native entity signature");
            int reward = f.knowledge.getAspectsDiscovered().values().stream().mapToInt(Integer::intValue).sum();
            f.check(reward == 16, "ordinary original player aspects total four base plus three times four RP");
            f.expectStatus(ScanReply.SUCCESS); f.unchangedInventory();
            f.begin(-1);
            // Native MAIN handlers and outbound writes may enqueue when called from an
            // existing tick callback. Bound delivery, but still require zero scan progress.
            f.watch(2, false, true, () -> {
                f.expectStatus(ScanReply.ALREADY_SCANNED);
                f.check(f.replies.stream().allMatch(reply -> reply.progress() == 0 && reply.status() != ScanReply.SCANNING), "known self is refused before any SCANNING receipt or progress tick");
                f.check(f.knowledge.save().equals(f.beforeKnowledge), "known self receives no repeated discoveries or RP");
                f.unchangedInventory(); f.pass();
            });
        });
    }

    public static void cancelAndHeartbeatTimeout(GameTestHelper test) {
        var f = new Fixture(test); f.begin(0);
        f.at(6, () -> dispatch.accept(f.player, new ScanRequest(f.request.containerId(), f.request.slot(), f.request.requestId(), false)));
        f.watch(28, true, true, () -> {
            f.expectStatus(ScanReply.CANCELLED); f.begin(0);
            f.watch(28, false, true, () -> { f.expectStatus(ScanReply.CANCELLED); f.unchangedInventory(); f.pass(); });
        });
    }

    public static void retiringPermissionCallback(GameTestHelper test) {
        var f = new Fixture(test);
        var backing = new SimpleContainer(1); backing.setItem(0, new ItemStack(Items.STONE, 64));
        final int[] retireAt = {Integer.MAX_VALUE}; final int[] completionPermissionCalls = {0}; final boolean[] retired = {false};
        var callbackSlot = new Slot(backing, 0, 0, 0) {
            @Override public boolean mayPickup(Player player) {
                if (!retired[0] && f.player.server.getTickCount() >= retireAt[0] && ++completionPermissionCalls[0] == 3) {
                    retired[0] = true;
                    f.menu.slots.set(0, new Slot(backing, 0, 0, 0));
                }
                return true;
            }
        };
        f.menu.slots.set(0, callbackSlot); f.rememberInventory(); f.begin(0);
        // With no heartbeat at tick25, the third permission callback is the final
        // physical revalidation after aspect resolution, immediately before the award.
        retireAt[0] = f.started + 25;
        f.watch(28, true, true, () -> {
            f.check(retired[0] && f.menu.slots.get(0) != callbackSlot, "actual native slot permission callback retired its owner at completion");
            f.expectStatus(ScanReply.FAILED); f.unchangedInventory(); f.pass();
        });
    }

    public static void disconnectAndReconnect(GameTestHelper test) {
        var f = new Fixture(test); f.begin(0);
        var profile = f.player.getGameProfile();
        f.at(7, () -> {
            // Real player-list removal owns the loader's logout/disconnect event.
            f.close(); assertDisconnectRemovedSession(test, f.player);
            f.driver.later(30, () -> {
                test.assertTrue(f.knowledge.save().equals(f.beforeKnowledge), "retired connection never completes its old pending scan");
                var fresh = new Fixture(test, profile);
                fresh.check(fresh.knowledge.getScannedItems().isEmpty(), "new actor starts with the declared empty knowledge fixture");
                fresh.begin(0); // request 1 is legal on a genuinely new connection.
                fresh.watch(27, true, false, () -> {
                    fresh.expectStatus(ScanReply.SUCCESS); fresh.exactPools(Map.of("terra", 2)); fresh.unchangedInventory(); fresh.pass();
                });
            });
        });
        f.watch(100, true, true, () -> { throw new AssertionError("disconnect fixture did not retire"); });
    }

    /** Observes private ownership without mutating it or introducing a production test API. */
    private static void assertDisconnectRemovedSession(GameTestHelper test, ServerPlayer player) {
        try {
            var servers = thaumcraft.common.lib.inventoryscan.InventoryScanServer.class.getDeclaredField("SERVERS");
            servers.setAccessible(true);
            var state = ((Map<?, ?>) servers.get(null)).get(player.server);
            if (state == null) return;
            var players = state.getClass().getDeclaredField("players"); players.setAccessible(true);
            test.assertTrue(!((Map<?, ?>) players.get(state)).containsKey(player),
                    "real native disconnect hook releases the exact actor immediately, before a fallback server tick");
        } catch (ReflectiveOperationException exception) { throw new AssertionError("test-only session owner observation failed", exception); }
    }

    /** Register scheduled GameTest entries once, before their map starts iterating. */
    private static Driver driver(GameTestHelper test) {
        var info = testInfo.apply(test);
        var existing = DRIVERS.get(info);
        if (existing != null) return existing;
        var created = new Driver(test, info);
        DRIVERS.put(info, created);
        return created;
    }

    private static final class Driver {
        final GameTestHelper test;
        final GameTestInfo info;
        final List<Fixture> fixtures = new ArrayList<>();
        final List<Delayed> actions = new ArrayList<>();
        boolean closed;
        record Delayed(int due, Runnable action) {}
        Driver(GameTestHelper test, GameTestInfo info) {
            this.test = test; this.info = info;
            info.addListener(new GameTestListener() {
                public void testStructureLoaded(GameTestInfo ignored) {}
                public void testPassed(GameTestInfo ignored, GameTestRunner runner) { release(); }
                public void testFailed(GameTestInfo ignored, GameTestRunner runner) { release(); }
                public void testAddedForRerun(GameTestInfo first, GameTestInfo second, GameTestRunner runner) { release(); }
            });
            test.onEachTick(this::tick);
        }
        void later(int delay, Runnable action) {
            actions.add(new Delayed(test.getLevel().getServer().getTickCount() + delay, action));
        }
        void tick() {
            if (closed) return;
            int now = test.getLevel().getServer().getTickCount();
            for (var action : List.copyOf(actions)) {
                if (closed) return;
                if (action.due() > now) continue;
                actions.remove(action); action.action().run();
            }
            for (var fixture : List.copyOf(fixtures)) {
                if (closed) return;
                fixture.tick();
            }
        }
        void release() {
            if (closed) return; closed = true;
            try { for (var fixture : List.copyOf(fixtures)) fixture.close(); }
            finally { actions.clear(); fixtures.clear(); DRIVERS.remove(info); }
        }
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper test; final Driver driver; final ServerPlayer player; final PlayerKnowledge knowledge;
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel channel = new EmbeddedChannel(connection);
        final ItemStack tool = new ItemStack(ThaumcraftContent.THAUMOMETER.get());
        final List<ScanReply> replies = new ArrayList<>();
        AbstractContainerMenu menu; ScanRequest request; int nextId; int started; boolean closed;
        CompoundTag beforeKnowledge; List<ItemStack> beforeInventory; ItemStack beforeCursor;
        Runnable waiting; int due; boolean heartbeat; boolean neverAward;

        Fixture(GameTestHelper test) { this(test, new GameProfile(UUID.randomUUID(), "tcis-" + UUID.randomUUID().toString().substring(0, 8))); }
        Fixture(GameTestHelper test, GameProfile profile) {
            this.test = test;
            if (login == null || dispatch == null || testInfo == null) throw new IllegalStateException("The real loader test platform was not installed");
            var level = test.getLevel();
            player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
            driver = driver(test);
            driver.fixtures.add(this);
            try {
                login.join(player, connection);
                player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true); player.setInvulnerable(true);
                var pos = test.absolutePos(new net.minecraft.core.BlockPos(1, 3, 1)); player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
                player.openMenu(new SimpleMenuProvider((id, inventory, actor) -> ChestMenu.oneRow(id, inventory), Component.literal("scan runtime fixture")));
                menu = player.containerMenu; check(menu instanceof ChestMenu, "native chest menu is actually open");
                menu.getSlot(0).set(new ItemStack(Items.STONE, 64)); menu.setCarried(tool);
                knowledge = KnowledgeAccess.get(player); knowledge.reset(); knowledge.setInitializedAspects(true);
                for (var aspect : Aspect.aspects.values()) knowledge.addDiscoveredAspect(aspect.getTag());
                capture(); replies.clear(); rememberInventory(); markKnowledge();
            } catch (RuntimeException | Error error) { close(); throw error; }
        }

        void begin(int slot) { replies.clear(); markKnowledge(); issue(menu.containerId, slot); started = player.server.getTickCount(); }
        void issue(int containerId, int slot) {
            started = player.server.getTickCount();
            request = new ScanRequest(containerId, slot, ++nextId, true);
            dispatch.accept(player, request); capture();
        }
        void markKnowledge() { beforeKnowledge = knowledge.save(); }
        void watch(int elapsed, boolean keepAlive, boolean noAwards, Runnable next) {
            due = started + elapsed; heartbeat = keepAlive; neverAward = noAwards; waiting = next;
        }
        void at(int delay, Runnable action) {
            driver.later(delay, () -> { if (!closed) guarded(action); });
        }
        void tick() {
            if (closed || waiting == null) return;
            guarded(() -> {
                int elapsed = player.server.getTickCount() - started;
                if (heartbeat && elapsed < 25 && elapsed % 5 == 0) dispatch.accept(player, request);
                capture();
                if (neverAward || elapsed < 25) {
                    CompoundTag now = knowledge.save();
                    check(now.equals(beforeKnowledge), "no knowledge before 25 actual server ticks, after cancellation, or after rejection; changed: " + changed(beforeKnowledge, now));
                }
                if (player.server.getTickCount() < due) return;
                var next = waiting; waiting = null; next.run();
            });
        }
        /** The top-level entries of the saved knowledge that differ, for a failure message. */
        static String changed(CompoundTag before, CompoundTag now) {
            var keys = new java.util.TreeSet<>(before.getAllKeys()); keys.addAll(now.getAllKeys());
            var text = new StringBuilder();
            for (String key : keys) {
                if (!java.util.Objects.equals(before.get(key), now.get(key))) text.append(key).append(": ").append(before.get(key)).append(" -> ").append(now.get(key)).append("; ");
            }
            return text.length() > 600 ? text.substring(0, 600) : text.toString();
        }
        void guarded(Runnable action) { try { action.run(); } catch (RuntimeException | Error error) { close(); throw error; } }
        void expectStatus(int status) {
            capture(); check(replies.stream().anyMatch(r -> r.requestId() == request.requestId() && r.status() == status), "actual native S2C reply contains status " + status + ", got " + replies);
        }
        void exactPools(Map<String, Integer> expected) {
            var actual = new java.util.TreeMap<String, Integer>();
            knowledge.getAspectsDiscovered().forEach((key, amount) -> { if (amount != 0) actual.put(key, amount); });
            check(actual.equals(expected), "exact native Thaumcraft RP expected " + expected + ", got " + actual);
        }
        void rememberInventory() {
            beforeInventory = new ArrayList<>();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) beforeInventory.add(player.getInventory().getItem(i).copy());
            for (var slot : menu.slots) beforeInventory.add(slot.getItem().copy());
            beforeCursor = menu.getCarried().copy();
        }
        void unchangedInventory() {
            int i = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) check(ItemStack.matches(beforeInventory.get(i++), player.getInventory().getItem(slot)), "all player inventory count/components remain unchanged");
            for (var slot : menu.slots) check(ItemStack.matches(beforeInventory.get(i++), slot.getItem()), "all menu inventory count/components remain unchanged");
            check(ItemStack.matches(beforeCursor, menu.getCarried()), "scanning never consumes or modifies the actual cursor tool");
        }
        void capture() {
            channel.runPendingTasks(); Object packet;
            // Drain what the connection wrote, so the embedded channel never grows; the receipts come from the transport.
            while ((packet = channel.readOutbound()) != null) ReferenceCountUtil.release(packet);
            var sent = SENT.remove(player);
            if (sent != null) replies.addAll(sent);
        }
        void check(boolean condition, String message) { test.assertTrue(condition, message); }
        void pass() { close(); test.succeed(); }
        @Override public void close() {
            if (closed) return; closed = true; waiting = null;
            try { var list = player.server.getPlayerList(); if (list.getPlayer(player.getUUID()) == player) player.connection.onDisconnect(new DisconnectionDetails(Component.literal("isolated GameTest cleanup"))); player.discard(); }
            finally { SENT.remove(player); channel.finishAndReleaseAll(); }
        }
    }
}
