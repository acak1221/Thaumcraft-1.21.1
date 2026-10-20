package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ThaumcraftLegacyTiles;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.tiles.LegacyTileRules;

/**
 * Original TileArcaneFurnaceNozzle: the essentia nozzle of the arcane furnace.
 * <p>
 * It sits in one of the four blocks around the furnace core, looks away from that core and draws fire
 * essentia from the vis network on that side. Every successful draw gives the furnace six hundred ticks of
 * speedy cooking.
 * <p>
 * Oracle: {@code thaumcraft/common/tiles/TileArcaneFurnaceNozzle.java}, lines 18-135.
 */
public class TileArcaneFurnaceNozzle extends BlockEntity implements IEssentiaTransport {
    /** TileArcaneFurnaceNozzle:21 - the side the nozzle draws from; null until it has found its core. */
    private Direction facing;
    /** TileArcaneFurnaceNozzle:22 - the furnace core this nozzle belongs to. */
    private TileArcaneFurnace furnace;
    /** TileArcaneFurnaceNozzle:23 - the tick counter of the five tick draw period. */
    private int drawDelay;

    public TileArcaneFurnaceNozzle(BlockPos pos, BlockState state) {
        super(ThaumcraftLegacyTiles.ARCANE_FURNACE_NOZZLE_BLOCK_ENTITY.get(), pos, state);
    }

    /** The side the nozzle draws from, or null while it has not found its core. */
    public Direction getFacing() { return facing; }
    /** The furnace core this nozzle belongs to, or null. */
    public TileArcaneFurnace getFurnace() { return furnace; }

    public static void tick(Level level, BlockPos pos, BlockState state, TileArcaneFurnaceNozzle tile) {
        if (tile.level != level || !pos.equals(tile.worldPosition) || tile.isRemoved() || tile.getBlockState() != state) return;
        if (tile.facing == null && tile.furnace == null) {
            tile.facing = null;
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = pos.relative(direction);
                if (!level.hasChunkAt(neighbour)) continue;
                if (!(level.getBlockEntity(neighbour) instanceof TileArcaneFurnace core)) continue;
                tile.facing = direction.getOpposite();
                tile.furnace = core;
                break;
            }
        }
        if (level.isClientSide) return;
        try {
            TileArcaneFurnace core = tile.furnace;
            if (core != null && core.speedyTime < 60 && tile.drawEssentia(level)) core.speedyTime += LegacyTileRules.NOZZLE_SPEEDY_GRANT;
        } catch (RuntimeException ignored) {
            // The original isolates a failing vis network draw.
        }
    }

    /** TileArcaneFurnaceNozzle:52-67 - the nozzle draws on every fifth tick. */
    boolean drawEssentia(Level level) {
        ++drawDelay;
        if (!LegacyTileRules.nozzleDrawTick(drawDelay - 1)) return false;
        Direction side = facing;
        if (side == null) return false;
        BlockEntity neighbour = EssentiaTransfers.connectableNeighbor(this, side);
        if (!(neighbour instanceof IEssentiaTransport transport)) return false;
        if (!transport.canOutputTo(side.getOpposite())) return false;
        if (!LegacyTileRules.nozzleOutdraws(transport.getSuctionAmount(side.getOpposite()), getSuctionAmount(side))) return false;
        return transport.takeEssentia(Aspect.FIRE, 1, side.getOpposite()) == 1;
    }

    @Override public boolean isConnectable(Direction face) { return facing != null; }
    @Override public boolean canInputFrom(Direction face) { return facing != null; }
    @Override public boolean canOutputTo(Direction face) { return false; }
    @Override public void setSuction(Aspect aspect, int amount) { }
    @Override public boolean renderExtendedTube() { return false; }
    @Override public int getMinimumSuction() { return 0; }
    @Override public Aspect getSuctionType(Direction face) { return Aspect.FIRE; }

    /** TileArcaneFurnaceNozzle:102-114 - the suction the nozzle offers while the furnace is still hungry. */
    @Override public int getSuctionAmount(Direction face) {
        try {
            if (furnace != null && furnace.speedyTime < LegacyTileRules.NOZZLE_SUCTION_CUTOFF) return LegacyTileRules.NOZZLE_SUCTION;
        } catch (RuntimeException ignored) {
            // The original isolates a failing furnace lookup.
        }
        return 0;
    }

    @Override public Aspect getEssentiaType(Direction loc) { return null; }
    @Override public int getEssentiaAmount(Direction loc) { return 0; }
    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) { return 0; }
    @Override public int addEssentia(Aspect aspect, int amount, Direction face) { return 0; }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // The original never saves the facing or the furnace; they are worked out again on the first tick.
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        facing = null;
        furnace = null;
        drawDelay = 0;
    }
}
