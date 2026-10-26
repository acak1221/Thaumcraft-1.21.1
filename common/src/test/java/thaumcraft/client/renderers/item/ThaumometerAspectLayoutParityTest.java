package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class ThaumometerAspectLayoutParityTest {
    @Test void firstRowAndSingleAspectUseTheOriginalOffsets() {
        assertArrayEquals(new int[][]{{-8, -8}}, positions(1));
        assertArrayEquals(new int[][]{{-40, -8}, {-24, -8}, {-8, -8}, {8, -8}, {24, -8}}, positions(5));
    }

    @Test void secondAndThirdRowsUseTheIncrementedRowCapacity() {
        // Direct original JAR bytecode1490-1529, rather than CFR's reordered combined expression.
        assertArrayEquals(new int[][]{{-40,-8},{-24,-8},{-8,-8},{8,-8},{24,-8},
                {-32,8},{-16,8},{0,8},{16,8},{-24,24}}, positions(10));
        assertArrayEquals(new int[][]{{-40,-8},{-24,-8},{-8,-8},{8,-8},{24,-8},
                {-32,8},{-16,8},{0,8},{16,8},{-24,24},{-8,24},{8,24}}, positions(12));
    }

    @Test void theOriginalRemainingCounterQuirkIsPreserved() {
        // The original subtracts the *new* row capacity, even when only one icon remains.
        // A mathematical recentering to x=-8 would differ from TC4.
        assertArrayEquals(new int[][]{{-40,-8},{-24,-8},{-8,-8},{8,-8},{24,-8},{-16,8}}, positions(6));
        assertArrayEquals(new int[][]{{-40,-8},{-24,-8},{-8,-8},{8,-8},{24,-8},
                {-32,8},{-16,8},{0,8},{16,8},{-24,24},{-8,24},{8,24},
                {-16,40},{0,40},{-8,56}}, positions(15));
    }

    @Test void eachAmountSitsAtTheLowerRightOfItsOwnGlyph() {
        // Original UtilsFX.drawTag (JAR bytecode, drawTag(double, double, ...) offsets 432-468):
        // drawString(amount, 32 - width + x * 2, 32 - FONT_HEIGHT + y * 2) at half size.
        assertArrayEquals(new int[]{-5, 7}, new int[]{ThaumometerReadout.amountX(5, -16), ThaumometerReadout.amountY(9, -8)});
        assertArrayEquals(new int[]{-37, 7}, new int[]{ThaumometerReadout.amountX(5, -32), ThaumometerReadout.amountY(9, -8)},
                "a second glyph one column to the left moves its amount with it");
        assertArrayEquals(new int[]{2, 39}, new int[]{ThaumometerReadout.amountX(14, -8), ThaumometerReadout.amountY(9, 8)},
                "a wider amount stays right-aligned, a lower row moves down");
    }

    private static int[][] positions(int count) {
        var layout = new ThaumometerReadout.AspectLayout(count);
        var result = new int[count][2];
        for (int i = 0; i < count; i++) {
            result[i] = new int[]{layout.x(), layout.y()};
            layout.next();
        }
        return result;
    }
}
