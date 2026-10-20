package thaumcraft.client.renderers.block;

import net.minecraft.core.Direction;

/**
 * The rune pattern of the obsidian totem.
 *
 * <p>Original {@code BlockCosmeticSolid} (oracle
 * {@code thaumcraft/common/blocks/BlockCosmeticSolid.java}) draws the obsidian
 * totem with one of seven textures: the plain tile on the top and the bottom,
 * the base of the totem, its shaded base and the four runes of the totem.
 * The rune of a side depends on the side and on the variant of the totem.
 *
 * <p>The class is a pure geometry helper, so the texture selection can be
 * checked in a unit test.
 */
public final class ObsidianTotemBakedModel {
    /** The original plain obsidian tile. */
    public static final int TEXTURE_TILE = 0;
    /** The original base of the totem. */
    public static final int TEXTURE_BASE = 1;
    /** The original shaded base of the totem. */
    public static final int TEXTURE_SHADED = 2;
    /** The original first rune of the totem. */
    public static final int TEXTURE_RUNE_START = 3;
    /** The original number of runes of the totem. */
    public static final int TEXTURE_COUNT = 7;
    /** The original style of the plain base of the totem. */
    public static final int STYLE_BASE = 0;
    /** The original style of the shaded base of the totem. */
    public static final int STYLE_SHADED = 1;

    private ObsidianTotemBakedModel() {}

    /**
     * The original texture of a face of the totem.
     *
     * @param side    the face of the totem
     * @param style   the style of the totem, {@link #STYLE_BASE} or {@link #STYLE_SHADED}
     * @param variant the variant of the totem that selects the rune
     */
    public static int textureIndex(Direction side, int style, int variant) {
        if (side == Direction.UP || side == Direction.DOWN) return TEXTURE_TILE;
        if (style == STYLE_BASE) return TEXTURE_BASE;
        if (style == STYLE_SHADED) return TEXTURE_SHADED;
        int rune = Math.abs(side.get3DDataValue() + variant) % 4;
        return TEXTURE_RUNE_START + rune;
    }
}
