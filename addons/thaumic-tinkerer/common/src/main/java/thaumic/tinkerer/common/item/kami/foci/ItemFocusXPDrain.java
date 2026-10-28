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

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.WandHelper;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
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
 * Wand Focus: Experience Drain. Held, it turns the caster's experience into vis, every tick into the primal aspect the
 * wand holds least of: 5 vis (+20% per potency) for 15 experience points (-1 per frugal level).
 */
public class ItemFocusXPDrain extends ItemModKamiFocus {

    public ItemFocusXPDrain(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isVisCostPerTick(ItemStack focusstack) {
        return true;
    }

    @Override
    public String getSortingHelper(ItemStack focusstack) {
        return "TTKXP" + super.getSortingHelper(focusstack);
    }

    @Override
    protected boolean isKamiRarity() {
        return true;
    }

    @Override
    public void onUsingFocusTick(ItemStack wandstack, Player player, int count) {
        if (player.level().isClientSide) return;

        AspectList aspects = WandHelper.getAllVis(wandstack);
        Aspect lowestAspect = null;
        int lowestAmount = Integer.MAX_VALUE;
        int maxVis = WandHelper.getMaxVis(wandstack);

        for (Aspect aspect : Aspect.getPrimalAspects()) {
            int amount = aspects.getAmount(aspect);
            if (amount < maxVis && amount < lowestAmount) {
                lowestAmount = amount;
                lowestAspect = aspect;
            }
        }
        if (lowestAspect == null) return;

        int potency = WandHelper.getFocusPotency(wandstack);
        float multiplier = 1.0F + (0.2F * potency);
        int intendedAdd = Math.round(500 * multiplier);
        int actualAdd = Math.min(intendedAdd, maxVis - lowestAmount);

        int xpUse = 15 - WandHelper.getFocusFrugal(wandstack);
        int scaledXp = Math.max(1, Math.round(xpUse * (actualAdd / (float) intendedAdd)));
        if (getPlayerXP(player) >= scaledXp) {
            drainPlayerXP(player, scaledXp);
            WandHelper.addRealVis(wandstack, lowestAspect, actualAdd, true);
        }
    }

    /** A colour that runs through the rainbow on the client (1.7.10 {@code Color.HSBtoRGB(ticksExisted * 2 % 360 / 360F, 1, 1)}); white where there is no client player. */
    @Override
    public int getFocusColor(ItemStack focusstack) {
        Player player = KamiHooks.localPlayer();
        return player == null ? 0xFFFFFF : Mth.hsvToRgb(player.tickCount * 2 % 360 / 360F, 1F, 1F);
    }

    int getXpUse(ItemStack focusstack) {
        return 15 - getUpgradeLevel(focusstack, FocusUpgradeType.frugal);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.Focus.cost2"));
        tooltip.add(Component.literal(" ")
                .append(Component.translatable("ttmisc.experience").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" x " + getXpUse(stack)).withStyle(ChatFormatting.WHITE)));
    }

    @Override
    public AspectList getVisCost(ItemStack focusstack) {
        return new AspectList();
    }

    @Override
    public FocusUpgradeType[] getPossibleUpgradesByRank(ItemStack focusstack, int rank) {
        return rank <= 5 ? new FocusUpgradeType[] { FocusUpgradeType.frugal, FocusUpgradeType.potency } : null;
    }

    // ---- experience (1.7.10 ExperienceHelper, with the 1.21.1 experience curve) -------------------------------

    /** The experience points of a level: the total needed to reach it from level 0. */
    public static int getExperienceForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    public static int getLevelForExperience(int experience) {
        int level = 0;
        while (getExperienceForLevel(level + 1) <= experience) level++;
        return level;
    }

    public static int getPlayerXP(Player player) {
        return getExperienceForLevel(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    public static void drainPlayerXP(Player player, int amount) {
        int experience = Math.max(0, getPlayerXP(player) - amount);
        player.totalExperience = experience;
        player.experienceLevel = getLevelForExperience(experience);
        int expForLevel = getExperienceForLevel(player.experienceLevel);
        player.experienceProgress = (float) (experience - expForLevel) / (float) player.getXpNeededForNextLevel();
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_FOCUS_XP_DRAIN, new ItemStack(this), 12,
                new AspectList().add(Aspect.MIND, 65).add(Aspect.TAINT, 16).add(Aspect.MAGIC, 50).add(Aspect.AURA, 32),
                new ItemStack(Items.ENDER_PEARL), ItemKamiResource.stack(ItemKamiResource.ICHOR),
                new ItemStack(Items.EXPERIENCE_BOTTLE), new ItemStack(Items.DIAMOND),
                TTRegistry.stack(LibItemNames.XP_TALISMAN), new ItemStack(Items.ENCHANTING_TABLE),
                ItemKamiResource.stack(ItemKamiResource.ICHOR));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_FOCUS_XP_DRAIN,
                new AspectList().add(Aspect.MIND, 2).add(Aspect.MAGIC, 1).add(Aspect.AURA, 1).add(Aspect.MAN, 1), 12, 3,
                TTRegistry.stack(LibItemNames.FOCUS_XP_DRAIN), 0, KamiResearch.of(LibResearch.KEY_ICHORCLOTH_ROD),
                KamiResearch.NONE, new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_FOCUS_XP_DRAIN));
    }
}
