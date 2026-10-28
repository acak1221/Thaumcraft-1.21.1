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
import thaumcraft.api.crafting.ShapedArcaneRecipe;

public class ThaumicTinkererArcaneRecipe extends ThaumicTinkererRecipe {
    private final Object[] stuff;
    public String name;
    public String research;
    public ItemStack output;
    public AspectList aspects;

    public ThaumicTinkererArcaneRecipe(String name, String research, ItemStack output, AspectList aspects,
            Object... stuff) {
        this.name = name;
        this.research = research;
        this.output = output;
        this.aspects = aspects;
        this.stuff = stuff;
    }

    @Override
    public void registerRecipe() {
        ShapedArcaneRecipe recipe = ThaumcraftApi.addArcaneCraftingRecipe(research, output, aspects, stuff);
        TTRecipes.put(name, recipe);
    }
}
