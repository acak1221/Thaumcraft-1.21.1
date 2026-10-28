/**
 * This class is new in the port of Thaumic Tinkerer to Minecraft 1.21.1 for Thaumcraft 4 Reload (no 1.7.10
 * counterpart); it is distributed under the same licence as the add-on (Creative Commons
 * Attribution-NonCommercial-ShareAlike 3.0, see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.core.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The particles of the simple blocks.
 *
 * <p>1.7.10 called {@code Thaumcraft.proxy.sparkle / wispFX2 / blockRunes / burst}, whose Thaumcraft particles have no
 * public facade in the 1.21.1 API ({@code docs/api-requests/SimpleBlocks.md}). Until they do, the effects are vanilla
 * particles: from the server (a {@link ServerLevel} sends them to every player in range) where the 1.7.10 code ran on
 * the server or on both sides, and from the client through {@code Level.addParticle} in the display ticks.</p>
 */
public final class SimpleBlockFx {
    /** The sparkle of the light: a white star (1.7.10 {@code sparkle}). */
    public static final ParticleOptions SPARKLE = ParticleTypes.END_ROD;
    /** The wisp of the shadow (1.7.10 {@code wispFX2} with the dark type 5). */
    public static final ParticleOptions SHADOW_WISP = ParticleTypes.SMOKE;

    private SimpleBlockFx() {}

    /** Sends {@code count} particles around a point to the players in range; does nothing on the client. */
    public static void send(Level level, ParticleOptions particle, double x, double y, double z, int count, double spread,
            double speed) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(particle, x, y, z, count, spread, spread, spread, speed);
        }
    }

    /** {@code sparkle(x + 0.5, y + 0.5, z + 0.5, 1)} of the Gaseous Illuminae. */
    public static void sparkleAt(Level level, BlockPos pos) {
        send(level, SPARKLE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.1, 0.0);
    }

    /** The wisp of the Gaseous Tenebrae that appears with a cell of gas. */
    public static void shadowWispAt(Level level, BlockPos pos) {
        send(level, SHADOW_WISP, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.1, 0.0);
    }

    /** The centre of a block. */
    public static Vec3 center(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}
