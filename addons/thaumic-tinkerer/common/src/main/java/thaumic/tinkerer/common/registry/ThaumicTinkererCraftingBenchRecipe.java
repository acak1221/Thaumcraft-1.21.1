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

/**
 * A crafting-table recipe. 1.7.10 registered it with {@code GameRegistry.addRecipe}; in 1.21.1 recipes are data, so
 * the recipe itself is the JSON file {@code data/thaumictinkerer/recipe/<name>.json} and this object only makes the
 * name known to the research pages ({@code ResearchHelper.recipePage(name)}).
 */
public class ThaumicTinkererCraftingBenchRecipe extends ThaumicTinkererRecipe {
    private final String name;

    public ThaumicTinkererCraftingBenchRecipe(String name) {
        this.name = name;
    }

    @Override
    public void registerRecipe() {
        if (name != null && !name.isEmpty()) TTRecipes.put(name, TTRecipes.dataRecipeId(name));
    }
}
