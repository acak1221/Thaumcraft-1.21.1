/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Nov 25, 2013, 8:52:43 PM (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.IGoggles;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.ItemApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.nodes.IRevealer;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererArcaneRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Helmet of Revealing: a Thaumium helmet with the goggles of revealing strapped on. It shows aura nodes and the aspects
 * of what the wearer looks at, gives a vis discount of 5% and is mended with thaumium ingots; its 500 points of durability
 * are those of the 1.7.10 helmet ({@code setMaxDamage(500)}).
 *
 * <p>Right-clicking a block with it tells the chat which block entity class stands there: a debugging leftover of the 1.7.10
 * helmet that the port keeps (once, from the server; 1.7.10 told it twice, from the client and from the server).</p>
 */
public class ItemRevealingHelm extends ArmorItem
        implements IRepairable, IRevealer, IGoggles, IVisDiscountGear, IRunicArmor, ITTinkererRegisterable {
    /** The 1.7.10 durability of the helmet. */
    public static final int DURABILITY = 500;
    /** The vis discount, in percent. */
    public static final int VIS_DISCOUNT = 5;

    /**
     * The armour material of the helmet: the numbers of Thaumcraft's Thaumium armour ({@code armorMatThaumium}: 2/6/5/2,
     * enchantability 25, thaumium ingot to mend) with the worn texture of this helmet,
     * {@code textures/models/armor/revealing_layer_1.png} (1.7.10 {@code getArmorTexture}: {@code textures/model/revealing.png}).
     */
    public static RegistrySupplier<ArmorMaterial> MATERIAL;

    public ItemRevealingHelm(Holder<ArmorMaterial> material, Item.Properties properties) {
        super(material, ArmorItem.Type.HELMET, properties.durability(DURABILITY).rarity(Rarity.UNCOMMON));
    }

    /** Registers the armour material; called from the registrar of the group. */
    public static void registerMaterial() {
        MATERIAL = TTRegistry.ARMOR_MATERIALS.register("revealing", () -> new ArmorMaterial(
                Map.of(ArmorItem.Type.HELMET, 2, ArmorItem.Type.CHESTPLATE, 6, ArmorItem.Type.LEGGINGS, 5,
                        ArmorItem.Type.BOOTS, 2),
                25, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(ItemsTC.require("thaumium_ingot")),
                List.of(new ArmorMaterial.Layer(LibMisc.id("revealing"))), 0, 0));
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity wearer) {
        return true;
    }

    @Override
    public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.visdiscount").append(": " + getVisDiscount(stack, null, null) + "%")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Thaumium ingots mend the helmet (1.7.10: {@code itemResource:2}). */
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ItemsTC.require("thaumium_ingot")) || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) {
        return VIS_DISCOUNT;
    }

    @Override
    public int getRunicCharge(ItemStack stack) {
        return 0;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            BlockEntity tile = context.getLevel().getBlockEntity(context.getClickedPos());
            // The text of 1.7.10, "class " included (Class.toString)
            player.sendSystemMessage(Component.literal(
                    "Tile Entity: " + (tile != null ? tile.getClass().toString() : "null")));
        }
        return super.useOn(context);
    }

    // ---- research and recipe -------------------------------------------------------------------------------------

    @Override
    public IRegisterableResearch getResearchItem() {
        IRegisterableResearch research;
        research = (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_REVEALING_HELM,
                new AspectList().add(Aspect.AURA, 2).add(Aspect.ARMOR, 1),
                0,
                0,
                1,
                new ItemStack(this)).setParents("GOGGLES").setParentsHidden("THAUMIUM");
        ((TTResearchItem) research)
                .setPages(new ResearchPage("0"), ResearchHelper.arcaneRecipePage(LibResearch.KEY_REVEALING_HELM));
        return research;
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererArcaneRecipe(
                LibResearch.KEY_REVEALING_HELM,
                LibResearch.KEY_REVEALING_HELM,
                new ItemStack(this),
                new AspectList().add(Aspect.EARTH, 5).add(Aspect.FIRE, 5).add(Aspect.WATER, 5).add(Aspect.AIR, 5)
                        .add(Aspect.ORDER, 5).add(Aspect.ENTROPY, 5),
                "GH",
                'G',
                ItemApi.getItem("itemGoggles", 0),
                'H',
                ItemApi.getItem("itemHelmetThaumium", 0));
    }
}
