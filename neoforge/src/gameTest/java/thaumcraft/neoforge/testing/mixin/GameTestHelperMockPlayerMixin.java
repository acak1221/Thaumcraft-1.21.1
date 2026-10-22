package thaumcraft.neoforge.testing.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Releases the mock players of a finished GameTest.
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
    private static final Map<GameTestHelper, List<ServerPlayer>> MOCK_PLAYERS = Collections.synchronizedMap(new WeakHashMap<>());

    @Inject(method = "makeMockServerPlayerInLevel", at = @At("RETURN"))
    private void thaumcraft$trackMockServerPlayer(CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer player = cir.getReturnValue();
        if (player == null) return;
        MOCK_PLAYERS.computeIfAbsent((GameTestHelper) (Object) this, ignored -> new ArrayList<>()).add(player);
    }

    @Inject(method = "makeMockPlayer", at = @At("RETURN"))
    private void thaumcraft$trackMockPlayerGameType(CallbackInfoReturnable<net.minecraft.world.entity.player.Player> cir) {
        if (cir.getReturnValue() instanceof ServerPlayer player) {
            MOCK_PLAYERS.computeIfAbsent((GameTestHelper) (Object) this, ignored -> new ArrayList<>()).add(player);
        }
    }

    @Inject(method = "succeed", at = @At("HEAD"))
    private void thaumcraft$releaseMockPlayers(CallbackInfo cir) {
        thaumcraft$release((GameTestHelper) (Object) this);
    }

    @Inject(method = "fail(Ljava/lang/String;)V", at = @At("HEAD"))
    private void thaumcraft$releaseMockPlayersOnFail(String message, CallbackInfo cir) {
        thaumcraft$release((GameTestHelper) (Object) this);
    }

    private static void thaumcraft$release(GameTestHelper helper) {
        List<ServerPlayer> players = MOCK_PLAYERS.remove(helper);
        if (players == null) return;
        for (ServerPlayer player : players) {
            try {
                var list = player.level() == null ? null : player.level().getServer().getPlayerList();
                if (list != null && list.getPlayer(player.getUUID()) == player) list.remove(player);
            } catch (RuntimeException ignored) {
                // A finished test must never fail because its mock player could not be released.
            } finally {
                try { player.discard(); } catch (RuntimeException ignored) { }
            }
        }
    }
}
