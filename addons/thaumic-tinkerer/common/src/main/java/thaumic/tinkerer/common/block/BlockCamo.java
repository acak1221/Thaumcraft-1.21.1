/**
 * This class was created by <Vazkii>. It's distributed as part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0
 * License (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4. Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [Dec 27, 2013, 6:25:36 PM (GMT)]
 *
 * Ported to Minecraft 1.21.1 for Thaumcraft 4 Reload (see addons/thaumic-tinkerer/NOTICE.md).
 */
package thaumic.tinkerer.common.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import thaumic.tinkerer.common.block.tile.TileCamo;

/**
 * A block that can be camouflaged: a right click with a block makes it look like that block, a right click with an
 * empty hand makes it look like itself again.
 *
 * <p>How it is drawn: the {@link #CAMOUFLAGED} block-state property is true while a camouflage is set. Then the block's
 * own model is not drawn ({@link RenderShape#INVISIBLE}) and the block entity renderer of the client
 * ({@code thaumic.tinkerer.client.render.tile.RenderTileCamo}) draws the camouflaged state in its place. 1.7.10 asked the
 * camouflaged block for its icons; 1.21.1 has no such thing, and a dynamic model needs a loader-specific API, so
 * the camouflage is drawn like a block entity (no animated models, see docs/deviations/SimpleBlocks.md).</p>
 *
 * <p>A subclass that has its own block entity extends {@link TileCamo} for it and lists its blockstate files'
 * {@code camouflaged=false/true} variants; {@link #isValidCamo} is the 1.7.10 render type test (types 0, 31 and 39:
 * plain cubes, logs and quartz).</p>
 */
public abstract class BlockCamo extends BaseEntityBlock {
    /** True while the block looks like another block. */
    public static final BooleanProperty CAMOUFLAGED = BooleanProperty.create("camouflaged");

    protected BlockCamo(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CAMOUFLAGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CAMOUFLAGED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(CAMOUFLAGED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    /** The block is a solid cube drawn from its model, whatever it imitates is drawn on top of it by the renderer. */
    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }

    /**
     * Whether a block may be imitated: the 1.7.10 test accepted the render types of the plain cube, the log and the quartz
     * block and refused air and other camouflage blocks. The 1.21.1 equivalent is a block that the chunk renderer
     * draws from its model and that fills its cell with a full cube.
     */
    public static boolean isValidCamo(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.getBlock() instanceof BlockCamo) return false;
        if (state.getRenderShape() != RenderShape.MODEL) return false;
        return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /**
     * The state a block item imitates when it is used on the face {@code face}: its default state, turned to the clicked
     * side when it has a horizontal facing (1.7.10 {@code BlockDirectional}). Null when the item is no valid block.
     */
    @Nullable
    public static BlockState camoStateOf(ItemStack stack, Direction face) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return null;
        BlockState state = blockItem.getBlock().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && face.getAxis().isHorizontal()) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, face);
        }
        return isValidCamo(state) ? state : null;
    }

    /**
     * 1.7.10 {@code onBlockActivated}: the empty hand clears the camouflage, a valid block sets it, any other item
     * is left alone. Only the main hand counts, as 1.7.10 had one hand; a sneaking player with an item never gets here
     * (Minecraft then uses the item instead, which places the block beside the camouflage block).
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof TileCamo tile)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        BlockState camo = null;
        if (!stack.isEmpty()) {
            camo = camoStateOf(stack, hit.getDirection());
            if (camo == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) tile.setCamo(camo);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The camouflaged state of the block at a position, or null. */
    @Nullable
    public static BlockState getCamo(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileCamo tile ? tile.getCamo() : null;
    }
}
