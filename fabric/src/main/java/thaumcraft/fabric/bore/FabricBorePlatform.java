package thaumcraft.fabric.bore;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.lib.bore.BorePlatform;

/** Real loader callbacks; never native game-mode destruction as well.
 * No strong world/actor cache, player-tool equip or online-human permission. */
public final class FabricBorePlatform implements BorePlatform.Bridge {
    private static final FabricBorePlatform INSTANCE = new FabricBorePlatform();
    private static final GameProfile PROFILE = new GameProfile(BorePlatform.MACHINE_UUID, BorePlatform.MACHINE_NAME);

    private FabricBorePlatform() {}
    public static void init() { BorePlatform.install(INSTANCE); }
    @Override public ServerPlayer actor(ServerLevel level) {
        canonical(level);
        ServerPlayer actor = FakePlayer.get(level, PROFILE);
        bound(level, actor);
        return actor;
    }
    @Override public boolean before(ServerLevel level, ServerPlayer actor, BlockPos target, BlockState oldState) {
        bound(level, actor);
        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, actor, target, oldState, null);
    }
    @Override public void canceled(ServerLevel level, ServerPlayer actor, BlockPos target, BlockState oldState) {
        bound(level, actor);
        PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, actor, target, oldState, null);
    }
    @Override public void after(ServerLevel level, ServerPlayer actor, BlockPos target, BlockState oldState) {
        bound(level, actor);
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, actor, target, oldState, null);
    }
    @Override public boolean isNativeMachineActor(ServerPlayer actor) {
        return actor != null && actor.getClass() == FakePlayer.class
                && actor.getUUID().equals(BorePlatform.MACHINE_UUID)
                && actor.getGameProfile().getId().equals(BorePlatform.MACHINE_UUID)
                && actor.getGameProfile().getName().equals(BorePlatform.MACHINE_NAME);
    }
    private void bound(ServerLevel level, ServerPlayer actor) {
        canonical(level);
        if (!isNativeMachineActor(actor) || actor.serverLevel() != level || actor.server != level.getServer()
                || actor.isRemoved() || level.getServer().getPlayerList().getPlayer(actor.getUUID()) != null)
            throw new IllegalStateException("Bore attribution requires the unspawned native machine actor in this exact level");
    }
    private static void canonical(ServerLevel level) {
        if (level == null || !level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level)
            throw new IllegalStateException("Bore platform callbacks require the canonical server level and thread");
    }
}
