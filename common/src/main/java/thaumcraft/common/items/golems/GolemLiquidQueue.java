package thaumcraft.common.items.golems;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;

/**
 * Pure rules of the Decanting golem's Entropy queue (original AILiquidGather:168-183, 255-300). With an Entropy upgrade the golem
 * does not drain the marked block but a queue built from it: every source block of the same liquid that is connected to the marked
 * block through the 26-neighbourhood of liquid blocks and lies within {@code range} of it, the <em>farthest first</em>. The marked
 * block itself is reachable from its neighbours, so it comes last.
 *
 * <p>The traversal is the original depth-first walk with its loop order (x, y, z each from -1 to +1), so cells of equal distance
 * keep the original order. Deliberate, documented deviations (docs/analysis/2026-10-19-golem-liquid-queue.md): a cell is queued
 * once (the original queued it once per visited neighbour and, when the very last entry had already been drained, granted a
 * bucket for it from nothing), the walk is iterative, and it stops after {@link #MAX_VISITED} cells so a huge lake cannot stall
 * the server thread.
 */
public final class GolemLiquidQueue {
    public static final int MAX_VISITED = 65536;

    private GolemLiquidQueue() {}

    /** Original {@code dist > pumpDist}: the cell is skipped when its squared distance exceeds {@code range * range} (float arithmetic). */
    public static boolean inRange(BlockPos origin, BlockPos cell, float range) {
        return (float) origin.distSqr(cell) <= range * range;
    }

    /**
     * The queued cells in the order they are drained.
     *
     * @param connects whether a cell is liquid of the wanted kind (source or flowing), so the walk may pass through it
     * @param source   whether a cell is a source block that may actually be drained
     */
    public static List<BlockPos> build(BlockPos origin, float range, Predicate<BlockPos> connects, Predicate<BlockPos> source) {
        BlockPos start = origin.immutable();
        Map<BlockPos, Long> found = new LinkedHashMap<>(); // first occurrence only; insertion order is discovery order
        Set<BlockPos> visited = new HashSet<>();
        visited.add(start);
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(start));
        search:
        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            if (frame.next == 27) { stack.pop(); continue; }
            int index = frame.next++;
            int dx = index / 9 - 1, dy = index / 3 % 3 - 1, dz = index % 3 - 1;
            if (dx == 0 && dy == 0 && dz == 0) continue;
            BlockPos cell = frame.pos.offset(dx, dy, dz);
            if (!inRange(start, cell, range) || !connects.test(cell)) continue;
            boolean fresh = !visited.contains(cell);
            if (fresh && visited.size() >= MAX_VISITED) break search;
            if (source.test(cell)) found.putIfAbsent(cell, (long) start.distSqr(cell));
            if (fresh) {
                visited.add(cell);
                stack.push(new Frame(cell));
            }
        }
        List<Map.Entry<BlockPos, Long>> ordered = new ArrayList<>(found.entrySet());
        ordered.sort(Map.Entry.<BlockPos, Long>comparingByValue(Comparator.reverseOrder())); // stable: ties keep discovery order
        List<BlockPos> queue = new ArrayList<>(ordered.size());
        for (Map.Entry<BlockPos, Long> entry : ordered) queue.add(entry.getKey());
        return queue;
    }

    private static final class Frame {
        final BlockPos pos;
        int next;

        Frame(BlockPos pos) { this.pos = pos; }
    }
}
