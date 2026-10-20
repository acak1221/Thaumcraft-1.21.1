package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.BlockArcaneEar;
import thaumcraft.common.config.ThaumcraftArcaneEar;
import thaumcraft.common.lib.ArcaneEarNotes;

/** Original TileSensor: a matching note within 64 blocks retriggers ten ticks of power. Pitch/tone persist; the pulse does not. */
public final class TileSensor extends BlockEntity {
    private int note, tone, redstoneSignal;
    public TileSensor(BlockPos pos, BlockState state) { super(ThaumcraftArcaneEar.ARCANE_EAR_TILE.get(), pos, state); }
    public int getNote() { return note; }
    public int getTone() { return tone; }
    public int getRedstoneTicks() { return redstoneSignal; }

    public void changePitch() {
        if (level == null || level.isClientSide) return;
        note = (note + 1) % 25;
        changed();
    }
    public void updateTone() {
        if (level == null || level.isClientSide) return;
        BlockState under = level.getBlockState(worldPosition.below());
        int next = ArcaneEarNotes.legacyInstrument(under.instrument());
        if (next < 0) next = 0;
        // TC wooden blocks did not all declare a native note instrument before the ear existed.
        if (next == 0 && under.getSoundType() == SoundType.WOOD) next = 4;
        if (tone != next) { tone = next; changed(); }
    }
    public void triggerNote(boolean sound) {
        if (level != null && !level.isClientSide && level.getBlockState(worldPosition.above()).isAir()) {
            level.blockEvent(worldPosition, getBlockState().getBlock(), sound ? tone : -1, note);
        }
    }
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    private void signalChanged() {
        if (level == null || level.isClientSide || !(level.getBlockState(worldPosition).getBlock() instanceof BlockArcaneEar)) return;
        BlockState state = level.getBlockState(worldPosition);
        boolean powered = redstoneSignal > 0;
        if (state.getValue(BlockArcaneEar.POWERED) != powered) level.setBlock(worldPosition, state.setValue(BlockArcaneEar.POWERED, powered), Block.UPDATE_CLIENTS);
        level.updateNeighborsAt(worldPosition, state.getBlock());
        level.updateNeighborsAt(worldPosition.below(), state.getBlock());
    }
    public static void tick(Level level, BlockPos pos, BlockState state, TileSensor tile) {
        if (!(level instanceof ServerLevel server) || tile.isRemoved() || server.getBlockEntity(pos) != tile) return;
        if (tile.redstoneSignal > 0 && --tile.redstoneSignal == 0) tile.signalChanged();
        if (ArcaneEarNotes.matches(server, pos, tile.tone, tile.note)) {
            tile.triggerNote(false);
            tile.redstoneSignal = 10;
            tile.signalChanged();
        } else if (tile.redstoneSignal == 0 && state.getValue(BlockArcaneEar.POWERED)) {
            tile.signalChanged(); // A persisted powered model is transient, as the original counter was.
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putByte("note", (byte) note); tag.putByte("tone", (byte) tone);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        note = Math.max(0, Math.min(24, tag.getByte("note")));
        int storedTone = tag.getByte("tone");
        tone = storedTone >= 0 && storedTone <= 4 ? storedTone : 0;
        redstoneSignal = 0;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag(); tag.putByte("note", (byte) note); tag.putByte("tone", (byte) tone); return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
