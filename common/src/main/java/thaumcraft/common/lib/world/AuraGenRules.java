package thaumcraft.common.lib.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * The scattered-feature branch and the {@code auraGen} exclusivity of the original surface
 * generator.
 *
 * <p>Oracle {@code thaumcraft/common/lib/world/ThaumcraftWorldGenerator.generateSurface:411-456}:</p>
 * <pre>
 * boolean auraGen = false;
 * if (... Config.genAura &amp;&amp; (newGen || Config.regenAura)) {
 *     ChunkPosition var7 = new MapGenScatteredFeature().func_151545_a(world,
 *             chunkX * 16 + 8, world.func_72976_f(chunkX * 16 + 8, chunkZ * 16 + 8), chunkZ * 16 + 8);
 *     if (var7 != null &amp;&amp; !this.structureNode.containsKey(var7.hashCode())) {
 *         auraGen = true;
 *         this.structureNode.put(var7.hashCode(), true);
 *         createRandomNodeAt(world, var7.x, world.func_72976_f(var7.x, var7.z) + 3, var7.z, random, false, false, false);
 *     }
 *     auraGen = this.generateWildNodes(world, random, chunkX, chunkZ, auraGen, newGen);
 * }
 * if (... Config.genStructure &amp;&amp; dim == 0 &amp;&amp; ...) {
 *     ... this.generateTotem(world, random, chunkX, chunkZ, auraGen, newGen);
 * }
 * </pre>
 *
 * <p>Two quirks of that block are load-bearing and are reproduced here:</p>
 * <ol>
 *   <li>the assignment of line 419 <em>overwrites</em> the flag the scattered feature set, and
 *       {@code generateWildNodes} refuses the chunk while that flag is set (line 190,
 *       {@code !auraGen}). A placed scattered node therefore disables the wild node pass and
 *       still allows the totem.</li>
 *   <li>the totem is suppressed only by a structure the chain actually <em>built</em> (line 434:
 *       {@code if (mound.func_76484_a(...))}), not by a chain roll that selected a structure and
 *       then failed its ground test.</li>
 * </ol>
 */
public final class AuraGenRules {
    private AuraGenRules() {}

    /**
     * Oracle ThaumcraftWorldGenerator:413 - the biome list of the 1.7.10 {@code
     * MapGenScatteredFeature}: Desert, DesertHills, Jungle, JungleHills and Swampland.
     *
     * <p>1.21.1 splits the same five biome slots over three structures; the desert hills and the
     * jungle hills are biome tags of the pyramid, and the swamp hut is the witch hut of the
     * original. These are the only structures of the original that {@code func_151545_a} could
     * return, so these are the only structures that may carry a scattered node.</p>
     */
    public static final List<ResourceKey<Structure>> SCATTERED_TEMPLES = List.of(
            key("desert_pyramid"),
            key("jungle_pyramid"),
            key("swamp_hut"));

    /** Oracle MapGenScatteredFeature:18-19 - the spacing of the scattered features. */
    public static final int SCATTERED_SPACING = 32;
    /** Oracle MapGenScatteredFeature:19 - the separation of the scattered features. */
    public static final int SCATTERED_SEPARATION = 8;
    /** Oracle MapGenScatteredFeature:41 - the salt of the scattered features. */
    public static final int SCATTERED_SALT = 14357617;

    /** The centre of a scattered feature, taken from the start that owns the chunk. */
    public record Temple(BlockPos centre) {}

    private static ResourceKey<Structure> key(String path) {
        return ResourceKey.create(Registries.STRUCTURE, ResourceLocation.withDefaultNamespace(path));
    }

    /**
     * Oracle ThaumcraftWorldGenerator:413-418 - the scattered feature of the chunk.
     *
     * <p>The original asks {@code MapGenScatteredFeature.func_151545_a} for the structure that
     * covers the centre of the chunk, then drops it through the {@code structureNode} map so one
     * temple pays one node. In 1.21.1 the structure starts are known to the chunk that owns them,
     * so the equivalent is the start of that chunk: it is returned exactly once, for the chunk
     * that carries it, which gives the same one-node-per-temple result without the cross-chunk
     * reach of the 1.7.10 prepared-structure map.</p>
     *
     * <p>The centre is the centre of the start bounding box, the value the original reads from
     * the first component ({@code StructureComponent.func_151553_a}).</p>
     *
     * @return the centre of the scattered feature of this chunk, or {@code null}
     */
    public static Temple scatteredTemple(WorldGenLevel level, int chunkX, int chunkZ) {
        ChunkAccess chunk;
        try {
            chunk = level.getChunk(chunkX, chunkZ);
        } catch (RuntimeException unavailable) {
            // A chunk outside the loaded area of a fresh generation region is not this chunk.
            return null;
        }
        if (chunk == null) return null;
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        for (ResourceKey<Structure> key : SCATTERED_TEMPLES) {
            // Registry#get is nullable: a datapack may remove a vanilla structure, and the
            // original tolerated a structure table that no longer lists one.
            Structure structure = structures.get(key);
            if (structure == null) continue;
            StructureStart start = chunk.getStartForStructure(structure);
            if (start == null || !start.isValid() || !start.getChunkPos().equals(pos)) continue;
            return new Temple(start.getBoundingBox().getCenter());
        }
        return null;
    }

    /**
     * Oracle ThaumcraftWorldGenerator:419 and :190 - the final value of {@code auraGen} after the
     * scattered branch.
     *
     * <p>{@code generateWildNodes} bails out with {@code false} whenever the flag it is handed is
     * already set, so the assignment overwrites a scattered node back to {@code false}. The totem
     * of the chunk is therefore <em>not</em> suppressed by a scattered temple.</p>
     *
     * @param scatteredPlaced the flag the scattered branch left behind
     * @param wildNodePass    the value returned by the wild node pass
     * @return the value the original assigns at line 419
     */
    public static boolean auraGenAfterWildPass(boolean scatteredPlaced, boolean wildNodePass) {
        return !scatteredPlaced && wildNodePass;
    }

    /**
     * The {@code auraGen} the totem of the chunk finally sees: oracle
     * ThaumcraftWorldGenerator:419 (the overwrite) and :434, :441, :451 (the structure that was
     * actually built).
     *
     * <p>Expanded: {@code structureBuilt || (!scatteredPlaced &amp;&amp; wildNodePass)}. A
     * scattered temple alone never suppresses the totem; a placed wild node never coexists with
     * a scattered temple, because the wild pass refuses the chunk in that case.</p>
     */
    public static boolean finalAuraGen(boolean scatteredPlaced, boolean wildNodePass, boolean structureBuilt) {
        return structureBuilt || auraGenAfterWildPass(scatteredPlaced, wildNodePass);
    }

    /**
     * Oracle ThaumcraftWorldGenerator:429-453 - the structure the exclusive chain of a chunk
     * selects, drawn from the chunk random of the original.
     */
    public static StructurePlacementRules.Kind chainKind(long worldSeed, int chunkX, int chunkZ) {
        RandomSource random = RandomSource.create(StructurePlacementRules.chunkSeed(worldSeed, chunkX, chunkZ));
        return StructurePlacementRules.draw(random, chunkX, chunkZ).kind();
    }

    /**
     * Oracle ThaumcraftWorldGenerator:429-453 - whether the chain of this chunk <em>builds</em>
     * its structure.
     *
     * <p>The original sets {@code auraGen} only inside the branch that the generator accepted
     * ({@code if (mound.func_76484_a(...))}), so a selected structure that fails its ground test
     * does not suppress the totem. Each native structure feature exposes the ground test as a
     * side-effect-free {@code preflight}.</p>
     */
    public static boolean structureBuilt(WorldGenLevel level, int chunkX, int chunkZ) {
        return structureBuilt(level, chunkX, chunkZ, chainKind(level.getLevel().getSeed(), chunkX, chunkZ));
    }

    /** The same test for a chain that the caller already drew from its own stream. */
    public static boolean structureBuilt(WorldGenLevel level, int chunkX, int chunkZ,
                                         StructurePlacementRules.Kind kind) {
        return switch (kind) {
            case MOUND -> WorldGenMound.preflight(level, chunkX, chunkZ);
            case ELDRITCH_RING -> EldritchRingFeature.preflight(level, chunkX, chunkZ);
            case HILLTOP_STONES -> WorldGenHilltopStones.preflight(level, chunkX, chunkZ);
            case NONE -> false;
        };
    }
}
