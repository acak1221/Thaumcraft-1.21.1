package thaumcraft.common.lib.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

/**
 * Original EnchantmentWandFortune (oracle thaumcraft/common/lib/enchantment/EnchantmentWandFortune.java).
 *
 * 1.21.1 registers no enchantment in code, so the definition is
 * data/thaumcraft/enchantment/wandfortune.json (max level 3, minimum cost 15 + 9 per level above the
 * first, maximum cost 61 + 10 per level above the first) and this class owns the identity and the
 * level lookup.
 *
 * Original values: minimum cost 15 + 9 * (level - 1) (line 25-27), maximum cost the 1.7.10 vanilla
 * minimum cost plus 50 (line 29-31), maximum level 3 (line 33-35), applicability a wand focus that
 * accepts this enchantment or a book (line 37-39).
 *
 * The original name is "wandfortune" (line 22); the audited 1.12 cross-check spells the registry
 * name "wand_fortune", the oracle wins.
 */
public final class EnchantmentWandFortune {
    /** The original enchantment name "wandfortune" (EnchantmentWandFortune:22). */
    public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "wandfortune"));
    public static final int MAX_LEVEL = WandEnchantmentRules.WAND_FORTUNE_MAX_LEVEL;
    public static final int MIN_COST_BASE = WandEnchantmentRules.WAND_FORTUNE_MIN_COST_BASE;

    private EnchantmentWandFortune() {}

    /** The wand fortune level of a stack, zero when the stack carries none. */
    public static int level(ItemStack stack, Level world) {
        return EquipmentEnchantments.level(stack, world, KEY);
    }

    /** Original EnchantmentWandFortune:25-27. */
    public static int minCost(int level) { return WandEnchantmentRules.wandFortuneCost(level, MIN_COST_BASE); }

    /** Original EnchantmentWandFortune:29-31. */
    public static int maxCost(int level) { return WandEnchantmentRules.maxCost(level); }

    /**
     * Original EnchantmentWandFortune:37-39: a wand focus that accepts this enchantment, or a book.
     *
     * No TC4 4.2.3.5 focus implements {@code IWandFocus.acceptsEnchant}, so only the book branch is
     * reachable; the datapack tag thaumcraft:enchantable/wandfortune lists the two book items.
     */
    public static boolean canEnchant(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
    }

    /**
     * The extra drops this enchantment grants. The original never reads the enchantment level: the
     * treasure focus UPGRADE hands the vanilla fortune level to the block loot (EventHandlerWorld:
     * 178-182), so this enchantment grants no extra drop.
     */
    public static int fortuneExtraDrops(int level) {
        return WandEnchantmentRules.fortuneExtraDrops(level);
    }
}
