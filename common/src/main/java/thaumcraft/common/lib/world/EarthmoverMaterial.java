package thaumcraft.common.lib.world;

import java.util.ArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import thaumcraft.common.blocks.ItemVariantBlock;

/** Original item/metadata payment, accounting for native states made from several placement items. */
public final class EarthmoverMaterial {
    private EarthmoverMaterial() {}

    /** Original block metadata never carried a second, unpaid water source. Keep its material and orientation. */
    public static BlockState placementState(BlockState source) {
        return source.hasProperty(BlockStateProperties.WATERLOGGED)
                ? source.setValue(BlockStateProperties.WATERLOGGED, false) : source;
    }

    /** A native double slab contains two slabs; layered and clustered states contain their displayed count. */
    public static int units(BlockState state) {
        if (state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) return 2;
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty count && switch (property.getName()) {
                case "layers", "candles", "pickles", "eggs", "flower_amount" -> true;
                default -> false;
            }) return Math.max(1, state.getValue(count));
        }
        return 1;
    }

    /** ItemVariantBlock and explicit native item-state options preserve the original material metadata. */
    public static boolean matches(ItemStack stack, BlockState source) {
        if (stack.isEmpty()) return false;
        BlockState supplied;
        if (stack.getItem() instanceof BlockItem blockItem) {
            if (blockItem.getBlock() != source.getBlock()) return false;
            supplied = blockItem instanceof ItemVariantBlock variant ? variant.variantState() : blockItem.getBlock().defaultBlockState();
        } else {
            if (!stack.is(source.getBlock().asItem())) return false;
            supplied = source.getBlock().defaultBlockState();
        }
        supplied = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(supplied);
        for (Property<?> property : source.getProperties()) {
            if (property instanceof IntegerProperty && (property.getName().equals("variant") || property.getName().equals("type"))) {
                if (!source.getValue(property).equals(supplied.getValue(property))) return false;
            }
        }
        return true;
    }

    /** Stage a complete main-inventory payment before removing any units, including payments split across stacks. */
    public static boolean consume(Player player, BlockState source) {
        int remaining = units(source);
        var paid = new ArrayList<ItemStack>();
        var amounts = new ArrayList<Integer>();
        for (ItemStack stack : player.getInventory().items) {
            if (!matches(stack, source)) continue;
            int take = Math.min(remaining, stack.getCount());
            paid.add(stack);
            amounts.add(take);
            remaining -= take;
            if (remaining == 0) break;
        }
        if (remaining != 0) return false;
        for (int index = 0; index < paid.size(); index++) paid.get(index).shrink(amounts.get(index));
        player.getInventory().setChanged();
        return true;
    }
}
