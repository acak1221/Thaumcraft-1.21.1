package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.config.ThaumcraftHoles;
import thaumcraft.common.lib.world.HoleBlockRules;
import thaumcraft.common.tiles.TileHole;

/**
 * Original BlockHole: what a Portable Hole leaves for a while where a block was. Unbreakable (the 1.7.10
 * setter value 6000000 stores 18000000 and the vanilla getter divides it by five, so the effective blast
 * resistance is 3600000), light 0.7 (10), no collision, no outline, no drops, not pickable and no item;
 * being solid MATERIAL in 1.7 (rock) it also kept fluids out, which {@code forceSolidOn} does here. Its
 * tile ({@link TileHole}) remembers the block and gives it back.
 */
public final class BlockHole extends BaseEntityBlock {
    public static final MapCodec<BlockHole> CODEC = simpleCodec(BlockHole::new);

    public BlockHole(Properties properties) { super(properties.strength(-1.0F, HoleBlockRules.HOLE_RESISTANCE).noCollission().noOcclusion().forceSolidOn().noLootTable()); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileHole(pos, state); }
    // The opening is drawn by a block-entity renderer in the original; nothing of the block itself is.
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Oracle TileHole.java:51-56 - the update tick of the original runs on both sides and the
        // client side draws the sparkles around the hole.
        return level.isClientSide
                ? createTickerHelper(type, ThaumcraftHoles.HOLE_BLOCK_ENTITY.get(), TileHole::clientTick)
                : createTickerHelper(type, ThaumcraftHoles.HOLE_BLOCK_ENTITY.get(), TileHole::serverTick);
    }
}
