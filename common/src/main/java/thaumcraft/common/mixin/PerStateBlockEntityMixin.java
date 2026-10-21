package thaumcraft.common.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.blocks.PerStateBlockEntity;

/**
 * Lets a block decide its block entity per state, the way the 1.7.10 original decided it per
 * metadata.
 *
 * <p>Minecraft 1.21.1 answers the question of the block entity per block:
 * {@code BlockBehaviour$BlockStateBase#hasBlockEntity} is {@code this.getBlock() instanceof
 * EntityBlock} (Minecraft 1.21.1
 * {@code net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase}:803-805). The
 * original answered it per metadata ({@code hasTileEntity(int)}), so a block that carried a tile
 * for one metadata only claimed a tile for all of them in the port. Two things were wrong with
 * that: the tile of a metadata that never had one was created (and a {@code DUMMY} marker was
 * written for it while the chunk was generated), and the piston refused the block, because
 * {@code PistonBaseBlock#isPushable} answers {@code !blockState.hasBlockEntity()} (Minecraft
 * 1.21.1 {@code net/minecraft/world/level/block/piston/PistonBaseBlock}:277).
 *
 * <p>Only the blocks that answer {@link PerStateBlockEntity} are touched; for every other block
 * the vanilla answer stays as it is. The state that has a tile keeps the tile, the states that
 * have none are moved by a piston like in the original.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class PerStateBlockEntityMixin {
    /** The state itself: {@code BlockBehaviour$BlockStateBase#asState()}. */
    @Shadow protected abstract BlockState asState();

    /**
     * The vanilla answer of {@code hasBlockEntity}: the block is an entity block.
     */
    @Inject(method = "hasBlockEntity", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$blockEntityPerState(CallbackInfoReturnable<Boolean> cir) {
        BlockState state = asState();
        if (state.getBlock() instanceof PerStateBlockEntity perState) {
            cir.setReturnValue(perState.hasBlockEntityForState(state));
        }
    }
}
