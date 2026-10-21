package thaumcraft.common.mixin;

import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.blocks.OriginalPistonPushable;

/**
 * Asks the piston of Minecraft 1.21.1 the question that the 1.7.10 original asked it.
 *
 * <p>The vanilla piston of 1.21.1 refuses every block that carries a block entity: the last line
 * of {@code PistonBaseBlock#isPushable} is {@code return !blockState.hasBlockEntity();} and
 * {@code BlockState#hasBlockEntity} is {@code block instanceof EntityBlock} (Minecraft 1.21.1
 * {@code net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase}:552-554, the decompilation of CFR 0.152). The
 * question is therefore asked per block, not per state, and every block entity block of the port
 * is refused, the blocks of Thaumcraft included.
 *
 * <p>The 1.7.10 original refused every block whose class implemented {@code
 * net.minecraft.block.ITileEntityProvider} and moved every other block, for every metadata (the
 * vanilla 1.7.10 {@code net.minecraft.block.BlockPistonBase#canPushBlock} ended with {@code
 * return !(block instanceof ITileEntityProvider);}). The answer of the original for the families
 * of Thaumcraft is in {@code thaumcraft.common.blocks.OriginalPistonRule}.
 *
 * <p>The mixin is common: the file {@code thaumcraft-common.mixins.json} is read by both loaders,
 * and the target is a vanilla class, so no loader module needs a copy of it.
 */
@Mixin(PistonBaseBlock.class)
public abstract class PistonPushabilityMixin {

    /**
     * The question of the piston, in the way the vanilla code asks it: the value that is returned
     * here is the answer of {@code BlockState#hasBlockEntity()}, and the vanilla code reads it as
     * {@code !hasBlockEntity()}.
     *
     * <p>For a block of Thaumcraft the answer of the original replaces the answer of the vanilla
     * code, so the value that is returned here is the negation of the answer of the original. For
     * every other block the answer of the vanilla code stays as it is.
     *
     * @param state the state the piston asks about
     * @return the answer of the piston for the state
     */
    @Redirect(method = "isPushable", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z"), require = 1)
    private static boolean thaumcraft$originalPushability(BlockState state) {
        if (state.getBlock() instanceof OriginalPistonPushable original) {
            return !original.isPushableInOriginal(state);
        }
        return state.hasBlockEntity();
    }
}
