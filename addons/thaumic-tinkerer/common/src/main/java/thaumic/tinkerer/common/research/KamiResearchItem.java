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
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;
import thaumcraft.api.research.ResearchItem;
import thaumic.tinkerer.common.lib.LibResearch;

/**
 * A Kami research: concealed, and only available once every other research of Thaumcraft's basic tabs and of the
 * add-on is known.
 *
 * <p>1.7.10 collected those requirements in {@code setPages}, in the order the registry happened to visit its
 * classes. They are collected in {@link #registerResearch()} now, and the generic registration registers the Kami
 * entries after all others, so the requirement list is the same on every start.</p>
 */
public class KamiResearchItem extends TTResearchItem {
    public static List<String> Blacklist = new ArrayList<>();

    static {
        Blacklist.add("MINILITH");
    }

    public KamiResearchItem(String key, AspectList tags, int column, int row, int complexity, ItemStack icon) {
        super(key, tags, column, row, complexity, icon);
        setConcealed();
    }

    @Override
    public void registerResearch() {
        List<String> requirements = parentsHidden == null || parentsHidden.length == 0 ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(parentsHidden));
        if (!isAutoUnlock()) {
            for (ResearchCategoryList category : ResearchCategories.researchCategories.values()) {
                for (String tag : category.research.keySet()) {
                    ResearchItem research = category.research.get(tag);
                    if (research.isLost() || research.isVirtual() || research instanceof KamiResearchItem
                            || requirements.contains(tag))
                        continue;
                    if (research.category.equals(LibResearch.CATEGORY_THAUMICTINKERER)
                            || research.category.equals("BASICS") || research.category.equals("GOLEMANCY")
                            || research.category.equals("ARTIFICE") || research.category.equals("ALCHEMY")
                            || research.category.equals("THAUMATURGY")) {
                        boolean found = false;
                        for (String black : Blacklist) {
                            if (tag.startsWith(black)) found = true;
                        }
                        if (tag.endsWith("KAMI")) found = true;
                        if (found) continue;
                        requirements.add(tag);
                    }
                }
            }
        }
        parentsHidden = requirements.toArray(new String[0]);
        super.registerResearch();
    }

    @Override
    boolean checkInfusion() {
        return false;
    }
}
