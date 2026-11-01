package thaumcraft.common.lib.research;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Stable scan keys for the item subtypes which 1.21.1 stores in components instead of item metadata. */
public final class ItemScanIdentity {
    private ItemScanIdentity() {}

    /**
     * Original ScanManager.generateItemHash keeps a potion's subtype metadata. Native named potion types are its
     * counterpart. Names, colour and custom effects are NBT in the original and do not create another scan identity.
     * Keep the existing plain item key for water/unspecified potions and every other item, so saved ordinary scans and
     * the former undifferentiated potion scan remain useful without hiding all the other potion types.
     */
    public static String signature(ItemStack stack) {
        String key = "@" + BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!(stack.getItem() instanceof PotionItem)) return key;
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents.potion().isEmpty() || contents.potion().get().is(Potions.WATER)) return key;
        return contents.potion().get().unwrapKey()
                .map(type -> key + "[potion=" + type.location() + "]").orElse(key);
    }

    /** Original type-one item/damage session shape, with native potion subtype metadata kept in its item identity. */
    public static String itemSessionKey(ItemStack stack, int metadata) {
        return "item:" + signature(stack).substring(1) + ":" + metadata;
    }

    /** Original type-two entity identity; named potion subtypes also distinguish a replaced bottle's contents. */
    public static String entitySessionKey(int entityId, ItemStack stack) {
        String signature = signature(stack);
        String plain = "@" + BuiltInRegistries.ITEM.getKey(stack.getItem());
        return "entity:" + entityId + (signature.equals(plain) ? "" : ":" + signature.substring(1));
    }
}
