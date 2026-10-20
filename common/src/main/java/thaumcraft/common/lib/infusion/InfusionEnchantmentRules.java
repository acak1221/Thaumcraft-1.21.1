package thaumcraft.common.lib.infusion;

import java.util.List;

/** Original InfusionEnchantmentRecipe arithmetic; the XP unit is a LEVEL, not an experience point. */
public final class InfusionEnchantmentRules {
    private InfusionEnchantmentRules() {}
    public static int instability(int base, int totalLevels) {
        if (base < 0 || totalLevels < 0) throw new IllegalArgumentException("Negative enchantment levels");
        return Math.addExact(base, totalLevels / 2);
    }
    public static int xpLevels(int firstLevelMinimumCost, int currentTargetLevel) {
        if (firstLevelMinimumCost < 0 || currentTargetLevel < 0) throw new IllegalArgumentException("Negative enchantment cost");
        return Math.multiplyExact(Math.max(1, firstLevelMinimumCost / 3), Math.addExact(1, currentTargetLevel));
    }
    public static float essentiaModifier(int currentTargetLevel, List<Integer> otherLevels) {
        if (currentTargetLevel < 0 || otherLevels.stream().anyMatch(level -> level < 0)) throw new IllegalArgumentException("Negative enchantment levels");
        float modifier = currentTargetLevel;
        for (int level : otherLevels) modifier += level * .1F;
        return modifier;
    }
    public static int aspectCost(int base, float modifier) {
        if (base <= 0) throw new IllegalArgumentException("Nonpositive aspect cost");
        if (!Float.isFinite(modifier) || modifier < 0) throw new IllegalArgumentException("Invalid essentia modifier");
        return Math.addExact(base, (int) (base * modifier));
    }
}
