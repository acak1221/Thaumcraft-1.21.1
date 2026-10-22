package thaumcraft.fabric.testing.mixin;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.testing.MockPlayerCleanup;

/**
 * Tracks helper-created players for release by {@link GameTestInfoMockPlayerMixin}.
 *
 * <p>Minecraft 1.21.1's {@code GameTestHelper.makeMockServerPlayerInLevel} adds a real
 * connected {@code ServerPlayer} to the server's player list and never removes it. A player
 * pins the chunks of its chunk ticket for the rest of the session, and in the flat GameTest
 * world every one of those chunks is a half-generated proto chunk that the chunk map keeps
 * until something asks for it at full status. The port's suite creates hundreds of mock
 * players, so the GameTest server ends the run with tens of thousands of proto chunks in the
 * chunk map - the measured cause of the {@code OutOfMemoryError} in the chunk save path.
 *
 * <p>The player is removed from the player list (which releases its chunk tickets) and then
 * discarded, exactly the way the port's own {@code FocusUpgradeRuntimeChecks.Actor} releases
 * the real actor it places. The player is still alive and usable for the whole lifetime of
 * its test.</p>
 */
@Mixin(GameTestHelper.class)
public abstract class GameTestHelperMockPlayerMixin {
    @Shadow @Final private GameTestInfo testInfo;

    @Inject(method = "makeMockServerPlayerInLevel", at = @At("RETURN"))
    private void thaumcraft$trackMockServerPlayer(CallbackInfoReturnable<ServerPlayer> cir) {
        MockPlayerCleanup.track(testInfo, cir.getReturnValue());
    }

    @Inject(method = "makeMockPlayer", at = @At("RETURN"))
    private void thaumcraft$trackMockPlayerGameType(CallbackInfoReturnable<net.minecraft.world.entity.player.Player> cir) {
        if (cir.getReturnValue() instanceof ServerPlayer player) {
            MockPlayerCleanup.track(testInfo, player);
        }
    }
}
