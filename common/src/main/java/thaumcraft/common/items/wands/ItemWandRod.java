package thaumcraft.common.items.wands;

import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.lib.baubles.RunicBaubleRules;
import thaumcraft.common.lib.baubles.RunicBaubleRules.Rod;

/**
 * Original ItemWandRod: the rod of a wand, one item class with a damage value. The metadata 0..7 are the greatwood, obsidian,
 * silverwood, ice, quartz, reed, blaze and bone rods, the metadata 50..57 the same eight as staff cores and the metadata 100 the
 * primal staff core. The mundane wood rod is a plain stick and has no metadata of its own. The native tree registers the
 * flattened rods; this class keeps the original numbering, the rod capability of that metadata and the resolution to the
 * registered item.
 */
public final class ItemWandRod {
    /** The original damage value of the greatwood rod. */
    public static final int GREATWOOD = 0;
    /** The original damage value of the obsidian rod. */
    public static final int OBSIDIAN = 1;
    /** The original damage value of the silverwood rod. */
    public static final int SILVERWOOD = 2;
    /** The original damage value of the ice rod. */
    public static final int ICE = 3;
    /** The original damage value of the quartz rod. */
    public static final int QUARTZ = 4;
    /** The original damage value of the reed rod. */
    public static final int REED = 5;
    /** The original damage value of the blaze rod. */
    public static final int BLAZE = 6;
    /** The original damage value of the bone rod. */
    public static final int BONE = 7;
    /** The original damage value of the greatwood staff core. */
    public static final int GREATWOOD_STAFF = 50;
    /** The original damage value of the obsidian staff core. */
    public static final int OBSIDIAN_STAFF = 51;
    /** The original damage value of the silverwood staff core. */
    public static final int SILVERWOOD_STAFF = 52;
    /** The original damage value of the ice staff core. */
    public static final int ICE_STAFF = 53;
    /** The original damage value of the quartz staff core. */
    public static final int QUARTZ_STAFF = 54;
    /** The original damage value of the reed staff core. */
    public static final int REED_STAFF = 55;
    /** The original damage value of the blaze staff core. */
    public static final int BLAZE_STAFF = 56;
    /** The original damage value of the bone staff core. */
    public static final int BONE_STAFF = 57;
    /** The original damage value of the primal staff core. */
    public static final int PRIMAL_STAFF = 100;

    private ItemWandRod() {}

    /** The original rod of a metadata value. */
    public static Rod variant(int metadata) { return RunicBaubleRules.rod(metadata); }

    /** The capacity of the rod metadata in whole vis; the wand stores a hundred times that in centivis. */
    public static int getCapacity(int metadata) { return variant(metadata).capacity(); }

    /** The craft cost of the rod metadata: the arcane workbench vis of the rod alone. */
    public static int getCraftCost(int metadata) { return variant(metadata).craftCost(); }

    /** The primal the rod metadata refills, the ANY_PRIMAL wildcard of the primal staff, or nothing. */
    public static String getPassiveAspect(int metadata) { return variant(metadata).passive(); }

    /** The original glowing rods: the blaze rod and the blaze staff core. */
    public static boolean isGlowing(int metadata) { return variant(metadata).glowing(); }

    /** The original runed rods: only the primal staff core. */
    public static boolean hasRunes(int metadata) { return variant(metadata).runes(); }

    /** The registered native item of an original metadata value. */
    public static ItemStack stack(int metadata) {
        Rod rod = variant(metadata);
        return ThaumcraftWands.rodItem(rod.tag());
    }
}
