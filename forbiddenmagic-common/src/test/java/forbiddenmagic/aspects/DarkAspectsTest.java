package forbiddenmagic.aspects;

import forbiddenmagic.common.aspects.DarkAspects;
import forbiddenmagic.common.config.ForbiddenMagicConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the six/seven sin aspects against the decompiled original DarkAspects.initAspects()
 * (fox.spiteful.forbidden.DarkAspects, Forbidden Magic 0.575 oracle jar) field for field. */
class DarkAspectsTest {
    @BeforeAll
    static void init() {
        ForbiddenMagicConfig.noLust = false;
        DarkAspects.initAspects();
    }

    @Test void netherMatchesOriginal() {
        assertEquals("infernus", DarkAspects.NETHER.getTag());
        assertEquals(16711680, DarkAspects.NETHER.getColor());
        assertArrayEquals(new Aspect[]{Aspect.FIRE, Aspect.MAGIC}, DarkAspects.NETHER.getComponents());
    }

    @Test void wrathMatchesOriginal() {
        assertEquals("ira", DarkAspects.WRATH.getTag());
        assertEquals(8848388, DarkAspects.WRATH.getColor());
        assertArrayEquals(new Aspect[]{Aspect.WEAPON, Aspect.FIRE}, DarkAspects.WRATH.getComponents());
    }

    @Test void envyMatchesOriginal() {
        assertEquals("invidia", DarkAspects.ENVY.getTag());
        assertEquals(47616, DarkAspects.ENVY.getColor());
        assertArrayEquals(new Aspect[]{Aspect.SENSES, Aspect.HUNGER}, DarkAspects.ENVY.getComponents());
    }

    @Test void gluttonyMatchesOriginal() {
        assertEquals("gula", DarkAspects.GLUTTONY.getTag());
        assertEquals(13999174, DarkAspects.GLUTTONY.getColor());
        assertArrayEquals(new Aspect[]{Aspect.HUNGER, Aspect.VOID}, DarkAspects.GLUTTONY.getComponents());
    }

    @Test void prideMatchesOriginal() {
        assertEquals("superbia", DarkAspects.PRIDE.getTag());
        assertEquals(9845247, DarkAspects.PRIDE.getColor());
        assertArrayEquals(new Aspect[]{Aspect.FLIGHT, Aspect.VOID}, DarkAspects.PRIDE.getComponents());
    }

    @Test void slothMatchesOriginal() {
        assertEquals("desidia", DarkAspects.SLOTH.getTag());
        assertEquals(7237230, DarkAspects.SLOTH.getColor());
        assertArrayEquals(new Aspect[]{Aspect.TRAP, Aspect.SOUL}, DarkAspects.SLOTH.getComponents());
    }

    @Test void lustRegisteredWhenNotConfiguredOff() {
        assertNotNull(DarkAspects.LUST);
        assertEquals("luxuria", DarkAspects.LUST.getTag());
        assertEquals(16761294, DarkAspects.LUST.getColor());
        assertArrayEquals(new Aspect[]{Aspect.FLESH, Aspect.HUNGER}, DarkAspects.LUST.getComponents());
    }

    @Test void greedAndTaintReuseBaseAspectsRatherThanAddingNewOnes() {
        // Original DarkAspects never declares a custom "greed"/"taint" Aspect field; the shards use
        // the base TC4 aspects directly (Aspect.GREED = lucrum, Aspect.TAINT = vitium).
        assertEquals("lucrum", Aspect.GREED.getTag());
        assertEquals("vitium", Aspect.TAINT.getTag());
    }
}
