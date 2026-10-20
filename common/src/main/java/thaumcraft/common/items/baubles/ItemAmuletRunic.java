package thaumcraft.common.items.baubles;

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.baubles.BaubleType;
import thaumcraft.common.config.ThaumcraftBaubles;
import thaumcraft.common.lib.baubles.RunicBaubleRules;
import thaumcraft.common.lib.baubles.RunicBaubleRules.Family;
import thaumcraft.common.lib.baubles.RunicBaubleRules.RunicAccessory;

/**
 * Original ItemAmuletRunic: the amulet accessory with the metadata 0 the runic amulet (a base charge of 8) and the metadata 1 the
 * emergency amulet (a base charge of 7). The original drew both from one item class with a damage value; the native tree keeps
 * the flattened variant {@link ItemRunicBauble} and this class keeps the original numbering, so a subsystem that still speaks the
 * original metadata (a recipe, a loot table, an equipment table) resolves it here instead of guessing.
 */
public final class ItemAmuletRunic {
    /** The original damage value of the runic amulet. */
    public static final int PLAIN = 0;
    /** The original damage value of the emergency runic amulet. */
    public static final int EMERGENCY = 1;

    private ItemAmuletRunic() {}

    /** The original variant of a metadata value. */
    public static RunicAccessory variant(int metadata) { return RunicBaubleRules.runic(Family.AMULET, metadata); }

    /** The original base runic charge: 8 for the runic amulet, 7 for the emergency one. */
    public static int getRunicCharge(int metadata) { return variant(metadata).charge(); }

    /** The original accessory slot: an amulet. */
    public static BaubleType getBaubleType() { return BaubleType.AMULET; }

    /** The original rarity: rare. */
    public static net.minecraft.world.item.Rarity getRarity(int metadata) { return variant(metadata).rarity(); }

    /** The registered native item of an original metadata value. */
    public static ItemStack stack(int metadata) {
        return new ItemStack(metadata == EMERGENCY ? ThaumcraftBaubles.RUNIC_AMULET_EMERGENCY.get() : ThaumcraftBaubles.RUNIC_AMULET.get());
    }
}
