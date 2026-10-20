package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.lib.GrateRules;
import thaumcraft.common.tiles.TileGrate;

/**
 * Original BlockMetalDevice metadata 5 (open) and 6 (closed): a metal plate across the top 3/16 of the block (hardness 3, resistance 17
 * stored, exposed 10.2). Dropped items fall through it while it is open; everything else, and every item once it is closed, stands on
 * it. A bare right click toggles it, a redstone signal closes it and its loss opens it again.
 */
public final class BlockItemGrate extends BaseEntityBlock {
    public static final MapCodec<BlockItemGrate> CODEC = simpleCodec(BlockItemGrate::new);
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    // Level events 1006 / 1012: the wooden door opening and closing sounds (the original sent the random door event 1003).
    private static final int OPEN_SOUND = 1006;
    private static final int CLOSE_SOUND = 1012;
    private static final VoxelShape PLATE = Block.box(0.0D, GrateRules.PLATE_BOTTOM * 16.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public BlockItemGrate(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CLOSED, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CLOSED); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileGrate(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return PLATE; }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Entity entity = context instanceof EntityCollisionContext entityContext ? entityContext.getEntity() : null;
        return GrateRules.collides(state.getValue(CLOSED), entity != null, entity instanceof ItemEntity) ? PLATE : Shapes.empty();
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        boolean closing = !state.getValue(CLOSED);
        if (!level.isClientSide) level.setBlock(pos, state.setValue(CLOSED, closing), Block.UPDATE_CLIENTS);
        playToggle(level, pos, player, closing);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block changed, BlockPos fromPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            boolean closed = state.getValue(CLOSED);
            boolean after = GrateRules.closedAfterNeighbourChange(closed, level.hasNeighborSignal(pos), changed.defaultBlockState().isSignalSource());
            if (after != closed) {
                level.setBlock(pos, state.setValue(CLOSED, after), Block.UPDATE_CLIENTS);
                playToggle(level, pos, null, after);
            }
        }
        super.neighborChanged(state, level, pos, changed, fromPos, movedByPiston);
    }

    private static void playToggle(Level level, BlockPos pos, Player player, boolean closing) {
        level.levelEvent(player, closing ? CLOSE_SOUND : OPEN_SOUND, pos, 0);
    }
}
