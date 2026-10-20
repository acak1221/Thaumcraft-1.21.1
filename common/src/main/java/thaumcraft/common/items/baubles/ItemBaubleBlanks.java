package thaumcraft.common.items.baubles;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.lib.baubles.RunicBaubleRules;
import thaumcraft.common.lib.baubles.RunicBaubleRules.BlankBauble;

/**
 * Original ItemBaubleBlanks: the mundane accessories that a runic infusion turns into a runic one. The metadata 0 is the
 * amulet, 1 the ring, 2 the belt and 3..8 the six discount rings, one for each primal in the original primal order. The
 * discount rings give one percent off the vis of their own primal; the mundane three give nothing. The native tree keeps the
 * flattened variant {@link ItemBaubleBlank}; this class keeps the original numbering and resolves it.
 */
public final class ItemBaubleBlanks {
    /** The original damage value of the mundane amulet blank. */
    public static final int AMULET = 0;
    /** The original damage value of the mundane ring blank. */
    public static final int RING = 1;
    /** The original damage value of the mundane belt blank. */
    public static final int BELT = 2;
    /** The original damage value of the first discount ring, the air one. */
    public static final int FIRST_DISCOUNT_RING = 3;

    private ItemBaubleBlanks() {}

    /** The original blank of a metadata value. */
    public static BlankBauble variant(int metadata) { return RunicBaubleRules.blank(metadata); }

    /** The original accessory slot of a metadata value: the amulet, a ring or the belt. */
    public static BaubleType getBaubleType(int metadata) { return variant(metadata).type(); }

    /** The original base runic charge of every blank: zero, a blank only carries the infusion. */
    public static int getRunicCharge() { return 0; }

    /**
     * The original vis discount of a metadata value: one percent, but only for the primal the discount ring was made for.
     *
     * @param aspectTag the primal of the payment, or null for a general query
     */
    public static int getVisDiscount(int metadata, String aspectTag) {
        return RunicBaubleRules.blankVisDiscount(metadata, aspectTag);
    }

    /**
     * The original item colour of a metadata value: the colour of the primal the discount ring discounts, and the plain white of
     * the mundane amulet, ring and belt.
     */
    public static int color(int metadata) {
        BlankBauble blank = variant(metadata);
        if (blank.discountAspect() == null) return 0xFFFFFF;
        Aspect aspect = Aspect.getAspect(blank.discountAspect());
        return aspect == null ? 0xFFFFFF : aspect.getColor();
    }

    /** The registered native item of an original metadata value. */
    public static ItemStack stack(int metadata) {
        BlankBauble blank = variant(metadata);
        if (blank.discountAspect() == null) {
            return new ItemStack(switch (metadata) {
                case AMULET -> ThaumcraftBaubles.MUNDANE_AMULET.get();
                case RING -> ThaumcraftBaubles.MUNDANE_RING.get();
                case BELT -> ThaumcraftBaubles.MUNDANE_BELT.get();
                default -> throw new IllegalArgumentException("No blank bauble with metadata " + metadata);
            });
        }
        var discount = ThaumcraftBaubles.DISCOUNT_RINGS.get(blank.discountAspect());
        if (discount == null) throw new IllegalStateException("Unregistered discount ring: " + blank.discountAspect());
        return new ItemStack(discount.get());
    }
}
