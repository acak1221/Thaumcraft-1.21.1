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
package thaumic.tinkerer.common.research;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionEnchantmentRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.client.lib.LibResources;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.TTRecipes;

/** The research tab of the add-on and the page factories that look recipes up by name. */
public final class ResearchHelper {
    private static final List<IRegisterableResearch> QUEUE = new ArrayList<>();

    private ResearchHelper() {}

    /** Queues a research entry for {@link #flush()}; null is ignored. */
    public static void queue(IRegisterableResearch research) {
        if (research != null) QUEUE.add(research);
    }

    /**
     * Registers the queued entries: every ordinary entry first, then the Kami entries, whose requirements are all
     * the entries registered before them (see {@link KamiResearchItem}).
     */
    public static void flush() {
        List<IRegisterableResearch> kami = new ArrayList<>();
        for (IRegisterableResearch research : QUEUE) {
            if (research instanceof KamiResearchItem) kami.add(research);
            else research.registerResearch();
        }
        for (IRegisterableResearch research : kami) research.registerResearch();
        QUEUE.clear();
    }

    /** Registers the "Thaumic Tinkerer" tab (after the six original tabs, as in 1.7.10). Idempotent. */
    public static void registerCategory() {
        ResourceLocation background = ResourceLocation.fromNamespaceAndPath("thaumcraft",
                "textures/gui/gui_researchback.png");
        ResearchCategories.registerCategory(LibResearch.CATEGORY_THAUMICTINKERER,
                ResourceLocation.parse(LibResources.MISC_R_ENCHANTING), background);
    }

    /** A page that shows the crafting-table recipe {@code data/thaumictinkerer/recipe/<name>.json}. */
    public static ResearchPage recipePage(String name) {
        return ResearchPage.recipe(TTRecipes.dataRecipeId(name));
    }

    public static ResearchPage arcaneRecipePage(String name) {
        return new ResearchPage(TTRecipes.get(name, IArcaneRecipe.class));
    }

    public static ResearchPage infusionPage(String name) {
        return new ResearchPage(TTRecipes.get(name, InfusionRecipe.class));
    }

    /** One page cycling through the recipes registered as {@code name0}, {@code name1}... */
    public static ResearchPage infusionPage(String name, int count) {
        InfusionRecipe[] recipes = new InfusionRecipe[count];
        for (int i = 0; i < count; i++) {
            recipes[i] = TTRecipes.get(name + i, InfusionRecipe.class);
        }
        return new ResearchPage(recipes);
    }

    public static ResearchPage enchantPage(String name) {
        return new ResearchPage(TTRecipes.get(name, InfusionEnchantmentRecipe.class));
    }

    public static ResearchPage crucibleRecipePage(String name) {
        return new ResearchPage(TTRecipes.get(name, CrucibleRecipe.class));
    }
}
