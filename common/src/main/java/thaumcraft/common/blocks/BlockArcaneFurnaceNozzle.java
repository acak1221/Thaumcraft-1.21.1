package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftLegacyTiles;

/**
 * Original BlockArcaneFurnace metadata 10: the output nozzle of the arcane furnace.
 * <p>
 * The nozzle is the half block that sits next to the furnace core. It is the half that touches the core, so
 * its box follows the core, and it lights up to thirteen. It holds no tile entity and it is restored to iron
 * bars when the furnace collapses.
 * <p>
 * 1.21.1 has no block metadata, so the nozzle became its own block with a facing.
 * <p>
 * Oracle: {@code thaumcraft/common/blocks/BlockArcaneFurnace.java}, lines 235-255 and 347-349.
 */
public final class BlockArcaneFurnaceNozzle extends Block {
    public static final MapCodec<BlockArcaneFurnaceNozzle> CODEC = simpleCodec(BlockArcaneFurnaceNozzle::new);
    /** The side the core of the furnace is on. */
    public static final DirectionProperty FACING = DirectionProperty.create("facing");
    private static final VoxelShape WEST_SHAPE = Shapes.box(0, 0, 0, 8, 16, 16);
    private static final VoxelShape EAST_SHAPE = Shapes.box(8, 0, 0, 16, 16, 16);
    private static final VoxelShape NORTH_SHAPE = Shapes.box(0, 0, 0, 16, 16, 8);
    private static final VoxelShape SOUTH_SHAPE = Shapes.box(0, 8, 0, 16, 16, 16);

    public BlockArcaneFurnaceNozzle(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The default state of the nozzle: the original metadata ten block. */
    public static BlockState state() {
        return ThaumcraftLegacyTiles.ARCANE_FURNACE_NOZZLE.get().defaultBlockState();
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            case NORTH -> NORTH_SHAPE;
            default -> SOUTH_SHAPE;
        };
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /**
     * The original looks for the core next to the nozzle: west first, then east, then north, and south when
     * there is none.
     */
    public static Direction facing(Level level, BlockPos pos) {
        if (isCore(level, pos.west())) return Direction.WEST;
        if (isCore(level, pos.east())) return Direction.EAST;
        if (isCore(level, pos.north())) return Direction.NORTH;
        return Direction.SOUTH;
    }

    private static boolean isCore(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof BlockArcaneFurnace && state.getValue(BlockArcaneFurnace.VARIANT) == 0;
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moving);
        if (level.isClientSide) return;
        Direction facing = facing(level, pos);
        if (facing != state.getValue(FACING)) level.setBlock(pos, state.setValue(FACING, facing), Block.UPDATE_ALL);
    }
}
