package thaumcraft.common.lib.events;

import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
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
import thaumcraft.common.lib.world.StructurePlacementRules;
import thaumcraft.common.lib.world.ThaumcraftNormalNodeFeature;
import thaumcraft.common.lib.world.ThaumcraftOreFeature;
import thaumcraft.common.lib.world.ThaumcraftTreeFeature;
import thaumcraft.common.lib.world.WorldGenMound;
import thaumcraft.common.lib.world.WorldGenHilltopStones;
import thaumcraft.common.lib.world.EldritchRingFeature;
import thaumcraft.common.lib.world.NodeEcologyPolicy;
import net.minecraft.world.level.levelgen.FlatLevelSource;

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
 * and {@code .read}), so the marker of the original rides on the chunk NBT of the port through
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
        if (hasMarker(chunkTag, regenKey())) return;
        Thaumcraft.LOG.warn("[Thaumcraft] World gen was never run for chunk at {}. Adding to queue for regeneration.", pos);
        queue(level).add(new ChunkLoc(pos.x, pos.z));
    }

    /** The key of the marker of the original (oracle Config:378-380). */
    public static String regenKey() {
        return ThaumcraftBiomeSettings.regen().regenKey();
    }

    /** Oracle EventHandlerWorld:125 tests key presence, not the stored boolean value. */
    public static boolean hasMarker(CompoundTag chunkTag, String key) {
        return chunkTag.getCompound(COMPOUND).contains(key);
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
     * {@code newGen = false}, i.e. {@code generateSurface:420-456}.
     *
     * <p>Every pass of the original is behind its own regeneration option, and every option is
     * off by default, so the regeneration of the original does nothing until a player switches
     * one of them on. The port keeps that gate, and it keeps the second gate of every pass: the
     * generation option of the same pass ({@code Config.genX && (newGen || Config.regenX)}).</p>
     *
     * <p>The original replays its own generator into an existing chunk with
     * {@code World#setBlock}. The port's features are plain {@code Feature} bodies, so the same
     * replay is possible here: each body is invoked directly on the live level, with the random
     * of the original regeneration ({@link #chunkSeed}). This is not the datapack feature
     * pipeline and does not regenerate terrain.</p>
     *
     * <p>The aura totem is deliberately absent: {@code generateTotem:149} requires
     * {@code newGen}, and the regeneration queue calls {@code worldGeneration(..., false)}
     * (oracle ServerTickEventsFML:96), so the original never grows a totem during a
     * regeneration either.</p>
     */
    public static void regenerate(ServerLevel level, int chunkX, int chunkZ, long seed) {
        OriginalRegenSettings settings = ThaumcraftBiomeSettings.regen();
        if (!settings.anyRegeneration()) return;
        RandomSource random = RandomSource.create(seed);
        var worldgen = ThaumcraftBiomeSettings.worldgen();
        net.minecraft.world.level.chunk.ChunkAccess chunk = null;
        // A world generation failure must not take the server down: the original had no such
        // guard, but a throwing feature in a regeneration tick would stop the whole server.
        try {
            // Oracle ThaumcraftWorldGenerator:143 - the original loads the chunk of the
            // regeneration before it writes into it.
            chunk = level.getChunk(chunkX, chunkZ);
            // Oracle ThaumcraftWorldGenerator:421 - the vegetation pass.
            if (ThaumcraftBiomeSettings.current().generateTrees() && settings.trees()) {
                ThaumcraftTreeFeature.placePass(level, random, chunkX, chunkZ);
            }
            // Oracle ThaumcraftWorldGenerator:422-423 and :481-546 - the ore pass. The pass
            // itself runs unconditionally and gates each type with genX && regenX.
            ThaumcraftOreFeature.placePass(level, random, chunkX, chunkZ,
                    worldgen.cinnabarOre() && settings.cinnibarOre(),
                    worldgen.amberOre() && settings.amberOre(),
                    worldgen.infusedStone() && settings.infusedStone());
            // Oracle ThaumcraftWorldGenerator:414-420 - the scattered feature node and the wild
            // node pass, with the overwriting assignment of line 419.
            if (worldgen.auraNodes() && settings.auraNodes()) {
                ThaumcraftNormalNodeFeature.placeAuraPass(level, random, chunkX, chunkZ);
            }
            // Oracle ThaumcraftWorldGenerator:429-453 - the exclusive structure chain. The
            // totem that follows it in the fresh generation is not replayed (see the class note).
            if (worldgen.structures() && settings.structures()
                    && level.dimension().equals(Level.OVERWORLD)
                    && NodeEcologyPolicy.dimensionLevel(level.dimension()) == -1
                    && !(level.getChunkSource().getGenerator() instanceof FlatLevelSource)) {
                placeStructure(level, random, chunkX, chunkZ);
            }
        } catch (RuntimeException failure) {
            Thaumcraft.LOG.error("[Thaumcraft] Chunk regeneration failed for chunk at {}; the chunk keeps its terrain",
                    new ChunkPos(chunkX, chunkZ), failure);
        }
        // Oracle Chunk.func_76630_e - the original marks the chunk as modified after the
        // regeneration, so the chunk is written again and carries the marker.
        if (chunk != null) chunk.setUnsaved(true);
    }

    /**
     * The structure chain of the regeneration: oracle ThaumcraftWorldGenerator:429-453.
     *
     * <p>The chain draws the position and its three rolls from the chunk random, and only the
     * selected structure is built. Unlike the independent fresh-generation features, replay
     * continues the stream already consumed by vegetation, ores and aura; it must not reseed
     * with the fresh-generation chunk seed (oracle ServerTickEventsFML:96).</p>
     */
    private static boolean placeStructure(ServerLevel level, RandomSource random, int chunkX, int chunkZ) {
        StructurePlacementRules.Draw draw = StructurePlacementRules.draw(random, chunkX, chunkZ);
        return switch (draw.kind()) {
            case MOUND -> new WorldGenMound().placeSelected(level, random, draw);
            case ELDRITCH_RING -> new EldritchRingFeature().placeSelected(level, random, draw, chunkX, chunkZ);
            case HILLTOP_STONES -> new WorldGenHilltopStones().placeSelected(level, random, draw, chunkX, chunkZ);
            case NONE -> false;
        };
    }
}
