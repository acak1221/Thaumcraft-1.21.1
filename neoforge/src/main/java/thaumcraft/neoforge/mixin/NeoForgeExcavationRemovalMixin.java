package thaumcraft.neoforge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.lib.world.ExcavationHarvest;
import thaumcraft.common.lib.world.StreamAxeHarvest;

/**
 * NeoForge half of the excavation removals of {@code ServerPlayerGameMode.destroyBlock}.
 *
 * <p>NeoForge 21.1 patches the vanilla destruction path (oracle: the NeoForge 21.1.259 patch of
 * {@code net/minecraft/server/level/ServerPlayerGameMode.java}):</p>
 * <ul>
 *   <li>the removal is no longer a direct {@code ServerLevel.removeBlock(BlockPos, boolean)} call but
 *       the private helper {@code removeBlock(BlockPos, BlockState, boolean)}, which calls
 *       {@code BlockState.onDestroyedByPlayer(Level, BlockPos, Player, boolean, FluidState)};</li>
 *   <li>the virtual destroy {@code Block.destroy(LevelAccessor, BlockPos, BlockState)} moved into
 *       that same private helper;</li>
 *   <li>the harvest test is {@code BlockState.canHarvestBlock(BlockGetter, BlockPos, Player)} instead
 *       of {@code ServerPlayer.hasCorrectToolForDrops(BlockState)}.</li>
 * </ul>
 *
 * <p>The behaviour is the same as the Fabric half: the paid attempt owns exactly one native
 * removal, the virtual destroy is skipped inside the frame and the drops are captured from the
 * original state. {@code require = 1} keeps the NeoForge call graph fail-closed.</p>
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class NeoForgeExcavationRemovalMixin {
    @Redirect(method = "removeBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z",
            require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onDestroyedByPlayer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;ZLnet/minecraft/world/level/material/FluidState;)Z"))
    private boolean thaumcraft$witnessNativeRemoval(BlockState state, Level world, BlockPos pos, Player actor,
                                                    boolean canHarvest, FluidState fluid) {
        var level = (ServerLevel) world;
        var frame = ExcavationHarvest.currentNative((ServerPlayerGameMode) (Object) this);
        if (frame != null) return frame.remove(level, pos, false);
        boolean removed = state.onDestroyedByPlayer(world, pos, actor, canHarvest, fluid);
        StreamAxeHarvest.witnessRemoval((ServerPlayerGameMode) (Object) this, level, pos, removed);
        return removed;
    }

    @Redirect(method = "removeBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Z",
            require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;destroy(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void thaumcraft$skipVirtualDestroyed(Block block, LevelAccessor world, BlockPos pos, BlockState state) {
        if (ExcavationHarvest.currentNative((ServerPlayerGameMode) (Object) this) == null) block.destroy(world, pos, state);
    }

    @Redirect(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;canHarvestBlock(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean thaumcraft$spellDoesNotUseWandTier(BlockState state, BlockGetter world, BlockPos pos, Player actor) {
        return ExcavationHarvest.currentNative((ServerPlayerGameMode) (Object) this) != null
                || state.canHarvestBlock(world, pos, actor);
    }
}
