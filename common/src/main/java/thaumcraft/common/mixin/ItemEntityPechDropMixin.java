package thaumcraft.common.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.pech.PechDrops;

@Mixin(ItemEntity.class)
public abstract class ItemEntityPechDropMixin implements PechDrops.Marker {
    @Unique private boolean thaumcraft$pechDrop;
    @Override public boolean thaumcraft$isPechDrop(){return thaumcraft$pechDrop;}
    @Override public void thaumcraft$setPechDrop(boolean value){thaumcraft$pechDrop=value;}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void thaumcraft$writePechDrop(CompoundTag tag,CallbackInfo ci){if(thaumcraft$pechDrop)tag.putBoolean("ThaumcraftPechDrop",true);}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void thaumcraft$readPechDrop(CompoundTag tag,CallbackInfo ci){thaumcraft$pechDrop=tag.getBoolean("ThaumcraftPechDrop");}
}
