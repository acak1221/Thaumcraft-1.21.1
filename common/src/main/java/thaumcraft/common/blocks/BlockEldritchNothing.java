package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.tiles.TileEldritchNothing;

/**
 * Original BlockEldritchNothing: the blank eldritch filler. It is unbreakable,
 * drops nothing, only exists as a hidden block entity once it is exposed to air
 * and hurts everything that walks into it.
 */
public final class BlockEldritchNothing extends BaseEntityBlock implements StateDependentBlockEntity, PerStateBlockEntity, OriginalPistonPushable {
    public static final MapCodec<BlockEldritchNothing> CODEC = simpleCodec(BlockEldritchNothing::new);
    /** Original metadata 1 marks the exposed variant that carries the block entity. */
    public static final IntegerProperty EXPOSED = IntegerProperty.create("exposed", 0, 1);

    public BlockEldritchNothing(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(EXPOSED, 0));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(EXPOSED); }

    /**
     * The original BlockEldritchNothing is not drawn at all: the 1.7.10 block answers the
     * render type minus one (OpenTC4 BlockEldritchNothing:57-58) and the void is drawn by the
     * tile renderer of the port ({@code TileEldritchNothingRenderer}), which paints the tunnel
     * on the faces that touch a solid block. The empty model of the port
     * ({@code assets/thaumcraft/models/block/eldritch_nothing.json}) is the 1.7.10 answer.
     */
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    /** BlockEldritchNothing:81-87 - only the exposed metadata carries the block entity. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(EXPOSED) == 1 ? new TileEldritchNothing(pos, state) : null;
    }

    /**
     * BlockEldritchNothing:61-63 - the original created the tile for the metadata one only,
     * the exposed filler, and answered {@code hasTileEntity(int)} with exactly that. The port
     * answers the question per state, so the covered filler is moved by a piston like in the
     * original.
     */
    @Override public boolean hasBlockEntityForState(BlockState state) {
        return state.getValue(EXPOSED) == 1;
    }

    /**
     * Oracle BlockEldritchNothing:42-43 - the 1.7.10 class extended {@code
     * net.minecraft.block.Block} and did not implement {@code ITileEntityProvider}, so the piston
     * of the original moved the filler for every metadata, the exposed filler (the metadata one)
     * included, and the move destroyed the tile of the exposed filler.
     *
     * <p>Minecraft 1.21.1 refuses every block entity block ({@code PistonBaseBlock#isPushable}
     * ends with {@code return !blockState.hasBlockEntity();}); the mixin {@code
     * thaumcraft.common.mixin.PistonPushabilityMixin} asks this answer instead.
     */
    @Override public boolean isPushableInOriginal(BlockState state) {
        return OriginalPistonRule.Family.ELDRITCH_NOTHING.isPushableInOriginal(state.getValue(EXPOSED));
    }

    /** BlockEldritchNothing:69-71 - the block never has a selection box. */
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** BlockEldritchNothing:108-111 - the collision box is inset by an eighth. */
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(2.0D, 2.0D, 2.0D, 14.0D, 14.0D, 14.0D);
    }

    /** BlockEldritchNothing:102-106 - everything that is not a fresh entity takes eight damage. */
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (entity.tickCount <= 20) return;
        if (entity instanceof Player player && !player.getAbilities().invulnerable) return;
        entity.hurt(level.damageSources().generic(), 8.0F);
    }

    /** BlockEldritchNothing:93-100 - the block entity only exists while the block is exposed. */
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighbourPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighbourPos, movedByPiston);
        boolean exposed = level.getBlockState(pos.above()).isAir() || level.getBlockState(pos.below()).isAir();
        if (!exposed) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(pos.relative(side)).isAir()) {
                    exposed = true;
                    break;
                }
            }
        }
        int wanted = exposed ? 1 : 0;
        if (state.getValue(EXPOSED) != wanted) {
            level.setBlock(pos, state.setValue(EXPOSED, wanted), Block.UPDATE_ALL);
        }
    }
}
