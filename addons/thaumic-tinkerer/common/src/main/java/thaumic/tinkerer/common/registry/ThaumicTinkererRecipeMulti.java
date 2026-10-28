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
package thaumic.tinkerer.common.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThaumicTinkererRecipeMulti extends ThaumicTinkererRecipe {
    private final List<ThaumicTinkererRecipe> recipes;

    public ThaumicTinkererRecipeMulti(ThaumicTinkererRecipe... recipes) {
        this.recipes = new ArrayList<>(Arrays.asList(recipes));
    }

    public ThaumicTinkererRecipeMulti() {
        this.recipes = new ArrayList<>();
    }

    public void addRecipe(ThaumicTinkererRecipe recipe) {
        recipes.add(recipe);
    }

    @Override
    public void registerRecipe() {
        for (ThaumicTinkererRecipe recipe : recipes) {
            recipe.registerRecipe();
        }
    }
}
