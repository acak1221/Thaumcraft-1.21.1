package thaumcraft.client.renderers.item;

import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.items.wands.ItemWandCasting;

/**
 * Charge synchronisation changes the contents of a held wand, not the wand being equipped.
 * Vanilla's full component comparison lowers and raises the hand after every such slot update.
 */
public final class WandRenderIdentity {
    private WandRenderIdentity() {}

    /** Preserve vanilla equality except for the vis amounts of otherwise identical native wands. */
    public static boolean matches(ItemStack cached, ItemStack current) {
        if (ItemStack.matches(cached, current)) return true;
        if (cached.isEmpty() || current.isEmpty() || cached.getCount() != current.getCount()
                || cached.getItem() != current.getItem() || !(cached.getItem() instanceof ItemWandCasting)) return false;
        var before = ItemWandCasting.data(cached);
        var after = ItemWandCasting.data(current);
        if (!before.rod().equals(after.rod()) || !before.cap().equals(after.cap())) return false;
        // Normalise only this component's changing amounts. Focus, sceptre, enchantments,
        // names, component removals and every other item property retain vanilla equality.
        ItemStack comparison = cached.copy();
        if (current.has(ThaumcraftContent.WAND_DATA.get())) comparison.set(ThaumcraftContent.WAND_DATA.get(), after);
        else comparison.remove(ThaumcraftContent.WAND_DATA.get());
        return ItemStack.matches(comparison, current);
    }
}
