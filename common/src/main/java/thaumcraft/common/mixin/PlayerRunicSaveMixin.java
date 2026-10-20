package thaumcraft.common.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.runic.RunicShield;

/** Replaces original separate .thaum file's shielding int with native player NBT, fresh worlds only. */
@Mixin(Player.class)
public abstract class PlayerRunicSaveMixin {
    @Inject(method="addAdditionalSaveData",at=@At("RETURN"))
    private void thaumcraft$saveRunic(CompoundTag tag, CallbackInfo ci) { RunicShield.save((Player)(Object)this,tag); }
    @Inject(method="readAdditionalSaveData",at=@At("RETURN"))
    private void thaumcraft$loadRunic(CompoundTag tag, CallbackInfo ci) { RunicShield.load((Player)(Object)this,tag); }
}
