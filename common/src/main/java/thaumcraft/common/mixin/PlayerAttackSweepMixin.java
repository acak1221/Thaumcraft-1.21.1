package thaumcraft.common.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.world.ElementalSwordSweep;

/** The original left-click hook of the Sword of the Zephyr ran before the blow it was part of, whoever called for it. */
@Mixin(Player.class)
public abstract class PlayerAttackSweepMixin {
    @Inject(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void thaumcraft$sweepBeforeTheBlow(Entity target, CallbackInfo callback) {
        ElementalSwordSweep.before((Player) (Object) this, target);
    }
}
