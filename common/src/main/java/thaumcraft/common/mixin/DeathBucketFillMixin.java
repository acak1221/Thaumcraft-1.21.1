package thaumcraft.common.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.lib.fluid.DeathBucketActions;

/**
 * The original FillBucketEvent of the bucket of death (oracle
 * {@code thaumcraft/common/lib/events/EventHandlerWorld.java:212-223}).
 *
 * <p>Forge 1.7.10 posted the event from the patched {@code ItemBucket} before it looked at the hit block, and
 * the TC4 handler answered the death fluid source cell with the bucket of death. The modern native bucket has
 * no event, so the port restores the original answer in front of it.</p>
 */
@Mixin(BucketItem.class)
public abstract class DeathBucketFillMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void thaumcraft$fillDeath(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir){
        var result=DeathBucketActions.fill(level,player,hand);
        if(result!=null)cir.setReturnValue(result);
    }
}
