/**
 * Port of Thaumic Tinkerer's {@code ItemInfusedInkwell} (a class without a licence header in the GTNewHorizons fork of
 * 1.7.10). Thaumic Tinkerer is distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 License
 * (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB); it is a Derivative Work on Thaumcraft 4.
 * Thaumcraft 4 (c) Azanor 2012 (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IScribeTools;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererCraftingBenchRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipeMulti;
import thaumic.tinkerer.common.research.IRegisterableResearch;

/**
 * The Infused Scribing Tools: scribing tools with 800 points of ink (Thaumcraft's own have 100), which the research table
 * takes in its ink slot because the item is an {@link IScribeTools}. The ink is the durability. It shines like an enchanted
 * item and has no research and no place in the creative tab: the item is unfinished in 1.7.10 too (its name ends in
 * "(NYI)"), so the infusion recipe stays under a research that does not exist.
 */
public class ItemInfusedInkwell extends Item implements IScribeTools, ITTinkererRegisterable {
    /** The ink the tools hold. */
    public static final int INK = 800;
    /** The name of the crafting-table recipe that refills the tools with eight Ink Sacs ({@code data/thaumictinkerer/recipe}). */
    public static final String REFILL_RECIPE = "infused_inkwell0";

    public ItemInfusedInkwell(Item.Properties properties) {
        super(properties.durability(INK).stacksTo(1));
    }

    @Override
    public boolean shouldDisplayInTab() {
        return false;
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        return null;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererRecipeMulti(
                // "QQQ", "QCQ", "QQQ": eight Ink Sacs around tools of any wear give a full set (the file infused_inkwell0.json)
                new ThaumicTinkererCraftingBenchRecipe(REFILL_RECIPE),
                new ThaumicTinkererInfusionRecipe(
                        LibResearch.KEY_INFUSED_INKWELL,
                        new ItemStack(this),
                        2,
                        new AspectList().add(Aspect.VOID, 8).add(Aspect.DARKNESS, 8),
                        ItemApi.getItem("itemInkwell", 0),
                        ItemApi.getItem("itemShard", 0),
                        ItemApi.getBlock("blockJar", 0),
                        ItemApi.getItem("itemResource", 3)));
    }

    /** 1.7.10 {@code hasEffect}: always. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
