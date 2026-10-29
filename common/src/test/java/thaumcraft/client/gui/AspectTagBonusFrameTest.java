package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The bonus sparkle of the aspect tag: a sixteen frame strip of the particle sheet, one frame per tick of the local player
 * (original {@code UtilsFX.drawTag}: {@code px = 16 * (ticksExisted & 15)}, row 80).
 */
class AspectTagBonusFrameTest {
    @Test void theSparkleWalksTheStripOneFramePerTick() {
        assertEquals(0, ThaumcraftRecipeDisplay.bonusSheetU(0));
        assertEquals(16, ThaumcraftRecipeDisplay.bonusSheetU(1));
        assertEquals(240, ThaumcraftRecipeDisplay.bonusSheetU(15), "the last frame ends at 256, the width of the sheet");
        assertEquals(0, ThaumcraftRecipeDisplay.bonusSheetU(16), "and it starts over");
        assertEquals(32, ThaumcraftRecipeDisplay.bonusSheetU(18));
        assertEquals(240, ThaumcraftRecipeDisplay.bonusSheetU(-1), "a negative tick never reads outside the sheet");
        assertEquals(80, ThaumcraftRecipeDisplay.BONUS_SHEET_V, "the row of the original");
    }

    @Test void everyFrameStaysInsideTheSheet() {
        for (int tick = -64; tick < 4096; tick++) {
            int u = ThaumcraftRecipeDisplay.bonusSheetU(tick);
            assertEquals(true, u >= 0 && u + 16 <= 256, "tick " + tick + " -> " + u);
        }
    }
}
