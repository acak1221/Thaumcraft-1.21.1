package thaumcraft.common.lib.fx;

import net.minecraft.world.phys.Vec3;

/**
 * The argument rules of the client effect facade ({@code thaumcraft.api.client.ThaumcraftClientFX}) and of the server
 * senders of {@code thaumcraft.api.ThaumcraftFX} that reach it, as pure functions of numbers.
 *
 * <p>One place holds them so the facade, the network payload and the unit tests agree: the facade checks its arguments before
 * it touches a particle, the payload checks what arrives from a server, and neither needs a client or a level to be tested.
 * Nothing here names a client class; a dedicated server loads this class.</p>
 *
 * <p>The rules refuse what would put a non-finite number or a particle that never ends into the client's particle engine; they
 * do not narrow what the originals accepted otherwise (the 1.7.10 proxy took any float).</p>
 */
public final class ClientFxRules {
    /** The sparkle types of the original {@code FXSparkle}: 0 to 7 (violet, yellow, blue, green, red, black, white, cyan). */
    public static final int SPARKLE_TYPES = 8;
    /** The colour types of the original primal wisps: 0 to 5 (the six primal colours, 5 being the dark smoke wisp). */
    public static final int WISP_TYPES = 6;
    /** The beam textures of the original beam: {@code beam}, {@code beam1}, {@code beam2}, {@code beam3}. */
    public static final int BEAM_TYPES = 4;
    /** The largest particle size the facade draws; the originals used sizes between 0.1 and 3. */
    public static final float MAX_SIZE = 16.0F;
    /** The longest rune duration, in the ticks of the 1.7.10 call; the rune lives three times as long. */
    public static final int MAX_RUNE_DURATION = 6000;
    /** The largest radius of a smoke spiral, in blocks. */
    public static final float MAX_SPIRAL_RADIUS = 64.0F;
    /** The longest distance of an essentia trail the client draws, in blocks (the essence flow packet uses the same). */
    public static final double MAX_TRAIL_DISTANCE = 64.0D;
    /** The farthest block coordinate an effect may name; the world border of the game. */
    public static final double MAX_COORDINATE = 30_000_032.0D;
    /** The most ticks the impact glint of a beam takes; the original foci passed 5 on a hit. */
    public static final int MAX_BEAM_IMPACT = 5;
    /** The largest scale of the far end of a beam. */
    public static final float MAX_BEAM_END = 8.0F;
    /** The roll of the proxy sparkle: it is drawn when {@code random.nextInt(6)} is below the particle count of 2. */
    public static final int SPARKLE_ROLL = SparkleCallRules.SPARKLE_ROLL;
    /** The lifetime multiplier of the proxy sparkle, six, as in the original call. */
    public static final int SPARKLE_MULTIPLIER = SparkleCallRules.SPARKLE_MULTIPLIER;
    /** The size of the sparkle that the type only overload draws (the original {@code sparkle(x, y, z, color)}). */
    public static final float TYPE_ONLY_SPARKLE_SIZE = 1.5F;
    /** The gravity of the sparkle that the type only overload draws: none, it hangs in the air. */
    public static final float TYPE_ONLY_SPARKLE_GRAVITY = 0.0F;

    private ClientFxRules() {}

    /** A coordinate must be a finite number inside the world; the name is used in the message. */
    public static void coordinate(String name, double value) {
        if (!Double.isFinite(value) || Math.abs(value) > MAX_COORDINATE)
            throw new IllegalArgumentException(name + " must be a finite coordinate inside the world, was " + value);
    }

    /** A float that only has to be finite (a gravity, a colour component). */
    public static void finite(String name, float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite, was " + value);
    }

    /**
     * A colour component as the particles draw it: from 0 to 1. The facade accepts any finite float, as the 1.7.10 proxy did, and
     * clamps it here, because the original packed it into a byte without a check and a component above 1 or below 0 spilled
     * into the neighbouring channels.
     */
    public static float unit(float value) {
        return value < 0.0F ? 0.0F : value > 1.0F ? 1.0F : value;
    }

    /** A size: finite, above zero and at most {@link #MAX_SIZE}. */
    public static void size(String name, float value) {
        if (!(value > 0.0F) || value > MAX_SIZE)
            throw new IllegalArgumentException(name + " must be above 0 and at most " + MAX_SIZE + ", was " + value);
    }

    /** A type index from zero to {@code count - 1}. */
    public static void type(String name, int value, int count) {
        if (value < 0 || value >= count) throw new IllegalArgumentException(name + " must be 0 to " + (count - 1) + ", was " + value);
    }

    /** The checks of {@code sparkle(level, x, y, z, size, type, gravity)}. */
    public static void sparkle(double x, double y, double z, float size, int type, float gravity) {
        coordinate("x", x); coordinate("y", y); coordinate("z", z);
        size("size", size);
        type("type", type, SPARKLE_TYPES);
        finite("gravity", gravity);
    }

    /** The checks of {@code burst(level, x, y, z, size)}. */
    public static void burst(double x, double y, double z, float size) {
        coordinate("x", x); coordinate("y", y); coordinate("z", z);
        size("size", size);
    }

    /** The checks of {@code wisp(level, x, y, z, size, red, green, blue)}. */
    public static void wisp(double x, double y, double z, float size, float red, float green, float blue) {
        coordinate("x", x); coordinate("y", y); coordinate("z", z);
        size("size", size);
        finite("red", red); finite("green", green); finite("blue", blue);
    }

    /** The checks of the typed wisp, with or without a target point. */
    public static void typedWisp(double x, double y, double z, float size, int type, float gravity) {
        coordinate("x", x); coordinate("y", y); coordinate("z", z);
        size("size", size);
        type("type", type, WISP_TYPES);
        finite("gravity", gravity);
    }

    /** The checks of {@code blockRunes(level, x, y, z, red, green, blue, duration, gravity)}. */
    public static void blockRunes(double x, double y, double z, float red, float green, float blue, int duration, float gravity) {
        coordinate("x", x); coordinate("y", y); coordinate("z", z);
        finite("red", red); finite("green", green); finite("blue", blue);
        if (duration < 1 || duration > MAX_RUNE_DURATION)
            throw new IllegalArgumentException("duration must be 1 to " + MAX_RUNE_DURATION + " ticks, was " + duration);
        finite("gravity", gravity);
    }

    /** The checks of {@code smokeSpiral(level, x, y, z, radius, start, minY, rgb)}. */
    public static void smokeSpiral(double x, double y, double z, float radius) {
        coordinate("x", x); coordinate("y", y); coordinate("z", z);
        if (!(radius >= 0.0F) || radius > MAX_SPIRAL_RADIUS)
            throw new IllegalArgumentException("radius must be 0 to " + MAX_SPIRAL_RADIUS + " blocks, was " + radius);
    }

    /** The checks of {@code beam(...)}: both ends, the texture, the end scale and the impact. */
    public static void beam(double fromX, double fromY, double fromZ, double toX, double toY, double toZ,
                            int type, float endScale, int impact) {
        coordinate("from.x", fromX); coordinate("from.y", fromY); coordinate("from.z", fromZ);
        coordinate("to.x", toX); coordinate("to.y", toY); coordinate("to.z", toZ);
        type("type", type, BEAM_TYPES);
        if (!(endScale >= 0.0F) || endScale > MAX_BEAM_END)
            throw new IllegalArgumentException("endScale must be 0 to " + MAX_BEAM_END + ", was " + endScale);
        if (impact < 0 || impact > MAX_BEAM_IMPACT)
            throw new IllegalArgumentException("impact must be 0 to " + MAX_BEAM_IMPACT + ", was " + impact);
    }

    /** The check of the scale of {@code essentiaTrail(level, from, to, phase, rgb, scale)}; the distance is checked by {@link #trailInRange}. */
    public static void essentiaTrail(float scale) {
        size("scale", scale);
    }

    /**
     * The checks of {@code essentiaTrail(level, from, to, phase, rgb, scale)}: both block positions inside the world, then the scale.
     * (A block position is three ints and can name a place far outside the world; the client would drop such a mote silently.)
     */
    public static void essentiaTrail(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, float scale) {
        coordinate("from.x", fromX); coordinate("from.y", fromY); coordinate("from.z", fromZ);
        coordinate("to.x", toX); coordinate("to.y", toY); coordinate("to.z", toZ);
        essentiaTrail(scale);
    }

    /** Whether two block positions are close enough for the trail the client draws (the essence flow rule: 64 blocks). */
    public static boolean trailInRange(double distanceSquared) {
        return distanceSquared <= MAX_TRAIL_DISTANCE * MAX_TRAIL_DISTANCE;
    }

    /** The base of the particle count of the proxy sparkle: 2 at the decreased setting, 4 at all, as in the original call. */
    public static final int SPARKLE_COUNT_BASE = 2;

    /**
     * Whether the proxy sparkle is drawn: the original draws it when {@code rand.nextInt(6) < particleCount(2)}.
     *
     * @param roll          the value of {@code random.nextInt(}{@link #SPARKLE_ROLL}{@code )}
     * @param particleCount the particle count of the setting, 0 minimal, 2 decreased, 4 all
     */
    public static boolean sparkleDraws(int roll, int particleCount) {
        return roll < particleCount;
    }

    /**
     * The particle count of the original {@code ClientProxy.particleCount(base)} for a particle setting: none at the
     * minimal setting, {@code base} at the decreased one and {@code 2 * base} at all. The doubling saturates at
     * {@link Integer#MAX_VALUE} (the original's {@code int} multiplication would wrap to a negative count for a base above
     * {@code 2^30}), so the answer for a non-negative base is never negative.
     *
     * @param setting 0 for all, 1 for decreased, 2 for minimal (the ordinal of {@code ParticleStatus})
     */
    public static int particleCount(int setting, int base) {
        return setting == 2 ? 0 : setting == 1 ? base : base > Integer.MAX_VALUE / 2 ? Integer.MAX_VALUE : base * 2;
    }

    /**
     * Where the particle of {@code blockRunes} sits. The original proxy ({@code ClientProxy.java:917-922}) creates the rune at
     * the point of the call plus half a block on every axis, so a caller that passes the corner of a block gets the middle.
     */
    public static double runeCentre(double coordinate) {
        return coordinate + 0.5D;
    }

    /**
     * Where the beam of a wand starts: the tip of the wand in the hand of a player. The original {@code FXBeamWand} took the eye of
     * the player, moved it {@code 0.066} against the cosine and {@code 0.04} against the sine of the yaw, {@code 0.06} down, and
     * {@code 0.3} along the look vector; the excavation focus of this port sends the same point to the clients.
     *
     * @param eye        the eye position of the player
     * @param look       the unit look vector of the player
     * @param yawDegrees the yaw of the player in degrees ({@code Entity.getYRot()})
     */
    public static Vec3 wandTip(Vec3 eye, Vec3 look, float yawDegrees) {
        float yaw = yawDegrees * (float) Math.PI / 180;
        return eye.add(-Math.cos(yaw) * .066, -.06, -Math.sin(yaw) * .04).add(look.scale(.3));
    }

    /** The low 24 bits of a colour: the facade takes {@code 0xRRGGBB} and ignores anything above, as {@code java.awt.Color(int)} did. */
    public static int rgb(int colour) {
        return colour & 0xFFFFFF;
    }
}
