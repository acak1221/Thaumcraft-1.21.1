package thaumcraft.common.lib.runic;

import java.util.Map;

/** Exact original runic arithmetic; no gameplay clock or native authority lives here. */
public final class RunicRules {
    private RunicRules() {}
    public static int nextHardening(int hardening) { return (byte)(hardening + 1); }
    public static int vis(int finalCharge) { return (int)(32D * Math.pow(2D, finalCharge)); }
    public static Map<String, Integer> aspects(int finalCharge) {
        int amount = vis(finalCharge);
        if (amount <= 0) return Map.of();
        var cost = new java.util.LinkedHashMap<String, Integer>();
        cost.put("tutamen", amount / 2); cost.put("praecantatio", amount / 2); cost.put("potentia", amount);
        return java.util.Collections.unmodifiableMap(cost);
    }
    public static int instability(int finalCharge) { return 5 + finalCharge / 2; }
    public static boolean safeInstability(int instability) { return instability >= 0 && instability <= 24; }
    public static int componentCount(int finalCharge) { return Math.addExact(2, Math.max(0, finalCharge)); }
    public static boolean hasDemand(Map<?, Integer> cost) { return cost.values().stream().anyMatch(amount -> amount > 0); }
    /** Original subtraction has no floor: two charged rings may make a configured500ms interval negative. */
    public static long rechargeInterval(long configured,int charged) { return configured-charged*500L; }
    public record Hit(int charge, float damage) {}
    public static Hit absorb(int charge, float damage) {
        if (charge <= 0 || damage <= 0) return new Hit(charge, damage);
        return (float)charge > damage ? new Hit((int)((float)charge - damage), 0F) : new Hit(0, damage - (float)charge);
    }
}
