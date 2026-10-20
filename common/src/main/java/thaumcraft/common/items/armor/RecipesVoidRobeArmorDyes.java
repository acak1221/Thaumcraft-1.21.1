package thaumcraft.common.items.armor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.common.config.ThaumcraftRunicBaubles;
import thaumcraft.common.lib.baubles.RunicBaubleRules;

/**
 * Original RecipesVoidRobeArmorDyes: the dyeing recipe of the void thaumaturge robes. It takes one void robe and any number of
 * vanilla dyes and mixes them: every contributor takes one share of the colour, the shares are averaged and then scaled to the
 * average brightness of all contributors. The original has an identical class for the enchanted robes; the native tree serves
 * both families with one recipe ({@link RecipesRobeArmorDyes}), this class keeps the void family on its own.
 */
public final class RecipesVoidRobeArmorDyes extends CustomRecipe {
    private static final List<Item> VANILLA_DYES = List.of(Items.WHITE_DYE, Items.ORANGE_DYE, Items.MAGENTA_DYE, Items.LIGHT_BLUE_DYE,
            Items.YELLOW_DYE, Items.LIME_DYE, Items.PINK_DYE, Items.GRAY_DYE, Items.LIGHT_GRAY_DYE, Items.CYAN_DYE,
            Items.PURPLE_DYE, Items.BLUE_DYE, Items.BROWN_DYE, Items.GREEN_DYE, Items.RED_DYE, Items.BLACK_DYE);

    public RecipesVoidRobeArmorDyes(CraftingBookCategory category) { super(category); }

    /** The wool index of a vanilla dye, or -1 when the item is not one of the sixteen original dyes. */
    private static int dyeIndex(ItemStack stack) {
        return stack.getItem() instanceof DyeItem ? VANILLA_DYES.indexOf(stack.getItem()) : -1;
    }

    /** The slot of the void robe, or -1 when the grid holds no robe, more than one robe or anything but dyes. */
    private static int robeSlot(CraftingInput input) {
        int robe = -1;
        boolean dye = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ItemVoidRobeArmor) {
                if (robe >= 0) return -1;
                robe = slot;
            } else if (dyeIndex(stack) >= 0) dye = true;
            else return -1;
        }
        return dye ? robe : -1;
    }

    @Override public boolean matches(CraftingInput input, Level level) { return robeSlot(input) >= 0; }

    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int robe = robeSlot(input);
        if (robe < 0) return ItemStack.EMPTY;
        ItemStack result = input.getItem(robe).copyWithCount(1);
        // The original walks the grid in order and truncates the running sums, so the order of the shares matters.
        List<RunicBaubleRules.DyeShare> shares = new ArrayList<>();
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (slot == robe) shares.add(RunicBaubleRules.DyeShare.robe(ItemRobeArmor.color(stack)));
            else shares.add(RunicBaubleRules.DyeShare.dye(dyeIndex(stack)));
        }
        ItemRobeArmor.setColor(result, RunicBaubleRules.mixRobeColor(shares));
        return result;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftRunicBaubles.VOID_ROBE_DYE_SERIALIZER.get(); }
}
