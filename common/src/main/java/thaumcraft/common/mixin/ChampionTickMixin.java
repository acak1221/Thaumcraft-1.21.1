package thaumcraft.common.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.mods.ChampionHooks;

/**
 * The original living update of a champion. The 1.7 original answered every living
 * update of a champion mob whose modifier triggered on the tick, and the client drew
 * the champion particles on the same event. The native port answers the mob tick and
 * lets the client draw the particles from the synced champion id.
 */
@Mixin(Mob.class)
public abstract class ChampionTickMixin {
    /** The original living update trigger of the champion modifiers. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void thaumcraft$championTick(CallbackInfo ci) {
        ChampionHooks.tickChampion((Mob) (Object) this);
    }
}
