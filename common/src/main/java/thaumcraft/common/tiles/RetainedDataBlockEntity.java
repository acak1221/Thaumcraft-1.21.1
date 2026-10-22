package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.lib.utils.RejectedBlockEntityData;

/** An unread owner is not an empty inventory. Only a subsequent valid load repairs it. */
public abstract class RetainedDataBlockEntity extends BlockEntity {
    private CompoundTag rejectedData;
    protected RetainedDataBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag,registries); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag,registries); }
    public final boolean isDataRejected() { return rejectedData!=null; }
    protected final boolean retainMalformed(CompoundTag tag, Runnable validate) {
        try { validate.run(); rejectedData=null; return false; }
        catch (RuntimeException malformed) {
            rejectedData=tag.copy();
            com.mojang.logging.LogUtils.getLogger().error("Retaining unread {} at {}: {}. Repair offline; gameplay disabled.", getClass().getSimpleName(),worldPosition,malformed.toString());
            archiveRejected();
            return true;
        }
    }
    protected final boolean saveRejected(CompoundTag tag) {
        if (rejectedData==null) return false;
        tag.merge(rejectedData.copy()); return true;
    }
    @Override public void setLevel(Level level) { super.setLevel(level); archiveRejected(); }
    @Override public void setRemoved() { archiveRejected(); super.setRemoved(); }
    private void archiveRejected() {
        if (rejectedData!=null && level instanceof ServerLevel server)
            RejectedBlockEntityData.retain(server,this,rejectedData);
    }
    protected static ListTag compoundList(CompoundTag tag, String key) {
        if (!tag.contains(key)) return new ListTag();
        if (!(tag.get(key) instanceof ListTag list) || (!list.isEmpty() && list.getElementType()!=Tag.TAG_COMPOUND))
            throw new IllegalArgumentException(key+" must be a compound list");
        return list;
    }
    protected static ItemStack requiredStack(HolderLookup.Provider registries, Tag raw) {
        if (!(raw instanceof CompoundTag stack)) throw new IllegalArgumentException("Stack must be a compound");
        ItemStack result=ItemStack.parse(registries,stack).orElseThrow(() -> new IllegalArgumentException("Unread item stack: "+stack));
        if (result.isEmpty()) throw new IllegalArgumentException("Explicit stack decoded empty: "+stack);
        return result;
    }
    protected static void validateSlots(CompoundTag tag, String key, int size, HolderLookup.Provider registries) {
        boolean[] seen=new boolean[size];
        for (Tag value:compoundList(tag,key)) {
            CompoundTag row=(CompoundTag)value; int slot=row.getByte("Slot") & 255;
            if (!row.contains("Slot",Tag.TAG_BYTE) || slot>=size || seen[slot]) throw new IllegalArgumentException(key+": invalid/duplicate Slot "+slot);
            seen[slot]=true; requiredStack(registries,row.get("Stack"));
        }
    }
}
