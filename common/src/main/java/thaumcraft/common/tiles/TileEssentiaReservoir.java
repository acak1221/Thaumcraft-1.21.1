package thaumcraft.common.tiles;

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
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.config.ThaumcraftTubes;
import thaumcraft.common.lib.essentia.EssentiaAtomicEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;
import thaumcraft.common.lib.essentia.ReservoirRules;

/**
 * Original TileEssentiaReservoir: a mixed store of up to 256 units in TOTAL (not per aspect) with one port, on the side it faces (down
 * until placed or turned with a wand). It wants essentia with suction 24 while it is not full, draws one unit from the connected source
 * every fifth tick, offers its stock only through that side, and answers a sided type query with nothing (a tube asks the unsided one,
 * which names the first aspect stored). The stock is kept like the buffer's: an immutable insertion-ordered map replaced by prepared
 * changes, so every transfer goes through the shared native executor; a malformed saved payload is kept verbatim and the reservoir
 * stays closed instead of the original's null-aspect crash, while a total above 256 is dropped as the original did. The client side
 * keeps the original's tank colour: the shown aspect cycles through the stock and the colour walks towards it, so the renderer can
 * draw the stored essentia as the original's coloured volume.
 */
public final class TileEssentiaReservoir extends BlockEntity implements IAspectSource, EssentiaAtomicEndpoint {
    public static final int MAX_AMOUNT = ReservoirRules.CAPACITY;
    private static final SoundEvent CREAK = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "creak"));
    private Map<Aspect, Integer> stock = Map.of();
    private Direction facing = Direction.DOWN;
    private int count;
    private CompoundTag rejected;
    private final ReservoirColour colour = new ReservoirColour();

    public TileEssentiaReservoir(BlockPos pos, BlockState state) { super(ThaumcraftTubes.RESERVOIR_BLOCK_ENTITY.get(), pos, state); }

    public Direction getFacing() { return facing; }
    public int getAmount() { return total(stock); }
    public int getSpace() { return ReservoirRules.space(getAmount()); }
    public int getTickCount() { return count; }
    public boolean isQuarantined() { return rejected != null; }
    /** The capacity of the tank; the original's maxAmount. */
    public int getMaxAmount() { return MAX_AMOUNT; }
    /** The aspect the tank shows; it cycles through the stock every twentieth tick, as the original's displayAspect does. */
    public Aspect getDisplayAspect() { return colour.displayAspect(); }
    /** The colour the tank is drawn in; the original's cr/cg/cb, faded towards the shown aspect's colour. */
    public float getRed() { return colour.red(); }
    public float getGreen() { return colour.green(); }
    public float getBlue() { return colour.blue(); }

    /** Original comparator: {@code floor(units / 256 * 14)} plus one when anything is stored. */
    public int comparatorSignal() { return ReservoirRules.comparator(getAmount()); }

    /** Wand and placement: the port faces {@code face}; always marks the block, as the original did even for the same side. */
    public boolean setFacing(Direction face) {
        if (face == null || !canMutate()) return false;
        facing = face;
        changed(false);
        return true;
    }

    @Override public AspectList getAspects() {
        // The original exposed its live list; a copy keeps outside writes from bypassing the total capacity and the transfer locks.
        AspectList copy = new AspectList();
        stock.forEach(copy::add);
        return copy;
    }
    /** The original replaced its list with a copy of any list (past the capacity too); like the jar and buffer this is not a fill API here. */
    @Override public void setAspects(AspectList aspects) {}

    @Override public int addToContainer(Aspect aspect, int amount) {
        if (amount == 0) return 0;
        if (amount < 0 || !canMutate() || !validAspect(aspect)) return amount;
        int moved = EssentiaTransfers.changeReservoir(this, aspect, amount, true);
        return amount - moved;
    }
    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        // Original AspectList.remove(aspect, negative) could create stock.
        if (amount < 0 || !canMutate() || !validAspect(aspect)) return false;
        if (amount == 0) return true;
        return EssentiaTransfers.changeReservoir(this, aspect, amount, false) == amount;
    }
    @Override @Deprecated public boolean takeFromContainer(AspectList aspects) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return validAspect(aspect) && amount >= 0 && containerContains(aspect) >= amount; }
    @Override public boolean doesContainerContain(AspectList aspects) {
        if (aspects == null) return false;
        for (Aspect aspect : aspects.getAspects()) if (containerContains(aspect) < aspects.getAmount(aspect)) return false;
        return true;
    }
    @Override public int containerContains(Aspect aspect) { return aspect == null ? 0 : stock.getOrDefault(aspect, 0); }
    @Override public boolean doesContainerAccept(Aspect aspect) { return validAspect(aspect); }

    @Override public boolean isConnectable(Direction face) { return !isQuarantined() && face == facing; }
    @Override public boolean canInputFrom(Direction face) { return isConnectable(face); }
    @Override public boolean canOutputTo(Direction face) { return isConnectable(face); }
    @Override public void setSuction(Aspect aspect, int amount) {}
    @Override public boolean renderExtendedTube() { return false; }
    @Override public int getMinimumSuction() { return ReservoirRules.SUCTION; }
    @Override public Aspect getSuctionType(Direction face) { return null; }
    @Override public int getSuctionAmount(Direction face) { return isQuarantined() ? 0 : ReservoirRules.suction(getAmount()); }
    /** Only the unsided (UNKNOWN) query names a type: the first aspect stored; a query from a face is answered with nothing. */
    @Override public Aspect getEssentiaType(Direction face) { return face == null && !stock.isEmpty() ? stock.keySet().iterator().next() : null; }
    @Override public int getEssentiaAmount(Direction face) { return getAmount(); }
    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return canOutputTo(face) && takeFromContainer(aspect, amount) ? amount : 0;
    }
    @Override public int addEssentia(Aspect aspect, int amount, Direction face) {
        return canInputFrom(face) ? amount - addToContainer(aspect, amount) : 0;
    }

    private record ReservoirState(Map<Aspect, Integer> stock, Direction facing, boolean quarantined) implements State {}
    @Override public State snapshotAtomicState() { return new ReservoirState(stock, facing, isQuarantined()); }
    @Override public boolean matchesAtomicState(State before) {
        return before instanceof ReservoirState saved && stock == saved.stock() && facing == saved.facing() && isQuarantined() == saved.quarantined();
    }
    @Override public Change prepareExtract(State before, Aspect aspect, int units) {
        if (!(before instanceof ReservoirState saved) || saved.quarantined() || !validAspect(aspect) || units <= 0 || saved.stock().getOrDefault(aspect, 0) < units) return null;
        var after = new LinkedHashMap<>(saved.stock());
        int left = after.get(aspect) - units;
        if (left == 0) after.remove(aspect); else after.put(aspect, left);
        return change(saved, after, units);
    }
    @Override public Change prepareInsert(State before, Aspect aspect, int units) {
        if (!(before instanceof ReservoirState saved) || saved.quarantined() || !validAspect(aspect) || units <= 0 || units > MAX_AMOUNT - total(saved.stock())) return null;
        var after = new LinkedHashMap<>(saved.stock());
        after.merge(aspect, units, Integer::sum);
        return change(saved, after, units);
    }
    private Change change(ReservoirState before, Map<Aspect, Integer> after, int units) {
        return new Change(this, before, new ReservoirState(Collections.unmodifiableMap(after), before.facing(), false), units, true);
    }
    @Override public void applyAtomicRaw(Change change) {
        EssentiaTransfers.requireAtomicChange(this, change);
        stock = ((ReservoirState) change.after()).stock();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEssentiaReservoir tile) {
        if (tile.level != level || !tile.worldPosition.equals(pos) || tile.isQuarantined() || tile.isRemoved()) return;
        ++tile.count;
        if (level.isClientSide) {
            // Original client branch: a creak now and then, more often the fuller the vessel, and the colour of the tank's contents
            // walks towards the aspect it shows (the original's displayAspect cycle).
            int units = tile.getAmount();
            if (units > 0) {
                if (ReservoirRules.creaks(level.random.nextInt(ReservoirRules.creakBound(units)))) {
                    level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, CREAK, SoundSource.BLOCKS, 1.0F, 1.4F + level.random.nextFloat() * .2F, false);
                }
                tile.colour.update(List.copyOf(tile.stock.keySet()), tile.count);
            }
            return;
        }
        if (tile.count % ReservoirRules.PULL_INTERVAL != 0 || tile.getAmount() >= MAX_AMOUNT || !tile.canMutate()) return;
        var attempt = EssentiaTransfers.pullReservoirOne(tile);
        if (attempt.failure() != null) {
            thaumcraft.common.Thaumcraft.LOG.warn("Reservoir attempt failed at {} (committed {})", pos, attempt.committedUnits(), attempt.failure());
        }
    }

    private boolean current() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this && level.getBlockState(worldPosition) == getBlockState(); }
    private boolean canMutate() {
        if (isQuarantined() || isRemoved() || EssentiaTransfers.isLocked(this) || level == null || level.isClientSide) return false;
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) throw new IllegalStateException("Reservoir changes require the server thread");
        return current();
    }
    private void changed(boolean comparator) {
        setChanged();
        if (current() && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            if (comparator && current()) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
    @Override public void notifyEssentiaStorageChanged() { if (current() && !level.isClientSide) changed(true); }

    private static boolean validAspect(Aspect aspect) { return EssentiaAtomicEndpoint.validAspect(aspect); }
    private static int total(Map<Aspect, Integer> contents) { int total = 0; for (int amount : contents.values()) total += amount; return total; }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); writeCustom(tag); }
    private void writeCustom(CompoundTag tag) {
        if (rejected != null) { tag.put("RejectedReservoir", rejected.copy()); return; }
        ListTag contents = new ListTag();
        stock.forEach((aspect, amount) -> { var row = new CompoundTag(); row.putString("key", aspect.getTag()); row.putInt("amount", amount); contents.add(row); });
        tag.put("Aspects", contents);
        tag.putByte("face", (byte) facing.get3DDataValue());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("RejectedReservoir")) { quarantine(tag.get("RejectedReservoir") instanceof CompoundTag saved ? saved : tag); return; }
        try {
            if (tag.contains("Aspects") && (!(tag.get("Aspects") instanceof ListTag entries) || !entries.isEmpty() && entries.getElementType() != Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Invalid aspect list");
            var parsed = new LinkedHashMap<Aspect, Integer>();
            long total = 0;
            for (Tag raw : tag.getList("Aspects", Tag.TAG_COMPOUND)) {
                var row = (CompoundTag) raw;
                Aspect aspect = Aspect.getAspect(row.getString("key"));
                int amount = row.getInt("amount");
                if (!row.contains("key", Tag.TAG_STRING) || !row.contains("amount", Tag.TAG_INT) || !validAspect(aspect) || amount <= 0) throw new IllegalArgumentException("Invalid reservoir stock");
                total += amount;
                parsed.merge(aspect, amount, Integer::sum);
            }
            int face = tag.contains("face") ? tag.getByte("face") : Direction.DOWN.get3DDataValue();
            if (tag.contains("face") && !tag.contains("face", Tag.TAG_BYTE) || face < 0 || face > 5) throw new IllegalArgumentException("Invalid reservoir face");
            // Original readCustomNBT: more than the capacity is an empty list, not a truncated one.
            stock = total > MAX_AMOUNT ? Map.of() : Collections.unmodifiableMap(parsed);
            facing = Direction.from3DDataValue(face);
            rejected = null;
        } catch (IllegalArgumentException malformed) {
            quarantine(tag);
            thaumcraft.common.Thaumcraft.LOG.warn("Quarantined malformed essentia reservoir data at {}", worldPosition);
        }
    }
    private void quarantine(CompoundTag payload) { rejected = payload.copy(); stock = Map.of(); facing = Direction.DOWN; }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { var tag = new CompoundTag(); writeCustom(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
