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
import thaumcraft.api.visnet.TileVisNode;
import thaumcraft.common.config.ThaumcraftNodeDevices;
import thaumcraft.common.lib.visnet.VisNodeFailures;
import thaumcraft.common.tiles.TileNodeEnergized;

/** Original airy5. No block item, essence harvest or ordinary-node drop hook. */
public final class BlockNodeEnergized extends BaseEntityBlock {
    public static final MapCodec<BlockNodeEnergized> CODEC = simpleCodec(BlockNodeEnergized::new);
    private static final VoxelShape OUTLINE = box(4.8, 4.8, 4.8, 11.2, 11.2, 11.2);
    public BlockNodeEnergized(Properties properties) { super(properties.strength(2, 200).lightLevel(state -> 8).noCollission().noOcclusion().noLootTable()); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileNodeEnergized(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return OUTLINE; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (level instanceof ServerLevel server && !VisNodeFailures.nativeServer(server)) return;
        if (!level.isClientSide && VisNodeFailures.loaded(level, pos) && level.getBlockState(pos) == state
                && level.getBlockEntity(pos) instanceof TileNodeEnergized source) VisNodeFailures.checkTower(source);
        super.neighborChanged(state, level, pos, block, fromPos, moving);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftNodeDevices.ENERGIZED_NODE_BLOCK_ENTITY.get(),
                level.isClientSide ? TileVisNode::tick : TileNodeEnergized::serverTick);
    }
}
