package thaumcraft.client.renderers.block;

/**
 * The two passes of the original infused ore.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/block/BlockCustomOreRenderer.java:40-63}.
 * The world ore is the stone cube, a coloured overlay on the full cube with a light
 * floor of {@code 160}, and, when the particle setting is the minimal one
 * ({@code Minecraft.gameSettings.particleSetting > 1}), a second stone pass in the
 * {@code 0.005} to {@code 0.995} box with the ordinary block light.
 *
 * <p>The particle setting, not the graphics setting, decides the second pass: the
 * oracle reads {@code field_151443_J} of {@code GameSettings}, which is
 * {@code particleSetting} (zero all, one decreased, two minimal), and it never reads
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
    /** The minimal particle setting of the original {@code particleSetting > 1}. */
    public static final int MINIMAL_PARTICLES = 2;

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
     * @param particleSetting the original {@code gameSettings.particleSetting}
     * @return true on the minimal particle setting only
     */
    public static boolean secondPass(int particleSetting) {
        return particleSetting > 1;
    }

    /** The minimum of the {@code 0.005} box. */
    public static float insetMin() { return INSET; }
    /** The maximum of the {@code 0.005} box. */
    public static float insetMax() { return 1.0F - INSET; }
}
