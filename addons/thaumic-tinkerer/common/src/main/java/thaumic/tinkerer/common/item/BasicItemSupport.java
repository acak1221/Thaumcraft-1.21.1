/**
 * New in the 1.21.1 port of Thaumic Tinkerer (no 1.7.10 counterpart): small helpers shared by the items of the
 * BasicItems group. Distributed under the same licence as the add-on (CC BY-NC-SA 3.0, see
 * addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Sounds of Thaumcraft by id, and the bottle search of the Experience Talisman. */
public final class BasicItemSupport {
    private BasicItemSupport() {}

    /**
     * Plays the Thaumcraft sound (one of the constants of {@code ThaumcraftSoundIds}) at the entity for everybody near
     * (server side). 1.7.10 played them by name in {@code playSoundAtEntity}; a sound that is not registered is skipped
     * silently, so the item still works when a resource pack or a stripped build lacks it.
     */
    public static void playThaumcraftSound(Level level, Entity at, ResourceLocation id, float volume, float pitch) {
        if (level.isClientSide) return;
        BuiltInRegistries.SOUND_EVENT.getOptional(id).ifPresent(sound -> level.playSound(null, at.getX(), at.getY(),
                at.getZ(), sound, SoundSource.PLAYERS, volume, pitch));
    }

    /** Plays a sound at the entity for everybody near (server side). */
    public static void playSound(Level level, Entity at, SoundEvent sound, float volume, float pitch) {
        if (level.isClientSide) return;
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /**
     * The stack of the item in the main inventory (hotbar included) or the off hand, the first one found, or an empty stack.
     * 1.7.10's {@code consumeInventoryItem} looked into the main inventory, which had no off hand yet.
     */
    public static ItemStack findInInventory(Player player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) return stack;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
