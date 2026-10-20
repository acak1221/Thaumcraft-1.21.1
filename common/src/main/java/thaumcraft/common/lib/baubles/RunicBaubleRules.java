package thaumcraft.common.lib.baubles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Rarity;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.common.items.baubles.ItemRunicBauble;

/**
 * The original accessory tables as pure data: the runic amulet/ring/girdle variants with their exact base runic charge, the nine
 * blank baubles with their primal vis discount, the ten cultist armour pieces with their protection, durability, rarity, vis
 * discount and warp, and the original ItemWandRod/ItemWandCap metadata with the rod and cap capability of that metadata.
 * No registry, world or ItemStack is touched here, so every original number stays unit testable.
 *
 * <p>Original sources: ItemAmuletRunic, ItemRingRunic, ItemGirdleRunic, ItemBaubleBlanks, ItemCultistBoots, ItemCultistRobeArmor,
 * ItemCultistPlateArmor, ItemCultistLeaderArmor, ItemWandRod, ItemWandCap, ConfigItems:301-334 and RecipesVoidRobeArmorDyes.
 */
public final class RunicBaubleRules {
    /** The original ItemWandRod and ItemWandCap are ordinary stackable parts; they never wear out. */
    public static final int WAND_PART_STACK_SIZE = 64;
    /** One whole vis in centivis: the original addVis(..., 1, true). */
    public static final int VIS_STEP = 100;
    /** The original WandRodPrimalOnUpdate of a fixed aspect rod: every two hundredth tick of the carrier. */
    public static final int FIXED_ASPECT_PERIOD = 200;
    /** The original WandRodPrimalOnUpdate of the primal staff: every fiftieth tick of the carrier. */
    public static final int ANY_PRIMAL_PERIOD = 50;
    /** The original primal staff rod: it refills any of the six primals, not a fixed one. */
    public static final String ANY_PRIMAL = "*";
    /** The original Aspect.getPrimalAspects() order, the order the blank bauble metadata 3..8 follows. */
    public static final List<String> PRIMALS = List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio");

    private RunicBaubleRules() {}

    // ---------------------------------------------------------------------------------------------------------------
    // The runic accessories: original ItemAmuletRunic, ItemRingRunic and ItemGirdleRunic.
    // ---------------------------------------------------------------------------------------------------------------

    /** The three original runic accessory classes; their metadata selects the variant inside each class. */
    public enum Family { AMULET, RING, GIRDLE }

    /**
     * One original runic accessory variant: the flattened id, the original metadata, the accessory slot it fits, the base runic
     * charge it contributes to the runic shield, its rarity and its upgrade kind.
     */
    public record RunicAccessory(String id, Family family, int metadata, BaubleType type, int charge, Rarity rarity,
                                  ItemRunicBauble.Upgrade upgrade) {}

    // The exact original values: the amulet 8 (7 for the emergency one), the lesser ring 1, the ring 5, the charged and the
    // healing ring 4, the girdle 10 (9 for the kinetic one). Only the lesser ring is uncommon.
    private static final List<RunicAccessory> RUNIC = List.of(
            new RunicAccessory("runic_amulet", Family.AMULET, 0, BaubleType.AMULET, 8, Rarity.RARE, ItemRunicBauble.Upgrade.NONE),
            new RunicAccessory("runic_amulet_emergency", Family.AMULET, 1, BaubleType.AMULET, 7, Rarity.RARE, ItemRunicBauble.Upgrade.EMERGENCY),
            new RunicAccessory("runic_ring_lesser", Family.RING, 0, BaubleType.RING, 1, Rarity.UNCOMMON, ItemRunicBauble.Upgrade.NONE),
            new RunicAccessory("runic_ring", Family.RING, 1, BaubleType.RING, 5, Rarity.RARE, ItemRunicBauble.Upgrade.NONE),
            new RunicAccessory("runic_ring_charged", Family.RING, 2, BaubleType.RING, 4, Rarity.RARE, ItemRunicBauble.Upgrade.CHARGED),
            new RunicAccessory("runic_ring_healing", Family.RING, 3, BaubleType.RING, 4, Rarity.RARE, ItemRunicBauble.Upgrade.HEALING),
            new RunicAccessory("runic_girdle", Family.GIRDLE, 0, BaubleType.BELT, 10, Rarity.RARE, ItemRunicBauble.Upgrade.NONE),
            new RunicAccessory("runic_girdle_kinetic", Family.GIRDLE, 1, BaubleType.BELT, 9, Rarity.RARE, ItemRunicBauble.Upgrade.KINETIC));

    public static List<RunicAccessory> runicAccessories() { return RUNIC; }

    /** The variant of an original runic accessory class; the metadata is the original 1.7.10 damage value. */
    public static RunicAccessory runic(Family family, int metadata) {
        for (RunicAccessory accessory : RUNIC) if (accessory.family() == family && accessory.metadata() == metadata) return accessory;
        throw new IllegalArgumentException("No runic " + family + " variant with metadata " + metadata);
    }

    /** The base runic charge of an original runic accessory: the runic shield points it adds before any hardening. */
    public static int runicCharge(Family family, int metadata) { return runic(family, metadata).charge(); }

    /** The accessory slot an original runic accessory fits: the amulet, a ring or the belt. */
    public static BaubleType slotType(Family family, int metadata) { return runic(family, metadata).type(); }

    /** The original rarity of a runic accessory: only the lesser ring is uncommon, every other one is rare. */
    public static Rarity runicRarity(Family family, int metadata) { return runic(family, metadata).rarity(); }

    /** The runic upgrade kind of a variant: the charged, the healing, the kinetic and the emergency one are the special rings. */
    public static ItemRunicBauble.Upgrade runicUpgrade(Family family, int metadata) { return runic(family, metadata).upgrade(); }

    // ---------------------------------------------------------------------------------------------------------------
    // The blank baubles: original ItemBaubleBlanks, metadata 0 the amulet, 1 the ring, 2 the belt and 3..8 the discount rings.
    // ---------------------------------------------------------------------------------------------------------------

    /** One original blank bauble: the flattened id, the original metadata, the slot it fits and the primal it discounts. */
    public record BlankBauble(String id, int metadata, BaubleType type, String discountAspect) {}

    private static List<BlankBauble> blanks() {
        List<BlankBauble> blanks = new ArrayList<>();
        blanks.add(new BlankBauble("mundane_amulet", 0, BaubleType.AMULET, null));
        blanks.add(new BlankBauble("mundane_ring", 1, BaubleType.RING, null));
        blanks.add(new BlankBauble("mundane_belt", 2, BaubleType.BELT, null));
        for (int index = 0; index < PRIMALS.size(); index++) {
            String primal = PRIMALS.get(index);
            blanks.add(new BlankBauble("discount_ring_" + primal, 3 + index, BaubleType.RING, primal));
        }
        return List.copyOf(blanks);
    }

    private static final List<BlankBauble> BLANKS = blanks();

    public static List<BlankBauble> blankBaubles() { return BLANKS; }

    /** The original blank bauble of a metadata value. */
    public static BlankBauble blank(int metadata) {
        for (BlankBauble blank : BLANKS) if (blank.metadata() == metadata) return blank;
        throw new IllegalArgumentException("No blank bauble with metadata " + metadata);
    }

    /**
     * The original vis discount of a blank bauble: one percent, but only for the primal the ring was made for. The mundane
     * amulet, ring and belt discount nothing.
     */
    public static int blankVisDiscount(int metadata, String aspectTag) {
        BlankBauble blank = blank(metadata);
        return blank.discountAspect() != null && blank.discountAspect().equals(aspectTag) ? 1 : 0;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The cultist armour: original ItemCultistRobeArmor, ItemCultistPlateArmor, ItemCultistLeaderArmor and ItemCultistBoots.
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * One cultist armour piece: the flattened id, the native slot, the protection, the toughness, the durability, the rarity, the
     * vis discount in percent and the warp the piece adds.
     */
    public record CultistPiece(String id, ArmorItem.Type type, int protection, int toughness, int durability, Rarity rarity,
                               int visDiscount, int warp) {}

    // The cultist robe, plate and boots use the vanilla iron material: protection 2/6/5/2, the iron durability factor 15 and
    // the iron enchantability 15. The Praetor uses armorMatThaumiumFortress: protection 3/7/6/3, the durability factor 40.
    // The original 1.7.10 has no armour toughness, so every piece keeps the native toughness of zero.
    private static final List<CultistPiece> CULTIST = List.of(
            new CultistPiece("cultist_robe_hood", ArmorItem.Type.HELMET, 2, 0, 165, Rarity.UNCOMMON, 1, 1),
            new CultistPiece("cultist_robe", ArmorItem.Type.CHESTPLATE, 6, 0, 240, Rarity.UNCOMMON, 1, 1),
            new CultistPiece("cultist_robe_legs", ArmorItem.Type.LEGGINGS, 5, 0, 225, Rarity.UNCOMMON, 1, 1),
            new CultistPiece("cultist_plate_helm", ArmorItem.Type.HELMET, 2, 0, 165, Rarity.UNCOMMON, 0, 0),
            new CultistPiece("cultist_plate", ArmorItem.Type.CHESTPLATE, 6, 0, 240, Rarity.UNCOMMON, 0, 0),
            new CultistPiece("cultist_plate_legs", ArmorItem.Type.LEGGINGS, 5, 0, 225, Rarity.UNCOMMON, 0, 0),
            new CultistPiece("cultist_leader_helm", ArmorItem.Type.HELMET, 3, 0, 440, Rarity.RARE, 0, 0),
            new CultistPiece("cultist_leader", ArmorItem.Type.CHESTPLATE, 7, 0, 640, Rarity.RARE, 0, 0),
            new CultistPiece("cultist_leader_legs", ArmorItem.Type.LEGGINGS, 6, 0, 600, Rarity.RARE, 0, 0),
            new CultistPiece("cultist_boots", ArmorItem.Type.BOOTS, 2, 0, 165, Rarity.UNCOMMON, 1, 1));

    public static List<CultistPiece> cultistArmor() { return CULTIST; }

    /** The cultist piece of a flattened id. */
    public static CultistPiece cultist(String id) {
        for (CultistPiece piece : CULTIST) if (piece.id().equals(id)) return piece;
        throw new IllegalArgumentException("No cultist armour piece: " + id);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The wand rods and caps: original ItemWandRod and ItemWandCap with their metadata, and the capability of that metadata.
    // ---------------------------------------------------------------------------------------------------------------

    /** One original wand rod metadata: the tag, the capacity in whole vis, the craft cost and the refilled primal. */
    public record Rod(int metadata, String tag, int capacity, int craftCost, String passive, boolean glowing, boolean runes) {}

    /** One original wand cap metadata: the tag, the cost modifier, the craft cost and the special aspects and modifier. */
    public record Cap(int metadata, String tag, float modifier, List<String> specialAspects, float specialModifier, int craftCost,
                      boolean charged) {}

    // The original ConfigItems:301-334. The wood rod is a plain stick and has no metadata of its own.
    private static final List<Rod> RODS = List.of(
            new Rod(-1, "wood", 25, 1, null, false, false),
            new Rod(0, "greatwood", 50, 3, null, false, false),
            new Rod(1, "obsidian", 75, 6, "terra", false, false),
            new Rod(2, "silverwood", 100, 9, null, false, false),
            new Rod(3, "ice", 75, 6, "aqua", false, false),
            new Rod(4, "quartz", 75, 6, "ordo", false, false),
            new Rod(5, "reed", 75, 6, "aer", false, false),
            new Rod(6, "blaze", 75, 6, "ignis", true, false),
            new Rod(7, "bone", 75, 6, "perditio", false, false),
            new Rod(50, "greatwood_staff", 125, 8, null, false, false),
            new Rod(51, "obsidian_staff", 175, 14, "terra", false, false),
            new Rod(52, "silverwood_staff", 250, 24, null, false, false),
            new Rod(53, "ice_staff", 175, 14, "aqua", false, false),
            new Rod(54, "quartz_staff", 175, 14, "ordo", false, false),
            new Rod(55, "reed_staff", 175, 14, "aer", false, false),
            new Rod(56, "blaze_staff", 175, 14, "ignis", true, false),
            new Rod(57, "bone_staff", 175, 14, "perditio", false, false),
            new Rod(100, "primal_staff", 250, 32, ANY_PRIMAL, false, true));

    // The original ConfigItems:301-304 and 328-334. The inert caps are the uncharged ones and the copper and the silver cap
    // only exist when another mod supplies that metal.
    private static final List<Cap> CAPS = List.of(
            new Cap(0, "iron", 1.1F, List.of(), 0F, 1, true),
            new Cap(1, "gold", 1.0F, List.of(), 0F, 3, true),
            new Cap(2, "thaumium", 0.9F, List.of(), 0F, 6, true),
            new Cap(3, "copper", 1.1F, List.of("ordo", "perditio"), 1.0F, 2, true),
            new Cap(4, "silver", 1.0F, List.of("aer", "terra", "ignis", "aqua"), 0.95F, 4, true),
            new Cap(5, "silver_inert", 0F, List.of(), 0F, 0, false),
            new Cap(6, "thaumium_inert", 0F, List.of(), 0F, 0, false),
            new Cap(7, "void", 0.8F, List.of(), 0F, 9, true),
            new Cap(8, "void_inert", 0F, List.of(), 0F, 0, false));

    public static List<Rod> rods() { return RODS; }

    public static List<Cap> caps() { return CAPS; }

    /** The original ItemWandRod metadata: the rod of that damage value. */
    public static Rod rod(int metadata) {
        for (Rod rod : RODS) if (rod.metadata() == metadata) return rod;
        throw new IllegalArgumentException("No wand rod with metadata " + metadata);
    }

    /** The original ItemWandCap metadata: the cap of that damage value. */
    public static Cap cap(int metadata) {
        for (Cap cap : CAPS) if (cap.metadata() == metadata) return cap;
        throw new IllegalArgumentException("No wand cap with metadata " + metadata);
    }

    /** The capacity of a rod metadata in whole vis; the wand stores a hundred times that in centivis. */
    public static int rodCapacity(int metadata) { return rod(metadata).capacity(); }

    /** The craft cost of a rod metadata: the arcane workbench vis of the rod alone. */
    public static int rodCraftCost(int metadata) { return rod(metadata).craftCost(); }

    /** The base cost modifier of a cap metadata: the factor every vis payment of the wand is multiplied with. */
    public static float capModifier(int metadata) { return cap(metadata).modifier(); }

    /** The craft cost of a cap metadata: the arcane workbench vis of the cap alone. */
    public static int capCraftCost(int metadata) { return cap(metadata).craftCost(); }

    /** The primal a rod metadata refills, or the ANY_PRIMAL wildcard of the primal staff, or nothing. */
    public static String rodPassive(int metadata) { return rod(metadata).passive(); }

    // ---------------------------------------------------------------------------------------------------------------
    // The primal rod refill: original WandRodPrimalOnUpdate.
    // ---------------------------------------------------------------------------------------------------------------

    /** The level below which a rod refills its aspect: a tenth of the wand's capacity, integer division. */
    public static int refillThreshold(int capacity) { return capacity / 10; }

    /**
     * The original WandRodPrimalOnUpdate of a fixed aspect rod: on every two hundredth tick of the carrier, if the wand holds
     * less than a tenth of its capacity of that aspect, one whole vis is added.
     *
     * @return the new amount of the refilled aspect
     */
    public static int fixedAspectRefill(int playerTick, int capacity, int amount, String aspect) {
        if (aspect == null || playerTick % FIXED_ASPECT_PERIOD != 0) return amount;
        if (amount >= refillThreshold(capacity)) return amount;
        return Math.min(capacity, amount + VIS_STEP);
    }

    /** The result of a primal staff refill: the chosen primal and its new amount, empty when nothing was due. */
    public record AnyPrimalRefill(String aspect, int amount) {
        public boolean due() { return aspect != null; }
    }

    /**
     * The original WandRodPrimalOnUpdate of the primal staff: on every fiftieth tick of the carrier one whole vis is added to
     * ONE primal that holds less than a tenth of the capacity, picked at random among those.
     *
     * @param pick {@code nextInt(bound)} of the carrier's random source, asked only when a primal is low
     */
    public static AnyPrimalRefill anyPrimalRefill(int playerTick, int capacity, Map<String, Integer> vis,
                                                   java.util.function.IntUnaryOperator pick) {
        if (playerTick % ANY_PRIMAL_PERIOD != 0) return new AnyPrimalRefill(null, 0);
        List<String> low = new ArrayList<>();
        for (String primal : PRIMALS) if (vis.getOrDefault(primal, 0) < refillThreshold(capacity)) low.add(primal);
        if (low.isEmpty()) return new AnyPrimalRefill(null, 0);
        String chosen = low.get(pick.applyAsInt(low.size()));
        return new AnyPrimalRefill(chosen, Math.min(capacity, vis.getOrDefault(chosen, 0) + VIS_STEP));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The void robe dye: original RecipesVoidRobeArmorDyes.
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * One contributor of the original robe dye arithmetic: the robe itself, whose colour is an integer RGB, or a vanilla dye,
     * whose colour is the sheep colour triple of EntitySheep.
     */
    public record DyeShare(boolean robe, float[] rgb) {
        public DyeShare { rgb = rgb.clone(); }
        @Override public float[] rgb() { return rgb.clone(); }
        /** The robe's own integer colour, converted exactly as the original divided the packed colour. */
        public static DyeShare robe(int rgb) {
            return new DyeShare(true, new float[]{(rgb >> 16 & 0xFF) / 255F, (rgb >> 8 & 0xFF) / 255F, (rgb & 0xFF) / 255F});
        }
        /** A vanilla dye, in the wool order of the original EntitySheep colours. */
        public static DyeShare dye(int wool) { return new DyeShare(false, SHEEP_COLORS[wool]); }
    }

    // Vanilla 1.7.10 EntitySheep colours: the sixteen dye colours of the wool blocks, in wool order.
    private static final float[][] SHEEP_COLORS = {
            {1F, 1F, 1F}, {.85F, .5F, .2F}, {.7F, .3F, .85F}, {.4F, .6F, .85F},
            {.9F, .9F, .2F}, {.5F, .8F, .1F}, {.95F, .5F, .65F}, {.3F, .3F, .3F},
            {.6F, .6F, .6F}, {.3F, .5F, .6F}, {.5F, .25F, .7F}, {.2F, .3F, .7F},
            {.4F, .3F, .2F}, {.4F, .5F, .2F}, {.6F, .2F, .2F}, {.1F, .1F, .1F}
    };

    /**
     * The original robe dye arithmetic: every contributor takes one share of the colour, the shares are averaged and then
     * scaled to the average brightness of all contributors. The order of the contributors is the order of the crafting grid,
     * because the original truncated the running sums.
     */
    public static int mixRobeColor(List<DyeShare> shares) {
        int red = 0, green = 0, blue = 0, brightness = 0, contributors = 0;
        for (DyeShare share : shares) {
            float r = share.rgb()[0], g = share.rgb()[1], b = share.rgb()[2];
            if (share.robe()) {
                // The original summed the float channels and truncated the running total.
                brightness = (int) ((float) brightness + Math.max(r, Math.max(g, b)) * 255F);
                red = (int) ((float) red + r * 255F);
                green = (int) ((float) green + g * 255F);
                blue = (int) ((float) blue + b * 255F);
            } else {
                // A dye channel is truncated to an integer first and then added.
                int ri = (int) (r * 255F), gi = (int) (g * 255F), bi = (int) (b * 255F);
                brightness += Math.max(ri, Math.max(gi, bi));
                red += ri; green += gi; blue += bi;
            }
            contributors++;
        }
        if (contributors == 0) return 0;
        int averageRed = red / contributors, averageGreen = green / contributors, averageBlue = blue / contributors;
        float averageBrightness = (float) brightness / (float) contributors;
        float maximum = Math.max(averageRed, Math.max(averageGreen, averageBlue));
        if (maximum == 0F) return 0;
        averageRed = (int) ((float) averageRed * averageBrightness / maximum);
        averageGreen = (int) ((float) averageGreen * averageBrightness / maximum);
        averageBlue = (int) ((float) averageBlue * averageBrightness / maximum);
        return (averageRed << 16) + (averageGreen << 8) + averageBlue;
    }

    /** The original blank bauble metadata of a flattened id, for the subsystems that keep the original numbering. */
    public static Map<String, Integer> blankMetadata() {
        Map<String, Integer> metadata = new LinkedHashMap<>();
        for (BlankBauble blank : BLANKS) metadata.put(blank.id(), blank.metadata());
        return Map.copyOf(metadata);
    }
}
