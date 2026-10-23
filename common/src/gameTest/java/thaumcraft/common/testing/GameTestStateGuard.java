package thaumcraft.common.testing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.common.config.ThaumcraftBiomeSettings;
import thaumcraft.common.config.ThaumcraftSettings;
import thaumcraft.common.config.ThaumcraftTaintSettings;
import thaumcraft.common.lib.bore.BoreNativeContext;
import thaumcraft.common.entities.golems.EntityGolemBase;

/** Test-only ownership ledger. Never restore somebody else's live fixture to a global snapshot.
 * Check after tickInternal, NOT finish: succeed() may run inside a try/finally whose restore
 * has not executed yet. Ownership is recorded only while that scenario's tick is running.
 */
public final class GameTestStateGuard {
    private record Snapshot(List<Object> settings, Set<?> recipes, boolean live) {}
    private static final Map<GameTestInfo, Snapshot> START = new IdentityHashMap<>();
    private static final Map<Object, GameTestInfo> RESOURCES = new HashMap<>();
    private static final Map<GameTestInfo, List<String>> VIOLATIONS = new IdentityHashMap<>();
    private static final Map<GameTestInfo, List<WorkerWatch>> WORKERS = new IdentityHashMap<>();
    private static GameTestInfo current;

    private GameTestStateGuard() {}

    /** Integrity snapshots cannot detect a healthy worker repeatedly completing an unusable path. */
    public static void watchWorker(EntityGolemBase golem, Runnable cleanup) {
        if (current != null) WORKERS.computeIfAbsent(current, ignored -> new ArrayList<>()).add(new WorkerWatch(golem, cleanup));
    }

    private static final class WorkerWatch {
        final EntityGolemBase golem;
        final Runnable cleanup;
        java.util.UUID target;
        int incompleteApproaches;
        boolean outsideReach;
        int carried;
        String lastState;

        WorkerWatch(EntityGolemBase golem, Runnable cleanup) { this.golem = golem; this.cleanup = cleanup; }

        void observe(GameTestInfo info) {
            if (golem.isRemoved()) return;
            lastState = GolemState.of(golem);
            int cargo = golem.getCarriedItem().getCount();
            if (cargo != carried) { incompleteApproaches = 0; carried = cargo; }
            var item = golem.unfinishedPickupTarget();
            if (item == null) { outsideReach = false; return; }
            if (!item.getUUID().equals(target)) { target = item.getUUID(); incompleteApproaches = 0; }
            if (!outsideReach) incompleteApproaches++;
            outsideReach = true;
            if (incompleteApproaches < 3 || info.isDone()) return;
            String message = "GameTest work progress: " + info.getTestName()
                    + ": Pickup completed three approaches outside distanceSqr<=2; actual=" + golem.distanceToSqr(item);
            thaumcraft.common.Thaumcraft.LOG.error("{}; {}", message, lastState);
            cleanup.run();
            info.fail(new AssertionError(message));
        }
    }

    public static void enter(GameTestInfo info) {
        current = info;
    }

    public static void begin(GameTestInfo info) {
        START.computeIfAbsent(info, ignored -> snapshot(info.getLevel()));
    }

    private static Snapshot snapshot(ServerLevel level) {
        return new Snapshot(List.of(ThaumcraftSettings.server(), ThaumcraftTaintSettings.values(),
                ThaumcraftBiomeSettings.current(), ThaumcraftBiomeSettings.spawns(),
                ThaumcraftBiomeSettings.worldgen(), ThaumcraftBiomeSettings.regen(),
                level.getServer().isPvpAllowed(), thaumcraft.common.lib.WarpEvents.isWussMode(),
                level.getGameRules().createTag(), level.getServer().getWorldData().getDifficulty(),
                level.getServer().getWorldData().isDifficultyLocked(), level.random,
                Map.copyOf(thaumcraft.common.lib.world.NodeEcologyPolicy.dimensionBlacklist),
                Map.copyOf(thaumcraft.common.lib.world.NodeEcologyPolicy.biomeBlacklist)),
                Set.copyOf(level.getRecipeManager().getRecipes()),
                BoreNativeContext.isLiveLevel(level));
    }

    public static void acquired(Object resource) {
        if (current != null) RESOURCES.putIfAbsent(resource, current);
    }

    public static void released(Object resource) {
        GameTestInfo owner = RESOURCES.remove(resource);
        if (owner != null && current != null && owner != current) {
            VIOLATIONS.computeIfAbsent(current, ignored -> new ArrayList<>())
                    .add("released " + owner.getTestName() + "'s " + resource);
        }
    }

    public static void exit() { current = null; }

    public static void leave(GameTestInfo info) {
        var workers = WORKERS.get(info);
        if (workers != null) for (var worker : List.copyOf(workers)) worker.observe(info);
        current = null;
        if (!info.isDone()) return;
        WORKERS.remove(info);
        if (info.getError() != null && workers != null) for (var worker : workers)
            thaumcraft.common.Thaumcraft.LOG.error("GameTest work witness: {}: {}", info.getTestName(), worker.lastState);
        Snapshot before = START.remove(info);
        if (before == null) return;
        Snapshot after = snapshot(info.getLevel());
        var leaks = new ArrayList<String>();
        var violations = VIOLATIONS.remove(info);
        if (violations != null) leaks.addAll(violations);
        if (!before.settings.equals(after.settings)) leaks.add("mutated server configuration");
        if (!before.recipes.equals(after.recipes)) leaks.add("replaced recipe corpus");
        if (before.live != after.live) leaks.add("changed shared level lifetime");
        RESOURCES.entrySet().removeIf(entry -> {
            if (entry.getValue() != info) return false;
            leaks.add("unreleased " + entry.getKey());
            return true;
        });
        if (!leaks.isEmpty()) {
            String message = "GameTest isolation: " + info.getTestName() + ": " + String.join("; ", leaks);
            thaumcraft.common.Thaumcraft.LOG.error(message);
            var failure = new AssertionError(message);
            if (info.getError() != null) failure.addSuppressed(info.getError());
            info.fail(failure);
        }
    }

    /** Value keys distinguish levels/sources and ticket kinds, including radius and payload. */
    public static Object resource(String kind, Object... parts) {
        return List.of(kind, List.of(parts));
    }
}
