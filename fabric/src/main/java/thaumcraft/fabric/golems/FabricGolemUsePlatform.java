package thaumcraft.fabric.golems;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.impl.event.interaction.FakePlayerNetworkHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.common.entities.golems.GolemHarvestPlatform;
import thaumcraft.common.entities.golems.GolemUsePlatform;

/** The same cached native machine actor as Harvest; never an owner's online player. */
public final class FabricGolemUsePlatform implements GolemUsePlatform.Bridge {
    private static final FabricGolemUsePlatform INSTANCE = new FabricGolemUsePlatform();
    private static final GameProfile PROFILE = new GameProfile(GolemHarvestPlatform.MACHINE_UUID, GolemHarvestPlatform.MACHINE_NAME);

    private FabricGolemUsePlatform() {}

    public static void init() { GolemUsePlatform.install(INSTANCE); }

    @Override public ServerPlayer actor(ServerLevel level) {
        canonical(level);
        ServerPlayer actor = FakePlayer.get(level, PROFILE);
        if (!isNativeActor(actor) || GolemUsePlatform.isActorBorrowed(actor)
                || level.getEntity(actor.getUUID()) != null || level.getEntity(actor.getId()) != null
                || level.getServer().getPlayerList().getPlayer(actor.getUUID()) != null) {
            throw new IllegalStateException("Use requires the unspawned, unborrowed native golem actor");
        }
        return actor;
    }

    @Override public boolean isNativeActor(ServerPlayer actor) {
        if (actor == null || actor.getClass() != FakePlayer.class || !(actor.level() instanceof ServerLevel level)
                || !level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level
                || actor.server != level.getServer() || actor.isRemoved()
                || !actor.getUUID().equals(GolemHarvestPlatform.MACHINE_UUID)
                || !actor.getGameProfile().getId().equals(GolemHarvestPlatform.MACHINE_UUID)
                || !actor.getGameProfile().getName().equals(GolemHarvestPlatform.MACHINE_NAME)
                || actor.connection == null || actor.connection.getClass() != FakePlayerNetworkHandler.class) return false;
        return FakePlayer.get(level, PROFILE) == actor;
    }

    private static void canonical(ServerLevel level) {
        if (level == null || !level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level) {
            throw new IllegalStateException("Golem Use requires the canonical server level and thread");
        }
    }
}
