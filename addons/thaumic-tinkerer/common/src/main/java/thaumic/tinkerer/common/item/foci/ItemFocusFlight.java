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

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftSoundIds;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/** Wand Focus: Uprising. A cast launches the player along his line of sight; costs 0.15 Aer, works while the config option {@code modFlight.enabled} is on. */
public class ItemFocusFlight extends ItemModFocus {
    /** The cost of a cast (or of a tick), in centivis. */
    public static final AspectList VIS_USAGE = new AspectList().add(Aspect.AIR, 15);

    /** The colour of the smoke spiral (1.7.10 passed {@code 0x9E2FF}, that is {@code 0x09E2FF}). */
    private static final int SPIRAL_COLOR = 0x09E2FF;

    public ItemFocusFlight(Item.Properties properties) {
        super(properties);
    }

    /**
     * The speed of the launch, in blocks per tick: {@code 1 / 1.5 * (1 + 0.2 * potency)}. 1.7.10 asked the wand's stack for
     * the upgrade level of the focus, which is always zero; the focus has no potency upgrade, so the speed is two thirds.
     */
    public static double force(int potencyLevel) {
        return 1 / 1.5 * (1 + potencyLevel * 0.2);
    }

    @Override
    public ItemStack onFocusRightClick(ItemStack wandstack, Level level, Player player, InteractionHand hand) {
        if (!ConfigHandler.enableFlight) {
            return wandstack;
        }
        ItemStack focus = WandHelper.getFocusItem(wandstack);
        if (WandHelper.consumeAllVis(wandstack, player, getVisCost(focus), true, false)) {
            Vec3 look = player.getLookAngle();
            double force = force(getUpgradeLevel(focus, FocusUpgradeType.potency));
            player.setDeltaMovement(look.x * force, look.y * force, look.z * force);
            player.resetFallDistance();
            if (player instanceof ServerPlayer serverPlayer) {
                FociHooks.resetFloatingTicks(serverPlayer);
            }
            FociFx.smokeSpiral(level, player, SPIRAL_COLOR);
            FociFx.thaumcraftSound(level, player, ThaumcraftSoundIds.WIND, 0.4F, 1F);
        }

        if (level.isClientSide) player.swing(hand);

        return wandstack;
    }

    @Override
    public String getSortingHelper(ItemStack focusstack) {
        return "TTFL" + super.getSortingHelper(focusstack);
    }

    @Override
    public int getFocusColor(ItemStack focusstack) {
        return 0x9EF2FF;
    }

    @Override
    protected boolean hasOrnament() {
        return true;
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
        TTResearchItem research = new TTResearchItem(LibResearch.KEY_FOCUS_FLIGHT,
                new AspectList().add(Aspect.MOTION, 1).add(Aspect.MAGIC, 1).add(Aspect.AIR, 2), -3, -4, 2,
                new ItemStack(this));
        research.setParents(LibResearch.KEY_FOCUS_SMELT).setConcealed().setPages(new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_FOCUS_FLIGHT));
        return research;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_FOCUS_FLIGHT, new ItemStack(this), 3,
                new AspectList().add(Aspect.AIR, 15).add(Aspect.MOTION, 20).add(Aspect.TRAVEL, 10),
                new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.QUARTZ), new ItemStack(Items.QUARTZ),
                new ItemStack(Items.QUARTZ), new ItemStack(Items.QUARTZ), new ItemStack(Items.FEATHER),
                new ItemStack(Items.FEATHER), tc("itemShard", 0));
    }
}
