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
package thaumic.tinkerer.common.item.foci;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.core.helper.ProjectileHelper;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;
import org.jetbrains.annotations.Nullable;

/**
 * Wand Focus: Distortion. Held, it pushes every arrow and thrown object that comes near the player (and was not shot by
 * him) past him; costs 0.08 Ordo and 0.04 Aer per tick. The enlarge upgrade widens the reach.
 *
 * <p>The rule itself, the owner of a projectile and the blacklist are in {@link ProjectileHelper}.</p>
 */
public class ItemFocusDeflect extends ItemModFocus {
    /** The cost of a cast (or of a tick), in centivis. */
    public static final AspectList VIS_USAGE = new AspectList().add(Aspect.ORDER, 8).add(Aspect.AIR, 4);

    public ItemFocusDeflect(Item.Properties properties) {
        super(properties);
    }

    /** 1.7.10 {@code setupBlackList}: the experience bottle is on the blacklist of entity-type ids. */
    public static void setupBlackList() {
        ProjectileHelper.setupBlackList();
    }

    /**
     * 1.7.10 {@code protectFromProjectiles(player, stack)}: the wand's enlarge upgrade widens the box by one block per level;
     * without a wand (the Kami chestplate called it with {@code null}) the box has its base size.
     */
    public static void protectFromProjectiles(Player player, @Nullable ItemStack wand) {
        int range = wand == null ? 0 : WandHelper.getFocusEnlarge(wand);
        ProjectileHelper.protectFromProjectiles(player, range);
    }

    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focusstack, int rank) {
        return switch (rank) {
            case 1, 3, 5 -> new FocusUpgradeType[] { FocusUpgradeType.frugal };
            case 2, 4 -> new FocusUpgradeType[] { FocusUpgradeType.frugal, FocusUpgradeType.enlarge };
            default -> null;
        };
    }

    @Override
    public void onUsingFocusTick(ItemStack wandstack, Player player, int count) {
        if (WandHelper.consumeAllVis(wandstack, player, getVisCost(WandHelper.getFocusItem(wandstack)), true, false)) {
            protectFromProjectiles(player, wandstack);
        }
    }

    @Override
    public String getSortingHelper(ItemStack focusstack) {
        return "TTDF" + super.getSortingHelper(focusstack);
    }

    @Override
    public boolean isVisCostPerTick(ItemStack focusstack) {
        return true;
    }

    @Override
    public int getFocusColor(ItemStack focusstack) {
        return 0xFFFFFF;
    }

    @Override
    public AspectList getVisCost(ItemStack focusstack) {
        return VIS_USAGE.copy();
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        if (!ThaumcraftApi.isMirrorsAllowed()) {
            return null;
        }
        TTResearchItem research = new TTResearchItem(LibResearch.KEY_FOCUS_DEFLECT,
                new AspectList().add(Aspect.MOTION, 2).add(Aspect.AIR, 1).add(Aspect.ORDER, 1).add(Aspect.DEATH, 1), -4,
                -3, 3, new ItemStack(this));
        research.setConcealed().setParents(LibResearch.KEY_FOCUS_SMELT).setPages(new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_FOCUS_DEFLECT)).setSecondary();
        return research;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_FOCUS_DEFLECT, new ItemStack(this), 5,
                new AspectList().add(Aspect.AIR, 15).add(Aspect.ARMOR, 5).add(Aspect.ORDER, 20),
                TTRegistry.stack(LibItemNames.FOCUS_FLIGHT), tc("itemResource", 10), tc("itemResource", 10),
                tcBlock("blockCosmeticSolid", 3), tc("itemShard", 4));
    }
}
