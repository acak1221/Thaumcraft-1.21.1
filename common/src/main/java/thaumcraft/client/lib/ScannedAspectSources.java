package thaumcraft.client.lib;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.research.ObjectAspectResolver;

/** Client-session-owned source index. Rendering never derives a catalogue of item aspects. */
public final class ScannedAspectSources {
    private static final Map<ScannedAspectSources, Boolean> INSTANCES = new WeakHashMap<>();
    private static final long WORK_NANOS = 2_000_000L;
    private static boolean initialized;
    private static Object metadataConnection, metadataLevel, metadata;
    private static long checkedMetadata, resolutions;

    public record Source(ItemStack item, Aspect aspect, int amount) {}
    private record Key(Item item, DataComponentPatch components) {
        static Key of(ItemStack stack) { return new Key(stack.getItem(), stack.getComponentsPatch()); }
    }
    private record Resolved(ItemStack item, List<Source> rows, String name) {}

    private final Supplier<? extends Collection<ItemStack>> ingredients;
    private final Map<Key, Resolved> resolved = new HashMap<>();
    private final Map<Key, ItemStack> requested = new LinkedHashMap<>();
    private final ArrayDeque<ItemStack> pending = new ArrayDeque<>();
    private List<ItemStack> candidates = List.of();
    private List<Source> all = List.of();
    private Map<Aspect, List<Source>> byAspect = Map.of();
    private Map<Key, List<Source>> byItem = Map.of();
    private Object player, level, connection, previousMetadata;
    private Object ownerConnection;
    private long scanRevision = -1;
    private boolean dirty = true, ingredientsDirty = true, retired;

    public ScannedAspectSources(Supplier<? extends Collection<ItemStack>> ingredients) {
        this.ingredients = ingredients;
        ownerConnection = Minecraft.getInstance().getConnection();
        synchronized (INSTANCES) { INSTANCES.put(this, Boolean.TRUE); }
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        ClientTickEvent.CLIENT_POST.register(ScannedAspectSources::tickAll);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(ignored -> clearAll(true));
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(ignored -> clearAll(false));
    }

    /** Counts actual derivations, allowing native probes to detect expensive work in repeated GUI lookups. */
    public static long resolvedItemCount() { return resolutions; }

    public static boolean isIdle() {
        for (ScannedAspectSources index : instances()) {
            synchronized (index) {
                if (!index.context()) continue;
                if (index.dirty || index.ingredientsDirty || !index.pending.isEmpty()) return false;
            }
        }
        return true;
    }

    public synchronized void invalidateIngredients() { ingredientsDirty = true; dirty = true; }

    public synchronized boolean isReady() { return context() && !dirty && !ingredientsDirty && pending.isEmpty(); }

    public synchronized boolean isPending() { return context() && (dirty || ingredientsDirty || !pending.isEmpty()); }

    public synchronized List<Source> forAspect(Aspect aspect) {
        return context() ? byAspect.getOrDefault(aspect, List.of()) : List.of();
    }

    public synchronized List<Source> all() { return context() ? all : List.of(); }

    /** Inventory-only component variants may not be present in a viewer's ingredient index. */
    public synchronized List<Source> forItem(ItemStack item) {
        if (!context() || !scanned(item)) return List.of();
        Key key = Key.of(item);
        List<Source> rows = byItem.get(key);
        if (rows != null) return rows;
        Resolved cached = resolved.get(key);
        if (cached != null) return cached.rows();
        ItemStack single = item.copyWithCount(1);
        requested.put(key, single);
        // One uncatalogued physical variant is bounded work; never resolve the whole index here.
        Resolved value = resolved.computeIfAbsent(key, ignored -> resolve(single));
        dirty = true;
        return value.rows();
    }

    private static List<ScannedAspectSources> instances() {
        synchronized (INSTANCES) { return new ArrayList<>(INSTANCES.keySet()); }
    }

    private static void clearAll(boolean retire) {
        metadataConnection = null; metadataLevel = null; metadata = null; checkedMetadata = 0;
        for (ScannedAspectSources index : instances()) synchronized (index) {
            if (retire && index.ownerConnection != null) index.retired = true;
            index.clear();
        }
    }

    private void clear() {
        if (retired) ownerConnection = null;
        player = null; level = null; connection = null; previousMetadata = null; scanRevision = -1;
        resolved.clear(); requested.clear(); pending.clear(); candidates = List.of();
        all = List.of(); byAspect = Map.of(); byItem = Map.of();
        dirty = true; ingredientsDirty = true;
    }

    private boolean context() {
        if (retired) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.getConnection() == null) {
            if (ownerConnection != null && ownerConnection != client.getConnection()) retired = true;
            clear();
            return false;
        }
        if (ownerConnection == null) ownerConnection = client.getConnection();
        if (ownerConnection != client.getConnection()) {
            retired = true; clear(); return false;
        }
        if (player != client.player || level != client.level || connection != client.getConnection()) {
            clear(); player = client.player; level = client.level; connection = client.getConnection();
        }
        long revision = KnowledgeAccess.get(client.player).itemScanRevision();
        if (scanRevision != revision) {
            scanRevision = revision; dirty = true;
            // A reset or same-size replacement must remove cached rows before another render/click.
            publish(all.stream().filter(source -> scanned(source.item())).toList());
        }
        return true;
    }

    private static boolean scanned(ItemStack item) {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && client.level != null && !item.isEmpty()
                && ThaumcraftApiHelper.hasScannedItem(client.player, item);
    }

    private static void tickAll(Minecraft client) {
        List<ScannedAspectSources> indexes = instances();
        if (indexes.isEmpty()) return;
        if (client.player == null || client.level == null || client.getConnection() == null) {
            clearAll(client.getConnection() == null); return;
        }
        long now = System.nanoTime();
        if (metadataConnection != client.getConnection() || metadataLevel != client.level || now - checkedMetadata >= 1_000_000_000L) {
            metadata = ObjectAspectResolver.viewerCacheToken(client.level);
            metadataConnection = client.getConnection(); metadataLevel = client.level; checkedMetadata = now;
        }
        // Bound work for the whole client, including simultaneous EMI and JEI installations.
        long deadline = System.nanoTime() + WORK_NANOS;
        for (ScannedAspectSources index : indexes) {
            synchronized (index) { index.tick(deadline); }
        }
    }

    private void tick(long deadline) {
        if (!context()) return;
        if (!java.util.Objects.equals(previousMetadata, metadata)) {
            previousMetadata = metadata; resolved.clear(); pending.clear(); dirty = true;
        }
        if (ingredientsDirty) {
            var values = ingredients.get();
            candidates = values == null ? List.of() : values.stream().filter(item -> !item.isEmpty())
                    .map(item -> item.copyWithCount(1)).toList();
            ingredientsDirty = false; dirty = true;
        }
        if (dirty) {
            pending.clear();
            Map<Key, ItemStack> known = new LinkedHashMap<>();
            for (ItemStack item : candidates) if (scanned(item)) known.putIfAbsent(Key.of(item), item);
            requested.entrySet().removeIf(entry -> !scanned(entry.getValue()));
            requested.forEach(known::putIfAbsent);
            resolved.keySet().retainAll(known.keySet());
            for (var entry : known.entrySet()) if (!resolved.containsKey(entry.getKey())) pending.add(entry.getValue());
            dirty = false;
            if (pending.isEmpty()) publishKnown();
        }
        boolean changed = false;
        while (!pending.isEmpty() && System.nanoTime() < deadline) {
            ItemStack item = pending.removeFirst();
            Key key = Key.of(item);
            if (scanned(item) && !resolved.containsKey(key)) { resolved.put(key, resolve(item)); changed = true; }
        }
        if (changed && pending.isEmpty()) publishKnown();
    }

    private Resolved resolve(ItemStack item) {
        resolutions++;
        var tags = ObjectAspectResolver.get(item, Minecraft.getInstance().level);
        List<Source> rows = new ArrayList<>();
        if (tags != null) for (Aspect aspect : tags.getAspectsSorted()) {
            int amount = tags.getAmount(aspect);
            if (amount > 0) rows.add(new Source(item, aspect, amount));
        }
        return new Resolved(item, List.copyOf(rows), item.getHoverName().getString());
    }

    private void publishKnown() {
        List<Resolved> values = resolved.values().stream().filter(value -> scanned(value.item()))
                .sorted(Comparator.comparing(Resolved::name)).toList();
        publish(values.stream().flatMap(value -> value.rows().stream()).toList());
    }

    private void publish(List<Source> rows) {
        if (all.equals(rows)) return;
        all = List.copyOf(rows);
        Map<Aspect, List<Source>> aspects = new HashMap<>();
        Map<Key, List<Source>> items = new HashMap<>();
        for (Source source : all) {
            aspects.computeIfAbsent(source.aspect(), ignored -> new ArrayList<>()).add(source);
            items.computeIfAbsent(Key.of(source.item()), ignored -> new ArrayList<>()).add(source);
        }
        aspects.replaceAll((key, values) -> List.copyOf(values));
        items.replaceAll((key, values) -> List.copyOf(values));
        byAspect = Map.copyOf(aspects); byItem = Map.copyOf(items);
    }
}
