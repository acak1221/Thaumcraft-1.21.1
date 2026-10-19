package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.BlockTube;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.TubeGeometry;

/** TC4 single-unit variants share one buffer and one local suction/transfer phase. */
public sealed class TileTube extends BlockEntity implements EssentiaStorageEndpoint permits TileTubeFilter {
    private static final SoundEvent SQUEEK = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "squeek"));
    private Direction facing = Direction.NORTH;
    private final boolean[] openSides = {true, true, true, true, true, true};
    private Aspect essentiaType, suctionType, filter;
    private int essentiaAmount, suction;
    private int count, venting, ventColor;
    private boolean allowFlow = true, powered;
    private float rotation;
    private static ClientEffects clientEffects;

    /** Client implementation owns original particles/audio; no client classes load on a server. */
    public interface ClientEffects {
        void tick(Level level, BlockPos pos, int venting, int color);
        /** Event0=creak; event1=fizz only when startingVenting, with the resolved aspect color. */
        default void event(Level level, BlockPos pos, int event, int color, boolean startingVenting) {}
    }

    public static void setClientEffects(ClientEffects effects) { clientEffects = effects; }

    public TileTube(BlockPos pos, BlockState state) {
        super(ThaumcraftTubes.TUBE_BLOCK_ENTITY.get(), pos, state);
    }

    /** Also used by the registered BE type, including vanilla loadStatic. */
    public static TileTube create(BlockPos pos, BlockState state) {
        return state.getBlock() instanceof BlockTube tube && tube.kind() == BlockTube.Kind.FILTER
                ? new TileTubeFilter(pos, state) : new TileTube(pos, state);
    }

    public Direction getFacing() { return facing; }
    public int getVenting() { return venting; }
    public int getVentColor() { return ventColor; }
    public int getTickCount() { return count; }
    /** The registered block owns the kind; item/save data cannot grant variant behavior. */
    public BlockTube.Kind kind() { return getBlockState().getBlock() instanceof BlockTube tube ? tube.kind() : BlockTube.Kind.BASIC; }
    public boolean isValve() { return kind() == BlockTube.Kind.VALVE; }
    public boolean isFilter() { return kind() == BlockTube.Kind.FILTER; }
    public boolean isRestrict() { return kind() == BlockTube.Kind.RESTRICT; }
    public boolean isOneWay() { return kind() == BlockTube.Kind.ONEWAY; }
    public Aspect getFilter() { return isFilter() ? filter : null; }
    public boolean isSideOpen(Direction face) { return face != null && openSides[face.get3DDataValue()]; }
    public boolean isFlowAllowed() { return allowFlow; }
    public boolean hadPower() { return powered; }
    public float getRotation() { return rotation; }

    /** Field-only label ownership write, not an unpaid configuration API. */
    public final void setFilterRaw(Aspect filter) {
        EssentiaTransfers.requireTubeFilterMutation(this);
        this.filter = filter;
    }

    public boolean toggleFlow() { return setFlowAllowed(!allowFlow); }

    /** Closing retains both the buffered unit and old suction until the next calculation. */
    public boolean setFlowAllowed(boolean allowed) {
        if (!isValve() || !canMutate()) return false;
        if (allowFlow == allowed) return true;
        allowFlow = allowed;
        changed();
        squeek();
        return true;
    }

    public boolean setFacing(Direction face) {
        if (!canMutate() || face == null) return false;
        if (facing == face) return true;
        facing = face;
        changed();
        return true;
    }

    /** A valve seeks an unoccupied stem side; a basic tube seeks an open opposite transport. */
    public boolean cycleFacing() {
        if (!canMutate() || level == null) return false;
        for (int next = facing.get3DDataValue() + 1; next < 20; next++) {
            Direction candidate = Direction.from3DDataValue(next % 6), input = candidate.getOpposite();
            if (isValve() ? TubeGeometry.hasTransportNeighbor(level, worldPosition, candidate)
                    : !isConnectable(input) || !TubeGeometry.hasTransportNeighbor(level, worldPosition, input)) continue;
            return setFacing(candidate);
        }
        // Original core clicks still dirty/swing/play the tool sound if no
        // connected facing can be found; they do not become a new use gate.
        changed();
        return true;
    }

    /** Both tubes receive the SAME open value on their opposing sides, before notifying either. */
    public boolean setSideOpen(Direction face, boolean open) {
        if (!canMutate() || face == null) return false;
        if (level == null) { openSides[face.get3DDataValue()] = open; changed(); return true; }
        return EssentiaTransfers.setTubeSideOpen(this, face, open);
    }

    public final void setSideOpenRaw(Direction face, boolean open) {
        EssentiaTransfers.requireTubeSideMutation(this);
        openSides[face.get3DDataValue()] = open;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileTube tube) {
        if (tube.level != level || !pos.equals(tube.worldPosition) || tube.isRemoved() || !level.hasChunkAt(pos)
                || level.getBlockEntity(pos) != tube || level.getBlockState(pos) != state || tube.getBlockState() != state
                || EssentiaTransfers.isLocked(tube) || !level.isClientSide && !tube.canMutate()) return;
        if (tube.isValve()) {
            // TileTubeValve:34-57 runs BEFORE the shared count seed/decrement.
            // Venting can freeze a divisible phase (sample every tick) or any
            // other phase (no sample); there is deliberately no second timer.
            if (level.isClientSide) {
                if (!tube.allowFlow && tube.rotation < 360) tube.rotation += 20;
                else if (tube.allowFlow && tube.rotation > 0) tube.rotation -= 20;
            } else if (tube.count % 5 == 0) {
                tube.samplePower();
                if (!tube.canMutate()) return;
            }
        }
        if (tube.venting > 0) tube.venting--;
        if (tube.count == 0) tube.count = level.random.nextInt(10);
        if (level.isClientSide) {
            if (tube.venting > 0 && clientEffects != null) clientEffects.tick(level, pos, tube.venting, tube.ventColor);
            return;
        }
        if (!tube.canMutate() || tube.venting > 0) return;
        if (++tube.count % 2 == 0) {
            tube.calculateSuction();
            tube.checkVenting();
            if (tube.essentiaType != null && tube.essentiaAmount == 0) tube.essentiaType = null;
        }
        // Do NOT recheck venting here: the conflict-starting tick may still pull.
        if (tube.count % 5 == 0 && tube.suction > 0) tube.equalizeWithNeighbours();
    }

    private void samplePower() {
        boolean nextPower = level.hasNeighborSignal(worldPosition);
        if (!canMutate()) return;
        boolean nextFlow = allowFlow;
        if (powered && !nextPower && !allowFlow) nextFlow = true;
        if (!powered && nextPower && allowFlow) nextFlow = false;
        boolean flowChanged = allowFlow != nextFlow, powerChanged = powered != nextPower;
        // Commit the edge latch before any update callback can observe it.
        powered = nextPower;
        allowFlow = nextFlow;
        if (flowChanged || powerChanged) changed();
        if (flowChanged) squeek();
    }

    private void squeek() {
        if (level != null && !level.isClientSide && !isRemoved() && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this) {
            level.playSound(null, worldPosition, SQUEEK, SoundSource.BLOCKS, .7F, .9F + level.random.nextFloat() * .2F);
        }
    }

    /** Original TileTube:133-155; the variants change policy, never the shared phase. */
    private void calculateSuction() {
        suction = 0;
        suctionType = null;
        Aspect configured = getFilter();
        for (Direction face : Direction.values()) {
            try {
                if (isOneWay() && face != facing.getOpposite() || !isConnectable(face)) continue;
                BlockEntity neighbor = EssentiaTransfers.connectableNeighbor(this, face);
                if (!(neighbor instanceof IEssentiaTransport transport)) continue;
                Direction opposite = face.getOpposite();
                Aspect type = transport.getSuctionType(opposite);
                if (configured != null && type != null && configured != type) continue;
                if (essentiaAmount > 0 && type != null && essentiaType != type
                        && (configured == null || essentiaType != null)) continue;
                int amount = transport.getSuctionAmount(opposite);
                // Compare against the ALREADY reduced value on every side:
                // restrict candidates64 then35 deliberately produce32 then17.
                if (amount > 0 && amount > suction + 1) {
                    setSuction(type == null ? configured : type, isRestrict() ? amount / 2 : amount - 1);
                }
            } catch (RuntimeException ignored) {
                // The original isolates a failing adjacent transport per side.
            }
        }
    }

    private void checkVenting() {
        for (Direction face : Direction.values()) {
            try {
                if (!isConnectable(face)) continue;
                BlockEntity neighbor = EssentiaTransfers.connectableNeighbor(this, face);
                if (!(neighbor instanceof IEssentiaTransport transport)) continue;
                int amount = transport.getSuctionAmount(face.getOpposite());
                if (suction <= 0 || amount != suction && amount != suction - 1 || suctionType == transport.getSuctionType(face.getOpposite())) continue;
                level.blockEvent(worldPosition, getBlockState().getBlock(), 1, aspectIndex(suctionType));
                venting = 40;
            } catch (RuntimeException ignored) {
                // Preserve the original's per-neighbor isolation, not flux/deletion.
            }
        }
    }

    private void equalizeWithNeighbours() {
        if (essentiaAmount > 0) return;
        for (Direction face : Direction.values()) {
            try {
                // Oneway's pressure face is excluded from pulls, not from its
                // raw ports; all other open faces still participate.
                if (isOneWay() && face == facing.getOpposite() || !isConnectable(face)) continue;
                // The caller profile preserves the original short-circuit type
                // query program under the same lock as accounting. A buffer's
                // random public representative is not a storage identity.
                var attempt = EssentiaTransfers.pullTubeOne(this, face);
                if (attempt.committedUnits() == 0) continue;
                if (level.random.nextInt(100) == 0) level.blockEvent(worldPosition, getBlockState().getBlock(), 0, 0);
                return;
            } catch (RuntimeException ignored) {
                // A post-commit notification failure never reverses a paid unit.
            }
        }
    }

    @Override public boolean isConnectable(Direction face) { return isSideOpen(face) && (!isValve() || face != facing); }
    // TileTubeValve only overrides isConnectable, NOT these inherited raw ports.
    @Override public boolean canInputFrom(Direction face) { return isSideOpen(face); }
    @Override public boolean canOutputTo(Direction face) { return isSideOpen(face); }
    @Override public Aspect getEssentiaType(Direction face) { return essentiaType; }
    @Override public int getEssentiaAmount(Direction face) { return essentiaAmount; }
    @Override public Aspect getSuctionType(Direction face) { return suctionType; }
    @Override public int getSuctionAmount(Direction face) { return suction; }
    @Override public int getMinimumSuction() { return 0; }
    @Override public boolean renderExtendedTube() { return false; }

    @Override public void setSuction(Aspect aspect, int amount) {
        if (!canMutate() || isValve() && !allowFlow) return;
        suctionType = aspect;
        suction = amount;
    }

    @Override public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (!canMutate() || !canInputFrom(face) || essentiaAmount != 0 || amount <= 0) return 0;
        essentiaType = aspect;
        essentiaAmount++;
        changed();
        return 1;
    }

    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (!canMutate() || !canOutputTo(face) || essentiaType != aspect || essentiaAmount <= 0 || amount <= 0) return 0;
        if (--essentiaAmount <= 0) essentiaType = null;
        changed();
        return 1;
    }

    @Override public Aspect getStoredEssentiaType() { return essentiaType; }
    @Override public int getStoredEssentiaAmount() { return essentiaAmount; }
    @Override public int getEssentiaCapacity() { return 1; }

    @Override public void setEssentiaStorageRaw(Aspect aspect, int amount) {
        EssentiaTransfers.requireMutation(this);
        essentiaType = aspect;
        essentiaAmount = amount;
    }

    @Override public void notifyEssentiaStorageChanged() {
        if (level != null && !level.isClientSide && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this) changed();
    }

    private boolean canMutate() {
        if (isRemoved() || EssentiaTransfers.isLocked(this) || level != null && level.isClientSide) return false;
        if (level == null) return true;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Tube changes require the server thread");
        return level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition) == getBlockState();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeStorage(tag);
        if (suctionType != null) tag.putString("stype", suctionType.getTag());
        tag.putInt("samount", suction);
        // Original count/venting/color/valve rotation are transient.
    }

    private void writeStorage(CompoundTag tag) {
        if (essentiaType != null) tag.putString("type", essentiaType.getTag());
        tag.putInt("amount", essentiaAmount);
        tag.putInt("side", facing.get3DDataValue());
        byte[] sides = new byte[6];
        for (int index = 0; index < 6; index++) sides[index] = openSides[index] ? (byte) 1 : 0;
        tag.putByteArray("open", sides);
        if (isValve()) {
            tag.putBoolean("flow", allowFlow);
            tag.putBoolean("hadpower", powered);
        }
        if (isFilter() && filter != null) tag.putString("AspectFilter", filter.getTag());
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        essentiaType = Aspect.getAspect(tag.getString("type"));
        essentiaAmount = Math.clamp(tag.getInt("amount"), 0, 1);
        int side = tag.getInt("side");
        facing = side >= 0 && side < 6 ? Direction.from3DDataValue(side) : Direction.NORTH;
        byte[] sides = tag.getByteArray("open");
        if (sides.length == 6) for (int index = 0; index < 6; index++) openSides[index] = sides[index] == 1;
        if (isValve()) {
            // New placement starts open, but an omitted persisted boolean is false.
            allowFlow = tag.getBoolean("flow");
            powered = tag.getBoolean("hadpower");
        }
        filter = isFilter() ? Aspect.getAspect(tag.getString("AspectFilter")) : null;
        // Custom client updates omit suction, just like original TileThaumcraft.
        if (tag.contains("samount")) {
            suctionType = Aspect.getAspect(tag.getString("stype"));
            suction = tag.getInt("samount");
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeStorage(tag);
        return tag;
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public boolean triggerEvent(int id, int data) {
        if (id != 0 && id != 1) return super.triggerEvent(id, data);
        if (level != null && level.isClientSide) {
            boolean starting = id == 1 && venting <= 0;
            if (id == 1) {
                venting = 50;
                ventColor = aspectColor(data);
            }
            if (clientEffects != null) clientEffects.event(level, worldPosition, id, id == 1 ? ventColor : 0, starting);
        }
        return true;
    }

    /** Config.initMisc:824-827 records the registration order of Aspect.aspects.values(). */
    private static int aspectIndex(Aspect wanted) {
        int index = 0;
        for (Aspect aspect : Aspect.aspects.values()) {
            if (aspect == wanted) return index;
            index++;
        }
        return -1;
    }

    private static int aspectColor(int wanted) {
        int index = 0;
        for (Aspect aspect : Aspect.aspects.values()) if (index++ == wanted) return aspect.getColor();
        return 0xAAAAAA;
    }
}
