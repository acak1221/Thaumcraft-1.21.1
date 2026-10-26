package thaumcraft.fabric.mixin.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.items.armor.Hover;

/**
 * Fabric half of the Hover Harness mining speed: a hovering player mines at the ground speed.
 *
 * <p>The original added it to the Forge break speed event, after the airborne fifth. On Fabric the
 * vanilla {@code Player.getDestroySpeed} is the break speed and ends with that fifth. NeoForge
 * breaks blocks through its own {@code getDigSpeed} and posts the break speed event, so its module
 * listens to the event instead; this injection would never run there and is Fabric only.</p>
 */
@Mixin(Player.class)
public abstract class FabricHoverMiningMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void thaumcraft$hoverMining(BlockState block, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Hover.miningSpeed((Player) (Object) this, cir.getReturnValue()));
    }
}
