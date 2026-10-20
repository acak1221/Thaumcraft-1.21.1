package thaumcraft.common.lib.enchantment;

/** Original EventHandlerEntity repair and movement arithmetic, independent of native entity state. */
public final class EquipmentEnchantmentRules {
    private EquipmentEnchantmentRules() {}

    public static int repairCentivis(int primalAmount, int enchantmentLevel) {
        if (primalAmount <= 0 || enchantmentLevel <= 0) throw new IllegalArgumentException("Nonpositive repair input");
        return Math.multiplyExact((int)Math.sqrt(Math.multiplyExact(primalAmount, 2)), Math.min(2, enchantmentLevel));
    }

    public static int repairPrice(int centivis, float craftingModifier) {
        if (centivis < 0 || !Float.isFinite(craftingModifier) || craftingModifier < .1F)
            throw new IllegalArgumentException("Invalid repair price");
        // Original consumeAllVis receives the tiny raw cost directly: NEVER multiply by100 here.
        return (int)(centivis * craftingModifier);
    }

    public static float hasteAcceleration(int enchantmentLevel, boolean hasImpulse, boolean inWater) {
        if (enchantmentLevel < 0) throw new IllegalArgumentException("Negative haste level");
        float bonus = enchantmentLevel * .015F;
        if (hasImpulse) bonus /= 2F;
        if (inWater) bonus /= 2F;
        return bonus;
    }
}
