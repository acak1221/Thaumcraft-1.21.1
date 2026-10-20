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
import thaumcraft.common.lib.fluid.PureBucketActions;

/** Removed Forge FillBucketEvent.ALLOW for original Pure sources only. */
@Mixin(BucketItem.class)
public abstract class PureBucketFillMixin {
    @Inject(method="use",at=@At("HEAD"),cancellable=true)
    private void thaumcraft$fillPure(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir){
        var result=PureBucketActions.fill(level,player,hand);
        if(result!=null)cir.setReturnValue(result);
    }
}
