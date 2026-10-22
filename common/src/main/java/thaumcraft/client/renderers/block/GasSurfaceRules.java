package thaumcraft.client.renderers.block;

/**
 * The surface geometry and the shading of the original flux gas and flux goo.
 *
 * <p>Oracle {@code thaumcraft/client/renderers/block/BlockGasRenderer.java}. The
 * original is a finite Forge fluid: the block metadata is the amount of quanta minus
 * one, the surface of the fluid is a bilinear patch whose four corners are averages
 * of the nine heights of {@code getFluidHeightForRender}, and the renderer shades the
 * faces with the fixed {@code LIGHT_*} factors of its own Tessellator calls.
 *
 * <p>The class is a pure rule: it receives the already sampled facts of the world and
 * returns numbers, so the height of a corner and the size of a face can be checked in
 * a unit test.
 *
 * <p>Forge pin: {@code MinecraftForge/MinecraftForge@1.7.10},
 * {@code src/main/java/net/minecraftforge/fluids/BlockFluidBase.java:404-408}
 * ({@code getQuantaPercentage = quanta / quantaPerBlock}) and
 * {@code BlockFluidFinite.java:29-54} ({@code quanta = metadata + 1}, the air zero, the
 * foreign block minus one and {@code getMaxRenderHeightMeta = quantaPerBlock - 1}).
 */
public final class GasSurfaceRules {
    /** Oracle {@code LIGHT_Y_NEG}. */
    public static final float LIGHT_Y_NEG = 0.5F;
    /** Oracle {@code LIGHT_Y_POS}. */
    public static final float LIGHT_Y_POS = 1.0F;
    /** Oracle {@code LIGHT_XZ_NEG}, the north and south faces. */
    public static final float LIGHT_XZ_NEG = 0.8F;
    /** Oracle {@code LIGHT_XZ_POS}, the west and east faces. */
    public static final float LIGHT_XZ_POS = 0.6F;
    /** Oracle {@code RENDER_OFFSET}, the {@code 0.001} of the bottom and side planes. */
    public static final double RENDER_OFFSET = 0.001F;
    /** Oracle {@code getMaxRenderHeightMeta} of a finite fluid of eight quanta. */
    public static final int MAX_RENDER_HEIGHT_META = 7;
    /** Oracle {@code quantaPerBlock}. */
    public static final int QUANTA_PER_BLOCK = 8;
    /** Oracle the {@code 0.875} factor of the render height. */
    public static final float RENDER_HEIGHT_FACTOR = 0.875F;
    /** Oracle the {@code 0.875} threshold of {@code getFluidHeightAverage}. */
    public static final float AVERAGE_OVERRIDE = 0.875F;

    private GasSurfaceRules() {}

    /**
     * The original {@code getFluidHeightForRender}.
     *
     * @param sameFluid       the sampled cell holds the same fluid block
     * @param belowLiquid     the material below the sampled cell is a liquid
     * @param metadata        the metadata of the sampled cell
     * @param cellSolid       the material of the sampled cell is solid
     * @param belowSameFluid  the cell below the sampled cell holds the same fluid block
     * @param quantaValue     the original {@code getQuantaValue}: zero for air, minus one
     *                        for a foreign block, {@code metadata + 1} for the same fluid
     * @return the height of the sampled cell
     */
    public static float heightForRender(boolean sameFluid, boolean belowLiquid, int metadata,
                                        boolean cellSolid, boolean belowSameFluid, int quantaValue) {
        if (sameFluid) {
            if (belowLiquid) return 1.0F;
            if (metadata == MAX_RENDER_HEIGHT_META) return RENDER_HEIGHT_FACTOR;
        }
        return !cellSolid && belowSameFluid ? 1.0F : quantaValue / (float) QUANTA_PER_BLOCK * RENDER_HEIGHT_FACTOR;
    }

    /**
     * The original {@code getFluidHeightAverage}.
     *
     * <p>The last sample at or above the {@code 0.875} override wins, unless a full
     * {@code 1.0} was already seen; every negative sample is ignored and a result of
     * zero falls back to the mean of the non negative samples.
     *
     * @param flow the four samples of one corner
     * @return the height of the corner
     */
    public static float heightAverage(float[] flow) {
        float total = 0.0F;
        int count = 0;
        float end = 0.0F;
        for (float sample : flow) {
            if (sample >= AVERAGE_OVERRIDE && end != 1.0F) end = sample;
            if (!(sample >= 0.0F)) continue;
            total += sample;
            ++count;
        }
        if (end == 0.0F) end = total / (float) count;
        return end;
    }

    /** The four surface corners of one cell, in the order of the original. */
    public record Corners(float northWest, float southWest, float southEast, float northEast) {}

    /**
     * The four corners of the surface from the nine samples of the original.
     *
     * <p>The samples are indexed {@code (dx + 1) * 3 + (dz + 1)} with north at minus z
     * and west at minus x, exactly like the {@code flow00} to {@code flow22} of
     * {@code BlockGasRenderer.java:95-113}.
     *
     * @param flow the nine samples of the three by three neighbourhood
     * @return the four corners of the surface
     */
    public static Corners corners(float[] flow) {
        if (flow.length != 9) throw new IllegalArgumentException("The original samples nine cells, not " + flow.length);
        float nw = heightAverage(new float[]{flow[0], flow[1], flow[3], flow[4]});
        float sw = heightAverage(new float[]{flow[1], flow[2], flow[5], flow[4]});
        float se = heightAverage(new float[]{flow[5], flow[7], flow[8], flow[4]});
        float ne = heightAverage(new float[]{flow[3], flow[6], flow[7], flow[4]});
        return new Corners(nw, sw, se, ne);
    }

    /**
     * The original {@code rises} flag: a flux gas has the density direction one and a
     * flux goo has the density direction minus one.
     *
     * @param densityDirection the original {@code densityDir} of the fluid
     * @return true for the gas, false for the goo
     */
    public static boolean rises(int densityDirection) {
        return densityDirection == 1;
    }

    /**
     * The shading factor the original writes into the colour of one face.
     *
     * @param face  the original face ordinal: zero north, one south, two west, three east
     * @param top   true for the top surface, false for the bottom plane
     * @return the factor of the original {@code setColorOpaque_F}
     */
    public static float sideLight(int face) {
        return face < 2 ? LIGHT_XZ_NEG : LIGHT_XZ_POS;
    }

    /** The original bottom factor: {@code 0.5} for the goo, {@code 1.0} for the gas. */
    public static float bottomLight(boolean rises) {
        return rises ? LIGHT_Y_POS : LIGHT_Y_NEG;
    }

    /** The original top factor: the top surface is never darkened. */
    public static float topLight() {
        return LIGHT_Y_POS;
    }
}
