package thaumcraft.common.lib.tiles;

/**
 * The numbers of the TC4 block entities that the 1.21.1 port reproduces, taken from the
 * TC4 4.2.3.5 oracle (the decompiled 1.7.10 original) and not from the 1.12 reference.
 * <p>
 * Every constant quotes the oracle file and line so a reviewer can check the port against
 * the authority:
 * <ul>
 * <li>{@code thaumcraft/common/tiles/TileCrucible.java} - the tank, the heat and the decay.</li>
 * <li>{@code thaumcraft/common/tiles/TileJarFillable.java} and {@code TileJarFillableVoid.java} - the jars.</li>
 * <li>{@code thaumcraft/common/tiles/TileAlembic.java} - the alembic.</li>
 * <li>{@code thaumcraft/common/tiles/TileInfusionMatrix.java} - the matrix.</li>
 * <li>{@code thaumcraft/common/tiles/TileThaumatorium.java} - the thaumatorium.</li>
 * <li>{@code thaumcraft/common/tiles/TileMirror.java} and {@code TileMirrorEssentia.java} - the mirrors.</li>
 * </ul>
 * The classes that own the world dependent part call these methods, so the numbers can be
 * checked without a game runtime.
 */
public final class TileParityRules {
    // ---------------------------------------------------------------------------------------------
    // Crucible (oracle thaumcraft/common/tiles/TileCrucible.java)
    // ---------------------------------------------------------------------------------------------

    /** TileCrucible:71 - the crucible holds a thousand units of water. */
    public static final int CRUCIBLE_CAPACITY = 1000;
    /** TileCrucible:68 - a hundred aspects are the nominal limit of the crucible. */
    public static final int CRUCIBLE_MAX_TAGS = 100;
    /** TileCrucible:108 - the crucible boils at one hundred and fifty one. */
    public static final int CRUCIBLE_BOIL_HEAT = 151;
    /** TileCrucible:106 - the heat stops growing at two hundred. */
    public static final int CRUCIBLE_MAX_HEAT = 200;
    /** TileCrucible:114 - the heat falls by one a tick. */
    public static final int CRUCIBLE_COOL_STEP = 1;
    /** TileCrucible:107 - every bellows adds two to the heat step. */
    public static final int CRUCIBLE_BELLOWS_STEP = 2;
    /** TileCrucible:360-371 - the bellows are counted once and then kept. */
    public static final int CRUCIBLE_BELLOWS_UNCOUNTED = -1;
    /** TileCrucible:123 - the overflow is offered on every fifth tick. */
    public static final int CRUCIBLE_OVERFLOW_INTERVAL = 5;
    /** TileCrucible:127 - the decay waits for a hundred ticks of the counter. */
    public static final long CRUCIBLE_DECAY_COUNTER = 100L;
    /** TileCrucible:127 - the decay needs a heat above one hundred and fifty. */
    public static final int CRUCIBLE_DECAY_HEAT = 150;
    /** TileCrucible:135 - the decay spends two units of water. */
    public static final int CRUCIBLE_DECAY_WATER = 2;
    /** TileCrucible:264 - a smelt spends fifty units of water. */
    public static final int CRUCIBLE_SMELT_WATER = 50;

    /** TileCrucible:107 - the heat grows by one plus two for every bellows. */
    public static int crucibleHeatStep(int bellows) { return 1 + CRUCIBLE_BELLOWS_STEP * Math.max(0, bellows); }

    /** TileCrucible:68 - the crucible overflows above a hundred aspects. */
    public static boolean crucibleOverflows(int tags) { return tags > CRUCIBLE_MAX_TAGS; }

    /** TileCrucible:127 - the decay runs on a counter above a hundred and a hot crucible. */
    public static boolean crucibleDecays(long counter, int heat) { return counter > CRUCIBLE_DECAY_COUNTER && heat > CRUCIBLE_DECAY_HEAT; }

    /** TileCrucible:315-325 - the fluid height of the crucible. */
    public static float crucibleFluidHeight(int water, int tags) {
        float base = 0.3F + 0.5F * water / CRUCIBLE_CAPACITY;
        float height = base + tags / 100F * (1 - base);
        if (height > 1.0F) height = 1.001F;
        if (height == 1.0F) height = 0.9999F;
        return height;
    }

    // ---------------------------------------------------------------------------------------------
    // Jars (oracle thaumcraft/common/tiles/TileJarFillable.java, TileJarFillableVoid.java)
    // ---------------------------------------------------------------------------------------------

    /** TileJarFillable:28 - a jar holds sixty four units of one aspect. */
    public static final int JAR_CAPACITY = 64;
    /** TileJarFillable:200 - the jar draws one unit every fifth tick. */
    public static final int JAR_PULL_INTERVAL = 5;
    /** TileJarFillable:158,168-174 - an unlabelled jar offers thirty two. */
    public static final int JAR_SUCTION = 32;
    /** TileJarFillable:158,168-174 - a labelled jar offers sixty four. */
    public static final int JAR_LABELLED_SUCTION = 64;
    /** TileJarFillableVoid:41,46 - a labelled void jar offers forty eight. */
    public static final int JAR_VOID_LABELLED_SUCTION = 48;

    /** TileJarFillable:158 - the minimum suction of a jar. */
    public static int jarMinimumSuction(boolean labelled, boolean voidJar) {
        if (!labelled) return JAR_SUCTION;
        return voidJar ? JAR_VOID_LABELLED_SUCTION : JAR_LABELLED_SUCTION;
    }

    /** TileJarFillable:167-175 - the suction a jar offers. */
    public static int jarSuction(boolean labelled, boolean voidJar, int amount) {
        if (voidJar) return labelled && amount < JAR_CAPACITY ? JAR_VOID_LABELLED_SUCTION : JAR_SUCTION;
        return amount < JAR_CAPACITY ? (labelled ? JAR_LABELLED_SUCTION : JAR_SUCTION) : 0;
    }

    // ---------------------------------------------------------------------------------------------
    // Alembic (oracle thaumcraft/common/tiles/TileAlembic.java)
    // ---------------------------------------------------------------------------------------------

    /** TileAlembic:44 - the alembic holds thirty two units of one aspect. */
    public static final int ALEMBIC_CAPACITY = 32;
    /** TileAlembic:194-205 - the alembic is connected on every side but down and its front. */
    public static boolean alembicConnectable(int faceOrdinal, int facingOrdinal) {
        return faceOrdinal != facingOrdinal && faceOrdinal != 0;
    }

    // ---------------------------------------------------------------------------------------------
    // Infusion matrix (oracle thaumcraft/common/tiles/TileInfusionMatrix.java)
    // ---------------------------------------------------------------------------------------------

    /** TileInfusionMatrix:110 - a craft cycle runs every ten ticks. */
    public static final int INFUSION_CYCLE_DELAY = 10;
    /** TileInfusionMatrix:431 - a cycle that takes an experience level runs every twenty ticks. */
    public static final int INFUSION_XP_CYCLE_DELAY = 20;
    /** TileInfusionMatrix:217 - the structure is checked every hundred ticks, every twenty while crafting. */
    public static final int INFUSION_STRUCTURE_PERIOD = 100, INFUSION_STRUCTURE_PERIOD_CRAFTING = 20;
    /** TileInfusionMatrix:349 - the instability rolls against five hundred. */
    public static final int INFUSION_INSTABILITY_ROLL = 500;
    /** TileInfusionMatrix:350 - the hazard is one of twenty one outcomes. */
    public static final int INFUSION_OUTCOMES = 21;
    /** TileInfusionMatrix:441-443,466 - the instability never rises above twenty five. */
    public static final int INFUSION_INSTABILITY_CAP = 25;
    /** TileInfusionMatrix:480 - the ingredient countdown starts at five. */
    public static final int INFUSION_INGREDIENT_COUNTDOWN = 5;
    /** TileInfusionMatrix:614-617 - the surroundings are scanned in a cube of twenty five. */
    public static final int INFUSION_SCAN_RADIUS = 12, INFUSION_SCAN_MIN_DEPTH = 5, INFUSION_SCAN_MAX_DEPTH = 10;
    /** TileInfusionMatrix:623 - a pedestal counts only inside a square of seventeen. */
    public static final int INFUSION_PEDESTAL_RADIUS = 8;
    /** TileInfusionMatrix:456 - the essentia source search reaches twelve blocks. */
    public static final int INFUSION_SOURCE_RANGE = 12;

    /** TileInfusionMatrix:217 - the structure check period of a matrix. */
    public static int infusionStructurePeriod(boolean crafting) {
        return crafting ? INFUSION_STRUCTURE_PERIOD_CRAFTING : INFUSION_STRUCTURE_PERIOD;
    }

    /** TileInfusionMatrix:349 - whether the instability ends the cycle. */
    public static boolean infusionInstabilityTriggers(int instability, int roll) {
        return instability > 0 && roll <= instability;
    }

    /** TileInfusionMatrix:441-443 - the instability is capped. */
    public static int infusionInstabilityAfter(int instability, int increase) {
        return Math.min(INFUSION_INSTABILITY_CAP, instability + increase);
    }

    /** TileInfusionMatrix:623 - a pedestal only counts inside the square of seventeen. */
    public static boolean infusionPedestalCounts(int dx, int dz) {
        return Math.abs(dx) <= INFUSION_PEDESTAL_RADIUS && Math.abs(dz) <= INFUSION_PEDESTAL_RADIUS;
    }

    // ---------------------------------------------------------------------------------------------
    // Thaumatorium (oracle thaumcraft/common/tiles/TileThaumatorium.java)
    // ---------------------------------------------------------------------------------------------

    /** TileThaumatorium:183 - the work runs every fifth tick. */
    public static final int THAUMATORIUM_WORK_INTERVAL = 5;
    /** TileThaumatorium:178 - the heat and the upgrades are read every fortieth tick. */
    public static final int THAUMATORIUM_HEAT_INTERVAL = 40;
    /** TileThaumatorium:360 - the thaumatorium offers a suction of one hundred and twenty eight. */
    public static final int THAUMATORIUM_SUCTION = 128;
    /** TileThaumatorium:461 - the catalyst slot holds sixty four items. */
    public static final int THAUMATORIUM_STACK_LIMIT = 64;
    /** TileThaumatorium:465 - the machine may be used from sixty four blocks away. */
    public static final double THAUMATORIUM_USE_RANGE = 64.0D;

    // ---------------------------------------------------------------------------------------------
    // Mirrors (oracle thaumcraft/common/tiles/TileMirror.java, TileMirrorEssentia.java)
    // ---------------------------------------------------------------------------------------------

    /** TileMirror:64 - a broken link is retried every fortieth tick. */
    public static final int MIRROR_LINK_RETRY = 40;
    /** TileMirror:297-300 - every failed retry waits twenty ticks longer, up to six hundred. */
    public static final int MIRROR_LINK_RETRY_STEP = 20, MIRROR_LINK_RETRY_MAX = 600;
    /** TileMirror:310 - the instability falls by one every twentieth tick. */
    public static final int MIRROR_INSTABILITY_DECAY = 20;
    /** TileMirror:290 - the ejection rate is the square of the instability over fifty. */
    public static final int MIRROR_INSTABILITY_DIVISOR = 50;
    /** TileMirror:199 - nothing leaves the mirror in its first twenty ticks. */
    public static final int MIRROR_EJECT_DELAY = 20;
    /** TileMirror:346-348 - the inventory of the mirror holds a single slot. */
    public static final int MIRROR_INVENTORY_SLOTS = 1;
    /** TileMirror:387-389 - a slot of the mirror holds sixty four items. */
    public static final int MIRROR_STACK_LIMIT = 64;
    /** TileMirrorEssentia:186-190 - the essentia mirror hands out one unit at a time. */
    public static final int MIRROR_ESSENTIA_MAX_TAKE = 1;
    /** TileMirrorEssentia:194 - the essentia mirror reaches eight units deep. */
    public static final int MIRROR_ESSENTIA_RANGE = 8;

    /** TileMirror:290 - the number of ticks between two ejections. */
    public static int mirrorEjectRate(int instability) {
        int rate = instability / MIRROR_INSTABILITY_DIVISOR;
        return rate * rate;
    }

    /** TileMirror:295,310 - the counter of the mirror is read before it is raised. */
    public static boolean mirrorRunsOnTick(int count, int period) { return count % period == 0; }

    private TileParityRules() {}
}
