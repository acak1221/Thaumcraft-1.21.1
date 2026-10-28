/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block.tile.container.slot.kami;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumic.tinkerer.common.item.kami.ItemIchorPouch;

/**
 * A slot that refuses focus pouches ({@code !(item instanceof ItemFocusPouch)}): Thaumcraft's own pouch, recognised by its
 * id because the class is not API (request A2), and the Bottomless Pouch.
 */
public class SlotNoPouches extends Slot {
    private static final ResourceLocation FOCUS_POUCH = ResourceLocation.fromNamespaceAndPath("thaumcraft", "focus_pouch");

    public SlotNoPouches(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    /** True for a focus pouch of Thaumcraft or the Bottomless Pouch. */
    public static boolean isPouch(ItemStack stack) {
        return stack.getItem() instanceof ItemIchorPouch
                || FOCUS_POUCH.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !isPouch(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }
}
