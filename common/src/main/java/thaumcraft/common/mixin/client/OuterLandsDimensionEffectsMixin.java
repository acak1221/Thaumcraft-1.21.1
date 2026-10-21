package thaumcraft.common.mixin.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.client.OuterLandsDimensionEffects;
import thaumcraft.common.lib.world.OuterLandsDimensionRules;

/**
 * The client needs the dimension effects of the Outer Lands.
 *
 * <p>Minecraft 1.21.1 has no registry for the dimension effects, they are a static map of the
 * client renderer, so the effects of the Outer Lands are answered here. Without them the client
 * has no sky, no fog and no lightmap in the Outer Lands.
 */
@Mixin(DimensionSpecialEffects.class)
public abstract class OuterLandsDimensionEffectsMixin {
    @Inject(method = "forType", at = @At("HEAD"), cancellable = true)
    private static void thaumcraft$outerLands(DimensionType type,
                                               CallbackInfoReturnable<DimensionSpecialEffects> result) {
        if (OuterLandsDimensionRules.EFFECTS_ID.equals(type.effectsLocation().toString())) {
            result.setReturnValue(OuterLandsDimensionEffects.INSTANCE);
        }
    }
}
