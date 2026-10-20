package thaumcraft.common.lib.crystals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.util.Mth;

/** Exact original TileCrystalRenderer random schedule; rendering never draws from the world's gameplay RNG. */
public final class CrystalClusterGeometry {
    private static final int[] COLORS = {0xffff7e, 0xff3c01, 0x0090ff, 0x00a000, 0xeeccff, 0x555577};
    public record Piece(int color, float yaw, float pitch, float scaleX, float scaleY, float scaleZ, int light) {}
    public record Attachment(float x, float y, float z, char axis, int angle) {}
    private CrystalClusterGeometry() {}
    public static int color(int metadata) { return COLORS[metadata]; }
    public static Attachment attachment(short orientation) {
        return switch (orientation) {
            case 0 -> new Attachment(.5F, 1.3F, .5F, 'x', 180);
            case 1 -> new Attachment(.5F, -.3F, .5F, 'x', 0);
            case 2 -> new Attachment(.5F, .5F, 1.3F, 'x', -90);
            case 3 -> new Attachment(.5F, .5F, -.3F, 'x', 90);
            case 4 -> new Attachment(1.3F, .5F, .5F, 'z', 90);
            case 5 -> new Attachment(-.3F, .5F, .5F, 'z', -90);
            default -> new Attachment(0, 0, 0, 'x', 0); // Original invalid SHORT values are not silently normalized.
        };
    }
    public static List<Piece> pieces(int metadata, int x, int y, int z, int viewerTicks) {
        if (metadata < 0 || metadata > 6) throw new IllegalArgumentException("Original crystal metadata must be0..6");
        Random random = new Random(metadata + x + y * z); // Original int overflow occurs before conversion to the long seed.
        var result = new ArrayList<Piece>(6);
        int color = COLORS[metadata == 6 ? 4 : metadata];
        result.add(piece(random, color, (random.nextFloat() - random.nextFloat()) * 5,
                (random.nextFloat() - random.nextFloat()) * 5, 1.1F, viewerTicks));
        for (int index = 1; index < 6; index++) {
            if (metadata == 6) color = COLORS[index == 5 ? 5 : index - 1];
            result.add(piece(random, color, random.nextInt(36) + 72 * index, 15 + random.nextInt(15), .8F, viewerTicks));
        }
        return List.copyOf(result);
    }
    private static Piece piece(Random random, int color, float yaw, float pitch, float size, int ticks) {
        float shade = Mth.sin((ticks + random.nextInt(10)) / (5.0F + random.nextFloat())) * .075F + .925F;
        return new Piece(color, yaw, pitch, (.15F + random.nextFloat() * .075F) * size,
                (.5F + random.nextFloat() * .1F) * size, (.15F + random.nextFloat() * .05F) * size, (int) (210 * shade));
    }
}
