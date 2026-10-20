package thaumcraft.common.items.armor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Armor with the original's ISpecialArmor first pass. Forge asked such a piece for its properties per damage source: for a magic source
 * it absorbed {@code armor / 35} of the damage, for the Thaumium Fortress armor also {@code armor / 20} for fire and explosions (all
 * ahead of the ordinary pieces, as priority 1), and for ordinary damage {@code armor / 25}, which vanilla 1.21.1 armor attributes already
 * do. So only the first pass is a hook here ({@code MagicWard}); a piece that took its share there is left out of vanilla's own armor
 * value for that damage (and a bonus to the ordinary share is added to it), see {@code MagicWard.armorDelta}.
 */
public interface MagicWardGear {
    /** The armor points the piece measures its ward by (the original damageReduceAmount). */
    int wardArmor(ItemStack stack);

    /** Whether the piece also guards against fire and explosions (the Fortress armor, not the robes). */
    default boolean wardsFire() { return false; }

    /** The multiplier on the piece's share (and on its ordinary armor): the Fortress armor's set bonus for a player, 1 otherwise. */
    default double wardFactor(ItemStack stack, LivingEntity wearer) { return 1.0; }
}
