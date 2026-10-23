package thaumcraft.api;

import java.lang.reflect.Field;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The original TC4 reflective item/block lookup.
 *
 * <p>Forced 1.21.1 change: the original reflected over {@code thaumcraft.common.config.ConfigItems} and
 * {@code ConfigBlocks}. The native port has no such classes, so the lookup is resolved through the live
 * registries first (a plain name means {@code thaumcraft:<name>}) and only then falls back to the original
 * reflective field names for an add-on that still publishes them.</p>
 */
public class ItemApi {
    private static final Logger LOG = LoggerFactory.getLogger("Thaumcraft");
    private static final String ITEM_CLASS = "thaumcraft.common.config.ConfigItems";
    private static final String BLOCK_CLASS = "thaumcraft.common.config.ConfigBlocks";

    private ItemApi() {}

    /** Returns a fresh named stack, or null for null/empty/missing names (failed lookups may log).
     * In 1.7.10 meta selected subtypes; now only damageable stacks receive wear in damage points.
     * Use native namespaced item IDs and data components for 1.21.1 variants, not subtype numbers. */
    public static ItemStack getItem(String itemString, int meta) {
        if (itemString == null || itemString.isEmpty()) return null;
        ResourceLocation id = id(itemString);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) return stack(BuiltInRegistries.ITEM.get(id), meta);
        Object reflected = reflect(ITEM_CLASS, itemString);
        if (reflected instanceof Item reflectedItem) return stack(reflectedItem, meta);
        if (reflected instanceof ItemStack stack) return stack.copy();
        LOG.warn("[Thaumcraft] Could not retrieve item identified by: {}", itemString);
        return null;
    }

    /** Returns a fresh named block-item stack, or null for null/empty/missing names (failed lookups may log).
     * In 1.7.10 meta selected block variants; now it only applies wear to damageable item forms.
     * Use native block IDs and BlockState properties for 1.21.1 block variants. */
    public static ItemStack getBlock(String itemString, int meta) {
        if (itemString == null || itemString.isEmpty()) return null;
        ResourceLocation id = id(itemString);
        if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) return stack(BuiltInRegistries.BLOCK.get(id), meta);
        Object reflected = reflect(BLOCK_CLASS, itemString);
        if (reflected instanceof Block reflectedBlock) return stack(reflectedBlock, meta);
        if (reflected instanceof ItemStack stack) return stack.copy();
        LOG.warn("[Thaumcraft] Could not retrieve block identified by: {}", itemString);
        return null;
    }

    /**
     * The original metadata is the native damage value.
     *
     * <p>Forced 1.21.1 change: 1.21.1 has no item metadata; the original metadata only survives on a
     * damageable item, and it is the damage value there.</p>
     */
    private static ItemStack stack(ItemLike item, int meta) {
        ItemStack stack = new ItemStack(item, 1);
        if (meta != 0 && stack.isDamageableItem()) stack.setDamageValue(Math.min(meta, stack.getMaxDamage() - 1));
        return stack;
    }

    /** The block item of a named block, when the block itself has one. */
    public static ItemStack getBlockItem(String itemString) {
        if (itemString == null || itemString.isEmpty()) return null;
        ResourceLocation id = id(itemString);
        Block block = id == null ? null : BuiltInRegistries.BLOCK.get(id);
        if (block == null) return null;
        Item item = block.asItem();
        return item instanceof BlockItem ? new ItemStack(item) : null;
    }

    private static ResourceLocation id(String name) {
        return name.contains(":") ? ResourceLocation.tryParse(name) : ResourceLocation.fromNamespaceAndPath("thaumcraft", name);
    }

    private static Object reflect(String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className);
            Field field = type.getField(fieldName);
            return field.get(null);
        } catch (ReflectiveOperationException | RuntimeException absent) {
            return null;
        }
    }
}
