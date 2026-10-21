package thaumcraft.common.lib.world;

import net.minecraft.core.Direction;

/**
 * The pure wiring rules of the Outer Lands generator.
 *
 * <p>Minecraft 1.21.1 binds the hardness and the blast resistance of a block
 * and not its metadata, so the three original metadata families of the Outer
 * Lands became small families of blocks that share one block state property
 * (the eldritch family, the cosmetic solid family and the airy family). The
 * generator therefore has to pick the <em>block</em> that hosts an original
 * metadata before it can set the metadata on it.
 *
 * <p>Oracle sources:
 * <ul>
 *   <li>{@code thaumcraft/common/blocks/BlockEldritch.java:120-135} and
 *       {@code thaumcraft/common/blocks/BlockCosmeticSolid.java:213-239} - the per metadata
 *       hardness, resistance and light that split the families.</li>
 *   <li>{@code thaumcraft/common/blocks/BlockAiry.java:190-230} - the airy metadata family.</li>
 *   <li>{@code thaumcraft/common/blocks/BlockCosmeticStoneSlab.java:20-40} - the two stone slabs.</li>
 *   <li>{@code thaumcraft/common/blocks/BlockWoodenDevice.java:264-290} and
 *       {@code thaumcraft/common/blocks/BlockWoodenDeviceItem.java:102-119} - the banner facing
 *       encoding of the original tile.</li>
 *   <li>{@code thaumcraft/common/lib/world/dim/GenCommon.java:151-260} - the 1.7 stair metadata
 *       and the slab metadata of the rooms.</li>
 * </ul>
 */
public final class OuterLandsWiringRules {
    private OuterLandsWiringRules() {}

    /* ------------------------------------------------------------------ */
    /* The registry paths of the eldritch family                            */
    /* ------------------------------------------------------------------ */

    /** The eldritch altar, the obelisk, the obelisk middle and the capstone. */
    public static final String ELDRITCH = "thaumcraft:eldritch";
    /** The glowing crust and the glyphed eldritch stone. */
    public static final String ELDRITCH_CRUST = "thaumcraft:eldritch_crust";
    /** The decorated eldritch stone. */
    public static final String ELDRITCH_DECO = "thaumcraft:eldritch_deco";
    /** The eldritch doorway and the eldritch lock. */
    public static final String ELDRITCH_DOORWAY = "thaumcraft:eldritch_doorway";
    /** The eldritch crab spawner and the runed eldritch trap. */
    public static final String ELDRITCH_SPAWNER = "thaumcraft:eldritch_spawner";
    /** The eldritch crystal. */
    public static final String ELDRITCH_CRYSTAL = "thaumcraft:eldritch_crystal";
    /** The invisible wall face. */
    public static final String ELDRITCH_NOTHING = "thaumcraft:eldritch_nothing";
    /** The gate between the overworld and the Outer Lands. */
    public static final String ELDRITCH_PORTAL = "thaumcraft:eldritch_portal";

    /* ------------------------------------------------------------------ */
    /* The registry paths of the cosmetic solid family                     */
    /* ------------------------------------------------------------------ */

    /** The ordinary cosmetic stone: metadata one, two, three, five and nine to fifteen. */
    public static final String COSMETIC_SOLID = "thaumcraft:cosmetic_solid";
    /** The obsidian tile and its shaded variant. */
    public static final String COSMETIC_SOLID_TOUGH = "thaumcraft:cosmetic_solid_tough";
    /** The thaumium block, the pedestal top and the arcane stone. */
    public static final String COSMETIC_SOLID_THAUMIUM = "thaumcraft:cosmetic_solid_thaumium";
    /** The arcane stone slab. */
    public static final String COSMETIC_STONE_SLAB_ARCANE = "thaumcraft:cosmetic_stone_slab_arcane";
    /** The eldritch stone slab. */
    public static final String COSMETIC_STONE_SLAB_ELDRITCH = "thaumcraft:cosmetic_stone_slab_eldritch";

    /* ------------------------------------------------------------------ */
    /* The registry paths of the airy family and of the devices             */
    /* ------------------------------------------------------------------ */

    /** The ordinary airy blocks. */
    public static final String AIRY = "thaumcraft:airy";
    /** The node and the energised node. */
    public static final String AIRY_NODE = "thaumcraft:airy_node";
    /** The magic and the void hole. */
    public static final String AIRY_HOLE = "thaumcraft:airy_hole";
    /** The unbreakable airy block, the invisible door of the rooms. */
    public static final String AIRY_UNBREAKABLE = "thaumcraft:airy_unbreakable";
    /** The eldritch stairs of the rooms. */
    public static final String STAIRS_ELDRITCH = "thaumcraft:eldritch_stairs";
    /** The wooden device that carries the banners of the altar. */
    public static final String WOODEN_DEVICE = "thaumcraft:wooden_device";
    /** The loot urn of the rooms. */
    public static final String LOOT_URN = "thaumcraft:loot_urn";
    /** The loot crate of the nest room. */
    public static final String LOOT_CRATE = "thaumcraft:loot_crate";

    /* ------------------------------------------------------------------ */
    /* The block that hosts an original metadata                           */
    /* ------------------------------------------------------------------ */

    /**
     * One row of the original eldritch metadata table.
     *
     * @param metadata the original 1.7 metadata of the oracle block family
     * @param blockId the native registry id of the block that hosts the metadata
     * @param variant the value of the native "variant" block state property
     */
    public record EldritchVariant(int metadata, String blockId, int variant) {
        /** The hardness of the metadata (oracle {@code BlockEldritch:231-246}). */
        public float hardness() { return EldritchBlockRules.eldritchHardness(metadata); }

        /** The blast resistance of the metadata (oracle {@code BlockEldritch:248-260}). */
        public float resistance() { return EldritchBlockRules.eldritchExplosionResistance(metadata); }

        /** The light level of the metadata (oracle {@code BlockEldritch:120-135}). */
        public int light() { return EldritchBlockRules.eldritchLight(metadata); }
    }

    /**
     * The original eldritch metadata table.
     *
     * <p>Oracle {@code BlockEldritch:78-94} gives every metadata its own icon and
     * {@code :231-260} its own hardness and blast resistance, so the original
     * family became six native blocks that carry the model of their metadata in
     * the "variant" block state property. The metadata eleven is the eldritch
     * crystal of the native port, the original crystal was a metadata of the
     * separate crystal block.
     *
     * <p>The native blockstates of the port are the authority of the grouping:
     * {@code blockstates/eldritch.json} carries the metadata zero to three,
     * {@code eldritch_crust.json} the metadata four and five, {@code eldritch_deco.json}
     * the metadata six, {@code eldritch_doorway.json} the metadata seven and eight and
     * {@code eldritch_spawner.json} the metadata nine and ten.
     */
    public static final java.util.List<EldritchVariant> ELDRITCH_VARIANTS = java.util.List.of(
            new EldritchVariant(0, ELDRITCH, 0),
            new EldritchVariant(1, ELDRITCH, 1),
            new EldritchVariant(2, ELDRITCH, 2),
            new EldritchVariant(3, ELDRITCH, 3),
            new EldritchVariant(4, ELDRITCH_CRUST, 4),
            new EldritchVariant(5, ELDRITCH_CRUST, 5),
            new EldritchVariant(6, ELDRITCH_DECO, 6),
            new EldritchVariant(7, ELDRITCH_DOORWAY, 7),
            new EldritchVariant(8, ELDRITCH_DOORWAY, 8),
            new EldritchVariant(9, ELDRITCH_SPAWNER, 9),
            new EldritchVariant(10, ELDRITCH_SPAWNER, 10),
            new EldritchVariant(11, ELDRITCH_CRYSTAL, 11));

    /**
     * The row of an original eldritch metadata, or null when the metadata is unknown.
     *
     * @param metadata the original 1.7 metadata
     */
    public static EldritchVariant eldritchVariant(int metadata) {
        for (EldritchVariant variant : ELDRITCH_VARIANTS) {
            if (variant.metadata() == metadata) return variant;
        }
        return null;
    }

    /**
     * The eldritch block that hosts an original {@code BlockEldritch} metadata.
     *
     * <p>Oracle {@code BlockEldritch:231-260} gives every metadata its own
     * hardness and blast resistance, so the 1.21.1 port splits the family into
     * the six blocks of {@link EldritchBlockRules#ELDRITCH_GROUPS}.
     */
    public static String eldritchBlock(int variant) {
        EldritchVariant row = eldritchVariant(variant);
        return row == null ? ELDRITCH : row.blockId();
    }

    /**
     * The cosmetic solid block that hosts an original {@code BlockCosmeticSolid} metadata.
     *
     * <p>Oracle {@code BlockCosmeticSolid:213-253} gives the obsidian tiles a
     * hardness of thirty and the thaumium stones a hardness of four.
     */
    public static String cosmeticSolidBlock(int variant) {
        return switch (EldritchBlockRules.cosmeticGroup(variant)) {
            case 1 -> COSMETIC_SOLID_TOUGH;
            case 2 -> COSMETIC_SOLID_THAUMIUM;
            default -> COSMETIC_SOLID;
        };
    }

    /**
     * The airy block that hosts an original {@code BlockAiry} metadata.
     *
     * <p>Oracle {@code BlockAiry:190-213} gives the node, the holes and the
     * unbreakable airy block their own hardness.
     */
    public static String airyBlock(int variant) {
        return switch (EldritchBlockRules.airyGroup(variant)) {
            case 1 -> AIRY_NODE;
            case 2 -> AIRY_HOLE;
            case 3 -> AIRY_UNBREAKABLE;
            default -> AIRY;
        };
    }

    /**
     * The stone slab of an original {@code BlockCosmeticStoneSlab} metadata.
     * The original metadata zero is the arcane slab and one is the eldritch slab.
     */
    public static String stoneSlabBlock(int variant) {
        return slabVariant(variant) == EldritchBlockRules.SLAB_STONE_ELDRITCH
                ? COSMETIC_STONE_SLAB_ELDRITCH : COSMETIC_STONE_SLAB_ARCANE;
    }

    /* ------------------------------------------------------------------ */
    /* The original metadata decoding                                      */
    /* ------------------------------------------------------------------ */

    /** The slab variant of an original slab metadata: the lower three bits. */
    public static int slabVariant(int metadata) { return metadata & 7; }

    /** The upper half of an original slab metadata: the third bit. */
    public static boolean slabUpper(int metadata) { return (metadata & 8) != 0; }

    /** The facing of an original 1.7 stair metadata: the lower two bits. */
    public static Direction stairsFacing(int metadata) {
        return switch (metadata & 3) {
            case 0 -> Direction.EAST;
            case 1 -> Direction.WEST;
            case 2 -> Direction.SOUTH;
            default -> Direction.NORTH;
        };
    }

    /** The upside down half of an original 1.7 stair metadata: the third bit. */
    public static boolean stairsUpper(int metadata) { return (metadata & 4) != 0; }

    /**
     * The direction a banner of the eldritch altar faces.
     *
     * <p>Oracle {@code BlockWoodenDevice:264-290} puts the cloth of a wall
     * banner into the block quarter of its facing: the value zero hangs the
     * cloth on the north side, four on the east side, eight on the south side
     * and twelve on the west side. Oracle {@code WorldGenEldritchRing:103-120}
     * writes exactly those four values for the four banners around an altar.
     */
    public static Direction bannerDirection(int facing) {
        return switch (facing) {
            case 4 -> Direction.EAST;
            case 8 -> Direction.SOUTH;
            case 12 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    /**
     * The direction a crystal of the rooms points at.
     *
     * <p>The original stores the 1.7 {@code ForgeDirection} ordinal, which is
     * the same numbering as the 1.21.1 {@link Direction}.
     */
    public static Direction crystalDirection(int facing) {
        return switch (facing) {
            case 1 -> Direction.UP;
            case 2 -> Direction.NORTH;
            case 3 -> Direction.SOUTH;
            case 4 -> Direction.WEST;
            case 5 -> Direction.EAST;
            default -> Direction.DOWN;
        };
    }

    /** The rarity of a loot container, clamped into the native zero to two range. */
    public static int lootRarity(int metadata) { return Math.max(0, Math.min(2, metadata)); }

    /**
     * The native item of an original {@code ItemEldritchObject} metadata.
     *
     * <p>Oracle {@code ItemEldritchObject:31-48} carried the five metadata values zero to four
     * on one item, the eldritch eye, the crimson rites, the runed tablet, the primordial pearl
     * and the obelisk placer. The 1.21.1 port flattens the metadata into an item of its own,
     * so the permanent item of the key room (oracle {@code GenKeyRoom:97}, the metadata two)
     * is the runed tablet.
     */
    public static String eldritchObjectItem(int metadata) {
        return switch (metadata) {
            case 0 -> "thaumcraft:eldritch_eye";
            case 1 -> "thaumcraft:crimson_rites";
            case 2 -> "thaumcraft:eldritch_tablet";
            case 3 -> "thaumcraft:primordial_pearl";
            case 4 -> "thaumcraft:eldritch_placer";
            default -> "thaumcraft:eldritch_object";
        };
    }

    /* ------------------------------------------------------------------ */
    /* The eldritch gate                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * The radius of the gate search of the original teleporter.
     *
     * <p>Oracle {@code TeleporterThaumcraft:64-96} walks a square of one hundred and twenty
     * eight blocks around the player and keeps the closest eldritch gate.
     */
    public static final int PORTAL_SEARCH_RADIUS = 128;

    /* ------------------------------------------------------------------ */
    /* The altar of the eldritch ring                                       */
    /* ------------------------------------------------------------------ */
    /** The banner offset of an original {@code ForgeDirection} ordinal. */
    public static int[] bannerOffset(int forgeDirectionOrdinal) {
        return switch (forgeDirectionOrdinal) {
            case 2 -> new int[] {0, -3};  // north
            case 3 -> new int[] {0, 3};   // south
            case 4 -> new int[] {3, 0};   // west
            default -> new int[] {-3, 0}; // east
        };
    }

    /* ------------------------------------------------------------------ */
    /* The labyrinth of the eldritch altar                                 */
    /* ------------------------------------------------------------------ */

    /**
     * The size of the labyrinth an eldritch altar grows.
     *
     * <p>Oracle {@code TileEldritchAltar:217-226} draws {@code 15 + random.nextInt(8) * 2} for
     * the width and for the height, so the labyrinth is an odd number of chunks between fifteen
     * and twenty nine.
     */
    public static int altarMazeSize(int roll) { return 15 + roll * 2; }
}
