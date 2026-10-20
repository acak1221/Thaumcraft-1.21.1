package thaumcraft.client.renderers.block;

import net.minecraft.core.Direction;

/**
 * The rune pattern of the warded glass.
 *
 * <p>Original {@code BlockCosmeticOpaque} (oracle
 * {@code thaumcraft/common/blocks/BlockCosmeticOpaque.java}) draws the warded
 * glass with one of forty seven textures. The texture of a face is chosen by
 * the eight warded glass blocks that touch it: the eight neighbour flags form
 * a bit mask and the original lookup table {@code UtilsFX.connectedTextureRefByID}
 * turns that mask into the index of the rune texture.
 *
 * <p>The class is a pure geometry helper, so the mask and the lookup table can
 * be checked in a unit test.
 */
public final class WardedGlassBakedModel {
    /** The original number of warded glass textures. */
    public static final int TEXTURE_COUNT = 47;
    /**
     * The original lookup table of the connected textures, copied from
     * {@code thaumcraft/client/lib/UtilsFX.java} of the oracle.
     */
    public static final int[] CONNECTED_TEXTURE = {
            0, 0, 6, 6, 0, 0, 6, 6, 3, 3, 19, 15, 3, 3, 19, 15,
            1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31, 2, 2, 27, 14,
            0, 0, 6, 6, 0, 0, 6, 6, 3, 3, 19, 15, 3, 3, 19, 15,
            1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31, 2, 2, 27, 14,
            4, 4, 5, 5, 4, 4, 5, 5, 17, 17, 22, 26, 17, 17, 22, 26,
            16, 16, 20, 20, 16, 16, 28, 28, 21, 21, 46, 42, 21, 21, 43, 38,
            4, 4, 5, 5, 4, 4, 5, 5, 9, 9, 30, 12, 9, 9, 30, 12,
            16, 16, 20, 20, 16, 16, 28, 28, 25, 25, 45, 37, 25, 25, 40, 32,
            0, 0, 6, 6, 0, 0, 6, 6, 3, 3, 19, 15, 3, 3, 19, 15,
            1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31, 2, 2, 27, 14,
            0, 0, 6, 6, 0, 0, 6, 6, 3, 3, 19, 15, 3, 3, 19, 15,
            1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31, 2, 2, 27, 14,
            4, 4, 5, 5, 4, 4, 5, 5, 17, 17, 22, 26, 17, 17, 22, 26,
            7, 7, 24, 24, 7, 7, 10, 10, 29, 29, 44, 41, 29, 29, 39, 33,
            4, 4, 5, 5, 4, 4, 5, 5, 9, 9, 30, 12, 9, 9, 30, 12,
            7, 7, 24, 24, 7, 7, 10, 10, 8, 8, 36, 35, 8, 8, 34, 11};

    private WardedGlassBakedModel() {}

    /**
     * The original bit mask of the eight neighbours of a face.
     *
     * @param mask the eight neighbour flags, in the order of the original
     */
    public static int mask(boolean[] mask) {
        int id = 0;
        for (int index = 0; index <= 7 && index < mask.length; index++) {
            if (mask[index]) id += 1 << index;
        }
        return id;
    }

    /**
     * The original rune texture of a face.
     *
     * @param mask the eight neighbour flags, in the order of the original
     * @return the index of the rune texture, zero to forty six
     */
    public static int textureIndex(boolean[] mask) {
        int id = mask(mask);
        if (id > 255 || id < 0) return 0;
        return CONNECTED_TEXTURE[id];
    }
}
