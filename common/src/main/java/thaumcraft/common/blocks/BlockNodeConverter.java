package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.lib.visnet.VisNodeFailures;
import thaumcraft.common.tiles.TileNodeConverter;
import thaumcraft.common.tiles.TileNodeEnergized;

/** Original stoneDevice11: ordinary block-item placement, full physical cube,
 * inverted animated frame. No facing, menu, inventory or stored item payload. */
public final class BlockNodeConverter extends BaseEntityBlock {
    public static final MapCodec<BlockNodeConverter> CODEC = simpleCodec(BlockNodeConverter::new);
    public BlockNodeConverter(Properties properties) { super(properties.strength(3, 25).noOcclusion()); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileNodeConverter(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.block(); }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) { return 0; }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (level instanceof ServerLevel server && !VisNodeFailures.nativeServer(server)) return;
        if (VisNodeFailures.loaded(level, pos) && level.getBlockState(pos) == state
                && level.getBlockEntity(pos) instanceof TileNodeConverter converter) converter.checkStatus();
        super.neighborChanged(state, level, pos, block, fromPos, moving);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (level instanceof ServerLevel server && !VisNodeFailures.nativeServer(server)) return;
        BlockEntity removing = VisNodeFailures.existing(level, pos);
        if (!replacement.is(state.getBlock()) && level instanceof ServerLevel && VisNodeFailures.loaded(level, pos.below())
                && level.getBlockEntity(pos.below()) instanceof TileNodeEnergized source) VisNodeFailures.checkTower(source);
        // A callback may have installed a new converter at this coordinate.
        if (removing instanceof TileNodeConverter && removing.getBlockState() == state && level.getBlockState(pos) == replacement
                && VisNodeFailures.existing(level, pos) == removing) super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftNodeDevices.NODE_TRANSDUCER_BLOCK_ENTITY.get(),
                level.isClientSide ? TileNodeConverter::clientTick : TileNodeConverter::serverTick);
    }
}
