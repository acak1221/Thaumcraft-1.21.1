package thaumcraft.client.renderers.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.renderer.LightTexture;
import org.junit.jupiter.api.Test;

/**
 * The lightmap of the thaumometer screen and readout.
 *
 * <p>The original hands the brightness value of
 * {@code 190 + sin(ticks - random(2)) * 10 + 10} to {@code Tessellator.setBrightness}
 * (oracle {@code UtilsFX:766-786}, MCP {@code func_78380_c}). The 1.7.10 fixed-function lightmap
 * carries the <b>Block</b> light in the low sixteen bits and the <b>Sky</b> light in the high
 * sixteen, and the layer samples both in 1/16 units, so the value must be split by 65536 and then
 * by 16 - not by 16 and then by 16, which is the mistake this test pins down.</p>
 *
 * <p>No block, item or client is constructed here: the conversion is a pure static function.</p>
 */
class ThaumometerLightmapParityTest {
    @Test void theBrightnessOfTheOriginalSplitsIntoBlockLightAndNoSkyLight() {
        // The brightness of the original only ever occupies the Block half: 190..210 out of 65536.
        int[][] fixtures = {{190,11,0,176}, {191,11,0,176}, {192,12,0,192},
                {200,12,0,192}, {207,12,0,192}, {208,13,0,208}, {210,13,0,208},
                {15728832,12,15,15728832}};
        for (int[] fixture : fixtures) {
            int packed = ThaumometerReadout.lightmap(fixture[0]);
            assertEquals(fixture[1], LightTexture.block(packed));
            assertEquals(fixture[2], LightTexture.sky(packed));
            assertEquals(fixture[3], packed);
        }
    }

    @Test void theScreenIsBrightAndNeverFlickersBetweenTwoComponents() {
        // The previous revision packed (b % 16, (b / 16) % 16), which moved 14/11 -> 8/12 -> 2/13
        // as the sine drifted. The original only ever moves the Block half, around 11..13.
        for (int ticks = 0; ticks < 200; ticks++) {
            int packed = ThaumometerReadout.packedLight(ticks, 0);
            int block = LightTexture.block(packed);
            assertEquals(0, LightTexture.sky(packed), "no sky light at " + ticks);
            // 190..210 in 1/16 units of the 0..15 light scale.
            assertTrue(block >= 11 && block <= 13, "block light " + block + " at " + ticks);
        }
    }

    @Test void theBrightnessIsTheOriginalSine() {
        // Oracle ItemThaumometerRenderer:157 - 190 + sin(ticks - random(2)) * 10 + 10.
        int[] expected = {200,208,209,201,192,190,197,206,209,204,194,190,194,204,209,206,197,190,192,201,209,208,199,191,190};
        for (int ticks = 0; ticks < expected.length; ticks++) {
            assertEquals(expected[ticks], ThaumometerReadout.brightness(ticks, 0));
            assertEquals(expected[ticks], ThaumometerReadout.brightness(ticks + 1, 1));
        }
        assertEquals(191, ThaumometerReadout.brightness(0, 1));
    }
}
