package thaumcraft.client.renderers.block;

import net.minecraft.core.Direction;

/**
 * The geometry of the eldritch crust block.
 *
 * <p>Original {@code BlockEldritchRenderer} (oracle
 * {@code thaumcraft/client/renderers/block/BlockEldritchRenderer.java}) draws the
 * metadata four, five and six of the eldritch stone with a box that is inset by
 * two sixteenths on every side that has no eldritch neighbour. The native
 * Minecraft 1.21.1 block state has no neighbour mask, so the bounds are computed
 * here from the six neighbour flags.
 *
 * <p>The class is a pure geometry helper: it has no world and no renderer, so
 * the bounds can be checked in a unit test.
 */
public final class EldritchCrustBakedModel {
    /** The original inset of the crust, in sixteenths of a block. */
    public static final float INSET = 2.0F;
    /** The original full size of a block, in sixteenths of a block. */
    public static final float FULL = 16.0F;

    /** The six bounds of the crust box, in the order of {@link Direction}. */
    private final float[] bounds = new float[6];

    /**
     * Computes the bounds of the crust.
     *
     * @param west  true when the block to the west is eldritch crust
     * @param down  true when the block below is eldritch crust
     * @param north true when the block to the north is eldritch crust
     * @param east  true when the block to the east is eldritch crust
     * @param up    true when the block above is eldritch crust
     * @param south true when the block to the south is eldritch crust
     */
    public EldritchCrustBakedModel(boolean west, boolean down, boolean north, boolean east, boolean up, boolean south) {
        bounds[0] = west ? 0.0F : INSET;
        bounds[1] = down ? 0.0F : INSET;
        bounds[2] = north ? 0.0F : INSET;
        bounds[3] = east ? FULL : FULL - INSET;
        bounds[4] = up ? FULL : FULL - INSET;
        bounds[5] = south ? FULL : FULL - INSET;
    }

    /** The minimum x of the crust box. */
    public float minX() { return bounds[0]; }
    /** The minimum y of the crust box. */
    public float minY() { return bounds[1]; }
    /** The minimum z of the crust box. */
    public float minZ() { return bounds[2]; }
    /** The maximum x of the crust box. */
    public float maxX() { return bounds[3]; }
    /** The maximum y of the crust box. */
    public float maxY() { return bounds[4]; }
    /** The maximum z of the crust box. */
    public float maxZ() { return bounds[5]; }

    /** The six bounds of the crust box, in the order of {@link Direction}. */
    public float[] bounds() { return bounds.clone(); }
}
