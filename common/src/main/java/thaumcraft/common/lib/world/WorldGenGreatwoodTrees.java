package thaumcraft.common.lib.world;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.blocks.BlockMagicalLeaves;
import thaumcraft.common.config.ThaumcraftNaturals;

/**
 * TC4 4.2.3.5 WorldGenGreatwoodTrees:40-380, not a vanilla oak feature.
 * Retains the private java.util.Random seeded by ONE caller nextLong, the
 * mutable obstruction-shortened height, 2x2 root search and two canopy tiers.
 * Native build limits/writable chunks replace legacy exception swallowing;
 * Math sin/cos/floor are not a promise of old-world seed identity.
 */
public final class WorldGenGreatwoodTrees {
    private static final int[] OTHER_COORDS = {2, 0, 0, 1, 2, 1};
    private static final AtomicLong SKIPPED_SPIDER_DUNGEONS = new AtomicLong();
    private final Random random = new Random(0);
    private MagicalTreePlacement tree;
    private final int[] base = new int[3];
    private int heightLimit;
    private int height;
    private double scaleWidth = 1.2;
    private int[][] leafNodes;
    private static final int TRUNK_SIZE = 2;
    private static final int HEIGHT_LIMIT_LIMIT = 11;
    private static final int LEAF_DISTANCE_LIMIT = 4;

    // Notify differences are handled by the private atomic native canopy commit.
    public WorldGenGreatwoodTrees(boolean notify) {}

    public boolean generate(WorldGenLevel level, RandomSource callerRandom, BlockPos origin, boolean spiders) {
        tree = new MagicalTreePlacement(level);
        random.setSeed(callerRandom.nextLong());
        base[0] = origin.getX(); base[1] = origin.getY(); base[2] = origin.getZ();
        if (heightLimit == 0) heightLimit = HEIGHT_LIMIT_LIMIT + random.nextInt(HEIGHT_LIMIT_LIMIT);
        boolean valid = false;
        search: for (int a = -1; a < 2; a++) {
            candidate: for (int b = -1; b < 2; b++) {
                for (int x = 0; x < TRUNK_SIZE; x++) for (int z = 0; z < TRUNK_SIZE; z++) {
                    if (!validTreeLocation(x + a, z + b)) continue candidate;
                }
                valid = true;
                base[0] += a; base[2] += b;
                break search;
            }
        }
        if (!valid) return false;
        generateLeafNodeList();
        generateLeaves();
        generateLeafNodeBases();
        generateTrunk();
        scaleWidth = 1.66;
        // Original quirk: the second tier resets X/Z to the requested origin,
        // not the offset chosen by the lower tier's 2x2 soil search.
        base[0] = origin.getX(); base[1] = origin.getY() + height; base[2] = origin.getZ();
        generateLeafNodeList();
        generateLeaves();
        generateLeafNodeBases();
        generateTrunk();
        if (!tree.finish()) return false;
        if (spiders) {
            // The oracle only names Forge's mutable dungeonChest hook; it does
            // not contain that complete 1.7 table (and several TC4 loot items
            // are still unported). Do NOT substitute 1.21 simple_dungeon loot,
            // an empty chest, or a spawner with counterfeit treasure. Preserve
            // the selected branch and caller's 50*3 cobweb coordinate draws.
            if (SKIPPED_SPIDER_DUNGEONS.getAndIncrement() == 0) Thaumcraft.LOG.warn(
                    "Greatwood spider-dungeon decoration is pending original dungeonChest loot provenance; rolled branch omitted without replacement (tree retained)");
            for (int i = 0; i < 50; i++) {
                callerRandom.nextInt(14); callerRandom.nextInt(10); callerRandom.nextInt(14);
            }
        }
        return true;
    }

    public static long skippedSpiderDungeons() { return SKIPPED_SPIDER_DUNGEONS.get(); }

    private void generateLeafNodeList() {
        height = (int)(heightLimit * .618);
        if (height >= heightLimit) height = heightLimit - 1;
        int count = Math.max(1, (int)(1.382 + Math.pow(.9 * heightLimit / 13.0, 2)));
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
                double radius = scaleWidth * size * (random.nextFloat() + .328);
                double angle = random.nextFloat() * 2.0 * Math.PI;
                int x = Mth.floor(radius * Math.sin(angle) + base[0] + .5);
                int z = Mth.floor(radius * Math.cos(angle) + base[2] + .5);
                int[] leaf = {x, y, z};
                if (checkBlockLine(leaf, new int[] {x, y + LEAF_DISTANCE_LIMIT, z}) != -1) continue;
                int[] branch = {base[0], base[1], base[2]};
                double slope = Math.sqrt(Math.pow(Math.abs(base[0] - x), 2) + Math.pow(Math.abs(base[2] - z), 2)) * .38;
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
        BlockState leaves = ThaumcraftNaturals.GREATWOOD_LEAVES.get().defaultBlockState();
        for (int[] node : leafNodes) for (int y = 0; y < LEAF_DISTANCE_LIMIT; y++) {
            float radius = y != 0 && y != LEAF_DISTANCE_LIMIT - 1 ? 3 : 2;
            int spread = (int)(radius + .618);
            for (int x = -spread; x <= spread; x++) for (int z = -spread; z <= spread; z++) {
                if (Math.pow(Math.abs(x) + .5, 2) + Math.pow(Math.abs(z) + .5, 2) > radius * radius) continue;
                BlockPos pos = new BlockPos(node[0] + x, node[1] + y, node[2] + z);
                if (!tree.available(pos)) { tree.reject(); continue; }
                BlockState existing = tree.state(pos);
                // The original greatwood layer only accepts air/magical leaves,
                // unlike silverwood's general canBeReplacedByLeaves predicate.
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
        int[] start = {base[0], base[1], base[2]};
        int[] end = {base[0], base[1] + height, base[2]};
        placeBlockLine(start, end);
        ++start[0]; ++end[0]; placeBlockLine(start, end);
        ++start[2]; ++end[2]; placeBlockLine(start, end);
        --start[0]; --end[0]; placeBlockLine(start, end);
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
            tree.put(new BlockPos(pos[0], pos[1], pos[2]), ThaumcraftNaturals.GREATWOOD_LOG.get().defaultBlockState()
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

    private boolean validTreeLocation(int x, int z) {
        BlockPos soil = new BlockPos(base[0] + x, base[1] - 1, base[2] + z);
        if (!tree.available(soil) || !MagicalTreePlacement.soil(tree.state(soil))) return false;
        int obstruction = checkBlockLine(new int[] {base[0] + x, base[1], base[2] + z},
                new int[] {base[0] + x, base[1] + heightLimit - 1, base[2] + z});
        if (obstruction == -1) return true;
        if (obstruction < 6) return false;
        heightLimit = obstruction;
        return true;
    }

    private static boolean clear(BlockState state) { return state.isAir() || state.getBlock() instanceof BlockMagicalLeaves; }
}
