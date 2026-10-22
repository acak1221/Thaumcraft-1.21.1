package thaumcraft.neoforge.testing.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.neoforge.testing.NeoForgeBreakPhases;

/**
 * The NeoForge half of the native AFTER phase of a block break.
 *
 * <p>NeoForge 21.1 has no "block was broken" event: the removal is the private helper
 * {@code ServerPlayerGameMode.removeBlock(BlockPos, BlockState, boolean)}, which calls
 * {@code BlockState.onDestroyedByPlayer(...)} and only then the virtual
 * {@code Block.destroy(...)}. The helper returns whether the block was really removed, so its
 * return value is the exact witness of a real removal - the same witness the Fabric AFTER event
 * has, which the Fabric API injects directly before the {@code Block.destroy} call of
 * {@code ServerPlayerGameMode.destroyBlock}.</p>
 *
 * <p>The target and its descriptor were read from the NeoForge-patched class
 * {@code net/minecraft/server/level/ServerPlayerGameMode.class} of the patched Minecraft archive
 * of the NeoForge 21.1.259 userdev jar: {@code private boolean removeBlock(BlockPos, BlockState,
 * boolean)}. {@code require = 1} keeps the observation fail-closed if the NeoForge call graph
 * ever changes.</p>
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class NeoForgeBreakPhaseMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "removeBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z",
            at = @At("RETURN"), require = 1, allow = 1)
    private void thaumcraft$afterNativeRemoval(BlockPos pos, BlockState state, boolean canHarvest,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        NeoForgeBreakPhases.afterRemoval(level, player, pos, state);
    }
}
