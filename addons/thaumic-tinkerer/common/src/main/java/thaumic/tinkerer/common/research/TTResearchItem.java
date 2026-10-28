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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.research.ResearchPage.PageType;
import thaumic.tinkerer.common.lib.LibResearch;

/**
 * A research entry of the "Thaumic Tinkerer" tab.
 *
 * <p>The name and text of the entry are the translations of {@code tc.research_name.<KEY>} and
 * {@code tc.research_text.<KEY>} (the native Thaumonomicon reads exactly these); the add-on's lang files carry the
 * {@code [TT]} prefix in the text. The text pages keep the 1.7.10 keys {@code ttresearch.page.<KEY>.<n>}.</p>
 */
public class TTResearchItem extends ResearchItem implements IRegisterableResearch {
    public int warp = 0;

    /** Set once the page texts got their key prefix, so a second {@link #setPages} does not prefix them again. */
    private boolean firstLoad = true;

    public TTResearchItem(String key) {
        super(key, LibResearch.CATEGORY_THAUMICTINKERER);
    }

    public TTResearchItem(String key, AspectList tags, int column, int row, int complexity, ItemStack icon) {
        super(key, LibResearch.CATEGORY_THAUMICTINKERER, tags, column, row, complexity, icon);
    }

    public TTResearchItem(String key, AspectList tags, int column, int row, int complexity, ResourceLocation icon) {
        super(key, LibResearch.CATEGORY_THAUMICTINKERER, tags, column, row, complexity, icon);
    }

    public TTResearchItem setWarp(int warp) {
        this.warp = warp;
        return this;
    }

    @Override
    public ResearchItem setPages(ResearchPage... pages) {
        for (ResearchPage page : pages) {
            if (page.type == PageType.TEXT && firstLoad) {
                page.text = "ttresearch.page." + key + "." + page.text;
            }
            if (checkInfusion() && page.type == PageType.INFUSION_CRAFTING) {
                // A page with an infusion recipe needs the infusion research first.
                if (parentsHidden == null || parentsHidden.length == 0) parentsHidden = new String[] { "INFUSION" };
                else {
                    String[] newParents = new String[parentsHidden.length + 1];
                    newParents[0] = "INFUSION";
                    System.arraycopy(parentsHidden, 0, newParents, 1, parentsHidden.length);
                    parentsHidden = newParents;
                }
            }
        }
        firstLoad = false;
        return super.setPages(pages);
    }

    boolean checkInfusion() {
        return true;
    }

    @Override
    public void registerResearch() {
        registerResearchItem();
        if (warp != 0) {
            ThaumcraftApi.addWarpToResearch(key, warp);
        }
    }
}
