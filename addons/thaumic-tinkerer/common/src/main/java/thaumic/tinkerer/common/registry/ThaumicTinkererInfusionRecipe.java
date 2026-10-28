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
import thaumcraft.api.crafting.InfusionRecipe;

public class ThaumicTinkererInfusionRecipe extends ThaumicTinkererRecipe {
    private final String name;
    private final String research;
    private final Object output;
    private final int instability;
    private final AspectList aspects;
    private final ItemStack input;
    private final ItemStack[] stuff;

    public ThaumicTinkererInfusionRecipe(String name, String research, Object output, int instability,
            AspectList aspects, ItemStack input, ItemStack... stuff) {
        this.name = name;
        this.research = research;
        this.output = output;
        this.instability = instability;
        this.aspects = aspects;
        this.input = input;
        this.stuff = stuff;
    }

    public ThaumicTinkererInfusionRecipe(String name, Object output, int instability, AspectList aspects,
            ItemStack input, ItemStack... stuff) {
        this(name, name, output, instability, aspects, input, stuff);
    }

    @Override
    public void registerRecipe() {
        InfusionRecipe recipe = ThaumcraftApi
                .addInfusionCraftingRecipe(research, output, instability, aspects, input, stuff);
        TTRecipes.put(name, recipe);
    }
}
