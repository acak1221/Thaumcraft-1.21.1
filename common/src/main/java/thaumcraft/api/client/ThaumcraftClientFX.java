package thaumcraft.api.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.fx.ClientFxFacade;
import thaumcraft.common.lib.fx.ClientFxRules;

/**
 * The client-side particle effects of Thaumcraft that 1.7.10 add-ons drew through {@code Thaumcraft.proxy}: the sparkle, the
 * burst, the wisp, the block runes, the continuous beam, the smoke spiral and the essentia trail.
 *
 * <p><b>Client only.</b> These draw on the client; the 1.7.10 {@code CommonProxy} had empty bodies for them, and so does this
 * class on a server: a call with a server level (or with {@code null}) draws nothing and does not fail. Only the signatures
 * name common types, so this class loads on a dedicated server without any client class (the client classes are reached only
 * from a call with a client level). Call it from the <b>client thread</b> (a block's {@code animateTick}, an item's
 * {@code inventoryTick} or {@code use} on the client, a client tick or render callback); on a client level from any other thread it
 * throws {@link IllegalStateException}, because the particle engine is not thread safe. To make every nearby client draw an
 * effect that the server decides on, call the server twins in {@link thaumcraft.api.ThaumcraftFX} ({@code sparkle},
 * {@code burst}, {@code blockRunes}, {@code smokeSpiral}, {@code essentiaTrail}).</p>
 *
 * <p><b>Units.</b> Positions are world coordinates in blocks (doubles) unless a method says "block"; sizes are the particle scale
 * of the original ({@code 1} is the ordinary size, the originals used 0.1 to 3); durations and lifetimes are game ticks; colours
 * are {@code 0xRRGGBB} ints (the bits above the low 24 are ignored) or float components from 0 to 1 (a finite component
 * outside that range is clamped to it); gravity is the original unit, the particle falls by {@code 0.04 * gravity} per tick
 * (negative rises).</p>
 *
 * <p><b>Arguments.</b> Every method checks its arguments first, on every side, and throws {@link IllegalArgumentException} for a
 * non-finite number (a colour component included), a coordinate outside the world, a size of zero or less, a type outside its
 * range or a duration that is not positive, so a mistake shows in a server-only test too and cannot put NaN into the particle
 * engine. The ranges are the constants of {@code thaumcraft.common.lib.fx.ClientFxRules} and are repeated in each method.</p>
 *
 * <p><b>Particle setting.</b> The sparkle and the beam honour the player's particle setting as the originals did (nothing at
 * "minimal"); {@link #particleCount} is the original {@code particleCount(base)} for add-on code that loops. The other effects
 * draw whatever the add-on asks for, as the originals did: the caller thins them with {@link #particleCount}.</p>
 *
 * <p><b>Not here.</b> The original proxy also had {@code wispFX4} (a wisp that homes on an entity), {@code beamPulseFX},
 * {@code bolt}, {@code arcLightning}, {@code crucibleBubble}, {@code blockWard}, {@code infusedStoneSparkle} and the infusion
 * particles; they are internal to Thaumcraft's own effects and have no facade. The beam returns no handle (see
 * {@link #beam}).</p>
 *
 * <p><b>Verification.</b> The argument rules and the geometry of the pure parts are asserted by unit tests; the particles
 * themselves have <em>not yet been inspected on a rendered frame</em> (see section 9 of {@code docs/audit/KNOWN_DEVIATIONS.md}):
 * they are the very particle classes Thaumcraft's own effects use, but a facade call that works is not a screenshot of how it
 * looks.</p>
 */
public final class ThaumcraftClientFX {
    private ThaumcraftClientFX() {}

    private static boolean client(Level level) {
        return level != null && level.isClientSide();
    }

    /**
     * The original {@code Thaumcraft.proxy.particleCount(base)}: how many particles a loop of the add-on should draw at the
     * player's particle setting. {@code 0} at "minimal", {@code base} at "decreased", {@code 2 * base} at "all". Always {@code 0}
     * for a server level or {@code null}.
     *
     * @param level a level; only a client level gives a non-zero answer
     * @param base  the count the add-on wants at the decreased setting, zero or more
     * @throws IllegalArgumentException when {@code base} is negative
     */
    public static int particleCount(Level level, int base) {
        if (base < 0) throw new IllegalArgumentException("base must not be negative, was " + base);
        return client(level) ? ClientFxFacade.particleCount(level, base) : 0;
    }

    /**
     * The original {@code Thaumcraft.proxy.sparkle(x, y, z, size, color, gravity)}: one sparkle, the glittering quad that
     * shrinks away over 18 ticks ({@code 3 * 6}) and falls by {@code 0.04 * gravity} per tick. The sparkle is drawn only on
     * a roll: with the particle setting at "all" four rolls of six draw it, at "decreased" two of six, at "minimal" none, so
     * a caller that wants a sure sparkle calls it several times, as the originals did.
     *
     * @param level   a client level; anything else draws nothing
     * @param x       the position, in blocks
     * @param y       the position, in blocks
     * @param z       the position, in blocks
     * @param size    the scale, above 0 and at most 16 (the originals used 0.5 to 2)
     * @param type    the colour, 0 to 7 (the {@code color} index of the original): 0 violet, 1 yellow, 2 blue, 3 green, 4 red,
     *                5 black (drawn like smoke), 6 white, 7 cyan; each with the small random variation of the original
     * @param gravity how fast it falls; 0 hangs in the air, a negative number rises
     */
    public static void sparkle(Level level, double x, double y, double z, float size, int type, float gravity) {
        ClientFxRules.sparkle(x, y, z, size, type, gravity);
        if (client(level)) ClientFxFacade.sparkle(level, x, y, z, size, type, gravity);
    }

    /**
     * The original {@code Thaumcraft.proxy.sparkle(x, y, z, color)}: {@link #sparkle(Level, double, double, double, float, int,
     * float)} with the size {@code 1.5} and no gravity. (The size is the one of the audited legacy source of this repository;
     * the 1.7.10 jar was not available to confirm it.)
     *
     * @param level a client level; anything else draws nothing
     * @param type  the colour, 0 to 7, see the long form
     */
    public static void sparkle(Level level, double x, double y, double z, int type) {
        sparkle(level, x, y, z, ClientFxRules.TYPE_ONLY_SPARKLE_SIZE, type, 0.0F);
    }

    /**
     * The original {@code Thaumcraft.proxy.burst(world, x, y, z, scale)}: the white puff of the wisp and the shock orb, a
     * flipbook of 32 frames that plays for 31 ticks at three quarters opacity. It is drawn in vanilla's particle layer, not
     * in Thaumcraft's, so the particle setting does not thin it.
     *
     * @param level a client level; anything else draws nothing
     * @param size  the scale of the puff, which is its half size, above 0 and at most 16 (the originals used 0.5 to 3)
     */
    public static void burst(Level level, double x, double y, double z, float size) {
        ClientFxRules.burst(x, y, z, size);
        if (client(level)) ClientFxFacade.burst(level, x, y, z, size);
    }

    /**
     * The original {@code Thaumcraft.proxy.wispFX(world, x, y, z, size, red, green, blue)}: one mote of the wisp in any colour.
     * It lives about 36 to 51 ticks, grows to full size by half its life and shrinks again, drifts with gravity {@code 0.02},
     * is pushed out of solid blocks and is not drawn farther from the camera than 50 blocks (25 on fast graphics). A red of
     * exactly 0 is drawn as 1, a quirk of the original.
     *
     * @param level a client level; anything else draws nothing
     * @param size  the scale, above 0 and at most 16 (the wisp entity uses 0.1)
     * @param red   0 to 1; any finite value is accepted and one outside that range is clamped to it
     * @param green 0 to 1, clamped like {@code red}
     * @param blue  0 to 1, clamped like {@code red}
     */
    public static void wisp(Level level, double x, double y, double z, float size, float red, float green, float blue) {
        ClientFxRules.wisp(x, y, z, size, red, green, blue);
        if (client(level)) ClientFxFacade.wisp(level, x, y, z, size, red, green, blue);
    }

    /**
     * The original {@code Thaumcraft.proxy.wispFX2(world, x, y, z, size, type, shrink, clip, gravity)}: one mote of a primal
     * colour that drifts with the given gravity.
     *
     * @param level   a client level; anything else draws nothing
     * @param size    the scale, above 0 and at most 16
     * @param type    the colour, 0 to 5: the six colours of the original wisps, 5 being the dark one that is drawn like smoke
     * @param shrink  {@code true} fades the mote out over its whole life; {@code false} grows it to full size at half its life
     *                and shrinks it again
     * @param noClip  {@code true} lets it pass through blocks (the original {@code clip} argument set {@code noClip})
     * @param gravity how fast it falls; negative rises
     */
    public static void wisp(Level level, double x, double y, double z, float size, int type, boolean shrink, boolean noClip, float gravity) {
        ClientFxRules.typedWisp(x, y, z, size, type, gravity);
        if (client(level)) ClientFxFacade.wisp(level, x, y, z, size, type, shrink, noClip, gravity);
    }

    /**
     * The original {@code Thaumcraft.proxy.wispFX3(world, x, y, z, tx, ty, tz, size, type, shrink, speed)}: one mote of a primal
     * colour that sets off from the first point towards the second at the speed that would cover the distance in its lifetime
     * (36 to 51 ticks). Like every wisp it loses 2% of its speed each tick, so it ends short of the second point, and it falls
     * with the gravity it is given. The parameter the original called the count is the colour {@code type}, and {@code speed}
     * is the gravity.
     *
     * @param level   a client level; anything else draws nothing
     * @param tx      the point it flies to, in blocks
     * @param ty      the point it flies to, in blocks
     * @param tz      the point it flies to, in blocks
     * @param size    the scale, above 0 and at most 16
     * @param type    the colour, 0 to 5, see the shorter form
     * @param shrink  see the shorter form
     * @param gravity how fast it falls on its way; negative rises
     */
    public static void wisp(Level level, double x, double y, double z, double tx, double ty, double tz, float size, int type,
                            boolean shrink, float gravity) {
        ClientFxRules.typedWisp(x, y, z, size, type, gravity);
        ClientFxRules.coordinate("tx", tx); ClientFxRules.coordinate("ty", ty); ClientFxRules.coordinate("tz", tz);
        if (client(level)) ClientFxFacade.wisp(level, x, y, z, tx, ty, tz, size, type, shrink, gravity);
    }

    /**
     * The original {@code Thaumcraft.proxy.blockRunes(world, x, y, z, red, green, blue, duration, gravity)}: one rune glyph that
     * fades in and out, as the warding stone and the thaumometer scan draw it.
     *
     * <p><b>The position is the corner of a block, not a point.</b> The original added half a block on every axis, so the rune
     * is placed relative to {@code (x + 0.5, y + 0.5, z + 0.5)}: pass the block's own coordinates and add the height you want
     * above the block to {@code y}: to put the rune just above the top face of the block at {@code (bx, by, bz)} pass
     * {@code (bx, by + 0.7, bz)}, which is {@code 1.2} above the bottom of the block. The glyph is a flat quad drawn upright
     * against a randomly chosen side of the block whose middle is that point, one of sixteen glyphs, with a small random offset
     * in its own plane, as the original did. It is not thinned by the particle setting: loop over {@link #particleCount} as the
     * original callers did.</p>
     *
     * @param level    a client level; anything else draws nothing
     * @param red      0 to 1; any finite value is accepted and one outside that range is clamped to it
     * @param green    0 to 1, clamped like {@code red}
     * @param blue     0 to 1, clamped like {@code red}
     * @param duration 1 to 6000; the rune lives three times this many ticks, fading in over the first fifth
     * @param gravity  the rune's vertical speed changes by {@code -0.04 * gravity} every tick: a negative gravity lifts it, as
     *                 the {@code -0.02} of Thaumcraft's own runes does
     */
    public static void blockRunes(Level level, double x, double y, double z, float red, float green, float blue, int duration, float gravity) {
        ClientFxRules.blockRunes(x, y, z, red, green, blue, duration, gravity);
        if (client(level)) ClientFxFacade.blockRunes(level, x, y, z, red, green, blue, duration, gravity);
    }

    /**
     * The original {@code Thaumcraft.proxy.beamCont(world, player, tx, ty, tz, type, color, reverse, endmod, input, impact)}:
     * a continuous beam of light, the one the focus of excavation draws, from one point to another. It is three crossed ribbons
     * that turn about the beam's axis, the texture sliding along them, ending in a glint when {@code impact} is above zero.
     *
     * <p><b>Call it every client tick while the beam should show.</b> A beam lasts 8 ticks from its first call, and each
     * further call for the same {@code owner} moves it to the new ends and makes it last at least 4 more ticks, which is what
     * the original did with the beam object it returned (and took back as {@code input}); there is no handle to keep, and a
     * beam is simply let go by not calling. One owner has one beam at a time: a second call for the same entity moves that
     * beam and changes its colour, so two beams from one player need two owners. The beam ends at once when {@code owner} is
     * removed or the start of the beam is more than 64 blocks from the local player, and is dropped when the level changes. At
     * most 32 beams are kept; the oldest goes first. It honours the particle setting: at "minimal" nothing is drawn.</p>
     *
     * <p><b>The impact counts down, and the call phase matters.</b> Thaumcraft steps every beam once at the end of each client
     * tick (the {@code CLIENT_POST} listener of Architectury, in the order of registration), and the step lowers
     * {@code impact} by one, as the update of the original particle did. A call that runs <em>before</em> that step in the same
     * tick, as the callers of the original did (an item's or entity's client tick, a block's tick, a network task, the start of
     * the client tick), is drawn with the value you passed minus one, so a 1 is gone before it is drawn. A call that runs
     * <em>after</em> it (a listener of the end of the client tick registered after Thaumcraft's, or a render callback) is drawn
     * with the full value: the glint is a step larger and fainter than the original's. Pick one phase and keep to it; the
     * example add-on calls it from the start of the client tick.</p>
     *
     * <p>The original drew the beam from the tip of the wand in the player's hand, which it worked out itself. This form takes
     * the start from the caller; the form that takes a {@link Player} works it out the way the original did, see
     * {@link #wandTip}.</p>
     *
     * @param level    a client level; anything else draws nothing
     * @param owner    the entity the beam belongs to (the player whose focus it is), in {@code level}
     * @param from     where the beam starts, in blocks
     * @param to       where it ends, in blocks
     * @param rgb      the colour, {@code 0xRRGGBB}
     * @param type     the texture, 0 to 3 ({@code beam}, {@code beam1}, {@code beam2}, {@code beam3}); the excavation focus
     *                 uses 2
     * @param reverse  whether the texture slides from the end to the start
     * @param endScale the width of the far end relative to the start, 0 to 8 (0 narrows the beam to a point), and, with
     *                 {@code impact}, the size of the glint; the excavation focus uses 2 on a hit and 0 otherwise
     * @param impact   0 draws no glint at the far end; above 0 draws one of the size {@code endScale / 2 / (6 - impact)}, which
     *                 grows with the value. The value counts down by one every client tick (see above), so pass 5 on every
     *                 call while the beam hits something, as Thaumcraft's own foci do, and 0 while it does not
     * @throws NullPointerException     when {@code owner}, {@code from} or {@code to} is {@code null}
     * @throws IllegalArgumentException for the ranges above (a texture outside 0 to 3, an {@code endScale} outside 0 to 8, an
     *                                  {@code impact} outside 0 to 5, a non-finite end), also when {@code level} is not a
     *                                  client level; checked before {@code owner} is looked at
     */
    public static void beam(Level level, Entity owner, Vec3 from, Vec3 to, int rgb, int type, boolean reverse, float endScale, int impact) {
        java.util.Objects.requireNonNull(from, "from");
        java.util.Objects.requireNonNull(to, "to");
        ClientFxRules.beam(from.x, from.y, from.z, to.x, to.y, to.z, type, endScale, impact);
        java.util.Objects.requireNonNull(owner, "owner");
        if (client(level)) ClientFxFacade.beam(level, owner, from, to, rgb, type, reverse, endScale, impact);
    }

    /**
     * The original {@code Thaumcraft.proxy.beamCont(world, player, tx, ty, tz, type, color, reverse, endmod, input, impact)}
     * as it was called: a beam from the tip of the wand of {@code owner} ({@link #wandTip}) to a point. Everything else is as in
     * {@link #beam(Level, Entity, Vec3, Vec3, int, int, boolean, float, int)}: call it every client tick, there is no handle,
     * and one player has one beam.
     *
     * @param level    a client level; anything else draws nothing
     * @param owner    the player whose wand it is, in {@code level}
     * @param to       where the beam ends, in blocks
     * @throws NullPointerException     when {@code owner} or {@code to} is {@code null}
     * @throws IllegalArgumentException for the ranges of the long form
     */
    public static void beam(Level level, Player owner, Vec3 to, int rgb, int type, boolean reverse, float endScale, int impact) {
        java.util.Objects.requireNonNull(owner, "owner");
        java.util.Objects.requireNonNull(to, "to");
        beam(level, owner, wandTip(owner), to, rgb, type, reverse, endScale, impact);
    }

    /**
     * Where the beam of a wand starts: the tip of the wand in the hand of a player, as the original {@code FXBeamWand} worked it
     * out and as the excavation focus of Thaumcraft sends it to the clients: the eye, moved {@code 0.066} against the cosine and
     * {@code 0.04} against the sine of the yaw (in radians), {@code 0.06} down, and {@code 0.3} along the look vector. It does not
     * look at the hand the wand is in or at the camera (the first-person model and the third-person one differ); it is the same
     * point for every viewer. Pure arithmetic: it works on any side.
     *
     * @throws NullPointerException when {@code player} is {@code null}
     */
    public static Vec3 wandTip(Player player) {
        java.util.Objects.requireNonNull(player, "player");
        return ClientFxRules.wandTip(player.getEyePosition(), player.getLookAngle(), player.getYRot());
    }

    /**
     * The original {@code Thaumcraft.proxy.smokeSpiral(world, x, y, z, radius, start, miny, color)}: <b>one</b> puff of smoke
     * that travels along a spiral, turning twice about the point while it sinks from the top to the bottom of a sphere of that
     * radius, and fades out. It lives 20 to 29 ticks. One call draws one puff, not a ring: the Sword of the Zephyr calls it 5
     * times and the awakening eldritch warden 33 times, each time with a random {@code start} angle, and the puffs together make
     * the ring. Loop, and give each call its own {@code start}.
     *
     * @param level  a client level; anything else draws nothing
     * @param x      the centre of the sphere, in blocks
     * @param y      the centre of the sphere, in blocks
     * @param z      the centre of the sphere, in blocks
     * @param radius the radius in blocks, 0 to 64
     * @param start  the angle in degrees at which the turn starts (the originals pass a random 0 to 359)
     * @param minY   the lowest y, in blocks, the smoke may sink to (a floor): it never draws below {@code minY + 0.1}
     * @param rgb    the colour, {@code 0xRRGGBB}
     */
    public static void smokeSpiral(Level level, double x, double y, double z, float radius, int start, int minY, int rgb) {
        ClientFxRules.smokeSpiral(x, y, z, radius);
        if (client(level)) ClientFxFacade.smokeSpiral(level, x, y, z, radius, start, minY, rgb);
    }

    /**
     * The original {@code Thaumcraft.proxy.essentiaTrailFx(world, x, y, z, tx, ty, tz, count, color, scale)}: one glowing mote of
     * essentia that leaves the middle of one block, wobbles on its way to the middle of another and dies when it arrives.
     * The original was called once or twice a tick to make a stream; a single call makes a single mote. (To show a running
     * transfer between two blocks, ask the server with {@link thaumcraft.api.ThaumcraftFX#essentiaTrail}.)
     *
     * @param level a client level; anything else draws nothing
     * @param from  the block the mote starts in
     * @param to    the block it flies to; farther than 64 blocks from {@code from} nothing is drawn
     * @param phase the original {@code count}: the wobble of the mote follows {@code sin(phase / 2)}, {@code sin(phase / 3)} and
     *              {@code sin(phase / 4)}, so pass a running counter (the age of the block entity) to make a stream curl
     * @param rgb   the colour, {@code 0xRRGGBB}, usually {@code aspect.getColor()}
     * @param scale the size, above 0 and at most 16 (the originals used 0.5 to 1)
     * @throws NullPointerException when {@code from} or {@code to} is {@code null}
     */
    public static void essentiaTrail(Level level, BlockPos from, BlockPos to, int phase, int rgb, float scale) {
        java.util.Objects.requireNonNull(from, "from");
        java.util.Objects.requireNonNull(to, "to");
        ClientFxRules.essentiaTrail(scale);
        if (client(level)) ClientFxFacade.essentiaTrail(level, from, to, phase, rgb, scale);
    }
}
