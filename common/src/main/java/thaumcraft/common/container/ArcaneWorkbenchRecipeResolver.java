package thaumcraft.common.container;

import java.util.Map;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import thaumcraft.common.crafting.ArcaneCrafting;
import thaumcraft.common.crafting.ArcaneVis;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.crafting.DeviceContainerRules;

/**
 * The original arcane workbench recipe decision, in one place.
 *
 * <p>Oracle ContainerArcaneWorkbench:60-77: an ordinary crafting recipe wins
 * over an arcane one, an arcane recipe needs a wand in the wand slot, and the
 * wand pays the recipe vis before the result is handed out.</p>
 */
public final class ArcaneWorkbenchRecipeResolver {
    private ArcaneWorkbenchRecipeResolver() {}

    /** The resolved recipe of the current matrix. */
    public record Resolution(RecipeHolder<?> recipe, ItemStack output, Map<String, Integer> prices, boolean arcane) {
        public static Resolution none() { return new Resolution(null, ItemStack.EMPTY, Map.of(), false); }
        public boolean present() { return recipe != null && !output.isEmpty(); }
        public boolean needsWand() { return arcane; }
    }

    /** The recipe of the given matrix, or {@link Resolution#none()}. */
    public static Resolution resolve(Player player, Container contents) {
        if (player == null || contents == null) return Resolution.none();
        ArcaneCrafting.Match match = ArcaneCrafting.find(player, contents);
        if (match == null) return Resolution.none();
        return new Resolution(match.recipe(), match.output().copy(), match.prices(), match.arcane());
    }

    /**
     * The wand the native workbench accepts: a crafting wand that is not a
     * staff. See {@code ArcaneVis.isWorkbenchWand}.
     */
    public static boolean validWand(ItemStack stack) { return ArcaneVis.isWorkbenchWand(stack); }

    /**
     * The original wand slot rule of SlotLimitedByWand:29: any casting wand
     * that is not a staff. The native workbench additionally demands a rod
     * and a cap the wand data can hold, so the native rule is stricter.
     */
    public static boolean originalWandSlotAccepts(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemWandCasting)) return false;
        return DeviceContainerRules.limitedByWand(true, ItemWandCasting.isStaff(stack));
    }
}
