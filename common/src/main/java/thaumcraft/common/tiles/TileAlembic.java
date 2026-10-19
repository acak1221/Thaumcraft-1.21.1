package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.blocks.BlockAlembic;
import thaumcraft.common.config.ThaumcraftAlembics;
import thaumcraft.common.lib.essentia.EssentiaStorageEndpoint;
import thaumcraft.common.lib.essentia.EssentiaTransfers;

/**
 * TC4 TileAlembic:32-unit, single-aspect, unticked furnace output buffer.
 * The furnace supplies essentia through IAspectContainer; tubes can only extract.
 * A filter chooses the furnace's aspect, NOT addToContainer's acceptance rule.
 */
public final class TileAlembic extends BlockEntity implements IAspectContainer, EssentiaStorageEndpoint {
    public static final int CAPACITY = 32;
    private Aspect aspect;
    private Aspect filter;
    private int amount;

    public TileAlembic(BlockPos pos, BlockState state) {
        super(ThaumcraftAlembics.ALEMBIC_BLOCK_ENTITY.get(), pos, state);
    }

    public Aspect getAspect() { return aspect; }
    public int getAmount() { return amount; }
    public int getMaxAmount() { return CAPACITY; }
    public Aspect getFilter() { return filter; }

    /** One source of truth: native blockstate persists/synchronizes the BE's facing too. */
    public Direction getFacing() { return getBlockState().getValue(BlockAlembic.FACING); }

    public boolean setFacing(Direction facing) {
        if (!canMutate() || facing == null || !facing.getAxis().isHorizontal()) return false;
        BlockState before = getBlockState();
        if (before.getValue(BlockAlembic.FACING) == facing) return true;
        BlockState after = before.setValue(BlockAlembic.FACING, facing);
        if (level == null) setBlockState(after);
        else if (!level.setBlock(worldPosition, after, Block.UPDATE_ALL)) return false;
        changed();
        return true;
    }

    /** State hook used by the native label item and the furnace's selection pass. */
    public void setFilter(Aspect filter) {
        if (!canMutate() || this.filter == filter) return;
        this.filter = filter;
        // BlockMetalDevice:614-618 seeds the type of an empty labelled alembic.
        if (filter != null && amount == 0) aspect = filter;
        changed();
    }

    @Override public AspectList getAspects() {
        return aspect == null ? new AspectList() : new AspectList().add(aspect, amount);
    }

    /** Original TileAlembic:55-57 deliberately does not replace contents. */
    @Override public void setAspects(AspectList aspects) {}

    @Override public boolean doesContainerAccept(Aspect aspect) {
        // Original: always true. Storage compatibility/capacity is decided by
        // addToContainer, and the furnace separately reads getFilter().
        return true;
    }

    @Override public int addToContainer(Aspect aspect, int offered) {
        if (!canMutate() || aspect == null || offered <= 0 || amount >= CAPACITY
                || amount > 0 && this.aspect != aspect) return offered;
        int accepted = Math.min(offered, CAPACITY - amount);
        this.aspect = aspect;
        amount += accepted;
        changed();
        return offered - accepted;
    }

    @Override public boolean takeFromContainer(Aspect aspect, int requested) {
        // Reject malformed negative/zero transfers rather than reproducing the
        // original API's negative-amount duplication defect.
        if (!canMutate() || aspect == null || requested <= 0 || this.aspect != aspect || amount < requested) return false;
        amount -= requested;
        if (amount == 0) this.aspect = null;
        changed();
        return true;
    }

    @Override @Deprecated public boolean takeFromContainer(AspectList aspects) { return false; }

    @Override public boolean doesContainerContainAmount(Aspect aspect, int requested) {
        return amount >= requested && this.aspect == aspect;
    }

    @Override @Deprecated public boolean doesContainerContain(AspectList aspects) {
        // Preserve this deprecated TC4 method's intersection (not subset) test.
        return amount > 0 && aspect != null && aspects != null && aspects.getAmount(aspect) > 0;
    }

    @Override public int containerContains(Aspect aspect) { return aspect != null && this.aspect == aspect ? amount : 0; }

    /** Original sneak-empty-hand action; no vis, GUI, flux or wand-emptying. */
    public void clearContents() {
        if (!canMutate() || amount == 0 && aspect == null) return;
        amount = 0;
        aspect = null;
        changed();
    }

    private boolean canMutate() {
        if (isRemoved() || level != null && level.isClientSide || EssentiaTransfers.isLocked(this)) return false;
        if (level != null && !level.getServer().isSameThread()) throw new IllegalStateException("Alembic changes require the server thread");
        return level == null || level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override public Aspect getStoredEssentiaType() { return aspect; }
    @Override public int getStoredEssentiaAmount() { return amount; }
    @Override public int getEssentiaCapacity() { return CAPACITY; }
    @Override public void setEssentiaStorageRaw(Aspect aspect, int amount) {
        EssentiaTransfers.requireMutation(this);
        this.aspect = aspect;
        this.amount = amount;
    }
    @Override public void notifyEssentiaStorageChanged() {
        if (level != null && !level.isClientSide && !isRemoved() && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this) changed();
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        aspect = Aspect.getAspect(tag.getString("aspect"));
        filter = Aspect.getAspect(tag.getString("AspectFilter"));
        amount = aspect == null ? 0 : Math.clamp(tag.getInt("amount"), 0, CAPACITY);
        // Orientation lives in the native horizontal blockstate, saved by the
        // chunk and sent before BE packets. Do not let a stale BE tag diverge it.
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (aspect != null) tag.putString("aspect", aspect.getTag());
        if (filter != null) tag.putString("AspectFilter", filter.getTag());
        tag.putShort("amount", (short) amount);
        // Direction's native ids exactly match TC4: N=2, S=3, W=4, E=5.
        tag.putByte("facing", (byte) getFacing().get3DDataValue());
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public boolean isConnectable(Direction face) { return face != null && face != Direction.DOWN && face != getFacing(); }
    @Override public boolean canInputFrom(Direction face) { return false; }
    @Override public boolean canOutputTo(Direction face) { return isConnectable(face); }
    @Override public void setSuction(Aspect aspect, int amount) {}
    @Override public Aspect getSuctionType(Direction face) { return null; }
    @Override public int getSuctionAmount(Direction face) { return 0; }
    @Override public Aspect getEssentiaType(Direction face) { return aspect; }
    @Override public int getEssentiaAmount(Direction face) { return amount; }
    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return canOutputTo(face) && takeFromContainer(aspect, amount) ? amount : 0;
    }
    @Override public int addEssentia(Aspect aspect, int amount, Direction face) { return 0; }
    @Override public int getMinimumSuction() { return 0; }
    @Override public boolean renderExtendedTube() { return true; }
}
