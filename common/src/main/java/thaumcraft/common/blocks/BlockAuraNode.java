package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ThaumcraftContent;
import thaumcraft.common.lib.world.NodeHarvest;
import thaumcraft.common.tiles.TileNode;

public final class BlockAuraNode extends BaseEntityBlock {
    public static final MapCodec<BlockAuraNode> CODEC = simpleCodec(BlockAuraNode::new);
    private static final VoxelShape NODE_SHAPE = box(4.8, 4.8, 4.8, 11.2, 11.2, 11.2);
    // BlockAiry:115-140 overrides NORMAL node hardness2/resistance200. The old
    // native fixture supplied unbreakable properties; do not inherit them or
    // return a whole-node item through ordinary native loot.
    public BlockAuraNode(Properties properties) { super(properties.strength(2.0F, 200.0F).noLootTable()); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileNode(ThaumcraftContent.NODE_BLOCK_ENTITY.get(), pos, state);
    }
    // Native BER draws the original node strips; never a glass/cube stand-in.
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Original BlockAiry node's 0.3..0.7 targeting bounds, no collision.
        return NODE_SHAPE;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? createTickerHelper(type, ThaumcraftContent.NODE_BLOCK_ENTITY.get(), TileNode::clientTick)
                : createTickerHelper(type, ThaumcraftContent.NODE_BLOCK_ENTITY.get(), TileNode::serverTick);
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        TileNode node = level.getBlockEntity(pos) instanceof TileNode value ? value : null;
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        // Native callbacks may already have removed/replaced this exact node.
        // Snapshot only a still-current server BE; this is not a drop yet.
        NodeHarvest.prepare(level, pos, state, player, node);
        return result;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        NodeHarvest.removing(level, pos, state, replacement);
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        super.destroy(level, pos, state);
        NodeHarvest.committed(level, pos, state);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                       Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof TileNode node)) return InteractionResult.PASS;
        // Keep the old fixture's diagnostic only; wild nodes do not grant a
        // free aspect readout instead of the thaumometer's real scan path.
        if (node.isNatural()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            var aspects = node.getAspects();
            StringBuilder message = new StringBuilder("Node: ");
            for (Aspect aspect : aspects.getAspects()) message.append(aspect.getTag()).append('=').append(aspects.getAmount(aspect)).append(' ');
            player.displayClientMessage(Component.literal(message.toString()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
