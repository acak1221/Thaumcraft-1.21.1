package thaumcraft.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.lib.world.WardedBehaviour;

/** Native log-distance equivalent of original BlockWarded.canSustainLeaves forwarding. */
@Mixin(LeavesBlock.class)
public abstract class WardedLeavesMixin {
    @Redirect(method = "updateDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState thaumcraft$rememberedLeafSupport(LevelAccessor level, BlockPos pos) { return WardedBehaviour.query(level, pos); }
}
