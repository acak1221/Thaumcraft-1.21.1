package thaumcraft.common.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.lib.items.EquipmentRules;

/**
 * The edible nuggets (original ItemNuggetEdible): the chicken, beef, pork and fish nuggets. Each of them restores one point of hunger and
 * a third of a point of saturation and is eaten in ten ticks, which is the original's own use duration.
 */
public final class ItemNuggetEdible extends Item {
    private final int type;

    public ItemNuggetEdible(Properties properties, int type) {
        super(properties.stacksTo(64).food(new FoodProperties.Builder()
                .nutrition(EquipmentRules.NUGGET_EDIBLE_HUNGER)
                .saturationModifier(EquipmentRules.NUGGET_EDIBLE_SATURATION)
                .build()));
        this.type = type;
    }

    /** The original metadata of this nugget: zero chicken, one beef, two pork, three fish. */
    public int type() { return type; }

    /** Original ItemNuggetEdible.func_77626_a: the nugget is eaten in ten ticks. */
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return EquipmentRules.NUGGET_EDIBLE_USE_DURATION; }
}
