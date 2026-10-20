package thaumcraft.common.items.equipment;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IWarpingGear;
import thaumcraft.common.items.VoidMetal;

/** Original ItemVoidSword: repairs itself, withers what it hits for three seconds, adds one warp. */
public class ItemVoidSword extends SwordItem implements IRepairable, IWarpingGear {
    public ItemVoidSword(Tier tier, Properties properties) {
        // Legacy sword 4 + material 3 = additive 7; the attack speed is a 1.21 combat adapter.
        super(tier, properties.rarity(Rarity.UNCOMMON).attributes(SwordItem.createAttributes(tier, 4, -2.4F)));
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidMetal.repair(stack, entity);
    }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        VoidMetal.wither(target, attacker, VoidMetal.SWORD_WITHER, true);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override public int getWarp(ItemStack stack, Player player) { return 1; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("enchantment.special.sapless").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
