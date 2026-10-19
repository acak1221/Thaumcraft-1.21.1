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
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.blocks.BlockEssentiaCrystalizer;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;

/** Original one-unit crystallizer. Actual Terra CV grants accelerate each fifth
 * unpowered processing tick; no source retains the original slow baseline. */
public final class TileEssentiaCrystalizer extends BlockEntity implements IAspectContainer, EssentiaAtomicEndpoint {
    private Aspect aspect;
    private Direction facing;
    private int progress, count, venting;
    private float spin, spinIncrement, red = 1, green = 1, blue = 1;
    private boolean ticking, fillPhase, finishPhase, drawingVis;
    private CompoundTag rejected;
    private static ClientEffects clientEffects;

    @FunctionalInterface public interface ClientEffects {
        /** Original white vent, six client world-RNG jitter draws per tick. */
        void vent(Level level, BlockPos pos, Direction outward);
    }
    public static void setClientEffects(ClientEffects effects) { clientEffects = effects; }
    public TileEssentiaCrystalizer(BlockPos pos, BlockState state) {
        super(ThaumcraftTubes.CRYSTALLIZER_BLOCK_ENTITY.get(), pos, state);
        facing = state.hasProperty(BlockEssentiaCrystalizer.FACING) ? state.getValue(BlockEssentiaCrystalizer.FACING) : Direction.DOWN;
    }
    public Aspect getAspect() { return aspect; }
    public int getProgress() { return progress; }
    public int getTickCount() { return count; }
    public Direction getFacing() { return facing; }
    public float getSpin() { return spin; }
    public float getSpinIncrement() { return spinIncrement; }
    public float getRed() { return red; }
    public float getGreen() { return green; }
    public float getBlue() { return blue; }
    public int getVenting() { return venting; }
    public boolean isPowered() { return level != null && level.hasNeighborSignal(worldPosition); }
    public boolean isQuarantined() { return rejected != null; }
    @Override public AspectList getAspects() { return aspect == null ? new AspectList() : new AspectList().add(aspect, 1); }
    @Override public void setAspects(AspectList aspects) {}
    @Override public boolean doesContainerAccept(Aspect type) { return !isQuarantined() && EssentiaAtomicEndpoint.validAspect(type); }
    @Override public int addToContainer(Aspect type, int amount) {
        if (amount <= 0 || !EssentiaAtomicEndpoint.validAspect(type) || !canMutate()) return amount;
        return amount - report(EssentiaTransfers.changeCrystallizer(this, type, amount, true));
    }
    @Override public boolean takeFromContainer(Aspect type, int amount) {
        // Explicit authority fix: original quantity1 removes ANY stored type.
        // Refuse a wrong/null type instead of charging the wrong physical unit.
        return amount == 1 && EssentiaAtomicEndpoint.validAspect(type) && canMutate()
                && report(EssentiaTransfers.changeCrystallizer(this, type, 1, false)) == 1;
    }
    @Override @Deprecated public boolean takeFromContainer(AspectList types) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect type, int amount) { return amount == 1 && containerContains(type) == 1; }
    @Override public int containerContains(Aspect type) { return EssentiaAtomicEndpoint.validAspect(type) && aspect == type ? 1 : 0; }
    @Override @Deprecated public boolean doesContainerContain(AspectList types) {
        if (types == null) return false;
        // Original ALL predicate, with vacuous success for an empty list.
        for (Aspect type : types.getAspects()) if (containerContains(type) != 1 || types.getAmount(type) != 1) return false;
        return true;
    }
    @Override public boolean isConnectable(Direction face) { return !isQuarantined() && face == facing; }
    @Override public boolean canInputFrom(Direction face) { return isConnectable(face); }
    @Override public boolean canOutputTo(Direction face) { return false; }
    @Override public void setSuction(Aspect type, int amount) {}
    @Override public Aspect getSuctionType(Direction face) { return null; }
    @Override public int getMinimumSuction() { return 0; }
    @Override public boolean renderExtendedTube() { return false; }
    @Override public int getSuctionAmount(Direction face) { return isQuarantined() || isPowered() ? 0 : aspect == null && face == facing ? 128 : 64; }
    @Override public Aspect getEssentiaType(Direction face) { return aspect; }
    @Override public int getEssentiaAmount(Direction face) { return aspect == null ? 0 : 1; }
    @Override public int takeEssentia(Aspect type, int amount, Direction face) { return 0; }
    @Override public int addEssentia(Aspect type, int amount, Direction face) { return canInputFrom(face) && amount > 0 ? amount - addToContainer(type, amount) : 0; }

    private record CrystalState(Aspect aspect, int progress, int count, Direction facing, boolean quarantined) implements State {}
    @Override public State snapshotAtomicState() { return new CrystalState(aspect, progress, count, facing, isQuarantined()); }
    @Override public Change prepareInsert(State before, Aspect type, int units) {
        if (drawingVis || !(before instanceof CrystalState saved) || saved.quarantined() || saved.aspect() != null || units != 1 || !EssentiaAtomicEndpoint.validAspect(type)) return null;
        return change(saved, type, saved.progress()); // Raw input does NOT reset old progress.
    }
    @Override public Change prepareExtract(State before, Aspect type, int units) {
        if (drawingVis || !(before instanceof CrystalState saved) || saved.quarantined() || units != 1 || !EssentiaAtomicEndpoint.validAspect(type) || saved.aspect() != type) return null;
        return change(saved, null, saved.progress());
    }
    public boolean isNativeFillPhase() { return ticking && fillPhase && aspect == null && !isQuarantined(); }
    public Aspect getNativeCompletionAspect(State before) {
        return ticking && finishPhase && before instanceof CrystalState saved && !saved.quarantined() && saved.progress() >= 200
                && EssentiaAtomicEndpoint.validAspect(saved.aspect()) ? saved.aspect() : null;
    }
    public Change prepareCompletion(State before) { return getNativeCompletionAspect(before) == null ? null : change((CrystalState) before, null, 0); }
    private Change change(CrystalState before, Aspect after, int afterProgress) {
        return new Change(this, before, new CrystalState(after, afterProgress, before.count(), before.facing(), false), 1, true);
    }
    @Override public void applyAtomicRaw(Change change) {
        EssentiaTransfers.requireAtomicChange(this, change);
        CrystalState after = (CrystalState) change.after(); aspect = after.aspect(); progress = after.progress();
    }
    public static void tick(Level level, BlockPos pos, BlockState state, TileEssentiaCrystalizer tile) {
        if (!level.isClientSide && (!(level instanceof ServerLevel server) || !server.getServer().isSameThread())) throw new IllegalStateException("Crystallizer ticks require the server thread");
        if (tile.level != level || !tile.worldPosition.equals(pos) || state != tile.getBlockState() || !tile.current() || tile.ticking || tile.isQuarantined() || EssentiaTransfers.isLocked(tile)) return;
        tile.ticking = true;
        try { if (level.isClientSide) tile.tickClient(); else tile.tickServer(); }
        finally { tile.ticking = false; tile.fillPhase = false; tile.finishPhase = false; tile.drawingVis = false; }
    }
    private void tickServer() {
        if (++count % 5 == 0 && !isPowered() && current() && !EssentiaTransfers.isLocked(this)) {
            if (aspect == null) {
                fillPhase = true;
                try { report(EssentiaTransfers.pullCrystallizerOne(this)); }
                finally { fillPhase = false; }
                if (!current() || isQuarantined()) return;
                progress = 0; // Including a SUCCESSFUL same-tick native fill.
            } else {
                // Original217: the network's raw int is centi-vis-compatible,
                // NOT whole node-vis and never multiplied/divided by100 here.
                var before = snapshotAtomicState();
                int request = Math.min(20, Math.max(1, (200 - progress) / 2));
                int drained;
                drawingVis = true;
                try { drained = VisNetHandler.drainVis(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), Aspect.EARTH, request); }
                finally { drawingVis = false; }
                // A post-debit effect may remove/replace this machine. Do not
                // credit a different input or resurrect the old owner; CV which
                // was already spent is not refunded or drained a second time.
                if (!current() || isQuarantined() || !matchesAtomicState(before)) return;
                if (drained < 0 || drained > request) throw new IllegalStateException("Invalid actual VisNet grant");
                progress += 1 + drained * 2;
            }
        }
        if (!current() || isQuarantined() || EssentiaTransfers.isLocked(this)) return;
        // Original completion is OUTSIDE the fifth-tick AND power branches.
        if (aspect != null && progress >= 200) {
            finishPhase = true;
            try { report(EssentiaTransfers.finishCrystallizer(this)); }
            finally { finishPhase = false; }
        }
    }
    private void tickClient() {
        int rgb = aspect == null ? 0 : aspect.getColor();
        float r = aspect == null ? 1 : (rgb >> 16 & 255) / 220F;
        float g = aspect == null ? 1 : (rgb >> 8 & 255) / 220F;
        float b = aspect == null ? 1 : (rgb & 255) / 220F;
        if (red < r) red += .05F; if (red > r) red -= .05F;
        if (green < g) green += .05F; if (green > g) green -= .05F;
        if (blue < b) blue += .05F; if (blue > b) blue -= .05F;
        spin += spinIncrement; if (spin > 360) spin -= 360;
        if (aspect != null && spinIncrement < 20 && !isPowered()) spinIncrement = Math.min(20, spinIncrement + .1F);
        else if ((aspect == null || isPowered()) && spinIncrement > 0) spinIncrement = Math.max(0, spinIncrement - .2F);
        if (venting > 0) { --venting; if (clientEffects != null) clientEffects.vent(level, worldPosition, facing.getOpposite()); }
    }
    @Override public boolean triggerEvent(int id, int data) {
        if (id >= 0) { if (level != null && level.isClientSide) venting = 7; return true; }
        return super.triggerEvent(id, data);
    }
    private int report(EssentiaTransfers.Attempt attempt) {
        if (attempt.failure() != null) thaumcraft.common.Thaumcraft.LOG.warn("Crystallizer attempt failed at {} (committed {})", worldPosition, attempt.committedUnits(), attempt.failure());
        return attempt.committedUnits();
    }
    private boolean current() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this && level.getBlockState(worldPosition) == getBlockState(); }
    private boolean canMutate() {
        if (level == null || level.isClientSide || isRemoved() || isQuarantined() || drawingVis || EssentiaTransfers.isLocked(this)) return false;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Crystallizer mutations require the server thread");
        return current();
    }
    @Override public void notifyEssentiaStorageChanged() {
        if (!current() || level.isClientSide) return;
        setChanged(); if (current()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); writeCustom(tag); }
    private void writeCustom(CompoundTag tag) {
        if (aspect != null) tag.putString("Aspect", aspect.getTag());
        tag.putByte("face", (byte) facing.get3DDataValue());
        if (rejected != null) tag.put("RejectedCrystallizer", rejected.copy());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // Includes detached loadStatic targets: an old saved input cannot be
        // resurrected in a new owner while its paid completion is still active.
        if (EssentiaTransfers.isMutationActive()) throw new IllegalStateException("Crystallizer stock cannot be restored during an active mutation");
        if (level != null && !level.isClientSide) {
            if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread() || ticking || EssentiaTransfers.isLocked(this)) throw new IllegalStateException("Crystallizer reload cannot bypass active server mutation");
        }
        super.loadAdditional(tag, registries);
        if (level == null || !level.isClientSide) { progress = 0; count = 0; spin = 0; spinIncrement = 0; red = green = blue = 1; venting = 0; }
        if (tag.contains("RejectedCrystallizer")) { quarantine(tag.get("RejectedCrystallizer") instanceof CompoundTag saved ? saved : tag); return; }
        try {
            Aspect parsed = null;
            if (tag.contains("Aspect")) {
                if (!tag.contains("Aspect", Tag.TAG_STRING) || !EssentiaAtomicEndpoint.validAspect(parsed = Aspect.getAspect(tag.getString("Aspect")))) throw new IllegalArgumentException("Invalid stored aspect");
            }
            Direction parsedFace = getBlockState().hasProperty(BlockEssentiaCrystalizer.FACING) ? getBlockState().getValue(BlockEssentiaCrystalizer.FACING) : Direction.DOWN;
            if (tag.contains("face")) {
                byte side = tag.getByte("face");
                if (!tag.contains("face", Tag.TAG_BYTE) || side < 0 || side > 5) throw new IllegalArgumentException("Invalid facing");
                parsedFace = Direction.from3DDataValue(side);
            }
            aspect = parsed; facing = parsedFace; rejected = null;
        } catch (IllegalArgumentException malformed) {
            quarantine(tag); thaumcraft.common.Thaumcraft.LOG.warn("Quarantined malformed crystallizer at {}", worldPosition);
        }
    }
    private void quarantine(CompoundTag payload) {
        rejected = payload.copy(); aspect = null; facing = Direction.DOWN; progress = count = venting = 0;
        spin = spinIncrement = 0; red = green = blue = 1;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { var tag = new CompoundTag(); writeCustom(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
