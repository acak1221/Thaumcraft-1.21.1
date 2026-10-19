package thaumcraft.common.items.armor;

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
import thaumcraft.common.config.ThaumcraftWearables;

/** Original RecipesRobeArmorDyes:29-111, adapted to a native special recipe.
 * OpenTC4 supplies readable names only; the original JAR defines the calculation. */
public final class RecipesRobeArmorDyes extends CustomRecipe {
    private static final List<Item> VANILLA_DYES = List.of(Items.WHITE_DYE, Items.ORANGE_DYE, Items.MAGENTA_DYE, Items.LIGHT_BLUE_DYE,
            Items.YELLOW_DYE, Items.LIME_DYE, Items.PINK_DYE, Items.GRAY_DYE, Items.LIGHT_GRAY_DYE, Items.CYAN_DYE,
            Items.PURPLE_DYE, Items.BLUE_DYE, Items.BROWN_DYE, Items.GREEN_DYE, Items.RED_DYE, Items.BLACK_DYE);
    // Vanilla1.7 wp.bp / EntitySheep.field_70898_d, static initializer0-348.
    // Wool-ID order; original dye metadata maps through (~metadata)&15.
    // These are Java FLOAT triples, NOT modern DyeColor texture/diffuse colors.
    private static final float[][] SHEEP_COLORS = {
            {1F, 1F, 1F}, {.85F, .5F, .2F}, {.7F, .3F, .85F}, {.4F, .6F, .85F},
            {.9F, .9F, .2F}, {.5F, .8F, .1F}, {.95F, .5F, .65F}, {.3F, .3F, .3F},
            {.6F, .6F, .6F}, {.3F, .5F, .6F}, {.5F, .25F, .7F}, {.2F, .3F, .7F},
            {.4F, .3F, .2F}, {.4F, .5F, .2F}, {.6F, .2F, .2F}, {.1F, .1F, .1F}
    };

    public RecipesRobeArmorDyes(CraftingBookCategory category) { super(category); }

    private static int dyeIndex(ItemStack stack) {
        // Exact sixteen actual vanilla dye items, never arbitrary DyeItem
        // subclasses, c:dyestags, wool, flowers or modern color providers.
        return stack.getItem() instanceof DyeItem ? VANILLA_DYES.indexOf(stack.getItem()) : -1;
    }

    private static int robeSlot(CraftingInput input) {
        int robe = -1;
        boolean dye = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ItemRobeArmor) {
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
        int red = 0, green = 0, blue = 0, brightness = 0, contributors = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (slot == robe) {
                int color = ItemRobeArmor.color(stack);
                float r = (float) (color >> 16 & 255) / 255F;
                float g = (float) (color >> 8 & 255) / 255F;
                float b = (float) (color & 255) / 255F;
                // Preserve the original float additions/truncations, including
                // grid order when dye slots precede the robe's color contribution.
                brightness = (int) ((float) brightness + Math.max(r, Math.max(g, b)) * 255F);
                red = (int) ((float) red + r * 255F);
                green = (int) ((float) green + g * 255F);
                blue = (int) ((float) blue + b * 255F);
            } else {
                float[] dye = SHEEP_COLORS[dyeIndex(stack)];
                int r = (int) (dye[0] * 255F), g = (int) (dye[1] * 255F), b = (int) (dye[2] * 255F);
                brightness += Math.max(r, Math.max(g, b));
                red += r; green += g; blue += b;
            }
            contributors++; // One per OCCUPIED SLOT, not per dye stack item.
        }
        red /= contributors; green /= contributors; blue /= contributors;
        float averageBrightness = (float) brightness / (float) contributors;
        float maximum = Math.max(red, Math.max(green, blue));
        red = (int) ((float) red * averageBrightness / maximum);
        green = (int) ((float) green * averageBrightness / maximum);
        blue = (int) ((float) blue * averageBrightness / maximum);
        ItemRobeArmor.setColor(result, (red << 16) + (green << 8) + blue);
        return result;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftWearables.ROBE_DYE_RECIPE_SERIALIZER.get(); }
}
