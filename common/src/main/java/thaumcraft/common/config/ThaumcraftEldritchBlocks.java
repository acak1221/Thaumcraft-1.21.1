package thaumcraft.common.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockAiry;
import thaumcraft.common.blocks.BlockArcaneDoor;
import thaumcraft.common.blocks.BlockArcanePressurePlate;
import thaumcraft.common.blocks.BlockCosmeticDoubleSlab;
import thaumcraft.common.blocks.BlockCosmeticOpaque;
import thaumcraft.common.blocks.BlockCosmeticStairs;
import thaumcraft.common.blocks.BlockCosmeticSolid;
import thaumcraft.common.blocks.BlockCosmeticStoneSlab;
import thaumcraft.common.blocks.BlockCosmeticWoodSlab;
import thaumcraft.common.blocks.BlockEldritch;
import thaumcraft.common.blocks.BlockJar;
import thaumcraft.common.blocks.BlockEldritchNothing;
import thaumcraft.common.blocks.BlockEldritchPortal;
import thaumcraft.common.blocks.BlockLoot;
import thaumcraft.common.blocks.BlockLootItem;
import thaumcraft.common.blocks.BlockMagicBox;
import thaumcraft.common.blocks.BlockMetalDevice;
import thaumcraft.common.blocks.BlockMirror;
import thaumcraft.common.blocks.BlockStoneDevice;
import thaumcraft.common.blocks.BlockWoodenDevice;
import thaumcraft.common.blocks.BannerData;
import thaumcraft.common.blocks.BlockArcanePressurePlate;
import thaumcraft.common.blocks.ItemArcaneDoor;
import thaumcraft.common.blocks.ItemMirrorBlock;
import thaumcraft.common.blocks.ItemVariantBlock;
import thaumcraft.common.container.ContainerMagicBox;
import thaumcraft.common.lib.CustomSoundType;
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
import thaumcraft.common.tiles.TileWandPedestal;
import thaumcraft.common.tiles.TileWardingStone;
import thaumcraft.common.tiles.TileWardingStoneFence;

/**
 * The original eldritch, cosmetic and device block families with their block
 * entities. Every value comes from the TC4 4.2.3.5 oracle and the pure rules
 * in {@link EldritchBlockRules}.
 *
 * <p>Minecraft 1.21.1 binds the hardness and the blast resistance to the block
 * and not to the block state, so the original metadata families that differ in
 * hardness become separate blocks of the same class. The cosmetic solid follows
 * the oracle: the metadata zero, one and eight are one block, the metadata four,
 * six and seven another one and the ten ordinary stones a third one.
 */
public final class ThaumcraftEldritchBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumcraft.MODID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Thaumcraft.MODID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumcraft.MODID, Registries.MENU);
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Thaumcraft.MODID, Registries.DATA_COMPONENT_TYPE);

    // ------------------------------------------------------------------ blocks

    /**
     * Original BlockMirror:68 - the custom sound type of the mirror: the sound
     * {@code thaumcraft:jar} with the volume 0.5 and the pitch 2.0.
     */
    public static final SoundType MIRROR_SOUND = new CustomSoundType("jar", 0.5F, 2.0F);
    /**
     * Original ConfigBlocks:302 - the custom sound type of the loot urn: the sound
     * {@code thaumcraft:urnbreak} with the volume 1.0 and the pitch 0.7.
     */
    public static final SoundType LOOT_URN_SOUND = new CustomSoundType("urnbreak", 1.0F, 0.7F);

    /** The eldritch altar, obelisk, obelisk middle and capstone. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH = BLOCKS.register("eldritch",
            () -> BlockEldritch.group(0, eldritchProperties(0)));
    /** The glowing crust and the glyphed eldritch stone. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH_CRUST = BLOCKS.register("eldritch_crust",
            () -> BlockEldritch.group(1, eldritchProperties(1)));
    /** The decorated eldritch stone. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH_DECO = BLOCKS.register("eldritch_deco",
            () -> BlockEldritch.group(2, eldritchProperties(2)));
    /** The eldritch doorway and the eldritch lock. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH_DOORWAY = BLOCKS.register("eldritch_doorway",
            () -> BlockEldritch.group(3, eldritchProperties(3)));
    /** The eldritch crab spawner and the runed eldritch trap. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH_SPAWNER = BLOCKS.register("eldritch_spawner",
            () -> BlockEldritch.group(4, eldritchProperties(4)));
    /** The eldritch crystal. */
    public static final RegistrySupplier<BlockEldritch> ELDRITCH_CRYSTAL = BLOCKS.register("eldritch_crystal",
            () -> BlockEldritch.group(5, eldritchProperties(5)));

    public static final RegistrySupplier<BlockEldritchNothing> ELDRITCH_NOTHING = BLOCKS.register("eldritch_nothing",
            () -> new BlockEldritchNothing(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    // Original BlockEldritchNothing:49 - setResistance(6000000) stores 18000000 and
                    // the getter divides it by five, so the effective blast resistance is 3600000.
                    .strength(-1.0F, EldritchBlockRules.ELDRITCH_NOTHING_RESISTANCE)
                    .sound(SoundType.STONE).noOcclusion().noCollission()
                    // Original BlockEldritchNothing:51 - setLightLevel(0.2) is the 1.7.10 setter that
                    // stores (int)(15 * 0.2) = 3, so the filler emits the light level three.
                    .lightLevel(state -> EldritchBlockRules.ELDRITCH_NOTHING_LIGHT)));

    public static final RegistrySupplier<BlockEldritchPortal> ELDRITCH_PORTAL = BLOCKS.register("eldritch_portal",
            () -> new BlockEldritchPortal(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA)
                    .strength(-1.0F, 200000.0F).sound(SoundType.STONE).noOcclusion().noCollission()
                    .lightLevel(state -> 15)));

    /**
     * The ordinary cosmetic stone. It hosts the original metadata 2, 3, 5, 9, 10, 11, 12, 13, 14
     * and 15; the metadata is the {@code variant} block state.
     */
    public static final RegistrySupplier<BlockCosmeticSolid> COSMETIC_SOLID = BLOCKS.register("cosmetic_solid",
            () -> BlockCosmeticSolid.group(0, cosmeticProperties(0)));
    /** The obsidian totem, the obsidian tile and the charged obsidian totem: the metadata 0, 1 and 8. */
    public static final RegistrySupplier<BlockCosmeticSolid> COSMETIC_SOLID_TOUGH = BLOCKS.register("cosmetic_solid_tough",
            () -> BlockCosmeticSolid.group(1, cosmeticProperties(1)));
    /** The thaumium block, the pedestal top and the arcane stone: the metadata 4, 6 and 7. */
    public static final RegistrySupplier<BlockCosmeticSolid> COSMETIC_SOLID_THAUMIUM = BLOCKS.register("cosmetic_solid_thaumium",
            () -> BlockCosmeticSolid.group(2, cosmeticProperties(2)));

    public static final RegistrySupplier<BlockCosmeticOpaque> COSMETIC_OPAQUE = BLOCKS.register("cosmetic_opaque",
            () -> BlockCosmeticOpaque.group(BlockCosmeticOpaque.GROUP_AMBER, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(EldritchBlockRules.COSMETIC_OPAQUE_HARDNESS, EldritchBlockRules.COSMETIC_OPAQUE_RESISTANCE)
                    .sound(SoundType.STONE)));
    /**
     * The warded glass, the original metadata two of the cosmetic opaque family. It is
     * unbreakable while the warded stone option is on and it resists every explosion.
     */
    public static final RegistrySupplier<BlockCosmeticOpaque> COSMETIC_OPAQUE_WARDED = BLOCKS.register("cosmetic_opaque_warded",
            () -> BlockCosmeticOpaque.group(BlockCosmeticOpaque.GROUP_WARDED, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(EldritchBlockRules.COSMETIC_OPAQUE_WARDED_HARDNESS, EldritchBlockRules.COSMETIC_OPAQUE_WARDED_RESISTANCE)
                    .sound(SoundType.STONE)));

    public static final RegistrySupplier<BlockCosmeticStoneSlab> COSMETIC_STONE_SLAB_ARCANE = BLOCKS.register("cosmetic_stone_slab_arcane",
            () -> new BlockCosmeticStoneSlab(EldritchBlockRules.SLAB_STONE_ARCANE,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.0F, EldritchBlockRules.COSMETIC_STONE_SLAB_RESISTANCE).sound(SoundType.STONE)));

    public static final RegistrySupplier<BlockCosmeticStoneSlab> COSMETIC_STONE_SLAB_ELDRITCH = BLOCKS.register("cosmetic_stone_slab_eldritch",
            () -> new BlockCosmeticStoneSlab(EldritchBlockRules.SLAB_STONE_ELDRITCH,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0F, EldritchBlockRules.COSMETIC_STONE_SLAB_RESISTANCE).sound(SoundType.STONE)));

    public static final RegistrySupplier<BlockCosmeticWoodSlab> COSMETIC_WOOD_SLAB_GREATWOOD = BLOCKS.register("cosmetic_wood_slab_greatwood",
            () -> new BlockCosmeticWoodSlab(EldritchBlockRules.SLAB_WOOD_GREATWOOD,
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, EldritchBlockRules.COSMETIC_WOOD_SLAB_RESISTANCE).sound(SoundType.WOOD)));

    public static final RegistrySupplier<BlockCosmeticWoodSlab> COSMETIC_WOOD_SLAB_SILVERWOOD = BLOCKS.register("cosmetic_wood_slab_silverwood",
            () -> new BlockCosmeticWoodSlab(EldritchBlockRules.SLAB_WOOD_SILVERWOOD,
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, EldritchBlockRules.COSMETIC_WOOD_SLAB_RESISTANCE).sound(SoundType.WOOD)));

    // The original blockCosmeticDoubleSlabWood and blockCosmeticDoubleSlabStone (ConfigBlocks:300-301): the double halves of the
    // four cosmetic slabs. The original registered two blocks with the metadata of their single slab; the native port keeps the
    // four double slabs in one block with the four metadata values, and the double half is the double value of the slab type.
    public static final RegistrySupplier<BlockCosmeticDoubleSlab> COSMETIC_DOUBLE_SLAB_WOOD_GREATWOOD = BLOCKS.register("cosmetic_double_slab_wood_greatwood",
            () -> new BlockCosmeticDoubleSlab(EldritchBlockRules.SLAB_DOUBLE_WOOD_GREATWOOD,
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, EldritchBlockRules.COSMETIC_WOOD_SLAB_RESISTANCE).sound(SoundType.WOOD)));

    public static final RegistrySupplier<BlockCosmeticDoubleSlab> COSMETIC_DOUBLE_SLAB_WOOD_SILVERWOOD = BLOCKS.register("cosmetic_double_slab_wood_silverwood",
            () -> new BlockCosmeticDoubleSlab(EldritchBlockRules.SLAB_DOUBLE_WOOD_SILVERWOOD,
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, EldritchBlockRules.COSMETIC_WOOD_SLAB_RESISTANCE).sound(SoundType.WOOD)));

    public static final RegistrySupplier<BlockCosmeticDoubleSlab> COSMETIC_DOUBLE_SLAB_STONE_ARCANE = BLOCKS.register("cosmetic_double_slab_stone_arcane",
            () -> new BlockCosmeticDoubleSlab(EldritchBlockRules.SLAB_DOUBLE_STONE_ARCANE,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.0F, EldritchBlockRules.COSMETIC_STONE_SLAB_RESISTANCE).sound(SoundType.STONE)));

    public static final RegistrySupplier<BlockCosmeticDoubleSlab> COSMETIC_DOUBLE_SLAB_STONE_ELDRITCH = BLOCKS.register("cosmetic_double_slab_stone_eldritch",
            () -> new BlockCosmeticDoubleSlab(EldritchBlockRules.SLAB_DOUBLE_STONE_ELDRITCH,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0F, EldritchBlockRules.COSMETIC_STONE_SLAB_RESISTANCE).sound(SoundType.STONE)));

    public static final RegistrySupplier<BlockArcaneDoor> ARCANE_DOOR = BLOCKS.register("arcane_door",
            () -> new BlockArcaneDoor(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    // Original BlockArcaneDoor:68 - setResistance(999) stores 2997 and the getter
                    // divides it by five, so the effective blast resistance is 599.4.
                    .strength(15.0F, 599.4F).sound(SoundType.METAL).noOcclusion()));

    public static final RegistrySupplier<BlockMagicBox> MAGIC_BOX = BLOCKS.register("magic_box",
            () -> new BlockMagicBox(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    // Original BlockMagicBox:34-38 - hardness 2.5 and NO setResistance at all, so
                    // the inherited 1.7.10 field stays 0 and the effective blast resistance is 0.
                    .strength(2.5F, 0.0F).sound(SoundType.WOOD).noOcclusion()));

    public static final RegistrySupplier<BlockMirror> MIRROR = BLOCKS.register("mirror",
            () -> new BlockMirror(BlockBehaviour.Properties.of().mapColor(MapColor.ICE)
                    // Original BlockMirror:67 - setResistance(10) stores 30 and the getter divides
                    // it by five, so the effective blast resistance is 6.
                    // Original BlockMirror:68 - the mirror plays the custom sound "thaumcraft:jar"
                    // with the volume 0.5 and the pitch 2.0 (CustomStepSound), not the vanilla
                    // glass sound.
                    .strength(1.0F, 6.0F).sound(MIRROR_SOUND).noOcclusion()));

    public static final RegistrySupplier<BlockMetalDevice> METAL_DEVICE = BLOCKS.register("metal_device",
            () -> new BlockMetalDevice(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .strength(EldritchBlockRules.DEVICE_METAL_HARDNESS, EldritchBlockRules.DEVICE_METAL_RESISTANCE)
                    .sound(SoundType.METAL).noOcclusion()));

    /**
     * The arcane pressure plate, the original BlockWoodenDevice metadata two and three. The
     * original gave it a hardness of two (none at all with the warded stone option) and the
     * absolute blast resistance 999, and Minecraft 1.21.1 binds both to the block, so the plate
     * is a block of its own (oracle BlockWoodenDevice:196-212).
     */
    public static final RegistrySupplier<BlockArcanePressurePlate> ARCANE_PRESSURE_PLATE = BLOCKS.register("arcane_pressure_plate",
            () -> new BlockArcanePressurePlate(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE_HARDNESS, EldritchBlockRules.DEVICE_WOOD_PRESSURE_PLATE_RESISTANCE)
                    .sound(SoundType.WOOD).noOcclusion()));

    public static final RegistrySupplier<BlockWoodenDevice> WOODEN_DEVICE = BLOCKS.register("wooden_device",
            () -> new BlockWoodenDevice(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(EldritchBlockRules.DEVICE_WOOD_HARDNESS, EldritchBlockRules.DEVICE_WOOD_RESISTANCE)
                    .sound(SoundType.WOOD).noOcclusion()));

    public static final RegistrySupplier<BlockStoneDevice> STONE_DEVICE = BLOCKS.register("stone_device",
            () -> new BlockStoneDevice(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(EldritchBlockRules.DEVICE_STONE_HARDNESS, EldritchBlockRules.DEVICE_STONE_RESISTANCE)
                    .sound(SoundType.STONE).noOcclusion()));

    public static final RegistrySupplier<BlockLoot> LOOT_URN = BLOCKS.register("loot_urn",
            // Original ConfigBlocks:302 - the loot urn plays the custom sound "thaumcraft:urnbreak"
            // with the volume 1.0 and the pitch 0.7 (CustomStepSound), not the vanilla stone sound.
            () -> new BlockLoot(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.15F, 0.0F)
                    .sound(LOOT_URN_SOUND).noOcclusion()));

    public static final RegistrySupplier<BlockLoot> LOOT_CRATE = BLOCKS.register("loot_crate",
            () -> new BlockLoot(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.15F, 0.0F).sound(SoundType.WOOD).noOcclusion()));

    /** The ordinary airy blocks. */
    public static final RegistrySupplier<BlockAiry> AIRY = BLOCKS.register("airy",
            () -> BlockAiry.group(0, airyProperties(0)));
    /** The node and the energised node. */
    public static final RegistrySupplier<BlockAiry> AIRY_NODE = BLOCKS.register("airy_node",
            () -> BlockAiry.group(1, airyProperties(1)));
    /** The magic and the void hole. */
    public static final RegistrySupplier<BlockAiry> AIRY_HOLE = BLOCKS.register("airy_hole",
            () -> BlockAiry.group(2, airyProperties(2)));
    /** The unbreakable airy block. */
    public static final RegistrySupplier<BlockAiry> AIRY_UNBREAKABLE = BLOCKS.register("airy_unbreakable",
            () -> BlockAiry.group(3, airyProperties(3)));

    public static final RegistrySupplier<JarBlock> JAR_BRAIN = BLOCKS.register("jar_brain",
            // Original BlockJar:87-90 - the jar block sets hardness 0.3, no blast resistance and
            // the light level 0.66; the 1.7.10 setter stores (int)(15*0.66) = 9. Every metadata
            // value of the original jar block, the brain jar and the node jar included, emits 9.
            () -> new JarBlock(0, BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F, 0.0F).sound(SoundType.GLASS).noOcclusion().lightLevel(state -> BlockJar.LIGHT)));

    public static final RegistrySupplier<JarBlock> JAR_NODE = BLOCKS.register("jar_node",
            // Original BlockJar:87-90 - the node jar is the same block as the ordinary jar and
            // emits the same light level 9. The original has no per metadata light override.
            () -> new JarBlock(1, BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F, 0.0F).sound(SoundType.GLASS).noOcclusion().lightLevel(state -> BlockJar.LIGHT)));

    public static final RegistrySupplier<EtherealBloomBlock> ETHEREAL_BLOOM = BLOCKS.register("ethereal_bloom",
            () -> new EtherealBloomBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
                    .noCollission().sound(SoundType.GRASS).lightLevel(state -> 15)));

    /**
     * The native properties of a block of the eldritch family.
     *
     * <p>The hardness, the blast resistance and the light level come from the oracle rules of
     * the original metadata family, so every native block of a group carries the values of
     * the metadata it hosts.
     *
     * @param group the hardness group of the native block
     */
    public static BlockBehaviour.Properties eldritchProperties(int group) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                .strength(EldritchBlockRules.eldritchGroupHardness(group), EldritchBlockRules.eldritchGroupResistance(group))
                .sound(SoundType.STONE).noOcclusion()
                .lightLevel(state -> EldritchBlockRules.eldritchLight(BlockEldritch.variantOf(state, group)));
    }

    private static BlockBehaviour.Properties cosmeticProperties(int group) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                .strength(EldritchBlockRules.cosmeticGroupHardness(group), EldritchBlockRules.cosmeticGroupResistance(group))
                .sound(SoundType.STONE).noOcclusion()
                .lightLevel(state -> EldritchBlockRules.cosmeticLight(BlockCosmeticSolid.variantOf(state, group)));
    }

    private static BlockBehaviour.Properties airyProperties(int group) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
                .strength(EldritchBlockRules.airyGroupHardness(group), EldritchBlockRules.airyGroupResistance(group))
                .sound(SoundType.WOOL).noOcclusion().noCollission()
                .lightLevel(state -> EldritchBlockRules.airyLight(BlockAiry.variantOf(state, group)));
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
    /** The paving stone of travel is the metadata two of the cosmetic solid (ConfigRecipes:209). */
    public static final RegistrySupplier<Item> PAVING_STONE_TRAVEL_ITEM = ITEMS.register("paving_stone_travel",
            () -> new ItemVariantBlock(COSMETIC_SOLID.get(), EldritchBlockRules.variantOf("paving_stone_travel"), "paving_stone_travel", new Item.Properties()));
    /** The paving stone of warding is the metadata three of the cosmetic solid (ConfigRecipes:212). */
    public static final RegistrySupplier<Item> PAVING_STONE_WARDING_ITEM = ITEMS.register("paving_stone_warding",
            () -> new ItemVariantBlock(COSMETIC_SOLID.get(), EldritchBlockRules.variantOf("paving_stone_warding"), "paving_stone_warding", new Item.Properties()));
    /** The warded glass is the metadata two of the cosmetic opaque (ConfigRecipes:192). */
    public static final RegistrySupplier<Item> WARDED_GLASS_ITEM = ITEMS.register("warded_glass",
            () -> new ItemVariantBlock(COSMETIC_OPAQUE_WARDED.get(), EldritchBlockRules.variantOf("warded_glass"), "warded_glass", new Item.Properties()));
    /**
     * The obsidian tile is the metadata one of the cosmetic solid: the original language file names it
     * "tile.blockCosmeticSolid.1.name" = "Obsidian Tile", and the original makes four of them out of four
     * obsidian (oracle ConfigRecipes:449). A 1.21.1 recipe result cannot carry a block state, so the
     * variant of the cosmetic solid family needs an item of its own.
     */
    public static final RegistrySupplier<Item> OBSIDIAN_TILE_ITEM = ITEMS.register("obsidian_tile",
            () -> new ItemVariantBlock(COSMETIC_SOLID_TOUGH.get(), EldritchBlockRules.variantOf("obsidian_tile"), "obsidian_tile", new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_OPAQUE_ITEM = ITEMS.register("cosmetic_opaque", () -> new BlockItem(COSMETIC_OPAQUE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_STONE_SLAB_ARCANE_ITEM = ITEMS.register("cosmetic_stone_slab_arcane", () -> new BlockItem(COSMETIC_STONE_SLAB_ARCANE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_STONE_SLAB_ELDRITCH_ITEM = ITEMS.register("cosmetic_stone_slab_eldritch", () -> new BlockItem(COSMETIC_STONE_SLAB_ELDRITCH.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_WOOD_SLAB_GREATWOOD_ITEM = ITEMS.register("cosmetic_wood_slab_greatwood", () -> new BlockItem(COSMETIC_WOOD_SLAB_GREATWOOD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_WOOD_SLAB_SILVERWOOD_ITEM = ITEMS.register("cosmetic_wood_slab_silverwood", () -> new BlockItem(COSMETIC_WOOD_SLAB_SILVERWOOD.get(), new Item.Properties()));
    // The original never offered the double slabs in the creative tab (BlockCosmeticWoodSlab:68-74).
    public static final RegistrySupplier<Item> COSMETIC_DOUBLE_SLAB_WOOD_GREATWOOD_ITEM = ITEMS.register("cosmetic_double_slab_wood_greatwood", () -> new BlockItem(COSMETIC_DOUBLE_SLAB_WOOD_GREATWOOD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_DOUBLE_SLAB_WOOD_SILVERWOOD_ITEM = ITEMS.register("cosmetic_double_slab_wood_silverwood", () -> new BlockItem(COSMETIC_DOUBLE_SLAB_WOOD_SILVERWOOD.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_DOUBLE_SLAB_STONE_ARCANE_ITEM = ITEMS.register("cosmetic_double_slab_stone_arcane", () -> new BlockItem(COSMETIC_DOUBLE_SLAB_STONE_ARCANE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> COSMETIC_DOUBLE_SLAB_STONE_ELDRITCH_ITEM = ITEMS.register("cosmetic_double_slab_stone_eldritch", () -> new BlockItem(COSMETIC_DOUBLE_SLAB_STONE_ELDRITCH.get(), new Item.Properties()));
    /**
     * The flux scrubber is the original blockStoneDevice metadata fourteen (ConfigRecipes:194). A 1.21.1 recipe result cannot carry a
     * block state, so the variant of the stone device family needs an item of its own.
     */
    public static final RegistrySupplier<Item> FLUX_SCRUBBER_ITEM = ITEMS.register("flux_scrubber",
            () -> new ItemVariantBlock(STONE_DEVICE.get(), EldritchBlockRules.DEVICE_STONE_FLUX_SCRUBBER, "flux_scrubber", new Item.Properties()));
    /**
     * The arcane pressure plate is the original blockWoodenDevice metadata two (ConfigRecipes:197). A 1.21.1 recipe result cannot carry
     * a block state, so the variant of the wooden device family needs an item of its own.
     */
    public static final RegistrySupplier<Item> ARCANE_PRESSURE_PLATE_ITEM = ITEMS.register("arcane_pressure_plate",
            () -> new BlockItem(ARCANE_PRESSURE_PLATE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> ARCANE_DOOR_ITEM = ITEMS.register("arcane_door", () -> new ItemArcaneDoor(new Item.Properties()));
    public static final RegistrySupplier<Item> MAGIC_BOX_ITEM = ITEMS.register("magic_box", () -> new BlockItem(MAGIC_BOX.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> MIRROR_ITEM = ITEMS.register("mirror", () -> new ItemMirrorBlock(MIRROR.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> METAL_DEVICE_ITEM = ITEMS.register("metal_device", () -> new BlockItem(METAL_DEVICE.get(), new Item.Properties()));
    public static final RegistrySupplier<Item> WOODEN_DEVICE_ITEM = ITEMS.register("wooden_device", () -> new BlockItem(WOODEN_DEVICE.get(), new Item.Properties()));
    /**
     * The banner of the original blockWoodenDevice metadata eight. The original kept it in one item
     * with the NBT byte "color" and the NBT string "aspect" (ConfigRecipes:185-188), the native item
     * carries both in one data component.
     */
    public static final RegistrySupplier<DataComponentType<BannerData>> BANNER_DATA = COMPONENTS.register("banner_data",
            () -> DataComponentType.<BannerData>builder().persistent(BannerData.CODEC).networkSynchronized(BannerData.STREAM_CODEC).build());
    /** The banner item of the original: it places the wooden device with its banner metadata. */
    public static final RegistrySupplier<Item> BANNER_ITEM = ITEMS.register("banner",
            () -> new ItemVariantBlock(WOODEN_DEVICE.get(), EldritchBlockRules.DEVICE_WOOD_BANNER, "banner", new Item.Properties()));
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
            () -> BlockEntityType.Builder.of((pos, state) -> new TileArcanePressurePlate(pos, state), ARCANE_PRESSURE_PLATE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileBrainbox>> BRAINBOX_TILE = BLOCK_ENTITIES.register("brainbox",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileBrainbox(pos, state), METAL_DEVICE.get()).build(null));

    /** The metal grate tile is owned by ThaumcraftGrate (the upstream item grate unit). */

    /**
     * The wand pedestal of the stone device, the original blockStoneDevice metadata five. The id of
     * the separate wand pedestal block tile belongs to ThaumcraftLegacyTiles, so this one carries the
     * name of its block family.
     */
    public static final RegistrySupplier<BlockEntityType<TileWandPedestal>> WAND_PEDESTAL_TILE = BLOCK_ENTITIES.register("stone_device_wand_pedestal",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileWandPedestal(pos, state), STONE_DEVICE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileFluxScrubber>> FLUX_SCRUBBER_TILE = BLOCK_ENTITIES.register("flux_scrubber",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileFluxScrubber(pos, state), STONE_DEVICE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileWardingStone>> WARDING_STONE_TILE = BLOCK_ENTITIES.register("warding_stone",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileWardingStone(pos, state), COSMETIC_SOLID.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileNode>> COSMETIC_NODE_TILE = BLOCK_ENTITIES.register("cosmetic_node",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileNode(cosmeticNodeType(), pos, state),
                    COSMETIC_SOLID_TOUGH.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileWardingStoneFence>> WARDING_FENCE_TILE = BLOCK_ENTITIES.register("warding_stone_fence",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileWardingStoneFence(pos, state), AIRY.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileEtherealBloom>> ETHEREAL_BLOOM_TILE = BLOCK_ENTITIES.register("ethereal_bloom",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileEtherealBloom(pos, state), ETHEREAL_BLOOM.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileJarBrain>> JAR_BRAIN_TILE = BLOCK_ENTITIES.register("jar_brain",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileJarBrain(pos, state), JAR_BRAIN.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileJarNode>> JAR_NODE_TILE = BLOCK_ENTITIES.register("jar_node",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileJarNode(pos, state), JAR_NODE.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<TileOwned>> OWNED_TILE = BLOCK_ENTITIES.register("owned",
            () -> BlockEntityType.Builder.of((pos, state) -> new TileOwned(pos, state), ARCANE_DOOR.get(), COSMETIC_OPAQUE.get(), COSMETIC_OPAQUE_WARDED.get()).build(null));

    /** The block entity type of the node inside the cosmetic solid. */
    private static BlockEntityType<TileNode> cosmeticNodeType() { return COSMETIC_NODE_TILE.get(); }

    // -------------------------------------------------------------------- menus

    /** The eldritch stairs of the original: the cosmetic stairs over the cosmetic solid metadata 11. */
    public static final RegistrySupplier<BlockCosmeticStairs> STAIRS_ELDRITCH = BLOCKS.register("eldritch_stairs",
            () -> new BlockCosmeticStairs(thaumcraft.common.lib.world.FamilyVariant.with(COSMETIC_SOLID.get().defaultBlockState(), 11),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                            // The 1.7.10 BlockStairs copies the hardness and the blast resistance of
                            // the block it copies the texture of: the cosmetic solid family, the
                            // hardness two and the effective resistance six of the setter value ten.
                            .strength(2.0F, EldritchBlockRules.COSMETIC_FAMILY_NATIVE_RESISTANCE).sound(SoundType.STONE)));

    public static final RegistrySupplier<Item> STAIRS_ELDRITCH_ITEM = ITEMS.register("eldritch_stairs",
            () -> new BlockItem(STAIRS_ELDRITCH.get(), new Item.Properties()));

    private ThaumcraftEldritchBlocks() {}

    /** The original jar variants of the magic box family. */
    public static final class JarBlock extends BaseEntityBlock {
        public static final MapCodec<JarBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.intRange(0, 1).fieldOf("kind").forGetter(JarBlock::kind),
                propertiesCodec()).apply(instance, JarBlock::new));
        private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 12.0D, 13.0D);
        private static final SoundEvent JAR = SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath("thaumcraft", "jar"));
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

        /**
         * Original BlockJar:223-242 - a right click on the brain jar hands a random
         * part of the stored experience back and pauses the jar for forty ticks.
         */
        @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                            Player player, InteractionHand hand, BlockHitResult hit) {
            if (kind != 0 || !emptyBrainJar(level, pos)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            return ItemInteractionResult.CONSUME;
        }

        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (kind != 0 || !emptyBrainJar(level, pos)) return InteractionResult.PASS;
            return InteractionResult.CONSUME;
        }

        /** The original hands back a random share of at most sixty four experience. */
        private static boolean emptyBrainJar(Level level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof TileJarBrain jar)) return false;
            jar.eatDelay = 40;
            if (level.isClientSide) return true;
            int amount = level.random.nextInt(Math.min(jar.getXp() + 1, 64));
            if (amount > 0) {
                jar.xp -= amount;
                dropExperience(level, pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D, amount);
                jar.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
            level.playSound(null, pos, JAR, SoundSource.BLOCKS, .2F, 1F);
            return true;
        }

        /** Original BlockJar:182-193 - breaking the brain jar drops the stored experience. */
        @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
            if (!state.is(newState.getBlock()) && !level.isClientSide
                    && level.getBlockEntity(pos) instanceof TileJarBrain jar) {
                dropExperience(level, pos.getX(), pos.getY(), pos.getZ(), jar.getXp());
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }

        /**
         * Original BlockJar:184-193 - the node jar drops the node it holds: the aspects, the node
         * type, the node modifier and the identity of the node travel with the dropped item.
         *
         * <p>The original dropped the {@code itemJarNode} item with its NBT; the native node jar
         * is a block, so the dropped block item carries the block entity data of the tile. The
         * vanilla {@code applyComponentsFromItemStack} reads it back when the item is placed.</p>
         */
        @Override protected java.util.List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            if (kind != 1) return super.getDrops(state, params);
            ItemStack drop = new ItemStack(state.getBlock());
            BlockEntity tile = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (tile instanceof TileJarNode node && node.drop) {
                node.saveToItem(drop, params.getLevel().registryAccess());
            }
            return java.util.List.of(drop);
        }

        /** The original splits the experience into orbs of the vanilla orb value. */
        private static void dropExperience(Level level, double x, double y, double z, int amount) {
            int remaining = amount;
            while (remaining > 0) {
                int value = ExperienceOrb.getExperienceValue(remaining);
                remaining -= value;
                level.addFreshEntity(new ExperienceOrb(level, x, y, z, value));
            }
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

    private static boolean registered;

    public static void register() {
        // The Outer Lands registration calls this class as well, so the call must be idempotent.
        if (registered) return;
        registered = true;
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        MENUS.register();
        COMPONENTS.register();
        CreativeTabRegistry.append(ThaumcraftContent.TAB, ELDRITCH_ITEM, ELDRITCH_CRUST_ITEM, ELDRITCH_DECO_ITEM,
                ELDRITCH_DOORWAY_ITEM, ELDRITCH_SPAWNER_ITEM, ELDRITCH_CRYSTAL_ITEM,
                COSMETIC_SOLID_ITEM, COSMETIC_SOLID_TOUGH_ITEM, COSMETIC_SOLID_THAUMIUM_ITEM,
                PAVING_STONE_TRAVEL_ITEM, PAVING_STONE_WARDING_ITEM, WARDED_GLASS_ITEM, OBSIDIAN_TILE_ITEM,
                COSMETIC_OPAQUE_ITEM,
                COSMETIC_STONE_SLAB_ARCANE_ITEM, COSMETIC_STONE_SLAB_ELDRITCH_ITEM,
                COSMETIC_WOOD_SLAB_GREATWOOD_ITEM, COSMETIC_WOOD_SLAB_SILVERWOOD_ITEM,
                FLUX_SCRUBBER_ITEM, ARCANE_PRESSURE_PLATE_ITEM,
                STAIRS_ELDRITCH_ITEM, ARCANE_DOOR_ITEM, MAGIC_BOX_ITEM, MIRROR_ITEM,
                METAL_DEVICE_ITEM, WOODEN_DEVICE_ITEM, STONE_DEVICE_ITEM, BANNER_ITEM,
                AIRY_ITEM, AIRY_NODE_ITEM, JAR_BRAIN_ITEM, JAR_NODE_ITEM, ETHEREAL_BLOOM_ITEM);
        // The original listed the sixteen coloured banners in the creative tab (BlockWoodenDevice:117-125).
    }
}
