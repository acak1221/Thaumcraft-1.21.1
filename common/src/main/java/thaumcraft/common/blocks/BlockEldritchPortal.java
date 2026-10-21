package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.tiles.TileEldritchPortal;

/**
 * Original BlockEldritchPortal: the light emitting eldritch gate. It is
 * unbreakable, has no collision and only survives between two eldritch blocks.
 */
public final class BlockEldritchPortal extends BaseEntityBlock implements OriginalPistonPushable {
    public static final MapCodec<BlockEldritchPortal> CODEC = simpleCodec(BlockEldritchPortal::new);

    public BlockEldritchPortal(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    /**
     * The original BlockEldritchPortal is not drawn as a block: the 1.7.10 block answers the
     * render type minus one (OpenTC4 BlockEldritchPortal:78-79) and the gate is drawn by the
     * tile renderer of the port ({@code TileEldritchPortalRenderer}). The empty model of the
     * port ({@code assets/thaumcraft/models/block/eldritch_portal.json}) is the 1.7.10 answer.
     */
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    /** BlockEldritchPortal:115-121 - every portal tile exists. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileEldritchPortal(pos, state); }

    /**
     * Oracle BlockEldritchPortal:38-39 - the 1.7.10 class extended {@code
     * net.minecraft.block.Block} and did not implement {@code ITileEntityProvider}, so the piston
     * of the original moved the gate, whose tile existed for every metadata
     * (BlockEldritchPortal:115-121), and the move destroyed the tile.
     *
     * <p>Minecraft 1.21.1 refuses every block entity block ({@code PistonBaseBlock#isPushable}
     * ends with {@code return !blockState.hasBlockEntity();}); the mixin {@code
     * thaumcraft.common.mixin.PistonPushabilityMixin} asks this answer instead.
     */
    @Override public boolean isPushableInOriginal(BlockState state) {
        return OriginalPistonRule.Family.ELDRITCH_PORTAL.isPushableInOriginal(0);
    }

    /**
     * Original TileEldritchPortal:41-54 - the gate tile updates every tick.
     *
     * <p>The 1.7 tile updated itself, so the native port needs the block ticker of the gate.
     */
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ThaumcraftEldritchBlocks.ELDRITCH_PORTAL_TILE.get(),
                (lvl, pos, st, tile) -> {
                    if (tile instanceof TileEldritchPortal portal) TileEldritchPortal.tick(lvl, pos, st, portal);
                });
    }

    /** BlockEldritchPortal:80-89 - the portal has neither a selection nor a collision box. */
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** BlockEldritchPortal:123-128 - the portal only survives between two eldritch blocks. */
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighbourPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighbourPos, movedByPiston);
        if (!level.getBlockState(pos.above()).is(ThaumcraftEldritchBlocks.ELDRITCH.get())
                || !level.getBlockState(pos.below()).is(ThaumcraftEldritchBlocks.ELDRITCH.get())) {
            level.destroyBlock(pos, false);
        }
    }
}
