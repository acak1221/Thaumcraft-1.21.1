package thaumcraft.common.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.entities.golems.GolemUsePlatform;

/** Fabric dispatches UseBlockCallback at HEAD; this fence is in the native body. */
@Mixin(ServerPlayerGameMode.class)
public abstract class GolemUseServerPlayerGameModeMixin implements GolemUsePlatform.NativeUseFence {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    ordinal = 0, shift = At.Shift.BEFORE), cancellable = true, require = 1)
    private void thaumcraft$golemUseAfterFabricHook(ServerPlayer actor, Level actionLevel, ItemStack stack,
                                                 InteractionHand hand, BlockHitResult hit,
                                                 CallbackInfoReturnable<InteractionResult> callback) {
        if (GolemUsePlatform.isActorBorrowed(actor)
                && (this.player != actor || this.level != actionLevel || (Object) this != actor.gameMode
                    || !GolemUsePlatform.canContinueNativeUse(actor, actionLevel, stack, hand, hit))) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }
}
