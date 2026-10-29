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

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Wand Focus: Ender Rift. A cast opens the ender chest of the player, at 1 Perditio and 1 Ordo.
 *
 * <p>The window is the vanilla one ({@code ChestMenu} of three rows over the player's ender chest). The EnderStorage
 * branch of 1.7.10 (a different mod's chests) is not ported: that mod does not exist for 1.21.1.</p>
 */
public class ItemFocusEnderChest extends ItemModFocus {
    public static final AspectList VIS_USAGE = new AspectList().add(Aspect.ENTROPY, 100).add(Aspect.ORDER, 100);

    public ItemFocusEnderChest(Item.Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack onFocusRightClick(ItemStack wandstack, Level level, Player player, InteractionHand hand) {
        if (WandHelper.consumeAllVis(wandstack, player, VIS_USAGE, true, false)) {
            if (!level.isClientSide) {
                player.openMenu(new SimpleMenuProvider((containerId, inventory, owner) -> ChestMenu.threeRows(containerId,
                        inventory, owner.getEnderChestInventory()), Component.translatable("container.enderchest")));
            }
            FociFx.sound(level, player, SoundEvents.ENDERMAN_TELEPORT, 1F, 1F);
        }

        return wandstack;
    }

    @Override
    public int getFocusColor(ItemStack focusstack) {
        return 0x132223;
    }

    @Override
    protected boolean hasDepth() {
        return true;
    }

    @Override
    public String getSortingHelper(ItemStack focusstack) {
        return "TTEC" + super.getSortingHelper(focusstack);
    }

    @Override
    public AspectList getVisCost(ItemStack focusstack) {
        return VIS_USAGE.copy();
    }

    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focusstack, int rank) {
        return rank >= 1 && rank <= 5 ? new FocusUpgradeType[] { FocusUpgradeType.frugal } : null;
    }

    @Override
    public IRegisterableResearch getResearchItem() {
        if (!ThaumcraftApi.isMirrorsAllowed()) {
            return null;
        }
        TTResearchItem research = new TTResearchItem(LibResearch.KEY_FOCUS_ENDER_CHEST,
                new AspectList().add(Aspect.ELDRITCH, 2).add(Aspect.VOID, 1).add(Aspect.MAGIC, 1), -6, -2, 2,
                new ItemStack(this));
        research.setWarp(1);
        research.setParents(LibResearch.KEY_FOCUS_DEFLECT).setConcealed().setPages(new ResearchPage("0"),
                ResearchHelper.arcaneRecipePage(LibResearch.KEY_FOCUS_ENDER_CHEST));
        return research;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        if (ThaumcraftApi.isMirrorsAllowed()) {
            return new ThaumicTinkererArcaneRecipe(LibResearch.KEY_FOCUS_ENDER_CHEST, LibResearch.KEY_FOCUS_ENDER_CHEST,
                    new ItemStack(this), new AspectList().add(Aspect.ORDER, 10).add(Aspect.ENTROPY, 10), "M", "E", "P",
                    'M', tcBlock("blockMirror", 0), 'E', new ItemStack(Items.ENDER_EYE), 'P',
                    tc("itemFocusPortableHole", 0));
        }
        return null;
    }
}
