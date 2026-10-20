package thaumcraft.common.items.baubles;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.lib.baubles.RunicBaubleRules;
import thaumcraft.common.lib.baubles.RunicBaubleRules.Family;
import thaumcraft.common.lib.baubles.RunicBaubleRules.RunicAccessory;

/**
 * Original ItemRingRunic: the ring accessory with the metadata 0 the lesser ring (a base charge of 1, uncommon), 1 the runic
 * ring (5), 2 the charged ring (4) and 3 the healing ring (4). The native tree keeps the flattened variant
 * {@link ItemRunicBauble}; this class keeps the original numbering and resolves it.
 */
public final class ItemRingRunic {
    /** The original damage value of the lesser runic ring. */
    public static final int LESSER = 0;
    /** The original damage value of the runic ring. */
    public static final int PLAIN = 1;
    /** The original damage value of the charged runic ring. */
    public static final int CHARGED = 2;
    /** The original damage value of the healing runic ring. */
    public static final int HEALING = 3;

    private ItemRingRunic() {}

    /** The original variant of a metadata value. */
    public static RunicAccessory variant(int metadata) { return RunicBaubleRules.runic(Family.RING, metadata); }

    /** The original base runic charge: 1 for the lesser ring, 5 for the runic ring and 4 for the charged and the healing ring. */
    public static int getRunicCharge(int metadata) { return variant(metadata).charge(); }

    /** The original accessory slot: a ring. */
    public static BaubleType getBaubleType() { return BaubleType.RING; }

    /** The original rarity: only the lesser ring is uncommon, every other one is rare. */
    public static net.minecraft.world.item.Rarity getRarity(int metadata) { return variant(metadata).rarity(); }

    /** The registered native item of an original metadata value. */
    public static ItemStack stack(int metadata) {
        ItemStack stack = switch (metadata) {
            case LESSER -> new ItemStack(ThaumcraftBaubles.RUNIC_RING_LESSER.get());
            case PLAIN -> new ItemStack(ThaumcraftBaubles.RUNIC_RING.get());
            case CHARGED -> new ItemStack(ThaumcraftBaubles.RUNIC_RING_CHARGED.get());
            case HEALING -> new ItemStack(ThaumcraftBaubles.RUNIC_RING_HEALING.get());
            default -> throw new IllegalArgumentException("No runic ring with metadata " + metadata);
        };
        return stack;
    }
}
