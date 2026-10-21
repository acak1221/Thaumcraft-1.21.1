package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.world.VillageBankerManager;
import thaumcraft.common.lib.world.VillageWizardManager;
import thaumcraft.common.lib.world.VillageWorldGenRules;
import thaumcraft.common.lib.world.WorldGenHilltopStones;
import thaumcraft.common.lib.world.WorldGenMound;

/**
 * Registration of the TC4 village components and of the two remaining surface
 * worldgen structures.
 *
 * <p>1.7.10 generated the banker home and the wizard tower as extra pieces of
 * a vanilla village through cpw.mods.fml.common.registry.VillagerRegistry.
 * 1.21.1 has no village provider hook and no villager registry, so both
 * components are deterministic per chunk features instead; the original piece
 * weights, house counts and block compositions are kept in
 * {@link VillageWorldGenRules}.
 *
 * <p>The class also resolves the block state of a component material. That is
 * world glue, so it lives here next to the registration instead of in the
 * pure rules class.
 */
public final class ThaumcraftVillages {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Thaumcraft.MODID, Registries.FEATURE);

    public static final RegistrySupplier<VillageBankerManager> BANKER_HOMES =
            FEATURES.register("village_banker_homes", VillageBankerManager::new);
    public static final RegistrySupplier<VillageWizardManager> WIZARD_TOWERS =
            FEATURES.register("village_wizard_towers", VillageWizardManager::new);
    public static final RegistrySupplier<WorldGenHilltopStones> HILLTOP_STONES =
            FEATURES.register("hilltop_stones", WorldGenHilltopStones::new);
    /**
     * The TC4 mound, the first structure of the exclusive chain of the overworld
     * (oracle WorldGenMound, one chunk in 150).
     */
    public static final RegistrySupplier<WorldGenMound> MOUND =
            FEATURES.register("tc4_mound", WorldGenMound::new);
    /**
     * The eldritch ring is the single port of the original WorldGenEldritchRing and it belongs
     * to the Outer Lands, because the ring seeds the labyrinth of the Outer Lands. It is
     * registered by {@link ThaumcraftOuterLands} as {@code thaumcraft:tc4_eldritch_ring}.
     */

    public static final ResourceKey<PlacedFeature> BANKER_HOMES_KEY = placed("village_banker_homes");
    public static final ResourceKey<PlacedFeature> WIZARD_TOWERS_KEY = placed("village_wizard_towers");
    public static final ResourceKey<PlacedFeature> HILLTOP_STONES_KEY = placed("hilltop_stones");
    public static final ResourceKey<PlacedFeature> MOUND_KEY = placed("tc4_mound");

    /**
     * The biomes that may carry the TC4 surface structures.
     *
     * <p>Oracle ThaumcraftWorldGenerator.generateSurface:421-456 draws its structures in
     * every chunk of the overworld, so the tag holds the vanilla overworld biomes and
     * the three TC4 biomes of the overworld.
     */
    public static final TagKey<Biome> SURFACE_STRUCTURE_BIOMES = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "surface_structure_biomes"));

    /**
     * The terrain the village ground levelling may replace. The blocks of
     * another structure are not in it, so a piece never overwrites them.
     */
    public static final Set<String> VILLAGE_GROUND_BLOCKS = Set.of(
            "minecraft:dirt", "minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
            "minecraft:podzol", "minecraft:mycelium", "minecraft:mud", "minecraft:sand", "minecraft:sandstone",
            "minecraft:gravel", "minecraft:clay", "minecraft:stone", "minecraft:andesite", "minecraft:diorite",
            "minecraft:granite", "minecraft:deepslate", "minecraft:tuff", "minecraft:snow", "minecraft:snow_block");

    /** The eldritch stone family of the Outer Lands unit. */
    private static final ResourceLocation ELDRITCH = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch");
    /** The cosmetic solid family of the Outer Lands unit; the metadata zero and one are the tough group. */
    private static final ResourceLocation COSMETIC_SOLID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "cosmetic_solid_tough");

    private ThaumcraftVillages() {}

    private static ResourceKey<PlacedFeature> placed(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path));
    }

    public static void register() {
        FEATURES.register();
    }

    // ------------------------------------------------------------------ component glue

    /**
     * The average ground level of a component footprint, the native form of
     * the 1.7.10 StructureComponent.getAverageGroundLevel. The 1.21.1 height
     * map has no top solid block query, so the surface height of every
     * footprint column is used.
     */
    public static int averageGroundLevel(WorldGenLevel level, int originX, int originZ, int width, int depth) {
        int sum = 0;
        int count = 0;
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                int worldX = originX + x;
                int worldZ = originZ + z;
                if (!level.hasChunk(worldX >> 4, worldZ >> 4)) continue;
                sum += level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ) - 1;
                count++;
            }
        }
        return count == 0 ? level.getMinBuildHeight() : sum / count;
    }

    /**
     * The space above a component is cleared and a cobble footing is laid
     * under it, oracle ComponentBankerHome:106-111 and
     * ComponentWizardTower:100-105.
     */
    public static void clearAboveAndFillBelow(WorldGenLevel level, BlockPos origin, int rotation,
                                              int width, int depth, int height) {
        int rotatedWidth = VillageWorldGenRules.rotatedWidth(width, depth, rotation);
        int rotatedDepth = VillageWorldGenRules.rotatedDepth(width, depth, rotation);
        for (int x = 0; x < rotatedWidth; x++) {
            for (int z = 0; z < rotatedDepth; z++) {
                int worldX = origin.getX() + x;
                int worldZ = origin.getZ() + z;
                if (!level.hasChunk(worldX >> 4, worldZ >> 4)) continue;
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ);
                for (int y = origin.getY() + height; y < surface; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    if (level.getBlockState(pos).isAir()) continue;
                    if (level.isOutsideBuildHeight(pos) || !thaumcraft.common.lib.world.WorldGenWriteArea.canWrite(level, pos)) break;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
                }
                for (int y = origin.getY() - 1; y >= level.getMinBuildHeight(); y--) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.getFluidState().isEmpty()) break;
                    if (level.isOutsideBuildHeight(pos) || !thaumcraft.common.lib.world.WorldGenWriteArea.canWrite(level, pos)) break;
                    level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
                }
            }
        }
    }

    /**
     * The ground of a component footprint is levelled to the level of the
     * component, the way the 1.7.10 village generator prepared the ground of a
     * piece before it built it.
     *
     * <p>Without it a component on a slope is refused by its own space check,
     * because the terrain of the higher side of the footprint stands in the room
     * of the piece. Only the terrain is levelled; the blocks of another
     * structure stay and keep the piece from overwriting them.
     */
    public static void levelGround(WorldGenLevel level, BlockPos origin, int rotation,
                                   int width, int depth) {
        int rotatedWidth = VillageWorldGenRules.rotatedWidth(width, depth, rotation);
        int rotatedDepth = VillageWorldGenRules.rotatedDepth(width, depth, rotation);
        for (int x = 0; x < rotatedWidth; x++) {
            for (int z = 0; z < rotatedDepth; z++) {
                int worldX = origin.getX() + x;
                int worldZ = origin.getZ() + z;
                if (!level.hasChunk(worldX >> 4, worldZ >> 4)) continue;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                if (surface - 1 < level.getMinBuildHeight()) continue;
                BlockState ground = level.getBlockState(new BlockPos(worldX, surface - 1, worldZ));
                if (!VILLAGE_GROUND_BLOCKS.contains(BuiltInRegistries.BLOCK.getKey(ground.getBlock()).toString())) continue;
                BlockPos floor = new BlockPos(worldX, origin.getY(), worldZ);
                if (level.isOutsideBuildHeight(floor) || !thaumcraft.common.lib.world.WorldGenWriteArea.canWrite(level, floor)) continue;
                level.setBlock(floor, ground, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
                for (int y = origin.getY() + 1; y < surface; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    if (level.isOutsideBuildHeight(pos) || !thaumcraft.common.lib.world.WorldGenWriteArea.canWrite(level, pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (!VILLAGE_GROUND_BLOCKS.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())) continue;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
                }
            }
        }
    }

    /**
     * Whether the space a component needs is free, the native form of the
     * 1.7.10 {@code StructureComponent.findIntersecting} check of the village
     * pieces. The original refused a piece that overlapped another piece of
     * the same village; the native check refuses a component that would
     * overwrite blocks that are already there.
     */
    public static boolean spaceIsFree(WorldGenLevel level, BlockPos origin, int rotation,
                                       int width, int depth, int height) {
        int rotatedWidth = VillageWorldGenRules.rotatedWidth(width, depth, rotation);
        int rotatedDepth = VillageWorldGenRules.rotatedDepth(width, depth, rotation);
        for (int x = 0; x < rotatedWidth; x++) {
            for (int z = 0; z < rotatedDepth; z++) {
                for (int y = 1; y < height; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (level.isOutsideBuildHeight(pos)) return false;
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !state.canBeReplaced()) return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ material glue

    /**
     * The block state of a component material.
     *
     * <p>The original metadata of the eldritch and cosmetic solid families
     * became a {@code variant} block state property. While the Outer Lands
     * unit has not registered those families yet the native arcane stone
     * blocks stand in for them, so the structures never write a missing block.
     */
    public static BlockState state(VillageWorldGenRules.Material material, int rotation) {
        BlockState state = block(material).defaultBlockState();
        int variant = material.variant();
        if (variant >= 0) {
            IntegerProperty property = variantProperty(state);
            if (property != null && property.getPossibleValues().contains(variant)) {
                state = state.setValue(property, variant);
            }
        }
        return switch (material) {
            case DOOR_BOTTOM -> state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                    .setValue(DoorBlock.FACING, direction(VillageWorldGenRules.frontDirection(rotation)));
            case DOOR_TOP -> state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                    .setValue(DoorBlock.FACING, direction(VillageWorldGenRules.frontDirection(rotation)));
            case LADDER -> state.setValue(LadderBlock.FACING,
                    direction(VillageWorldGenRules.rotateDirection("west", rotation)));
            case TRAPDOOR -> state.setValue(TrapDoorBlock.HALF, Half.BOTTOM)
                    .setValue(TrapDoorBlock.FACING, direction(VillageWorldGenRules.rotateDirection("west", rotation)));
            case STONE_STAIRS -> state.setValue(StairBlock.FACING,
                    direction(VillageWorldGenRules.frontDirection(rotation)));
            default -> state;
        };
    }

    /** The block of a material, with the native stand ins for the missing families. */
    public static Block block(VillageWorldGenRules.Material material) {
        switch (material) {
            case COSMETIC_SOLID_0: {
                Block block = registryBlock(COSMETIC_SOLID);
                return block != null ? block : ThaumcraftMasonry.ARCANE_STONE.get();
            }
            case COSMETIC_SOLID_1: {
                Block block = registryBlock(COSMETIC_SOLID);
                return block != null ? block : ThaumcraftMasonry.ARCANE_STONE_BRICKS.get();
            }
            case ELDRITCH_ALTAR:
            case ELDRITCH_OBELISK:
            case ELDRITCH_OBELISK_MIDDLE:
            case ELDRITCH_CAPSTONE: {
                Block block = registryBlock(ELDRITCH);
                return block != null ? block : Blocks.OBSIDIAN;
            }
            default: {
                Block block = registryBlock(ResourceLocation.parse(material.id()));
                return block != null ? block : Blocks.STONE;
            }
        }
    }

    private static Block registryBlock(ResourceLocation id) {
        return BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }

    private static IntegerProperty variantProperty(BlockState state) {
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && "variant".equals(property.getName())) return integer;
        }
        return null;
    }

    private static Direction direction(String name) {
        return switch (name) {
            case "east" -> Direction.EAST;
            case "south" -> Direction.SOUTH;
            case "west" -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }
}
