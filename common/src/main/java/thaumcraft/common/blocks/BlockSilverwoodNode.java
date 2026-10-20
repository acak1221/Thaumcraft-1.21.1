package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.config.ThaumcraftNaturals;
import thaumcraft.common.lib.world.NodeHarvest;
import thaumcraft.common.tiles.TileNode;

/** Original blockMagicalLog metadata2: a real full log containing an ordinary TileNode, not an aura block beside a tree. */
public final class BlockSilverwoodNode extends RotatedPillarBlock implements EntityBlock {
    public static final MapCodec<BlockSilverwoodNode> CODEC = simpleCodec(BlockSilverwoodNode::new);
    public BlockSilverwoodNode(Properties properties) { super(properties); }
    @Override public MapCodec<BlockSilverwoodNode> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileNode(ThaumcraftContent.NODE_BLOCK_ENTITY.get(), pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ThaumcraftContent.NODE_BLOCK_ENTITY.get()) return null;
        return level.isClientSide ? (world, pos, owner, tile) -> TileNode.clientTick(world, pos, owner, (TileNode) tile)
                : (world, pos, owner, tile) -> TileNode.serverTick(world, pos, owner, (TileNode) tile);
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        TileNode node = level.getBlockEntity(pos) instanceof TileNode value ? value : null;
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        NodeHarvest.prepare(level, pos, state, player, node);
        return result;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        NodeHarvest.removing(level, pos, state, replacement);
        // Original exhausted knot changed metadata2 -> silverwood1 within the same log block, without breakBlock leaf decay.
        if (!replacement.is(this) && !replacement.is(ThaumcraftNaturals.SILVERWOOD_LOG.get()) && !level.isClientSide)
            BlockMagicalLeaves.beginDecay(level, pos, 4);
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state); NodeHarvest.committed(level, pos, state);
    }
    @Override public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ThaumcraftNaturals.SILVERWOOD_LOG.get());
    }
}
