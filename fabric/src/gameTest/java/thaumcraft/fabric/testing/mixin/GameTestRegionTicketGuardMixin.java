package thaumcraft.fabric.testing.mixin;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thaumcraft.common.testing.GameTestStateGuard;

@Mixin(ServerChunkCache.class)
public abstract class GameTestRegionTicketGuardMixin {
    @Inject(method = "addRegionTicket", at = @At("RETURN"))
    private void thaumcraft$acquireTicket(TicketType<?> type, ChunkPos chunk, int radius, Object value, CallbackInfo ci) {
        // Native expiring tickets (e.g. teleport/portal) are not manual fixture leases.
        if (type.timeout() != 0) return;
        GameTestStateGuard.acquired(GameTestStateGuard.resource("region ticket", this, type, chunk, radius, value));
    }

    @Inject(method = "removeRegionTicket", at = @At("RETURN"))
    private void thaumcraft$releaseTicket(TicketType<?> type, ChunkPos chunk, int radius, Object value, CallbackInfo ci) {
        GameTestStateGuard.released(GameTestStateGuard.resource("region ticket", this, type, chunk, radius, value));
    }
}
