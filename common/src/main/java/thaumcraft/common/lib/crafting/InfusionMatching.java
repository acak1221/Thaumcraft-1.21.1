package thaumcraft.common.lib.crafting;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;

/** Bounded native infusion matching, not a metadata-to-damage or general NBT adapter. */
public final class InfusionMatching {
    // Explicit original logWood equivalence, NOT every modern tag the items share.
    public static final TagKey<Item> ORIGINAL_LOG_WOOD = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "original_log_wood"));

    private InfusionMatching() {}

    public static boolean matches(Ingredient required, ItemStack offered) {
        if (offered.isEmpty()) return false;
        if (required.test(offered)) return true;
        // Also retain the original declared-silverwood-stack representation:
        // either that stack OR an explicit logWood ingredient is ore-fuzzy.
        if (offered.is(ORIGINAL_LOG_WOOD)) {
            for (ItemStack declared : required.getItems()) if (declared.is(ORIGINAL_LOG_WOOD)) return true;
        }
        return false;
    }

    /** Declared component wear is independent of the central retention rule below. */
    public static boolean matches(Ingredient required, Optional<Integer> damage, ItemStack offered) {
        return matches(required, offered) && (damage.isEmpty() || damage.get() >= 0 && offered.getDamageValue() == damage.get());
    }

    /**
     * Matrix:339-345 / InventoryUtils:283-310: ore match precedes the NBT
     * subset check, and damageable-item wear is ignored. Count is not equality.
     * Plain recipe inputs may carry arbitrary additional native components.
     */
    public static boolean centralStillMatches(ItemStack current, ItemStack snapshot) {
        if (current.isEmpty() || snapshot.isEmpty()) return false;
        if (current.is(ORIGINAL_LOG_WOOD) && snapshot.is(ORIGINAL_LOG_WOOD)) return true;
        if (!ItemStack.isSameItem(current, snapshot) || current.getCount() > current.getMaxStackSize()) return false;
        boolean ignoreWear = current.isDamageableItem() && snapshot.isDamageableItem();
        for (var entry : snapshot.getComponentsPatch().entrySet()) {
            if (ignoreWear && entry.getKey() == DataComponents.DAMAGE) continue;
            Object expected = entry.getValue().orElse(null);
            Object actual = current.get(entry.getKey());
            if (entry.getKey() == DataComponents.CUSTOM_DATA && expected instanceof CustomData expectedData) {
                if (!(actual instanceof CustomData actualData) || !containsTopLevel(actualData.copyTag(), expectedData.copyTag())) return false;
            } else if (!Objects.equals(expected, actual)) return false;
        }
        return true;
    }

    private static boolean containsTopLevel(CompoundTag actual, CompoundTag expected) {
        // ThaumcraftApiHelper:128-145 compares whole values for each required
        // top-level key. Do not replace that with recursive fuzzy NBT matching.
        for (String key : expected.getAllKeys()) if (!Objects.equals(actual.get(key), expected.get(key))) return false;
        return true;
    }
}
