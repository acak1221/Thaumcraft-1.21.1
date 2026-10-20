package thaumcraft.common.items.armor;

import net.minecraft.world.item.ItemStack;

/**
 * Armor that stands against magic damage. The original gave this through Forge's {@code ISpecialArmor.getProperties}: for a magic source
 * (and only then, in the void robes) a worn piece absorbs {@code armor / 35} of the damage in a first pass, ahead of every ordinary
 * piece. Vanilla 1.21.1 armor attributes already carry the ordinary {@code armor / 25} share, so only the magic pass is a hook here
 * ({@code MagicWard}).
 */
public interface MagicWardGear {
    /** The armor points the piece measures its magic ward by (the original damageReduceAmount). */
    int wardArmor(ItemStack stack);
}
