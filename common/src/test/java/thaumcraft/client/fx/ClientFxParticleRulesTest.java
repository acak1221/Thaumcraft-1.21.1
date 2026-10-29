package thaumcraft.client.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import thaumcraft.client.ThaumcraftExcavationClient;
import thaumcraft.client.fx.particles.PrimalWispParticle;
import thaumcraft.client.fx.particles.TypeSparkleParticle;
import thaumcraft.common.lib.fx.ClientFxRules;

/**
 * The rules of the particles that the client effect facade makes, as pure functions: the size of a wisp over its life, the
 * sparkle types the facade accepts against the types the sparkle knows, and the lifetime and slide of the continuous beam.
 */
class ClientFxParticleRulesTest {
    // ---- wisp: the shrink flag of the original FXWisp ----

    @Test void aShrinkingWispFadesOutOverItsWholeLife() {
        assertEquals(1.0F, PrimalWispParticle.sizeFactor(true, 0, 40), 1.0E-6F);
        assertEquals(0.5F, PrimalWispParticle.sizeFactor(true, 20, 40), 1.0E-6F);
        assertEquals(0.025F, PrimalWispParticle.sizeFactor(true, 39, 40), 1.0E-6F);
        assertEquals(0.0F, PrimalWispParticle.sizeFactor(true, 40, 40), 1.0E-6F);
    }

    @Test void aWispThatDoesNotShrinkGrowsToFullSizeAtHalfItsLifeAndShrinksAgain() {
        // Original FXWisp render: age / (maxAge / 2), folded at one.
        assertEquals(0.0F, PrimalWispParticle.sizeFactor(false, 0, 40), 1.0E-6F);
        assertEquals(0.5F, PrimalWispParticle.sizeFactor(false, 10, 40), 1.0E-6F);
        assertEquals(1.0F, PrimalWispParticle.sizeFactor(false, 20, 40), 1.0E-6F);
        assertEquals(0.5F, PrimalWispParticle.sizeFactor(false, 30, 40), 1.0E-6F);
        assertEquals(0.0F, PrimalWispParticle.sizeFactor(false, 40, 40), 1.0E-6F);
        // The half life is an integer division, as in the original: a lifetime of 37 has a half life of 18.
        assertEquals(1.0F, PrimalWispParticle.sizeFactor(false, 18, 37), 1.0E-6F);
        assertEquals(1.0F - 1.0F / 18.0F, PrimalWispParticle.sizeFactor(false, 19, 37), 1.0E-6F);
    }

    @Test void aLifetimeOfOneDoesNotDivideByZero() {
        assertEquals(0.0F, PrimalWispParticle.sizeFactor(false, 0, 1), 1.0E-6F);
        assertTrue(Float.isFinite(PrimalWispParticle.sizeFactor(false, 1, 1)));
    }

    // ---- sparkle: the types the facade accepts are the types the sparkle has a colour for ----

    @Test void everySparkleTypeOfTheFacadeHasItsOwnColourAndNoOtherDoes() {
        RandomSource random = RandomSource.create(7L);
        for (int type = 0; type < ClientFxRules.SPARKLE_TYPES; type++) {
            float[] colour = TypeSparkleParticle.colour(type, random);
            for (float component : colour) assertTrue(component >= 0.0F && component <= 1.0F, "type " + type + " component " + component);
            assertTrue(colour[0] != 1.0F || colour[1] != 1.0F || colour[2] != 1.0F || type == 6, "type " + type + " is not the default white");
        }
        float[] beyond = TypeSparkleParticle.colour(ClientFxRules.SPARKLE_TYPES, random);
        assertEquals(1.0F, beyond[0]);
        assertEquals(1.0F, beyond[1]);
        assertEquals(1.0F, beyond[2]);
    }

    @Test void theSparkleIsDrawnWithTheProxyRollAndTheParticleCountOfTheSetting() {
        assertEquals(6, ClientFxRules.SPARKLE_ROLL);
        // The facade (ClientFxFacade.sparkle) and TypeSparkleParticle.particleCount() both ask this very function, with this
        // base, for the id of the particle setting of the game: base two, doubled at "all" (oracle ClientProxy.java:899-905).
        assertEquals(2, ClientFxRules.SPARKLE_COUNT_BASE);
        for (net.minecraft.client.ParticleStatus status : net.minecraft.client.ParticleStatus.values()) {
            int expected = switch (status) {
                case ALL -> 4;
                case DECREASED -> 2;
                case MINIMAL -> 0;
            };
            assertEquals(expected, ClientFxRules.particleCount(status.getId(), ClientFxRules.SPARKLE_COUNT_BASE), status.name());
        }
    }

    // ---- the continuous beam ----

    @Test void aBeamLastsEightTicksAndEveryRefreshKeepsItForAtLeastFour() {
        assertEquals(8, ThaumcraftExcavationClient.BEAM_LIFETIME);
        assertEquals(4, ThaumcraftExcavationClient.BEAM_REFRESH);
        assertEquals(8, ThaumcraftExcavationClient.refreshedLifetime(8), "a fresh beam keeps its eight");
        assertEquals(5, ThaumcraftExcavationClient.refreshedLifetime(5));
        assertEquals(4, ThaumcraftExcavationClient.refreshedLifetime(4));
        assertEquals(4, ThaumcraftExcavationClient.refreshedLifetime(2), "a beam about to end is given four more");
        assertEquals(4, ThaumcraftExcavationClient.refreshedLifetime(0));
    }

    @Test void theReverseFlagSlidesTheTextureTheOtherWay() {
        for (int slide = 0; slide < 40; slide++) {
            for (float partial : new float[]{0.0F, 0.25F, 0.99F}) {
                float forward = ThaumcraftExcavationClient.scroll(slide, partial, false);
                assertEquals(ThaumcraftExcavationClient.scroll(slide, partial), forward, 0.0F, "the old form is the forward slide");
                float value = slide + partial;
                // The slide is -value * 0.2 minus the floor of -value * 0.1; reversed, the age changes its sign.
                assertEquals(value * 0.2F - (float) Math.floor(value * 0.1F), ThaumcraftExcavationClient.scroll(slide, partial, true), 1.0E-5F);
            }
        }
        assertEquals(0.0F, ThaumcraftExcavationClient.scroll(0, 0.0F, true), 0.0F);
        // After the same age the two directions are not the same place of the texture.
        assertTrue(ThaumcraftExcavationClient.scroll(7, 0.5F, true) != ThaumcraftExcavationClient.scroll(7, 0.5F, false));
    }
}
