package thaumcraft.common.lib.foci;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.FocusUpgradeType;
import thaumcraft.api.wands.ItemFocusBasic;

/** Independent native cost policy. Original authority: TileFocalManipulator124-162
 * and ResearchManager466-494 (SUM, never energized-node MAX). */
public final class FocalUpgradeCosts {
    private FocalUpgradeCosts() {}

    public static final class Plan {
        private final Map<Aspect, Integer> amounts;
        private final int rank, total;
        private Plan(AspectList primals, int rank) {
            amounts = Collections.unmodifiableMap(new LinkedHashMap<>(primals.aspects));
            this.rank = rank;
            total = checkedTotal(amounts, false);
        }
        public AspectList aspects() {
            AspectList copy = new AspectList();
            amounts.forEach(copy::add);
            return copy;
        }
        public int initialTotal() { return total; }
        public int total() { return total; }
        public int rank() { return rank; }
        public int xpLevels() { return rank * 8; }
    }

    public static Plan plan(ItemStack focus, FocusUpgradeType type, int rank) {
        if (focus == null || focus.isEmpty() || !(focus.getItem() instanceof ItemFocusBasic)
                || type == null || FocusUpgradeType.get(type.id) != type) throw invalid("Unknown focus/upgrade");
        return plan(type.getAspects(), rank);
    }

    /** Deliberately ignore declared VALUES, including zero: TC4 iterates KEYS. */
    public static Plan plan(AspectList declaredAspects, int rank) {
        if (rank < 1 || rank > 5 || declaredAspects == null || declaredAspects.aspects.isEmpty())
            throw invalid("A paid upgrade needs a rank1..5 and declared aspect keys");
        AspectList weighted = new AspectList();
        int amount = 200 << (rank - 1);
        for (Aspect aspect : declaredAspects.getAspects()) {
            requireCanonical(aspect);
            weighted.add(aspect, amount);
        }
        return new Plan(reduceToPrimals(weighted), rank);
    }

    private record Visit(Aspect aspect, boolean returning) {}

    /** Ordered DAG evaluation without recursion or a depth cap. A shared subgraph
     * is evaluated once, but EACH incoming path contributes its full amount. */
    public static AspectList reduceToPrimals(AspectList input) {
        if (input == null) throw invalid("Missing aspect input");
        var roots = new LinkedHashMap<>(input.aspects);
        checkedTotal(roots, false);
        var values = new IdentityHashMap<Aspect, LinkedHashMap<Aspect, Integer>>();
        var visiting = new IdentityHashMap<Aspect, Boolean>();
        var parents = new IdentityHashMap<Aspect, Aspect[]>();
        var work = new ArrayDeque<Visit>();
        for (Aspect root : roots.keySet()) {
            work.push(new Visit(root, false));
            while (!work.isEmpty()) {
                Visit visit = work.pop();
                Aspect aspect = visit.aspect();
                requireCanonical(aspect);
                if (values.containsKey(aspect)) continue;
                if (visit.returning()) {
                    Aspect[] components = parents.get(aspect);
                    var sum = new LinkedHashMap<Aspect, Integer>();
                    addScaled(sum, values.get(components[0]), 1);
                    addScaled(sum, values.get(components[1]), 1);
                    values.put(aspect, sum);
                    visiting.remove(aspect);
                } else {
                    if (visiting.put(aspect, true) != null) throw invalid("Cyclic aspect graph");
                    Aspect[] components = aspect.getComponents();
                    if (components == null) {
                        if (!aspect.isPrimal()) throw invalid("Inconsistent primal aspect");
                        var leaf = new LinkedHashMap<Aspect, Integer>();
                        leaf.put(aspect, 1);
                        values.put(aspect, leaf);
                        visiting.remove(aspect);
                    } else {
                        if (aspect.isPrimal() || components.length != 2) throw invalid("Invalid compound aspect");
                        requireCanonical(components[0]); requireCanonical(components[1]);
                        parents.put(aspect, components.clone());
                        work.push(new Visit(aspect, true));
                        if (components[0] != components[1]) work.push(new Visit(components[1], false));
                        work.push(new Visit(components[0], false));
                    }
                }
            }
        }
        var result = new LinkedHashMap<Aspect, Integer>();
        roots.forEach((aspect, amount) -> addScaled(result, values.get(aspect), amount));
        checkedTotal(result, false);
        AspectList output = new AspectList();
        result.forEach(output::add);
        return output;
    }

    /** Original positive-only swaps preserve zero barriers and insertion-order
     * ties; sorting with a generic descending comparator changes that quirk. */
    public static Aspect[] drainOrder(AspectList remaining) {
        if (remaining == null) throw invalid("Missing remaining debt");
        checkedTotal(remaining.aspects, true);
        Aspect[] ordered = remaining.getAspects();
        for (int end = ordered.length - 1; end > 0; end--) {
            boolean changed = false;
            for (int i = 0; i < end; i++) {
                int left = remaining.getAmount(ordered[i]), right = remaining.getAmount(ordered[i + 1]);
                if (left > 0 && right > left) {
                    Aspect swap = ordered[i]; ordered[i] = ordered[i + 1]; ordered[i + 1] = swap;
                    changed = true;
                }
            }
            if (!changed) break;
        }
        return ordered;
    }

    private static void addScaled(Map<Aspect, Integer> target, Map<Aspect, Integer> source, int scale) {
        if (source == null || scale <= 0) throw invalid("Incomplete aspect graph");
        for (var entry : source.entrySet()) {
            long amount = (long) entry.getValue() * scale + target.getOrDefault(entry.getKey(), 0);
            if (amount <= 0 || amount > Integer.MAX_VALUE) throw invalid("Unrepresentable primal amount");
            target.put(entry.getKey(), (int) amount);
        }
    }

    private static int checkedTotal(Map<Aspect, Integer> amounts, boolean allowZero) {
        long total = 0;
        for (var entry : amounts.entrySet()) {
            requireCanonical(entry.getKey());
            Integer amount = entry.getValue();
            if (amount == null || amount < 0 || !allowZero && amount == 0) throw invalid("Invalid aspect amount");
            total += amount;
            if (total > Integer.MAX_VALUE) throw invalid("Unrepresentable total vis");
        }
        return (int) total;
    }

    private static void requireCanonical(Aspect aspect) {
        if (aspect == null || aspect.getTag() == null || aspect.getTag().isEmpty()
                || Aspect.getAspect(aspect.getTag()) != aspect) throw invalid("Unknown aspect identity");
    }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
