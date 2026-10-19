package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Flattened log species: the native axis is orientation, never the old species metadata. */
public final class BlockMagicalLog extends RotatedPillarBlock {
    public static final MapCodec<BlockMagicalLog> CODEC = simpleCodec(BlockMagicalLog::new);

    public BlockMagicalLog(Properties properties) { super(properties); }
    @Override public MapCodec<BlockMagicalLog> codec() { return CODEC; }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && !level.isClientSide) BlockMagicalLeaves.beginDecay(level, pos, 4);
        super.onRemove(state, level, pos, replacement, moving);
    }
}
