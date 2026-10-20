package thaumcraft.common.items.equipment;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * The 1.21.1 form of the original {@code ToolAttributeHelper}: the inherited attack damage of a tool is replaced by an explicit value.
 *
 * <p>The original 1.12 helper took the tool's attribute multimap and swapped its single main hand attack damage modifier for one with
 * the wanted amount. In 1.21.1 the attributes of an item are a data component, so the same job is done by rebuilding the modifier list
 * with the attack damage modifier replaced.
 */
public final class ToolAttributeHelper {
    private ToolAttributeHelper() {}

    /**
     * Replaces the attack damage of the inherited modifiers with {@code damage}, keeping every other modifier, its id and its operation.
     * The attack speed and any other attribute are left untouched.
     */
    public static ItemAttributeModifiers withMainHandDamage(ItemAttributeModifiers inherited, double damage) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        for (ItemAttributeModifiers.Entry entry : inherited.modifiers()) {
            if (entry.attribute() == Attributes.ATTACK_DAMAGE) {
                AttributeModifier modifier = entry.modifier();
                builder.add(entry.attribute(), new AttributeModifier(modifier.id(), damage, modifier.operation()), entry.slot());
            } else {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }
        return builder.build();
    }
}
