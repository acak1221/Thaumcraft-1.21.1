package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.ThaumcraftExcavationClient;
import thaumcraft.client.fx.particles.BlockRuneParticle;
import thaumcraft.client.fx.particles.EssentiaTrailParticle;
import thaumcraft.client.fx.particles.FXSmokeSpiral;
import thaumcraft.client.fx.particles.MonsterSpriteParticle;
import thaumcraft.client.fx.particles.PrimalWispParticle;
import thaumcraft.client.fx.particles.TypeSparkleParticle;
import thaumcraft.common.lib.fx.ClientFxRules;

/**
 * The client half of {@code thaumcraft.api.client.ThaumcraftClientFX}: each method does what the 1.7.10 proxy method of the
 * same name did, with the particle classes of this port. The public facade validates its arguments
 * ({@link ClientFxRules}) and calls these only for a client level, so every method here assumes valid arguments and a level
 * that is a {@link ClientLevel}.
 *
 * <p>This class names client classes; a dedicated server never loads it. The facade reaches it only after
 * {@code level.isClientSide()} is true.</p>
 */
public final class ClientFxFacade {
    private ClientFxFacade() {}

    /** The client level of a level, or {@code null} for anything else; the particle engine is not thread safe, so another thread is refused. */
    private static ClientLevel on(Level level) {
        if (!(level instanceof ClientLevel client)) return null;
        if (!Minecraft.getInstance().isSameThread())
            throw new IllegalStateException("The client effect facade must be called on the client thread");
        return client;
    }

    /** The original {@code ClientProxy.particleCount(base)}: nothing at the minimal setting, {@code base} at decreased, twice at all. */
    public static int particleCount(Level level, int base) {
        if (on(level) == null) return 0;
        return ClientFxRules.particleCount(Minecraft.getInstance().options.particles().get().getId(), base);
    }

    /**
     * The original {@code ClientProxy.sparkle(x, y, z, size, type, gravity)}: drawn when {@code random.nextInt(6)} is below the
     * particle count of two, with the lifetime multiplier six.
     */
    public static void sparkle(Level level, double x, double y, double z, float size, int type, float gravity) {
        ClientLevel client = on(level);
        if (client == null) return;
        int count = ClientFxRules.particleCount(Minecraft.getInstance().options.particles().get().getId(), ClientFxRules.SPARKLE_COUNT_BASE);
        if (!ClientFxRules.sparkleDraws(client.random.nextInt(ClientFxRules.SPARKLE_ROLL), count)) return;
        Minecraft.getInstance().particleEngine.add(new TypeSparkleParticle(client, x, y, z, size, type, ClientFxRules.SPARKLE_MULTIPLIER, gravity));
    }

    /** The original {@code ClientProxy.burst(world, x, y, z, scale)}. */
    public static void burst(Level level, double x, double y, double z, float size) {
        ClientLevel client = on(level);
        if (client != null) MonsterSpriteParticle.burst(client, x, y, z, size);
    }

    /** The original {@code ClientProxy.wispFX(world, x, y, z, size, red, green, blue)}. */
    public static void wisp(Level level, double x, double y, double z, float size, float red, float green, float blue) {
        ClientLevel client = on(level);
        if (client != null) MonsterSpriteParticle.mote(client, x, y, z, size, ClientFxRules.unit(red), ClientFxRules.unit(green), ClientFxRules.unit(blue));
    }

    /** The original {@code ClientProxy.wispFX2(world, x, y, z, size, type, shrink, clip, gravity)}. */
    public static void wisp(Level level, double x, double y, double z, float size, int type, boolean shrink, boolean noClip, float gravity) {
        ClientLevel client = on(level);
        if (client != null) PrimalWispParticle.spawn(client, x, y, z, size, type, shrink, noClip, gravity);
    }

    /** The original {@code ClientProxy.wispFX3(world, x, y, z, tx, ty, tz, size, type, shrink, speed)}. */
    public static void wisp(Level level, double x, double y, double z, double tx, double ty, double tz, float size, int type,
                            boolean shrink, float gravity) {
        ClientLevel client = on(level);
        if (client != null) PrimalWispParticle.spawnToward(client, x, y, z, tx, ty, tz, size, type, shrink, gravity);
    }

    /**
     * The original {@code ClientProxy.blockRunes(world, x, y, z, red, green, blue, duration, gravity)} (oracle
     * {@code ClientProxy.java:917-922}): exactly one rune particle, half a block above and beside the point on every axis.
     */
    public static void blockRunes(Level level, double x, double y, double z, float red, float green, float blue, int duration, float gravity) {
        ClientLevel client = on(level);
        if (client == null) return;
        Minecraft.getInstance().particleEngine.add(new BlockRuneParticle(client, ClientFxRules.runeCentre(x), ClientFxRules.runeCentre(y),
                ClientFxRules.runeCentre(z), ClientFxRules.unit(red), ClientFxRules.unit(green), ClientFxRules.unit(blue), duration, gravity));
    }

    /** The original {@code ClientProxy.smokeSpiral(world, x, y, z, radius, start, minY, color)}. */
    public static void smokeSpiral(Level level, double x, double y, double z, float radius, int start, int minY, int rgb) {
        ClientLevel client = on(level);
        if (client != null) FXSmokeSpiral.spiral(client, x, y, z, radius, start, minY, ClientFxRules.rgb(rgb));
    }

    /** The original {@code ClientProxy.essentiaTrailFx(world, x, y, z, tx, ty, tz, count, color, scale)}: one mote from the centre of a block to the centre of another. */
    public static void essentiaTrail(Level level, BlockPos from, BlockPos to, int phase, int rgb, float scale) {
        ClientLevel client = on(level);
        if (client == null || !ClientFxRules.trailInRange(from.distSqr(to))) return;
        Minecraft.getInstance().particleEngine.add(new EssentiaTrailParticle(client, from, Vec3.atCenterOf(from), Vec3.atCenterOf(to),
                ClientFxRules.rgb(rgb), phase, scale));
    }

    /**
     * The original {@code ClientProxy.beamCont(...)}, without the returned object: a beam from {@code from} to {@code to} that the
     * caller keeps alive by calling again, drawn only when the particle setting allows particles (the original asked
     * {@code particleCount(8)}).
     */
    public static void beam(Level level, Entity owner, Vec3 from, Vec3 to, int rgb, int type, boolean reverse, float endScale, int impact) {
        if (on(level) == null || owner.level() != level || ClientFxRules.particleCount(Minecraft.getInstance().options.particles().get().getId(), 8) <= 0) return;
        ThaumcraftExcavationClient.addFacadeBeam(owner, from, to, ClientFxRules.rgb(rgb), type, reverse, endScale, impact);
    }
}
