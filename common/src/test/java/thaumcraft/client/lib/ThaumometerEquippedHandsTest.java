package thaumcraft.client.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Real native stack changes exercise the same per-hand tracker invoked by the client tick. */
class ThaumometerEquippedHandsTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void changingMainHandCannotMoveAnUnchangedOffhandInstrument() {
        var hands = new UtilsFX.EquippedHands();
        ItemStack main = new ItemStack(Items.STICK), instrument = new ItemStack(Items.CLOCK);
        settle(hands, main, instrument, 0);
        for (float expected : new float[]{.6F, .2F, 0F, .4F, .8F, 1F}) {
            hands.tick(ItemStack.EMPTY, instrument, 1);
            assertEquals(expected, hands.progress(1, InteractionHand.MAIN_HAND), 1e-6F, "main hand retains the original curve");
            assertEquals(1F, hands.progress(.5F, InteractionHand.OFF_HAND), "stable offhand remains raised throughout interpolation");
        }
    }

    @Test void changingOffhandCannotMoveAnUnchangedMainHandInstrument() {
        var hands = new UtilsFX.EquippedHands();
        ItemStack instrument = new ItemStack(Items.CLOCK), off = new ItemStack(Items.STICK);
        settle(hands, instrument, off, 0);
        for (float expected : new float[]{.6F, .2F, 0F, .4F, .8F, 1F}) {
            hands.tick(instrument, ItemStack.EMPTY, 0);
            assertEquals(expected, hands.progress(1, InteractionHand.OFF_HAND), 1e-6F, "offhand uses the same original curve");
            assertEquals(1F, hands.progress(.5F, InteractionHand.MAIN_HAND), "main hand ignores an unrelated offhand swap");
        }
    }

    @Test void interpolationResetAndSameItemReplacementPreserveOriginalRules() {
        var hands = new UtilsFX.EquippedHands();
        ItemStack main = new ItemStack(Items.STICK), off = new ItemStack(Items.CLOCK);
        settle(hands, main, off, 0);
        hands.tick(main.copy(), off.copy(), 2);
        assertEquals(1F, hands.progress(.5F, InteractionHand.MAIN_HAND), "same type and damage retains the original held-item shortcut");
        assertEquals(1F, hands.progress(.5F, InteractionHand.OFF_HAND));
        hands.tick(ItemStack.EMPTY, ItemStack.EMPTY, 3);
        assertEquals(.8F, hands.progress(.5F, InteractionHand.MAIN_HAND), 1e-6F);
        assertEquals(.8F, hands.progress(.5F, InteractionHand.OFF_HAND), 1e-6F);
        hands.reset();
        assertEquals(0F, hands.progress(.5F, InteractionHand.MAIN_HAND));
        assertEquals(0F, hands.progress(.5F, InteractionHand.OFF_HAND));
    }

    private static void settle(UtilsFX.EquippedHands hands, ItemStack main, ItemStack off, int slot) {
        for (int tick = 0; tick < 4; tick++) hands.tick(main, off, slot);
        assertEquals(1F, hands.progress(1, InteractionHand.MAIN_HAND));
        assertEquals(1F, hands.progress(1, InteractionHand.OFF_HAND));
    }
}
