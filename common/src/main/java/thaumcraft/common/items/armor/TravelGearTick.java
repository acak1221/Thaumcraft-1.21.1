package thaumcraft.common.items.armor;

import net.minecraft.world.entity.player.Player;

/** Original armor order is feet before chest. Existing shared accessory and
 * Haste/Repair/Runic loops remain single-owned and are not repeated here. */
public final class TravelGearTick {
    private TravelGearTick() {}
    public static void tick(Player player) {TravellerBoots.tick(player);Hover.handleHoverArmor(player);}
}
