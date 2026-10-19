package thaumcraft.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.lib.world.ExcavationHarvest;

@Mixin(InfestedBlock.class)
public abstract class ExcavationInfestedBlockMixin {
    // Leave super.spawnAfterBreak/XP and every ordinary/external infestation intact.
    // Original Excavation called getDrops/getExpDrop, not modern silverfish spawning.
    @Inject(method = "spawnAfterBreak(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V", require = 1, allow = 1, cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/InfestedBlock;spawnInfestation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V"))
    private void thaumcraft$noDirectSpellInfestation(BlockState state, ServerLevel level, BlockPos pos,
                                                    ItemStack tool, boolean experience, CallbackInfo ci) {
        if (ExcavationHarvest.suppressInfestation((InfestedBlock) (Object) this, state, level, pos, tool)) ci.cancel();
    }
}
