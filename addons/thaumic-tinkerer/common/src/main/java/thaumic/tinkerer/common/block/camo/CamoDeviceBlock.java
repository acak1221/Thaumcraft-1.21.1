package thaumic.tinkerer.common.block.camo;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A device that can be camouflaged: right-click it with a block to make it look like that block, with an empty hand to
 * make it look like itself again (1.7.10: {@code BlockCamo}).
 *
 * <p>How it is drawn: the {@code camo} state property is true while a camouflage is set. Then the block's own model is
 * not rendered ({@link RenderShape#INVISIBLE}) and the block entity renderer of the client draws the camouflage state
 * instead. This works on every loader without model-data APIs; the camouflage is rendered like a block entity, so it
 * gets no ambient occlusion and no tint from its position (see {@code docs/deviations/TileBlocks.md}).</p>
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
