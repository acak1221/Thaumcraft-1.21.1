package thaumcraft.common.items.wands;

import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftWands;
import thaumcraft.common.lib.baubles.RunicBaubleRules;
import thaumcraft.common.lib.baubles.RunicBaubleRules.Cap;

/**
 * Original ItemWandCap: the cap of a wand, one item class with a damage value. The metadata 0 is the iron cap, 1 the gold one,
 * 2 the thaumium one, 3 the copper one and 4 the silver one, which only exist when another mod supplies that metal, 5 the inert
 * silver cap, 6 the inert thaumium cap, 7 the void metal cap and 8 the inert void metal cap. The native tree registers the
 * flattened caps; this class keeps the original numbering, the cap capability of that metadata and the resolution to the
 * registered item.
 */
public final class ItemWandCap {
    /** The original damage value of the iron cap. */
    public static final int IRON = 0;
    /** The original damage value of the gold cap. */
    public static final int GOLD = 1;
    /** The original damage value of the thaumium cap. */
    public static final int THAUMIUM = 2;
    /** The original damage value of the copper cap; copper is vanilla in 1.21.1, so the cap is registered. */
    public static final int COPPER = 3;
    /** The original damage value of the silver cap; it needs another mod's silver ingot. */
    public static final int SILVER = 4;
    /** The original damage value of the inert silver cap. */
    public static final int SILVER_INERT = 5;
    /** The original damage value of the inert thaumium cap. */
    public static final int THAUMIUM_INERT = 6;
    /** The original damage value of the void metal cap. */
    public static final int VOID = 7;
    /** The original damage value of the inert void metal cap. */
    public static final int VOID_INERT = 8;

    private ItemWandCap() {}

    /** The original cap of a metadata value. */
    public static Cap variant(int metadata) { return RunicBaubleRules.cap(metadata); }

    /** The base cost modifier of the cap metadata: the factor every vis payment of the wand is multiplied with. */
    public static float getBaseCostModifier(int metadata) { return variant(metadata).modifier(); }

    /** The craft cost of the cap metadata: the arcane workbench vis of the cap alone. */
    public static int getCraftCost(int metadata) { return variant(metadata).craftCost(); }

    /** The special cost modifier aspects of the cap metadata, empty for the four ordinary caps. */
    public static java.util.List<String> getSpecialCostModifierAspects(int metadata) { return variant(metadata).specialAspects(); }

    /** The special cost modifier of the cap metadata, the factor used for its special aspects. */
    public static float getSpecialCostModifier(int metadata) { return variant(metadata).specialModifier(); }

    /** The original charged caps: the inert ones are the raw, uncharged caps. */
    public static boolean isCharged(int metadata) { return variant(metadata).charged(); }

    /**
     * The registered native item of an original metadata value.
     *
     * @throws IllegalStateException when the cap needs another mod's silver ingot and is therefore not registered
     */
    public static ItemStack stack(int metadata) {
        return switch (metadata) {
            case IRON -> ThaumcraftWands.capItem("iron");
            case GOLD -> ThaumcraftWands.capItem("gold");
            case THAUMIUM -> ThaumcraftWands.capItem("thaumium");
            case COPPER -> ThaumcraftWands.capItem("copper");
            case THAUMIUM_INERT -> new ItemStack(ThaumcraftWands.THAUMIUM_WAND_CAP_INERT.get());
            case VOID -> ThaumcraftWands.capItem("void");
            case VOID_INERT -> new ItemStack(ThaumcraftWands.VOID_WAND_CAP_INERT.get());
            case SILVER, SILVER_INERT -> throw new IllegalStateException(
                    "The " + variant(metadata).tag() + " cap needs another mod's silver ingot");
            default -> throw new IllegalArgumentException("No wand cap with metadata " + metadata);
        };
    }
}
