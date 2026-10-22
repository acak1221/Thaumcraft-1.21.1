package thaumcraft.common.lib.events;

import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.config.OriginalRegenSettings;
import thaumcraft.common.config.ThaumcraftBiomeSettings;
import thaumcraft.common.lib.world.ChunkLoc;
import thaumcraft.common.lib.world.ThaumcraftNormalNodeFeature;

/**
 * The chunk save and chunk load marker of the original and the chunk regeneration queue.
 *
 * <p>The original of 1.7.10 wrote a compound into the NBT of every chunk it saved and read it
 * back when the chunk was loaded:</p>
 * <pre>
 * // oracle thaumcraft/common/lib/events/EventHandlerWorld.java:114-119
 * public void chunkSave(ChunkDataEvent.Save event) {
 *     NBTTagCompound var4 = new NBTTagCompound();
 *     event.getData().func_74782_a("Thaumcraft", (NBTBase)var4);
 *     var4.func_74757_a(Config.regenKey, true);
 * }
 *
 * // oracle thaumcraft/common/lib/events/EventHandlerWorld.java:121-137
 * public void chunkLoad(ChunkDataEvent.Load event) {
 *     ...
 *     if (!event.getData().func_74775_l("Thaumcraft").func_74764_b(Config.regenKey)
 *             &amp;&amp; (Config.regenAmber || Config.regenAura || Config.regenCinnibar
 *                     || Config.regenInfusedStone || Config.regenStructure || Config.regenTrees)) {
 *         ... "World gen was never run for chunk at " + loc + ". Adding to queue for regeneration."
 *         ServerTickEventsFML.chunksToGenerate.get(dim).add(new ChunkLoc(loc.chunkXPos, loc.chunkZPos));
 *     }
 * }
 * </pre>
 *
 * <p>The purpose of the marker is the regeneration category of the original configuration: the
 * key {@code Config.regenKey} ("DEFAULT" by default, oracle Config:378-380) records that the
 * chunk was written by a world that runs the TC4 world generation, and the six regeneration
 * options (all off by default, oracle Config:381-386) ask for the chunks that never received
 * it to be generated again.</p>
 *
 * <p>Minecraft 1.21.1 has no chunk data event, but the vanilla chunk NBT is written and read in
 * exactly one place each ({@code net.minecraft.world.level.chunk.storage.ChunkSerializer.write}
 * and {@@code .read}), so the marker of the original rides on the chunk NBT of the port through
 * the mixin {@code Tc4ChunkRegenerationMarkerMixin}.</p>
 */
public final class ChunkRegeneration {
    /** Oracle EventHandlerWorld:117 - the compound of the chunk NBT that carries the marker. */
    public static final String COMPOUND = "Thaumcraft";

    /** Oracle ServerTickEventsFML:88 - the number of the chunks that one world tick regenerates. */
    public static final int CHUNKS_PER_TICK = 10;

    /** The chunks of every dimension that wait for their regeneration. */
    private static final Map<ResourceKey<Level>, Queue<ChunkLoc>> QUEUE = new ConcurrentHashMap<>();

    private ChunkRegeneration() {}

    /**
     * The write side of the marker: oracle EventHandlerWorld:114-119.
     *
     * <p>The original writes the marker on <em>every</em> chunk save, not only on the chunks the
     * TC4 world generation touched, so the port does the same.</p>
     */
    public static void writeMarker(CompoundTag chunkTag) {
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(regenKey(), true);
        chunkTag.put(COMPOUND, marker);
    }

    /**
     * The read side of the marker: oracle EventHandlerWorld:121-137.
     *
     * <p>The vanilla reads the chunk on the worker thread of the storage, so the method only
     * records the chunk; the regeneration itself happens on the server thread.</p>
     */
    public static void readMarker(ServerLevel level, ChunkPos pos, CompoundTag chunkTag) {
        OriginalRegenSettings settings = ThaumcraftBiomeSettings.regen();
        if (!settings.anyRegeneration()) return;
        CompoundTag marker = chunkTag.getCompound(COMPOUND);
        if (marker.getBoolean(regenKey())) return;
        Thaumcraft.LOG.warn("[Thaumcraft] World gen was never run for chunk at {}. Adding to queue for regeneration.", pos);
        queue(level).add(new ChunkLoc(pos.x, pos.z));
    }

    /** The key of the marker of the original (oracle Config:378-380). */
    public static String regenKey() {
        return ThaumcraftBiomeSettings.regen().regenKey();
    }

    /** The chunks of the dimension that wait for their regeneration. */
    private static Queue<ChunkLoc> queue(ServerLevel level) {
        return QUEUE.computeIfAbsent(level.dimension(), key -> new ConcurrentLinkedQueue<>());
    }

    /** The number of the chunks of the dimension that wait for their regeneration. */
    public static int queued(ServerLevel level) {
        Queue<ChunkLoc> chunks = QUEUE.get(level.dimension());
        return chunks == null ? 0 : chunks.size();
    }

    /**
     * The drain of the queue: oracle ServerTickEventsFML:83-104.
     *
     * <p>The original regenerates ten chunks of the dimension per world tick, with the random of
     * the original and the log line of the original.</p>
     */
    public static void tick(ServerLevel level) {
        Queue<ChunkLoc> chunks = QUEUE.get(level.dimension());
        if (chunks == null || chunks.isEmpty()) return;
        int count = 0;
        while (count < CHUNKS_PER_TICK) {
            ChunkLoc loc = chunks.poll();
            if (loc == null) break;
            ++count;
            regenerate(level, loc.chunkXPos, loc.chunkZPos, chunkSeed(level.getSeed(), loc.chunkXPos, loc.chunkZPos));
        }
        Thaumcraft.LOG.info("[Thaumcraft] Regenerated {} chunks. {} chunks left", count, Math.max(0, chunks.size()));
    }

    /**
     * The random of the regeneration of a chunk: oracle ServerTickEventsFML:91-95.
     *
     * <p>The original draws two seeds from the world seed, shifts them right by three bits and
     * mixes them with the chunk and the world seed.</p>
     */
    public static long chunkSeed(long worldSeed, int chunkX, int chunkZ) {
        Random random = new Random(worldSeed);
        long xSeed = random.nextLong() >> 3;
        long zSeed = random.nextLong() >> 3;
        return xSeed * chunkX + zSeed * chunkZ ^ worldSeed;
    }

    /**
     * The regeneration of a chunk: oracle ThaumcraftWorldGenerator.worldGeneration:125-146 with
     * {@code newGen = false}.
     *
     * <p>Every pass of the original is behind its own regeneration option, and every option is
     * off by default, so the regeneration of the original does nothing until a player switches
     * one of them on. The port keeps that gate.</p>
     */
    public static void regenerate(ServerLevel level, int chunkX, int chunkZ, long seed) {
        OriginalRegenSettings settings = ThaumcraftBiomeSettings.regen();
        if (!settings.anyRegeneration()) return;
        // Oracle ThaumcraftWorldGenerator:143 - the original loads the chunk of the
        // regeneration before it writes into it.
        var chunk = level.getChunk(chunkX, chunkZ);
        RandomSource random = RandomSource.create(seed);
        // Oracle ThaumcraftWorldGenerator:412-419 - the aura node pass.
        if (settings.auraNodes()) ThaumcraftNormalNodeFeature.placeWildNode(level, random, chunkX, chunkZ);
        // The ore pass (:495-509), the vegetation pass (:459-479) and the structure chain
        // (:421-456) of the original run through the feature pipeline of the datapack of
        // 1.21.1, and the vanilla only runs that pipeline while it generates a chunk
        // (net.minecraft.world.level.levelgen.ChunkGenerator.applyBiomeDecoration). They
        // cannot be replayed for a chunk that exists, so they are not replayed here.
        // Oracle Chunk.func_76630_e - the original marks the chunk as modified after the
        // regeneration, so the chunk is written again and carries the marker.
        chunk.setUnsaved(true);
    }
}
