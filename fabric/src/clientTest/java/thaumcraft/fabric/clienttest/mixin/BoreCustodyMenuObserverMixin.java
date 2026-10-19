package thaumcraft.fabric.clienttest.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.fabric.clienttest.BoreClientSequence;

/** Private observer only; does not cancel or alter the native menu operation. */
@Mixin(AbstractContainerMenu.class)
public abstract class BoreCustodyMenuObserverMixin {
    @Inject(method="clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",at=@At("HEAD"),require=1,allow=1)
    private void thaumcraft$boreCustodyMenuHead(int slot,int button,ClickType type,Player player,CallbackInfo ci) {
        BoreClientSequence.observeCustodyMenuHead((AbstractContainerMenu)(Object)this,slot,button,type,player);
    }
    @Inject(method="clicked(IILnet/minecraft/world/inventory/ClickType;Lnet/minecraft/world/entity/player/Player;)V",at=@At("RETURN"),require=1,allow=1)
    private void thaumcraft$boreCustodyMenuReturn(int slot,int button,ClickType type,Player player,CallbackInfo ci) {
        BoreClientSequence.observeCustodyMenuReturn((AbstractContainerMenu)(Object)this,slot,button,type,player);
    }
}
