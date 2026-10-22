package thaumcraft.common.items.equipment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRepairable;
import thaumcraft.common.lib.world.StreamAxeItemMotion;

/**
 * Axe of the Stream (original ItemElementalAxe, Api THAUMIUM_ELEMENTAL). A real axe:
 * tier 3, 1500 uses, speed 10, enchantability 18. Chopping is handled where the
 * real player's break completes (StreamAxeHarvest); this class owns the held
 * right-click vacuum. Repair-enchantment self repair is handled by the shared
 * {@code EquipmentEnchantments.tick} / {@code EnchantmentRepair.tick} path.
 */
public class ItemElementalAxe extends AxeItem implements IRepairable {
    public static final int USE_DURATION = 72000;

    public ItemElementalAxe(Tier tier, Properties properties) {
        // Legacy axe3 + elemental material3 = additive 6 (player total 7), not modern axe damage.
        // The -3.0 attack speed is the 1.9+ axe adaptation, not a TC4 value.
        super(tier, properties.rarity(Rarity.RARE).attributes(DiggerItem.createAttributes(tier, 3, -3.0F)));
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return USE_DURATION; }

    /** The original has no strip/scrape/wax-off branch; PASS lets use() start the held vacuum. */
    @Override public InteractionResult useOn(UseOnContext context) { return InteractionResult.PASS; }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) StreamAxeItemMotion.vacuum(player);
    }
}
