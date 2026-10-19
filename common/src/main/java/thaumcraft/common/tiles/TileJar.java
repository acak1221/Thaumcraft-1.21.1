package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.BlockJar;
import thaumcraft.common.config.ThaumcraftLabels;
import thaumcraft.common.items.JarLabelData;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;

/** TC4 ordinary/void fillable jars: 64 stored units and one top-only five-tick pull. */
public final class TileJar extends BlockEntity implements IAspectSource, EssentiaStorageEndpoint {
    private final JarContents contents = new JarContents();
    private Aspect filter;
    private Aspect emptyAspect;
    private Direction facing = Direction.NORTH;
    private int count;
    public TileJar(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    /** Variant is the real block, never an item/save tag granting sink permission. */
    public boolean isVoid() { return getBlockState().getBlock() instanceof BlockJar jar && jar.isVoid(); }
    public Aspect getAspect() { return contents.amount() > 0 ? Aspect.getAspect(contents.tag()) : emptyAspect; }
    public int getAmount() { return contents.amount(); }
    public int getMaxAmount() { return JarContents.CAPACITY; }
    public Aspect getFilter() { return filter; }
    public Direction getFacing() { return facing; }
    @Override public AspectList getAspects() { return getAmount() > 0 ? new AspectList().add(getAspect(), getAmount()) : new AspectList(); }
    /** Original TileJarFillable:69-70 is deliberately not a replacement/fill API. */
    @Override public void setAspects(AspectList aspects) {}
    // Original acceptance checks only the label, not contents or remaining space.
    // Native null safely fails a labelled match rather than throwing via equals.
    @Override public boolean doesContainerAccept(Aspect aspect) { return filter == null || filter == aspect; }
    public boolean setFilter(Aspect filter) {
        if (!canMutate() || filter != null && (Aspect.getAspect(filter.getTag()) != filter || getAmount() > 0 && getAspect() != filter)) return false;
        if (this.filter == filter) return true;
        this.filter = filter;
        if (filter != null && getAmount() == 0) emptyAspect = filter;
        changed(false);
        return true;
    }
    public void setFacing(Direction facing) {
        if (!canMutate() || facing == null || !facing.getAxis().isHorizontal() || this.facing == facing) return;
        this.facing = facing;
        changed(false);
    }
    @Override public int addToContainer(Aspect aspect, int amount) {
        // Original low-level insertion ignores the label. Manual phials must
        // separately check acceptance, matching contents and whole-phial space.
        // Keep native storage typed; null/unknown inputs and negative duplication
        // are refused, while the original zero-add is an immediate no-op.
        if (amount <= 0 || !canMutate() || aspect == null || Aspect.getAspect(aspect.getTag()) != aspect) return amount;
        boolean notify = !isVoid() || getAmount() < getMaxAmount();
        int left = contents.add(aspect.getTag(), amount, isVoid());
        if (left != amount) emptyAspect = null;
        // TileJarFillableVoid:19-35: a full overflow sink has no changed state.
        if (notify) changed();
        return left;
    }
    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        if (!canMutate() || amount < 0 || getAmount() < amount || getAspect() != aspect) return false;
        int remaining = getAmount() - amount;
        // Unlike JarContents.take, the original jar accepts a matching zero take,
        // including null/zero and clearing the retained type of an empty jar.
        contents.restore(remaining > 0 ? aspect.getTag() : null, remaining);
        emptyAspect = null;
        changed(); return true;
    }
    @Override @Deprecated public boolean takeFromContainer(AspectList aspects) { return false; }
    @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) { return getAmount() >= amount && getAspect() == aspect; }
    @Override @Deprecated public boolean doesContainerContain(AspectList aspects) {
        if (getAmount() > 0 && aspects != null) {
            // Intersection of types, irrespective of each requested amount.
            for (Aspect aspect : aspects.getAspects()) if (getAspect() == aspect) return true;
        }
        return false;
    }
    @Override public int containerContains(Aspect aspect) { return 0; }
    public void clearContents() {
        if (!canMutate()) return;
        Aspect previous = getAspect();
        contents.restore(null, 0);
        // BlockJar:267-270 keeps the type while the filter remains attached.
        emptyAspect = filter == null ? null : previous;
        changed();
    }
    private boolean canMutate() {
        if (isRemoved() || level != null && level.isClientSide || EssentiaTransfers.isLocked(this)) return false;
        if (level != null && !level.getServer().isSameThread()) throw new IllegalStateException("Jar changes require the server thread");
        return level == null || level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this
                && level.getBlockState(worldPosition) == getBlockState();
    }
    private void changed() { changed(true); }
    private void changed(boolean comparator) {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // Labels/facing do not change the signal; avoid inventing a new
            // comparator callback inside those existing manual transactions.
            if (comparator && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this)
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
    @Override public Aspect getStoredEssentiaType() { return getAspect(); }
    @Override public int getStoredEssentiaAmount() { return getAmount(); }
    @Override public int getEssentiaCapacity() { return getMaxAmount(); }
    @Override public boolean allowsEssentiaOverflow() { return isVoid(); }
    @Override public void setEssentiaStorageRaw(Aspect aspect, int amount) {
        EssentiaTransfers.requireMutation(this);
        contents.restore(aspect == null ? null : aspect.getTag(), amount);
        emptyAspect = contents.amount() == 0 ? aspect : null;
    }
    @Override public void notifyEssentiaStorageChanged() {
        if (level != null && !level.isClientSide && !isRemoved() && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this) changed();
    }

    // Direction has no UNKNOWN member; null is the unsided/UNKNOWN query. Only
    // UP is connectable, but the original getters do not gate on their face.
    @Override public boolean isConnectable(Direction face) { return face == Direction.UP; }
    @Override public boolean canInputFrom(Direction face) { return face == Direction.UP; }
    @Override public boolean canOutputTo(Direction face) { return face == Direction.UP; }
    @Override public void setSuction(Aspect aspect, int amount) {}
    @Override public Aspect getSuctionType(Direction face) { return filter != null ? filter : getAspect(); }
    @Override public int getSuctionAmount(Direction face) {
        if (isVoid()) return filter != null && getAmount() < getMaxAmount() ? 48 : 32;
        return getAmount() < getMaxAmount() ? (filter != null ? 64 : 32) : 0;
    }
    @Override public Aspect getEssentiaType(Direction face) { return getAspect(); }
    @Override public int getEssentiaAmount(Direction face) { return getAmount(); }
    @Override public int getMinimumSuction() { return filter != null ? (isVoid() ? 48 : 64) : 32; }
    @Override public boolean renderExtendedTube() { return true; }
    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return canOutputTo(face) && takeFromContainer(aspect, amount) ? amount : 0;
    }
    @Override public int addEssentia(Aspect aspect, int amount, Direction face) {
        return canInputFrom(face) ? amount - addToContainer(aspect, amount) : 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileJar jar) {
        if (level.isClientSide || jar.level != level || !pos.equals(jar.worldPosition) || !jar.canMutate()) return;
        if (++jar.count % 5 == 0 && (jar.isVoid() || jar.getAmount() < jar.getMaxAmount())) jar.fillJar();
    }

    private void fillJar() {
        if (!isConnectable(Direction.UP) || !canInputFrom(Direction.UP)) return;
        // TileJarFillable:213-221 checks the source minimum ONLY in the empty,
        // unlabelled fallback. Its explicit native profile also makes ZERO
        // source-type queries for a labelled/filled jar, including mixed buffers.
        EssentiaTransfers.pullJarOne(this);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents.restore(tag.getString("Aspect"), tag.getInt("Amount"));
        emptyAspect = contents.amount() == 0 ? Aspect.getAspect(tag.getString("Aspect")) : null;
        filter = Aspect.getAspect(tag.getString("AspectFilter"));
        Direction savedFacing = Direction.from3DDataValue(tag.getByte("facing"));
        facing = savedFacing.getAxis().isHorizontal() ? savedFacing : Direction.NORTH;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (getAspect() != null) tag.putString("Aspect", getAspect().getTag());
        tag.putInt("Amount", contents.amount());
        if (filter != null) tag.putString("AspectFilter", filter.getTag());
        tag.putByte("facing", (byte) facing.get3DDataValue());
    }
    @Override protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        // Filled-jar non-stackability is an item presentation, not persistent
        // BE state: an emptied, unlabelled jar must drop as an ordinary jar.
        input.get(DataComponents.MAX_STACK_SIZE);
        JarLabelData data = input.get(ThaumcraftLabels.JAR_LABEL_DATA.get());
        filter = data == null ? null : Aspect.getAspect(data.tag());
        if (filter != null && getAmount() == 0 && emptyAspect == null) emptyAspect = filter;
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (filter != null) builder.set(ThaumcraftLabels.JAR_LABEL_DATA.get(), new JarLabelData(filter.getTag()));
    }
    @Override public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("AspectFilter");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
