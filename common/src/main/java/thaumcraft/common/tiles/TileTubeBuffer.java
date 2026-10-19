package thaumcraft.common.tiles;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;

/** Original mixed eight-TOTAL pool. Not a capacity variant of the single-unit tube.
 * Authority hardening: private ordered stock/defensive container reads, registered
 * non-null types, no negative extraction, and verbatim disabled malformed-data
 * quarantine instead of truncating or dropping a subset of mixed saved units. */
public final class TileTubeBuffer extends BlockEntity implements IAspectContainer, EssentiaAtomicEndpoint {
    public static final int MAXAMOUNT = 8;
    private Map<Aspect, Integer> stock = Map.of();
    private final boolean[] open = {true, true, true, true, true, true};
    private final byte[] choke = new byte[6];
    private int count, bellows = -1;
    private CompoundTag rejected;

    public TileTubeBuffer(BlockPos pos, BlockState state) { super(ThaumcraftTubes.BUFFER_BLOCK_ENTITY.get(), pos, state); }
    public int getTickCount() { return count; }
    public int getBellowsCount() { return bellows; }
    public int getChoke(Direction face) { return face == null ? 0 : choke[face.get3DDataValue()]; }
    public boolean isSideOpen(Direction face) { return face != null && open[face.get3DDataValue()]; }
    public boolean isQuarantined() { return rejected != null; }
    public int getAmount() { return total(stock); }

    public boolean setSideOpen(Direction face, boolean allowed) { return EssentiaTransfers.setTubeSideOpen(this, face, allowed); }
    public void setSideOpenRaw(Direction face, boolean allowed) {
        EssentiaTransfers.requireTubeSideMutation(this);
        open[face.get3DDataValue()] = allowed;
    }
    public boolean cycleChoke(Direction face) {
        if (face == null || !canMutate()) return false;
        int side = face.get3DDataValue(); choke[side] = (byte) ((choke[side] + 1) % 3);
        changed(); return true;
    }

    @Override public AspectList getAspects() {
        // The oracle exposes a live list; a native copy prevents external alias
        // writes from bypassing the total capacity and active mutation locks.
        AspectList copy = new AspectList(); stock.forEach(copy::add); return copy;
    }
    @Override public void setAspects(AspectList aspects) {}
    @Override public int addToContainer(Aspect aspect, int amount) {
        if (amount != 1 || !canMutate() || !EssentiaAtomicEndpoint.validAspect(aspect)) return amount;
        return EssentiaTransfers.changeBuffer(this, aspect, 1, true, null, true) == 1 ? 0 : amount;
    }
    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        // Original AspectList.remove(aspect, negative) could CREATE stock.
        if (amount < 0 || !canMutate() || !EssentiaAtomicEndpoint.validAspect(aspect)) return false;
        // Original zero take succeeds without moving stock. Do not create a
        // dirty/notification callback for a zero-resource native operation.
        return amount == 0 || EssentiaTransfers.changeBuffer(this, aspect, amount, false, null, true) == amount;
    }
    @Override @Deprecated public boolean takeFromContainer(AspectList aspects) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return EssentiaAtomicEndpoint.validAspect(aspect) && amount >= 0 && containerContains(aspect) >= amount; }
    @Override @Deprecated public boolean doesContainerContain(AspectList aspects) { return false; }
    @Override public int containerContains(Aspect aspect) { return aspect == null ? 0 : stock.getOrDefault(aspect, 0); }
    @Override public boolean doesContainerAccept(Aspect aspect) { return EssentiaAtomicEndpoint.validAspect(aspect); }
    @Override public boolean isConnectable(Direction face) { return !isQuarantined() && isSideOpen(face); }
    @Override public boolean canInputFrom(Direction face) { return isConnectable(face); }
    @Override public boolean canOutputTo(Direction face) { return isConnectable(face); }
    @Override public void setSuction(Aspect aspect, int amount) {}
    @Override public boolean renderExtendedTube() { return false; }
    @Override public int getMinimumSuction() { return 0; }
    @Override public Aspect getSuctionType(Direction face) { return null; }
    @Override public int getSuctionAmount(Direction face) {
        if (face == null || isQuarantined()) return 0; // Original UNKNOWN indexing crashed.
        int value = getChoke(face);
        return value == 2 ? 0 : value == 1 || bellows <= 0 ? 1 : bellows * 32;
    }
    @Override public Aspect getEssentiaType(Direction face) {
        if (stock.isEmpty() || level == null) return null;
        // Uniform over insertion-ordered TYPES, not counts. Even nextInt(1) is
        // a real world-RNG query; accounting/snapshots never call this method.
        int index = level.random.nextInt(stock.size());
        for (Aspect aspect : stock.keySet()) if (index-- == 0) return aspect;
        throw new IllegalStateException("Buffer type query changed its immutable stock");
    }
    @Override public int getEssentiaAmount(Direction face) { return getAmount(); }
    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (amount <= 0 || !canMutate() || !canOutputTo(face) || !EssentiaAtomicEndpoint.validAspect(aspect)) return 0;
        return EssentiaTransfers.changeBuffer(this, aspect, amount, false, face, false);
    }
    @Override public int addEssentia(Aspect aspect, int amount, Direction face) { return canInputFrom(face) ? amount - addToContainer(aspect, amount) : 0; }

    /** Original take arbitration is callback policy, not a fake public take/refund. */
    @Override public boolean preflightTake(Aspect aspect, Direction face) {
        if (!canOutputTo(face) || level == null) return false;
        BlockEntity target = EssentiaTransfers.connectableNeighbor(this, face);
        int requested = target instanceof IEssentiaTransport transport ? transport.getSuctionAmount(face.getOpposite()) : 0;
        if (target != null && !currentNeighbor(target)) return false;
        for (Direction other : Direction.values()) {
            if (other == face || !canOutputTo(other)) continue;
            BlockEntity neighbor = EssentiaTransfers.connectableNeighbor(this, other);
            if (!(neighbor instanceof IEssentiaTransport transport)) continue;
            int demand = transport.getSuctionAmount(other.getOpposite());
            Aspect type = transport.getSuctionType(other.getOpposite());
            if (!currentNeighbor(neighbor)) return false;
            if ((type == null || type == aspect) && demand > requested && demand > getSuctionAmount(other)) return false;
        }
        return true;
    }

    private boolean currentNeighbor(BlockEntity tile) {
        return !tile.isRemoved() && tile.getLevel() == level && level.hasChunkAt(tile.getBlockPos())
                && level.getBlockEntity(tile.getBlockPos()) == tile && level.getBlockState(tile.getBlockPos()) == tile.getBlockState();
    }

    private record BufferState(Map<Aspect, Integer> stock, int open, int choke, int bellows, int count, boolean quarantined) implements State {}
    @Override public State snapshotAtomicState() { return new BufferState(stock, openMask(), chokeMask(), bellows, count, isQuarantined()); }
    @Override public boolean matchesAtomicState(State before) {
        return before instanceof BufferState saved && stock == saved.stock() && openMask() == saved.open() && chokeMask() == saved.choke()
                && bellows == saved.bellows() && count == saved.count() && isQuarantined() == saved.quarantined();
    }
    @Override public Change prepareExtract(State before, Aspect aspect, int units) {
        if (!(before instanceof BufferState saved) || saved.quarantined() || !validAspect(aspect) || units <= 0 || saved.stock().getOrDefault(aspect, 0) < units) return null;
        var after = new LinkedHashMap<>(saved.stock()); int left = after.get(aspect) - units;
        if (left == 0) after.remove(aspect); else after.put(aspect, left);
        return change(saved, after, units);
    }
    @Override public Change prepareInsert(State before, Aspect aspect, int units) {
        if (!(before instanceof BufferState saved) || saved.quarantined() || !validAspect(aspect) || units != 1 || total(saved.stock()) >= MAXAMOUNT) return null;
        var after = new LinkedHashMap<>(saved.stock()); after.merge(aspect, 1, Integer::sum);
        return change(saved, after, units);
    }
    private Change change(BufferState before, Map<Aspect, Integer> after, int units) {
        Map<Aspect, Integer> immutable = Collections.unmodifiableMap(after);
        return new Change(this, before, new BufferState(immutable, before.open(), before.choke(), before.bellows(), before.count(), false), units, true);
    }
    @Override public void applyAtomicRaw(Change change) {
        EssentiaTransfers.requireAtomicChange(this, change);
        stock = ((BufferState) change.after()).stock();
    }
    private static boolean validAspect(Aspect aspect) { return EssentiaAtomicEndpoint.validAspect(aspect); }
    private static int total(Map<Aspect, Integer> contents) { int total = 0; for (int amount : contents.values()) total += amount; return total; }
    private int openMask() { int bits = 0; for (int side = 0; side < 6; side++) if (open[side]) bits |= 1 << side; return bits; }
    private int chokeMask() { int bits = 0; for (int side = 0; side < 6; side++) bits |= choke[side] << side * 2; return bits; }

    public static void tick(Level level, BlockPos pos, BlockState state, TileTubeBuffer buffer) {
        if (!level.isClientSide && (!(level instanceof ServerLevel server) || !server.getServer().isSameThread())) throw new IllegalStateException("Buffer ticks require the server thread");
        if (buffer.level != level || !buffer.worldPosition.equals(pos) || !buffer.current() || state != buffer.getBlockState()
                || buffer.isQuarantined() || EssentiaTransfers.isLocked(buffer) || !level.isClientSide && !buffer.canMutate()) return;
        ++buffer.count;
        if (buffer.bellows < 0 || buffer.count % 20 == 0) {
            int found = TileBellows.getBellows(level, pos, List.of(Direction.values()));
            if (!buffer.current() || EssentiaTransfers.isLocked(buffer)) return;
            buffer.bellows = found;
        }
        if (!level.isClientSide && buffer.count % 5 == 0 && buffer.getAmount() < MAXAMOUNT) buffer.fillBuffer();
    }
    private void fillBuffer() {
        for (Direction face : Direction.values()) {
            var attempt = EssentiaTransfers.pullBufferOne(this, face);
            if (attempt.failure() != null) {
                thaumcraft.common.Thaumcraft.LOG.warn("Buffer attempt failed at {} (committed {})", worldPosition, attempt.committedUnits(), attempt.failure());
                return; // Failure is not permission to attempt a later side.
            }
            if (attempt.selected()) return;
        }
    }
    private boolean current() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this && level.getBlockState(worldPosition) == getBlockState(); }
    private boolean canMutate() {
        if (isQuarantined() || isRemoved() || EssentiaTransfers.isLocked(this) || level == null || level.isClientSide) return false;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Buffer changes require the server thread");
        return current();
    }
    private void changed() {
        setChanged();
        if (current() && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            if (current()) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
    @Override public void notifyEssentiaStorageChanged() { if (current() && !level.isClientSide) changed(); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); writeCustom(tag); }
    private void writeCustom(CompoundTag tag) {
        ListTag contents = new ListTag();
        stock.forEach((aspect, amount) -> { var row = new CompoundTag(); row.putString("key", aspect.getTag()); row.putInt("amount", amount); contents.add(row); });
        tag.put("Aspects", contents);
        byte[] sides = new byte[6]; for (int side = 0; side < 6; side++) sides[side] = open[side] ? (byte) 1 : 0;
        tag.putByteArray("open", sides); tag.putByteArray("choke", choke.clone());
        if (rejected != null) tag.put("RejectedBuffer", rejected.copy());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("RejectedBuffer")) { quarantine(tag.get("RejectedBuffer") instanceof CompoundTag saved ? saved : tag); return; }
        try {
            if (tag.contains("Aspects") && (!(tag.get("Aspects") instanceof ListTag entries) || !entries.isEmpty() && entries.getElementType() != Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Invalid aspect list");
            var parsed = new LinkedHashMap<Aspect, Integer>(); long total = 0;
            for (Tag raw : tag.getList("Aspects", Tag.TAG_COMPOUND)) {
                var row = (CompoundTag) raw;
                Aspect aspect = Aspect.getAspect(row.getString("key")); int amount = row.getInt("amount");
                if (!row.contains("key", Tag.TAG_STRING) || !row.contains("amount", Tag.TAG_INT) || !validAspect(aspect) || amount <= 0 || (total += amount) > MAXAMOUNT) throw new IllegalArgumentException("Invalid buffer stock");
                parsed.merge(aspect, amount, Integer::sum);
            }
            byte[] sides = readSides(tag, "open", true), chokes = readSides(tag, "choke", false);
            stock = Collections.unmodifiableMap(parsed); rejected = null;
            for (int side = 0; side < 6; side++) { open[side] = sides[side] == 1; choke[side] = chokes[side]; }
        } catch (IllegalArgumentException malformed) {
            // Reject the WHOLE payload, preserving it verbatim for diagnosis/
            // repair. Never truncate valid mixed units to an arbitrary eight.
            quarantine(tag);
            thaumcraft.common.Thaumcraft.LOG.warn("Quarantined malformed essentia buffer data at {}", worldPosition);
        }
    }
    private static byte[] readSides(CompoundTag tag, String name, boolean flags) {
        if (!tag.contains(name)) { byte[] defaults = new byte[6]; if (flags) Arrays.fill(defaults, (byte) 1); return defaults; }
        byte[] values = tag.getByteArray(name);
        if (!tag.contains(name, Tag.TAG_BYTE_ARRAY) || values.length != 6) throw new IllegalArgumentException("Invalid " + name);
        for (byte value : values) if (value < 0 || value > (flags ? 1 : 2)) throw new IllegalArgumentException("Invalid " + name);
        return values;
    }
    private void quarantine(CompoundTag payload) { rejected = payload.copy(); stock = Map.of(); Arrays.fill(open, true); Arrays.fill(choke, (byte) 0); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { var tag = new CompoundTag(); writeCustom(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
