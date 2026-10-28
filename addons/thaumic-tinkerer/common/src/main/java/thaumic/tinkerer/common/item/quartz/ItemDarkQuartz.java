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
package thaumic.tinkerer.common.item.quartz;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererCraftingBenchRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Smokey Quartz item and the research entry {@code DARK_QUARTZ} of the whole dark quartz family.
 *
 * <p>1.7.10 named the six crafting recipes of the family {@code DARK_QUARTZ + n} (n = 0..5) and showed them on the pages
 * of the entry. A recipe name is also the file name of the data recipe in 1.21.1, which must be lower case, so the
 * names are {@code dark_quartz_n}; the recipe of the item is number 0, the blocks register 1, 3 and 4, the slab 2 and
 * the stairs 5, exactly as before.</p>
 */
public class ItemDarkQuartz extends Item implements ITTinkererRegisterable {

    /** The names of the six crafting recipes ({@code data/thaumictinkerer/recipe/<name>.json}). */
    public static final String[] RECIPE_NAMES = { "dark_quartz_0", "dark_quartz_1", "dark_quartz_2", "dark_quartz_3",
            "dark_quartz_4", "dark_quartz_5" };

    public ItemDarkQuartz(Properties properties) {
        super(properties);
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        // the 1.7.10 code also called registerResearchItem() on the entry here and registered it a second time later
        return (IRegisterableResearch) new TTResearchItem(
                LibResearch.KEY_DARK_QUARTZ,
                new AspectList(),
                -2,
                2,
                0,
                new ItemStack(this))
                        .setPages(
                                new ResearchPage("0"),
                                ResearchHelper.recipePage(RECIPE_NAMES[0]),
                                ResearchHelper.recipePage(RECIPE_NAMES[1]),
                                ResearchHelper.recipePage(RECIPE_NAMES[2]),
                                ResearchHelper.recipePage(RECIPE_NAMES[3]),
                                ResearchHelper.recipePage(RECIPE_NAMES[4]),
                                ResearchHelper.recipePage(RECIPE_NAMES[5]))
                        .setStub().setAutoUnlock().setRound();
    }

    /** Both recipes of 1.7.10 (coal and charcoal) are one data recipe with a two-item ingredient. */
    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererCraftingBenchRecipe(RECIPE_NAMES[0]);
    }
}
