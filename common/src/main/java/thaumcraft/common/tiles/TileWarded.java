package thaumcraft.common.tiles;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.config.ThaumcraftWards;
import thaumcraft.common.lib.world.WardingCasts;

/** Exact original blockstate and owner, with no timer and no copied block inventory: block entities are ineligible for warding. */
public final class TileWarded extends BlockEntity {
    private BlockState original = Blocks.AIR.defaultBlockState();
    private UUID owner;
    private CompoundTag rejected;
    public TileWarded(BlockPos pos, BlockState state) { super(ThaumcraftWards.WARDED_TILE.get(), pos, state); }
    public BlockState originalState() { return original; }
    public UUID owner() { return owner; }
    public boolean validWard() { return rejected == null && owner != null && !original.isAir() && !original.hasBlockEntity() && !original.is(ThaumcraftWards.WARDED.get()); }
    public boolean ownedBy(UUID actor) { return validWard() && owner.equals(actor); }

    /** Only the actual focus's current replacement may initialize its new tile. */
    public void initialize(BlockState original, UUID owner) {
        if (!WardingCasts.isInitializing(this, original, owner) || this.owner != null || owner == null || original == null
                || original.isAir() || original.hasBlockEntity() || original.is(ThaumcraftWards.WARDED.get())) {
            throw new IllegalStateException("No matching ward creation scope");
        }
        this.original = original;
        this.owner = owner;
        setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
    }
    private void write(CompoundTag tag) {
        if (rejected != null) { tag.put("RejectedWard", rejected.copy()); return; }
        if (owner == null) return;
        tag.put("original", NbtUtils.writeBlockState(original));
        tag.putUUID("owner", owner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        original = Blocks.AIR.defaultBlockState(); owner = null; rejected = null;
        if (tag.contains("RejectedWard", Tag.TAG_COMPOUND)) { rejected = tag.getCompound("RejectedWard").copy(); return; }
        try {
            if (!tag.hasUUID("owner") || !tag.contains("original", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Missing ward owner or original state");
            BlockState decoded = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("original"));
            if (decoded.isAir() || decoded.hasBlockEntity() || decoded.is(ThaumcraftWards.WARDED.get())) throw new IllegalArgumentException("Invalid remembered ward block");
            if (!NbtUtils.writeBlockState(decoded).equals(tag.getCompound("original"))) throw new IllegalArgumentException("Unrecognized remembered state properties");
            original = decoded;
            owner = tag.getUUID("owner");
        } catch (RuntimeException malformed) { rejected = tag.copy(); }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { var tag = new CompoundTag(); write(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
