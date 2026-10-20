package thaumcraft.common.lib.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import thaumcraft.api.wands.ItemFocusBasic;

/**
 * Original EnchantmentFrugal (oracle thaumcraft/common/lib/enchantment/EnchantmentFrugal.java).
 *
 * 1.21.1 registers no enchantment in code: {@code Enchantment} is a datapack record, so the
 * definition is data/thaumcraft/enchantment/frugal.json (max level 3, minimum cost 5 + 11 per
 * level above the first, maximum cost 61 + 10 per level above the first) and this class owns the
 * identity, the level lookup and the original per-level rules.
 *
 * Original values: minimum cost 5 + 11 * (level - 1) (line 27-29), maximum cost the 1.7.10
 * vanilla minimum cost plus 50 (line 31-33), maximum level 3 (line 35-37), applicability a wand
 * focus that accepts this enchantment or a book (line 39-41).
 */
public final class EnchantmentFrugal {
    /** The original enchantment name "frugal" (EnchantmentFrugal:24). */
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "frugal"));
    public static final int MAX_LEVEL = WandEnchantmentRules.FRUGAL_MAX_LEVEL;
    public static final int MIN_COST_BASE = WandEnchantmentRules.FRUGAL_MIN_COST_BASE;

    private EnchantmentFrugal() {}

    /** The frugal level of a stack, zero when the stack carries none. */
    public static int level(ItemStack stack, Level world) {
        return EquipmentEnchantments.level(stack, world, KEY);
    }

    /** Original EnchantmentFrugal:27-29. */
    public static int minCost(int level) { return WandEnchantmentRules.frugalCost(level, MIN_COST_BASE); }

    /** Original EnchantmentFrugal:31-33. */
    public static int maxCost(int level) { return WandEnchantmentRules.maxCost(level); }

    /**
     * Original EnchantmentFrugal:39-41: a wand focus that accepts this enchantment, or a book.
     *
     * The focus branch needs a focus that ACCEPTS the enchantment. No TC4 4.2.3.5 focus implements
     * {@code IWandFocus.acceptsEnchant} - the interface has no implementation in the oracle - so only
     * the book branch is reachable, exactly as in the original. The datapack tag
     * thaumcraft:enchantable/frugal therefore lists the two book items.
     */
    public static boolean canEnchant(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    /**
     * Original EnchantmentFrugal:47-50: a frugal focus keeps its enchantments only when it escapes
     * the damage roll. The chance is {@code 1 - level / 5} and only a wand focus can be damaged.
     */
    public static boolean doDamage(ItemStack stack, int level, RandomSource random) {
        float chance = WandEnchantmentRules.frugalDamageChance(level);
        return stack.getItem() instanceof ItemFocusBasic && random.nextFloat() < chance;
    }
}
