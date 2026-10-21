package thaumcraft.fabric.golems;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.entities.golems.GolemHarvestPlatform;

/** Real Fabric claim/protection callbacks; never an online player's permissions or tools. */
public final class FabricGolemHarvestPlatform implements GolemHarvestPlatform.Bridge {
    private static final FabricGolemHarvestPlatform INSTANCE = new FabricGolemHarvestPlatform();
    private static final GameProfile PROFILE = new GameProfile(GolemHarvestPlatform.MACHINE_UUID, GolemHarvestPlatform.MACHINE_NAME);
    private FabricGolemHarvestPlatform() {}
    public static void init() { GolemHarvestPlatform.install(INSTANCE); }

    @Override public ServerPlayer actor(ServerLevel level) {
        canonical(level);
        ServerPlayer actor = FakePlayer.get(level, PROFILE);
        bound(level, actor);
        return actor;
    }
    @Override public boolean isNativeActor(ServerPlayer actor) {
        return actor != null && actor.getClass() == FakePlayer.class
                && actor.getUUID().equals(GolemHarvestPlatform.MACHINE_UUID)
                && actor.getGameProfile().getId().equals(GolemHarvestPlatform.MACHINE_UUID)
                && actor.getGameProfile().getName().equals(GolemHarvestPlatform.MACHINE_NAME);
    }
    @Override public boolean sharedBreak(ServerLevel level, ServerPlayer actor, BlockPos pos, BlockState state) {
        bound(level, actor);
        // Fabric has no native counterpart of the shared Architectury break event,
        // so the loader delivers it here, exactly once.
        return !dev.architectury.event.events.common.BlockEvent.BREAK.invoker()
                .breakBlock(level, pos, state, actor, null).isFalse();
    }
    @Override public boolean before(ServerLevel level, ServerPlayer actor, BlockPos pos, BlockState state) {
        bound(level, actor);
        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, actor, pos, state, null);
    }
    @Override public void canceled(ServerLevel level, ServerPlayer actor, BlockPos pos, BlockState state) {
        bound(level, actor);
        PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, actor, pos, state, null);
    }
    @Override public void after(ServerLevel level, ServerPlayer actor, BlockPos pos, BlockState state) {
        bound(level, actor);
        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, actor, pos, state, null);
    }
    /** Fabric's UseBlockCallback is the claim mods' placement hook; only an untouched PASS means nobody objected or took over. */
    @Override public boolean allowsPlacement(ServerLevel level, ServerPlayer actor, BlockHitResult hit) {
        bound(level, actor);
        return UseBlockCallback.EVENT.invoker().interact(actor, level, InteractionHand.MAIN_HAND, hit) == InteractionResult.PASS;
    }
    private static void bound(ServerLevel level, ServerPlayer actor) {
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
