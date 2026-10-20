package thaumcraft.common.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.BathSaltTicks;

@Mixin(ItemEntity.class)
public abstract class BathSaltExpiryMixin {
    @Inject(method="tick",at=@At("TAIL"))
    private void thaumcraft$expireBathSalts(CallbackInfo ci) { BathSaltTicks.afterNativeTick((ItemEntity)(Object)this); }
}
