package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class BoneBowGlintParityTest {
    @Test void originalTintIsNotVanillaPurple() {
        assertEquals(.38F, BoneBowGlintRules.RED);
        assertEquals(.19F, BoneBowGlintRules.GREEN);
        assertEquals(.608F, BoneBowGlintRules.BLUE);
    }
    @Test void bothOriginalPeriodsWrapAndScrollInOppositeDirections() {
        assertArrayEquals(new float[]{0, 0}, BoneBowGlintRules.uv(0, 0, 3000, 0), 1e-6F);
        assertArrayEquals(new float[]{0, 0}, BoneBowGlintRules.uv(0, 0, 4873, 1), 1e-6F);
        assertArrayEquals(new float[]{.5F, 0}, BoneBowGlintRules.uv(0, 0, 1500, 0), 1e-6F);
        assertArrayEquals(new float[]{-2436F/4873F, 0}, BoneBowGlintRules.uv(0, 0, 2436, 1), 1e-6F);
    }
    @Test void originalTextureMatrixScalesAfterRotatingAndTranslating() {
        assertArrayEquals(new float[]{.08034845F, -.095755555F}, BoneBowGlintRules.uv(1, 0, 0, 0), 1e-6F);
        assertArrayEquals(new float[]{.12310097F, .021706022F}, BoneBowGlintRules.uv(1, 0, 0, 1), 1e-6F);
    }
}
