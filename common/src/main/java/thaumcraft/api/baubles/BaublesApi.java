package thaumcraft.api.baubles;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.lib.baubles.PlayerAccessories;

/**
 * Read access to what an entity wears in the four accessory slots (amulet, two rings, belt).
 *
 * <p>The Baubles mod that TC4 relied on had a {@code BaublesApi.getBaubles(player)} that answered the live four-slot
 * inventory. This port keeps the slots as its own, and this class answers the read-only half of that: what is worn, for
 * the "is X worn" checks of an add-on. Items go into a slot through the Baubles menu, a shift-click into it, or the
 * right-click hand equip (which an item can keep for itself, {@link IBauble#equipsOnUse}); they come out through the menu.
 * {@link IBauble#onEquipped} / {@link IBauble#onUnequipped} tell the item when that happens.</p>
 */
public final class BaublesApi {
    private BaublesApi() {}

    /**
     * The accessories the entity wears, in slot order (amulet, ring, ring, belt), without the empty slots.
     *
     * <p>The list cannot be modified and holds independent copies: changing a stack in it changes nothing the entity wears.
     * The stack an {@link IBauble} callback ({@link IBauble#onWornTick}) receives is the live one; use that one to change a
     * worn item. The answer is a snapshot, so ask again after the slots may have changed.</p>
     *
     * <p>Only a player has accessory slots: any other entity, and {@code null}, answers an empty list, and so does a player
     * whose saved slots could not be read. On a client the slots of the local player are known; those of other players are
     * not sent to it and read as empty. Cheap enough to ask every tick, and safe on either side.</p>
     *
     * @param entity the entity to ask, or {@code null}
     * @return the worn stacks, never {@code null}
     */
    public static List<ItemStack> getWorn(LivingEntity entity) {
        return entity instanceof Player player ? PlayerAccessories.worn(PlayerAccessories.get(player)) : List.of();
    }
}
