package thaumcraft.common.items.wands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftWands;

/**
 * The item of a wand part, native or add-on, and the parts a grid stack is.
 *
 * <p>Original ArcaneWandRecipe:43-57 and ArcaneSceptreRecipe:47-61 walk {@code WandCap.caps} and {@code WandRod.rods} in
 * registry order and compare the grid stack with {@code getItem()} of every part. The native parts come first (the original
 * registered them first, ConfigItems:292-334); their items are the canonical native stacks. An add-on part without an item
 * (null or empty) is never matched.</p>
 */
public final class WandPartItems {
    private WandPartItems() {}

    /** A fresh native rod stack, or the add-on's live registered stack (empty when it has none). */
    public static ItemStack rodItem(WandMaterials.Rod rod) {
        if (!rod.isAddon()) return ThaumcraftWands.rodItem(rod.tag());
        ItemStack item = rod.addon().getItem();
        return item == null ? ItemStack.EMPTY : item;
    }

    /** A fresh native cap stack, or the add-on's live registered stack (empty when it has none). */
    public static ItemStack capItem(WandMaterials.Cap cap) {
        if (!cap.isAddon()) return ThaumcraftWands.capItem(cap.tag());
        ItemStack item = cap.addon().getItem();
        return item == null ? ItemStack.EMPTY : item;
    }

    /** Every rod (native first, then add-ons in registry order) whose item {@code equals(gridStack, item)} accepts. */
    public static List<WandMaterials.Rod> rods(ItemStack gridStack, BiPredicate<ItemStack, ItemStack> equals) {
        List<WandMaterials.Rod> matches = new ArrayList<>();
        if (gridStack.isEmpty()) return matches;
        for (var rod : WandMaterials.allRods()) {
            ItemStack item = rodItem(rod);
            if (!item.isEmpty() && equals.test(gridStack, item)) matches.add(rod);
        }
        return matches;
    }

    /** Every cap (native first, then add-ons in registry order) whose item {@code equals(gridStack, item)} accepts. */
    public static List<WandMaterials.Cap> caps(ItemStack gridStack, BiPredicate<ItemStack, ItemStack> equals) {
        List<WandMaterials.Cap> matches = new ArrayList<>();
        if (gridStack.isEmpty()) return matches;
        for (var cap : WandMaterials.allCaps()) {
            ItemStack item = capItem(cap);
            if (!item.isEmpty() && equals.test(gridStack, item)) matches.add(cap);
        }
        return matches;
    }
}
