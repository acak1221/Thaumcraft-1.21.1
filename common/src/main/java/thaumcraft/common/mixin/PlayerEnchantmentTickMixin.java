package thaumcraft.common.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.enchantment.EquipmentEnchantments;

/** Shared living tick preserves the original client/server Haste application before travel. */
@Mixin(LivingEntity.class)
public abstract class PlayerEnchantmentTickMixin {
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void thaumcraft$equipmentEnchantmentTick(CallbackInfo ci) {
        if ((Object)this instanceof Player player) EquipmentEnchantments.playerLivingTick(player);
    }
}
