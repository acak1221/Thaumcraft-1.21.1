package thaumcraft.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import thaumcraft.api.client.ThaumcraftClientFX;
import thaumcraft.common.lib.fx.ClientFxRules;

/**
 * The client effect facade ({@code ThaumcraftClientFX}) without a client: what it promises about its arguments, about a level
 * that is not a client level, and about being loadable where no client class exists.
 *
 * <p>What it draws is not tested here: a particle needs a running client. These are the rules around the drawing.</p>
 */
class ClientFxApiTest {
    private static final double OK = 10.5D;

    // ---- a level that is no client level: nothing happens, nothing fails ----

    @Test void aMissingLevelDrawsNothingAndFailsNothing() {
        ThaumcraftClientFX.sparkle(null, OK, OK, OK, 1.0F, 2, 0.0F);
        ThaumcraftClientFX.sparkle(null, OK, OK, OK, 3);
        ThaumcraftClientFX.burst(null, OK, OK, OK, 1.0F);
        ThaumcraftClientFX.wisp(null, OK, OK, OK, 0.1F, 0.2F, 0.3F, 0.4F);
        ThaumcraftClientFX.wisp(null, OK, OK, OK, 0.1F, 4, true, true, -0.02F);
        ThaumcraftClientFX.wisp(null, OK, OK, OK, 0.0, 70.0, 0.0, 0.1F, 4, true, 0.0F);
        ThaumcraftClientFX.blockRunes(null, OK, OK, OK, 0.5F, 0.0F, 0.5F, 20, -0.02F);
        ThaumcraftClientFX.smokeSpiral(null, OK, OK, OK, 2.0F, 90, 60, 0x9E2FF);
        ThaumcraftClientFX.essentiaTrail(null, BlockPos.ZERO, new BlockPos(3, 4, 5), 12, 0xFFAA00, 0.7F);
        assertEquals(0, ThaumcraftClientFX.particleCount(null, 2), "no particles without a client level");
        assertEquals(0, ThaumcraftClientFX.particleCount(null, 0));
    }

    // ---- arguments are checked first, on every side ----

    @Test void aMistakeIsRefusedEvenWhereNothingWouldBeDrawn() {
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.sparkle(null, Double.NaN, OK, OK, 1.0F, 2, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.sparkle(null, OK, Double.POSITIVE_INFINITY, OK, 1.0F, 2, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.sparkle(null, OK, OK, 30_000_033.0D, 1.0F, 2, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.sparkle(null, OK, OK, OK, 0.0F, 2, 0.0F), "size zero");
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.sparkle(null, OK, OK, OK, -1.0F, 2, 0.0F), "negative size");
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.sparkle(null, OK, OK, OK, Float.NaN, 2, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.sparkle(null, OK, OK, OK, 16.5F, 2, 0.0F), "larger than the limit");
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.sparkle(null, OK, OK, OK, 1.0F, 8, 0.0F), "type 8 does not exist");
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.sparkle(null, OK, OK, OK, 1.0F, -1, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.sparkle(null, OK, OK, OK, 1.0F, 2, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.sparkle(null, OK, OK, OK, 8), "the type-only form checks its type");
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.burst(null, OK, OK, OK, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.wisp(null, OK, OK, OK, 0.1F, Float.NaN, 0.0F, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.wisp(null, OK, OK, OK, 0.1F, 6, true, true, 0.0F), "primal colours are 0 to 5");
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.wisp(null, OK, OK, OK, OK, Double.NaN, OK, 0.1F, 3, true, 0.0F), "the target is checked too");
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.wisp(null, OK, OK, OK, OK, OK, OK, 0.1F, -1, true, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.blockRunes(null, OK, OK, OK, 1.0F, 1.0F, 1.0F, 0, 0.0F), "a rune lives at least one tick");
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.blockRunes(null, OK, OK, OK, 1.0F, 1.0F, 1.0F, 6001, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.blockRunes(null, OK, OK, OK, Float.NaN, 1.0F, 1.0F, 20, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.smokeSpiral(null, OK, OK, OK, -1.0F, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.smokeSpiral(null, OK, OK, OK, 65.0F, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.essentiaTrail(null, BlockPos.ZERO, BlockPos.ZERO, 0, 0, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.particleCount(null, -1));
    }

    @Test void theBlockPositionsOfAnEssentiaTrailMustBeInsideTheWorld() {
        // A BlockPos is three ints and can name a place far outside the world; the facade says it checks every position.
        assertRefused("from.x", () -> ThaumcraftClientFX.essentiaTrail(null, new BlockPos(30_000_033, 0, 0), BlockPos.ZERO, 0, 0, 1.0F));
        assertRefused("from.y", () -> ThaumcraftClientFX.essentiaTrail(null, new BlockPos(0, -100_000_000, 0), BlockPos.ZERO, 0, 0, 1.0F));
        assertRefused("from.z", () -> ThaumcraftClientFX.essentiaTrail(null, new BlockPos(0, 0, Integer.MAX_VALUE), BlockPos.ZERO, 0, 0, 1.0F));
        assertRefused("to.x", () -> ThaumcraftClientFX.essentiaTrail(null, BlockPos.ZERO, new BlockPos(-30_000_033, 0, 0), 0, 0, 1.0F));
        assertRefused("to.y", () -> ThaumcraftClientFX.essentiaTrail(null, BlockPos.ZERO, new BlockPos(0, 100_000_000, 0), 0, 0, 1.0F));
        assertRefused("to.z", () -> ThaumcraftClientFX.essentiaTrail(null, BlockPos.ZERO, new BlockPos(0, 0, Integer.MIN_VALUE), 0, 0, 1.0F));
        // The limit itself passes, and the scale is still checked.
        ThaumcraftClientFX.essentiaTrail(null, new BlockPos(30_000_032, -64, -30_000_032), new BlockPos(-30_000_032, 320, 30_000_032), 0, 0, 16.0F);
        assertRefused("scale", () -> ThaumcraftClientFX.essentiaTrail(null, BlockPos.ZERO, BlockPos.ZERO, 0, 0, 16.5F));
    }

    @Test void theBeamNeedsItsOwnerAndItsEnds() {
        assertThrows(NullPointerException.class, () -> ThaumcraftClientFX.beam(null, (net.minecraft.world.entity.Entity) null, Vec3.ZERO, Vec3.ZERO, 0xFF0000, 2, true, 0.0F, 1), "owner");
        assertThrows(NullPointerException.class, () -> ThaumcraftClientFX.beam(null, (net.minecraft.world.entity.player.Player) null, Vec3.ZERO, 0xFF0000, 2, true, 0.0F, 1), "player");
        assertThrows(NullPointerException.class, () -> ThaumcraftClientFX.beam(null, (net.minecraft.world.entity.Entity) null, null, Vec3.ZERO, 0xFF0000, 2, true, 0.0F, 1), "from");
        assertThrows(NullPointerException.class, () -> ThaumcraftClientFX.beam(null, (net.minecraft.world.entity.Entity) null, Vec3.ZERO, null, 0xFF0000, 2, true, 0.0F, 1), "to");
        assertThrows(NullPointerException.class, () -> ThaumcraftClientFX.wandTip(null));
        assertThrows(NullPointerException.class, () -> ThaumcraftClientFX.essentiaTrail(null, null, BlockPos.ZERO, 0, 0, 1.0F), "from");
        assertThrows(NullPointerException.class, () -> ThaumcraftClientFX.essentiaTrail(null, BlockPos.ZERO, null, 0, 0, 1.0F), "to");
    }

    /**
     * The ranges of the beam are checked by the public method itself, before the owner is looked at, so they can be asserted
     * without an entity: the owner is {@code null} here, and a call that passes the ranges then fails on the owner instead.
     */
    @Test void theBeamRangesAreCheckedByThePublicMethod() {
        Vec3 from = new Vec3(0, 64, 0), to = new Vec3(3, 64, 4);
        net.minecraft.world.entity.Entity nobody = null;
        // texture 0 to 3 (beam to beam3); a 4 would index past the textures of the renderer every frame
        assertRefused("type", () -> ThaumcraftClientFX.beam(null, nobody, from, to, 0xFFFFFF, 4, false, 1.0F, 0));
        assertRefused("type", () -> ThaumcraftClientFX.beam(null, nobody, from, to, 0xFFFFFF, -1, false, 1.0F, 0));
        assertRefused("endScale", () -> ThaumcraftClientFX.beam(null, nobody, from, to, 0xFFFFFF, 2, false, -0.1F, 0));
        assertRefused("endScale", () -> ThaumcraftClientFX.beam(null, nobody, from, to, 0xFFFFFF, 2, false, 8.5F, 0));
        assertRefused("endScale", () -> ThaumcraftClientFX.beam(null, nobody, from, to, 0xFFFFFF, 2, false, Float.NaN, 0));
        assertRefused("impact", () -> ThaumcraftClientFX.beam(null, nobody, from, to, 0xFFFFFF, 2, false, 1.0F, 6));
        assertRefused("impact", () -> ThaumcraftClientFX.beam(null, nobody, from, to, 0xFFFFFF, 2, false, 1.0F, -1));
        assertRefused("to.z", () -> ThaumcraftClientFX.beam(null, nobody, from, new Vec3(0, 64, Double.NaN), 0xFFFFFF, 2, false, 1.0F, 0));
        assertRefused("from.y", () -> ThaumcraftClientFX.beam(null, nobody, new Vec3(0, Double.POSITIVE_INFINITY, 0), to, 0xFFFFFF, 2, false, 1.0F, 0));
        assertRefused("from.x", () -> ThaumcraftClientFX.beam(null, nobody, new Vec3(30_000_033.0D, 64, 0), to, 0xFFFFFF, 2, false, 1.0F, 0));
        // The limits themselves pass the ranges (and then meet the missing owner).
        assertThrows(NullPointerException.class, () -> ThaumcraftClientFX.beam(null, nobody, from, to, 0xFFFFFF, 0, false, 0.0F, 0), "lowest limits");
        assertThrows(NullPointerException.class, () -> ThaumcraftClientFX.beam(null, nobody, from, to, 0xFFFFFF, 3, true, 8.0F, 5), "highest limits");
        // The rules the method applies, called directly: the same answers.
        assertThrows(IllegalArgumentException.class, () -> ClientFxRules.beam(0, 0, 0, 1, 1, 1, 4, 1.0F, 0), "texture 4");
        ClientFxRules.beam(0, 64, 0, 3, 64, 4, 3, 8.0F, 5);
    }

    private static void assertRefused(String what, org.junit.jupiter.api.function.Executable call) {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, call, what);
        assertTrue(refused.getMessage().contains(what), "the message names " + what + ": " + refused.getMessage());
    }

    @Test void aColourComponentOutsideZeroToOneIsClampedAndOneInsideIsKept() {
        assertEquals(0.0F, ClientFxRules.unit(-0.5F), 0.0F);
        assertEquals(0.0F, ClientFxRules.unit(Float.NEGATIVE_INFINITY), 0.0F);
        assertEquals(0.0F, ClientFxRules.unit(0.0F), 0.0F);
        assertEquals(0.25F, ClientFxRules.unit(0.25F), 0.0F);
        assertEquals(1.0F, ClientFxRules.unit(1.0F), 0.0F);
        assertEquals(1.0F, ClientFxRules.unit(2.0F), 0.0F);
        assertEquals(1.0F, ClientFxRules.unit(Float.POSITIVE_INFINITY), 0.0F);
        // The facade refuses NaN and infinity before the clamp is reached, and accepts any other finite component.
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.wisp(null, OK, OK, OK, 0.1F, Float.POSITIVE_INFINITY, 0.0F, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> ThaumcraftClientFX.blockRunes(null, OK, OK, OK, 0.5F, Float.NaN, 0.5F, 20, 0.0F));
        ThaumcraftClientFX.wisp(null, OK, OK, OK, 0.1F, -3.0F, 7.0F, 0.5F);
        ThaumcraftClientFX.blockRunes(null, OK, OK, OK, -3.0F, 7.0F, 0.5F, 20, 0.0F);
    }

    @Test void theLimitsAreInclusive() {
        ThaumcraftClientFX.sparkle(null, 30_000_032.0D, -30_000_032.0D, 0, 16.0F, 7, -100.0F);
        ThaumcraftClientFX.sparkle(null, 0, 0, 0, 0.001F, 0, 100.0F);
        ThaumcraftClientFX.blockRunes(null, 0, 0, 0, 0, 0, 0, 1, 0);
        ThaumcraftClientFX.blockRunes(null, 0, 0, 0, 2, 2, 2, 6000, 0);
        ThaumcraftClientFX.smokeSpiral(null, 0, 0, 0, 0.0F, -720, -64, 0xFFFFFFFF);
        ThaumcraftClientFX.smokeSpiral(null, 0, 0, 0, 64.0F, 720, 320, -1);
        ThaumcraftClientFX.wisp(null, 0, 0, 0, 16.0F, 5, false, false, 0.0F);
        ThaumcraftClientFX.wisp(null, 0, 0, 0, 0, 0, 0, 0.5F, 0, false, 0.0F);
    }

    @Test void theOriginalRulesOfThePrimitivesAreTheDocumentedOnes() {
        assertEquals(8, ClientFxRules.SPARKLE_TYPES);
        assertEquals(6, ClientFxRules.WISP_TYPES);
        assertEquals(4, ClientFxRules.BEAM_TYPES);
        assertEquals(6, ClientFxRules.SPARKLE_ROLL, "the proxy sparkle rolls one in six");
        assertEquals(6, ClientFxRules.SPARKLE_MULTIPLIER, "and lives 3 * 6 ticks");
        // The type-only sparkle forwards these two named constants to the long form (the forwarding itself needs a client level
        // to observe; the constants it forwards are the audited legacy source's size 1.5 and no gravity).
        assertEquals(1.5F, ClientFxRules.TYPE_ONLY_SPARKLE_SIZE);
        assertEquals(0.0F, ClientFxRules.TYPE_ONLY_SPARKLE_GRAVITY, 0.0F);
        assertEquals(0xABCDEF, ClientFxRules.rgb(0xFFABCDEF), "only the low 24 bits of a colour count");
        assertEquals(0xFFFFFF, ClientFxRules.rgb(-1));
        // Oracle ClientProxy.java:917-922: the rune is placed half a block from the point on every axis.
        assertEquals(10.5D, ClientFxRules.runeCentre(10.0D));
        assertEquals(-0.5D, ClientFxRules.runeCentre(-1.0D));
    }

    @Test void theProxySparkleIsDrawnWhenTheRollIsBelowTheParticleCount() {
        // Oracle ClientProxy.java:648-655: if (rand.nextInt(6) < particleCount(2)).
        for (int roll = 0; roll < ClientFxRules.SPARKLE_ROLL; roll++) {
            assertFalse(ClientFxRules.sparkleDraws(roll, 0), "minimal particles: never");
            assertEquals(roll < 2, ClientFxRules.sparkleDraws(roll, 2), "decreased: two rolls of six");
            assertEquals(roll < 4, ClientFxRules.sparkleDraws(roll, 4), "all: four rolls of six");
        }
    }

    @Test void theParticleCountFollowsTheThreeSettings() {
        // Oracle ClientProxy.java:899-905; the settings are ParticleStatus ALL (0), DECREASED (1), MINIMAL (2).
        assertEquals(8, ClientFxRules.particleCount(0, 4));
        assertEquals(4, ClientFxRules.particleCount(1, 4));
        assertEquals(0, ClientFxRules.particleCount(2, 4));
        assertEquals(0, ClientFxRules.particleCount(0, 0));
        // The doubling at "all" saturates instead of wrapping to a negative count: the facade documents "zero or more".
        assertEquals(Integer.MAX_VALUE, ClientFxRules.particleCount(0, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE - 1, ClientFxRules.particleCount(0, Integer.MAX_VALUE / 2), "the largest base that doubles exactly");
        assertEquals(Integer.MAX_VALUE, ClientFxRules.particleCount(0, Integer.MAX_VALUE / 2 + 1));
        assertEquals(Integer.MAX_VALUE, ClientFxRules.particleCount(1, Integer.MAX_VALUE), "decreased is the base itself");
        assertEquals(0, ClientFxRules.particleCount(2, Integer.MAX_VALUE));
        assertEquals(0, ThaumcraftClientFX.particleCount(null, Integer.MAX_VALUE), "a server level has no particles whatever the base");
        assertEquals(net.minecraft.client.ParticleStatus.ALL.getId(), 0);
        assertEquals(net.minecraft.client.ParticleStatus.DECREASED.getId(), 1);
        assertEquals(net.minecraft.client.ParticleStatus.MINIMAL.getId(), 2);
    }

    @Test void theBeamOfAWandStartsAtTheTipOfTheWand() {
        // Original FXBeamWand: the eye, 0.066 against the cosine and 0.04 against the sine of the yaw, 0.06 down, 0.3 along the look.
        Vec3 eye = new Vec3(10.0, 65.62, -4.0), look = new Vec3(0.0, 0.0, 1.0);
        Vec3 south = ClientFxRules.wandTip(eye, look, 0.0F);
        assertEquals(10.0 - 0.066, south.x, 1.0E-9);
        assertEquals(65.62 - 0.06, south.y, 1.0E-9);
        assertEquals(-4.0 + 0.3, south.z, 1.0E-9);
        Vec3 west = ClientFxRules.wandTip(eye, new Vec3(-1.0, 0.0, 0.0), 90.0F);
        assertEquals(10.0 - 0.3, west.x, 1.0E-6);
        assertEquals(65.62 - 0.06, west.y, 1.0E-9);
        assertEquals(-4.0 - 0.04, west.z, 1.0E-6);
        Vec3 up = ClientFxRules.wandTip(eye, new Vec3(0.0, 1.0, 0.0), 180.0F);
        assertEquals(10.0 + 0.066, up.x, 1.0E-6);
        assertEquals(65.62 - 0.06 + 0.3, up.y, 1.0E-9);
        assertEquals(-4.0, up.z, 1.0E-6);
    }

    @Test void anEssentiaTrailIsDrawnOnlyWithinSixtyFourBlocks() {
        assertTrue(ClientFxRules.trailInRange(64.0D * 64.0D));
        assertFalse(ClientFxRules.trailInRange(64.0D * 64.0D + 1.0D));
        assertTrue(ClientFxRules.trailInRange(0.0D));
    }

    @Test void thePublicSurfaceIsTheSevenEffectsAndTheParticleCount() {
        // A new public method of the facade is a decision to document, not an accident.
        List<String> names = new ArrayList<>();
        for (Method method : ThaumcraftClientFX.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) names.add(method.getName());
        }
        assertEquals(List.of("beam", "blockRunes", "burst", "essentiaTrail", "particleCount", "smokeSpiral", "sparkle", "wandTip", "wisp").stream().sorted().toList(),
                names.stream().distinct().sorted().toList());
        assertEquals(2, names.stream().filter("sparkle"::equals).count(), "sparkle has the long form and the type-only form");
        assertEquals(2, names.stream().filter("beam"::equals).count(), "beam has the form with a start and the form from the wand of a player");
        assertEquals(3, names.stream().filter("wisp"::equals).count(), "wisp has the three forms of wispFX, wispFX2 and wispFX3");
    }

    // ---- loadable where no client class exists ----

    /**
     * A dedicated server has the common classes and no {@code net.minecraft.client}. This loads the facade through a class loader
     * that refuses every client class, which is what linking it on a server would meet, and calls it with no client level.
     */
    @Test void theFacadeLoadsAndIgnoresAServerLevelWithoutAnyClientClass() throws Exception {
        List<String> refused = new ArrayList<>();
        ClassLoader parent = ClientFxApiTest.class.getClassLoader();
        ClassLoader serverLike = new ClassLoader(parent) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.client.") || name.startsWith("thaumcraft.client.") || name.startsWith("com.mojang.blaze3d.")) {
                    refused.add(name);
                    throw new ClassNotFoundException(name + " is a client class");
                }
                if (name.equals("thaumcraft.api.client.ThaumcraftClientFX") || name.startsWith("thaumcraft.common.lib.fx.")) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> loaded = findLoadedClass(name);
                        if (loaded == null) {
                            try (InputStream in = parent.getResourceAsStream(name.replace('.', '/') + ".class")) {
                                if (in == null) throw new ClassNotFoundException(name);
                                byte[] bytes = in.readAllBytes();
                                loaded = defineClass(name, bytes, 0, bytes.length);
                            } catch (IOException e) {
                                throw new ClassNotFoundException(name, e);
                            }
                        }
                        if (resolve) resolveClass(loaded);
                        return loaded;
                    }
                }
                return super.loadClass(name, resolve);
            }
        };
        assertThrows(ClassNotFoundException.class, () -> serverLike.loadClass("net.minecraft.client.Minecraft"), "the loader really refuses client classes");
        refused.clear();
        Class<?> fx = serverLike.loadClass("thaumcraft.api.client.ThaumcraftClientFX");
        assertTrue(fx.getClassLoader() == serverLike, "the facade was defined by the restricted loader, not borrowed from the parent");
        Method sparkle = fx.getMethod("sparkle", Level.class, double.class, double.class, double.class, float.class, int.class, float.class);
        Method count = fx.getMethod("particleCount", Level.class, int.class);
        Method burst = fx.getMethod("burst", Level.class, double.class, double.class, double.class, float.class);
        sparkle.invoke(null, null, OK, OK, OK, 1.0F, 2, 0.0F);
        burst.invoke(null, null, OK, OK, OK, 1.0F);
        assertEquals(0, count.invoke(null, null, 2));
        InvocationTargetException refusedCall = assertThrows(InvocationTargetException.class, () -> sparkle.invoke(null, null, Double.NaN, OK, OK, 1.0F, 2, 0.0F));
        assertTrue(refusedCall.getCause() instanceof IllegalArgumentException, "the argument rules run on a server too: " + refusedCall.getCause());
        assertTrue(refused.isEmpty(), "no client class was needed to load or call the facade: " + refused);
    }
}
