package thaumcraft.common.blocks;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;

/**
 * Original BlockLootItem: the loot container item. Its rarity follows the
 * rarity of the container, which the item keeps in its custom data.
 */
public final class BlockLootItem extends BlockItem {
    public BlockLootItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** Original BlockLootItem:22-30 - the rarity of the container. */
    public static Rarity rarityOf(ItemStack stack) {
        Integer rarity = rarity(stack);
        if (rarity == null) return Rarity.COMMON;
        return switch (rarity) {
            case 1 -> Rarity.UNCOMMON;
            case 2 -> Rarity.RARE;
            default -> Rarity.COMMON;
        };
    }

    /** The rarity is stored in the custom data of the item stack. */
    public static Integer rarity(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        return tag.contains("rarity") ? tag.getInt("rarity") : null;
    }

    /** Original BlockLootItem:32-37 - the rarity is shown in the tooltip. */
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Rarity rarity = rarityOf(stack);
        if (rarity == Rarity.COMMON) return;
        tooltip.add(net.minecraft.network.chat.Component.translatable("item.minecraft.rarity." + rarity.getSerializedName())
                .withStyle(rarity.color()));
    }

    /** Stores the rarity of the container in the item stack. */
    public static void setRarity(ItemStack stack, int rarity) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        tag.putInt("rarity", rarity);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
