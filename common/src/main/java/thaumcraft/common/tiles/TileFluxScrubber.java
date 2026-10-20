package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftEldritchBlocks;
import thaumcraft.common.lib.world.EldritchBlockRules;

/**
 * Original TileFluxScrubber: the flux scrubber. It removes the flux blocks
 * around it and turns them into essentia.
 */
public final class TileFluxScrubber extends BlockEntity {
    public int essentia = 0;
    public int charges = 0;
    public int power = 0;
    public Direction facing = Direction.DOWN;

    public TileFluxScrubber(BlockPos pos, BlockState state) {
        super(ThaumcraftEldritchBlocks.FLUX_SCRUBBER_TILE.get(), pos, state);
    }

    public int getEssentia() { return essentia; }
    public int getCharges() { return charges; }
    public int getPower() { return power; }
    public Direction getFacing() { return facing; }

    public void setFacing(Direction facing) {
        this.facing = facing == null ? Direction.DOWN : facing;
        setChanged();
    }

    /** Original TileFluxScrubber:33-53 - the scrubber collects and spends its charges. */
    public static void tick(Level level, BlockPos pos, BlockState state, TileFluxScrubber scrubber) {
        if (level.isClientSide) return;
        if (scrubber.charges >= EldritchBlockRules.FLUX_CHARGE_COST) {
            scrubber.charges -= EldritchBlockRules.FLUX_CHARGE_COST;
            if (level.getRandom().nextInt(EldritchBlockRules.FLUX_ESSENTIA_CHANCE) == 0) {
                scrubber.essentia = EldritchBlockRules.fluxScrubberEssentiaAfterCharge(scrubber.essentia);
                scrubber.setChanged();
            }
        }
        if (scrubber.power >= EldritchBlockRules.FLUX_POWER_MIN) {
            scrubber.checkFlux(level, pos);
        }
    }

    /** Original TileFluxScrubber:55-88 - the flux blocks around the scrubber are removed. */
    private void checkFlux(Level level, BlockPos pos) {
        int distance = EldritchBlockRules.FLUX_SCAN_DISTANCE;
        outer:
        for (int a = -distance; a <= distance; a++) {
            for (int b = -distance; b <= distance; b++) {
                for (int c = -distance; c <= distance; c++) {
                    BlockPos target = new BlockPos(pos.getX() + a, pos.getY() + b, pos.getZ() + c);
                    if (level.getBlockState(target).isAir()) continue;
                    if (!isFlux(level, target)) continue;
                    power -= EldritchBlockRules.FLUX_POWER_PER_BLOCK;
                    BlockState fluxState = level.getBlockState(target);
                    if (fluxState.getBlock() instanceof thaumcraft.common.blocks.FluxFiniteBlock) {
                        // The original metadata of the flux block is its quanta.
                        int quanta = thaumcraft.common.blocks.FluxFiniteBlock.quanta(fluxState);
                        int reduced = EldritchBlockRules.fluxScrubberReducedLevel(quanta);
                        if (reduced < 0) {
                            level.destroyBlock(target, false);
                        } else {
                            level.setBlock(target, fluxState.setValue(thaumcraft.common.blocks.FluxFiniteBlock.LEVEL, reduced - 1),
                                    Block.UPDATE_ALL);
                        }
                    }
                    charges++;
                    setChanged();
                    break outer;
                }
            }
        }
    }

    /** Original TileFluxScrubber:90-93 - only the flux goo is removed. */
    private boolean isFlux(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof thaumcraft.common.blocks.BlockFluxGoo;
    }

    /** Original TileFluxScrubber:170-176 - the essentia is taken from the front. */
    public int takeEssentia(int amount) {
        int taken = Math.min(essentia, amount);
        essentia -= taken;
        setChanged();
        return taken;
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        facing = Direction.from3DDataValue(tag.getByte("facing"));
        charges = tag.getInt("charges");
        power = tag.getInt("power");
        essentia = tag.getInt("essentia");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("facing", (byte) facing.get3DDataValue());
        tag.putInt("charges", charges);
        tag.putInt("power", power);
        tag.putInt("essentia", essentia);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
