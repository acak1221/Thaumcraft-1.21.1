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
    /** The 1.7.10 decomposition, written out the way {@code OpenGlHelper} did it. */
    private static int oracleBlock(int brightness) { return brightness % 65536 / 16; }
    private static int oracleSky(int brightness) { return brightness / 65536 / 16; }

    @Test void theBrightnessOfTheOriginalSplitsIntoBlockLightAndNoSkyLight() {
        // The brightness of the original only ever occupies the Block half: 190..210 out of 65536.
        for (int ticks = 0; ticks < 40; ticks++) {
            for (int jitter = 0; jitter < 2; jitter++) {
                int brightness = ThaumometerReadout.brightness(ticks, jitter);
                int packed = ThaumometerReadout.lightmap(brightness);
                assertEquals(oracleBlock(brightness), LightTexture.block(packed),
                        "block light of brightness " + brightness + " at " + ticks + "/" + jitter);
                assertEquals(oracleSky(brightness), LightTexture.sky(packed),
                        "sky light of brightness " + brightness + " at " + ticks + "/" + jitter);
            }
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
        for (int ticks = 0; ticks < 25; ticks++) {
            int expected = (int) (190.0F + (float) Math.sin(ticks) * 10.0F + 10.0F);
            assertEquals(expected, ThaumometerReadout.brightness(ticks, 0));
        }
    }
}
