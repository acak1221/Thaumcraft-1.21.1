package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import thaumcraft.client.fx.particles.BlockSparkleParticle;
import thaumcraft.common.lib.network.BlockSparkleFxPayload;

/**
 * The client half of the original block sparkle packet.
 *
 * <p>Oracle {@code thaumcraft/common/lib/network/fx/PacketFXBlockSparkle.java:50-52} calls
 * {@code Thaumcraft.proxy.blockSparkle(world, x, y, z, color, 7)}, and the original
 * {@code blockSparkle} (oracle {@code thaumcraft/client/ClientProxy.java:632-644}) draws
 * {@code particleCount(7)} particles: none at the minimal particle setting, seven at the decreased one
 * and fourteen at the full one (oracle {@code ClientProxy.java:899-905}).</p>
 */
public final class BlockSparkleClientEffects {
    /** The number of particles of the original block sparkle. */
    public static final int PARTICLES = 7;
    private BlockSparkleClientEffects() {}

    /** The original particleCount: nothing at the minimal setting, the base at the decreased one, twice at all. */
    public static int particleCount(int base) {
        ParticleStatus setting = Minecraft.getInstance().options.particles().get();
        return setting == ParticleStatus.MINIMAL ? 0 : setting == ParticleStatus.DECREASED ? base : base * 2;
    }

    /** The visual answer of one payload; nothing here changes the world. */
    public static void accept(BlockSparkleFxPayload payload) {
        var client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || !level.dimension().location().equals(payload.dimension())) return;
        if (!level.hasChunkAt(payload.target())) return;
        int count = particleCount(PARTICLES);
        for (int i = 0; i < count; i++) client.particleEngine.add(new BlockSparkleParticle(level, payload.target(), payload.rgb()));
    }
}
