package thaumcraft.common.config;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.feature.Feature;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockAiry;
import thaumcraft.common.blocks.BlockCosmeticSolid;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.blocks.BlockLoot;
import thaumcraft.common.blocks.BlockTaintFibres;
import thaumcraft.common.blocks.BlockWoodenDevice;
import thaumcraft.common.lib.world.DungeonRooms;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.lib.world.OuterLandsWiringRules;
import thaumcraft.common.tiles.TileBanner;
import thaumcraft.common.tiles.TileEldritchAltar;
import thaumcraft.common.tiles.TileEldritchCrabSpawner;
import thaumcraft.common.tiles.TileEldritchCrystal;

/**
 * Registration of the TC4 Outer Lands and of the blocks it needs from the eldritch content.
 *
 * <p>The eldritch blocks, tiles and entities of the Outer Lands are ported by the eldritch content
 * groups, so they are resolved through their real registry suppliers here. The original 1.7
 * metadata of a block family became a block state property, and because 1.21.1 binds the hardness
 * and the blast resistance to the block, the metadata families that differ became small families
 * of blocks: the generator asks {@link OuterLandsWiringRules} for the block that hosts a metadata
 * and then sets the metadata on it.
 *
 * <p>The contract of the eldritch content groups is:
 * <ul>
 *   <li>{@code thaumcraft:eldritch} - the eldritch altar, the obelisk, the obelisk middle and the
 *       capstone. It carries the integer property {@code variant}.</li>
 *   <li>{@code thaumcraft:eldritch_crust} - the glowing crust and the glyphed eldritch stone.</li>
 *   <li>{@code thaumcraft:eldritch_deco} - the decorated eldritch stone.</li>
 *   <li>{@code thaumcraft:eldritch_doorway} - the eldritch doorway and the eldritch lock.</li>
 *   <li>{@code thaumcraft:eldritch_spawner} - the eldritch crab spawner and the runed eldritch trap.</li>
 *   <li>{@code thaumcraft:eldritch_crystal} - the eldritch crystal of the rooms.</li>
 *   <li>{@code thaumcraft:cosmetic_solid}, {@code thaumcraft:cosmetic_solid_tough} and
 *       {@code thaumcraft:cosmetic_solid_thaumium} - the cosmetic solid family with its integer
 *       property {@code variant}.</li>
 *   <li>{@code thaumcraft:cosmetic_stone_slab_arcane} and
 *       {@code thaumcraft:cosmetic_stone_slab_eldritch} - the two stone slabs of the library.</li>
 *   <li>{@code thaumcraft:eldritch_stairs} - the eldritch stairs with their facing and half.</li>
 *   <li>{@code thaumcraft:eldritch_nothing}, {@code thaumcraft:airy},
 *       {@code thaumcraft:airy_unbreakable}, {@code thaumcraft:eldritch_portal},
 *       {@code thaumcraft:loot_urn}, {@code thaumcraft:loot_crate} - the plain blocks.</li>
 *   <li>{@code thaumcraft:eldritch_guardian} - the key room guardian entity.</li>
 *   <li>{@code thaumcraft:eldritch_tablet} - the item of the permanent item of the key room.</li>
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

    private static boolean eldritchRegistered;

    public static void register() {
        // The eldritch content group owns the blocks; the Outer Lands owns the
        // registration hook because the labyrinth and the ring are their only
        // consumer. The class guards itself against a second registration.
        if (!eldritchRegistered) {
            eldritchRegistered = true;
            ThaumcraftEldritchBlocks.register();
        }
        FEATURES.register();
    }

    /* ------------------------------------------------------------------ */
    /* The eldritch content of the Outer Lands                              */
    /* ------------------------------------------------------------------ */

    /** The registry id of an eldritch block of the Outer Lands. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
    }

    /** The eldritch block of the altar, the obelisk and the capstone. */
    public static Block eldritch() {
        return ThaumcraftEldritchBlocks.ELDRITCH.get();
    }

    /** The ordinary cosmetic stone of the rooms. */
    public static Block cosmeticSolid() {
        return ThaumcraftEldritchBlocks.COSMETIC_SOLID.get();
    }

    /** The eldritch stairs of the rooms. */
    public static Block stairsEldritch() {
        return ThaumcraftEldritchBlocks.STAIRS_ELDRITCH.get();
    }

    /** The invisible wall face of the rooms. */
    public static Block eldritchNothing() {
        return ThaumcraftEldritchBlocks.ELDRITCH_NOTHING.get();
    }

    /* ------------------------------------------------------------------ */
    /* The native state of an original room placement                       */
    /* ------------------------------------------------------------------ */

    /**
     * The native state of an original room placement.
     *
     * @param variant the original 1.7 metadata
     * @param facing the original direction ordinal, -1 when the block has no direction
     * @return the native state
     */
    public static BlockState state(DungeonRooms.Kind kind, int variant, int facing) {
        return switch (kind) {
            case BEDROCK -> Blocks.BEDROCK.defaultBlockState();
            case AIR -> Blocks.AIR.defaultBlockState();
            case WEB -> Blocks.COBWEB.defaultBlockState();
            case MOB_SPAWNER -> Blocks.SPAWNER.defaultBlockState();
            case TAINT_FIBRES -> ThaumcraftTaint.FIBRES.get().defaultBlockState()
                    .setValue(BlockTaintFibres.TYPE, variant);
            case CRYSTAL -> {
                // Oracle BlockCrystal:117-125 - the metadata seven of the crystal block is the
                // eldritch crystal, it is the eleventh metadata of the eldritch family here.
                Block block = ThaumcraftEldritchBlocks.ELDRITCH_CRYSTAL.get();
                yield block.defaultBlockState().setValue(BlockEldritch.VARIANT, EldritchBlockRules.ELDRITCH_CRYSTAL);
            }
            case ELDRITCH_NOTHING -> ThaumcraftEldritchBlocks.ELDRITCH_NOTHING.get().defaultBlockState();
            case AIRY -> {
                // Oracle GenCommon:88-92 - the invisible door of the rooms is the unbreakable airy.
                Block block = airyBlock(variant);
                yield block.defaultBlockState().setValue(BlockAiry.VARIANT, variant);
            }
            case ELDRITCH_PORTAL -> ThaumcraftEldritchBlocks.ELDRITCH_PORTAL.get().defaultBlockState();
            case LOOT_URN -> ThaumcraftEldritchBlocks.LOOT_URN.get().defaultBlockState()
                    .setValue(BlockLoot.RARITY, OuterLandsWiringRules.lootRarity(variant));
            case LOOT_CRATE -> ThaumcraftEldritchBlocks.LOOT_CRATE.get().defaultBlockState()
                    .setValue(BlockLoot.RARITY, OuterLandsWiringRules.lootRarity(variant));
            case COSMETIC_SLAB_STONE -> {
                // Oracle GenCommon:12 and GenLibraryRoom:78-98 - the slab metadata holds the
                // variant in the lower bits and the upper half in the third bit.
                Block block = slabBlock(variant);
                yield block.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE,
                        OuterLandsWiringRules.slabUpper(variant) ? Half.TOP : Half.BOTTOM);
            }
            case COSMETIC_SOLID -> {
                Block block = cosmeticSolidBlock(variant);
                yield block.defaultBlockState().setValue(BlockCosmeticSolid.VARIANT, variant);
            }
            case ELDRITCH -> {
                Block block = eldritchBlock(variant);
                yield block.defaultBlockState().setValue(BlockEldritch.VARIANT, variant);
            }
            case STAIRS -> {
                Block block = ThaumcraftEldritchBlocks.STAIRS_ELDRITCH.get();
                yield block.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, OuterLandsWiringRules.stairsFacing(variant))
                        .setValue(BlockStateProperties.HALF, OuterLandsWiringRules.stairsUpper(variant) ? Half.TOP : Half.BOTTOM);
            }
        };
    }

    /** The eldritch block that hosts an original {@code BlockEldritch} metadata. */
    public static Block eldritchBlock(int variant) {
        return switch (OuterLandsWiringRules.eldritchBlock(variant)) {
            case "thaumcraft:eldritch_crust" -> ThaumcraftEldritchBlocks.ELDRITCH_CRUST.get();
            case "thaumcraft:eldritch_deco" -> ThaumcraftEldritchBlocks.ELDRITCH_DECO.get();
            case "thaumcraft:eldritch_doorway" -> ThaumcraftEldritchBlocks.ELDRITCH_DOORWAY.get();
            case "thaumcraft:eldritch_spawner" -> ThaumcraftEldritchBlocks.ELDRITCH_SPAWNER.get();
            case "thaumcraft:eldritch_crystal" -> ThaumcraftEldritchBlocks.ELDRITCH_CRYSTAL.get();
            default -> ThaumcraftEldritchBlocks.ELDRITCH.get();
        };
    }

    /** The cosmetic solid block that hosts an original {@code BlockCosmeticSolid} metadata. */
    public static Block cosmeticSolidBlock(int variant) {
        return switch (OuterLandsWiringRules.cosmeticSolidBlock(variant)) {
            case "thaumcraft:cosmetic_solid_tough" -> ThaumcraftEldritchBlocks.COSMETIC_SOLID_TOUGH.get();
            case "thaumcraft:cosmetic_solid_thaumium" -> ThaumcraftEldritchBlocks.COSMETIC_SOLID_THAUMIUM.get();
            default -> ThaumcraftEldritchBlocks.COSMETIC_SOLID.get();
        };
    }

    /** The stone slab of an original slab metadata. */
    public static Block slabBlock(int variant) {
        return OuterLandsWiringRules.slabVariant(variant) == EldritchBlockRules.SLAB_STONE_ELDRITCH
                ? ThaumcraftEldritchBlocks.COSMETIC_STONE_SLAB_ELDRITCH.get()
                : ThaumcraftEldritchBlocks.COSMETIC_STONE_SLAB_ARCANE.get();
    }

    /** The airy block that hosts an original {@code BlockAiry} metadata. */
    public static Block airyBlock(int variant) {
        return switch (OuterLandsWiringRules.airyBlock(variant)) {
            case "thaumcraft:airy_node" -> ThaumcraftEldritchBlocks.AIRY_NODE.get();
            case "thaumcraft:airy_hole" -> ThaumcraftEldritchBlocks.AIRY_HOLE.get();
            case "thaumcraft:airy_unbreakable" -> ThaumcraftEldritchBlocks.AIRY_UNBREAKABLE.get();
            default -> ThaumcraftEldritchBlocks.AIRY.get();
        };
    }

    /**
     * True when the state belongs to the eldritch family.
     *
     * <p>The 1.7 original had a single eldritch block with eleven metadata, so every test of the
     * original that looks for "the eldritch block" has to look for the whole native family.
     */
    public static boolean isEldritch(BlockState state) {
        return state.is(ThaumcraftEldritchBlocks.ELDRITCH.get())
                || state.is(ThaumcraftEldritchBlocks.ELDRITCH_CRUST.get())
                || state.is(ThaumcraftEldritchBlocks.ELDRITCH_DECO.get())
                || state.is(ThaumcraftEldritchBlocks.ELDRITCH_DOORWAY.get())
                || state.is(ThaumcraftEldritchBlocks.ELDRITCH_SPAWNER.get())
                || state.is(ThaumcraftEldritchBlocks.ELDRITCH_CRYSTAL.get());
    }

    /** True when the state belongs to the cosmetic solid family. */
    public static boolean isCosmeticSolid(BlockState state) {
        return state.is(ThaumcraftEldritchBlocks.COSMETIC_SOLID.get())
                || state.is(ThaumcraftEldritchBlocks.COSMETIC_SOLID_TOUGH.get())
                || state.is(ThaumcraftEldritchBlocks.COSMETIC_SOLID_THAUMIUM.get());
    }

    /* ------------------------------------------------------------------ */
    /* The eldritch altar and the banners                                  */
    /* ------------------------------------------------------------------ */

    /**
     * The banner device of the eldritch altar (the original blockWoodenDevice metadata 8).
     *
     * @param variant the original metadata of the wooden device
     */
    public static BlockState bannerState(int variant) {
        return ThaumcraftEldritchBlocks.WOODEN_DEVICE.get().defaultBlockState()
                .setValue(BlockWoodenDevice.VARIANT, variant);
    }

    /**
     * Flags the eldritch altar as a spawner.
     *
     * @param type 0 the original spawn type 0, 1 the original spawn type 1
     */
    public static boolean configureAltar(BlockEntity entity, boolean spawner, byte type) {
        if (!(entity instanceof TileEldritchAltar altar)) return false;
        altar.setSpawner(spawner);
        altar.setSpawnType(type);
        altar.setChanged();
        return true;
    }

    /**
     * Points the banner of an eldritch altar to the original facing value.
     *
     * <p>Oracle {@code WorldGenEldritchRing:103-120} writes the values eight, zero, twelve and
     * four; oracle {@code BlockWoodenDevice:264-290} puts the cloth of the banner into the
     * quarter of the block that the value names.
     */
    public static boolean configureBanner(BlockEntity entity, byte facing) {
        if (!(entity instanceof TileBanner banner)) return false;
        banner.setFacing(OuterLandsWiringRules.bannerDirection(facing));
        return true;
    }

    /** Points the eldritch crab spawner into the original direction. */
    public static boolean configureFacing(BlockEntity entity, byte facing) {
        if (!(entity instanceof TileEldritchCrabSpawner spawner)) return false;
        spawner.setFacing(facing);
        return true;
    }

    /** Points the crystal of a room into the original direction. */
    public static boolean configureCrystal(BlockEntity entity, int facing) {
        if (!(entity instanceof TileEldritchCrystal crystal)) return false;
        crystal.setOrientation(OuterLandsWiringRules.crystalDirection(facing));
        return true;
    }

    /* ------------------------------------------------------------------ */
    /* The entities of the rooms                                            */
    /* ------------------------------------------------------------------ */
    /**
     * Creates the entities of a room (oracle GenKeyRoom:97-115 and GenPassage:244-258).
     */
    public static void spawnRoomEntities(WorldGenLevel level, DungeonRooms.Builder builder) {
        for (DungeonRooms.Spawn spawn : builder.spawns) {
            BlockPos pos = new BlockPos(spawn.x(), spawn.y(), spawn.z());
            switch (spawn.kind()) {
                case ELDRITCH_GUARDIAN -> spawnEntity(level, pos, ThaumcraftWarp.ELDRITCH_GUARDIAN.get());
                case PERMANENT_ITEM -> spawnPermanentItem(level, pos);
                case MIND_SPIDER -> spawnMindSpider(level, pos);
                default -> {
                }
            }
        }
    }

    private static void spawnEntity(WorldGenLevel level, BlockPos pos, EntityType<?> type) {
        Entity entity = type.create(level.getLevel());
        if (entity == null) return;
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getLevel().getRandom().nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(entity);
    }

    /** Oracle GenKeyRoom:97-101 - the permanent eldritch object of the key room. */
    private static void spawnPermanentItem(WorldGenLevel level, BlockPos pos) {
        Item item = ThaumcraftLegacyItems.ELDRITCH_TABLET.get();
        Entity entity = ThaumcraftContent.INVENTORY_ITEM.get().create(level.getLevel());
        if (!(entity instanceof ItemEntity carrier)) return;
        carrier.setItem(new ItemStack(item, 1));
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(entity);
    }

    /** Oracle GenPassage:249-257 - the mind spider spawner of a spider passage. */
    private static void spawnMindSpider(WorldGenLevel level, BlockPos pos) {
        EntityType<?> type = ThaumcraftWarp.MIND_SPIDER.get();
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)) return;
        spawner.setEntityId(type, level.getLevel().getRandom());
    }
}
