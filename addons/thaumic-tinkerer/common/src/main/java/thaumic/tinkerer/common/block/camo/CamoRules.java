/**
 * This file is new in the Minecraft 1.21.1 port of Thaumic Tinkerer for Thaumcraft 4 Reload (no 1.7.10 counterpart, see
 * addons/thaumic-tinkerer/NOTICE.md). Thaumic Tinkerer was created by Vazkii; the port is distributed under a Creative
 * Commons Attribution-NonCommercial-ShareAlike 3.0 License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 */
package thaumic.tinkerer.common.block.camo;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumic.tinkerer.common.lib.LibBlockNames;
import thaumic.tinkerer.common.lib.LibMisc;

/**
 * Which blocks a camouflageable device may imitate, and what a right click changes (1.7.10: {@code BlockCamo}, whose
 * check was "the render type of the block is 0, 31 or 39, it is no camouflageable block and not air").
 *
 * <p>1.21.1 has no render types. The three types were the cube-like drawings (the plain block with a texture on each side,
 * the log, the quartz pillar), which the device showed on its own cube. The equivalent is a block that the chunk renderer
 * draws from its model ({@link RenderShape#MODEL}) <em>and</em> whose collision shape is the full cube: stairs, slabs,
 * fences, panes, doors, torches, plants, carpets, rails and so on are refused, as were every other render type, and so are
 * the 1.7.10 plain-block types that were not cubes (slabs, farmland, soul sand, snow layers), whose model the port draws
 * itself and would show smaller than the device. Fluids, invisible blocks and blocks drawn by a special renderer (chests,
 * beds, skulls) are refused by the render shape. Glass, leaves, ice, a furnace or a dispenser (a plain cube in 1.7.10) are
 * accepted: the model is drawn, without the block entity.</p>
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
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(LibMisc.id(LibBlockNames.CAMO))) return false;
        return state.getRenderShape() == RenderShape.MODEL
                && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /**
     * What clicking the face {@code face} of a device with {@code stack} does. An empty hand clears the camouflage; a
     * block item sets it to its default state (1.7.10 also copied the item's damage as metadata; the variants are
     * separate blocks now), turned to face the clicked side when it is a pumpkin ({@code BlockDirectional}, the one
     * directional block among the cube-like ones; the 1.7.10 furnace and dispenser were not directional and are not turned).
     */
    public static Change change(ItemStack stack, Direction face) {
        if (stack.isEmpty()) return Change.CLEAR;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return Change.NONE;
        BlockState state = blockItem.getBlock().defaultBlockState();
        if (state.getBlock() instanceof CarvedPumpkinBlock && face.getAxis().isHorizontal()) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, face);
        }
        if (!isValidCamo(state)) return Change.NONE;
        return new Change(Change.Kind.SET, state);
    }
}
