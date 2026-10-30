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
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A device that can be camouflaged: right-click it with a block to make it look like that block, with an empty hand to
 * make it look like itself again (1.7.10: {@code BlockCamo}).
 *
 * <p>How it is drawn: the {@code camo} state property is true while a camouflage is set. Then the block's own model is
 * not rendered ({@link RenderShape#INVISIBLE}) and the block entity renderer of the client draws the camouflage state
 * instead. This works on every loader without model-data APIs; the camouflage is rendered like a block entity, so it
 * gets no ambient occlusion and no tint from its position (see {@code docs/deviations/TileBlocks.md}).</p>
 *
 * <p>An invisible block must not hide the faces of its neighbours either: while {@code camo=true} the block occludes
 * nothing ({@link #getOcclusionShape}), whatever its {@code noOcclusion()} setting says, so a camouflage that is not a
 * solid cube (glass, ice, leaves) shows the faces behind it and not the void.</p>
 */
public abstract class CamoDeviceBlock extends BaseEntityBlock {
    /** True while the device looks like another block. */
    public static final BooleanProperty CAMO = BooleanProperty.create("camo");

    protected CamoDeviceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CAMO, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CAMO);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(CAMO) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    /**
     * The shape the neighbours cull their faces against. A device that is drawn as another block does not own its cell's
     * faces: its own model is not rendered, so a full-cube occlusion shape would only hide the neighbours' faces (the floor
     * under a camouflaged Golem Connector became a hole to the sky) and the camouflage is drawn by the block entity
     * renderer, which culls its own faces against the neighbours. The state is part of the shape cache of the game, so the
     * answer may depend on {@code camo} (and not on the level). Light is another matter: see
     * {@code BlockGolemConnector#getLightBlock}.
     */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(CAMO) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    /**
     * What a device does with an item before the camouflage logic looks at it (the wand of the placer and the
     * dislocator). Returns null to let the camouflage logic go on.
     */
    @Nullable
    protected ItemInteractionResult useSpecialItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        return null;
    }

    /**
     * {@code BlockCamo.onBlockActivated}: the empty hand clears the camouflage, a block item sets it, any other item
     * is left to the item (the Transvector Binder). Only the main hand counts, as 1.7.10 had one hand.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        ItemInteractionResult special = useSpecialItem(stack, state, level, pos, player, hit);
        if (special != null) return special;
        if (!(level.getBlockEntity(pos) instanceof CamoDeviceBlockEntity device)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        CamoRules.Change change = CamoRules.change(stack, hit.getDirection());
        if (change.kind() == CamoRules.Change.Kind.NONE) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) device.setCamo(change.state());
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
