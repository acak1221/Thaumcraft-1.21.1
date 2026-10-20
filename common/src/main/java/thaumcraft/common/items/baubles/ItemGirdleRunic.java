package thaumcraft.common.items.baubles;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.lib.baubles.RunicBaubleRules;
import thaumcraft.common.lib.baubles.RunicBaubleRules.Family;
import thaumcraft.common.lib.baubles.RunicBaubleRules.RunicAccessory;

/**
 * Original ItemGirdleRunic: the belt accessory with the metadata 0 the runic girdle (a base charge of 10) and the metadata 1 the
 * kinetic girdle (9). The native tree keeps the flattened variant {@link ItemRunicBauble}; this class keeps the original
 * numbering and resolves it.
 */
public final class ItemGirdleRunic {
    /** The original damage value of the runic girdle. */
    public static final int PLAIN = 0;
    /** The original damage value of the kinetic runic girdle. */
    public static final int KINETIC = 1;

    private ItemGirdleRunic() {}

    /** The original variant of a metadata value. */
    public static RunicAccessory variant(int metadata) { return RunicBaubleRules.runic(Family.GIRDLE, metadata); }

    /** The original base runic charge: 10 for the runic girdle, 9 for the kinetic one. */
    public static int getRunicCharge(int metadata) { return variant(metadata).charge(); }

    /** The original accessory slot: the belt. */
    public static BaubleType getBaubleType() { return BaubleType.BELT; }

    /** The original rarity: rare. */
    public static net.minecraft.world.item.Rarity getRarity(int metadata) { return variant(metadata).rarity(); }

    /** The registered native item of an original metadata value. */
    public static ItemStack stack(int metadata) {
        return new ItemStack(metadata == KINETIC ? ThaumcraftBaubles.RUNIC_GIRDLE_KINETIC.get() : ThaumcraftBaubles.RUNIC_GIRDLE.get());
    }
}
