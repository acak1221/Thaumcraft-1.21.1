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
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumic.tinkerer.common.core.handler.kami.KamiArmorHandler;
import thaumic.tinkerer.common.item.kami.KamiComponents;
import thaumic.tinkerer.common.item.kami.KamiHooks;

/**
 * The awakened armour pieces: Ichorcloth armour (vis discount 10% instead of 6%) with an effect each. Sneak-clicking a
 * piece switches that piece off or on (1.7.10: the damage value), the toggle key switches all pieces
 * ({@link KamiArmorHandler#getArmorStatus}). The effects run from {@link KamiArmorHandler#onPlayerTick}.
 */
public abstract class ItemIchorclothArmorAdv extends ItemIchorclothArmor {

    protected ItemIchorclothArmorAdv(ArmorItem.Type type, Item.Properties properties) {
        super(KamiArmorMaterials.ICHOR_GEM, type, properties);
    }

    /** True when this piece was switched off (1.7.10: damage value 1). */
    public static boolean isDisabled(ItemStack stack) {
        return stack.getOrDefault(KamiComponents.PIECE_DISABLED.get(), false);
    }

    /** The master switch is on and this piece is not switched off. */
    public static boolean isActive(Player player, ItemStack stack) {
        return KamiArmorHandler.getArmorStatus(player) && !isDisabled(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            boolean disabled = !isDisabled(stack);
            stack.set(KamiComponents.PIECE_DISABLED.get(), disabled);
            KamiHooks.playToggleSound(level, player);
            KamiHooks.showTooltip(level, Component.translatable("ttmisc.awakenedArmor" + (disabled ? 1 : 0)));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (isDisabled(stack)) {
            tooltip.add(Component.translatable("ttmisc.awakenedArmor1").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) {
        return 10;
    }

    /**
     * Keeps a potion effect on the player for at least {@code duration} ticks (1.7.10 raised the duration of the running
     * effect or added a new one); an effect that is already longer is left alone.
     */
    protected static void ensureEffect(Player player, Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() < duration) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, true));
        }
    }
}
