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
package thaumic.tinkerer.common.item.kami.tool;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/** The Ichorium Pickaxe; it never breaks and mines everything ({@link IchorTier}). */
public class ItemIchorPick extends PickaxeItem implements ITTinkererRegisterable {

    public ItemIchorPick(Item.Properties properties) {
        super(IchorTier.INSTANCE, properties);
    }

    /** The unbreakable component takes the damageable-ness away, and with it the enchanting table; 1.7.10 enchanted these tools. */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean shouldDisplayInTab() {
        return ConfigHandler.enableKami;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(LibResearch.KEY_ICHOR_PICK, LibResearch.KEY_ICHOR_TOOLS,
                new ItemStack(this), new AspectList().add(Aspect.FIRE, 75), "III", " R ", " R ", 'R',
                KamiResearch.tc("itemWandRod", 2), 'I', ItemKamiResource.stack(ItemKamiResource.ICHORIUM));
    }

    /** ICHOR_TOOLS: the research of the four plain tools (1.7.10: {@code ItemIchorPick.getResearchItem}). */
    public static TTResearchItem toolsResearch() {
        return KamiResearch.entry(LibResearch.KEY_ICHOR_TOOLS,
                new AspectList().add(Aspect.TOOL, 2).add(Aspect.WEAPON, 1).add(Aspect.METAL, 1).add(Aspect.CRAFT, 1),
                13, 12, TTRegistry.stack(LibItemNames.ICHOR_PICK), 2, KamiResearch.of(LibResearch.KEY_ICHORIUM),
                KamiResearch.of(LibResearch.KEY_ICHORCLOTH_ROD), new ResearchPage("0"),
                ResearchHelper.arcaneRecipePage(LibResearch.KEY_ICHOR_PICK),
                ResearchHelper.arcaneRecipePage(LibResearch.KEY_ICHOR_SHOVEL),
                ResearchHelper.arcaneRecipePage(LibResearch.KEY_ICHOR_AXE),
                ResearchHelper.arcaneRecipePage(LibResearch.KEY_ICHOR_SWORD));
    }
}
