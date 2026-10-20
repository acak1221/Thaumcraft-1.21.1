package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.common.items.ItemResearchNotes;

/** A whole transferred discovery must survive an empty chest/ender-chest setter; ordinary items retain native limits. */
@Mixin({SimpleContainer.class, BaseContainerBlockEntity.class})
abstract class ResearchNoteContainerCountMixin {
    @WrapOperation(method = "setItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;limitSize(I)V"))
    private void thaumcraft$keepCopiedNotes(ItemStack stack, int limit, Operation<Void> nativeLimit) {
        nativeLimit.call(stack, stack.getItem() instanceof ItemResearchNotes ? Math.max(limit, stack.getCount()) : limit);
    }
}
