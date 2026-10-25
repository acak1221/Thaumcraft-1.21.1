package thaumcraft.common.blocks;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The item of the compound recharge focus, the original BlockStoneDevice metadata 8.
 *
 * <p>The original focus was the metadata eight of the stone device family and it had no tile of
 * its own: the wand pedestal looked at the block above it and asked whether it was a focus
 * (TileWandPedestal:337-358). The native port keeps the focus as the {@code focus} block state of
 * the wand pedestal block. Its dedicated item chooses the focus state before the native
 * block entity is created; the focus itself must not acquire the pedestal's tile.
 *
 * <p>Original sources: {@code thaumcraft/common/blocks/BlockStoneDevice.java}:271 (the metadata
 * eight has no tile), {@code thaumcraft/common/config/ConfigRecipes.java}:342 (the WandPedFocus
 * infusion) and {@code thaumcraft/common/tiles/TileWandPedestal.java}:337.
 */
public final class ItemWandPedestalFocus extends BlockItem {
    public ItemWandPedestalFocus(Block block, Properties properties) {
        super(block, properties);
    }

    /** The focus state of the wand pedestal. */
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = getBlock().defaultBlockState();
        for (var property : state.getProperties()) {
            if (property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty flag
                    && "focus".equals(property.getName())) {
                return state.setValue(flag, true);
            }
        }
        return state;
    }
}
