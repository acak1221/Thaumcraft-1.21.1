package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.common.items.ItemResearchNotes;

/** Original Container.mergeItemStack moves the whole nonstackable remainder to its first valid empty slot. */
@Mixin(AbstractContainerMenu.class)
abstract class ResearchNoteMenuCountMixin {
    @ModifyExpressionValue(method = "moveItemStackTo", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int thaumcraft$moveWholeCopiedNotes(int amount, @Local(argsOnly = true) ItemStack stack) {
        return stack.getItem() instanceof ItemResearchNotes ? stack.getCount() : amount;
    }
}
