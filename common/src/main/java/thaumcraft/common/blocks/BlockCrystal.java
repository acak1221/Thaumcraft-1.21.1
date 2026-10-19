package thaumcraft.common.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.crafting.IInfusionStabiliser;

/** Adds only BlockCrystal:246-247's stabiliser contract to the existing flattened crystals. */
public final class BlockCrystal extends Block implements IInfusionStabiliser {
    public static final MapCodec<BlockCrystal> CODEC = simpleCodec(BlockCrystal::new);
    public BlockCrystal(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override public boolean canStabaliseInfusion(Level level, BlockPos pos) { return true; }
}
