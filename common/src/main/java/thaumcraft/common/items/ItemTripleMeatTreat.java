package thaumcraft.common.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The Triple Meat Treat (original ItemTripleMeatTreat): six points of hunger, four fifths of a point of saturation, always edible, and two
 * thirds of the time a few seconds of regeneration.
 */
public final class ItemTripleMeatTreat extends Item {
    public ItemTripleMeatTreat(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(EquipmentRules.TREAT_HUNGER)
                .saturationModifier(EquipmentRules.TREAT_SATURATION)
                .alwaysEdible()
                .effect(new MobEffectInstance(MobEffects.REGENERATION, EquipmentRules.TREAT_REGENERATION_TICKS), EquipmentRules.TREAT_EFFECT_CHANCE)
                .build()));
    }
}
