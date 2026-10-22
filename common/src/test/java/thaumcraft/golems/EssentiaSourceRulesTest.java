package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.essentia.EssentiaSourceRules;

/**
 * The source set of the Alchemy golem.
 *
 * <p>Oracle {@code thaumcraft/common/entities/ai/fluid/AIEssentiaGather.java:56-124}: the home cell
 * of the golem may be <b>any</b> essentia transport — a jar, a reservoir or any other transport that
 * can output to the home facing and holds the aspect. These tests pin the numbers of that rule.
 * No tile, no block and no world are built here: only the pure rule class is exercised.</p>
 */
class EssentiaSourceRulesTest {
    @Test
    void thePaymentSideIsTheOriginals() {
        // AIEssentiaGather:94-99 - the alembic and the jar pay upwards, the reservoir pays on its
        // own facing and every other transport pays on the home facing of the golem.
        assertEquals(Direction.UP, EssentiaSourceRules.paymentFace(EssentiaSourceRules.Kind.JAR, Direction.NORTH, Direction.EAST));
        assertEquals(Direction.EAST, EssentiaSourceRules.paymentFace(EssentiaSourceRules.Kind.RESERVOIR, Direction.NORTH, Direction.EAST));
        assertEquals(Direction.NORTH, EssentiaSourceRules.paymentFace(EssentiaSourceRules.Kind.OTHER, Direction.NORTH, Direction.EAST));
        assertEquals(Direction.UP, EssentiaSourceRules.paymentFace(EssentiaSourceRules.Kind.JAR, Direction.DOWN, Direction.DOWN));
    }

    @Test
    void theJarAndTheReservoirNeedNoSideTest() {
        // AIEssentiaGather:60 - (te instanceof TileJarFillable || te instanceof TileEssentiaReservoir
        // || etrans.canOutputTo(facing)).
        assertTrue(EssentiaSourceRules.eligible(EssentiaSourceRules.Kind.JAR, false, null, Aspect.FIRE, null, 10, 0, 64));
        assertTrue(EssentiaSourceRules.eligible(EssentiaSourceRules.Kind.RESERVOIR, false, null, Aspect.FIRE, null, 10, 0, 64));
        // Every other transport must be able to output to the home facing.
        assertFalse(EssentiaSourceRules.eligible(EssentiaSourceRules.Kind.OTHER, false, null, Aspect.FIRE, null, 10, 0, 64));
        assertTrue(EssentiaSourceRules.eligible(EssentiaSourceRules.Kind.OTHER, true, null, Aspect.FIRE, null, 10, 0, 64));
    }

    @Test
    void theTransportMustHoldEssentiaAndTheCargoMustHaveRoom() {
        // AIEssentiaGather:60 - getEssentiaAmount(facing) > 0 and essentiaAmount < getCarryLimit().
        assertFalse(EssentiaSourceRules.eligible(EssentiaSourceRules.Kind.OTHER, true, null, Aspect.FIRE, null, 0, 0, 64));
        assertFalse(EssentiaSourceRules.eligible(EssentiaSourceRules.Kind.OTHER, true, null, Aspect.FIRE, null, 10, 64, 64));
        assertTrue(EssentiaSourceRules.eligible(EssentiaSourceRules.Kind.OTHER, true, null, Aspect.FIRE, null, 10, 63, 64));
    }

    @Test
    void aLoadedCargoTakesOnlyTheAspectOfTheTransport() {
        // AIEssentiaGather:60 - the cargo aspect must equal the type of the side OR the unsided type.
        assertTrue(EssentiaSourceRules.aspectMatches(null, Aspect.FIRE, Aspect.WATER));
        assertTrue(EssentiaSourceRules.aspectMatches(Aspect.FIRE, Aspect.FIRE, Aspect.WATER));
        assertTrue(EssentiaSourceRules.aspectMatches(Aspect.WATER, Aspect.FIRE, Aspect.WATER));
        assertFalse(EssentiaSourceRules.aspectMatches(Aspect.WATER, Aspect.FIRE, Aspect.FIRE));
        assertTrue(EssentiaSourceRules.eligible(EssentiaSourceRules.Kind.OTHER, true, Aspect.WATER, Aspect.FIRE, Aspect.WATER, 10, 4, 64));
        assertFalse(EssentiaSourceRules.eligible(EssentiaSourceRules.Kind.OTHER, true, Aspect.WATER, Aspect.FIRE, Aspect.FIRE, 10, 4, 64));
    }

    @Test
    void theTakenAspectIsTheSideAndThenTheUnsidedQuery() {
        // AIEssentiaGather:104-107 - the type of the side, the unsided type when the side names none.
        assertEquals(Aspect.FIRE, EssentiaSourceRules.takenAspect(Aspect.FIRE, Aspect.WATER));
        assertEquals(Aspect.WATER, EssentiaSourceRules.takenAspect(null, Aspect.WATER));
        assertEquals(null, EssentiaSourceRules.takenAspect(null, null));
    }

    @Test
    void theTakenAmountNeverExceedsTheRoomOfTheCargo() {
        // AIEssentiaGather:108-112 - min(stock, getCarryLimit() - essentiaAmount).
        assertEquals(10, EssentiaSourceRules.takenAmount(10, 0, 64));
        assertEquals(4, EssentiaSourceRules.takenAmount(10, 60, 64));
        assertEquals(0, EssentiaSourceRules.takenAmount(10, 64, 64));
    }
}
