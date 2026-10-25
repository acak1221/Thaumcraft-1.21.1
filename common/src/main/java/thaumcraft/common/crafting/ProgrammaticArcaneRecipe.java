package thaumcraft.common.crafting;

import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import thaumcraft.api.crafting.ArcaneRecipes;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.config.ThaumcraftArcane;

/** Server runtime adapter: original virtual recipe methods remain authoritative. */
public record ProgrammaticArcaneRecipe(IArcaneRecipe original) implements NativeArcaneRecipe {
    @Override public String research() { return java.util.Objects.toString(original.getResearch(), ""); }
    @Override public Map<String, Integer> vis() { return ArcaneRecipes.vis(original.getAspects()); }
    @Override public Map<String, Integer> vis(ArcaneRecipeInput input) { return ArcaneRecipes.vis(original.getAspects(input)); }
    @Override public boolean hasResearch(ArcaneRecipeInput input, Player actor) { return original.matches(input, actor.level(), actor); }
    @Override public boolean matches(ArcaneRecipeInput input, Level level) { return original.matches(input, level, null); }
    @Override public ItemStack assemble(ArcaneRecipeInput input, HolderLookup.Provider registries) { return original.getCraftingResult(input).copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return original.getRecipeOutput().copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width > 0 && height > 0 && width * height >= original.getRecipeSize(); }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return ThaumcraftArcane.ARCANE_SHAPED_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ThaumcraftArcane.ARCANE_RECIPE_TYPE.get(); }
}
