package thaumcraft.common.lib.visnet;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Original ResearchManager470-494, merge=true. Deliberately NOT the accepted
 * AspectList.reduceToPrimals helper, whose compound return sums overlapping paths. */
public final class EnergizedNodeReducer {
    private EnergizedNodeReducer() {}

    public static boolean canonical(Aspect aspect) {
        return aspect != null && aspect.getTag() != null && !aspect.getTag().isEmpty()
                && Aspect.getAspect(aspect.getTag()) == aspect;
    }

    /** Refuse the whole input, rather than silently normalizing bad stored aura. */
    public static AspectList checkedCopy(AspectList input) {
        if (input == null) throw new IllegalArgumentException("Missing node aura");
        AspectList copy = new AspectList();
        long total = 0;
        for (var entry : input.aspects.entrySet()) {
            if (!canonical(entry.getKey()) || entry.getValue() == null || entry.getValue() <= 0
                    || (total = Math.addExact(total, (long) entry.getValue())) < 0) throw new IllegalArgumentException("Invalid node aura");
            copy.add(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    private record Visit(Aspect aspect, boolean returning) {}

    /** Iterative, canonical-identity/cycle-checked DAG reduction. No built-in-only
     * whitelist or recursive stack limit; unrepresentable integer paths fail closed. */
    public static AspectList reduceToPrimals(AspectList input) {
        AspectList aura = checkedCopy(input);
        var values = new IdentityHashMap<Aspect, LinkedHashMap<Aspect, Long>>();
        var visiting = new IdentityHashMap<Aspect, Boolean>();
        var components = new IdentityHashMap<Aspect, Aspect[]>();
        var stack = new ArrayDeque<Visit>();
        for (Aspect root : aura.getAspects()) {
            stack.push(new Visit(root, false));
            while (!stack.isEmpty()) {
                Visit visit = stack.pop();
                Aspect aspect = visit.aspect();
                if (!canonical(aspect)) throw new IllegalArgumentException("Unknown aspect component");
                if (values.containsKey(aspect)) continue;
                if (!visit.returning()) {
                    if (visiting.put(aspect, true) != null) throw new IllegalArgumentException("Cyclic aspect components");
                    Aspect[] parents = aspect.getComponents();
                    if (parents == null) {
                        if (!aspect.isPrimal()) throw new IllegalArgumentException("Inconsistent primal aspect");
                        var primal = new LinkedHashMap<Aspect, Long>();
                        primal.put(aspect, 1L);
                        values.put(aspect, primal);
                        visiting.remove(aspect);
                    } else {
                        if (aspect.isPrimal() || parents.length != 2 || !canonical(parents[0]) || !canonical(parents[1]))
                            throw new IllegalArgumentException("Invalid compound aspect");
                        components.put(aspect, parents);
                        stack.push(new Visit(aspect, true));
                        if (parents[0] != parents[1]) stack.push(new Visit(parents[1], false));
                        stack.push(new Visit(parents[0], false));
                    }
                } else {
                    Aspect[] parents = components.get(aspect);
                    var merged = new LinkedHashMap<Aspect, Long>();
                    // Original send.add twice SUMS an identical immediate parent.
                    if (parents[0] == parents[1]) merge(merged, values.get(parents[0]), 2);
                    else { merge(merged, values.get(parents[0]), 1); merge(merged, values.get(parents[1]), 1); }
                    values.put(aspect, merged);
                    visiting.remove(aspect);
                }
            }
        }
        var result = new LinkedHashMap<Aspect, Long>();
        for (Aspect aspect : aura.getAspects()) merge(result, values.get(aspect), aura.getAmount(aspect));
        AspectList out = new AspectList();
        long total = 0;
        for (var entry : result.entrySet()) {
            total = Math.addExact(total, entry.getValue());
            out.add(entry.getKey(), Math.toIntExact(entry.getValue()));
        }
        return out;
    }

    private static void merge(Map<Aspect, Long> target, Map<Aspect, Long> source, int multiplier) {
        if (source == null) throw new IllegalArgumentException("Cyclic or incomplete aspect graph");
        for (var entry : source.entrySet()) {
            long amount = Math.multiplyExact(entry.getValue(), (long) multiplier);
            if (amount <= 0 || amount > Integer.MAX_VALUE) throw new IllegalArgumentException("Unrepresentable aspect path");
            target.merge(entry.getKey(), amount, Math::max);
        }
    }
}
