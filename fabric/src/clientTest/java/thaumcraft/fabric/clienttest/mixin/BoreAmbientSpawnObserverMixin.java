package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.fabric.clienttest.BoreClientSequence;

/** STAGING ONLY: root must add exception-contained private-harness routes.
 * Terminal overload, NOT Java-final. No redirect/cancel/result replacement. */
@Mixin(Entity.class)
public abstract class BoreAmbientSpawnObserverMixin {
    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreAmbientSpawnHead(ItemStack offered, float yOffset, CallbackInfoReturnable<ItemEntity> cir) {
        BoreClientSequence.observeAmbientSpawnHead((Entity)(Object)this, offered, yOffset);
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"), require = 3, allow = 3)
    private void thaumcraft$boreAmbientSpawnReturn(ItemStack offered, float yOffset, CallbackInfoReturnable<ItemEntity> cir) {
        BoreClientSequence.observeAmbientSpawnReturn((Entity)(Object)this, offered, yOffset, cir.getReturnValue());
    }
}
