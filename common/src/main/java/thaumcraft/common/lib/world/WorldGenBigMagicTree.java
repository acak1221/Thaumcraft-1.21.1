package thaumcraft.common.lib.world;

import java.util.Random;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Original WorldGenBigMagicTree. Its shared first/obstruction-shortened
 * height survives chunk calls through a world-scoped integer state. A native
 * parallel generator serializes this state; chunk visitation order can differ
 * from1.7. The state is deliberately transient, as it was not saved in TC4.
 * No vanilla fancy-oak feature or Greatwood geometry substitutes this tree. */
public final class WorldGenBigMagicTree {
    /** Oracle WorldGenBigMagicTree:354 - the tree draws 11 + nextInt(12) as its height limit. */
    public static final int MIN_HEIGHT_LIMIT = 11;
    /** Oracle WorldGenBigMagicTree:354 - the tree draws 11 + nextInt(12) as its height limit. */
    public static final int HEIGHT_LIMIT_ROLLS = 12;
    /** Oracle WorldGenBigMagicTree:354 - the tree draws 11 + nextInt(12) as its height limit. */
    public static final int MAX_HEIGHT_LIMIT = MIN_HEIGHT_LIMIT + HEIGHT_LIMIT_ROLLS - 1;
    private static final int[] OTHER_COORDS = {2, 0, 0, 1, 2, 1};
    private static final Map<ServerLevel, HeightState[]> WORLD_HEIGHTS = new WeakHashMap<>();
    private static final class HeightState { int value, pinned; }
    private final boolean taint;
    public WorldGenBigMagicTree(boolean notify) { this(notify,false); }
    public WorldGenBigMagicTree(boolean notify,boolean taint) {this.taint=taint;}
    public boolean generate(WorldGenLevel level, RandomSource callerRandom, BlockPos origin) {
        HeightState state = state(level.getLevel(), taint);
        synchronized (state) {
            if (state.pinned != 0) state.value = state.pinned;
            return new Generator().generate(level, callerRandom, origin, state);
        }
    }

    private static HeightState state(ServerLevel level, boolean taint) {
        synchronized (WORLD_HEIGHTS) { return WORLD_HEIGHTS.computeIfAbsent(level, key -> new HeightState[]{new HeightState(),new HeightState()})[taint?1:0]; }
    }

    /**
     * The height limit every later tree of the level starts from: {@code 0} until the first tree rolls
     * its height, then only ever shortened by an obstructed trunk. GameTests only, with
     * {@link #setSharedHeight} and {@link #pinHeight}.
     */
    public static int sharedHeight(ServerLevel level, boolean taint) {
        HeightState state = state(level, taint);
        synchronized (state) { return state.value; }
    }

    /** GameTests only: puts back a value read with {@link #sharedHeight}. */
    public static void setSharedHeight(ServerLevel level, boolean taint, int value) {
        HeightState state = state(level, taint);
        synchronized (state) { state.value = value; }
    }

    /**
     * GameTests only: every tree starts from {@code height} until it is released with {@code 0}.
     *
     * <p>Which tree of a region rolls the shared height, and which obstructed trunk shortens it for
     * the rest, depends on the order in which the worker threads decorate the chunks, so the same
     * region of the same seed grows 346 or 1738 oak logs, none or nine mana pods. A scenario that
     * observes the decoration of a region pins the height to take that order out of its result;
     * the rule itself (one height for the level, oracle {@code BiomeGenMagicalForest.bigTree}) is
     * untouched in play.</p>
     */
    public static void pinHeight(ServerLevel level, boolean taint, int height) {
        HeightState state = state(level, taint);
        synchronized (state) { state.pinned = height; }
    }

    /**
     * The number of leaf nodes one layer of the canopy carries.
     *
     * <p>Oracle {@code WorldGenBigMagicTree:39}:
     * {@code (int)(1.382 + Math.pow(this.leafDensity * (double)this.heightLimit / (double)13.0F, 2.0F))}
     * with {@code leafDensity} of the field initialiser, {@code 1.0F}
     * ({@code WorldGenBigMagicTree:23}). The {@code 0.8} of
     * {@code setScale}({@code WorldGenBigMagicTree:343}) is only the scale
     * factor of the removed 1.6 tree, and {@code setScale} is never called for
     * the TC4 tree: {@code new WorldGenBigMagicTree(false)} is the only
     * construction of the original. The factor 0.8 therefore shrank the canopy
     * of every tree whose height was 11, 12, 17, 18, 19, 20 or 22 by one leaf
     * node per layer.</p>
     *
     * <p>The rule lives on the outer class so that it is reachable from the
     * parity test; the nested generator calls it.</p>
     */
    public static int leafNodeCount(int heightLimit) {
        return Math.max(1, (int) (1.382 + Math.pow(heightLimit / 13.0, 2)));
    }

    private static final class Generator {
    private final Random random = new Random(0);
    private MagicalTreePlacement tree;
    private final int[] base = new int[3];
    private int heightLimit, height;
    private int[][] leafNodes;
    private static final int LEAF_DISTANCE_LIMIT = 3;

    private boolean generate(WorldGenLevel level, RandomSource callerRandom, BlockPos origin, HeightState state) {
        tree = new MagicalTreePlacement(level);
        random.setSeed(callerRandom.nextLong());
        base[0] = origin.getX(); base[1] = origin.getY(); base[2] = origin.getZ();
        heightLimit = state.value;
        if (heightLimit == 0) heightLimit = MIN_HEIGHT_LIMIT + random.nextInt(HEIGHT_LIMIT_ROLLS);
        try {
            if (!validTreeLocation()) return false;
            generateLeafNodeList(); generateLeaves(); generateTrunk(); generateLeafNodeBases();
            return tree.finish();
        } finally { state.value = heightLimit; }
    }

    /**
     * The number of leaf nodes one layer of the canopy carries.
     *
     * <p>Oracle {@code WorldGenBigMagicTree:39}:
     * {@code (int)(1.382 + Math.pow(this.leafDensity * (double)this.heightLimit / (double)13.0F, 2.0F))}
     * with {@code leafDensity} of the field initialiser, {@code 1.0F}
     * ({@code WorldGenBigMagicTree:23}). The {@code 0.8} of
     * {@code setScale}({@code WorldGenBigMagicTree:343}) is only the scale
     * factor of the removed 1.6 tree, and {@code setScale} is never called for
     * the TC4 tree: {@code new WorldGenBigMagicTree(false)} is the only
     * construction of the original. The factor 0.8 therefore shrank the canopy
     * of every tree whose height was 11, 12, 17, 18, 19, 20 or 22 by one leaf
     * node per layer.
     */
    private void generateLeafNodeList() {
        height = (int)(heightLimit * .6618);
        if (height >= heightLimit) height = heightLimit - 1;
        int count = leafNodeCount(heightLimit);
        int[][] nodes = new int[count * heightLimit][4];
        int y = base[1] + heightLimit - LEAF_DISTANCE_LIMIT;
        int used = 1;
        int trunkTop = base[1] + height;
        int layer = y - base[1];
        nodes[0] = new int[] {base[0], y--, base[2], trunkTop};
        while (layer >= 0) {
            float size = layerSize(layer);
            if (size < 0) { --y; --layer; continue; }
            for (int i = 0; i < count; i++) {
                double radius = size * (random.nextFloat() + .328);
                double angle = random.nextFloat() * 2.0 * Math.PI;
                int x = Mth.floor(radius * Math.sin(angle) + base[0] + .5);
                int z = Mth.floor(radius * Math.cos(angle) + base[2] + .5);
                int[] leaf = {x, y, z};
                if (checkBlockLine(leaf, new int[] {x, y + LEAF_DISTANCE_LIMIT, z}) != -1) continue;
                int[] branch = {base[0], base[1], base[2]};
                double slope = Math.sqrt(Math.pow(Math.abs(base[0] - x), 2) + Math.pow(Math.abs(base[2] - z), 2)) * .381;
                branch[1] = y - slope > trunkTop ? trunkTop : (int)(y - slope);
                if (checkBlockLine(branch, leaf) != -1) continue;
                nodes[used++] = new int[] {x, y, z, branch[1]};
            }
            --y; --layer;
        }
        leafNodes = new int[used][4];
        System.arraycopy(nodes, 0, leafNodes, 0, used);
    }

    private float layerSize(int layer) {
        if (layer < heightLimit * .3) return -1.618F;
        float half = heightLimit / 2.0F;
        float offset = half - layer;
        float radius = offset == 0 ? half : Math.abs(offset) >= half ? 0
                : (float)Math.sqrt(Math.pow(Math.abs(half), 2) - Math.pow(Math.abs(offset), 2));
        return radius * .5F;
    }

    private void generateLeaves() {
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
        for (int[] node : leafNodes) for (int y = 0; y < LEAF_DISTANCE_LIMIT; y++) {
            float radius = y != 0 && y != LEAF_DISTANCE_LIMIT - 1 ? 3 : 2;
            int spread = (int)(radius + .618);
            for (int x = -spread; x <= spread; x++) for (int z = -spread; z <= spread; z++) {
                if (Math.pow(Math.abs(x) + .5, 2) + Math.pow(Math.abs(z) + .5, 2) > radius * radius) continue;
                BlockPos pos = new BlockPos(node[0] + x, node[1] + y, node[2] + z);
                if (!tree.available(pos)) { tree.reject(); continue; }
                BlockState existing = tree.state(pos);
                // Original LEAVES accepts only old LOG/LEAVES species0..3,
                // not LEAVES2 acacia/dark-oak or magical/modded foliage.
                if (clear(existing)) tree.put(pos, leaves);
            }
        }
    }

    private void generateLeafNodeBases() {
        for (int[] node : leafNodes) {
            if (node[3] - base[1] < heightLimit * .2) continue;
            placeBlockLine(new int[] {base[0], node[3], base[2]}, new int[] {node[0], node[1], node[2]});
        }
    }

    private void generateTrunk() {
        placeBlockLine(new int[] {base[0], base[1], base[2]}, new int[] {base[0], base[1] + height, base[2]});
    }

    private void placeBlockLine(int[] start, int[] end) {
        int[] delta = {end[0] - start[0], end[1] - start[1], end[2] - start[2]};
        int major = 0;
        for (int i = 0; i < 3; i++) if (Math.abs(delta[i]) > Math.abs(delta[major])) major = i;
        if (delta[major] == 0) return;
        int first = OTHER_COORDS[major], second = OTHER_COORDS[major + 3];
        int step = delta[major] > 0 ? 1 : -1;
        double slope1 = (double)delta[first] / delta[major], slope2 = (double)delta[second] / delta[major];
        int[] pos = new int[3];
        for (int offset = 0; offset != delta[major] + step; offset += step) {
            pos[major] = Mth.floor(start[major] + offset + .5);
            pos[first] = Mth.floor(start[first] + offset * slope1 + .5);
            pos[second] = Mth.floor(start[second] + offset * slope2 + .5);
            int dx = Math.abs(pos[0] - start[0]), dz = Math.abs(pos[2] - start[2]);
            int horizontal = Math.max(dx, dz);
            Direction.Axis axis = horizontal == 0 ? Direction.Axis.Y : dx == horizontal ? Direction.Axis.X : Direction.Axis.Z;
            tree.put(new BlockPos(pos[0], pos[1], pos[2]), Blocks.OAK_LOG.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, axis));
        }
    }

    private int checkBlockLine(int[] start, int[] end) {
        int[] delta = {end[0] - start[0], end[1] - start[1], end[2] - start[2]};
        int major = 0;
        for (int i = 0; i < 3; i++) if (Math.abs(delta[i]) > Math.abs(delta[major])) major = i;
        if (delta[major] == 0) return -1;
        int first = OTHER_COORDS[major], second = OTHER_COORDS[major + 3];
        int step = delta[major] > 0 ? 1 : -1;
        double slope1 = (double)delta[first] / delta[major], slope2 = (double)delta[second] / delta[major];
        int[] pos = new int[3];
        int offset;
        for (offset = 0; offset != delta[major] + step; offset += step) {
            pos[major] = start[major] + offset;
            pos[first] = Mth.floor(start[first] + offset * slope1);
            pos[second] = Mth.floor(start[second] + offset * slope2);
            if (!clear(tree.state(new BlockPos(pos[0], pos[1], pos[2])))) break;
        }
        return offset == delta[major] + step ? -1 : Math.abs(offset);
    }

    private boolean validTreeLocation() {
        BlockPos soil = new BlockPos(base[0], base[1] - 1, base[2]);
        if (!tree.available(soil) || !MagicalTreePlacement.soil(tree.state(soil))) return false;
        int obstruction = checkBlockLine(new int[] {base[0], base[1], base[2]},
                new int[] {base[0], base[1] + heightLimit - 1, base[2]});
        if (obstruction == -1) return true;
        if (obstruction < 6) return false;
        heightLimit = obstruction;
        return true;
    }

    private static boolean clear(BlockState state) { return state.isAir() || state.is(Blocks.OAK_LEAVES) || state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.BIRCH_LEAVES) || state.is(Blocks.JUNGLE_LEAVES); }
    }
}
