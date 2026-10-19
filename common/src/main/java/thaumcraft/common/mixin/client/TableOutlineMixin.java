package thaumcraft.common.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.blocks.BlockTable;

/** Old BlockTable selection bounds were separate from its full-cube ray trace. */
@Mixin(LevelRenderer.class)
public abstract class TableOutlineMixin {
    @Redirect(method = "renderHitOutline", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape thaumcraft$tableOutline(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getBlock() instanceof BlockTable ? BlockTable.selectionShape(state) : state.getShape(level, pos, context);
    }
}
