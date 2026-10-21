package thaumcraft.common.lib.research;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;

/** TC4 knowledge owned by one player, with no process-wide player-name cache. */
public final class PlayerKnowledge {
    public static final int DATA_VERSION = 1;
    public static final List<String> PRIMAL_TAGS = List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio");

    // Presence is discovery; the value is the research pool, including zero.
    private final Map<String, Integer> discoveredAspects = new LinkedHashMap<>();
    private final Set<String> completedResearch = new LinkedHashSet<>();
    private final Set<String> scannedItems = new LinkedHashSet<>();
    private final Set<String> scannedEntities = new LinkedHashSet<>();
    private final Set<String> scannedPhenomena = new LinkedHashSet<>();
    private boolean initializedAspects;
    private int warpPerm;
    private int warpSticky;
    private int warpTemp;
    private int warpCounter;

    public PlayerKnowledge() {
        addDiscoveredPrimalAspects();
    }

    public void initializePrimalAspects(RandomSource random) {
        if (initializedAspects) return;
        for (String tag : PRIMAL_TAGS) {
            discoveredAspects.put(tag, 15 + random.nextInt(5));
        }
        initializedAspects = true;
    }

    public boolean hasInitializedAspects() { return initializedAspects; }
    public void setInitializedAspects(boolean initialized) { initializedAspects = initialized; }

    public void addDiscoveredPrimalAspects() {
        for (String tag : PRIMAL_TAGS) discoveredAspects.putIfAbsent(tag, 0);
    }

    public boolean hasDiscoveredAspect(String tag) {
        return tag != null && discoveredAspects.containsKey(tag);
    }

    public boolean addDiscoveredAspect(String tag) {
        if (tag == null || tag.isEmpty() || discoveredAspects.containsKey(tag)) return false;
        discoveredAspects.put(tag, 0);
        return true;
    }

    public Map<String, Integer> getAspectsDiscovered() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(discoveredAspects));
    }

    public int getAspectPoolFor(String tag) {
        return tag == null ? 0 : discoveredAspects.getOrDefault(tag, 0);
    }

    public boolean setAspectPool(String tag, int amount) {
        if (tag == null || tag.isEmpty()) return false;
        discoveredAspects.put(tag, amount);
        return true;
    }

    public boolean addAspectPool(String tag, int amount) {
        if (tag == null || tag.isEmpty() || amount == 0) return false;
        int current = getAspectPoolFor(tag);
        if (amount > 0) {
            discoveredAspects.put(tag, current + amount);
            return true;
        }
        if (current <= 0) return false;
        // TC4 PlayerKnowledge.addAspectPool reports success on overdraw, but
        // AspectList.reduce leaves the positive pool unchanged in that case.
        if ((long) current + amount >= 0) discoveredAspects.put(tag, current + amount);
        return true;
    }

    public Set<String> getResearchComplete() { return snapshot(completedResearch); }
    public boolean isResearchComplete(String key) { return completedResearch.contains(key); }
    public boolean addResearch(String key) { return addKey(completedResearch, key); }
    public boolean removeResearch(String key) { return completedResearch.remove(key); }
    public Set<String> getScannedItems() { return snapshot(scannedItems); }
    public Set<String> getScannedEntities() { return snapshot(scannedEntities); }
    public Set<String> getScannedPhenomena() { return snapshot(scannedPhenomena); }
    public boolean hasScannedItem(String key) { return scannedItems.contains(key); }
    public boolean hasScannedEntity(String key) { return scannedEntities.contains(key); }
    public boolean hasScannedPhenomena(String key) { return scannedPhenomena.contains(key); }
    public boolean scanItem(String key) { return addScanKey(scannedItems, key); }
    public boolean scanEntity(String key) { return addScanKey(scannedEntities, key); }
    public boolean scanPhenomena(String key) { return addScanKey(scannedPhenomena, key); }

    public int getWarpPerm() { return warpPerm; }
    public int getWarpSticky() { return warpSticky; }
    public int getWarpTemp() { return warpTemp; }
    public int getWarpCounter() { return warpCounter; }
    public int getTotalWarp() { return warpPerm + warpSticky + warpTemp; }
    public void setWarpPerm(int value) { warpPerm = Math.max(0, value); }
    public void setWarpSticky(int value) { warpSticky = Math.max(0, value); }
    public void setWarpTemp(int value) { warpTemp = Math.max(0, value); }
    public void setWarpCounter(int value) { warpCounter = value; }
    public void addWarpPerm(int value) { setWarpPerm(warpPerm + value); }
    public void addWarpSticky(int value) { setWarpSticky(warpSticky + value); }
    public void addWarpTemp(int value) { setWarpTemp(warpTemp + value); }

    /**
     * Original PlayerKnowledge:24-34 - the wipe of a player drops the research, the discovered
     * aspects, the scanned objects, entities and phenomena and the three warp values, but it
     * keeps the warp counter of the player.
     */
    public void reset() {
        discoveredAspects.clear();
        completedResearch.clear();
        scannedItems.clear();
        scannedEntities.clear();
        scannedPhenomena.clear();
        initializedAspects = false;
        warpPerm = warpSticky = warpTemp = 0;
        addDiscoveredPrimalAspects();
    }

    /** A faithful live-state copy; persistence filtering must never shape snapshots. */
    public PlayerKnowledge copy() {
        PlayerKnowledge copy = new PlayerKnowledge();
        copy.copyFrom(this);
        return copy;
    }

    public void copyFrom(PlayerKnowledge source) {
        if (source == this) return;
        discoveredAspects.clear();
        discoveredAspects.putAll(source.discoveredAspects);
        replace(completedResearch, source.completedResearch);
        replace(scannedItems, source.scannedItems);
        replace(scannedEntities, source.scannedEntities);
        replace(scannedPhenomena, source.scannedPhenomena);
        initializedAspects = source.initializedAspects;
        warpPerm = source.warpPerm;
        warpSticky = source.warpSticky;
        warpTemp = source.warpTemp;
        warpCounter = source.warpCounter;
    }

    /** Native 1.21.1 data only. This is not a reader for legacy .thaum files. */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", DATA_VERSION);
        tag.putBoolean("initializedAspects", initializedAspects);
        tag.putInt("warpPerm", warpPerm);
        tag.putInt("warpSticky", warpSticky);
        tag.putInt("warpTemp", warpTemp);
        tag.putInt("warpCounter", warpCounter);
        ListTag aspects = new ListTag();
        for (Map.Entry<String, Integer> entry : discoveredAspects.entrySet()) {
            CompoundTag aspect = new CompoundTag();
            aspect.putString("key", entry.getKey());
            aspect.putInt("amount", entry.getValue());
            aspects.add(aspect);
        }
        tag.put("discoveredAspects", aspects);
        writeSet(tag, "researchComplete", completedResearch);
        writeSet(tag, "scannedItems", scannedItems);
        writeSet(tag, "scannedEntities", scannedEntities);
        writeSet(tag, "scannedPhenomena", scannedPhenomena);
        return tag;
    }

    public void load(CompoundTag tag) {
        reset();
        initializedAspects = tag.getBoolean("initializedAspects");
        setWarpPerm(tag.getInt("warpPerm"));
        setWarpSticky(tag.getInt("warpSticky"));
        setWarpTemp(tag.getInt("warpTemp"));
        setWarpCounter(tag.getInt("warpCounter"));
        ListTag aspects = tag.getList("discoveredAspects", Tag.TAG_COMPOUND);
        for (int i = 0; i < aspects.size(); i++) {
            CompoundTag entry = aspects.getCompound(i);
            setAspectPool(entry.getString("key"), entry.getInt("amount"));
        }
        readSet(tag, "researchComplete", completedResearch);
        readSet(tag, "scannedItems", scannedItems);
        readSet(tag, "scannedEntities", scannedEntities);
        readSet(tag, "scannedPhenomena", scannedPhenomena);
    }

    private static void writeSet(CompoundTag tag, String key, Set<String> values) {
        ListTag list = new ListTag();
        for (String value : values) list.add(StringTag.valueOf(value));
        tag.put(key, list);
    }

    private static void readSet(CompoundTag tag, String key, Set<String> values) {
        values.clear();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) addKey(values, list.getString(i));
    }

    private static boolean addKey(Set<String> values, String key) {
        return key != null && !key.isEmpty() && values.add(key);
    }

    private static boolean addScanKey(Set<String> values, String key) {
        boolean changed = addKey(values, key);
        if (key != null && key.startsWith("#")) values.remove("@" + key.substring(1));
        return changed;
    }

    private static void replace(Set<String> target, Set<String> source) {
        target.clear();
        target.addAll(source);
    }

    private static Set<String> snapshot(Set<String> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }
}
