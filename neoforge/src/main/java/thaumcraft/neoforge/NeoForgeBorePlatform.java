package thaumcraft.neoforge;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import thaumcraft.common.lib.bore.BorePlatform;

/**
 * NeoForge half of the Arcane Bore platform.
 *
 * <p>Fabric asks the loader whether the machine actor may break the target with
 * {@code PlayerBlockBreakEvents.BEFORE} and notifies the loader with the cancelled and after
 * notifications. NeoForge has no such trio: its protection event is the cancellable
 * {@link BlockEvent.BreakEvent}, which the game fires in
 * {@code ServerPlayerGameMode.destroyBlock}. The bore never calls that method - it removes the
 * block itself - so this bridge posts the very same event and honours its cancellation.</p>
 *
 * <p>The actor is the NeoForge fake player of the reserved machine profile. It is never an online
 * player and never inherits an owner's permissions or tools.</p>
 */
public final class NeoForgeBorePlatform implements BorePlatform.Bridge {
    private static final NeoForgeBorePlatform INSTANCE = new NeoForgeBorePlatform();
    private static final GameProfile PROFILE = new GameProfile(BorePlatform.MACHINE_UUID, BorePlatform.MACHINE_NAME);

    private NeoForgeBorePlatform() {}

    public static void init() {
        BorePlatform.install(INSTANCE);
    }

    @Override
    public ServerPlayer actor(ServerLevel level) {
        canonical(level);
        ServerPlayer actor = FakePlayerFactory.get(level, PROFILE);
        bound(level, actor);
        return actor;
    }

    @Override
    public boolean before(ServerLevel level, ServerPlayer actor, BlockPos target, BlockState oldState) {
        bound(level, actor);
        var event = new BlockEvent.BreakEvent(level, target, oldState, actor);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override
    public void canceled(ServerLevel level, ServerPlayer actor, BlockPos target, BlockState oldState) {
        bound(level, actor);
        // NeoForge has no separate "the break was cancelled" notification. The
        // cancelled BreakEvent of before() already carried that state to every
        // native listener, so the cancelled notification has no extra payload.
    }

    @Override
    public void after(ServerLevel level, ServerPlayer actor, BlockPos target, BlockState oldState) {
        bound(level, actor);
        // NeoForge has no "the break succeeded" notification either. The bore
        // already owns the durable witness of its own removal.
    }

    @Override
    public boolean isNativeMachineActor(ServerPlayer actor) {
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
