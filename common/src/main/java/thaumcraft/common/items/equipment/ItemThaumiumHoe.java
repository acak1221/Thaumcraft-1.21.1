package thaumcraft.common.items.equipment;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import thaumcraft.api.IRepairable;

public class ItemThaumiumHoe extends HoeItem implements IRepairable {
    public ItemThaumiumHoe(Tier tier, Properties properties) {
        // Original hoe has NO attack-damage entry. Only adapt native attack speed.
        super(tier, properties.rarity(Rarity.UNCOMMON).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -1,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()));
    }

    @Override
    public int getEnchantmentValue() {
        return 5; // ItemThaumiumHoe:48-49, deliberately not the tier's22.
    }

    // In1.7 ItemHoe extended Item, not ItemTool. Its inherited hit hook
    // returned false and did NOT spend the two durability of a digger hit.
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { return false; }
    @Override public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {}
}
