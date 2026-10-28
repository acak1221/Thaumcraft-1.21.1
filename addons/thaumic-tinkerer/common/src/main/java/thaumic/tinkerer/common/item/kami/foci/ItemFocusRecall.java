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
package thaumic.tinkerer.common.item.kami.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.ItemSkyPearl;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Wand Focus: Celestial Recall. Held for three seconds with an attuned Celestial Pearl in the hotbar, it teleports the
 * caster to that pearl's gateway (of the same dimension) for 40 vis each of Aer, Terra and Ordo.
 */
public class ItemFocusRecall extends ItemModKamiFocus {
    /** 1.7.10: 40 vis of each, in centivis. */
    private static final int COST = 4000;
    /** 1.7.10: the use had to run for more than 60 ticks. */
    private static final int CHANNEL_TICKS = 60;

    public ItemFocusRecall(Item.Properties properties) {
        super(properties);
    }

    @Override
    public String getSortingHelper(ItemStack focusstack) {
        return "TTKCR" + super.getSortingHelper(focusstack);
    }

    @Override
    protected boolean hasDepth() {
        return true;
    }

    @Override
    public boolean isUseItem(ItemStack stack) {
        return true;
    }

    @Override
    public void onUsingFocusTick(ItemStack wandstack, Player player, int count) {
        if (player.getTicksUsingItem() <= CHANNEL_TICKS) return;
        if (!player.level().isClientSide && player instanceof ServerPlayer server) recall(wandstack, server);
        player.stopUsingItem();
    }

    private void recall(ItemStack wand, ServerPlayer player) {
        ItemStack pearl = null;
        for (int i = 0; i < 9; i++) {
            ItemStack inSlot = player.getInventory().getItem(i);
            if (inSlot.getItem() instanceof ItemSkyPearl && ItemSkyPearl.isAttuned(inSlot)) {
                pearl = inSlot;
                break;
            }
        }
        if (pearl == null || !player.level().dimension().equals(ItemSkyPearl.getDim(pearl))) return;

        BlockPos target = ItemSkyPearl.getPos(pearl);
        AspectList cost = getVisCost(WandHelper.getFocusItem(wand));
        if (WandHelper.consumeAllVis(wand, player, cost, false, false)
                && KamiHooks.warpGateTeleporter.teleport(player, target)) {
            WandHelper.consumeAllVis(wand, player, cost, true, false);
        }
    }

    @Override
    public int getFocusColor(ItemStack focusstack) {
        return 0x9CF8FF;
    }

    @Override
    public AspectList getVisCost(ItemStack focusstack) {
        return new AspectList().add(Aspect.AIR, COST).add(Aspect.EARTH, COST).add(Aspect.ORDER, COST);
    }

    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focusstack, int rank) {
        return rank >= 1 && rank <= 5 ? new FocusUpgradeType[] { FocusUpgradeType.frugal } : null;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_FOCUS_RECALL, new ItemStack(this), 10,
                new AspectList().add(Aspect.TRAVEL, 100).add(Aspect.ELDRITCH, 64).add(Aspect.MAGIC, 50),
                TTRegistry.stack(LibItemNames.SKY_PEARL), ItemKamiResource.stack(ItemKamiResource.ICHOR),
                ItemKamiResource.stack(ItemKamiResource.ICHOR), new ItemStack(Items.ENDER_PEARL),
                new ItemStack(Items.DIAMOND), KamiResearch.tcBlock("blockMirror", 0),
                KamiResearch.tc("itemFocusPortableHole", 0));
    }

    /** The research entry; 1.7.10 skipped it when Thaumcraft's {@code allowMirrors} was off (no API for that yet, A5). */
    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_FOCUS_RECALL,
                new AspectList().add(Aspect.TRAVEL, 2).add(Aspect.ELDRITCH, 1).add(Aspect.FLIGHT, 1)
                        .add(Aspect.MAGIC, 1),
                20, 8, TTRegistry.stack(LibItemNames.FOCUS_RECALL), 0, KamiResearch.of(LibResearch.KEY_WARP_GATE),
                KamiResearch.of(LibResearch.KEY_ICHORCLOTH_ROD), new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_FOCUS_RECALL));
    }
}
