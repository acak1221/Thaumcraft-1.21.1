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

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.IWarpingGear;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.KamiItemBase;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Ichorcloth armour: the armour of a diamond set that never wears out, with a vis discount of 6%, warp 3 per piece.
 * Item names: {@link LibItemNames#ICHOR_HELM}, {@link LibItemNames#ICHOR_CHEST}, {@link LibItemNames#ICHOR_LEGS},
 * {@link LibItemNames#ICHOR_BOOTS}.
 */
public class ItemIchorclothArmor extends ArmorItem
        implements IVisDiscountGear, ITTinkererRegisterable, IWarpingGear, IRunicArmor {

    public ItemIchorclothArmor(Holder<ArmorMaterial> material, ArmorItem.Type type, Item.Properties properties) {
        super(material, type, properties.rarity(KamiItemBase.KAMI_RARITY).stacksTo(1));
    }

    /** 1.7.10 {@code isItemTool}: the armour is enchantable; with no durability the default would say no. */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.visdiscount")
                .append(": " + getVisDiscount(stack, null, null) + "%").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) {
        return 6;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 3;
    }

    @Override
    public int getRunicCharge(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean shouldDisplayInTab() {
        return ConfigHandler.enableKami;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        ItemStack cloth = ItemKamiResource.stack(ItemKamiResource.ICHOR_CLOTH);
        return switch (getType()) {
            case HELMET -> new ThaumicTinkererArcaneRecipe(LibResearch.KEY_ICHORCLOTH_HELM,
                    LibResearch.KEY_ICHORCLOTH_ARMOR, new ItemStack(this), new AspectList().add(Aspect.WATER, 75), "CCC",
                    "C C", 'C', cloth);
            case CHESTPLATE -> new ThaumicTinkererArcaneRecipe(LibResearch.KEY_ICHORCLOTH_CHEST,
                    LibResearch.KEY_ICHORCLOTH_ARMOR, new ItemStack(this), new AspectList().add(Aspect.AIR, 75), "C C",
                    "CCC", "CCC", 'C', cloth);
            case LEGGINGS -> new ThaumicTinkererArcaneRecipe(LibResearch.KEY_ICHORCLOTH_LEGS,
                    LibResearch.KEY_ICHORCLOTH_ARMOR, new ItemStack(this), new AspectList().add(Aspect.FIRE, 75), "CCC",
                    "C C", "C C", 'C', cloth);
            case BOOTS -> new ThaumicTinkererArcaneRecipe(LibResearch.KEY_ICHORCLOTH_BOOTS,
                    LibResearch.KEY_ICHORCLOTH_ARMOR, new ItemStack(this), new AspectList().add(Aspect.EARTH, 75), "C C",
                    "C C", 'C', cloth);
            default -> null;
        };
    }

    /** ICHORCLOTH_ARMOR: the research of the four plain pieces (1.7.10: the helmet's {@code getResearchItem}). */
    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_ICHORCLOTH_ARMOR,
                new AspectList().add(Aspect.ARMOR, 2).add(Aspect.CLOTH, 1).add(Aspect.LIGHT, 1).add(Aspect.CRAFT, 1),
                17, 5, TTRegistry.stack(LibItemNames.ICHOR_HELM), 4, KamiResearch.of(LibResearch.KEY_ICHOR_CLOTH),
                KamiResearch.NONE, new ResearchPage("0"),
                ResearchHelper.arcaneRecipePage(LibResearch.KEY_ICHORCLOTH_HELM),
                ResearchHelper.arcaneRecipePage(LibResearch.KEY_ICHORCLOTH_CHEST),
                ResearchHelper.arcaneRecipePage(LibResearch.KEY_ICHORCLOTH_LEGS),
                ResearchHelper.arcaneRecipePage(LibResearch.KEY_ICHORCLOTH_BOOTS));
    }
}
