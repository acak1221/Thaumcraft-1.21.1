package forbiddenmagic.common.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Original ItemResource.func_111207_a (itemInteractionForEntity), subtype 1 only: dyes an
 * un-sheared, not-already-black sheep black and consumes one item. Modern equivalent of
 * {@code EntitySheep.func_70892_o()/func_70896_n()/func_70891_b(15)}.
 */
public final class ItemShadowInk extends Item {
    public ItemShadowInk(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof Sheep sheep && !sheep.isSheared() && sheep.getColor() != DyeColor.BLACK) {
            sheep.setColor(DyeColor.BLACK);
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
