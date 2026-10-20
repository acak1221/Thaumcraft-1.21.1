package thaumcraft.common.lib.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

/**
 * Original EnchantmentPotency (oracle thaumcraft/common/lib/enchantment/EnchantmentPotency.java).
 *
 * 1.21.1 registers no enchantment in code, so the definition is
 * data/thaumcraft/enchantment/potency.json (max level 3, minimum cost 10 + 11 per level above the
 * first, maximum cost 61 + 10 per level above the first) and this class owns the identity and the
 * level lookup.
 *
 * Original values: minimum cost 10 + 11 * (level - 1) (line 25-27), maximum cost the 1.7.10
 * vanilla minimum cost plus 50 (line 29-31), maximum level 3 (line 33-35), applicability a wand
 * focus that accepts this enchantment or a book (line 37-39).
 */
public final class EnchantmentPotency {
    /** The original enchantment name "potency" (EnchantmentPotency:22). */
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "potency"));
    public static final int MAX_LEVEL = WandEnchantmentRules.POTENCY_MAX_LEVEL;
    public static final int MIN_COST_BASE = WandEnchantmentRules.POTENCY_MIN_COST_BASE;

    private EnchantmentPotency() {}

    /** The potency level of a stack, zero when the stack carries none. */
    public static int level(ItemStack stack, Level world) {
        return EquipmentEnchantments.level(stack, world, KEY);
    }

    /** Original EnchantmentPotency:25-27. */
    public static int minCost(int level) { return WandEnchantmentRules.potencyCost(level, MIN_COST_BASE); }

    /** Original EnchantmentPotency:29-31. */
    public static int maxCost(int level) { return WandEnchantmentRules.maxCost(level); }

    /**
     * Original EnchantmentPotency:37-39: a wand focus that accepts this enchantment, or a book.
     *
     * No TC4 4.2.3.5 focus implements {@code IWandFocus.acceptsEnchant}, so only the book branch is
     * reachable; the datapack tag thaumcraft:enchantable/potency lists the two book items.
     */
    public static boolean canEnchant(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    /**
     * The multiplier this enchantment contributes to a focus. The original never reads the enchantment
     * level: only the potency focus UPGRADE raises a focus power (ItemFocusFire:95 and the sibling
     * foci), so the enchantment contributes exactly one.
     */
    public static float potencyMultiplier(int level) {
        return WandEnchantmentRules.potencyMultiplier(level);
    }
}
