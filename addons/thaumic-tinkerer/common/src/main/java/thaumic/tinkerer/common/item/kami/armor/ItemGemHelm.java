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
package thaumic.tinkerer.common.item.kami.armor;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import thaumcraft.api.IGoggles;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * Cowl of the Abyssal Depths: goggles of revealing; in water unlimited breath and night vision; in lava, as in the
 * pinned 1.7.10 source, unlimited breath and blindness (the code names {@code Potion.blindness} there); and the
 * regeneration of a full belly while the food bar is low.
 */
public class ItemGemHelm extends ItemIchorclothArmorAdv implements IGoggles, IRevealer {

    public ItemGemHelm(Item.Properties properties) {
        super(ArmorItem.Type.HELMET, properties);
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity wearer) {
        return true;
    }

    @Override
    public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) {
        return true;
    }

    /** 1.7.10 {@code tickPlayer}, run while the helmet is worn. */
    public static void tickPlayer(Player player, ItemStack armor) {
        if (player.level().isClientSide) return;
        boolean active = isActive(player, armor);
        if (active && player.isEyeInFluid(FluidTags.WATER)) {
            player.setAirSupply(player.getMaxAirSupply());
            ensureEffect(player, MobEffects.NIGHT_VISION, 202, 0);
        }
        if (active && player.isEyeInFluid(FluidTags.LAVA)) {
            player.setAirSupply(player.getMaxAirSupply());
            ensureEffect(player, MobEffects.BLINDNESS, 202, 0);
        }
        int food = player.getFoodData().getFoodLevel();
        // The food heal of 1.7.10 does not look at the switch.
        if (food > 0 && food < 18 && player.isHurt() && player.tickCount % 80 == 0) player.heal(1F);
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_ICHORCLOTH_HELM_GEM, new ItemStack(this), 13,
                new AspectList().add(Aspect.WATER, 50).add(Aspect.ARMOR, 32).add(Aspect.HUNGER, 32)
                        .add(Aspect.AURA, 32).add(Aspect.LIGHT, 64).add(Aspect.FLESH, 16).add(Aspect.MIND, 16),
                TTRegistry.stack(LibItemNames.ICHOR_HELM), new ItemStack(Items.DIAMOND),
                ItemKamiResource.stack(ItemKamiResource.ICHOR), ItemKamiResource.stack(ItemKamiResource.ICHOR),
                KamiResearch.tc("itemThaumonomicon", 0), KamiResearch.tc("itemFocusPrimal", 0),
                new ItemStack(Items.GOLDEN_HELMET),
                PotionContents.createItemStack(Items.POTION, Potions.NIGHT_VISION),
                KamiResearch.tc("itemGoggles", 0), new ItemStack(Items.GHAST_TEAR), new ItemStack(Items.COD),
                new ItemStack(Items.CAKE), new ItemStack(Items.ENDER_EYE));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_ICHORCLOTH_HELM_GEM,
                new AspectList().add(Aspect.WATER, 2).add(Aspect.HEAL, 1).add(Aspect.HUNGER, 1).add(Aspect.AURA, 1), 18,
                3, TTRegistry.stack(LibItemNames.ICHOR_HELM_GEM), 0, KamiResearch.of(LibResearch.KEY_ICHORCLOTH_ARMOR),
                KamiResearch.NONE, new ResearchPage("0"),
                ResearchHelper.infusionPage(LibResearch.KEY_ICHORCLOTH_HELM_GEM));
    }
}
