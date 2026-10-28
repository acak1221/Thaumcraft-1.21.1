/**
 * Port of Thaumic Tinkerer's {@code SpellClothCraftingHandler} (a class without a licence header in the GTNewHorizons fork
 * of 1.7.10). Thaumic Tinkerer is distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 License
 * (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB); it is a Derivative Work on Thaumcraft 4.
 * Thaumcraft 4 (c) Azanor 2012 (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import net.minecraft.world.item.ItemStack;

/**
 * What a cleaning does to the cloth. 1.7.10 did it in an {@code ItemCraftedEvent} handler: it emptied the slot of the
 * enchanted item and added 1 to the damage of the cloth, and the crafting slot then handed the cloth back as the container
 * item, destroying it when its damage was above its maximum. 1.21.1 asks the recipe for the stacks that stay in the grid
 * ({@link SpellClothRecipe#getRemainingItems}), so the same rule is a function here, and no event handler is needed.
 */
public final class SpellClothCraftingHandler {
    private SpellClothCraftingHandler() {}

    /**
     * The cloth that stays in the grid after one cleaning: one more point of damage, or nothing when that would put the damage
     * above the maximum (the 1.7.10 rule, so the cloth lasts {@code maxDamage + 1} cleanings).
     */
    public static ItemStack wornCloth(ItemStack cloth) {
        int damage = cloth.getDamageValue() + 1;
        if (damage > cloth.getMaxDamage()) return ItemStack.EMPTY;
        ItemStack worn = cloth.copyWithCount(1);
        worn.setDamageValue(damage);
        return worn;
    }
}
