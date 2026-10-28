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

import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;

public class ThaumicTinkererCrucibleRecipe extends ThaumicTinkererRecipe {
    private final String name;
    private final ItemStack output;
    private final ItemStack input;
    private final AspectList aspects;

    public ThaumicTinkererCrucibleRecipe(String name, ItemStack output, ItemStack input, AspectList aspects) {
        this.name = name;
        this.output = output;
        this.input = input;
        this.aspects = aspects;
    }

    @Override
    public void registerRecipe() {
        CrucibleRecipe recipe = ThaumcraftApi.addCrucibleRecipe(name, output, input, aspects);
        TTRecipes.put(name, recipe);
    }
}
