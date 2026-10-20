package thaumcraft.common.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.items.armor.Hover;
import thaumcraft.common.items.armor.TravellerBoots;

@Mixin(Player.class)
public abstract class PlayerTravelGearMixin {
    @Inject(method="jumpFromGround",at=@At("TAIL"))
    private void thaumcraft$travellerJump(CallbackInfo ci) {TravellerBoots.jumped((Player)(Object)this);}
    @Inject(method="getFlyingSpeed",at=@At("RETURN"),cancellable=true)
    private void thaumcraft$travellerAir(CallbackInfoReturnable<Float> cir) {cir.setReturnValue(TravellerBoots.airAcceleration((Player)(Object)this,cir.getReturnValue()));}
    @Inject(method="getDestroySpeed",at=@At("RETURN"),cancellable=true)
    private void thaumcraft$hoverMining(BlockState block,CallbackInfoReturnable<Float> cir) {cir.setReturnValue(Hover.miningSpeed((Player)(Object)this,cir.getReturnValue()));}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void thaumcraft$intrinsicFlightSave(CompoundTag tag,CallbackInfo ci) {Hover.saveIntrinsicFlight((Player)(Object)this,tag);}
}
