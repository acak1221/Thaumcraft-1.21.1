package thaumcraft.neoforge;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import thaumcraft.common.entities.golems.GolemHarvestPlatform;
import thaumcraft.common.entities.golems.GolemUsePlatform;

/**
 * NeoForge half of the golem use platform.
 *
 * <p>The borrowed actor is the NeoForge fake player of the reserved golem profile - the same
 * profile the harvest bridge uses, never an owner's online player.</p>
 */
public final class NeoForgeGolemUsePlatform implements GolemUsePlatform.Bridge {
    private static final NeoForgeGolemUsePlatform INSTANCE = new NeoForgeGolemUsePlatform();
    private static final GameProfile PROFILE = new GameProfile(GolemHarvestPlatform.MACHINE_UUID, GolemHarvestPlatform.MACHINE_NAME);

    private NeoForgeGolemUsePlatform() {}

    public static void init() {
        GolemUsePlatform.install(INSTANCE);
    }

    @Override
    public ServerPlayer actor(ServerLevel level) {
        canonical(level);
        ServerPlayer actor = FakePlayerFactory.get(level, PROFILE);
        if (!isNativeActor(actor) || GolemUsePlatform.isActorBorrowed(actor)
                || level.getEntity(actor.getUUID()) != null || level.getEntity(actor.getId()) != null
                || level.getServer().getPlayerList().getPlayer(actor.getUUID()) != null) {
            throw new IllegalStateException("Use requires the unspawned, unborrowed native golem actor");
        }
        return actor;
    }

    @Override
    public boolean isNativeActor(ServerPlayer actor) {
        if (actor == null || actor.getClass() != FakePlayer.class || !(actor.level() instanceof ServerLevel level)
                || !level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level
                || actor.server != level.getServer() || actor.isRemoved()
                || !actor.getUUID().equals(GolemHarvestPlatform.MACHINE_UUID)
                || !actor.getGameProfile().getId().equals(GolemHarvestPlatform.MACHINE_UUID)
                || !actor.getGameProfile().getName().equals(GolemHarvestPlatform.MACHINE_NAME)
                || actor.connection == null) return false;
        return FakePlayerFactory.get(level, PROFILE) == actor;
    }

    private static void canonical(ServerLevel level) {
        if (level == null || !level.getServer().isSameThread() || level.getServer().getLevel(level.dimension()) != level) {
            throw new IllegalStateException("Golem Use requires the canonical server level and thread");
        }
    }
}
