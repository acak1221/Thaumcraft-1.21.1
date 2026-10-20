package thaumcraft.common.lib.enchantment;

/**
 * Pure arithmetic of the original TC4 wand enchantments and of the wand sceptre.
 *
 * Oracle (Thaumcraft 4.2.3.5, decompiled):
 * - thaumcraft/common/lib/enchantment/EnchantmentFrugal.java
 * - thaumcraft/common/lib/enchantment/EnchantmentPotency.java
 * - thaumcraft/common/lib/enchantment/EnchantmentWandFortune.java
 * - thaumcraft/common/lib/crafting/ArcaneSceptreRecipe.java
 *
 * 1.21.1 has no code-registered enchantments: {@code net.minecraft.world.item.enchantment.Enchantment}
 * is a datapack record, so the definitions live in data/thaumcraft/enchantment/*.json and every
 * number the game reads is produced here.
 *
 * Fidelity note: in 4.2.3.5 only Haste and Repair are instantiated (Config.java:274-281) and no focus
 * implements {@code IWandFocus.acceptsEnchant}, so the frugal/potency/wandfortune enchantments never
 * reach a focus and never change a number in the original. Their canonical definition is still ported
 * (max level, cost progression, applicability), and the two inert per-level accessors below state that
 * emptiness instead of inventing a gameplay bonus. The real per-level frugal/potency/treasure effects
 * belong to the focus UPGRADES (FocusUpgradeType), not to these enchantments.
 */
public final class WandEnchantmentRules {
    private WandEnchantmentRules() {}

    /** Original EnchantmentFrugal:35-37, EnchantmentPotency:33-35, EnchantmentWandFortune:33-35. */
    public static final int FRUGAL_MAX_LEVEL = 3;
    public static final int POTENCY_MAX_LEVEL = 3;
    public static final int WAND_FORTUNE_MAX_LEVEL = 3;

    /** Original EnchantmentFrugal:27-29 (5 + 11 * (level - 1)). */
    public static final int FRUGAL_MIN_COST_BASE = 5;
    /** Original EnchantmentPotency:25-27 (10 + 11 * (level - 1)). */
    public static final int POTENCY_MIN_COST_BASE = 10;
    /** Original EnchantmentWandFortune:25-27 (15 + 9 * (level - 1)). */
    public static final int WAND_FORTUNE_MIN_COST_BASE = 15;
    /** The cost step of frugal and potency. */
    public static final int COST_STEP = 11;
    /** The cost step of the wand fortune enchantment. */
    public static final int WAND_FORTUNE_COST_STEP = 9;

    /**
     * The 1.7.10 vanilla minimum cost progression the original maximum cost is built on:
     * 11 + 10 * (level - 1). The original adds a flat margin to it, so the maximum cost is
     * 61 / 71 / 81 for levels 1..3 - the same progression the already ported
     * thaumcraft:haste and thaumcraft:repair definitions use.
     */
    public static final int VANILLA_MIN_COST_BASE = 11;
    public static final int VANILLA_MIN_COST_STEP = 10;
    /** Original EnchantmentFrugal:31-33 and its two siblings: the vanilla minimum cost plus 50. */
    public static final int MAX_COST_MARGIN = 50;

    /** Original EnchantmentFrugal:27-29: the frugal minimum cost of one level. */
    public static int frugalCost(int level, int base) { return cost(level, base, COST_STEP); }

    /** Original EnchantmentPotency:25-27: the potency minimum cost of one level. */
    public static int potencyCost(int level, int base) { return cost(level, base, COST_STEP); }

    /** Original EnchantmentWandFortune:25-27: the wand fortune minimum cost of one level. */
    public static int wandFortuneCost(int level, int base) { return cost(level, base, WAND_FORTUNE_COST_STEP); }

    private static int cost(int level, int base, int step) {
        if (level < 1) throw new IllegalArgumentException("Enchantment level must be positive: " + level);
        if (base < 0) throw new IllegalArgumentException("Negative enchantment cost base: " + base);
        return base + step * (level - 1);
    }

    /** The original maximum cost of one level: the vanilla minimum progression plus the fixed margin. */
    public static int maxCost(int level) {
        if (level < 1) throw new IllegalArgumentException("Enchantment level must be positive: " + level);
        return VANILLA_MIN_COST_BASE + VANILLA_MIN_COST_STEP * (level - 1) + MAX_COST_MARGIN;
    }

    /**
     * Original EnchantmentFrugal:47-50: the chance that a frugal focus still takes damage.
     * {@code chance = 1 - level / 5}, so level 1 keeps four fifths of the damage.
     */
    public static float frugalDamageChance(int level) {
        if (level < 1) throw new IllegalArgumentException("Enchantment level must be positive: " + level);
        return 1F - (float) level / 5F;
    }

    /**
     * The original enchantment adds no potency to a focus: only the potency focus UPGRADE does
     * (FocusUpgradeType.potency, ItemFocusFire:95 and the sibling foci). The multiplier this
     * enchantment contributes is therefore exactly one at every level.
     */
    public static float potencyMultiplier(int level) {
        if (level < 1) throw new IllegalArgumentException("Enchantment level must be positive: " + level);
        return 1F;
    }

    /**
     * The original enchantment adds no drops: the treasure focus UPGRADE hands the vanilla fortune
     * level to the block loot (EventHandlerWorld:178-182), not this enchantment. The extra drop
     * count of this enchantment is therefore zero at every level.
     */
    public static int fortuneExtraDrops(int level) {
        if (level < 1) throw new IllegalArgumentException("Enchantment level must be positive: " + level);
        return 0;
    }

    /**
     * Original ArcaneSceptreRecipe:64 and :99: the sceptre costs fifty percent more than the plain
     * wand of the same materials: {@code (int) ((float) (capCost * rodCost) * 1.5F)}.
     */
    public static int sceptreCost(int capCraftCost, int rodCraftCost) {
        if (capCraftCost < 0 || rodCraftCost < 0) throw new IllegalArgumentException("Negative wand craft cost");
        return (int) ((float) (capCraftCost * rodCraftCost) * 1.5F);
    }
}
