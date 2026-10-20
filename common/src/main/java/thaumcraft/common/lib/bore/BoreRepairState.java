package thaumcraft.common.lib.bore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Original transient Bore repair counter, MAX cost and accumulated raw primal vis; never a disk recipe or public write capability. */
public record BoreRepairState(long counter, Map<String, Integer> cost, Map<String, Integer> vis) {
    private static final List<String> PRIMALS = List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio");
    public static final BoreRepairState EMPTY = new BoreRepairState(0, Map.of(), Map.of());
    public BoreRepairState {
        if (counter < 0) throw new IllegalArgumentException("Negative repair counter");
        cost = checked(cost, true); vis = checked(vis, false);
    }
    private static Map<String, Integer> checked(Map<String, Integer> source, boolean positive) {
        var result = new LinkedHashMap<String, Integer>();
        source.forEach((tag, amount) -> {
            if (!PRIMALS.contains(tag) || amount == null || amount < (positive ? 1 : 0)) throw new IllegalArgumentException("Invalid repair primal amount");
            result.put(tag, amount);
        });
        return Collections.unmodifiableMap(result);
    }
    public boolean isAttemptTick() { return counter % 40 == 0; }
    public boolean isFillTick() { return counter % 5 == 0; }
    public BoreRepairState advance() { return new BoreRepairState(Math.incrementExact(counter), cost, vis); }
    public BoreRepairState mergeCost(Map<String, Integer> incoming) {
        var next = new LinkedHashMap<>(cost);
        checked(incoming, true).forEach((tag, amount) -> next.merge(tag, amount, Math::max));
        return new BoreRepairState(counter, next, vis);
    }
    public BoreRepairState clearCost() { return new BoreRepairState(counter, Map.of(), vis); }
    public int available(String tag) { return vis.getOrDefault(tag, 0); }
    public boolean canPay() { return !cost.isEmpty() && cost.entrySet().stream().allMatch(e -> available(e.getKey()) >= e.getValue()); }
    public BoreRepairState paid() {
        if (!canPay()) throw new IllegalStateException("Incomplete Bore repair vis");
        var next = new LinkedHashMap<>(vis);
        cost.forEach((tag, amount) -> next.put(tag, available(tag) - amount));
        return new BoreRepairState(counter, cost, next);
    }
    public BoreRepairState addVis(String tag, int amount) {
        if (!PRIMALS.contains(tag) || amount < 0) throw new IllegalArgumentException("Invalid raw repair grant");
        var next = new LinkedHashMap<>(vis); next.put(tag, Math.addExact(available(tag), amount));
        return new BoreRepairState(counter, cost, next);
    }
}
