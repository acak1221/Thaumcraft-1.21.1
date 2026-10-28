/**
 * Created by pixlepix on 4/19/14.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.item;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.core.handler.ConfigHandler;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.potion.ModPotions;
import thaumic.tinkerer.common.registry.ITTinkererRegisterable;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.registry.ThaumicTinkererCrucibleRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.research.IRegisterableResearch;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The infused potions: drunk, they give the imbued effect of their aspect for three minutes (3600 ticks). The four
 * 1.7.10 metadata values (aer, ignis, terra, aqua) are four items. This class also owns the research entry
 * {@code INFUSED_POTIONS} (Infused Crops), registered by the first variant.
 */
public class ItemInfusedPotion extends Item implements ITTinkererRegisterable {
    /** The aspect of each variant, in the order of the 1.7.10 metadata. */
    public static final Aspect[] ASPECTS = ItemInfusedGrain.ASPECTS;

    private final int variant;

    public ItemInfusedPotion(int variant, Properties properties) {
        super(properties.stacksTo(1));
        this.variant = variant;
    }

    /** The registry path of a variant: {@code infused_potion_aer} and so on. */
    public static String path(int variant) {
        return LibItemNames.INFUSED_POTION + "_" + ItemInfusedGrain.SUFFIXES[variant];
    }

    /** 1.7.10 {@code getMetaForAspect}. */
    public static int getMetaForAspect(Aspect aspect) {
        return ItemInfusedGrain.getMetaForAspect(aspect);
    }

    public Aspect getAspect(ItemStack stack) {
        return ASPECTS[variant];
    }

    public int getVariant() {
        return variant;
    }

    /** 1.7.10 {@code getItemStackDisplayName}: {@code item.infusedPotion.<Aspect>.name}. */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.infusedPotion." + getAspect(stack).getName() + ".name");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(getAspect(stack).getName()));
    }

    // ---- drinking, as the potion of 1.7.10 ----------------------------------------------------------------------

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        Player player = entity instanceof Player p ? p : null;
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(ModPotions.holder(variant), 3600));
        }
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
            stack.consume(1, player);
        }
        if (player == null || !player.hasInfiniteMaterials()) {
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            if (player != null) {
                player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        entity.gameEvent(GameEvent.DRINK);
        return stack;
    }

    // ---- research and recipes ------------------------------------------------------------------------------------

    @Override
    public IRegisterableResearch getResearchItem() {
        if (variant != 0) return null;
        TTResearchItem research = (TTResearchItem) new TTResearchItem(
                LibResearch.KEY_POTIONS,
                new AspectList().add(Aspect.WATER, 5).add(Aspect.ENTROPY, 5),
                7,
                -5,
                2,
                ItemInfusedSeeds.getStackFromAspect(Aspect.FIRE));
        if (ConfigHandler.enableFire) {
            research.setParents(
                    LibResearch.KEY_FIRE_PERDITIO,
                    LibResearch.KEY_FIRE_ORDO,
                    LibResearch.KEY_FIRE_IGNIS,
                    LibResearch.KEY_FIRE_TERRA,
                    LibResearch.KEY_FIRE_AER,
                    LibResearch.KEY_FIRE_AQUA);
        }
        return (TTResearchItem) research.setParentsHidden("INFUSION").setConcealed().setPages(
                new ResearchPage("0"),
                new ResearchPage("1"),
                new ResearchPage("2"),
                new ResearchPage("3"),
                new ResearchPage("4"),
                new ResearchPage("5"),
                ResearchHelper.infusionPage(LibResearch.KEY_POTIONS, 4),
                ResearchHelper.crucibleRecipePage(LibResearch.KEY_POTIONS + "POT0"),
                ResearchHelper.crucibleRecipePage(LibResearch.KEY_POTIONS + "POT1"),
                ResearchHelper.crucibleRecipePage(LibResearch.KEY_POTIONS + "POT2"),
                ResearchHelper.crucibleRecipePage(LibResearch.KEY_POTIONS + "POT3"));
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererCrucibleRecipe(
                LibResearch.KEY_POTIONS + "POT" + variant,
                new ItemStack(this),
                new ItemStack(TTRegistry.item(ItemInfusedGrain.path(variant))),
                new AspectList().add(Aspect.AURA, 5).add(ASPECTS[variant], 5));
    }
}
