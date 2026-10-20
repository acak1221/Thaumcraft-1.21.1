package thaumcraft.common.lib.research;

import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Closed bindings for naturally available original item triggers. Missing late content remains explicitly unbound. */
public final class ResearchTriggerBindings {
    private static final Map<String, Set<String>> ITEMS = Map.of(
            "BONEBOW", Set.of("minecraft:bow", "minecraft:bone"),
            "JARBRAIN", Set.of("thaumcraft:quick_silver"),
            "MIRROR", Set.of("minecraft:ender_pearl"));
    private static final Set<String> CLOTHING = Set.of("TINYBOWTIE", "TINYFEZ", "TINYGLASSES", "TINYHAT");
    private ResearchTriggerBindings() {}

    public static boolean matches(String researchKey, ResearchClues.Context context) {
        var research = ResearchCatalog.get(researchKey);
        if (research == null) return false;
        if (context.entityName() != null) return research.entityTriggers().contains(context.entityName());
        ItemStack stack = context.item();
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        boolean identity = ITEMS.getOrDefault(researchKey, Set.of()).contains(id.toString())
                || CLOTHING.contains(researchKey) && "minecraft".equals(id.getNamespace()) && id.getPath().endsWith("_wool");
        if (!identity) return false;
        // ConfigItems registers itemResource:3 as nuggetQuicksilver. Its original OreDictionary shortcut
        // accepts the native quicksilver identity before NBT comparison; zombie brains are not this trigger.
        if ("JARBRAIN".equals(researchKey) && id.equals(ResourceLocation.fromNamespaceAndPath("thaumcraft", "quick_silver"))) return true;
        // Original ignoreDamage=true still compares NBT unless an OreDictionary shortcut succeeds.
        // Damage moved from metadata to a native component; remove that component's varying value only for durable items.
        ItemStack comparable = stack.copyWithCount(1);
        if (comparable.isDamageableItem()) comparable.setDamageValue(0);
        return ItemStack.isSameItemSameComponents(new ItemStack(stack.getItem()), comparable);
    }
}
