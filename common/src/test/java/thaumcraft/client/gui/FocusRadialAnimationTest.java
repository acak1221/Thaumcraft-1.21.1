package thaumcraft.client.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocusRadialAnimationTest {
    @Test void originalFiveMillisecondGateIsStrictAndDoesNotCatchUp() {
        var animation = new FocusRadialAnimation();
        assertEquals(0F, animation.scale());
        assertTrue(animation.advance(100, true));
        assertEquals(.05F, animation.scale());
        assertFalse(animation.advance(105, true));
        assertTrue(animation.advance(106, true));
        assertEquals(.10F, animation.scale());
        assertTrue(animation.advance(10000, true));
        assertEquals(.15F, animation.scale()); // One frame, not 1978 steps.
    }

    @Test void originalOpenAndCloseStepIsPointZeroFiveAndClamps() {
        var animation = new FocusRadialAnimation();
        for (int i = 1; i <= 20; i++) animation.advance(i * 6, true);
        assertEquals(1F, animation.scale());
        animation.advance(126, true);
        assertEquals(1F, animation.scale());
        animation.advance(132, false);
        assertEquals(.95F, animation.scale());
        for (int i = 23; i <= 45; i++) animation.advance(i * 6, false);
        assertEquals(0F, animation.scale());
    }

    @Test void originalHoverStepsArePointZeroTwoFiveNotAnInstantJump() {
        assertEquals(1.025F, FocusRadialAnimation.hover(1F, true));
        assertEquals(1.275F, FocusRadialAnimation.hover(1.3F, false));
        assertEquals(1F, FocusRadialAnimation.hover(1F, false));
        assertEquals(1.3F, FocusRadialAnimation.hover(1.3F, true));
        float scale = 1F;
        for (int i = 0; i < 13; i++) scale = FocusRadialAnimation.hover(scale, true);
        assertEquals(1.3249997F, scale); // Preserve the oracle's un-clamped float overshoot.
    }

    @Test void originalRadiusAndOpeningSpiralUseAllChoices() {
        assertEquals(18.5F, FocusRadialAnimation.radius(1));
        assertEquals(48.5F, FocusRadialAnimation.radius(13));
        assertEquals(0F, FocusRadialAnimation.angle(0, 4, 0));
        assertEquals(-45F, FocusRadialAnimation.angle(0, 4, .5F));
        assertEquals(-90F, FocusRadialAnimation.angle(0, 4, 1));
        assertEquals(180F, FocusRadialAnimation.angle(3, 4, 1));
    }
}
