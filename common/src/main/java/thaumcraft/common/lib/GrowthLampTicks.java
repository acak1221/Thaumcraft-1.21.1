package thaumcraft.common.lib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.common.tiles.TileArcaneLampGrowth;

/** Persistent one-tick delayed ORIGINAL plant updates. Modern crops grow in randomTick, not their empty scheduled tick.
 * A paid queued update survives lamp removal and retains native same-position/block deduplication. */
public final class GrowthLampTicks extends SavedData {
    private static final String ID = "thaumcraft_growth_lamp_ticks";
    private static final Factory<GrowthLampTicks> FACTORY = new Factory<>(GrowthLampTicks::new, GrowthLampTicks::load, null);
    private record Key(BlockPos pos, Block block) {}
    private record Due(Key key, long time) {}
    private record Schedule(TileArcaneLampGrowth lamp, BlockPos pos, BlockState state) {}
    private static final ThreadLocal<Schedule> SCHEDULING = new ThreadLocal<>();
    private final Map<Key, Due> pending = new LinkedHashMap<>();
    private final ListTag rejected = new ListTag();
    public GrowthLampTicks() {}
    public static void requirePaidSchedule(TileArcaneLampGrowth lamp, BlockPos pos, BlockState state) {
        var frame = SCHEDULING.get();
        if (frame == null || frame.lamp() != lamp || frame.pos() != pos || frame.state() != state) throw new IllegalStateException("Growth debit requires its exact native scheduler frame");
    }
    public static boolean schedule(TileArcaneLampGrowth lamp, BlockPos pos, BlockState state) {
        if (!(lamp.getLevel() instanceof ServerLevel level) || SCHEDULING.get() != null || !lamp.isNativeGrowthPhase(pos, state)
                || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || level.getBlockState(pos) != state) return false;
        var queue = level.getDataStorage().computeIfAbsent(FACTORY, ID);
        var key = new Key(pos.immutable(), state.getBlock()); var due = new Due(key, level.getGameTime() + 1);
        var frame = new Schedule(lamp, pos, state);
        if (!lamp.isNativeGrowthPhase(pos, state) || level.getBlockState(pos) != state) return false;
        SCHEDULING.set(frame);
        try { lamp.consumePreparedGrowthCharge(pos, state); queue.pending.putIfAbsent(key, due); queue.setDirty(); }
        finally { SCHEDULING.remove(); }
        return true;
    }
    public static void tick(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Growth updates require the server thread");
        var queue = level.getDataStorage().get(FACTORY, ID); if (queue == null) return;
        long now = level.getGameTime();
        for (Due due : new ArrayList<>(queue.pending.values())) {
            BlockPos pos = due.key().pos();
            if (due.time() > now || !level.hasChunkAt(pos)) continue;
            if (!queue.pending.remove(due.key(), due)) continue;
            queue.setDirty(); // Native update is at-most-once, including a callback which saves or removes its original lamp.
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) continue;
            var current = level.getBlockState(pos);
            if (current.getBlock() == due.key().block()) current.randomTick(level, pos, level.random);
        }
    }
    public static GrowthLampTicks load(CompoundTag tag, HolderLookup.Provider registries) {
        var queue = new GrowthLampTicks();
        for (Tag raw : tag.getList("Rejected", Tag.TAG_COMPOUND)) queue.rejected.add(raw.copy());
        for (Tag raw : tag.getList("Pending", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) raw;
            ResourceLocation id = row.contains("Block", Tag.TAG_STRING) ? ResourceLocation.tryParse(row.getString("Block")) : null;
            Block block = id != null && BuiltInRegistries.BLOCK.containsKey(id) ? BuiltInRegistries.BLOCK.get(id) : Blocks.AIR;
            if (!row.contains("Pos", Tag.TAG_LONG) || !row.contains("Due", Tag.TAG_LONG) || block == Blocks.AIR) { queue.rejected.add(row.copy()); continue; }
            var key = new Key(BlockPos.of(row.getLong("Pos")), block);
            if (queue.pending.putIfAbsent(key, new Due(key, row.getLong("Due"))) != null) queue.rejected.add(row.copy());
        }
        return queue;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var rows = new ListTag();
        for (Due due : pending.values()) {
            var row = new CompoundTag(); row.putLong("Pos", due.key().pos().asLong());
            row.putString("Block", BuiltInRegistries.BLOCK.getKey(due.key().block()).toString()); row.putLong("Due", due.time()); rows.add(row);
        }
        tag.put("Pending", rows); if (!rejected.isEmpty()) tag.put("Rejected", rejected.copy());
        return tag;
    }
}
