package thaumcraft.common.mixin;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.lib.bore.BoreNativeContext;

/** Native exact-cell requests, map identity reads and old-state removal call.
 * Observers never cancel/change vanilla, including non-Bore state/BE operations. */
@Mixin(LevelChunk.class)
public abstract class BoreChunkMutationMixin implements BoreNativeContext.ChunkAccess {
    @Shadow @Final private Map<BlockPos, Object> tickersInLevel; // Native private wrapper type is intentionally not named here.

    // Owner-map reads are inherited from the required ChunkAccess-parent mixin.
    // Shadowing that parent field here leaves its remapping unresolved by AP.
    @Override public boolean thaumcraft$boreRegisteredTicker(BlockPos pos, Object wrapper) { return tickersInLevel.get(pos) == wrapper; }

    @Inject(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreStateRequest(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> cir) {
        BoreNativeContext.nativeChunkRequest((LevelChunk) (Object) this, pos);
    }
    @Inject(method = "setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreOwnerRequest(BlockEntity owner, CallbackInfo ci) {
        BoreNativeContext.nativeChunkRequest((LevelChunk) (Object) this, owner.getBlockPos());
    }
    @Inject(method = "removeBlockEntity(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreOwnerRemoval(BlockPos pos, CallbackInfo ci) {
        BoreNativeContext.nativeChunkRequest((LevelChunk) (Object) this, pos);
    }
    @Inject(method = "createBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreOwnerCreation(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        var chunk = (LevelChunk)(Object)this;
        // Native IMMEDIATE getBlockEntity also calls this for ordinary stone/
        // ore/AIR. That branch returns null without creating/calling an owner:
        // observing it as a mutation invalidates every legal non-BE harvest.
        // Real BE construction remains fenced BEFORE its factory is invoked.
        if (chunk.getBlockState(pos).hasBlockEntity()) BoreNativeContext.nativeChunkRequest(chunk, pos);
    }
    @Inject(method = "promotePendingBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreOwnerPromotion(BlockPos pos, CompoundTag tag, CallbackInfoReturnable<BlockEntity> cir) {
        BoreNativeContext.nativeChunkRequest((LevelChunk) (Object) this, pos);
    }
    @Inject(method = "clearAllBlockEntities()V", at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreOwnerClear(CallbackInfo ci) {
        BoreNativeContext.nativeChunkClear((LevelChunk) (Object) this);
    }
    // Observe the two native removal branches BEFORE their first callout.
    // Do not redirect Map.remove: Fabric Lifecycle owns that callsite and must
    // retain its unload callback. Empty CHECK reads remain nonmutating.
    @Inject(method = "getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
            at = @At("HEAD"), require = 1, allow = 1)
    private void thaumcraft$boreLazyOwnerRequest(BlockPos pos, LevelChunk.EntityCreationType creation, CallbackInfoReturnable<BlockEntity> cir) {
        var owners = (BoreNativeContext.ChunkAccess)(Object)this;
        BlockEntity owner = owners.thaumcraft$boreExistingOwner(pos);
        if (owner != null && owner.isRemoved() || owner == null && owners.thaumcraft$boreHasPendingOwner(pos))
            BoreNativeContext.nativeChunkRequest((LevelChunk)(Object)this, pos);
    }
    @Redirect(method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onRemove(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V"),
            require = 1, allow = 1)
    private void thaumcraft$boreOnRemove(BlockState oldState, Level world, BlockPos pos, BlockState replacement, boolean moving) {
        BoreNativeContext.nativeOnRemove((LevelChunk) (Object) this, oldState, world, pos, replacement, moving);
    }
}
