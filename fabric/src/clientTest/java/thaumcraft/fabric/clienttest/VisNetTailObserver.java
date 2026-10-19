package thaumcraft.fabric.clienttest;

import com.google.gson.GsonBuilder;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileNodeConverter;
import thaumcraft.common.tiles.TileNodeEnergized;

/** Root-only independent evidence through the LAST live SERVER_POST and normal
 * server stop. Never ticks, drains, saves, loads, freezes or edits a world/item.
 * The story removes its own ledger before PNG163; this observer does NOT. */
final class VisNetTailObserver {
    private static final BlockPos TARGET = new BlockPos(1551, 218, 377);
    private final MinecraftServer server;
    private final Path output;
    private final String runId;
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private final List<Map<String, Object>> captures = new ArrayList<>();
    private final TickEvent.Server post = this::afterTick;
    private final LifecycleEvent.ServerState stopped = this::afterStop;
    private final Map<String, Object> summary = new LinkedHashMap<>();
    private volatile Throwable failure;
    private boolean sawEnergized, sawReversion, closed;
    private int posts;
    private Map<String, Object> last = Map.of();

    VisNetTailObserver(MinecraftServer server, Path directory, String runId) {
        if (server == null || !runId.matches("client-smoke-[a-f0-9-]{36}")) throw new IllegalArgumentException("Owned VisNet run required");
        this.server = server; this.runId = runId;
        output = directory.resolve("visnet-tail-" + runId + ".json");
        if (Files.exists(output)) throw new IllegalStateException("Refuse overwriting a tail evidence file");
        TickEvent.SERVER_POST.register(post);
        ClientTestServerLifecycle.register(server, stopped);
    }

    void check() {
        if (failure != null) throw new IllegalStateException("Independent VisNet tail observation failed", failure);
    }
    void capture(String event, String name) {
        if (!name.matches("(?:15[4-9]|16[0-3])-.*\\.png")) return;
        synchronized (captures) {
            captures.add(Map.of("event", event, "name", name, "utc", Instant.now().toString(),
                    "monotonic_nanos", System.nanoTime(), "not_a_server_tick_or_stock_snapshot", true));
        }
    }

    private void afterTick(MinecraftServer actual) {
        if (actual != server || closed) return;
        try {
            if (!server.isSameThread()) throw new IllegalStateException("Tail reads require the actual server thread");
            ServerLevel level = server.getLevel(Level.OVERWORLD);
            if (level == null) return;
            var chunk = level.getChunkSource().getChunkNow(TARGET.getX() >> 4, TARGET.getZ() >> 4);
            if (chunk == null) return; // Unknown is not a removed/zero node. Never force load.
            var node = chunk.getBlockEntities().get(TARGET);
            var row = new LinkedHashMap<String, Object>();
            row.put("native_POST", ++posts); row.put("native_tick", level.getGameTime());
            row.put("utc", Instant.now().toString()); row.put("monotonic_nanos", System.nanoTime());
            row.put("world", server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString());
            if (node instanceof TileNode normal) {
                row.put("kind", "ORDINARY"); row.put("UUID", normal.getScanId());
                row.put("actual_count", normal.getTickCount()); row.put("actual_lock", normal.getLock());
                row.put("actual_cached_regeneration", normal.getRegenerationTicks());
                row.put("actual_recharge_wait", normal.getRechargeWait());
                row.put("actual_current_INCLUDING_ZERO_KEYS", amounts(normal.getAspects()));
                row.put("actual_BASE", amounts(normal.getAspectsBase()));
                row.put("actual_type", normal.getNodeType().name());
                row.put("actual_modifier", normal.getNodeModifier() == null ? "NONE" : normal.getNodeModifier().name());
                row.put("actual_natural", normal.isNatural());
                if (sawEnergized && !sawReversion) {
                    sawReversion = true; summary.put("first_actual_reversion_POST", Map.copyOf(row));
                }
                if (sawReversion && normal.getTickCount() >= 1200) summary.put("original_prune_boundary_was_reached", true);
            } else if (node instanceof TileNodeEnergized source) {
                sawEnergized = true; row.put("kind", "ENERGIZED");
                row.put("actual_BASE", amounts(source.getAuraBase())); row.put("actual_refill_epoch", source.getRefillEpoch());
            } else row.put("kind", node == null ? "NONE" : node.getClass().getName());
            var upperChunk = level.getChunkSource().getChunkNow(TARGET.getX() >> 4, TARGET.getZ() >> 4);
            var upper = upperChunk == null ? null : upperChunk.getBlockEntities().get(TARGET.above());
            if (upper instanceof TileNodeConverter converter) row.put("actual_converter_count_status", List.of(converter.getCount(), converter.getStatus()));
            last = Map.copyOf(row);
            // Keep all returned-node POSTs, including PNG163 and normal closing.
            // Before that, transitions and100-step samples suffice for this
            // independent tail check; the story owns full warmup/epoch accounting.
            if (sawReversion || posts % 100 == 0 || rows.isEmpty()
                    || !rows.getLast().get("kind").equals(row.get("kind"))) rows.add(last);
        } catch (RuntimeException | Error error) { if (failure == null) failure = error; }
    }

    private void afterStop(MinecraftServer actual) {
        if (actual != server || closed) return;
        closed = true;
        TickEvent.SERVER_POST.unregister(post); ClientTestServerLifecycle.unregister(stopped);
        summary.put("status", failure == null ? "STOPPED" : "FAILED"); summary.put("run_id", runId);
        summary.put("stopped_utc", Instant.now().toString()); summary.put("actual_SERVER_POSTs", posts);
        summary.put("saw_actual_energized", sawEnergized); summary.put("saw_actual_reversion", sawReversion);
        summary.put("last_actual_live_POST", last); summary.put("ordinary_and_transition_rows", rows);
        synchronized (captures) { summary.put("capture_events", List.copyOf(captures)); }
        summary.put("scope", "Pure SERVER_POST through normal server stop; actual stopped chunk NBT must still be independently read. No world save/tick/phase/RNG/resource operations.");
        if (failure != null) { var text = new StringWriter(); failure.printStackTrace(new PrintWriter(text)); summary.put("error", text.toString()); }
        try {
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(summary) + "\n", StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE_NEW);
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot record independent stopped VisNet tail", error); }
    }

    private static List<Map<String, Object>> amounts(AspectList aspects) {
        var result = new ArrayList<Map<String, Object>>();
        for (var aspect : aspects.getAspects()) result.add(Map.of("tag", aspect.getTag(), "amount", aspects.getAmount(aspect)));
        return List.copyOf(result);
    }
}
