package thaumcraft.common.lib.world.biomes;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import thaumcraft.common.config.ThaumcraftBiomes;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules.Generator;
import thaumcraft.common.lib.world.biomes.BiomeSurfaceRules.TreeKind;

/**
 * Native 1.21.1 replacement of the original TC4 1.7.10 {@code BiomeGenMagicalForest}.
 *
 * <p>Original source: {@code thaumcraft.common.lib.world.biomes.BiomeGenMagicalForest}
 * lines 55-167 of TC4 4.2.3.5. The original class was a {@code BiomeGenBase} subclass and
 * therefore both the biome definition and its decorator. 1.21.1 registers a biome as a
 * datapack document, so the definition itself lives in
 * {@code data/thaumcraft/worldgen/biome/magical_forest.json} and this class keeps the
 * original generator identity and the original generator specific draws that the shared
 * native decorator {@code ThaumcraftBiomeDecorationFeature} reproduces.
 *
 * <p>The original magical forest is the only TC4 biome with its own trees and its own
 * flowers: one silverwood tree in fourteen, one greatwood tree in ten, otherwise the big
 * magic tree; nine red flower types with weight ten each; double grass one time in four;
 * up to two mossy cobblestone blobs; one big mushroom in forty on a four by four grid;
 * ten mana pods from y64 and eight vishrooms next to wood logs.
 */
public final class BiomeGenMagicalForest {
    /** The original 1.7.10 biome name, {@code BiomeGenBase.func_76735_a}. */
    public static final String NAME="Magical Forest";
    /** The native 1.21.1 biome key of the magical forest. */
    public static final ResourceKey<Biome> KEY=ThaumcraftBiomes.MAGICAL_FOREST;
    /** The original generator that owns this biome. */
    public static final Generator GENERATOR=Generator.MAGICAL_FOREST;

    private BiomeGenMagicalForest() {}

    /**
     * The original tree draw, {@code BiomeGenMagicalForest.func_150567_a}: one silverwood
     * tree in fourteen, then one greatwood tree in ten, otherwise the big magic tree.
     * The greatwood draw is only taken when the silverwood draw failed.
     */
    public static TreeKind treeKind(RandomSource random) {
        int silverwood=random.nextInt(14);
        return BiomeSurfaceRules.treeKind(GENERATOR,silverwood,silverwood==0?0:random.nextInt(10));
    }

    /** The original grass draw, {@code BiomeGenMagicalForest.func_76730_b}: double grass one time in four. */
    public static boolean doubleGrass(RandomSource random) {return BiomeSurfaceRules.doubleGrass(random.nextInt(4));}

    /** The original nine red flower types with their original weight. */
    public static List<String> flowers() {return BiomeSurfaceRules.MAGICAL_FOREST_FLOWERS;}

    /** The original mossy cobblestone blob count, {@code random.nextInt(3)}. */
    public static int mossyCobblestoneBlobs(RandomSource random) {return BiomeSurfaceRules.mossyBlobCount(random.nextInt(3));}

    /** The original big mushroom draw, one in forty on a four by four grid. */
    public static boolean bigMushroom(RandomSource random) {return random.nextInt(BiomeSurfaceRules.BIG_MUSHROOM_CHANCE)==0;}

    /** The original number of mana pods generated per chunk. */
    public static int manaPods() {return BiomeSurfaceRules.MANA_PODS;}

    /** The original y level the mana pods were generated from. */
    public static int manaPodY() {return BiomeSurfaceRules.MANA_POD_Y;}

    /** The original number of vishroom attempts per chunk. */
    public static int vishrooms() {return BiomeSurfaceRules.VISHROOMS;}

    /** The original lowest y level the vishroom search descended to. */
    public static int vishroomMinY() {return BiomeSurfaceRules.VISHROOM_MIN_Y;}

    /** The original vishroom had to touch a wood log. */
    public static boolean vishroomNeedsWood() {return true;}
}
