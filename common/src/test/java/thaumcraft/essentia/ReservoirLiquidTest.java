package thaumcraft.essentia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.essentia.ReservoirRules;
import thaumcraft.common.tiles.ReservoirColour;

/**
 * The tank of the essentia reservoir and the colour of what it shows (original TileEssentiaReservoirRenderer:61-90 and the client
 * branch of TileEssentiaReservoir.update).
 */
class ReservoirLiquidTest {
    @Test void theTankIsTheInnerThreeToThirteenSixteenthsOfTheBlock() {
        assertEquals(3 / 16F, ReservoirRules.LIQUID_EDGE);
        assertEquals(13 / 16F, ReservoirRules.LIQUID_FAR);
        assertEquals(10 / 16F, ReservoirRules.LIQUID_HEIGHT);
    }

    @Test void theTankGrowsWithTheStoredUnits() {
        assertEquals(3 / 16F, ReservoirRules.liquidTop(0), "an empty reservoir shows nothing");
        assertEquals(3 / 16F + 10 / 16F / 256, ReservoirRules.liquidTop(1), "one unit is a two hundred fifty sixth of the tank");
        assertEquals(3 / 16F + 10 / 16F / 4, ReservoirRules.liquidTop(64), "a quarter full is a quarter of the tank");
        assertEquals(3 / 16F + 10 / 16F / 2, ReservoirRules.liquidTop(128), "half full is half of the tank");
        assertEquals(3 / 16F + 10 / 16F, ReservoirRules.liquidTop(256), "full fills the whole tank");
        for (int units = 1; units <= 256; units++) {
            assertTrue(ReservoirRules.liquidTop(units) >= ReservoirRules.liquidTop(units - 1), "monotone at " + units);
        }
    }

    @Test void nothingIsShownBeforeTheTwentiethTick() {
        ReservoirColour colour = new ReservoirColour();
        List<Aspect> stored = List.of(Aspect.FIRE);
        for (int count = 1; count < 20; count++) colour.update(stored, count);
        assertNull(colour.displayAspect(), "the original picks the shown aspect on the twentieth tick");
        assertEquals(1F, colour.red());
        assertEquals(1F, colour.green());
        assertEquals(1F, colour.blue());
    }

    @Test void theShownAspectCyclesThroughTheStock() {
        ReservoirColour colour = new ReservoirColour();
        List<Aspect> stored = List.of(Aspect.FIRE, Aspect.WATER);
        colour.update(stored, 20);
        assertEquals(Aspect.WATER, colour.displayAspect(), "the original indexes the stock with count / 20");
        colour.update(stored, 40);
        assertEquals(Aspect.FIRE, colour.displayAspect());
        colour.update(stored, 60);
        assertEquals(Aspect.WATER, colour.displayAspect());
    }

    @Test void theColourWalksToTheAspectColourInTwentyTicks() {
        ReservoirColour colour = new ReservoirColour();
        List<Aspect> stored = List.of(Aspect.FIRE);
        float red = 0xFF / 255F, green = 0x5A / 255F, blue = 0x01 / 255F;
        colour.update(stored, 20);
        assertEquals(Aspect.FIRE, colour.displayAspect());
        // The original moves the colour on the very tick the aspect is picked.
        assertEquals(1F - (1F - red) / 20F, colour.red(), 1.0E-6F, "fire is 0xff5a01");
        assertEquals(1F - (1F - green) / 20F, colour.green(), 1.0E-6F);
        assertEquals(1F - (1F - blue) / 20F, colour.blue(), 1.0E-6F);
        for (int count = 21; count <= 39; count++) colour.update(stored, count);
        assertEquals(red, colour.red(), 1.0E-6F, "after twenty ticks the colour is the aspect's colour");
        assertEquals(green, colour.green(), 1.0E-6F);
        assertEquals(blue, colour.blue(), 1.0E-6F);
    }
}
