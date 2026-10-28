/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [9 Sep 2013, 01:20:26 (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import thaumic.tinkerer.api.INoRemoveEnchant;
import thaumic.tinkerer.common.content.BasicItems;

/**
 * The recipe of the Spellbinding Cloth: exactly one cloth and one enchanted item (and nothing else) in the grid give that
 * item without its enchantments. An item whose class implements {@link INoRemoveEnchant} is not accepted. The enchanted
 * item is used up and the cloth stays, worn by one use ({@link SpellClothCraftingHandler#wornCloth}).
 *
 * <p>Only the enchantments of the item count, as in 1.7.10 ({@code ench}); the enchantments a book stores are not wiped
 * (and 1.21.1's {@code ItemStack.isEnchanted} agrees).</p>
 */
public class SpellClothRecipe extends CustomRecipe {

    public SpellClothRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static boolean isWipeable(ItemStack stack) {
        return stack.isEnchanted() && !(stack.getItem() instanceof INoRemoveEnchant);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean foundCloth = false;
        boolean foundEnchanted = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (isWipeable(stack) && !foundEnchanted) foundEnchanted = true;
            else if (stack.getItem() instanceof ItemSpellCloth && !foundCloth) foundCloth = true;
            else return false; // Found an invalid item, breaking the recipe
        }

        return foundCloth && foundEnchanted;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (isWipeable(stack)) {
                // One item comes out for the one the grid gives up (a stack of enchanted items is not wiped as a whole).
                ItemStack result = stack.copyWithCount(1);
                result.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof ItemSpellCloth) remaining.set(i, SpellClothCraftingHandler.wornCloth(stack));
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BasicItems.SPELL_CLOTH_SERIALIZER.get();
    }
}
