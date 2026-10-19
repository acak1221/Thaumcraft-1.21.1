package thaumcraft.common.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.lib.capabilities.KnowledgeAccess;
import thaumcraft.common.lib.network.KnowledgeEvents;

/** Original ItemZombieBrain:35-37,51-58. Wolf food is a root-owned native tag;
 * this class charges knowledge, not the still-unported Warp effect engine. */
public final class ItemZombieBrain extends Item {
    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(4).saturationModifier(.2F)
            .effect(new MobEffectInstance(MobEffects.HUNGER, 30 * 20, 0), .8F).build();

    public ItemZombieBrain(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        // LivingEntity's native completion is still using the actual hand stack
        // with remaining time zero. Display copies/direct finish calls grant no warp.
        if (consumer instanceof ServerPlayer player && level instanceof ServerLevel server
                && player.level() == server && server.getServer().isSameThread() && player.isAlive()
                && !stack.isEmpty() && stack.getItem() == this && stack.has(DataComponents.FOOD)
                && player.isUsingItem() && player.getUseItemRemainingTicks() == 0
                && player.getUseItem() == stack && player.getItemInHand(player.getUsedItemHand()) == stack) {
            boolean sticky = server.random.nextFloat() < .1F;
            int amount = sticky ? 1 : 1 + server.random.nextInt(3);
            var knowledge = KnowledgeAccess.get(player);
            if (sticky) knowledge.addWarpSticky(amount);
            else knowledge.addWarpTemp(amount);
            knowledge.setWarpCounter(knowledge.getTotalWarp());
            KnowledgeEvents.sync(player);
            // Same native persistence/notification path as InfusionInstability.warp.
            player.displayClientMessage(Component.translatable(sticky ? "tc.addwarpsticky" : "tc.addwarptemp"), true);
            if (sticky) player.playNotifySound(SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath("thaumcraft", "whispers")), SoundSource.PLAYERS, .5F, 1);
        }
        // Keep native hunger/saturation, effect roll, statistics, advancements,
        // creative handling and payment; warp is deliberately BEFORE this call.
        return super.finishUsingItem(stack, level, consumer);
    }
}
