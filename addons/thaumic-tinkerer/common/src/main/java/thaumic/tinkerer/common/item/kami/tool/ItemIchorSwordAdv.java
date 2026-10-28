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
package thaumic.tinkerer.common.item.kami.tool;

import dev.architectury.event.EventResult;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchPage;
import thaumic.tinkerer.common.item.kami.ItemKamiResource;
import thaumic.tinkerer.common.item.kami.KamiHooks;
import thaumic.tinkerer.common.item.kami.KamiResearch;
import thaumic.tinkerer.common.lib.LibItemNames;
import thaumic.tinkerer.common.lib.LibMisc;
import thaumic.tinkerer.common.lib.LibResearch;
import thaumic.tinkerer.common.registry.ThaumicTinkererInfusionRecipe;
import thaumic.tinkerer.common.registry.ThaumicTinkererRecipe;
import thaumic.tinkerer.common.registry.TTRegistry;
import thaumic.tinkerer.common.research.ResearchHelper;
import thaumic.tinkerer.common.research.TTResearchItem;

/**
 * The Awakened Ichorium Sword: single mode, area mode (every mob of the same kind within 3 blocks of the one hit is hit
 * too) and soul mode (the hit target takes less damage, the wielder gains a Soul Heart).
 */
public class ItemIchorSwordAdv extends ItemIchorSword implements IAdvancedTool {
    private static final ResourceLocation AREA_STRIKE = LibMisc.id("kami_area_strike");

    /** The original's {@code ignoreLeftClick}: set while the area mode attacks the others. */
    private boolean ignoreLeftClick = false;

    public ItemIchorSwordAdv(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            ToolHandler.changeMode(stack);
            KamiHooks.showTooltip(level, ToolHandler.getToolModeStr(this, stack));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    /** Architectury {@code PlayerEvent.ATTACK_ENTITY}: 1.7.10 {@code Item.onLeftClickEntity} (before the hit). */
    public static EventResult onAttackEntity(Player player, Level level, Entity target, InteractionHand hand,
            net.minecraft.world.phys.EntityHitResult hit) {
        if (level.isClientSide || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer server)) {
            return EventResult.pass();
        }
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemIchorSwordAdv sword) sword.onLeftClickEntity(held, server, target);
        return EventResult.pass();
    }

    public void onLeftClickEntity(ItemStack stack, ServerPlayer player, Entity entity) {
        if (ignoreLeftClick || !(entity instanceof LivingEntity living) || living.hurtTime != 0
                || living.isDeadOrDying()) {
            return;
        }
        switch (ToolHandler.getMode(stack)) {
            case 1 -> {
                int range = 3;
                @SuppressWarnings("unchecked")
                Class<Entity> kind = (Class<Entity>) entity.getClass();
                List<Entity> entities = player.level().getEntitiesOfClass(kind,
                        new AABB(entity.getX() - range, entity.getY() - range, entity.getZ() - range,
                                entity.getX() + range, entity.getY() + range, entity.getZ() + range));
                ignoreLeftClick = true;
                // 1.7.10 had no attack cooldown; without a fast attack speed every hit after the first would deal a fifth.
                AttributeInstance speed = player.getAttribute(Attributes.ATTACK_SPEED);
                boolean boosted = speed != null && !speed.hasModifier(AREA_STRIKE);
                if (boosted) {
                    speed.addTransientModifier(new AttributeModifier(AREA_STRIKE, 1000.0,
                            AttributeModifier.Operation.ADD_VALUE));
                }
                try {
                    for (Entity other : entities) player.attack(other);
                } finally {
                    if (boosted) speed.removeModifier(AREA_STRIKE);
                    ignoreLeftClick = false;
                }
            }
            case 2 -> {
                living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1, 1));
                KamiHooks.soulHeartSink.accept(player);
            }
            default -> {}
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(ToolHandler.getToolModeStr(this, stack).copy().withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public String getType() {
        return "sword";
    }

    @Override
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererInfusionRecipe(LibResearch.KEY_ICHOR_SWORD_GEM, new ItemStack(this), 15,
                new AspectList().add(Aspect.AIR, 50).add(Aspect.HUNGER, 64).add(Aspect.SOUL, 32)
                        .add(Aspect.WEAPON, 32).add(Aspect.ENERGY, 32).add(Aspect.ORDER, 16).add(Aspect.CRYSTAL, 16),
                TTRegistry.stack(LibItemNames.ICHOR_SWORD), ItemKamiResource.stack(ItemKamiResource.ICHORIUM),
                ItemKamiResource.stack(ItemKamiResource.ICHOR), KamiResearch.tc("itemSwordElemental", 0),
                KamiResearch.tc("itemFocusFrost", 0), new ItemStack(Items.CACTUS), KamiResearch.tc("itemNugget", 21),
                KamiResearch.tc("itemNugget", 16), KamiResearch.tc("itemNugget", 31), new ItemStack(Items.DIAMOND),
                KamiResearch.tc("itemFocusFrost", 0), KamiResearch.tc("itemSwordElemental", 0),
                ItemKamiResource.stack(ItemKamiResource.ICHOR_CLOTH));
    }

    public static TTResearchItem research() {
        return KamiResearch.entry(LibResearch.KEY_ICHOR_SWORD_GEM,
                new AspectList().add(Aspect.AIR, 2).add(Aspect.WEAPON, 1).add(Aspect.SOUL, 1).add(Aspect.HUNGER, 1), 16,
                12, TTRegistry.stack(LibItemNames.ICHOR_SWORD_GEM), 0, KamiResearch.of(LibResearch.KEY_ICHOR_TOOLS),
                KamiResearch.NONE, new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_ICHOR_SWORD_GEM),
                new ResearchPage("1"));
    }
}
