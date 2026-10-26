package thaumcraft.common.lib.infusion;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import thaumcraft.common.config.ThaumcraftTweaks;

/**
 * Tweak (TC4Tweaks, "infusion NBT carry-over"): infusing armour or a tool keeps the data of the
 * centre item on the result.
 *
 * <p>In TC4 4.2.3.5 the result of an infusion is the recipe's own stack, so an enchanted or named
 * centre item lost its enchantments and name. With the setting on, every component the centre item
 * changes is copied onto the result, unless the recipe's result changes that component itself: the
 * recipe always wins. Only armour and tools on both sides take part, as in TC4Tweaks' default. The
 * components that make an item what it is (its durability limit, stack size and similar) are never
 * copied, and a carried-over wear never breaks the result.</p>
 */
public final class InfusionItemData {
    /** Components that describe the kind of item rather than this one item. */
    private static final Set<DataComponentType<?>> KIND = Set.of(
            DataComponents.MAX_DAMAGE, DataComponents.MAX_STACK_SIZE, DataComponents.UNBREAKABLE,
            DataComponents.ATTRIBUTE_MODIFIERS, DataComponents.TOOL, DataComponents.FOOD,
            DataComponents.RARITY, DataComponents.CREATIVE_SLOT_LOCK, DataComponents.INTANGIBLE_PROJECTILE,
            DataComponents.FIRE_RESISTANT, DataComponents.JUKEBOX_PLAYABLE, DataComponents.ITEM_NAME);

    private InfusionItemData() {}

    /** The result with the centre item's data, or the result itself when the setting is off. */
    public static ItemStack apply(ItemStack centre, ItemStack result) {
        if (!ThaumcraftTweaks.infusionKeepsData() || centre.isEmpty() || result.isEmpty()
                || !armourOrTool(centre) || !armourOrTool(result)) return result;
        ItemStack kept = result.copy();
        var own = result.getComponentsPatch();
        for (Map.Entry<DataComponentType<?>, Optional<?>> change : centre.getComponentsPatch().entrySet()) {
            DataComponentType<?> type = change.getKey();
            if (change.getValue().isEmpty() || KIND.contains(type) || own.get(type) != null) continue;
            copy(kept, type, change.getValue().get());
        }
        if (kept.isDamageableItem() && kept.getDamageValue() >= kept.getMaxDamage())
            kept.setDamageValue(Math.max(0, kept.getMaxDamage() - 1));
        return kept;
    }

    /** Armour or a tool: TC4Tweaks took ItemArmor or an item with a tool class. */
    public static boolean armourOrTool(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem || stack.getItem() instanceof TieredItem || stack.has(DataComponents.TOOL);
    }

    @SuppressWarnings("unchecked")
    private static <T> void copy(ItemStack stack, DataComponentType<T> type, Object value) {
        stack.set(type, (T) value);
    }
}
