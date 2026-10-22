package thaumcraft.golems;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import org.junit.jupiter.api.Test;
import thaumcraft.client.renderers.entity.GolemBobberSprite;

/**
 * The sprite of the golem bobber.
 *
 * <p>TC4 4.2.3.5, oracle {@code thaumcraft/client/renderers/entity/RenderGolemBobber.java:31,40-45}:
 * the quad reads the cell <b>(1,2)</b> of the <b>vanilla 1.7.10</b>
 * {@code textures/particle/particles.png}. 1.21.1 has no such sheet, so the port
 * ships the cell itself as an 8x8 asset. The test pins the sheet fractions of
 * the original and the exact pixels of the cell, which were read out of the
 * official Mojang 1.7.10 {@code client.jar}
 * ({@code assets/minecraft/textures/particle/particles.png}, the columns 8..15
 * and the rows 16..23).</p>
 *
 * <p>No block, item or entity is constructed here.</p>
 */
class GolemBobberSpriteTest {
    /** The 64 pixels of the cell (1,2), row by row, as 0xRRGGBBAA. */
    private static final int[] EXPECTED = {
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0xD3D3D3FF, 0xD3D3D3FF, 0x919191FF, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0xD32A2AFF, 0xD32A2AFF, 0x891414FF, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0xD3D3D3FF, 0x919191FF, 0x919191FF, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x000000FF, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x000000FF, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x000000FF, 0x00000000, 0x000000FF, 0x00000000, 0x00000000, 0x00000000,
            0x00000000, 0x00000000, 0x00000000, 0x000000FF, 0x000000FF, 0x00000000, 0x00000000, 0x00000000
    };

    @Test void theSheetFractionsAreTheOnesOfTheOriginal() {
        assertEquals(128, GolemBobberSprite.SHEET, "the vanilla 1.7.10 particle sheet is 128x128");
        assertEquals(8, GolemBobberSprite.CELL, "one cell of the sheet is eight pixels");
        assertEquals(1, GolemBobberSprite.CELL_X, "the original's b0 is one");
        assertEquals(2, GolemBobberSprite.CELL_Y, "the original's b1 is two");
        assertEquals(0.0625F, GolemBobberSprite.U0, 1e-6F, "the original's f2 is (b0*8+0)/128");
        assertEquals(0.125F, GolemBobberSprite.U1, 1e-6F, "the original's f3 is (b0*8+8)/128");
        assertEquals(0.125F, GolemBobberSprite.V0, 1e-6F, "the original's f4 is (b1*8+0)/128");
        assertEquals(0.1875F, GolemBobberSprite.V1, 1e-6F, "the original's f5 is (b1*8+8)/128");
        assertEquals(8, GolemBobberSprite.PIXEL_X, "the first column of the cell in the sheet");
        assertEquals(16, GolemBobberSprite.PIXEL_Y, "the first row of the cell in the sheet");
    }

    @Test void theCellCornersAreTheCornersOfTheAsset() {
        assertEquals(0F, GolemBobberSprite.cellU(GolemBobberSprite.U0), 1e-6F);
        assertEquals(1F, GolemBobberSprite.cellU(GolemBobberSprite.U1), 1e-6F);
        assertEquals(0F, GolemBobberSprite.cellV(GolemBobberSprite.V0), 1e-6F);
        assertEquals(1F, GolemBobberSprite.cellV(GolemBobberSprite.V1), 1e-6F);
    }

    @Test void theAssetIsTheCellOfTheVanillaSheet() throws Exception {
        byte[] png;
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("assets/thaumcraft/textures/entity/golem_bobber.png")) {
            assertNotNull(stream, "the bobber sprite asset is missing");
            png = stream.readAllBytes();
        }
        int[] pixels = decode(png);
        assertEquals(8, pixels[0], "the asset is eight pixels wide");
        assertEquals(8, pixels[1], "the asset is eight pixels high");
        int[] actual = new int[64];
        System.arraycopy(pixels, 2, actual, 0, 64);
        assertArrayEquals(EXPECTED, actual, "the asset must be the cell (1,2) of the vanilla 1.7.10 sheet");
    }

    /** A minimal reader of an 8 bit RGBA PNG: the header, the inflated rows and the unfiltered pixels. */
    private static int[] decode(byte[] png) throws IOException {
        int width = 0, height = 0;
        byte[] raw = null;
        try (DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(png))) {
            byte[] signature = new byte[8];
            in.readFully(signature);
            assertArrayEquals(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, signature, "the PNG signature");
            while (true) {
                int length = in.readInt();
                byte[] type = new byte[4];
                in.readFully(type);
                byte[] data = new byte[length];
                in.readFully(data);
                in.readInt(); // the CRC of the chunk
                if (type[0] == 'I' && type[1] == 'H' && type[2] == 'D' && type[3] == 'R') {
                    width = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
                    height = ((data[4] & 0xFF) << 24) | ((data[5] & 0xFF) << 16) | ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
                    assertEquals(8, data[8] & 0xFF, "the bit depth");
                    assertEquals(6, data[9] & 0xFF, "the colour type is RGBA");
                } else if (type[0] == 'I' && type[1] == 'D' && type[2] == 'A' && type[3] == 'T') {
                    raw = inflate(data);
                } else if (type[0] == 'I' && type[1] == 'E' && type[2] == 'N' && type[3] == 'D') {
                    break;
                }
            }
        }
        assertNotNull(raw, "the PNG has no image data");
        assertEquals(8, width, "the asset is eight pixels wide");
        assertEquals(8, height, "the asset is eight pixels high");
        int stride = width * 4;
        byte[] previous = new byte[stride];
        byte[] pixels = new byte[width * height * 4];
        int source = 0, target = 0;
        for (int y = 0; y < height; y++) {
            int filter = raw[source++] & 0xFF;
            byte[] line = new byte[stride];
            System.arraycopy(raw, source, line, 0, stride);
            source += stride;
            for (int i = 0; i < stride; i++) {
                int left = i >= 4 ? line[i - 4] & 0xFF : 0;
                int up = previous[i] & 0xFF;
                int upLeft = i >= 4 ? previous[i - 4] & 0xFF : 0;
                int value = line[i] & 0xFF;
                switch (filter) {
                    case 0 -> { }
                    case 1 -> value += left;
                    case 2 -> value += up;
                    case 3 -> value += (left + up) / 2;
                    case 4 -> {
                        int estimate = left + up - upLeft;
                        int dl = Math.abs(estimate - left), du = Math.abs(estimate - up), dul = Math.abs(estimate - upLeft);
                        value += dl <= du && dl <= dul ? left : du <= dul ? up : upLeft;
                    }
                    default -> throw new IOException("unknown PNG filter " + filter);
                }
                line[i] = (byte) value;
            }
            System.arraycopy(line, 0, pixels, target, stride);
            target += stride;
            previous = line;
        }
        int[] out = new int[2 + width * height];
        out[0] = width;
        out[1] = height;
        for (int i = 0; i < width * height; i++) {
            out[2 + i] = ((pixels[i * 4] & 0xFF) << 24) | ((pixels[i * 4 + 1] & 0xFF) << 16)
                    | ((pixels[i * 4 + 2] & 0xFF) << 8) | (pixels[i * 4 + 3] & 0xFF);
        }
        return out;
    }

    private static byte[] inflate(byte[] data) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        try {
            while (!inflater.finished()) {
                int read = inflater.inflate(buffer);
                if (read == 0) break;
                out.write(buffer, 0, read);
            }
        } catch (java.util.zip.DataFormatException failure) {
            throw new IOException("the PNG image data is not deflated", failure);
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }
}
