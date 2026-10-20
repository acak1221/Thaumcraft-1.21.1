package thaumcraft.common.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockAiry;
import thaumcraft.common.blocks.BlockArcaneDoor;
import thaumcraft.common.blocks.BlockCosmeticOpaque;
import thaumcraft.common.blocks.BlockCosmeticSolid;
import thaumcraft.common.blocks.BlockCosmeticStoneSlab;
import thaumcraft.common.blocks.BlockCosmeticWoodSlab;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.blocks.BlockEldritchNothing;
import thaumcraft.common.blocks.BlockEldritchPortal;
import thaumcraft.common.blocks.BlockLoot;
import thaumcraft.common.blocks.BlockLootItem;
import thaumcraft.common.blocks.BlockMagicBox;
import thaumcraft.common.blocks.BlockMetalDevice;
import thaumcraft.common.blocks.BlockMirror;
import thaumcraft.common.blocks.BlockStoneDevice;
import thaumcraft.common.blocks.BlockWoodenDevice;
import thaumcraft.common.blocks.ItemArcaneDoor;
import thaumcraft.common.lib.world.EldritchBlockRules;
import thaumcraft.common.tiles.TileArcanePressurePlate;
import thaumcraft.common.tiles.TileBanner;
import thaumcraft.common.tiles.TileBrainbox;
import thaumcraft.common.tiles.TileEldritchNothing;
import thaumcraft.common.tiles.TileEldritchPortal;
import thaumcraft.common.tiles.TileEtherealBloom;
import thaumcraft.common.tiles.TileFluxScrubber;
import thaumcraft.common.tiles.TileJarBrain;
import thaumcraft.common.tiles.TileJarNode;
import thaumcraft.common.tiles.TileMagicBox;
import thaumcraft.common.tiles.TileMirror;
import thaumcraft.common.tiles.TileMirrorEssentia;
import thaumcraft.common.tiles.TileNode;
import thaumcraft.common.tiles.TileOwned;
import thaumcraft.common.tiles.TileWardingStone;
import thaumcraft.common.tiles.TileWardingStoneFence;

/**
 * The original eldritch, cosmetic and device block families with their block
 * entities. Every value comes from the TC4 4.2.3.5 oracle and the pure rules
 * in {@link EldritchBlockRules}.
 *
 * <p>Minecraft 1.21.1 binds the hardness and the blast resistance to the block
 * and not to the block state, so the original metadata families that differ in
 * hardness become separate blocks of the same class.
 */
public final class ThaumcraftEldritchBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);

    // ------------------------------------------------------------------ blocks

    /** The eldritch altar, obelisk, obelisk middle and capstone. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH = BLOCKS.register("eldritch",
            () -> new BlockEldritch(0, eldritchProperties(0)));
    /** The glowing crust and the glyphed eldritch stone. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH_CRUST = BLOCKS.register("eldritch_crust",
            () -> new BlockEldritch(1, eldritchProperties(1)));
    /** The decorated eldritch stone. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH_DECO = BLOCKS.register("eldritch_deco",
            () -> new BlockEldritch(2, eldritchProperties(2)));
    /** The eldritch doorway and the eldritch lock. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH_DOORWAY = BLOCKS.register("eldritch_doorway",
            () -> new BlockEldritch(3, eldritchProperties(3)));
    /** The eldritch crab spawner and the runed eldritch trap. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH_SPAWNER = BLOCKS.register("eldritch_spawner",
            () -> new BlockEldritch(4, eldritchProperties(4)));
    /** The eldritch crystal. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH_CRYSTAL = BLOCKS.register("eldritch_crystal",
            () -> new BlockEldritch(5, eldritchProperties(5)));

    public static final RegistrySupplier<BlockEldritchNothing> ELDRITCH_NOTHING = BLOCKS.register("eldritch_nothing",
            () -> new BlockEldritchNothing(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 6000000.0F).sound(SoundType.STONE).noOcclusion().noCollission()));

    public static final RegistrySupplier<BlockEldritchPortal> ELDRITCH_PORTAL = BLOCKS.register("eldritch_portal",
            () -> new BlockEldritchPortal(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA)
                    .strength(-1.0F, 200000.0F).sound(SoundType.STONE).noOcclusion().noCollission()
                    .lightLevel(state -> 15)));

    /** The ordinary cosmetic stone. */
    public static final RegistrySupplier<BlockCosmeticSolid> COSMETIC_SOLID = BLOCKS.register("cosmetic_solid",
            () -> new BlockCosmeticSolid(0, cosmeticProperties(0)));
    /** The obsidian tile and its shaded variant. */
    public static final RegistrySupplier<BlockCosmeticSolid> COSMETIC_SOLID_TOUGH = BLOCKS.register("cosmetic_solid_tough",
            () -> new BlockCosmeticSolid(1, cosmeticProperties(1)));
    /** The thaumium block, the pedestal top and the arcane stone. */
    public static final RegistrySupplier<BlockCosmeticSolid> COSMETIC_SOLID_THAUMIUM = BLOCKS.register("cosmetic_solid_thaumium",
            () -> new BlockCosmeticSolid(2, cosmeticProperties(2)));

    public static final RegistrySupplier<BlockCosmeticOpaque> COSMETIC_OPAQUE = BLOCKS.register("cosmetic_opaque",
            () -> new BlockCosmeticOpaque(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(EldritchBlockRules.COSMETIC_OPAQUE_HARDNESS, EldritchBlockRules.COSMETIC_OPAQUE_RESISTANCE)
                    .sound(SoundType.STONE)));

    public static final RegistrySupplier<BlockCosmeticStoneSlab> COSMETIC_STONE_SLAB_ARCANE = BLOCKS.register("cosmetic_stone_slab_arcane",
            () -> new BlockCosmeticStoneSlab(EldritchBlockRules.SLAB_STONE_ARCANE,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 10.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<BlockCosmeticStoneSlab> COSMETIC_STONE_SLAB_ELDRITCH = BLOCKS.register("cosmetic_stone_slab_eldritch",
            () -> new BlockCosmeticStoneSlab(EldritchBlockRules.SLAB_STONE_ELDRITCH,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0F, 10.0F).sound(SoundType.STONE)));

    public static final RegistrySupplier<BlockCosmeticWoodSlab> COSMETIC_WOOD_SLAB_GREATWOOD = BLOCKS.register("cosmetic_wood_slab_greatwood",
            () -> new BlockCosmeticWoodSlab(EldritchBlockRules.SLAB_WOOD_GREATWOOD,
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 5.0F).sound(SoundType.WOOD)));

    public static final RegistrySupplier<BlockCosmeticWoodSlab> COSMETIC_WOOD_SLAB_SILVERWOOD = BLOCKS.register("cosmetic_wood_slab_silverwood",
            () -> new BlockCosmeticWoodSlab(EldritchBlockRules.SLAB_WOOD_SILVERWOOD,
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 5.0F).sound(SoundType.WOOD)));

    // The eldritch stairs are not registered here: the id "eldritch_stairs" belongs
    // to the legacy tile unit, whose block takes the texture of the eldritch stone,
    // exactly like the original cosmetic stairs.

    public static final RegistrySupplier<BlockArcaneDoor> ARCANE_DOOR = BLOCKS.register("arcane_door",
            () -> new BlockArcaneDoor(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(15.0F, 999.0F).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<BlockMagicBox> MAGIC_BOX = BLOCKS.register("magic_box",
            () -> new BlockMagicBox(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.5F, 12.5F).sound(SoundType.WOOD).noOcclusion()));

    public static final RegistrySupplier<BlockMirror> MIRROR = BLOCKS.register("mirror",
            () -> new BlockMirror(BlockBehaviour.Properties.of().mapColor(MapColor.ICE)
                    .strength(1.0F, 10.0F).sound(SoundType.GLASS).noOcclusion()));

    public static final RegistrySupplier<BlockMetalDevice> METAL_DEVICE = BLOCKS.register("metal_device",
            () -> new BlockMetalDevice(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(EldritchBlockRules.DEVICE_METAL_HARDNESS, EldritchBlockRules.DEVICE_METAL_RESISTANCE)
                    .sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<BlockWoodenDevice> WOODEN_DEVICE = BLOCKS.register("wooden_device",
            () -> new BlockWoodenDevice(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(EldritchBlockRules.DEVICE_WOOD_HARDNESS, EldritchBlockRules.DEVICE_WOOD_RESISTANCE)
                    .sound(SoundType.WOOD).noOcclusion()));

    public static final RegistrySupplier<BlockStoneDevice> STONE_DEVICE = BLOCKS.register("stone_device",
            () -> new BlockStoneDevice(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(EldritchBlockRules.DEVICE_STONE_HARDNESS, EldritchBlockRules.DEVICE_STONE_RESISTANCE)
                    .sound(SoundType.STONE).noOcclusion()));

    public static final RegistrySupplier<BlockLoot> LOOT_URN = BLOCKS.register("loot_urn",
            () -> new BlockLoot(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.15F, 0.0F).sound(SoundType.STONE).noOcclusion()));

    public static final RegistrySupplier<BlockLoot> LOOT_CRATE = BLOCKS.register("loot_crate",
            () -> new BlockLoot(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.15F, 0.0F).sound(SoundType.WOOD).noOcclusion()));

    /** The ordinary airy blocks. */
    public static final RegistrySupplier<BlockAiry> AIRY = BLOCKS.register("airy",
            () -> new BlockAiry(0, airyProperties(0)));
    /** The node and the energised node. */
    public static final RegistrySupplier<BlockAiry> AIRY_NODE = BLOCKS.register("airy_node",
            () -> new BlockAiry(1, airyProperties(1)));
    /** The magic and the void hole. */
    public static final RegistrySupplier<BlockAiry> AIRY_HOLE = BLOCKS.register("airy_hole",
            () -> new BlockAiry(2, airyProperties(2)));
    /** The unbreakable airy block. */
    public static final RegistrySupplier<BlockAiry> AIRY_UNBREAKABLE = BLOCKS.register("airy_unbreakable",
            () -> new BlockAiry(3, airyProperties(3)));

    public static final RegistrySupplier<JarBlock> JAR_BRAIN = BLOCKS.register("jar_brain",
            () -> new JarBlock(0, BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F, 15.0F).sound(SoundType.GLASS).noOcclusion()));

    public static final RegistrySupplier<JarBlock> JAR_NODE = BLOCKS.register("jar_node",
            () -> new JarBlock(1, BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F, 15.0F).sound(SoundType.GLASS).noOcclusion()));

    public static final RegistrySupplier<EtherealBloomBlock> ETHEREAL_BLOOM = BLOCKS.register("ethereal_bloom",
            () -> new EtherealBloomBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
                    .noCollission().sound(SoundType.GRASS).lightLevel(state -> 15)));

    private static BlockBehaviour.Properties eldritchProperties(int group) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                .strength(EldritchBlockRules.eldritchGroupHardness(group), EldritchBlockRules.eldritchGroupResistance(group))
                .sound(SoundType.STONE).noOcclusion()
                .lightLevel(state -> EldritchBlockRules.eldritchLight(state.getValue(BlockEldritch.VARIANT)));
    }

    private static BlockBehaviour.Properties cosmeticProperties(int group) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                .strength(EldritchBlockRules.cosmeticGroupHardness(group), EldritchBlockRules.cosmeticGroupResistance(group))
                .sound(SoundType.STONE).noOcclusion()
                .lightLevel(state -> EldritchBlockRules.cosmeticLight(state.getValue(BlockCosmeticSolid.VARIANT)));
    }

    private static BlockBehaviour.Properties airyProperties(int group) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
                .strength(EldritchBlockRules.airyGroupHardness(group), EldritchBlockRules.airyGroupResistance(group))
                .sound(SoundType.WOOL).noOcclusion().noCollission()
                .lightLevel(state -> EldritchBlockRules.airyLight(state.getValue(BlockAiry.VARIANT)));
    }

    // ------------------------------------------------------------------- items

    public static final RegistrySupplier<Item> ELDRITCH_ITEM = ITEMS.register("eldritch", () -> new BlockItem(ELDRITCH.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ELDRITCH_CRUST_ITEM = ITEMS.register("eldritch_crust", () -> new BlockItem(ELDRITCH_CRUST.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ELDRITCH_DECO_ITEM = ITEMS.register("eldritch_deco", () -> new BlockItem(ELDRITCH_DECO.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ELDRITCH_DOORWAY_ITEM = ITEMS.register("eldritch_doorway", () -> new BlockItem(ELDRITCH_DOORWAY.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ELDRITCH_SPAWNER_ITEM = ITEMS.register("eldritch_spawner", () -> new BlockItem(ELDRITCH_SPAWNER.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ELDRITCH_CRYSTAL_ITEM = ITEMS.register("eldritch_crystal", () -> new BlockItem(ELDRITCH_CRYSTAL.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ELDRITCH_NOTHING_ITEM = ITEMS.register("eldritch_nothing", () -> new BlockItem(ELDRITCH_NOTHING.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ELDRITCH_PORTAL_ITEM = ITEMS.register("eldritch_portal", () -> new BlockItem(ELDRITCH_PORTAL.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_SOLID_ITEM = ITEMS.register("cosmetic_solid", () -> new BlockItem(COSMETIC_SOLID.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_SOLID_TOUGH_ITEM = ITEMS.register("cosmetic_solid_tough", () -> new BlockItem(COSMETIC_SOLID_TOUGH.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_SOLID_THAUMIUM_ITEM = ITEMS.register("cosmetic_solid_thaumium", () -> new BlockItem(COSMETIC_SOLID_THAUMIUM.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_OPAQUE_ITEM = ITEMS.register("cosmetic_opaque", () -> new BlockItem(COSMETIC_OPAQUE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_STONE_SLAB_ARCANE_ITEM = ITEMS.register("cosmetic_stone_slab_arcane", () -> new BlockItem(COSMETIC_STONE_SLAB_ARCANE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_STONE_SLAB_ELDRITCH_ITEM = ITEMS.register("cosmetic_stone_slab_eldritch", () -> new BlockItem(COSMETIC_STONE_SLAB_ELDRITCH.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_WOOD_SLAB_GREATWOOD_ITEM = ITEMS.register("cosmetic_wood_slab_greatwood", () -> new BlockItem(COSMETIC_WOOD_SLAB_GREATWOOD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_WOOD_SLAB_SILVERWOOD_ITEM = ITEMS.register("cosmetic_wood_slab_silverwood", () -> new BlockItem(COSMETIC_WOOD_SLAB_SILVERWOOD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ARCANE_DOOR_ITEM = ITEMS.register("arcane_door", () -> new ItemArcaneDoor(new Item.Properties()));
    public static final RegistrySupplier<Item> MAGIC_BOX_ITEM = ITEMS.register("magic_box", () -> new BlockItem(MAGIC_BOX.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> MIRROR_ITEM = ITEMS.register("mirror", () -> new BlockItem(MIRROR.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> METAL_DEVICE_ITEM = ITEMS.register("metal_device", () -> new BlockItem(METAL_DEVICE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> WOODEN_DEVICE_ITEM = ITEMS.register("wooden_device", () -> new BlockItem(WOODEN_DEVICE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> STONE_DEVICE_ITEM = ITEMS.register("stone_device", () -> new BlockItem(STONE_DEVICE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> LOOT_URN_ITEM = ITEMS.register("loot_urn", () -> new BlockLootItem(LOOT_URN.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> LOOT_CRATE_ITEM = ITEMS.register("loot_crate", () -> new BlockLootItem(LOOT_CRATE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> AIRY_ITEM = ITEMS.register("airy", () -> new BlockItem(AIRY.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> AIRY_NODE_ITEM = ITEMS.register("airy_node", () -> new BlockItem(AIRY_NODE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> AIRY_HOLE_ITEM = ITEMS.register("airy_hole", () -> new BlockItem(AIRY_HOLE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> JAR_BRAIN_ITEM = ITEMS.register("jar_brain", () -> new BlockItem(JAR_BRAIN.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> JAR_NODE_ITEM = ITEMS.register("jar_node", () -> new BlockItem(JAR_NODE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ETHEREAL_BLOOM_ITEM = ITEMS.register("ethereal_bloom", () -> new BlockItem(ETHEREAL_BLOOM.get(), new Item.Properties()));

    // ---------------------------------------------------------- block entities

    public static final RegistrySupplier<BlockEntityType<BlockEntity>> ELDRITCH_TILE = BLOCK_ENTITIES.register("eldritch",
            () -> BlockEntityType.Builder.of((pos, state) -> BlockEldritch.createTile(pos, state),
                    ELDRITCH.get(), ELDRITCH_CRUST.get(), ELDRITCH_DECO.get(), ELDRITCH_DOORWAY.get(),
                    ELDRITCH_SPAWNER.get(), ELDRITCH_CRYSTAL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileEldritchNothing>> ELDRITCH_NOTHING_TILE = BLOCK_ENTITIES.register("eldritch_nothing",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileEldritchNothing(pos, state), ELDRITCH_NOTHING.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileEldritchPortal>> ELDRITCH_PORTAL_TILE = BLOCK_ENTITIES.register("eldritch_portal",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileEldritchPortal(pos, state), ELDRITCH_PORTAL.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileMirror>> MIRROR_TILE = BLOCK_ENTITIES.register("mirror",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileMirror(pos, state), MIRROR.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileMirrorEssentia>> MIRROR_ESSENTIA_TILE = BLOCK_ENTITIES.register("mirror_essentia",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileMirrorEssentia(pos, state), MIRROR.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileMagicBox>> MAGIC_BOX_TILE = BLOCK_ENTITIES.register("magic_box",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileMagicBox(pos, state), MAGIC_BOX.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileBanner>> BANNER_TILE = BLOCK_ENTITIES.register("banner",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileBanner(pos, state), WOODEN_DEVICE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileArcanePressurePlate>> PRESSURE_PLATE_TILE = BLOCK_ENTITIES.register("arcane_pressure_plate",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileArcanePressurePlate(pos, state), WOODEN_DEVICE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileBrainbox>> BRAINBOX_TILE = BLOCK_ENTITIES.register("brainbox",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileBrainbox(pos, state), METAL_DEVICE.get()).build(null));

    /** The metal grate tile is owned by ThaumcraftGrate (the upstream item grate unit). */

    // The wand pedestal tile is owned by the legacy tile unit, whose type claims the
    // stone device of this family as well, so the stone device and the wand pedestal
    // block share one block entity type.

    public static final RegistrySupplier<BlockEntityType<TileFluxScrubber>> FLUX_SCRUBBER_TILE = BLOCK_ENTITIES.register("flux_scrubber",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileFluxScrubber(pos, state), STONE_DEVICE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileWardingStone>> WARDING_STONE_TILE = BLOCK_ENTITIES.register("warding_stone",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileWardingStone(pos, state), COSMETIC_SOLID.get(), COSMETIC_SOLID_TOUGH.get(), COSMETIC_SOLID_THAUMIUM.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileNode>> COSMETIC_NODE_TILE = BLOCK_ENTITIES.register("cosmetic_node",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileNode(cosmeticNodeType(), pos, state),
                    COSMETIC_SOLID.get(), COSMETIC_SOLID_TOUGH.get(), COSMETIC_SOLID_THAUMIUM.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileWardingStoneFence>> WARDING_FENCE_TILE = BLOCK_ENTITIES.register("warding_stone_fence",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileWardingStoneFence(pos, state), AIRY.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileEtherealBloom>> ETHEREAL_BLOOM_TILE = BLOCK_ENTITIES.register("ethereal_bloom",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileEtherealBloom(pos, state), ETHEREAL_BLOOM.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileJarBrain>> JAR_BRAIN_TILE = BLOCK_ENTITIES.register("jar_brain",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileJarBrain(pos, state), JAR_BRAIN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileJarNode>> JAR_NODE_TILE = BLOCK_ENTITIES.register("jar_node",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileJarNode(pos, state), JAR_NODE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileOwned>> OWNED_TILE = BLOCK_ENTITIES.register("owned",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileOwned(pos, state), ARCANE_DOOR.get(), COSMETIC_OPAQUE.get()).build(null));

    /** The block entity type of the node inside the cosmetic solid. */
    private static BlockEntityType<TileNode> cosmeticNodeType() { return COSMETIC_NODE_TILE.get(); }

    // -------------------------------------------------------------------- menus

    // The magic box menu is owned by the device menu unit, whose type the magic box
    // container and the client screen already read.

    private ThaumcraftEldritchBlocks() {}

    /** The original jar variants of the magic box family. */
    public static final class JarBlock extends BaseEntityBlock {
        public static final MapCodec<JarBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.intRange(0, 1).fieldOf("kind").forGetter(JarBlock::kind),
                propertiesCodec()).apply(instance, JarBlock::new));
        private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 12.0D, 13.0D);
        private final int kind;

        public JarBlock(int kind, Properties properties) {
            super(properties);
            this.kind = kind;
        }

        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        public int kind() { return kind; }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
        @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return kind == 0 ? new TileJarBrain(pos, state) : new TileJarNode(pos, state);
        }
    }

    /** The original ethereal bloom plant. */
    public static final class EtherealBloomBlock extends BaseEntityBlock {
        public static final MapCodec<EtherealBloomBlock> CODEC = simpleCodec(EtherealBloomBlock::new);
        private static final VoxelShape SHAPE = Block.box(1.6D, 0.0D, 1.6D, 14.4D, 12.8D, 14.4D);

        public EtherealBloomBlock(Properties properties) {
            super(properties);
        }

        @Override public MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TileEtherealBloom(pos, state);
        }
    }

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, ELDRITCH_ITEM, ELDRITCH_CRUST_ITEM, ELDRITCH_DECO_ITEM,
                ELDRITCH_DOORWAY_ITEM, ELDRITCH_SPAWNER_ITEM, ELDRITCH_CRYSTAL_ITEM,
                COSMETIC_SOLID_ITEM, COSMETIC_SOLID_TOUGH_ITEM, COSMETIC_SOLID_THAUMIUM_ITEM, COSMETIC_OPAQUE_ITEM,
                COSMETIC_STONE_SLAB_ARCANE_ITEM, COSMETIC_STONE_SLAB_ELDRITCH_ITEM,
                COSMETIC_WOOD_SLAB_GREATWOOD_ITEM, COSMETIC_WOOD_SLAB_SILVERWOOD_ITEM,
                ARCANE_DOOR_ITEM, MAGIC_BOX_ITEM, MIRROR_ITEM,
                METAL_DEVICE_ITEM, WOODEN_DEVICE_ITEM, STONE_DEVICE_ITEM,
                AIRY_ITEM, AIRY_NODE_ITEM, JAR_BRAIN_ITEM, JAR_NODE_ITEM, ETHEREAL_BLOOM_ITEM);
    }
}
