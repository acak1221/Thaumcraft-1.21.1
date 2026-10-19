package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftResources;

/** TC4's stateless, client-ticking Nitor tile. No server timer or saved FX data. */
public final class TileNitor extends BlockEntity {
    private static ClientEffects effects;

    public TileNitor(BlockPos pos, BlockState state) { super(ThaumcraftResources.NITOR_BLOCK_ENTITY.get(), pos, state); }

    public static void setClientEffects(ClientEffects value) { effects = value; }
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileNitor tile) {
        if (level.isClientSide && effects != null) effects.tick(level, pos);
    }
    public static void animate(Level level, BlockPos pos, RandomSource random) {
        if (level.isClientSide && effects != null) effects.sparkle(level, pos, random);
    }

    /** Installed only by the client entry point; the dedicated server never loads a renderer. */
    public interface ClientEffects {
        void tick(Level level, BlockPos pos);
        void sparkle(Level level, BlockPos pos, RandomSource random);
    }
}
