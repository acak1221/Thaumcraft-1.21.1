package thaumcraft.fabric.mixin.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.common.lib.world.ExcavationHarvest;
import thaumcraft.common.lib.world.StreamAxeHarvest;

/**
 * Fabric half of the excavation removals of {@code ServerPlayerGameMode.destroyBlock}.
 *
 * <p>Fabric runs the unmodified vanilla 1.21.1 call graph: the removal is
 * {@code ServerLevel.removeBlock(BlockPos, boolean)}, the virtual destroy is
 * {@code Block.destroy(LevelAccessor, BlockPos, BlockState)} and the harvest test is
 * {@code ServerPlayer.hasCorrectToolForDrops(BlockState)}. NeoForge patches all three, so the
 * NeoForge half lives in {@code thaumcraft.neoforge.mixin.NeoForgeExcavationRemovalMixin}. Both
 * halves keep {@code require = 1}: a drifted vanilla call graph fails closed instead of silently
 * harvesting without the paid attempt.</p>
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class FabricExcavationRemovalMixin {
    @Redirect(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean thaumcraft$witnessNativeRemoval(ServerLevel world, BlockPos pos, boolean moving) {
        var frame = ExcavationHarvest.currentNative((ServerPlayerGameMode) (Object) this);
        if (frame != null) return frame.remove(world, pos, moving);
        boolean removed = world.removeBlock(pos, moving);
        StreamAxeHarvest.witnessRemoval((ServerPlayerGameMode) (Object) this, world, pos, removed);
        return removed;
    }

    @Redirect(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;destroy(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void thaumcraft$skipVirtualDestroyed(Block block, LevelAccessor world, BlockPos pos, BlockState state) {
        if (ExcavationHarvest.currentNative((ServerPlayerGameMode) (Object) this) == null) block.destroy(world, pos, state);
    }

    @Redirect(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", require = 1, allow = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;hasCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean thaumcraft$spellDoesNotUseWandTier(ServerPlayer actor, BlockState state) {
        return ExcavationHarvest.currentNative((ServerPlayerGameMode) (Object) this) != null || actor.hasCorrectToolForDrops(state);
    }
}
