package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import thaumcraft.common.tiles.TileWarded;

/** Original warded wrapper: unbreakable, no item/loot, no explosion removal. The renderer draws its remembered real block. */
public final class BlockWarded extends BaseEntityBlock {
    public static final MapCodec<BlockWarded> CODEC = simpleCodec(BlockWarded::new);
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    public BlockWarded(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(LIGHT, 0)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIGHT); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileWarded(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> drop) {
        // Original BlockWarded.onBlockExploded is deliberately empty, including when a huge explosion reaches the block.
    }
}
