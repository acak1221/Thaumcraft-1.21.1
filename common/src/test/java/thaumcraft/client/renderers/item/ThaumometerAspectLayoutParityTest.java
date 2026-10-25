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
