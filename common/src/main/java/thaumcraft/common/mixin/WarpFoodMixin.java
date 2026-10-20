package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import thaumcraft.common.lib.WarpEvents;

/** Preserve the consumed food identity even when native eating shrinks the last item to EMPTY. */
@Mixin(LivingEntity.class)
abstract class WarpFoodMixin {
    @WrapMethod(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;")
    private ItemStack thaumcraft$warpFood(Level level, ItemStack stack, FoodProperties food, Operation<ItemStack> original) {
        ItemStack before = stack.copy();
        ItemStack result = original.call(level, stack, food);
        if (!level.isClientSide && (Object)this instanceof ServerPlayer player) WarpEvents.afterEating(player, before);
        return result;
    }
}
