package thaumcraft.common.tiles;

import java.util.UUID;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.BlockArcaneBoreBase;
import thaumcraft.common.config.ThaumcraftArcaneBore;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.lib.bore.BoreWork;
import thaumcraft.common.lib.essentia.EssentiaTransfers;

/** Original stockless all-face Perditio suction endpoint. Real pulls and top
 * credit are ONE root-owned atomic operation; this BE never stores a unit. */
public final class TileArcaneBoreBase extends BlockEntity implements IEssentiaTransport {
    private static final SoundEvent TOOL = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", "tool"));
    private UUID lifetime = UUID.randomUUID();
    private long revision;
    private boolean mutating, serializing, retired;
    private CompoundTag rejected;

    public TileArcaneBoreBase(BlockPos pos, BlockState state) { super(ThaumcraftArcaneBore.ARCANE_BORE_BASE_BLOCK_ENTITY.get(), pos, state); }
    public Direction nozzle() { return getBlockState().getValue(BlockArcaneBoreBase.NOZZLE); }
    public UUID lifetimeId() { return lifetime; }
    public long revision() { return revision; }
    public boolean isQuarantined() { return rejected != null; }
    public boolean isSerializing() { return serializing; }
    public boolean isCurrent() {
        return level != null && !retired && !isRemoved() && !isQuarantined()
                && (level.isClientSide || BoreWork.isCurrentServerOwner(this))
                && level.isInWorldBounds(worldPosition) && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this && level.getBlockState(worldPosition) == getBlockState()
                && getBlockState().is(ThaumcraftArcaneBore.ARCANE_BORE_BASE.get());
    }
    private boolean mutable() {
        if (!(level instanceof ServerLevel server)) return false;
        if (!server.getServer().isSameThread()) throw new IllegalStateException("Bore base changes require the server thread");
        return !mutating && !serializing && !BoreWork.isMutationActive() && !EssentiaTransfers.isMutationActive()
                && !BoreWork.isLocked(this) && revision < Long.MAX_VALUE && isCurrent();
    }
    public InteractionResult onWandUse(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.level() != level || !worldPosition.equals(context.getClickedPos())
                || context.getItemInHand() != player.getItemInHand(context.getHand())
                || !(context.getItemInHand().getItem() instanceof ItemWandCasting) || context.getItemInHand().getCount() != 1) return InteractionResult.PASS;
        if (!isCurrent() || !player.isAlive() || player.isSpectator() || !player.canInteractWithBlock(worldPosition, 0)
                || !level.mayInteract(player, worldPosition)
                || !player.mayUseItemAt(worldPosition, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!mutable()) return InteractionResult.FAIL;
        mutating = true;
        try {
            ++revision; // Fence any already-captured nozzle before callback-bearing state update.
            BlockState desired = getBlockState().setValue(BlockArcaneBoreBase.NOZZLE, context.getClickedFace());
            level.setBlock(worldPosition, desired, Block.UPDATE_ALL);
            if (isCurrent() && getBlockState() == desired) {
                setChanged();
                level.playSound(null, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5,
                        TOOL, SoundSource.BLOCKS, .3F, 1.9F + level.random.nextFloat() * .2F);
                player.swing(context.getHand());
            }
            return InteractionResult.CONSUME; // Original wand action costs0 vis.
        } finally { mutating = false; }
    }
    @Override public boolean isConnectable(Direction face) { return !isQuarantined() && !retired; }
    @Override public boolean canInputFrom(Direction face) { return isConnectable(face); }
    @Override public boolean canOutputTo(Direction face) { return false; }
    @Override public void setSuction(Aspect type, int amount) {}
    @Override public Aspect getSuctionType(Direction face) { return Aspect.ENTROPY; }
    @Override public int getSuctionAmount(Direction face) { return isQuarantined() || retired ? 0 : face != nozzle() ? 128 : 0; }
    @Override public int takeEssentia(Aspect type, int amount, Direction face) { return 0; }
    @Override public int addEssentia(Aspect type, int amount, Direction face) { return 0; }
    @Override public Aspect getEssentiaType(Direction face) { return null; }
    @Override public int getEssentiaAmount(Direction face) { return 0; }
    @Override public int getMinimumSuction() { return 0; }
    @Override public boolean renderExtendedTube() { return true; }
    @Override public void setRemoved() {
        retired = true;
        try { BoreWork.retired(this); } finally { super.setRemoved(); }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        beginSerialization();
        try {
            super.saveAdditional(tag, registries); writeCustom(tag);
            tag.putLong("BoreRevision", revision);
            if (rejected != null) tag.put("RejectedBoreBase", rejected.copy());
        } finally { serializing = false; }
    }
    private void writeCustom(CompoundTag tag) {
        tag.putInt("orientation", nozzle().get3DDataValue());
    }
    private void beginSerialization() {
        if (serializing || level instanceof ServerLevel server && !server.getServer().isSameThread())
            throw new IllegalStateException("Recursive/off-thread bore base serialization");
        serializing = true;
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // Global fence applies to detached loadStatic too, before any restoration.
        if (BoreWork.isMutationActive() || EssentiaTransfers.isMutationActive() || mutating || serializing || retired)
            throw new IllegalStateException("Bore base cannot load during an atomic mutation");
        if (level instanceof ServerLevel server && (!server.getServer().isSameThread() || BoreWork.isLocked(this)))
            throw new IllegalStateException("Bore base load requires an idle server owner");
        mutating = true;
        try {
            super.loadAdditional(tag, registries);
            lifetime = UUID.randomUUID(); revision = 0; rejected = null;
            if (tag.contains("RejectedBoreBase")) {
                rejected = tag.contains("RejectedBoreBase", Tag.TAG_COMPOUND) ? tag.getCompound("RejectedBoreBase").copy() : tag.copy(); return;
            }
            readCustom(tag);
        } finally { mutating = false; }
    }
    private void readCustom(CompoundTag tag) {
        try {
            if (!tag.contains("orientation", Tag.TAG_INT) || tag.getInt("orientation") < 0 || tag.getInt("orientation") > 5)
                throw new IllegalArgumentException("Invalid bore nozzle");
            if (!levelIsClient() && Direction.from3DDataValue(tag.getInt("orientation")) != nozzle())
                throw new IllegalArgumentException("Bore nozzle/state mismatch");
            if (tag.contains("BoreRevision")) {
                if (!tag.contains("BoreRevision", Tag.TAG_LONG) || tag.getLong("BoreRevision") < 0 || tag.getLong("BoreRevision") == Long.MAX_VALUE)
                    throw new IllegalArgumentException("Invalid bore base revision");
                revision = tag.getLong("BoreRevision");
            }
        } catch (RuntimeException malformed) { rejected = tag.copy(); }
    }
    private boolean levelIsClient() { return level != null && level.isClientSide; }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        beginSerialization();
        try { CompoundTag tag = new CompoundTag(); writeCustom(tag); return tag; }
        finally { serializing = false; }
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
