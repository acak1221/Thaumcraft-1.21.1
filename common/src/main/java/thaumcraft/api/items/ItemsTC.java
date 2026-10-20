package thaumcraft.api.items;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ThaumcraftContent;

/**
 * The public item registry of the native port.
 *
 * <p>Forced 1.21.1 change: the original TC4 kept its items in {@code ConfigItems} and the original add-on
 * surface kept them in {@code ItemsTC}; 1.21.1 registers every item in the item registry, so this class
 * resolves the native registry instead of holding a second copy of it. A plain name means
 * {@code thaumcraft:<name>}.</p>
 */
public final class ItemsTC {
    private ItemsTC() {}

    /** The native registry identifier of a name. */
    public static ResourceLocation id(String name) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("An item needs a name");
        return name.contains(":") ? ResourceLocation.tryParse(name) : ResourceLocation.fromNamespaceAndPath("thaumcraft", name);
    }

    /** The registered item of a name, or null when it does not exist in this runtime. */
    public static Item get(String name) {
        ResourceLocation id = id(name);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
        return BuiltInRegistries.ITEM.get(id);
    }

    /** The registered item of a name; a missing item is an error. */
    public static Item require(String name) {
        Item item = get(name);
        if (item == null) throw new IllegalArgumentException("Unknown item: " + name);
        return item;
    }

    /** A stack of the named item. */
    public static ItemStack stack(String name) { return stack(name, 1); }

    /** A stack of the named item. */
    public static ItemStack stack(String name, int count) {
        return new ItemStack(require(name), count);
    }

    /** True when the name is a registered item. */
    public static boolean isRegistered(String name) { return get(name) != null; }

    /** Every registered item of this mod. */
    public static Set<String> names() {
        Set<String> result = new LinkedHashSet<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            if ("thaumcraft".equals(id.getNamespace())) result.add(id.getPath());
        }
        return result;
    }

    /** One of the native material items, such as the thaumium ingot or the nitor. */
    public static Item material(String name) {
        var supplier = ThaumcraftContent.MATERIALS.get(name);
        if (supplier == null) throw new IllegalArgumentException("Unknown Thaumcraft material: " + name);
        return supplier.get();
    }

    /** One of the native aspect shards. */
    public static Item shard(String aspectTag) {
        var supplier = ThaumcraftContent.SHARDS.get(aspectTag);
        if (supplier == null) throw new IllegalArgumentException("Unknown aspect shard: " + aspectTag);
        return supplier.get();
    }

    /** The native thaumometer. */
    public static Item thaumometer() { return ThaumcraftContent.THAUMOMETER.get(); }

    /** The native thaumonomicon. */
    public static Item thaumonomicon() { return ThaumcraftContent.THAUMONOMICON.get(); }

    /** The native wand. */
    public static Item wand() { return ThaumcraftContent.WAND.get(); }

    /** The native inkwell. */
    public static Item inkwell() { return ThaumcraftContent.INKWELL.get(); }

    /** The native empty phial. */
    public static Item emptyPhial() { return ThaumcraftContent.EMPTY_PHIAL.get(); }

    /** The native filled phial. */
    public static Item filledPhial() { return ThaumcraftContent.FILLED_PHIAL.get(); }

    /** The native crystal essence. */
    public static Item crystalEssence() { return ThaumcraftContent.CRYSTAL_ESSENCE.get(); }

    /** The native research note. */
    public static Item researchNote() { return ThaumcraftContent.RESEARCH_NOTE.get(); }

    /** The native primordial pearl. */
    public static Item primordialPearl() { return ThaumcraftContent.PRIMORDIAL_PEARL.get(); }

    /** The native iron wand cap. */
    public static Item ironWandCap() { return ThaumcraftContent.IRON_WAND_CAP.get(); }
}
