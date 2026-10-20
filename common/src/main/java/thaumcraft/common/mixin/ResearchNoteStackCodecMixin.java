package thaumcraft.common.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.common.lib.research.ResearchNoteOverstackCodec;

/** Wrap CODEC at creation, before OPTIONAL_CODEC/STRICT_CODEC capture it; the second lazy codec is SINGLE_ITEM_CODEC. */
@Mixin(ItemStack.class)
abstract class ResearchNoteStackCodecMixin {
    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;lazyInitialized(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;", ordinal = 0))
    private static Codec<ItemStack> thaumcraft$preserveResearchNoteCount(Codec<ItemStack> vanilla) {
        return ResearchNoteOverstackCodec.wrap(vanilla);
    }
}
