package thaumcraft.common.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.baubles.AccessoryHolder;
import thaumcraft.common.lib.baubles.PlayerAccessories;
import thaumcraft.common.lib.baubles.PlayerAccessoryInventory;

/** Fresh-world player attachment; no global player cache and no external .baub files. */
@Mixin(Player.class)
public abstract class PlayerAccessoriesMixin implements AccessoryHolder {
    @Unique private PlayerAccessoryInventory thaumcraft$accessories;
    @Override public PlayerAccessoryInventory thaumcraft$accessories() {
        if(thaumcraft$accessories==null)thaumcraft$accessories=new PlayerAccessoryInventory((Player)(Object)this);
        return thaumcraft$accessories;
    }
    @Inject(method="addAdditionalSaveData",at=@At("RETURN"))
    private void thaumcraft$saveAccessories(CompoundTag tag,CallbackInfo ci){PlayerAccessories.save((Player)(Object)this,tag);}
    @Inject(method="readAdditionalSaveData",at=@At("RETURN"))
    private void thaumcraft$loadAccessories(CompoundTag tag,CallbackInfo ci){PlayerAccessories.load((Player)(Object)this,tag);}
    @Inject(method="dropEquipment",at=@At("RETURN"))
    private void thaumcraft$dropAccessories(CallbackInfo ci){PlayerAccessories.dropOnDeath((Player)(Object)this);}
}
