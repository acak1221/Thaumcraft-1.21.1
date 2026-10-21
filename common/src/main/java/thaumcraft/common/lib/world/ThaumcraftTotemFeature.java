package thaumcraft.common.lib.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.config.ThaumcraftVillages;

/**
 * The aura totem of the original TC4 world generator.
 *
 * <p>Oracle {@code thaumcraft/common/lib/world/ThaumcraftWorldGenerator.generateTotem:148-187}:
 * one chunk in {@code Config.nodeRarity * 10} (360 with the original default) grows a small
 * column of the cosmetic solid with a dark aura node on top of it. The column stands on the
 * ground block below the surface, the node sits on the first free block of the column or, if
 * the one in four roll of the original misses, five blocks above the ground.
 *
 * <p>1.21.1 has no world generator, so the totem became a deterministic per chunk feature.
 * The rarity, the ground rules, the height of the column and the node are the original ones.
 */
public final class ThaumcraftTotemFeature extends Feature<NoneFeatureConfiguration> {
    /** Oracle ThaumcraftWorldGenerator:149 - one chunk in Config.nodeRarity * 10. */
    public static final int TOTEM_CHANCE = StructurePlacementRules.TOTEM_CHANCE;
    /** Oracle ThaumcraftWorldGenerator:156 - the totem is only built below this height. */
    public static final int MIN_TOTEM_Y = 40;
    /** Oracle ThaumcraftWorldGenerator:160 - the column reaches four blocks above the ground. */
    public static final int COLUMN_HEIGHT = 4;
    /** Oracle ThaumcraftWorldGenerator:165 - the node sits five blocks above the ground. */
    public static final int NODE_HEIGHT = 5;
    /** Oracle ThaumcraftWorldGenerator:163 - the node roll is one in four. */
    public static final int NODE_CHANCE = 4;

    public ThaumcraftTotemFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!ThaumcraftOreFeature.isSurfaceDimension(level)) return false;
        // Oracle ThaumcraftWorldGenerator:149 - the totem only grows in the overworld.
        if (!level.getLevel().dimension().equals(Level.OVERWORLD)) return false;
        int chunkX = context.origin().getX() >> 4;
        int chunkZ = context.origin().getZ() >> 4;
        RandomSource random = RandomSource.create(StructurePlacementRules.chunkSeed(
                level.getLevel().getSeed(), chunkX, chunkZ));
        // The exclusive structure chain comes first: the original draws the totem after the
        // chain and refuses the chunk that already received a structure with an aura node.
        StructurePlacementRules.Draw draw = StructurePlacementRules.draw(random, chunkX, chunkZ);
        if (draw.kind() != StructurePlacementRules.Kind.NONE) return false;
        // Oracle ThaumcraftWorldGenerator:149 - the rarity of the totem.
        if (random.nextInt(TOTEM_CHANCE) != 0) return false;
        // Oracle ThaumcraftWorldGenerator:151-152 - the position inside the chunk.
        int x = chunkX * 16 + random.nextInt(16);
        int z = chunkZ * 16 + random.nextInt(16);
        return build(level, random, x, z);
    }

    /**
     * Oracle ThaumcraftWorldGenerator:153-186 - the totem itself.
     */
    private boolean build(WorldGenLevel level, RandomSource random, int x, int z) {
        // Oracle ThaumcraftWorldGenerator:153 - the block below the first free block.
        int topy = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        if (topy < level.getMinBuildHeight() || topy >= level.getMaxBuildHeight()) return false;
        // Oracle ThaumcraftWorldGenerator:157-160 - a totem under leaves stands on the ground.
        if (level.getBlockState(new BlockPos(x, topy, z)).is(BlockTags.LEAVES)) {
            while (topy > MIN_TOTEM_Y && !level.getBlockState(new BlockPos(x, topy, z)).is(Blocks.GRASS_BLOCK)) {
                --topy;
            }
        }
        // Oracle ThaumcraftWorldGenerator:161-163 - the snow layer and the tall grass are skipped.
        BlockState ground = level.getBlockState(new BlockPos(x, topy, z));
        if (ground.is(Blocks.SNOW) || ground.is(Blocks.SHORT_GRASS)) {
            --topy;
            ground = level.getBlockState(new BlockPos(x, topy, z));
        }
        // Oracle ThaumcraftWorldGenerator:164 - the totem only grows on grass, sand, dirt or stone.
        if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.SAND) && !ground.is(Blocks.DIRT)
                && !ground.is(Blocks.STONE)) return false;
        // Oracle ThaumcraftWorldGenerator:165-168 - the totem needs one free block above the ground.
        int count = 1;
        while (count < 3 && isFree(level, x, topy + count, z)) ++count;
        if (count < 2) return false;
        // Oracle ThaumcraftWorldGenerator:169 - the base of the totem replaces the ground block.
        if (!set(level, x, topy, z, VillageWorldGenRules.Material.COSMETIC_SOLID_1)) return false;
        count = 1;
        // Oracle ThaumcraftWorldGenerator:171-186 - the column and its node.
        while (isFree(level, x, topy + count, z) && count < COLUMN_HEIGHT + 1) {
            if (count > 1 && random.nextInt(NODE_CHANCE) == 0) {
                // Oracle ThaumcraftWorldGenerator:173-177 - the node of the totem replaces
                // the block of the column it grows on.
                NodeGeneration.createRandomNodeAt(level, new BlockPos(x, topy + count, z), random, false, true, false);
                return true;
            }
            if (!set(level, x, topy + count, z, VillageWorldGenRules.Material.COSMETIC_SOLID_0)) return false;
            ++count;
            if (count >= COLUMN_HEIGHT + 1) {
                // Oracle ThaumcraftWorldGenerator:179-182 - the node sits above the column.
                NodeGeneration.createRandomNodeAt(level, new BlockPos(x, topy + NODE_HEIGHT, z), random, false, true, false);
            }
        }
        return true;
    }

    /** Oracle ThaumcraftWorldGenerator:165 - a free block is air, snow or tall grass. */
    private static boolean isFree(WorldGenLevel level, int x, int y, int z) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return false;
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return state.isAir() || state.is(Blocks.SNOW) || state.is(Blocks.SHORT_GRASS);
    }

    private static boolean set(WorldGenLevel level, int x, int y, int z, VillageWorldGenRules.Material material) {
        BlockState state = ThaumcraftVillages.state(material, 0);
        if (state == null) return false;
        BlockPos pos = new BlockPos(x, y, z);
        if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) return false;
        return level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
    }
}
