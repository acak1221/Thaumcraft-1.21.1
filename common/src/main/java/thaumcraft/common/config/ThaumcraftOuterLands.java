package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.Feature;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.world.DungeonRooms;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.OuterLandsProvider;
import thaumcraft.common.lib.world.OuterLandsWiringRules;

/**
 * Registration of the TC4 Outer Lands and of the blocks it needs from the eldritch content.
 *
 * The eldritch blocks, tiles and entities of the Outer Lands are ported by the eldritch content
 * groups in parallel. They are therefore referenced lazily through the registry ids below
 * instead of a compile time dependency: a missing block is reported by the generator instead of
 * breaking the whole build.
 *
 * The contract for the parallel groups is:
 * <ul>
 *   <li>{@code thaumcraft:eldritch} - the eldritch altar, the obelisk, the obelisk shaft and
 *       the capstone; the integer "variant" property accepts the original 1.7 metadata 0..3.</li>
 *   <li>{@code thaumcraft:eldritch_crust} - the glowing crust and the glyphed eldritch stone,
 *       the original 1.7 metadata 4 and 5.</li>
 *   <li>{@code thaumcraft:eldritch_deco} - the decorated eldritch stone, the metadata 6.</li>
 *   <li>{@code thaumcraft:eldritch_doorway} - the eldritch doorway and the eldritch lock, the
 *       metadata 7 and 8.</li>
 *   <li>{@code thaumcraft:eldritch_spawner} - the eldritch crab spawner and the runed eldritch
 *       trap, the metadata 9 and 10.</li>
 *   <li>{@code thaumcraft:eldritch_crystal} - the eldritch crystal, the metadata 11.</li>
 *   <li>{@code thaumcraft:cosmetic_solid}, {@code thaumcraft:cosmetic_solid_tough} and
 *       {@code thaumcraft:cosmetic_solid_thaumium} - the cosmetic solid with an integer "variant"
 *       property that accepts the original metadata 0..15 (1 ring floor, 11 wall, 12 rock, 13 no
 *       spawn, 14 crust, 15 table). The metadata that differ in hardness became separate blocks, so
 *       the original metadata decides the block.</li>
 *   <li>{@code thaumcraft:stairs_eldritch} - the eldritch stairs with a "facing" and a "half"
 *       property that reproduce the 1.7 stair metadata.</li>
 *   <li>{@code thaumcraft:eldritch_nothing}, {@code thaumcraft:airy},
 *       {@code thaumcraft:eldritch_portal}, {@code thaumcraft:loot_urn},
 *       {@code thaumcraft:loot_crate}, {@code thaumcraft:cosmetic_slab_stone} - plain blocks.</li>
 *   <li>{@code thaumcraft:eldritch_guardian} - the key room guardian entity.</li>
 *   <li>{@code thaumcraft:eldritch_object} - the item of the permanent item of the key room.</li>
 *   <li>{@code thaumcraft:mind_spider} - the entity of the mind spider spawner.</li>
 * </ul>
 */
public final class ThaumcraftOuterLands {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Thaumcraft.MODID, Registries.FEATURE);
    /** The feature that builds the Outer Lands rooms out of the labyrinth cells. */
    public static final RegistrySupplier<OuterLandsProvider.OuterLandsMazeFeature> MAZE =
            FEATURES.register("tc4_outer_lands_maze", OuterLandsProvider.OuterLandsMazeFeature::new);
    /** The feature that builds the eldritch ring and seeds the labyrinth. */
    public static final RegistrySupplier<thaumcraft.common.lib.world.EldritchRingFeature> RING =
            FEATURES.register("tc4_eldritch_ring", thaumcraft.common.lib.world.EldritchRingFeature::new);

    public static final ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> MAZE_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "tc4_outer_lands_maze"));
    public static final ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> RING_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "tc4_eldritch_ring"));

    private ThaumcraftOuterLands() {}

    public static void register() {
        FEATURES.register();
        // The dimension itself is a data pack level stem, so the server only learns about it
        // when the world loads. The server start reports the level that was created.
        thaumcraft.common.lib.world.OuterLandsDimension.registerLogging();
    }

    /* ------------------------------------------------------------------ */
    /* The lazily resolved eldritch content                                 */
    /* ------------------------------------------------------------------ */

    /** The registry id of an eldritch block of the Outer Lands. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
    }

    /** The eldritch block, or null when the eldritch content group has not landed yet. */
    public static Block block(String path) {
        ResourceLocation key = id(path);
        // The block registry answers with the air block for an unknown id, which would place
        // air where the original places a block. A missing block is reported instead.
        if (!BuiltInRegistries.BLOCK.containsKey(key)) return null;
        return BuiltInRegistries.BLOCK.get(key);
    }

    public static Block eldritch() {
        return block("eldritch");
    }

    /**
     * The native block that hosts an original eldritch metadata.
     *
     * <p>Oracle {@code BlockEldritch:231-260} gives every metadata its own hardness and blast
     * resistance, so the original family became six native blocks. The native blockstates carry
     * the model of their metadata, so the metadata decides the block and the variant.
     *
     * @param variant the original 1.7 metadata
     */
    public static Block eldritchBlock(int variant) {
        return blockById(OuterLandsWiringRules.eldritchBlock(variant));
    }

    /**
     * The cosmetic solid block of the hardness group that hosts the original metadata.
     *
     * <p>The original metadata that differ in hardness became separate blocks of the same class
     * (the oracle BlockCosmeticSolid:213-225 and :241-253), so the metadata decides the block.
     */
    public static Block cosmeticSolid(int variant) {
        return block(cosmeticSolidId(EldritchBlockRules.cosmeticGroup(variant)));
    }

    /** The block id of a cosmetic hardness group. */
    public static String cosmeticSolidId(int group) {
        if (group == 1) return "cosmetic_solid_tough";
        if (group == 2) return "cosmetic_solid_thaumium";
        return "cosmetic_solid";
    }

    /** True when the state is one of the three blocks of the cosmetic solid family. */
    public static boolean isCosmeticSolid(BlockState state) {
        for (int group = 0; group < 3; group++) {
            Block block = block(cosmeticSolidId(group));
            if (block != null && state.is(block)) return true;
        }
        return false;
    }

    /**
     * The eldritch stairs of the Outer Lands. The contract id is {@code stairs_eldritch}; the native
     * registration of the eldritch stairs carries the id of the original block, {@code eldritch_stairs}.
     */
    public static Block stairsEldritch() {
        Block block = block("stairs_eldritch");
        return block == null ? block("eldritch_stairs") : block;
    }

    public static Block eldritchNothing() {
        return block("eldritch_nothing");
    }

    /**
     * The native state of an original room placement.
     *
     * @param variant the original 1.7 metadata
     * @param facing the original direction ordinal, -1 when the block has no direction
     * @return the native state, or null when the block does not exist yet
     */
    public static BlockState state(DungeonRooms.Kind kind, int variant, int facing) {
        switch (kind) {
            case BEDROCK -> {
                return Blocks.BEDROCK.defaultBlockState();
            }
            case AIR -> {
                return Blocks.AIR.defaultBlockState();
            }
            case WEB -> {
                return Blocks.COBWEB.defaultBlockState();
            }
            case MOB_SPAWNER -> {
                return Blocks.SPAWNER.defaultBlockState();
            }
            case TAINT_FIBRES -> {
                return ThaumcraftTaint.FIBRES.get().defaultBlockState();
            }
            case CRYSTAL -> {
                // The original eldritch crystal uses the out of range metadata 7, which renders as
                // the aspect free crystal. The native mixed crystal is the same block.
                return ThaumcraftCrystalClusters.MIXED.get().defaultBlockState();
            }
            case ELDRITCH_NOTHING -> {
                Block block = eldritchNothing();
                return block == null ? null : block.defaultBlockState();
            }
            case AIRY -> {
                Block block = block("airy");
                return block == null ? null : withVariant(block.defaultBlockState(), variant);
            }
            case ELDRITCH_PORTAL -> {
                Block block = block("eldritch_portal");
                return block == null ? null : block.defaultBlockState();
            }
            case LOOT_URN -> {
                Block block = block("loot_urn");
                return block == null ? null : withVariant(block.defaultBlockState(), variant);
            }
            case LOOT_CRATE -> {
                Block block = block("loot_crate");
                return block == null ? null : withVariant(block.defaultBlockState(), variant);
            }
            case COSMETIC_SLAB_STONE -> {
                // The contract id is cosmetic_slab_stone; the native registration splits the
                // original slab into its two kinds, the arcane one and the eldritch one.
                Block block = blockById(OuterLandsWiringRules.stoneSlabBlock(variant));
                if (block == null) block = block("cosmetic_slab_stone");
                if (block == null) block = block("cosmetic_stone_slab_eldritch");
                return block == null ? null : slabState(block, variant);
            }
            case COSMETIC_SOLID -> {
                Block block = cosmeticSolid(variant);
                return block == null ? null : withVariant(block.defaultBlockState(), variant);
            }
            case ELDRITCH -> {
                // Oracle BlockEldritch:231-260 - the metadata decides the block, because the
                // hardness, the blast resistance and the light belong to the block in 1.21.1.
                Block block = eldritchBlock(variant);
                return block == null ? null : withVariant(block.defaultBlockState(), variant);
            }
            case STAIRS -> {
                Block block = stairsEldritch();
                return block == null ? null : withStairs(block.defaultBlockState(), variant);
            }
            default -> {
                return null;
            }
        }
    }

    /** Applies the original metadata to an integer "variant" property of the native block. */
    private static BlockState withVariant(BlockState state, int variant) {
        for (var property : state.getProperties()) {
            if (!(property instanceof IntegerProperty intProperty)) continue;
            String name = property.getName();
            if (!"variant".equals(name) && !"type".equals(name)) continue;
            java.util.Collection<Integer> values = intProperty.getPossibleValues();
            if (values.isEmpty()) continue;
            int min = java.util.Collections.min(values);
            int max = java.util.Collections.max(values);
            return state.setValue(intProperty, Math.min(Math.max(variant, min), max));
        }
        return state;
    }

    /**
     * The native state of an original slab metadata.
     *
     * <p>Oracle {@code GenLibraryRoom:78,86,95,98}: the desk of the library room is the
     * stone slab with the metadata one below the eldritch block and the stone slab with the
     * metadata nine above it. The metadata nine is the metadata one as the upper half of the
     * slab. The 1.21.1 slab carries its half in the "type" property and not in a "variant"
     * property, so the original metadata has to be decoded into that property.
     *
     * @param block the slab block that hosts the original metadata
     * @param metadata the original 1.7 metadata of the slab
     */
    public static BlockState slabState(Block block, int metadata) {
        BlockState state = block.defaultBlockState();
        if (!state.hasProperty(SlabBlock.TYPE)) return state;
        return state.setValue(SlabBlock.TYPE,
                OuterLandsWiringRules.slabUpper(metadata) ? SlabType.TOP : SlabType.BOTTOM);
    }

    /** The block of a full registry id, or null when the block does not exist. */
    private static Block blockById(String registryId) {
        ResourceLocation key = ResourceLocation.parse(registryId);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) return null;
        return BuiltInRegistries.BLOCK.get(key);
    }

    /** Applies the original 1.7 stair metadata (0 east, 1 west, 2 south, 3 north, 4..7 upside down). */
    private static BlockState withStairs(BlockState state, int meta) {
        Direction facing = switch (meta & 3) {
            case 0 -> Direction.EAST;
            case 1 -> Direction.WEST;
            case 2 -> Direction.SOUTH;
            default -> Direction.NORTH;
        };
        if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, facing);
        if (state.hasProperty(BlockStateProperties.HALF)) {
            state = state.setValue(BlockStateProperties.HALF, (meta & 4) != 0 ? Half.TOP : Half.BOTTOM);
        }
        return state;
    }

    /* ------------------------------------------------------------------ */
    /* The eldritch altar and the banners                                  */
    /* ------------------------------------------------------------------ */

    /** The banner device of the eldritch altar (the original blockWoodenDevice metadata 8). */
    public static BlockState bannerState(int variant) {
        Block block = block("wooden_device");
        return block == null ? null : withVariant(block.defaultBlockState(), variant);
    }

    /**
     * Flags the eldritch altar as a spawner. The altar tile belongs to the parallel eldritch
     * content group, so the setter is resolved by name instead of a compile time dependency.
     *
     * @param type 0 the original spawn type 0, 1 the original spawn type 1
     */
    public static boolean configureAltar(BlockEntity entity, boolean spawner, byte type) {
        if (entity == null) return false;
        boolean flagged = invoke(entity, "setSpawner", new Class<?>[] {boolean.class}, spawner);
        return invoke(entity, "setSpawnType", new Class<?>[] {byte.class}, type) && flagged;
    }

    /** Points the banner of an eldritch altar to the original facing value. */
    public static boolean configureBanner(BlockEntity entity, byte facing) {
        if (entity == null) return false;
        return invoke(entity, "setFacing", new Class<?>[] {byte.class}, facing);
    }

    /** Points the eldritch crab spawner into the original direction. */
    public static boolean configureFacing(BlockEntity entity, byte facing) {
        if (entity == null) return false;
        return invoke(entity, "setFacing", new Class<?>[] {byte.class}, facing);
    }

    private static boolean invoke(Object target, String name, Class<?>[] types, Object... arguments) {
        try {
            target.getClass().getMethod(name, types).invoke(target, arguments);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    /* ------------------------------------------------------------------ */
    /* The entities of the rooms                                            */
    /* ------------------------------------------------------------------ */
    /**
     * Creates the entities of a room (oracle GenKeyRoom:97-115 and GenPassage:244-258).
     * The entity ids of the parallel eldritch content groups are resolved lazily.
     */
    public static void spawnRoomEntities(WorldGenLevel level, DungeonRooms.Builder builder) {
        for (DungeonRooms.Spawn spawn : builder.spawns) {
            BlockPos pos = new BlockPos(spawn.x(), spawn.y(), spawn.z());
            switch (spawn.kind()) {
                case ELDRITCH_GUARDIAN -> spawnEntity(level, pos, "eldritch_guardian");
                case PERMANENT_ITEM -> spawnPermanentItem(level, pos, spawn.variant());
                case MIND_SPIDER -> spawnMindSpider(level, pos);
                default -> {
                }
            }
        }
    }

    private static void spawnEntity(WorldGenLevel level, BlockPos pos, String entityId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id(entityId));
        if (type == null) return;
        Entity entity = type.create(level.getLevel());
        if (entity == null) return;
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getLevel().getRandom().nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(entity);
    }

    /** Oracle GenKeyRoom:97-101 - the permanent eldritch object of the key room. */
    private static void spawnPermanentItem(WorldGenLevel level, BlockPos pos, int variant) {
        // The original item carried the metadata two, the runed tablet.
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(OuterLandsWiringRules.eldritchObjectItem(variant)));
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id("inventory_item"));
        if (item == null || type == null) return;
        Entity entity = type.create(level.getLevel());
        if (!(entity instanceof ItemEntity carrier)) return;
        carrier.setItem(new ItemStack(item, 1));
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(entity);
    }

    /** Oracle GenPassage:249-257 - the mind spider spawner of a spider passage. */
    private static void spawnMindSpider(WorldGenLevel level, BlockPos pos) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id("mind_spider"));
        if (type == null) return;
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)) return;
        spawner.setEntityId(type, level.getLevel().getRandom());
    }
}
