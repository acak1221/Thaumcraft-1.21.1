package thaumcraft.neoforge;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import thaumcraft.common.entities.golems.GolemHarvestPlatform;

/**
 * NeoForge half of the golem harvest platform.
 *
 * <p>Fabric asks the loader whether the machine actor may break the target with
 * {@code PlayerBlockBreakEvents.BEFORE} and whether a replant may be placed with the untouched
 * {@code UseBlockCallback}. NeoForge owns the cancellable {@link BlockEvent.BreakEvent} and the
 * cancellable {@link PlayerInteractEvent.RightClickBlock}; both are posted here, so every native
 * protection mod of the loader sees the machine actor exactly as it sees a real player.</p>
 */
public final class NeoForgeGolemHarvestPlatform implements GolemHarvestPlatform.Bridge {
    private static final NeoForgeGolemHarvestPlatform INSTANCE = new NeoForgeGolemHarvestPlatform();
    private static final GameProfile PROFILE = new GameProfile(GolemHarvestPlatform.MACHINE_UUID, GolemHarvestPlatform.MACHINE_NAME);

    private NeoForgeGolemHarvestPlatform() {}

    public static void init() {
        GolemHarvestPlatform.install(INSTANCE);
    }

    @Override
    public ServerPlayer actor(ServerLevel level) {
        canonical(level);
        ServerPlayer actor = FakePlayerFactory.get(level, PROFILE);
        bound(level, actor);
        return actor;
    }

    @Override
    public boolean isNativeActor(ServerPlayer actor) {
        return actor != null && actor.getClass() == FakePlayer.class
                && actor.getUUID().equals(GolemHarvestPlatform.MACHINE_UUID)
                && actor.getGameProfile().getId().equals(GolemHarvestPlatform.MACHINE_UUID)
                && actor.getGameProfile().getName().equals(GolemHarvestPlatform.MACHINE_NAME);
    }

    @Override
    public boolean before(ServerLevel level, ServerPlayer actor, BlockPos pos, BlockState state) {
        bound(level, actor);
        var event = new BlockEvent.BreakEvent(level, pos, state, actor);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override
    public void canceled(ServerLevel level, ServerPlayer actor, BlockPos pos, BlockState state) {
        bound(level, actor);
        // NeoForge has no separate cancelled notification; the cancelled
        // BreakEvent of before() already carried that state.
    }

    @Override
    public void after(ServerLevel level, ServerPlayer actor, BlockPos pos, BlockState state) {
        bound(level, actor);
        // NeoForge has no separate succeeded notification; the golem owns its own witness.
    }

    /** NeoForge's right click consultation for a replanting placement. */
    @Override
    public boolean allowsPlacement(ServerLevel level, ServerPlayer actor, BlockHitResult hit) {
        bound(level, actor);
        var event = new PlayerInteractEvent.RightClickBlock(actor, InteractionHand.MAIN_HAND, hit.getBlockPos(), hit);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled() && event.getCancellationResult() != InteractionResult.FAIL;
    }

    private void bound(ServerLevel level, ServerPlayer actor) {
        canonical(level);
        if (!INSTANCE.isNativeActor(actor) || actor.serverLevel() != level || actor.server != level.getServer()
                || actor.isRemoved() || level.getServer().getPlayerList().getPlayer(actor.getUUID()) != null)
            throw new IllegalStateException("Crop permissions require the unspawned native golem actor in this level");
    }

    private static void canonical(ServerLevel level) {
        if (level == null || !level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level)
            throw new IllegalStateException("Crop callbacks require the canonical server level and thread");
    }
}
