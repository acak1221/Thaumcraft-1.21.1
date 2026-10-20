package thaumcraft.common.items.equipment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import thaumcraft.api.IRepairable;
import thaumcraft.api.IWarpingGear;
import thaumcraft.common.items.VoidMetal;

/** Original ItemVoidHoe: repairs itself, withers what it hits for four seconds, adds one warp; enchantability 5, no attack damage. */
public class ItemVoidHoe extends HoeItem implements IRepairable, IWarpingGear {
    public ItemVoidHoe(Tier tier, Properties properties) {
        // Original hoe has NO attack-damage entry. Only adapt native attack speed.
        super(tier, properties.rarity(Rarity.UNCOMMON).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -1,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()));
    }

    @Override public int getEnchantmentValue() { return 5; } // ItemVoidHoe:35-37, deliberately not the tier's 10.

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidMetal.repair(stack, entity);
    }

    // In 1.7 ItemHoe extended Item: its hit hook did not spend durability. The wither is the original left-click effect.
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        VoidMetal.wither(target, attacker, VoidMetal.TOOL_WITHER, false);
        return false;
    }

    @Override public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {}

    @Override public int getWarp(ItemStack stack, Player player) { return 1; }
}
