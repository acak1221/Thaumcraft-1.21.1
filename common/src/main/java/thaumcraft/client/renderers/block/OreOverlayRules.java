package thaumcraft.client.renderers.block;

/**
 * The two passes of the original infused ore.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/block/BlockCustomOreRenderer.java:54-70}.
 * The world ore is the stone cube, a coloured overlay on the full cube with a light
 * floor of {@code 160}, and, when anisotropic filtering exceeds one
 * ({@code Minecraft.gameSettings.anisotropicFiltering > 1}), a second stone pass in the
 * {@code 0.005} to {@code 0.995} box with the ordinary block light.
 *
 * <p>Anisotropic filtering, not the graphics or particle setting, decides the second pass: the
 * oracle reads {@code field_151443_J} of {@code GameSettings}, which is
 * {@code anisotropicFiltering} (FML 1.7.10 conf/fields.csv:4214), and it never reads
 * {@code field_74347_j} (fancy graphics). The inventory overlay is the same
 * {@code 0.005} box as the second world pass.
 *
 * <p>The class is a pure rule, so the floor and the box can be checked in a unit test.
 */
public final class OreOverlayRules {
    /** Oracle {@code Math.max(bb, 160)} of the coloured overlay. */
    public static final int MIN_OVERLAY_LIGHT = 160;
    /** Oracle the {@code 0.005} inset of the second world pass and of the inventory. */
    public static final float INSET = 0.005F;
    /** First enabled value of the original {@code anisotropicFiltering > 1}. */
    public static final int MIN_ANISOTROPY_FOR_STONE = 2;

    private OreOverlayRules() {}

    /**
     * The light of the coloured overlay, {@code Math.max(bb, 160)}.
     *
     * @param blockLight the original {@code getMixedBrightnessForBlock} of the ore
     * @return the light of the overlay pass
     */
    public static int overlayLight(int blockLight) {
        return Math.max(blockLight, MIN_OVERLAY_LIGHT);
    }

    /**
     * Whether the original draws the second stone pass.
     *
     * @param anisotropicFiltering the original {@code gameSettings.anisotropicFiltering}
     * @return true when anisotropic filtering exceeds one
     */
    public static boolean secondPass(int anisotropicFiltering) {
        return anisotropicFiltering > 1;
    }

    /** The minimum of the {@code 0.005} box. */
    public static float insetMin() { return INSET; }
    /** The maximum of the {@code 0.005} box. */
    public static float insetMax() { return 1.0F - INSET; }
}
