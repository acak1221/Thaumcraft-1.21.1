package thaumcraft.common.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.lib.potions.FluxEffectAccess;
import thaumcraft.common.lib.potions.FluxEffects;

/** Per-effect empty curative list, rather than making the whole exhaustion type uncurable. */
@Mixin(MobEffectInstance.class)
public abstract class FluxEffectInstanceMixin implements FluxEffectAccess {
    @Unique private static final String thaumcraft$RESET_CURATIVES = "thaumcraft:reset_flux_curatives";
    @Unique private boolean thaumcraft$uncurableFlux;

    @Override public boolean thaumcraft$isUncurableFlux() { return thaumcraft$uncurableFlux; }
    @Override public void thaumcraft$setUncurableFlux() {
        thaumcraft$uncurableFlux = true;
        FluxEffects.clearNativeCures((MobEffectInstance) (Object) this);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("RETURN"))
    private void thaumcraft$copyCuratives(MobEffectInstance other, CallbackInfo callback) {
        if (FluxEffects.isUncurable(other)) thaumcraft$setUncurableFlux();
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void thaumcraft$mergeCuratives(MobEffectInstance other, CallbackInfoReturnable<Boolean> callback) {
        // Forge 1.7.10 PotionEffect.combine does NOT copy incoming curatives.
        // In particular, goo does not make a pre-existing curable effect uncurable.
        if (thaumcraft$uncurableFlux) FluxEffects.clearNativeCures((MobEffectInstance) (Object) this);
    }

    @Inject(method = "setDetailsFrom", at = @At("RETURN"))
    private void thaumcraft$retainCurativesOnHiddenTransition(MobEffectInstance other, CallbackInfo callback) {
        if (thaumcraft$uncurableFlux) FluxEffects.clearNativeCures((MobEffectInstance) (Object) this);
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void thaumcraft$saveCurativeReset(CallbackInfoReturnable<Tag> callback) {
        // Do NOT persist protection. This marker makes NeoForge reset its native
        // serialized cure set on load just as original PotionEffect NBT did.
        if (thaumcraft$uncurableFlux && callback.getReturnValue() instanceof CompoundTag tag) tag.putBoolean(thaumcraft$RESET_CURATIVES, true);
    }

    @Inject(method = "load", at = @At("RETURN"))
    private static void thaumcraft$loadCurativeReset(CompoundTag tag, CallbackInfoReturnable<MobEffectInstance> callback) {
        if (tag.getBoolean(thaumcraft$RESET_CURATIVES) && callback.getReturnValue() != null) FluxEffects.resetLoadedNativeCures(callback.getReturnValue());
    }
}
