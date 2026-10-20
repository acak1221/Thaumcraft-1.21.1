package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.BlockEssentiaLamp;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;

/** Native lamp-only private fuel transition. Public transport insertion/extraction remain zero as in TC4. */
public abstract class TileEssentiaLamp extends BlockEntity implements EssentiaAtomicEndpoint {
    protected enum Fill { NONE, GROWTH_FUEL, GROWTH_RESERVE, FERTILITY }
    protected int charges, drawDelay;
    protected boolean reserve, ticking;
    private Fill filling = Fill.NONE;
    private CompoundTag rejected;
    protected TileEssentiaLamp(BlockEntityType<?> type, BlockPos pos, BlockState state, int initialCharges) { super(type, pos, state); charges = initialCharges; }
    public final Direction getFacing() { return getBlockState().getValue(BlockEssentiaLamp.FACING); }
    public final int getCharges() { return charges; }
    public final boolean hasReserve() { return reserve; }
    public final int getDrawDelay() { return drawDelay; }
    public final boolean isQuarantined() { return rejected != null; }
    public final boolean isGrowth() { return this instanceof TileArcaneLampGrowth; }
    public final Aspect fuelAspect() { return isGrowth() ? Aspect.PLANT : Aspect.LIFE; }
    public final boolean isNativeDrawPhase() { return ticking && filling != Fill.NONE && !isQuarantined(); }
    public final boolean current() {
        return level instanceof ServerLevel server && server.getServer().isSameThread() && !isRemoved()
                && server.hasChunkAt(worldPosition) && server.getBlockEntity(worldPosition) == this
                && getType().isValid(getBlockState()) && server.getBlockState(worldPosition) == getBlockState();
    }
    public static void tick(Level level, BlockPos pos, BlockState state, TileEssentiaLamp tile) {
        if (level.isClientSide || tile.level != level || !tile.worldPosition.equals(pos) || tile.getBlockState() != state
                || tile.ticking || tile.isQuarantined() || EssentiaTransfers.isLocked(tile) || !tile.current()) return;
        tile.ticking = true;
        try { tile.tickLamp(); }
        finally { tile.filling = Fill.NONE; tile.ticking = false; }
    }
    protected abstract void tickLamp();
    protected final boolean draw(Fill purpose) {
        if (++drawDelay % 5 != 0) return false; // Original counts INVOCATIONS, potentially twice per growth-lamp tick.
        filling = purpose;
        try {
            var result = EssentiaTransfers.pullLampOne(this);
            if (result.failure() != null) throw result.failure();
            return result.committedUnits() == 1;
        } finally { filling = Fill.NONE; }
    }
    protected final void changed() {
        setChanged();
        if (!current()) return;
        BlockState before = getBlockState(), after = before.setValue(BlockEssentiaLamp.ACTIVE, charges > 0);
        if (after != before) level.setBlock(worldPosition, after, Block.UPDATE_ALL);
        else level.sendBlockUpdated(worldPosition, before, before, Block.UPDATE_CLIENTS);
    }
    @Override public final boolean isConnectable(Direction face) { return !isQuarantined() && face == getFacing(); }
    @Override public final boolean canInputFrom(Direction face) { return isConnectable(face); }
    @Override public final boolean canOutputTo(Direction face) { return false; }
    @Override public final void setSuction(Aspect aspect, int amount) {}
    @Override public final boolean renderExtendedTube() { return false; }
    @Override public final int getMinimumSuction() { return 0; }
    @Override public final Aspect getSuctionType(Direction face) { return fuelAspect(); }
    @Override public final int getSuctionAmount(Direction face) {
        return isQuarantined() || face != getFacing() ? 0 : isGrowth() ? (!reserve || charges <= 0 ? 128 : 0) : 128 - charges * 10;
    }
    @Override public final Aspect getEssentiaType(Direction face) { return null; }
    @Override public final int getEssentiaAmount(Direction face) { return 0; }
    @Override public final int takeEssentia(Aspect aspect, int amount, Direction face) { return 0; }
    @Override public final int addEssentia(Aspect aspect, int amount, Direction face) { return 0; }
    private record LampState(int charges, boolean reserve, int drawDelay, Direction facing, boolean quarantined) implements State {}
    @Override public final State snapshotAtomicState() { return new LampState(charges, reserve, drawDelay, getFacing(), isQuarantined()); }
    @Override public final Change prepareExtract(State before, Aspect aspect, int units) { return null; }
    @Override public final Change prepareInsert(State before, Aspect aspect, int units) {
        if (!isNativeDrawPhase() || units != 1 || aspect != fuelAspect() || !(before instanceof LampState saved) || saved.quarantined()) return null;
        int afterCharges = saved.charges(); boolean afterReserve = saved.reserve();
        if (isGrowth()) {
            if (filling == Fill.GROWTH_FUEL && saved.charges() <= 0 && !saved.reserve()) afterCharges = 100;
            else if (filling == Fill.GROWTH_RESERVE && !saved.reserve()) afterReserve = true;
            else return null;
        } else {
            if (filling != Fill.FERTILITY || saved.charges() >= 4) return null;
            afterCharges++;
        }
        return new Change(this, before, new LampState(afterCharges, afterReserve, saved.drawDelay(), saved.facing(), false), 1, true);
    }
    @Override public final void applyAtomicRaw(Change change) {
        EssentiaTransfers.requireAtomicChange(this, change);
        LampState after = (LampState) change.after(); charges = after.charges(); reserve = after.reserve();
    }
    @Override public final void notifyEssentiaStorageChanged() { changed(); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (ticking || EssentiaTransfers.isLocked(this)) throw new IllegalStateException("Lamp reload during native resource mutation");
        super.loadAdditional(tag, registries); rejected = null; charges = isGrowth() ? -1 : 0; reserve = false; drawDelay = 0; filling = Fill.NONE;
        if (tag.contains("RejectedEssentiaLamp")) rejected = tag.get("RejectedEssentiaLamp") instanceof CompoundTag saved ? saved.copy() : tag.copy();
        else if (!tag.contains("charges", Tag.TAG_INT) || tag.getInt("charges") < (isGrowth() ? -1 : 0) || tag.getInt("charges") > (isGrowth() ? 100 : 4)
                || isGrowth() && (!tag.contains("reserve", Tag.TAG_BYTE) || tag.getByte("reserve") < 0 || tag.getByte("reserve") > 1)) rejected = tag.copy();
        else { charges = tag.getInt("charges"); reserve = isGrowth() && tag.getBoolean("reserve"); }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (rejected != null) tag.put("RejectedEssentiaLamp", rejected.copy());
        else { tag.putInt("orientation", getFacing().get3DDataValue()); tag.putInt("charges", charges); if (isGrowth()) tag.putBoolean("reserve", reserve); }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
