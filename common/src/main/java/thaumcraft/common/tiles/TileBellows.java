package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockBellows;
import thaumcraft.common.config.ThaumcraftBellows;
import thaumcraft.common.mixin.VanillaFurnaceBellowsAccess;

/** Original TileBellows:29-92; unpowered oscillation and one vanilla cooking tick per two ticks. */
public final class TileBellows extends BlockEntity {
    private float inflation = 1;
    private boolean expanding, firstRun = true, onVanillaFurnace;
    private int delay;
    public TileBellows(BlockPos pos, BlockState state) { super(ThaumcraftBellows.BELLOWS_TILE.get(), pos, state); }
    public float getInflation() { return inflation; }
    public Direction getFacing() { return getBlockState().getValue(BlockBellows.FACING); }
    public boolean isPowered() { return level != null && level.hasNeighborSignal(worldPosition); }
    public void setOnVanillaFurnace(boolean value) {
        onVanillaFurnace = value;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    public static void tick(Level level, BlockPos pos, BlockState state, TileBellows tile) {
        if (tile.isRemoved() || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != tile || tile.isPowered()) return;
        if (level.isClientSide) {
            if (tile.firstRun) tile.inflation = .35F + level.random.nextFloat() * .55F;
            tile.firstRun = false;
            if (tile.inflation > .35F && !tile.expanding) tile.inflation -= .075F;
            if (tile.inflation <= .35F && !tile.expanding) tile.expanding = true;
            if (tile.inflation < 1 && tile.expanding) tile.inflation += .025F;
            if (tile.inflation >= 1 && tile.expanding) {
                tile.expanding = false;
                level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, SoundEvents.GHAST_SHOOT,
                        SoundSource.BLOCKS, .01F, .5F + (level.random.nextFloat() - level.random.nextFloat()) * .2F, false);
            }
        } else if (tile.onVanillaFurnace && ++tile.delay >= 2) {
            tile.delay = 0;
            Direction direction = tile.getFacing();
            // Original :55 intentionally ignores offsetY, even with UP/DOWN.
            BlockPos target = pos.offset(direction.getStepX(), 0, direction.getStepZ());
            if (level.hasChunkAt(target) && level.getBlockState(target).is(Blocks.FURNACE)
                    && level.getBlockEntity(target) instanceof FurnaceBlockEntity furnace) {
                var data = (VanillaFurnaceBellowsAccess) furnace;
                int progress = data.thaumcraft$getCookingProgress();
                // Modern datapacks may change duration: never skip the native
                // equality completion tick. For vanilla200 this is original199.
                if (progress > 0 && progress < Math.min(199, data.thaumcraft$getCookingTotalTime() - 1)) {
                    data.thaumcraft$setCookingProgress(progress + 1);
                    furnace.setChanged();
                }
            }
        }
    }
    public static int getBellows(Level level, BlockPos target, Iterable<Direction> sides) {
        int count = 0;
        for (Direction direction : sides) {
            BlockPos pos = target.relative(direction);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof TileBellows bellows
                    && bellows.getFacing() == direction.getOpposite() && !bellows.isPowered()) count++;
        }
        return count;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("orientation", (byte) getFacing().get3DDataValue());
        tag.putBoolean("onVanillaFurnace", onVanillaFurnace);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        onVanillaFurnace = tag.getBoolean("onVanillaFurnace");
        // Native facing is saved in block state. No second divergent direction
        // is admitted from item/custom NBT; only fresh native worlds are supported.
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
