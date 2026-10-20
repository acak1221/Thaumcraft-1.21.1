package thaumcraft.api.crafting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.ArcaneRecipeInput;

/**
 * The original TC4 arcane recipe contract.
 *
 * <p>Forced 1.21.1 changes: the original inventory became the native {@link ArcaneRecipeInput}, the world
 * became {@link Level} and the player became {@link Player}.</p>
 */
public interface IArcaneRecipe {
    boolean matches(ArcaneRecipeInput inventory, Level level, Player player);

    ItemStack getCraftingResult(ArcaneRecipeInput inventory);

    int getRecipeSize();

    ItemStack getRecipeOutput();

    AspectList getAspects();

    AspectList getAspects(ArcaneRecipeInput inventory);

    String getResearch();
}
