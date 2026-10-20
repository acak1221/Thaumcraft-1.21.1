package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The original cosmetic-opaque amber variants: transparent faces, stone sound and light opacity three. */
public final class BlockAmber extends Block {
    public static final MapCodec<BlockAmber> CODEC = simpleCodec(BlockAmber::new);
    public BlockAmber(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 3; }
}
