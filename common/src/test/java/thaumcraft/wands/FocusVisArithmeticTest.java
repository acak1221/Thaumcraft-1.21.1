package thaumcraft.wands;

import org.junit.jupiter.api.Test;
import thaumcraft.common.items.wands.WandVisDiscount;
import thaumcraft.common.items.wands.FocusVis;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FocusVisArithmeticTest {
    @Test void castingKeepsSeparateFloatSubtractionsWhileRecipesIgnoreFrugal() {
        // ItemWandCasting:223-259, evaluated with IEEE single-precision rounding offline.
        float[] caps = {1.1F, 1.1F, 1F, .9F, .9F, .9F, 1F};
        int[] gear = {-30, 5, 10, 10, 0, 80, 100};
        int[] frugal = {0, 2, 3, 3, 5, 5, 0};
        int[] casting = {0x3fb33334,0x3f59999b,0x3f199999,0x3efffffe,0x3ecccccc,0x3dcccccd,0x3dcccccd};
        int[] recipes = {0x3fb33334,0x3f866667,0x3f666666,0x3f4ccccc,0x3f666666,0x3dcccccd,0x3dcccccd};
        for (int i = 0; i < caps.length; i++) {
            assertEquals(casting[i], Float.floatToIntBits(WandVisDiscount.focusModifier(caps[i], gear[i], frugal[i])));
            assertEquals(recipes[i], Float.floatToIntBits(WandVisDiscount.modifier(caps[i], gear[i])));
        }
    }

    /**
     * The centivis arithmetic of a cast: the modifier is applied to the raw centivis cost and the
     * result is truncated, never rounded; individual aspect prices may reach zero.
     *
     * <p>Oracle {@code WandManager:104-120}: the raw cost of an aspect is multiplied by the
     * modifier of the wand and the payer, and the amount actually debited is the truncation of that
     * product. This test calls the actual casting price API with its default cap. The registered
     * Frugal-wand price/debit cases run in {@code AccessoriesRuntimeChecks.focusCentivis}, where
     * native components and items exist; no registry mutation is introduced into JUnit.</p>
     */
    @Test void centivisTruncationZeroAspectsAndClampAreNotRoundedOrFreeRecipes() {
        // A .9 cap with five Frugal is .9F - .5F, which in Java float arithmetic is 0.39999998,
        // NOT 0.4. That is the original's behaviour and the reason the truncation matters:
        // 5 centivis at that modifier cost 1, not 2 (checked on a native wand in GameTest).
        float frugalFive = WandVisDiscount.focusModifier(.9F, 0, 5);
        assertEquals(0.39999998F, frugalFive, 1e-9F, "the float subtraction is the original's, not an exact .4");
        assertEquals(Map.of("aer", 5, "terra", 2, "ignis", 15, "aqua", 1000),
                FocusVis.price(new AspectList().add(Aspect.AIR, 5).add(Aspect.EARTH, 2)
                        .add(Aspect.FIRE, 15).add(Aspect.WATER, 1000), ItemStack.EMPTY, null));
        assertThrows(IllegalArgumentException.class, () -> FocusVis.price(new AspectList(), ItemStack.EMPTY, null));
        assertThrows(IllegalArgumentException.class, () -> FocusVis.price(new AspectList().add(Aspect.MAGIC, 5), ItemStack.EMPTY, null));
        assertFalse(FocusVis.debit(ItemStack.EMPTY, Map.of("aer", 1)));
        // A full cap with one Frugal is .9 exactly, and 15 centivis cost 13: 13.5 truncated.
        assertEquals(0.9F, WandVisDiscount.focusModifier(1F, 0, 1), 1e-9F);
        // The clamp is a floor of .1F and it is reached, not merely approached.
        assertEquals(.1F, WandVisDiscount.focusModifier(.9F, 500, 5));
        // A gear discount of 90 on a .9 cap leaves .9F - .9F = 0 in the RAW formula; the original
        // clamps it to .1F, so 1000 centivis still cost 100 - the cast is never free.
        assertEquals(.1F, WandVisDiscount.modifier(.9F, 90));
        // The original rejects a negative gear discount, an out-of-range Frugal and a NaN cap.
        assertThrows(IllegalArgumentException.class, () -> WandVisDiscount.focusModifier(.9F, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> WandVisDiscount.focusModifier(.9F, 0, 6));
        assertThrows(IllegalArgumentException.class, () -> WandVisDiscount.focusModifier(Float.NaN, 0, 0));
    }
}
