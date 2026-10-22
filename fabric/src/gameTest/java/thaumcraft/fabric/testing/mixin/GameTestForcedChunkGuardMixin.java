package thaumcraft.fabric.testing.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.testing.GameTestStateGuard;

@Mixin(ServerLevel.class)
public abstract class GameTestForcedChunkGuardMixin {
    @Inject(method = "setChunkForced", at = @At("HEAD"))
    private void thaumcraft$trackForcedChunk(int x, int z, boolean forced, CallbackInfoReturnable<Boolean> cir) {
        var level = (ServerLevel) (Object) this;
        Object key = GameTestStateGuard.resource("forced chunk", level, new ChunkPos(x, z));
        if (!forced) GameTestStateGuard.released(key);
        else if (!level.getForcedChunks().contains(ChunkPos.asLong(x, z))) GameTestStateGuard.acquired(key);
    }
}
