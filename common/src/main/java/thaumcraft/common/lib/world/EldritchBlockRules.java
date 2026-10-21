package thaumcraft.common.lib.world;

import java.util.Map;
import java.util.Random;

/**
 * Pure decision rules for the original TC4 4.2.3.5 eldritch, cosmetic and
 * device block families (oracle {@code thaumcraft/common/blocks} and
 * {@code thaumcraft/common/tiles}).
 *
 * <p>Every number below is taken verbatim from the 1.7.10 oracle. The block and
 * block entity classes only call these methods, so the decision logic stays
 * unit testable without a world, a level or a registry.
 */
public final class EldritchBlockRules {
    private EldritchBlockRules() {}

    /**
     * BlockEldritchNothing:51 - the original calls {@code setLightLevel(0.2)}, the 1.7.10 setter
     * that stores {@code (int)(15 * 0.2)} = 3, so the blank filler emits the light level three.
     */
    public static final int ELDRITCH_NOTHING_LIGHT = 3;

    /** BlockEldritchNothing:49 - the setter value 6000000 is the effective 3600000. */
    public static final float ELDRITCH_NOTHING_RESISTANCE = 3600000.0F;

    // ------------------------------------------------------------------ eldritch

    /** BlockEldritch metadata 0..10, in the original order. */
    public static final int ELDRITCH_ALTAR = 0;
    public static final int ELDRITCH_OBELISK = 1;
    public static final int ELDRITCH_OBELISK_MIDDLE = 2;
    public static final int ELDRITCH_CAPSTONE = 3;
    public static final int ELDRITCH_GLOWING_CRUST = 4;
    public static final int ELDRITCH_GLYPHED = 5;
    public static final int ELDRITCH_DECO = 6;
    public static final int ELDRITCH_DOORWAY = 7;
    public static final int ELDRITCH_LOCK = 8;
    public static final int ELDRITCH_CRAB_SPAWNER = 9;
    public static final int ELDRITCH_RUNED = 10;
    /**
     * The original eldritch crystal was a metadata of the separate crystal
     * block. It is hosted by the eldritch family here, because the crystal
     * block itself is already ported with its own crystal clusters.
     */
    public static final int ELDRITCH_CRYSTAL = 11;

    /**
     * The original ItemEldritchObject metadata two, the runed tablet.
     *
     * <p>Oracle {@code ItemEldritchObject:31-48} carried the five metadata values zero to
     * four on one item. The 1.21.1 port flattens them into one item each, so the metadata two
     * is the runed tablet, the key of the eldritch lock and the permanent item of the key room.
     */
    public static final int ELDRITCH_OBJECT_TABLET = 2;

    /** BlockEldritch:72-75 - family hardness 50 and the blast resistance setter value 20000. */
    public static final float ELDRITCH_FAMILY_HARDNESS = 50.0F;
    public static final float ELDRITCH_FAMILY_RESISTANCE = 20000.0F;
    /**
     * The blast resistance of the family in native units. The 1.7.10 setter value 20000 is
     * stored as 60000 and the vanilla getter divides the stored value by five, so the
     * effective value is 12000.
     */
    public static final float ELDRITCH_FAMILY_NATIVE_RESISTANCE = 12000.0F;

    /** BlockEldritch:120-135 - the per metadata light level. */
    public static int eldritchLight(int variant) {
        switch (variant) {
            case ELDRITCH_GLOWING_CRUST:
            case ELDRITCH_GLYPHED:
            case ELDRITCH_DOORWAY:
                return 12;
            case ELDRITCH_DECO:
            case ELDRITCH_LOCK:
                return 5;
            case ELDRITCH_CRAB_SPAWNER:
                return 4;
            case ELDRITCH_RUNED:
                return 0;
            default:
                return 8;
        }
    }

    /** BlockEldritch:231-246 - the per metadata hardness, -1 means unbreakable. */
    public static float eldritchHardness(int variant) {
        switch (variant) {
            case ELDRITCH_GLOWING_CRUST:
            case ELDRITCH_GLYPHED:
                return 2.0F;
            case ELDRITCH_DECO:
                return 4.0F;
            case ELDRITCH_DOORWAY:
            case ELDRITCH_LOCK:
                return -1.0F;
            case ELDRITCH_CRAB_SPAWNER:
            case ELDRITCH_RUNED:
                return 15.0F;
            default:
                return ELDRITCH_FAMILY_HARDNESS;
        }
    }

    /** BlockEldritch:248-260 - the per metadata blast resistance. */
    public static float eldritchExplosionResistance(int variant) {
        switch (variant) {
            case ELDRITCH_GLOWING_CRUST:
            case ELDRITCH_GLYPHED:
            case ELDRITCH_CRAB_SPAWNER:
            case ELDRITCH_RUNED:
                return 30.0F;
            case ELDRITCH_DECO:
                return 100.0F;
            case ELDRITCH_DOORWAY:
            case ELDRITCH_LOCK:
                return Float.MAX_VALUE;
            default:
                return ELDRITCH_FAMILY_NATIVE_RESISTANCE;
        }
    }

    /**
     * Minecraft 1.21.1 binds the hardness and the blast resistance to the block
     * and not to the block state. The original metadata therefore became a
     * small family of blocks; these helpers map a metadata to its group.
     */
    public static final int[] ELDRITCH_GROUPS = {0, 0, 0, 0, 1, 1, 2, 3, 3, 4, 4, 5};
    public static final float[] ELDRITCH_GROUP_HARDNESS = {50.0F, 2.0F, 4.0F, -1.0F, 15.0F, 50.0F};
    public static final float[] ELDRITCH_GROUP_RESISTANCE = {12000.0F, 30.0F, 100.0F, Float.MAX_VALUE, 30.0F, 12000.0F};

    /** The hardness group of an eldritch metadata. */
    public static int eldritchGroup(int variant) {
        return variant >= 0 && variant < ELDRITCH_GROUPS.length ? ELDRITCH_GROUPS[variant] : 0;
    }

    /** The hardness of an eldritch group. */
    public static float eldritchGroupHardness(int group) { return ELDRITCH_GROUP_HARDNESS[group]; }

    /** The blast resistance of an eldritch group. */
    public static float eldritchGroupResistance(int group) { return ELDRITCH_GROUP_RESISTANCE[group]; }

    /** The metadata values of an eldritch group. */
    public static java.util.List<Integer> eldritchGroupVariants(int group) {
        java.util.List<Integer> variants = new java.util.ArrayList<>();
        for (int variant = 0; variant < ELDRITCH_GROUPS.length; variant++) {
            if (ELDRITCH_GROUPS[variant] == group) variants.add(variant);
        }
        return variants;
    }

    /** BlockEldritch:151-153 - only these metadata values carry a block entity. */
    public static boolean eldritchHasTile(int variant) {
        return variant == ELDRITCH_ALTAR || variant == ELDRITCH_OBELISK || variant == ELDRITCH_CAPSTONE
                || variant == ELDRITCH_LOCK || variant == ELDRITCH_CRAB_SPAWNER || variant == ELDRITCH_RUNED
                || variant == ELDRITCH_CRYSTAL;
    }

    /** BlockEldritch:189-191 - metadata 4 drops itself, metadata 5 drops a resource. */
    public static boolean eldritchDropsSelf(int variant) {
        return variant == ELDRITCH_GLOWING_CRUST;
    }

    /** BlockEldritch:207-214 - metadata 5 drops the knowledge fragment resource. */
    public static boolean eldritchDropsResource(int variant) {
        return variant == ELDRITCH_GLYPHED;
    }

    /**
     * BlockCrystal:149-151 - the eldritch crystal, the original metadata seven of the
     * crystal block, drops one balanced shard (the original itemShard metadata six).
     */
    public static boolean eldritchDropsBalancedShard(int variant) {
        return variant == ELDRITCH_CRYSTAL;
    }

    /** BlockEldritch:197-205 - metadata 5/10 drop 1..4 xp, metadata 9 drops 6..10 xp. */
    public static int eldritchExperience(int variant, Random random) {
        if (variant == ELDRITCH_GLYPHED || variant == ELDRITCH_RUNED) {
            return 1 + random.nextInt(4);
        }
        if (variant == ELDRITCH_CRAB_SPAWNER) {
            return 6 + random.nextInt(5);
        }
        return 0;
    }

    /** BlockEldritch:216-229 - breaking a structural piece removes the whole altar. */
    public static boolean eldritchBreaksStructure(int variant) {
        return variant < ELDRITCH_GLOWING_CRUST;
    }

    /** BlockEldritch:218-220 - the structure removal window around a broken piece. */
    public static int eldritchStructureRadiusX() {
        return 3;
    }

    public static int eldritchStructureRadiusY() {
        return 2;
    }

    /** BlockEldritch:221 - only pieces below the glowing crust are removed. */
    public static boolean eldritchStructureMember(int variant) {
        return variant < ELDRITCH_GLOWING_CRUST;
    }

    // -------------------------------------------------------------- cosmetic solid

    /** BlockCosmeticSolid metadata 0..15, only the ones with a real meaning. */
    public static final int COSMETIC_OBSIDIAN_TILE = 0;
    public static final int COSMETIC_OBSIDIAN_TOTEM_BASE = 1;
    public static final int COSMETIC_TRAVEL_STONE = 2;
    public static final int COSMETIC_WARDING_STONE = 3;
    public static final int COSMETIC_THAUMIUM_BLOCK = 4;
    public static final int COSMETIC_TALLOW_BLOCK = 5;
    public static final int COSMETIC_PEDESTAL_TOP = 6;
    public static final int COSMETIC_ARCANE_STONE = 7;
    public static final int COSMETIC_OBSIDIAN_TILE_SHADED = 8;
    public static final int COSMETIC_GOLEM_STONE = 9;
    public static final int COSMETIC_GOLEM_STONE_ACTIVE = 10;
    public static final int COSMETIC_ELDRITCH_TILE = 11;
    public static final int COSMETIC_ELDRITCH_RUNE = 12;
    public static final int COSMETIC_ELDRITCH_TILE_ALT = 13;
    public static final int COSMETIC_TAINT_CRUST = 14;
    public static final int COSMETIC_ELDRITCH_PILLAR = 15;

    /** BlockCosmeticSolid:66-67 - family hardness 2 and the blast resistance setter value 10. */
    public static final float COSMETIC_FAMILY_HARDNESS = 2.0F;
    public static final float COSMETIC_FAMILY_RESISTANCE = 10.0F;
    /**
     * The blast resistance of the family in native units. The 1.7.10 setter value 10 is stored as
     * 30 and the vanilla getter divides the stored value by five, so the effective value is 6.
     */
    public static final float COSMETIC_FAMILY_NATIVE_RESISTANCE = 6.0F;

    /** BlockCosmeticSolid:213-225 - the per metadata hardness, -1 means unbreakable. */
    public static float cosmeticHardness(int variant) {
        switch (variant) {
            case COSMETIC_OBSIDIAN_TILE:
            case COSMETIC_OBSIDIAN_TOTEM_BASE:
            case COSMETIC_OBSIDIAN_TILE_SHADED:
                return 30.0F;
            case COSMETIC_THAUMIUM_BLOCK:
            case COSMETIC_PEDESTAL_TOP:
            case COSMETIC_ARCANE_STONE:
                return 4.0F;
            default:
                return COSMETIC_FAMILY_HARDNESS;
        }
    }

    /** BlockCosmeticSolid:227-239 - the per metadata light level. */
    public static int cosmeticLight(int variant) {
        if (variant == COSMETIC_TRAVEL_STONE) return 9;
        if (variant == COSMETIC_TAINT_CRUST) return 4;
        return 0;
    }

    /** BlockCosmeticSolid:241-253 - the per metadata blast resistance. */
    public static float cosmeticExplosionResistance(int variant) {
        switch (variant) {
            case COSMETIC_OBSIDIAN_TILE:
            case COSMETIC_OBSIDIAN_TOTEM_BASE:
            case COSMETIC_OBSIDIAN_TILE_SHADED:
                return 999.0F;
            case COSMETIC_THAUMIUM_BLOCK:
            case COSMETIC_PEDESTAL_TOP:
            case COSMETIC_ARCANE_STONE:
                return 20.0F;
            default:
                return COSMETIC_FAMILY_NATIVE_RESISTANCE;
        }
    }

    /** BlockCosmeticSolid:190-193 - travel stone, warding stone and the runed tile never host mobs. */
    public static boolean cosmeticBlocksSpawning(int variant) {
        return variant == COSMETIC_TRAVEL_STONE || variant == COSMETIC_WARDING_STONE
                || variant == COSMETIC_ELDRITCH_TILE_ALT;
    }

    /** BlockCosmeticSolid:278-286 - warding stone and the node tile carry a block entity. */
    public static boolean cosmeticHasTile(int variant) {
        return variant == COSMETIC_WARDING_STONE || variant == COSMETIC_OBSIDIAN_TILE_SHADED;
    }

    /**
     * The hardness groups of the cosmetic solid family, one group per original metadata.
     *
     * <p>Minecraft 1.21.1 binds the hardness and the blast resistance to the block and not to the
     * block state, so the original metadata that differ in hardness become separate blocks. The
     * groups are the ones of the oracle: BlockCosmeticSolid:213-225 gives the hardness 30 for the
     * metadata zero, one and eight, 4 for the metadata four, six and seven and 2 for the rest, and
     * :241-253 gives the blast resistance 999, 20 and the inherited 6.
     */
    public static final int[] COSMETIC_GROUPS = {1, 1, 0, 0, 2, 0, 2, 2, 1, 0, 0, 0, 0, 0, 0, 0};
    /** The hardness of a cosmetic group: the ordinary, the tough and the thaumium one. */
    public static final float[] COSMETIC_GROUP_HARDNESS = {2.0F, 30.0F, 4.0F};
    /** The blast resistance of a cosmetic group. */
    public static final float[] COSMETIC_GROUP_RESISTANCE = {6.0F, 999.0F, 20.0F};

    /** The hardness group of a cosmetic metadata. */
    public static int cosmeticGroup(int variant) {
        return variant >= 0 && variant < COSMETIC_GROUPS.length ? COSMETIC_GROUPS[variant] : 0;
    }

    /** The hardness of a cosmetic group. */
    public static float cosmeticGroupHardness(int group) { return COSMETIC_GROUP_HARDNESS[group]; }

    /** The blast resistance of a cosmetic group. */
    public static float cosmeticGroupResistance(int group) { return COSMETIC_GROUP_RESISTANCE[group]; }

    /** The metadata values of a cosmetic group. */
    public static java.util.List<Integer> cosmeticGroupVariants(int group) {
        java.util.List<Integer> variants = new java.util.ArrayList<>();
        for (int variant = 0; variant <= 15; variant++) {
            if (cosmeticGroup(variant) == group) variants.add(variant);
        }
        return variants;
    }

    // ------------------------------------------------------------- cosmetic slabs

    /**
     * Oracle ConfigBlocks:298 and :300 - the cosmetic wooden slabs call
     * {@code func_149711_c(2.0f).func_149752_b(5.0f)}, so the hardness is two and the blast
     * resistance is the 1.7.10 setter value five. That setter stores the value three times and
     * the 1.7.10 getter divides it by five, so the effective resistance of the original is
     * three, and that is what 1.21.1, which has neither the multiplication nor the division,
     * needs.
     */
    public static final float COSMETIC_WOOD_SLAB_RESISTANCE = 3.0F;

    /**
     * Oracle ConfigBlocks:299 and :301 - the cosmetic stone slabs call
     * {@code func_149711_c(2.0f).func_149752_b(10.0f)}, so the effective resistance of the
     * original is ten times three divided by five, that is six.
     */
    public static final float COSMETIC_STONE_SLAB_RESISTANCE = 6.0F;

    // ------------------------------------------------------------- cosmetic opaque

    /** BlockCosmeticOpaque:57-58 - family hardness 1.5 and the blast resistance setter value 5, effective 3.0. */
    public static final float COSMETIC_OPAQUE_HARDNESS = 1.5F;
    public static final float COSMETIC_OPAQUE_RESISTANCE = 3.0F;
    /** BlockCosmeticOpaque:253-259 - the warded glass has a hardness of five, or none at all with the warded stone option. */
    public static final float COSMETIC_OPAQUE_WARDED_HARDNESS = 5.0F;
    /** BlockCosmeticOpaque:261-266 - the warded glass always resists an explosion with 999. */
    public static final float COSMETIC_OPAQUE_WARDED_RESISTANCE = 999.0F;

    /** BlockCosmeticOpaque metadata 2 is the owned arcane door variant. */
    public static final int COSMETIC_OPAQUE_OWNED = 2;

    /** BlockCosmeticOpaque:107-113 - only metadata 2 carries the owned block entity. */
    public static boolean cosmeticOpaqueHasTile(int variant) {
        return variant == COSMETIC_OPAQUE_OWNED;
    }

    // -------------------------------------------------------------- variant items

    /**
     * The original recipes that produce ONE metadata value of a family block: the item id and the
     * metadata value that it places (ConfigRecipes:192, 194, 197, 209 and 212).
     *
     * <p>A 1.21.1 recipe result cannot carry a block state, so every one of them has its own item.
     */
    public static final Map<String, Integer> VARIANT_ITEMS = variantItems();

    private static Map<String, Integer> variantItems() {
        Map<String, Integer> items = new java.util.LinkedHashMap<>();
        items.put("paving_stone_travel", COSMETIC_TRAVEL_STONE);
        items.put("paving_stone_warding", COSMETIC_WARDING_STONE);
        items.put("warded_glass", COSMETIC_OPAQUE_OWNED);
        // The original blockStoneDevice metadata fourteen and blockWoodenDevice metadata two and eight.
        items.put("flux_scrubber", DEVICE_STONE_FLUX_SCRUBBER);
        // The original blockWoodenDevice metadata eight: one item that carries its colour and its
        // aspect in a data component.
        items.put("banner", DEVICE_WOOD_BANNER);
        return Map.copyOf(items);
    }

    /** The metadata value that the variant item of that id places, or -1 when there is none. */
    public static int variantOf(String item) { return VARIANT_ITEMS.getOrDefault(item, -1); }

    // ---------------------------------------------------------------------- airy

    /** BlockAiry metadata values with a distinct meaning. */
    public static final int AIRY_NODE = 0;
    public static final int AIRY_NITOR = 1;
    public static final int AIRY_LEAVES = 2;
    public static final int AIRY_LEAVES_ALT = 3;
    public static final int AIRY_WARDING_FENCE = 4;
    public static final int AIRY_ENERGIZED_NODE = 5;
    public static final int AIRY_MAGIC_HOLE = 10;
    public static final int AIRY_VOID_HOLE = 11;
    public static final int AIRY_UNBREAKABLE = 12;

    /** BlockAiry:216-230 - the per metadata light level. */
    public static int airyLight(int variant) {
        switch (variant) {
            case AIRY_NITOR:
            case AIRY_LEAVES:
            case AIRY_LEAVES_ALT:
                return 15;
            case AIRY_WARDING_FENCE:
            case AIRY_UNBREAKABLE:
                return 0;
            case AIRY_NODE:
            case AIRY_ENERGIZED_NODE:
            case AIRY_MAGIC_HOLE:
            case AIRY_VOID_HOLE:
                return 8;
            default:
                return 0;
        }
    }

    /** BlockAiry:190-205 - the per metadata hardness, -1 means unbreakable. */
    public static float airyHardness(int variant) {
        switch (variant) {
            case AIRY_NODE:
            case AIRY_ENERGIZED_NODE:
                return 2.0F;
            case AIRY_MAGIC_HOLE:
            case AIRY_VOID_HOLE:
                return 100.0F;
            case AIRY_UNBREAKABLE:
                return -1.0F;
            default:
                return 0.0F;
        }
    }

    /** BlockAiry:196-210 - the per metadata blast resistance. */
    public static float airyExplosionResistance(int variant) {
        switch (variant) {
            case AIRY_NODE:
            case AIRY_ENERGIZED_NODE:
                return 200.0F;
            case AIRY_MAGIC_HOLE:
            case AIRY_VOID_HOLE:
                return 50.0F;
            case AIRY_UNBREAKABLE:
                return Float.MAX_VALUE;
            default:
                return 0.0F;
        }
    }

    /** BlockAiry:206-213 - these metadata values are replaceable by other blocks. */
    public static boolean airyReplaceable(int variant) {
        return variant == AIRY_LEAVES || variant == AIRY_LEAVES_ALT || variant == AIRY_WARDING_FENCE
                || variant == AIRY_MAGIC_HOLE || variant == AIRY_VOID_HOLE;
    }

    /** BlockAiry:216-230 - these metadata values count as air. */
    public static boolean airyIsAir(int variant) {
        return variant == AIRY_LEAVES || variant == AIRY_LEAVES_ALT
                || variant == AIRY_MAGIC_HOLE || variant == AIRY_VOID_HOLE;
    }

    /** BlockAiry:214-218 - leaves metadata values are treated as leaves. */
    public static boolean airyIsLeaves(int variant) {
        return variant == AIRY_LEAVES || variant == AIRY_LEAVES_ALT;
    }

    /** The hardness groups of the airy family. */
    public static int airyGroup(int variant) {
        if (variant == AIRY_NODE || variant == AIRY_ENERGIZED_NODE) return 1;
        if (variant == AIRY_MAGIC_HOLE || variant == AIRY_VOID_HOLE) return 2;
        if (variant == AIRY_UNBREAKABLE) return 3;
        return 0;
    }

    public static final float[] AIRY_GROUP_HARDNESS = {0.0F, 2.0F, 100.0F, -1.0F};
    public static final float[] AIRY_GROUP_RESISTANCE = {0.0F, 200.0F, 50.0F, Float.MAX_VALUE};

    /** The hardness of an airy group. */
    public static float airyGroupHardness(int group) { return AIRY_GROUP_HARDNESS[group]; }

    /** The blast resistance of an airy group. */
    public static float airyGroupResistance(int group) { return AIRY_GROUP_RESISTANCE[group]; }

    /** The metadata values of an airy group. */
    public static java.util.List<Integer> airyGroupVariants(int group) {
        java.util.List<Integer> variants = new java.util.ArrayList<>();
        for (int variant = 0; variant <= 12; variant++) {
            if (airyGroup(variant) == group) variants.add(variant);
        }
        return variants;
    }

    /** BlockAiry:96-100 - the warding fence carries the warding stone block entity. */
    public static boolean airyHasTile(int variant) {
        return variant == AIRY_NODE || variant == AIRY_NITOR || variant == AIRY_WARDING_FENCE
                || variant == AIRY_ENERGIZED_NODE;
    }

    // ------------------------------------------------------------ device families

    /** The metal device metadata values owned by this port. */
    /**
     * BlockMetalDevice:677-686 and :711-720 - the metadata five is the OPEN item filter and
     * the metadata six is the CLOSED one: a right click on five sets six and the other way
     * round, and a powered filter becomes six, an unpowered one five. TileGrate:65-67 accepts
     * an item only while the metadata is five.
     */
    public static final int DEVICE_METAL_GRATE_OPEN = 5;
    public static final int DEVICE_METAL_GRATE_CLOSED = 6;
    public static final int DEVICE_METAL_BRAINBOX = 12;

    /** The wooden device metadata values owned by this port. */
    public static final int DEVICE_WOOD_PRESSURE_PLATE = 2;
    public static final int DEVICE_WOOD_PRESSURE_PLATE_PRESSED = 3;
    public static final int DEVICE_WOOD_BANNER = 8;

    /** The stone device metadata values owned by this port. */
    public static final int DEVICE_STONE_WAND_PEDESTAL = 5;
    public static final int DEVICE_STONE_FLUX_SCRUBBER = 14;

    /** Device kinds, used by the family blocks to pick their block entity. */
    public static final int DEVICE_KIND_NONE = 0;
    public static final int DEVICE_KIND_GRATE = 1;
    public static final int DEVICE_KIND_BRAINBOX = 2;
    public static final int DEVICE_KIND_PRESSURE_PLATE = 3;
    public static final int DEVICE_KIND_BANNER = 4;
    public static final int DEVICE_KIND_WAND_PEDESTAL = 5;
    public static final int DEVICE_KIND_FLUX_SCRUBBER = 6;

    /** BlockMetalDevice:398-435 - the metal device metadata owned by this port. */
    public static int metalDeviceKind(int variant) {
        switch (variant) {
            case DEVICE_METAL_GRATE_CLOSED:
            case DEVICE_METAL_GRATE_OPEN:
                return DEVICE_KIND_GRATE;
            case DEVICE_METAL_BRAINBOX:
                return DEVICE_KIND_BRAINBOX;
            default:
                return DEVICE_KIND_NONE;
        }
    }

    /** BlockWoodenDevice:473-495 - the wooden device metadata owned by this port. */
    public static int woodenDeviceKind(int variant) {
        switch (variant) {
            case DEVICE_WOOD_PRESSURE_PLATE:
            case DEVICE_WOOD_PRESSURE_PLATE_PRESSED:
                return DEVICE_KIND_PRESSURE_PLATE;
            case DEVICE_WOOD_BANNER:
                return DEVICE_KIND_BANNER;
            default:
                return DEVICE_KIND_NONE;
        }
    }

    /** BlockStoneDevice:257-289 - the stone device metadata owned by this port. */
    public static int stoneDeviceKind(int variant) {
        switch (variant) {
            case DEVICE_STONE_WAND_PEDESTAL:
                return DEVICE_KIND_WAND_PEDESTAL;
            case DEVICE_STONE_FLUX_SCRUBBER:
                return DEVICE_KIND_FLUX_SCRUBBER;
            default:
                return DEVICE_KIND_NONE;
        }
    }

    /**
     * BlockMetalDevice:101-103 - hardness 3 and the blast resistance setter value 17.
     * The 1.7.10 setter stores three times the argument and the vanilla getter divides
     * the stored value by five, so the effective blast resistance is 17*3/5 = 10.2.
     */
    public static final float DEVICE_METAL_HARDNESS = 3.0F;
    public static final float DEVICE_METAL_RESISTANCE = 10.2F;
    /** BlockWoodenDevice:82 - the hardness of the wooden device family is 2.5. */
    public static final float DEVICE_WOOD_HARDNESS = 2.5F;
    /** BlockWoodenDevice:83 - the blast resistance setter value 10, effective 6.0. */
    public static final float DEVICE_WOOD_RESISTANCE = 6.0F;

    /**
     * BlockWoodenDevice:196-203 - the hardness of the arcane pressure plate is two, or none at
     * all while the warded stone option is on.
     */
    public static final float DEVICE_WOOD_PRESSURE_PLATE_HARDNESS = 2.0F;

    /**
     * BlockWoodenDevice:204-212 - the blast resistance of the arcane pressure plate is the
     * absolute value 999, which the original returns from its own getter. It is therefore not
     * the 0.6 factor of a setter value and must not be scaled.
     */
    public static final float DEVICE_WOOD_PRESSURE_PLATE_RESISTANCE = 999.0F;
    /** BlockStoneDevice:72-73 - hardness 3 and the blast resistance setter value 25, effective 15.0. */
    public static final float DEVICE_STONE_HARDNESS = 3.0F;
    public static final float DEVICE_STONE_RESISTANCE = 15.0F;

    // --------------------------------------------------------------------- altar

    /** BlockEldritch:266 - the altar accepts at most four eldritch eyes. */
    public static final int ALTAR_MAX_EYES = 4;

    /** BlockEldritch:267-270 - from the third eye the altar becomes a spawner. */
    public static final int ALTAR_SPAWNER_EYES = 2;

    /** TileEldritchAltar:95-106 - spawn type 0 spawns cultists, type 1 the guardian. */
    public static final byte ALTAR_SPAWN_TYPE_CULTIST = 0;
    public static final byte ALTAR_SPAWN_TYPE_GUARDIAN = 1;

    /** BlockEldritch:268-270 - the altar switches to the guardian spawner at this eye count. */
    public static byte altarSpawnTypeForEyes(int eyes) {
        return eyes >= ALTAR_SPAWNER_EYES ? ALTAR_SPAWN_TYPE_GUARDIAN : ALTAR_SPAWN_TYPE_CULTIST;
    }

    /** BlockEldritch:271 - one eye is consumed per activation, capped by the altar. */
    public static int altarEyesAfterActivation(int eyes) {
        return Math.min(ALTAR_MAX_EYES, eyes + 1);
    }

    /** BlockEldritch:266 - the altar refuses further eyes at this count. */
    public static boolean altarAcceptsEye(int eyes) {
        return eyes < ALTAR_MAX_EYES;
    }

    /** TileEldritchAltar:219-220 - the eldritch maze is 15..29 blocks wide and deep. */
    public static int altarMazeSize(Random random) {
        return 15 + random.nextInt(8) * 2;
    }

    /** TileEldritchAltar:219-220 - the same size for an already drawn roll. */
    public static int altarMazeSize(int roll) {
        return 15 + roll * 2;
    }

    /** TileEldritchAltar:93-107 - the first spawn happens after 80 ticks, then every 40. */
    public static final int ALTAR_FIRST_SPAWN_DELAY = 80;
    public static final int ALTAR_SPAWN_PERIOD = 40;

    /** TileEldritchAltar:111-118 - a cleric must be present, and eight cultists cap the knights. */
    public static final int ALTAR_MAX_CULTISTS = 8;

    /** TileEldritchAltar:187-188 - more than two successful clerics counts as spawned. */
    public static final int ALTAR_CLERICS_REQUIRED = 2;

    /** TileEldritchAltar:151-175 - the four cleric corners around the altar. */
    public static int altarClericOffsetX(int index) {
        return index == 0 || index == 1 ? -2 : 2;
    }

    public static int altarClericOffsetZ(int index) {
        return index == 0 || index == 2 ? -2 : 2;
    }

    // ------------------------------------------------------------------- obelisk

    /** TileEldritchObelisk:70 - the obelisk affects living entities within six blocks. */
    public static final double OBELISK_RANGE = 6.0D;

    /**
     * TileEldritchObelisk:47 and EntityUtils:181-192 - the obelisk searches a cube of the
     * original range around the middle of its own block. The range is a radius around the
     * obelisk and never an absolute corner of the world.
     */
    public static net.minecraft.world.phys.AABB obeliskBox(net.minecraft.core.BlockPos pos) {
        return new net.minecraft.world.phys.AABB(pos.getX()+.5D,pos.getY(),pos.getZ()+.5D,
                pos.getX()+.5D,pos.getY(),pos.getZ()+.5D).inflate(OBELISK_RANGE,OBELISK_RANGE,OBELISK_RANGE);
    }

    /** TileEldritchObelisk:76-77 - the applied effects last forty ticks. */
    public static final int OBELISK_EFFECT_DURATION = 40;
    public static final int OBELISK_EFFECT_AMPLIFIER = 0;

    /**
     * TileEldritchObelisk:45-63 - the original's counter is never raised, so the
     * obelisk offers its pair of effects on EVERY server tick. What actually
     * limits the effect is the guard on the regeneration of the mob.
     */
    public static final int OBELISK_TICK_PERIOD = 1;

    /** TileEldritchObelisk:72 - only eldritch mobs are affected. */
    public static boolean obeliskAffects(boolean eldritchMob, boolean living) {
        return eldritchMob && living;
    }

    /**
     * TileEldritchObelisk:49 - the original skips an eldritch mob that already
     * carries regeneration, so the pair of effects is only handed out again
     * after the forty ticks ran out. The obelisk BUFFS the eldritch mobs: it
     * gives them damage boost and regeneration, never a slowdown.
     */
    public static boolean obeliskAffects(boolean eldritchMob, boolean living, boolean alreadyRegenerating) {
        return obeliskAffects(eldritchMob, living) && !alreadyRegenerating;
    }

    // ------------------------------------------------------------------- portal

    /** TileEldritchPortal:73-79 - the transfer cooldown in ticks. */
    public static final int PORTAL_COOLDOWN = 100;

    /** TileEldritchPortal:67 - the portal scans for players every five ticks. */
    public static final int PORTAL_SCAN_PERIOD = 5;

    /** TileEldritchPortal:52 - the portal sound repeats every 250 ticks. */
    public static final int PORTAL_SOUND_PERIOD = 250;

    /** TileEldritchPortal:60-61 - the open counter stops at 30. */
    public static final int PORTAL_OPEN_COUNT_MAX = 30;

    /** TileEldritchPortal:64-65 - the portal only moves a player that is not already travelling. */
    public static boolean portalCanTransfer(boolean travelling, boolean riding) {
        return !travelling && !riding;
    }

    // ------------------------------------------------------------------- mirror

    /**
     * BlockMirror:168-175 - the original adds the side the player clicked to the metadata of
     * the item, so the metadata of the placed mirror is the direction it looks at.
     * BlockMirror:268-295 - its plate is the thin square on the opposite side, the side of the
     * wall it hangs on, and BlockMirror:177-203 - the wall behind it is the opposite side too.
     */
    public static net.minecraft.core.Direction mirrorWallSide(net.minecraft.core.Direction facing) {
        return facing.getOpposite();
    }

    /** BlockMirror:268-295 - the plate of the mirror is a sixteenth thick. */
    public static final double MIRROR_PLATE_WIDTH = 0.0625D;

    /** TileMirror:64 - the mirror retries a broken link every 40 ticks. */
    public static final int MIRROR_LINK_RETRY = 40;

    /** TileMirror:352-356 - a failing link retries less often, up to 600 ticks. */
    public static final int MIRROR_LINK_RETRY_STEP = 20;
    public static final int MIRROR_LINK_RETRY_MAX = 600;

    /** TileMirror:352-356 - the retry interval after a failed restore. */
    public static int mirrorLinkRetryInterval(int current) {
        return Math.min(MIRROR_LINK_RETRY_MAX, current + MIRROR_LINK_RETRY_STEP);
    }

    /** TileMirror:365 - the instability decays by one every twenty ticks. */
    public static final int MIRROR_INSTABILITY_DECAY_PERIOD = 20;

    /** TileMirror:314 - the instability is paid with at most one ORDER vis per call. */
    public static final int MIRROR_INSTABILITY_VIS_MAX = 1;

    /** TileMirror:314 - the vis that pays for the instability of a mirror. */
    public static int mirrorInstabilityVisPayment(int instability) {
        return instability > 0 ? Math.min(instability, MIRROR_INSTABILITY_VIS_MAX) : 0;
    }

    /** TileMirror:341 - the ejection rate is the square of the instability over fifty. */
    public static int mirrorEjectTickRate(int instability) {
        int rate = instability / 50;
        return rate * rate;
    }

    /** TileMirror:340-342 - a zero rate ejects every tick, otherwise on the rate divisor. */
    public static boolean mirrorShouldEject(int count, int instability) {
        int rate = mirrorEjectTickRate(instability);
        return rate == 0 || count % rate == 0;
    }

    /** TileMirror:297-300 - an item is only ejected once the mirror has run for 20 ticks. */
    public static final int MIRROR_EJECT_DELAY = 20;

    /** TileMirror:64 - the output inventory holds at most 64 stacks. */
    public static final int MIRROR_OUTPUT_CAPACITY = 64;

    /** TileMirrorEssentia:186-190 - only a single unit may be taken per request. */
    public static final int MIRROR_ESSENTIA_MAX_TAKE = 1;

    /** TileMirrorEssentia:191 - the essentia mirror reaches eight units at once. */
    public static final int MIRROR_ESSENTIA_DRAIN = 8;

    /** TileMirrorEssentia:187-188 - a larger request is refused outright. */
    public static boolean mirrorEssentiaTakeAllowed(int requested) {
        return requested <= MIRROR_ESSENTIA_MAX_TAKE;
    }

    /** TileMirrorEssentia:194 - the drain amount is capped at eight. */
    public static int mirrorEssentiaDrainAmount(int available) {
        return Math.min(available, MIRROR_ESSENTIA_DRAIN);
    }

    /**
     * TileMirrorEssentia:194-201 - the network that pays for a unit is the slab
     * in front of the twin: eight blocks deep in the direction the twin looks at
     * and seventeen blocks wide in the other two axes.
     */
    public static int[] mirrorEssentiaSlabOffset(net.minecraft.core.Direction facing, int first, int second, int depth) {
        if (facing.getAxis() == net.minecraft.core.Direction.Axis.Y) {
            return new int[] {first, depth * facing.getStepY(), second};
        }
        if (facing.getAxis() == net.minecraft.core.Direction.Axis.Z) {
            return new int[] {first, second, depth * facing.getStepZ()};
        }
        return new int[] {depth * facing.getStepX(), first, second};
    }

    /** TileMirrorEssentia:129-139 - the link is only valid when both mirrors point at each other. */
    public static boolean mirrorLinksMatch(boolean linked, boolean targetLinked, int targetX, int targetY, int targetZ,
                                           int targetDim, int x, int y, int z, int dim) {
        if (!linked || !targetLinked) return false;
        return targetX == x && targetY == y && targetZ == z && targetDim == dim;
    }

    // ---------------------------------------------------------------- magic box

    /** TileMagicBox:60 - one box contributes 27 slots. */
    public static final int MAGIC_BOX_SLOTS_PER_BOX = 27;

    /** TileMagicBox:207 - the stack limit of a magic box slot. */
    public static final int MAGIC_BOX_MAX_STACK = 64;

    /** TileMagicBox:271 - at most 1024 boxes are searched for a link. */
    public static final int MAGIC_BOX_MAX_LINKED = 1024;

    /** TileMagicBox:60 - the slot count of a box chain. */
    public static int magicBoxSize(int linkedBoxes) {
        return MAGIC_BOX_SLOTS_PER_BOX * (linkedBoxes + 1);
    }

    /** TileMagicBox:78 - sorting is disabled with -1. */
    public static final int MAGIC_BOX_SORT_NONE = -1;
    public static final int MAGIC_BOX_SORT_NAME_ASCENDING = 0;
    public static final int MAGIC_BOX_SORT_NAME_DESCENDING = 1;
    public static final int MAGIC_BOX_SORT_MOD = 2;

    /** TileMagicBox:270-286 - the sorting mode of a box. */
    public static int magicBoxNextSorting(int sorting) {
        return sorting >= MAGIC_BOX_SORT_MOD ? MAGIC_BOX_SORT_NONE : sorting + 1;
    }

    /** TileMagicBox:281-306 - only name sorting compares the two display names. */
    public static boolean magicBoxSortsByName(int sorting) {
        return sorting == MAGIC_BOX_SORT_NAME_ASCENDING || sorting == MAGIC_BOX_SORT_NAME_DESCENDING;
    }

    /** TileMagicBox:308 - only mod sorting compares the mod identifier first. */
    public static boolean magicBoxSortsByMod(int sorting) {
        return sorting == MAGIC_BOX_SORT_MOD;
    }

    /** TileMagicBox:404-411 - a box links to its six direct neighbours. */
    public static final int MAGIC_BOX_LINK_DIRECTIONS = 6;

    /**
     * TileMagicBox:210 - two neighbouring slots are only merged when they hold the same item
     * with the same tags. The original guarded the merge with {@code isItemEqual} and
     * {@code areItemStackTagsEqual}, so a different item or a different tag is never merged
     * into a single stack.
     */
    public static boolean magicBoxMerges(net.minecraft.world.item.ItemStack first, net.minecraft.world.item.ItemStack second) {
        return net.minecraft.world.item.ItemStack.isSameItem(first,second)
                && net.minecraft.world.item.ItemStack.isSameItemSameComponents(first,second);
    }

    /**
     * TileMagicBox:210 - a slot that is already full takes no part in a merge.
     */
    public static boolean magicBoxSlotHasRoom(net.minecraft.world.item.ItemStack stack) {
        return stack.getCount() < stack.getMaxStackSize();
    }

    // ----------------------------------------------------------- warding stones

    /** TileWardingStone:29 - entities inside the ward are pushed away every five ticks. */
    public static final int WARDING_STONE_PUSH_PERIOD = 5;

    /** TileWardingStone:29-31 - the push strength and the vertical drop. */
    public static final float WARDING_STONE_PUSH = 0.2F;
    public static final double WARDING_STONE_PUSH_Y = -0.1D;

    /** TileWardingStone:23-25 - the ward covers the three blocks above the stone. */
    public static final int WARDING_STONE_HEIGHT = 3;

    /** TileWardingStone:34-42 - the ward re-seals its airy blocks every hundred ticks. */
    public static final int WARDING_STONE_SEAL_PERIOD = 100;

    /** TileWardingStone:25 - players are never pushed by a ward. */
    public static boolean wardingStonePushes(boolean player, boolean dead) {
        return !player && !dead;
    }

    /** TileWardingStoneFence:20-24 - a warding fence survives only above a warding stone. */
    public static boolean wardingStoneFenceSupported(boolean stoneBelow, boolean stoneTwoBelow) {
        return stoneBelow || stoneTwoBelow;
    }

    // ------------------------------------------------------------------- banner

    /** TileBanner:22 - an unset banner has no colour. */
    public static final int BANNER_COLOUR_NONE = -1;

    /** TileBanner:25 - the banner remembers its facing and wall state. */
    public static final int BANNER_DEFAULT_FACING = 0;

    /** TileBanner:87 - a banner item keeps its aspect tag in the item data. */
    public static String bannerAspectTag(String aspectTag) {
        return aspectTag == null ? "" : aspectTag;
    }

    /** TileBanner:100 - a banner is drawn only when it has a colour or an aspect. */
    public static boolean bannerHasContent(int colour, boolean hasAspect) {
        return colour >= 0 || hasAspect;
    }

    /**
     * The original facing byte of the banner block entity.
     *
     * <p>Original BlockWoodenDevice:266-283 draws the wall banner of the facing
     * byte zero on the north side, eight on the south side, twelve on the west
     * side and four on the east side; original EntityCultistPortal:143-157 and
     * WorldGenEldritchRing:101-125 set the same four bytes. The native keeps the
     * direction and converts it back to that byte here, so the original
     * {@code facing * 360 / 16} rotation of the banner renderer is reproduced.
     */
    public static int bannerOriginalFacing(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case NORTH -> 0;
            case SOUTH -> 8;
            case WEST -> 12;
            case EAST -> 4;
            default -> 0;
        };
    }

    /**
     * The original colour table of the banner.
     *
     * <p>Original {@code thaumcraft/common/lib/utils/Utils.java:80} keeps its own
     * sixteen dye colours, which are NOT the vanilla dye colours of 1.7.10: the
     * white is {@code 0xF0F0F0} and the black is {@code 0x1E1B1B}. Original
     * TileBannerRenderer:55-58 tints the tabs and the cloth with this table.
     */
    public static int bannerColour(int colour) {
        int[] colours = {0xF0F0F0, 15435844, 12801229, 6719955, 14602026, 4312372, 14188952, 0x434343,
                0xA0A0A0, 2651799, 8073150, 2437522, 5320730, 3887386, 11743532, 0x1E1B1B};
        return colour < 0 || colour >= colours.length ? 0xFFFFFF : colours[colour];
    }

    // --------------------------------------------------------- arcane pressure plate

    /** BlockWoodenDevice:371-387 - the plate cycles through three sensitivity modes. */
    public static final int PLATE_SETTING_ALL = 0;
    public static final int PLATE_SETTING_NOT_OWNER = 1;
    public static final int PLATE_SETTING_OWNER_ONLY = 2;
    public static final int PLATE_SETTING_COUNT = 3;

    /** BlockWoodenDevice:371-375 - activating the plate advances the setting. */
    public static int plateNextSetting(int setting) {
        return (setting + 1) % PLATE_SETTING_COUNT;
    }

    /** BlockWoodenDevice:559-576 - the detection box of the plate. */
    public static final double PLATE_INSET = 0.125D;
    public static final double PLATE_HEIGHT = 0.25D;

    /**
     * BlockWoodenDevice:249-256 - the box of the plate itself is inset by a sixteenth on every
     * side. The unpressed plate is a sixteenth high, the pressed one only half of that.
     */
    public static final double PLATE_BOX_INSET = 0.0625D;
    public static final double PLATE_BOX_HEIGHT = 0.0625D;
    public static final double PLATE_BOX_HEIGHT_PRESSED = 0.03125D;

    /**
     * BlockWoodenDevice:568-571 - the original filter skips dead entities and, depending on
     * the setting, the owner or everybody but the owner.
     */
    public static boolean plateDetects(int setting, boolean dead, boolean player, boolean owner, boolean access) {
        if (dead) return false;
        if (setting == PLATE_SETTING_NOT_OWNER && player && (owner || access)) return false;
        if (setting == PLATE_SETTING_OWNER_ONLY && player && !owner && !access) return false;
        return true;
    }

    // ----------------------------------------------------------------- brainbox

    /** TileBrainbox:17 - the brainbox points at the machine half it serves. */
    public static final int BRAINBOX_FACING_UNKNOWN = -1;

    /** TileThaumatorium:504 - the brainbox must face back at the thaumatorium. */
    public static boolean brainboxFacesMachine(int brainboxFacing, int machineFacing) {
        return brainboxFacing != BRAINBOX_FACING_UNKNOWN && brainboxFacing == machineFacing;
    }

    /** BlockMetalDevice:487-490 - the brainbox needs a free block in front of it. */
    public static boolean brainboxHasRoom(boolean freeInFront) {
        return freeInFront;
    }

    /**
     * EntityItemGrate:26-36 - the item filter is the metal device of the fifth
     * and the sixth metadata. The native tree carries that filter twice: as the
     * dedicated item grate block and as the two metal device variants.
     */
    public static boolean itemFilterBlock(boolean dedicatedGrate, boolean metalDevice, int variant) {
        if (dedicatedGrate) return true;
        return metalDevice && (variant == DEVICE_METAL_GRATE_CLOSED || variant == DEVICE_METAL_GRATE_OPEN);
    }

    // -------------------------------------------------------------- flux scrubber

    /** TileFluxScrubber:41-47 - four charges produce essentia, at most four are stored. */
    public static final int FLUX_CHARGE_COST = 4;
    public static final int FLUX_ESSENTIA_MAX = 4;
    public static final int FLUX_ESSENTIA_CHANCE = 4;

    /** TileFluxScrubber:49-52 - the scrubber needs five power to work. */
    public static final int FLUX_POWER_MIN = 5;
    public static final int FLUX_POWER_DRAIN = 10;

    /** TileFluxScrubber:57 - the scrubber scans sixteen blocks in every direction. */
    public static final int FLUX_SCAN_DISTANCE = 16;

    /** TileFluxScrubber:81 - at most sixteen candidate blocks are examined per pass. */
    public static final int FLUX_SCAN_BATCH = 16;

    /** TileFluxScrubber:71 - removing one flux block costs five power. */
    public static final int FLUX_POWER_PER_BLOCK = 5;

    /** TileFluxScrubber:55 - the stored essentia never exceeds four. */
    public static int fluxScrubberEssentiaAfterCharge(int essentia) {
        return Math.min(FLUX_ESSENTIA_MAX, essentia + 1);
    }

    /** TileFluxScrubber:41-46 - a charge is only spent when at least four are stored. */
    public static boolean fluxScrubberSpendsCharge(int charges) {
        return charges >= FLUX_CHARGE_COST;
    }

    /** TileFluxScrubber:78-84 - a flux block loses one level, or vanishes at level zero. */
    public static int fluxScrubberReducedLevel(int level) {
        return level > 0 ? level - 1 : -1;
    }

    /**
     * TileFluxScrubber:92 - only the flux blocks whose squared distance to the
     * scrubber is below the squared radius of sixteen are cleaned.
     */
    public static boolean fluxScrubberInRange(int dx, int dy, int dz) {
        return dx * dx + dy * dy + dz * dz < FLUX_SCAN_DISTANCE * FLUX_SCAN_DISTANCE;
    }

    // ------------------------------------------------------------ ethereal bloom

    /** TileEtherealBloom:36 - the bloom converts a tainted column back to the overworld. */
    public static final int ETHEREAL_BLOOM_CONVERT_PERIOD = 20;

    /** TileEtherealBloom:37-38 - the random column offset is minus eight to plus eight. */
    public static final int ETHEREAL_BLOOM_OFFSET = 8;

    /** TileEtherealBloom:41 - the conversion reaches nine blocks around the bloom. */
    public static final double ETHEREAL_BLOOM_RADIUS_SQUARED = 81.0D;

    /** TileEtherealBloom:41 - the bloom only converts tainted, eerie and magical forest biomes. */
    public static boolean etherealBloomConverts(boolean tainted, boolean eerie, boolean magicalForest) {
        return tainted || eerie || magicalForest;
    }

    // ---------------------------------------------------------------------- jars

    /** TileJarFillable:23 - the fillable jar stores 64 units. */
    public static final int JAR_CAPACITY = 64;

    /** TileJarFillable:222-232 - the unlabelled suction is 32, the labelled suction 64. */
    public static final int JAR_SUCTION_DEFAULT = 32;
    public static final int JAR_SUCTION_LABELLED = 64;

    /** TileJarFillableVoid:40-46 - the void jar keeps 32 suction, 48 when labelled. */
    public static final int JAR_SUCTION_VOID = 32;
    public static final int JAR_SUCTION_VOID_LABELLED = 48;

    /** TileJarFillable:209-215 - the jar pulls one unit every five ticks. */
    public static final int JAR_FILL_PERIOD = 5;

    /** TileJarFillable:131-138 - only the matching aspect may be added or taken. */
    public static int jarAddedAmount(int amount, int stored, int capacity, boolean sameAspect, boolean empty) {
        if (amount == 0) return 0;
        if (stored < capacity && sameAspect || empty) {
            int added = Math.min(amount, capacity - stored);
            return added;
        }
        return 0;
    }

    /** TileJarFillableVoid:19-30 - the void jar clamps the stored amount to the capacity. */
    public static int jarVoidStoredAmount(int amount, int capacity) {
        return Math.min(capacity, amount);
    }

    /** TileJarFillable:150-158 - the suction drops to zero once the jar is full. */
    public static int jarSuctionAmount(int stored, int capacity, boolean labelled, boolean voidJar) {
        if (voidJar) return labelled && stored < capacity ? JAR_SUCTION_VOID_LABELLED : JAR_SUCTION_VOID;
        if (stored < capacity) return labelled ? JAR_SUCTION_LABELLED : JAR_SUCTION_DEFAULT;
        return 0;
    }

    /** TileJarFillable:139-144 - the minimum suction the source must offer. */
    public static int jarMinimumSuction(boolean labelled, boolean voidJar) {
        if (voidJar) return labelled ? JAR_SUCTION_VOID_LABELLED : JAR_SUCTION_VOID;
        return labelled ? JAR_SUCTION_LABELLED : JAR_SUCTION_DEFAULT;
    }

    // --------------------------------------------------------------------- loot

    /** BlockLoot:78 - a container drops one plus its rarity plus zero to two extra items. */
    public static int lootDropCount(int rarity, Random random) {
        return lootDropCount(rarity, random.nextInt(3));
    }

    /** The drop count of a loot container with an explicit bonus roll. */
    public static int lootDropCount(int rarity, int bonus) {
        return 1 + rarity + bonus;
    }

    /** BlockLoot:78-84 - the rarity of the generated loot equals the container rarity. */
    public static int lootRarity(int metadata) {
        return Math.max(0, Math.min(2, metadata));
    }

    /** BlockLoot:82 - every generated item keeps the container rarity. */
    public static boolean lootUsesRarity(int metadata) {
        return metadata > 0;
    }

    // -------------------------------------------------------------- arcane door

    /** BlockArcaneDoor:44-46 - the door metadata holds the orientation in bits 0 and 1. */
    public static final int DOOR_METADATA_ORIENTATION_MASK = 3;
    public static final int DOOR_METADATA_OPEN = 4;
    public static final int DOOR_METADATA_UPPER = 8;
    public static final int DOOR_METADATA_FLIP = 16;

    /** BlockArcaneDoor:288-291 - opening a door flips the open bit of the lower half. */
    public static int doorToggleOpen(int metadata) {
        return (metadata & 7) ^ DOOR_METADATA_OPEN;
    }

    /** BlockArcaneDoor:335-345 - the lower half carries the orientation and the open bit. */
    public static int doorLowerMetadata(int metadata) {
        return metadata & 7;
    }

    /** BlockArcaneDoor:346-357 - the upper half carries the flip bit. */
    public static int doorUpperMetadata(int metadata) {
        return DOOR_METADATA_UPPER | (metadata & 1);
    }

    /** BlockArcaneDoor:145-151 - the open bit is the fourth metadata bit. */
    public static boolean doorIsOpen(int metadata) {
        return (metadata & DOOR_METADATA_OPEN) != 0;
    }

    /** BlockArcaneDoor:245-248 - the orientation is the lowest two metadata bits. */
    public static int doorOrientation(int metadata) {
        return metadata & DOOR_METADATA_ORIENTATION_MASK;
    }

    /** BlockArcaneDoor:349-357 - the flip bit is the fifth metadata bit. */
    public static boolean doorIsFlipped(int metadata) {
        return (metadata & DOOR_METADATA_FLIP) != 0;
    }

    /** BlockArcaneDoor:141-146 - the upper half keeps the flip of the lower half. */
    public static int doorFlipBit(boolean flipped) {
        return flipped ? 1 : 0;
    }

    /** BlockArcaneDoor:160-163 - the door refuses everybody who is not the owner. */
    public static boolean doorAllows(String owner, java.util.List<String> accessList, String playerName) {
        if (owner != null && owner.equals(playerName)) return true;
        if (accessList == null) return false;
        return accessList.contains("0" + playerName) || accessList.contains("1" + playerName);
    }

    // ------------------------------------------------------------------- stairs

    /** BlockCosmeticStairs - the stairs mirror the reference block shape. */
    public static boolean stairsAreSeamless(int referenceMetadata) {
        return referenceMetadata == COSMETIC_ELDRITCH_TILE;
    }

    /** BlockCosmeticStoneSlab - the slab variants are the arcane and the eldritch stone. */
    public static final int SLAB_STONE_ARCANE = 0;
    public static final int SLAB_STONE_ELDRITCH = 1;

    /** BlockCosmeticWoodSlab - the slab variants are greatwood and silverwood. */
    public static final int SLAB_WOOD_GREATWOOD = 0;
    public static final int SLAB_WOOD_SILVERWOOD = 1;

    /**
     * The original blockCosmeticDoubleSlabWood and blockCosmeticDoubleSlabStone (ConfigBlocks:300-301):
     * two double slab blocks that carry the metadata of their single slab. The native port keeps the
     * four double slabs of the two families in one block with the four metadata values.
     */
    public static final int SLAB_DOUBLE_WOOD_GREATWOOD = 0;
    public static final int SLAB_DOUBLE_WOOD_SILVERWOOD = 1;
    public static final int SLAB_DOUBLE_STONE_ARCANE = 2;
    public static final int SLAB_DOUBLE_STONE_ELDRITCH = 3;

    /** BlockCosmeticWoodSlab:56-58 and BlockCosmeticStoneSlab:56-58 - the double slab drops the single slab. */
    public static boolean doubleSlabIsWooden(int variant) { return variant <= SLAB_DOUBLE_WOOD_SILVERWOOD; }

    /** The double wooden slab drops two single slabs, the double stone slab drops one. */
    public static int doubleSlabDropCount(int variant) { return doubleSlabIsWooden(variant) ? 2 : 1; }
}
