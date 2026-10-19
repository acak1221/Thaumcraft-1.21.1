package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;

/** Original IN1/OUT1 centrifuge: one input becomes ONE immediate component.
 * Raw positive input is deliberately face/power/output-blind; ordinary automated
 * drawing is not. No representative singleton, recursive split or output queue. */
public final class TileCentrifuge extends BlockEntity implements IAspectContainer, EssentiaAtomicEndpoint {
    private static final SoundEvent PUMP = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "pump"));
    private Aspect input, output;
    private Direction facing = Direction.NORTH;
    private int process, count;
    private float rotation, rotationSpeed;
    private CompoundTag rejected;
    private boolean ticking, drawPhase, completionPhase;

    public TileCentrifuge(BlockPos pos, BlockState state) { super(ThaumcraftTubes.CENTRIFUGE_BLOCK_ENTITY.get(), pos, state); }
    public Aspect getInputAspect() { return input; }
    public Aspect getOutputAspect() { return output; }
    public int getProcess() { return process; }
    /** Original idle counter, NOT elapsed world time or processing ticks. */
    public int getTickCount() { return count; }
    public Direction getFacing() { return facing; }
    public float getRotation() { return rotation; }
    public float getRotationSpeed() { return rotationSpeed; }
    public boolean isPowered() { return level != null && level.hasNeighborSignal(worldPosition); }
    public boolean isQuarantined() { return rejected != null; }

    @Override public AspectList getAspects() { return output == null ? new AspectList() : new AspectList().add(output, 1); }
    @Override public void setAspects(AspectList aspects) {}
    @Override public boolean doesContainerAccept(Aspect aspect) { return !isQuarantined() && EssentiaAtomicEndpoint.validAspect(aspect); }
    @Override public int addToContainer(Aspect aspect, int amount) {
        if (amount <= 0 || !canMutate() || !EssentiaAtomicEndpoint.validAspect(aspect)) return amount;
        return amount - changedUnits(EssentiaTransfers.changeCentrifuge(this, EssentiaTransfers.CentrifugeMutation.OUTPUT_INSERT, aspect, amount));
    }
    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        // The oracle ignores amount, removes one and can falsely report N paid.
        // An exact boolean container request must really account for that one.
        if (amount != 1 || !canMutate() || !EssentiaAtomicEndpoint.validAspect(aspect)) return false;
        return changedUnits(EssentiaTransfers.changeCentrifuge(this, EssentiaTransfers.CentrifugeMutation.OUTPUT_EXTRACT, aspect, 1)) == 1;
    }
    @Override @Deprecated public boolean takeFromContainer(AspectList aspects) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return amount == 1 && containerContains(aspect) == 1; }
    @Override @Deprecated public boolean doesContainerContain(AspectList aspects) {
        if (aspects == null || output == null) return false;
        // Preserve the original ANY matching output-type test, not an invented
        // all-types/amounts predicate. This never inspects processing input.
        for (Aspect aspect : aspects.getAspects()) if (containerContains(aspect) == 1) return true;
        return false;
    }
    @Override public int containerContains(Aspect aspect) { return EssentiaAtomicEndpoint.validAspect(aspect) && output == aspect ? 1 : 0; }
    @Override public boolean isConnectable(Direction face) { return !isQuarantined() && (face == Direction.DOWN || face == Direction.UP); }
    @Override public boolean canInputFrom(Direction face) { return !isQuarantined() && face == Direction.DOWN; }
    @Override public boolean canOutputTo(Direction face) { return !isQuarantined() && face == Direction.UP; }
    @Override public void setSuction(Aspect aspect, int amount) {}
    @Override public boolean renderExtendedTube() { return false; }
    @Override public int getMinimumSuction() { return 0; }
    @Override public Aspect getSuctionType(Direction face) { return null; }
    @Override public int getSuctionAmount(Direction face) { return face == Direction.DOWN && !isQuarantined() && !isPowered() ? input == null ? 128 : 64 : 0; }
    /** Original public output is visible even on DOWN/side/null; permission is separate. */
    @Override public Aspect getEssentiaType(Direction face) { return output; }
    @Override public int getEssentiaAmount(Direction face) { return output == null ? 0 : 1; }
    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (amount <= 0 || !canOutputTo(face) || !canMutate() || !EssentiaAtomicEndpoint.validAspect(aspect)) return 0;
        return changedUnits(EssentiaTransfers.changeCentrifuge(this, EssentiaTransfers.CentrifugeMutation.OUTPUT_EXTRACT, aspect, amount));
    }
    @Override public int addEssentia(Aspect aspect, int amount, Direction face) {
        // Refuse the oracle's free amount0/negative input creation, NOT valid
        // positive input through a lateral/null face, power or occupied output.
        if (amount <= 0 || !canMutate() || !validInput(aspect)) return 0;
        return changedUnits(EssentiaTransfers.changeCentrifuge(this, EssentiaTransfers.CentrifugeMutation.INPUT, aspect, amount));
    }

    private record CentrifugeState(Aspect input, Aspect output, int process, int count, Direction facing, boolean quarantined) implements State {}
    @Override public State snapshotAtomicState() { return new CentrifugeState(input, output, process, count, facing, isQuarantined()); }
    @Override public Change prepareInsert(State before, Aspect aspect, int units) {
        if (!(before instanceof CentrifugeState saved) || saved.quarantined() || units != 1 || saved.input() != null || !validInput(aspect)) return null;
        return change(saved, aspect, saved.output(), 39);
    }
    @Override public Change prepareExtract(State before, Aspect aspect, int units) {
        if (!(before instanceof CentrifugeState saved) || saved.quarantined() || units != 1 || !EssentiaAtomicEndpoint.validAspect(aspect) || saved.output() != aspect) return null;
        return change(saved, saved.input(), null, saved.process());
    }
    /** Native container insertion targets OUT, not the transport's IN slot. */
    public Change prepareOutputInsert(State before, Aspect aspect) {
        if (!(before instanceof CentrifugeState saved) || saved.quarantined() || saved.output() != null || !EssentiaAtomicEndpoint.validAspect(aspect)) return null;
        return change(saved, saved.input(), aspect, saved.process());
    }
    /** Read-only authorization hook; no public setter can mint this tick phase. */
    public boolean isNativeDrawPhase() { return ticking && drawPhase && input == null && output == null && !isQuarantined(); }
    /** Only the actual unpowered native tick can authorize completion. Checking
     * all components before RNG prevents malformed/primal input from drawing. */
    public Aspect[] getNativeCompletionComponents(State before) {
        if (!ticking || !completionPhase || !(before instanceof CentrifugeState saved) || saved.quarantined()
                || saved.output() != null || saved.process() != 0) return null;
        return inputComponents(saved.input());
    }
    public Change prepareCompletion(State before, Aspect component) {
        Aspect[] components = getNativeCompletionComponents(before);
        if (components == null || !EssentiaAtomicEndpoint.validAspect(component) || component != components[0] && component != components[1]) return null;
        return change((CentrifugeState) before, null, component, 0);
    }
    private Change change(CentrifugeState before, Aspect afterInput, Aspect afterOutput, int afterProcess) {
        return new Change(this, before, new CentrifugeState(afterInput, afterOutput, afterProcess, before.count(), before.facing(), false), 1, true);
    }
    @Override public void applyAtomicRaw(Change change) {
        EssentiaTransfers.requireAtomicChange(this, change);
        CentrifugeState after = (CentrifugeState) change.after();
        input = after.input(); output = after.output(); process = after.process();
    }
    private static boolean validInput(Aspect aspect) { return inputComponents(aspect) != null; }
    private static Aspect[] inputComponents(Aspect aspect) {
        if (!EssentiaAtomicEndpoint.validAspect(aspect) || aspect.isPrimal()) return null;
        Aspect[] components = aspect.getComponents();
        return components != null && components.length == 2 && EssentiaAtomicEndpoint.validAspect(components[0]) && EssentiaAtomicEndpoint.validAspect(components[1]) ? components : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileCentrifuge tile) {
        if (!level.isClientSide && (!(level instanceof ServerLevel server) || !server.getServer().isSameThread())) throw new IllegalStateException("Centrifuge ticks require the server thread");
        if (tile.level != level || !tile.worldPosition.equals(pos) || state != tile.getBlockState() || !tile.current()
                || tile.isQuarantined() || tile.ticking || EssentiaTransfers.isLocked(tile)) return;
        tile.ticking = true;
        try {
            if (level.isClientSide) tile.tickClient();
            else tile.tickServer();
        } finally { tile.drawPhase = false; tile.completionPhase = false; tile.ticking = false; }
    }
    private void tickServer() {
        // Sample control ONCE at branch entry. Native draw's public suction
        // queries retain their original power semantics, but completion must not
        // add a new late power poll or let external callers ignore power.
        boolean powered = isPowered();
        if (powered || !current() || isQuarantined() || EssentiaTransfers.isLocked(this)) return;
        if (output == null && input == null && ++count % 5 == 0) {
            EssentiaTransfers.Attempt attempt;
            drawPhase = true;
            try { attempt = EssentiaTransfers.pullCentrifugeOne(this); }
            finally { drawPhase = false; }
            changedUnits(attempt);
        }
        if (!current() || isQuarantined() || EssentiaTransfers.isLocked(this)) return;
        if (process > 0) --process; // Including while OUT blocks completion.
        if (output == null && input != null && process == 0) {
            EssentiaTransfers.Attempt attempt;
            completionPhase = true;
            try { attempt = EssentiaTransfers.completeCentrifuge(this); }
            finally { completionPhase = false; }
            changedUnits(attempt);
        }
    }
    private void tickClient() {
        boolean powered = input != null && isPowered();
        if (!current() || isQuarantined()) return;
        // Literal original guards, not clamping/interpolation: fractional
        // spin-down followed by resumption can step slightly past20.
        if (input != null && !powered && rotationSpeed < 20) rotationSpeed += 2;
        if ((input == null || powered) && rotationSpeed > 0) rotationSpeed -= .5F;
        int previous = (int) rotation;
        rotation += rotationSpeed;
        if (rotation % 180 <= 20 && previous % 180 >= 160 && rotationSpeed > 0) {
            level.playLocalSound(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, PUMP, SoundSource.BLOCKS, 1, 1, false);
        }
    }
    private int changedUnits(EssentiaTransfers.Attempt attempt) {
        if (attempt.failure() != null) thaumcraft.common.Thaumcraft.LOG.warn("Centrifuge attempt failed at {} (committed {})", worldPosition, attempt.committedUnits(), attempt.failure());
        return attempt.committedUnits();
    }
    private boolean current() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this && level.getBlockState(worldPosition) == getBlockState(); }
    private boolean canMutate() {
        if (isQuarantined() || isRemoved() || EssentiaTransfers.isLocked(this) || level == null || level.isClientSide) return false;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Centrifuge changes require the server thread");
        return current();
    }
    @Override public void notifyEssentiaStorageChanged() {
        if (!current() || level.isClientSide) return;
        setChanged();
        if (current()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); writeCustom(tag); }
    private void writeCustom(CompoundTag tag) {
        if (input != null) tag.putString("aspectIn", input.getTag());
        if (output != null) tag.putString("aspectOut", output.getTag());
        tag.putInt("facing", facing.get3DDataValue());
        if (rejected != null) tag.put("RejectedCentrifuge", rejected.copy());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // Disk loading is not an escape hatch from active stock/tick authority.
        if (level != null && !level.isClientSide) {
            if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Centrifuge reloads require the server thread");
            if (ticking || EssentiaTransfers.isLocked(this)) throw new IllegalStateException("Cannot reload a centrifuge during an active native mutation");
        }
        super.loadAdditional(tag, registries);
        if (level == null || !level.isClientSide) { process = 0; count = 0; rotation = 0; rotationSpeed = 0; }
        if (tag.contains("RejectedCentrifuge")) { quarantine(tag.get("RejectedCentrifuge") instanceof CompoundTag saved ? saved : tag); return; }
        try {
            Aspect parsedInput = readAspect(tag, "aspectIn"), parsedOutput = readAspect(tag, "aspectOut");
            if (parsedInput != null && !validInput(parsedInput)) throw new IllegalArgumentException("Invalid processing input");
            Direction parsedFacing = Direction.NORTH;
            if (tag.contains("facing")) {
                int side = tag.getInt("facing");
                if (!tag.contains("facing", Tag.TAG_INT) || side < 0 || side > 5) throw new IllegalArgumentException("Invalid facing");
                parsedFacing = Direction.from3DDataValue(side);
            }
            input = parsedInput; output = parsedOutput; facing = parsedFacing; rejected = null;
        } catch (IllegalArgumentException malformed) {
            // Retain the WHOLE payload for repair. Never delete just an invalid
            // slot, truncate units, invent a component or restart process39.
            quarantine(tag);
            thaumcraft.common.Thaumcraft.LOG.warn("Quarantined malformed centrifuge data at {}", worldPosition);
        }
    }
    private static Aspect readAspect(CompoundTag tag, String name) {
        if (!tag.contains(name)) return null;
        if (!tag.contains(name, Tag.TAG_STRING)) throw new IllegalArgumentException("Invalid " + name);
        Aspect aspect = Aspect.getAspect(tag.getString(name));
        if (!EssentiaAtomicEndpoint.validAspect(aspect)) throw new IllegalArgumentException("Invalid " + name);
        return aspect;
    }
    private void quarantine(CompoundTag payload) {
        rejected = payload.copy(); input = null; output = null; facing = Direction.NORTH;
        process = 0; count = 0; rotation = 0; rotationSpeed = 0;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { var tag = new CompoundTag(); writeCustom(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
