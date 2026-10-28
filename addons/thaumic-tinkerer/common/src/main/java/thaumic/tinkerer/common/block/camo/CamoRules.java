package thaumic.tinkerer.common.block.camo;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Which blocks a camouflageable device may imitate, and what a right click changes (1.7.10: {@code BlockCamo}, whose
 * check was "the render type of the block is 0, 31 or 39, it is no camouflageable block and not air").
 *
 * <p>1.21.1 has no render types. The equivalent is a block that the chunk renderer draws from its model
 * ({@link RenderShape#MODEL}) and that has no block entity; fluids, invisible blocks and blocks with a special renderer
 * are therefore refused, as the original refused every render type but the plain cube, the log and the pillar.</p>
 */
public final class CamoRules {
    private CamoRules() {}

    /** The result of a right click with a stack on a camouflageable device. */
    public record Change(Kind kind, @Nullable BlockState state) {
        public enum Kind {
            /** The click does nothing to the camouflage (the item is no valid block). */
            NONE,
            /** The camouflage is removed (the empty hand). */
            CLEAR,
            /** The camouflage becomes {@link Change#state()}. */
            SET
        }

        public static final Change NONE = new Change(Kind.NONE, null);
        public static final Change CLEAR = new Change(Kind.CLEAR, null);
    }

    /** True when a device may look like this state. */
    public static boolean isValidCamo(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.getBlock() instanceof CamoDeviceBlock) return false;
        if (state.getBlock() instanceof EntityBlock) return false;
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(LibMisc.id(LibBlockNames.CAMO))) return false;
        return state.getRenderShape() == RenderShape.MODEL;
    }

    /**
     * What clicking the face {@code face} of a device with {@code stack} does. An empty hand clears the camouflage; a
     * block item sets it to its default state (1.7.10 also copied the item's damage as metadata; the variants are
     * separate blocks now), turned to face the clicked side when it has a horizontal facing ({@code BlockDirectional}).
     */
    public static Change change(ItemStack stack, Direction face) {
        if (stack.isEmpty()) return Change.CLEAR;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return Change.NONE;
        BlockState state = blockItem.getBlock().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && face.getAxis().isHorizontal()) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, face);
        }
        if (!isValidCamo(state)) return Change.NONE;
        return new Change(Change.Kind.SET, state);
    }
}
