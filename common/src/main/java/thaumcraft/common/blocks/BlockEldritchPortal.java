package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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
public final class BlockEldritchPortal extends BaseEntityBlock {
    public static final MapCodec<BlockEldritchPortal> CODEC = simpleCodec(BlockEldritchPortal::new);

    public BlockEldritchPortal(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    /** BlockEldritchPortal:115-121 - every portal tile exists. */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileEldritchPortal(pos, state); }

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
